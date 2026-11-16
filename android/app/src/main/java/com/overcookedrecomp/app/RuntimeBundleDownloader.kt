package com.overcookedrecomp.app

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * Downloads the version-matched runtime manifest and archive from GitHub Releases.
 * SHA-256 and declared size are checked before the safe staging installer runs.
 */
object RuntimeBundleDownloader {
    private const val TAG = "RuntimeBundleDownload"
    private const val MAX_MANIFEST_BYTES = 64 * 1024
    private const val MAX_ARCHIVE_BYTES = 8L * 1024L * 1024L * 1024L
    private const val MAX_ATTEMPTS = 3

    @JvmStatic
    @Throws(IOException::class)
    fun downloadAndInstall(context: Context, progress: Consumer<String>?) {
        val version = BuildConfig.VERSION_NAME
        val manifestUrl = "https://github.com/dnctf/ghost-town-recomp/releases/download/v$version/runtime-manifest.json"
        progress?.accept("Memeriksa manifest runtime v$version...")
        val manifest = JSONObject(readHttps(manifestUrl, MAX_MANIFEST_BYTES))
        if (manifest.optString("runtime_version") != version) {
            throw IOException("Versi manifest tidak cocok dengan APK ($version).")
        }
        if (manifest.optString("architecture") != "arm64-v8a") {
            throw IOException("Runtime manifest bukan untuk Android ARM64.")
        }

        val archiveUrl = manifest.optString("archive_url")
        val archiveUri = URL(archiveUrl)
        if (archiveUri.protocol != "https" || archiveUri.host != "github.com") {
            throw IOException("URL runtime harus berasal dari HTTPS GitHub Release.")
        }
        val expectedSha = manifest.optString("sha256").lowercase()
        if (!expectedSha.matches(Regex("[0-9a-f]{64}"))) {
            throw IOException("SHA-256 pada manifest tidak valid.")
        }
        val expectedSize = manifest.optLong("size_bytes", -1L)
        if (expectedSize <= 0L || expectedSize > MAX_ARCHIVE_BYTES) {
            throw IOException("Ukuran runtime pada manifest tidak valid.")
        }

        val downloadDir = File(context.cacheDir, "runtime-download")
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            throw IOException("Tidak dapat membuat direktori unduhan runtime.")
        }
        val partial = File(downloadDir, "runtime-$version.tar.gz.part")
        val archive = File(downloadDir, "runtime-$version.tar.gz")

        var lastError: Throwable? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                if (partial.length() == expectedSize) {
                    verifySha256(partial, expectedSha)
                    lastError = null
                    break
                }
                downloadAttempt(archiveUrl, partial, expectedSize, attempt, progress)
                if (partial.length() != expectedSize) {
                    throw IOException("Unduhan runtime tidak lengkap (${partial.length()} / $expectedSize byte).")
                }
                progress?.accept("Memverifikasi SHA-256 runtime...")
                verifySha256(partial, expectedSha)
                lastError = null
                break
            } catch (error: Throwable) {
                lastError = error
                Log.w(TAG, "Runtime download attempt $attempt failed", error)
                if (attempt == MAX_ATTEMPTS) break
                progress?.accept("Unduhan terputus; mencoba lagi ($attempt/$MAX_ATTEMPTS)...")
            }
        }
        if (lastError != null) throw IOException("Unduhan runtime gagal: ${lastError.message}", lastError)
        if (!partial.exists() || partial.length() != expectedSize) {
            throw IOException("Arsip runtime tidak tersedia setelah unduhan.")
        }
        if (archive.exists() && !archive.delete()) throw IOException("Tidak dapat mengganti arsip sementara.")
        if (!partial.renameTo(archive)) throw IOException("Tidak dapat menyiapkan arsip runtime terverifikasi.")

        progress?.accept("Checksum cocok. Mengekstrak dan memasang runtime...")
        try {
            RuntimeBundleInstaller.install(context, Uri.fromFile(archive), Consumer { message ->
                progress?.accept(message)
            })
        } finally {
            archive.delete()
            partial.delete()
        }
        progress?.accept("Runtime v$version berhasil dipasang.")
    }

    private fun readHttps(url: String, maxBytes: Int): String {
        val connection = openHttps(url)
        try {
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > maxBytes) {
                        throw IOException("Manifest runtime terlalu besar.")
                    }
                    output.write(buffer, 0, count)
                }
            }
            return output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }

    private fun openHttps(url: String): HttpURLConnection {
        val parsed = URL(url)
        if (parsed.protocol != "https" || parsed.host != "github.com") {
            throw IOException("Hanya URL HTTPS GitHub yang diizinkan.")
        }
        val connection = parsed.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
        connection.readTimeout = TimeUnit.SECONDS.toMillis(60).toInt()
        connection.setRequestProperty("User-Agent", "GhostTownRecomp/${BuildConfig.VERSION_NAME}")
        connection.setRequestProperty("Accept", "application/octet-stream")
        val status = connection.responseCode
        val finalHost = connection.url.host
        val allowedFinalHost = finalHost == "github.com" ||
            finalHost == "githubusercontent.com" ||
            finalHost.endsWith(".githubusercontent.com")
        if (connection.url.protocol != "https" || !allowedFinalHost) {
            connection.disconnect()
            throw IOException("Redirect unduhan keluar dari host GitHub yang diizinkan.")
        }
        if (status !in 200..299) {
            connection.disconnect()
            throw IOException("HTTP $status saat mengakses GitHub Release.")
        }
        return connection
    }

    private fun downloadAttempt(
        url: String,
        destination: File,
        expectedSize: Long,
        attempt: Int,
        progress: Consumer<String>?
    ) {
        val offset = destination.length()
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
        connection.readTimeout = TimeUnit.SECONDS.toMillis(90).toInt()
        connection.setRequestProperty("User-Agent", "GhostTownRecomp/${BuildConfig.VERSION_NAME}")
        connection.setRequestProperty("Accept", "application/octet-stream")
        if (offset > 0L) connection.setRequestProperty("Range", "bytes=$offset-")
        try {
            val status = connection.responseCode
            val finalHost = connection.url.host
            val allowedFinalHost = finalHost == "github.com" ||
                finalHost == "githubusercontent.com" ||
                finalHost.endsWith(".githubusercontent.com")
            if (connection.url.protocol != "https" || !allowedFinalHost) {
                throw IOException("Redirect arsip keluar dari host GitHub yang diizinkan.")
            }
            val append = offset > 0L && status == HttpURLConnection.HTTP_PARTIAL
            if (status != HttpURLConnection.HTTP_OK && !append) {
                throw IOException("HTTP $status saat mengunduh runtime.")
            }
            if (append) {
                val contentRange = connection.getHeaderField("Content-Range") ?: ""
                if (!contentRange.startsWith("bytes $offset-")) {
                    throw IOException("Server mengirim Content-Range yang tidak sesuai.")
                }
            } else if (destination.exists() && !destination.delete()) {
                throw IOException("Tidak dapat mengulang unduhan runtime dari awal.")
            }

            var downloaded = if (append) offset else 0L
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination, append).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloaded += count
                        if (downloaded > expectedSize || downloaded > MAX_ARCHIVE_BYTES) {
                            throw IOException("Unduhan melebihi ukuran yang dinyatakan manifest.")
                        }
                        output.write(buffer, 0, count)
                        if (downloaded % (8L * 1024L * 1024L) < count) {
                            val percent = (downloaded * 100L / expectedSize).coerceIn(0L, 100L)
                            progress?.accept("Mengunduh runtime: $percent% (percobaan $attempt/$MAX_ATTEMPTS)")
                        }
                    }
                    output.fd.sync()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun verifySha256(file: File, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != expected) {
            file.delete()
            throw IOException("SHA-256 runtime tidak cocok. Arsip sementara dihapus.")
        }
    }
}
