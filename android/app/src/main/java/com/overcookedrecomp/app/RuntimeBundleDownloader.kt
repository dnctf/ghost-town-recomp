package com.overcookedrecomp.app

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.function.Consumer

/** Downloads the project-owned runtime, verifies its SHA-256 sidecar, and installs it transactionally. */
object RuntimeBundleDownloader {
    private const val RELEASE_BASE = "https://github.com/dnctf/ghost-town-recomp/releases/download/v0.4.0-alpha.1"
    private const val ARCHIVE_NAME = "ghost-town-wine-runtime-arm64.tar.gz"
    private const val CHECKSUM_NAME = "$ARCHIVE_NAME.sha256"
    private const val MAX_ARCHIVE_BYTES = 8L * 1024L * 1024L * 1024L

    @JvmStatic
    @Throws(IOException::class)
    fun downloadAndInstall(context: Context, progress: Consumer<String>? = null) {
        val cache = File(context.cacheDir, "runtime-download")
        if (!cache.exists() && !cache.mkdirs()) throw IOException("Tidak dapat membuat cache download.")
        val archive = File(cache, ARCHIVE_NAME)
        val partial = File(cache, "$ARCHIVE_NAME.part")
        val checksum = fetchText("$RELEASE_BASE/$CHECKSUM_NAME").trim()
        val expected = checksum.split(Regex("\\s+"), limit = 2).firstOrNull()?.lowercase(Locale.ROOT)
            ?: throw IOException("Checksum sidecar kosong.")
        if (!expected.matches(Regex("[0-9a-f]{64}"))) throw IOException("Checksum sidecar tidak valid.")

        progress?.accept("Mengunduh runtime resmi 0.4.0-alpha.1...")
        partial.delete()
        downloadFile("$RELEASE_BASE/$ARCHIVE_NAME", partial, progress)
        val actual = sha256(partial)
        if (!actual.equals(expected, ignoreCase = true)) {
            partial.delete()
            throw IOException("SHA-256 runtime tidak cocok. Arsip ditolak.")
        }
        if (archive.exists() && !archive.delete()) throw IOException("Tidak dapat mengganti cache runtime lama.")
        if (!partial.renameTo(archive)) throw IOException("Tidak dapat menyimpan arsip runtime terverifikasi.")

        progress?.accept("Checksum cocok. Memasang runtime ke penyimpanan privat...")
        try {
            RuntimeBundleInstaller.install(context, Uri.fromFile(archive), progress)
        } finally {
            archive.delete()
        }
    }

    private fun fetchText(url: String): String {
        val connection = open(url)
        try {
            if (connection.responseCode !in 200..299) throw IOException("Server checksum merespons HTTP ${connection.responseCode}.")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { connection.disconnect() }
    }

    private fun downloadFile(url: String, destination: File, progress: Consumer<String>?) {
        val connection = open(url)
        try {
            if (connection.responseCode !in 200..299) throw IOException("Server runtime merespons HTTP ${connection.responseCode}.")
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_ARCHIVE_BYTES) throw IOException("Arsip runtime melewati batas 8 GiB.")
            var downloaded = 0L
            BufferedInputStream(connection.inputStream).use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloaded += count
                        if (downloaded > MAX_ARCHIVE_BYTES) throw IOException("Download runtime melewati batas 8 GiB.")
                        output.write(buffer, 0, count)
                        if (downloaded % (16L * 1024L * 1024L) < count) {
                            val total = if (contentLength > 0) " / ${contentLength / (1024 * 1024)} MiB" else ""
                            progress?.accept("Mengunduh: ${downloaded / (1024 * 1024)} MiB$total")
                        }
                    }
                }
            }
            if (downloaded == 0L) throw IOException("Arsip runtime kosong.")
            if (contentLength > 0 && downloaded != contentLength) throw IOException("Download terputus: ukuran file tidak sesuai Content-Length.")
        } finally { connection.disconnect() }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 30_000
        connection.readTimeout = 120_000
        connection.setRequestProperty("User-Agent", "GhostTownRecomp-Android/0.4.0-alpha.1")
        connection.setRequestProperty("Accept", "application/octet-stream")
        connection.connect()
        if (connection.url.protocol != "https") {
            connection.disconnect()
            throw IOException("Runtime hanya boleh diunduh melalui HTTPS.")
        }
        return connection
    }

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
