package com.overcookedrecomp.app

import android.content.Context
import android.net.Uri
import android.os.StatFs
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.function.Consumer

/** Manifest-driven runtime pack provisioning. No archive URL or checksum is hardcoded here. */
object RuntimeBundleDownloader {
    private const val MAX_ARCHIVE_BYTES = 8L * 1024L * 1024L * 1024L
    private const val RESERVE_BYTES = 64L * 1024L * 1024L
    private const val MAX_ATTEMPTS = 3

    private data class RuntimePack(
        val version: String,
        val url: String,
        val sizeBytes: Long,
        val expandedSizeBytes: Long,
        val sha256: String,
        val minimumAppVersion: String
    )

    @JvmStatic
    @Throws(IOException::class)
    fun downloadAndInstall(context: Context, progress: Consumer<String>? = null) {
        val pack = parseManifest(fetchText(BuildConfig.RUNTIME_MANIFEST_URL))
        if (compareVersions(BuildConfig.VERSION_NAME, pack.minimumAppVersion) < 0) {
            throw IOException("Runtime ini memerlukan aplikasi minimal " + pack.minimumAppVersion + ".")
        }

        val cache = File(context.cacheDir, "runtime-download")
        if (!cache.exists() && !cache.mkdirs()) throw IOException("Tidak dapat membuat cache download.")
        val partial = File(cache, "runtime-pack.tar.gz.part")
        if (partial.length() > pack.sizeBytes) partial.delete()

        val expandedReserve = if (pack.expandedSizeBytes > Long.MAX_VALUE / 10L) Long.MAX_VALUE
            else (pack.expandedSizeBytes * 10L) / 9L
        val required = safeAdd(safeAdd(pack.sizeBytes, expandedReserve), RESERVE_BYTES)
        val available = StatFs(context.filesDir.absolutePath).availableBytes
        if (available < required) {
            throw IOException(
                "Ruang penyimpanan tidak cukup. Diperlukan sekitar " + (required / (1024 * 1024)) +
                    " MiB; tersedia " + (available / (1024 * 1024)) + " MiB."
            )
        }

        progress?.accept("Memeriksa runtime pack " + pack.version + "...")
        var lastError: IOException? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                downloadResumable(pack, partial, progress)
                lastError = null
                break
            } catch (error: IOException) {
                lastError = error
                if (attempt == MAX_ATTEMPTS) break
                progress?.accept("Download terputus. Melanjutkan ulang (percobaan " + (attempt + 1) + "/" + MAX_ATTEMPTS + ")...")
                try {
                    Thread.sleep(1000L shl (attempt - 1))
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Download dibatalkan.", interrupted)
                }
            }
        }
        if (lastError != null) throw lastError

        if (partial.length() != pack.sizeBytes) {
            partial.delete()
            throw IOException("Ukuran runtime tidak cocok dengan manifest.")
        }
        progress?.accept("Memverifikasi SHA-256 runtime...")
        if (!sha256(partial).equals(pack.sha256, ignoreCase = true)) {
            partial.delete()
            throw IOException("SHA-256 runtime tidak cocok. File parsial yang korup dihapus.")
        }

        installVerifiedArchive(context, pack, partial, progress)
        partial.delete()
        progress?.accept("Runtime " + pack.version + " berhasil diunduh, diverifikasi, dan dipasang.")
    }


    /** Imports a SAF-selected archive only when it exactly matches the published manifest. */
    @JvmStatic
    @Throws(IOException::class)
    fun importLocalArchive(context: Context, archiveUri: Uri, progress: Consumer<String>? = null) {
        val pack = parseManifest(fetchText(BuildConfig.RUNTIME_MANIFEST_URL))
        if (compareVersions(BuildConfig.VERSION_NAME, pack.minimumAppVersion) < 0) {
            throw IOException("Runtime ini memerlukan aplikasi minimal " + pack.minimumAppVersion + ".")
        }
        val cache = File(context.cacheDir, "runtime-download")
        if (!cache.exists() && !cache.mkdirs()) throw IOException("Tidak dapat membuat cache impor runtime.")
        val local = File(cache, "runtime-local.tar.gz.part")
        if (local.exists() && !local.delete()) throw IOException("Tidak dapat membersihkan impor runtime sebelumnya.")
        val expandedReserve = if (pack.expandedSizeBytes > Long.MAX_VALUE / 10L) Long.MAX_VALUE
            else (pack.expandedSizeBytes * 10L) / 9L
        val required = safeAdd(safeAdd(pack.sizeBytes, expandedReserve), RESERVE_BYTES)
        val available = StatFs(context.filesDir.absolutePath).availableBytes
        if (available < required) {
            throw IOException("Ruang penyimpanan tidak cukup untuk impor runtime lokal.")
        }

        progress?.accept("Menyalin runtime lokal untuk verifikasi...")
        try {
            val raw = context.contentResolver.openInputStream(archiveUri)
                ?: throw IOException("Tidak dapat membuka arsip runtime lokal.")
            var total = 0L
            raw.use { input ->
                FileOutputStream(local, false).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > pack.sizeBytes || total > MAX_ARCHIVE_BYTES) {
                            throw IOException("Arsip lokal melebihi ukuran runtime pada manifest.")
                        }
                        output.write(buffer, 0, count)
                        if (total / (16L * 1024L * 1024L) != (total - count) / (16L * 1024L * 1024L)) {
                            progress?.accept("Menyalin runtime lokal: " + (total / (1024 * 1024)) + " MiB")
                        }
                    }
                    output.fd.sync()
                }
            }
            if (total != pack.sizeBytes) throw IOException("Ukuran file runtime lokal tidak cocok dengan manifest.")
            if (!sha256(local).equals(pack.sha256, ignoreCase = true)) {
                throw IOException("SHA-256 file runtime lokal tidak cocok dengan manifest.")
            }
            installVerifiedArchive(context, pack, local, progress)
            progress?.accept("Runtime lokal berhasil diverifikasi dan dipasang.")
        } catch (error: Throwable) {
            local.delete()
            throw if (error is IOException) error else IOException("Impor runtime lokal gagal: ${error.message}", error)
        } finally {
            local.delete()
        }
    }

    private fun installVerifiedArchive(context: Context, pack: RuntimePack, archive: File, progress: Consumer<String>?) {
        progress?.accept("Checksum cocok. Mengekstrak runtime secara atomik...")
        FileInputStream(archive).use { input ->
            RuntimeBundleInstaller.install(context, input, progress)
        }
        val installed = File(context.filesDir, "runtime/wine/.runtime-version")
        installed.writeText(pack.version + "\n")
        if (!installed.isFile || installed.readText().trim() != BuildConfig.VERSION_NAME) {
            throw IOException("Versi runtime terpasang tidak dapat diverifikasi.")
        }
    }

    private fun parseManifest(raw: String): RuntimePack {
        val json = try { JSONObject(raw) } catch (error: Exception) {
            throw IOException("Manifest runtime bukan JSON yang valid.", error)
        }
        val manifestVersion = json.optString("release")
        if (manifestVersion != BuildConfig.VERSION_NAME) {
            throw IOException("Manifest release " + manifestVersion + " tidak cocok dengan aplikasi " + BuildConfig.VERSION_NAME + ".")
        }
        val item = json.optJSONObject("runtime_pack")
            ?: throw IOException("Manifest belum memuat konfigurasi runtime_pack.")
        if (item.optString("status", "pending") != "published") {
            throw IOException("Runtime pack belum dipublikasikan. Silakan coba lagi setelah rilis runtime tersedia.")
        }
        val version = item.optString("version")
        val url = item.optString("url")
        val size = item.optLong("size_bytes", -1L)
        val expanded = item.optLong("expanded_size_bytes", -1L)
        val hash = item.optString("sha256").lowercase(Locale.ROOT)
        val minimum = item.optString("minimum_app_version")
        if (version.isBlank() || version != manifestVersion) throw IOException("Versi runtime pack tidak sinkron.")
        if (!url.startsWith("https://")) throw IOException("URL runtime pack harus menggunakan HTTPS.")
        if (size <= 0L || size > MAX_ARCHIVE_BYTES || expanded <= 0L || expanded > MAX_ARCHIVE_BYTES) {
            throw IOException("Ukuran runtime pack pada manifest tidak valid.")
        }
        if (!hash.matches(Regex("[0-9a-f]{64}"))) throw IOException("SHA-256 runtime pack tidak valid.")
        if (minimum.isBlank()) throw IOException("minimum_app_version tidak tersedia.")
        return RuntimePack(version, url, size, expanded, hash, minimum)
    }

    private fun downloadResumable(pack: RuntimePack, destination: File, progress: Consumer<String>?) {
        val existing = destination.length()
        val connection = open(pack.url, if (existing > 0L) existing else null)
        try {
            val code = connection.responseCode
            if (code == 416 && existing == pack.sizeBytes) return
            if (code !in 200..299) throw IOException("Server runtime merespons HTTP " + code + ".")
            val append = existing > 0L && code == HttpURLConnection.HTTP_PARTIAL
            if (append) {
                val range = connection.getHeaderField("Content-Range") ?: ""
                if (!range.startsWith("bytes " + existing + "-")) {
                    throw IOException("Content-Range tidak sesuai; file parsial dipertahankan.")
                }
            }
            val start = if (append) existing else 0L
            val responseLength = connection.contentLengthLong
            if (responseLength > 0L && responseLength != pack.sizeBytes - start) {
                throw IOException("Content-Length runtime tidak cocok dengan manifest.")
            }
            var total = start
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination, append).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > pack.sizeBytes || total > MAX_ARCHIVE_BYTES) {
                            throw IOException("Download runtime melebihi ukuran pada manifest.")
                        }
                        output.write(buffer, 0, count)
                        if (total / (16L * 1024L * 1024L) != (total - count) / (16L * 1024L * 1024L)) {
                            progress?.accept("Mengunduh runtime: " + (total / (1024 * 1024)) + " / " + (pack.sizeBytes / (1024 * 1024)) + " MiB")
                        }
                    }
                    output.fd.sync()
                }
            }
            if (total != pack.sizeBytes) throw IOException("Download belum lengkap; file parsial akan dilanjutkan.")
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchText(url: String): String {
        val connection = open(url)
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Server manifest merespons HTTP " + connection.responseCode + ".")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, rangeStart: Long? = null): HttpURLConnection {
        var current = URL(url)
        repeat(6) {
            if (current.protocol != "https") throw IOException("Hanya URL HTTPS yang diizinkan.")
            val connection = current.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            connection.setRequestProperty("User-Agent", "GhostTownRecomp-Android/" + BuildConfig.VERSION_NAME)
            connection.setRequestProperty("Accept", "application/octet-stream, application/json")
            if (rangeStart != null) connection.setRequestProperty("Range", "bytes=" + rangeStart + "-")
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IOException("Redirect runtime tidak memiliki Location.")
                connection.disconnect()
                current = URL(current, location)
            } else {
                return connection
            }
        }
        throw IOException("Terlalu banyak redirect saat mengakses runtime pack.")
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split("-", limit = 2)
        val rightParts = right.split("-", limit = 2)
        val a = Regex("\\d+").findAll(leftParts[0]).map { it.value.toInt() }.toList()
        val b = Regex("\\d+").findAll(rightParts[0]).map { it.value.toInt() }.toList()
        for (i in 0 until maxOf(a.size, b.size)) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        if (leftParts.size == 1 && rightParts.size == 2) return 1
        if (leftParts.size == 2 && rightParts.size == 1) return -1
        return if (leftParts.size == 2) leftParts[1].compareTo(rightParts[1]) else 0
    }

    private fun safeAdd(a: Long, b: Long): Long =
        if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
