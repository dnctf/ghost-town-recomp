package com.overcookedrecomp.app

import android.content.Context
import android.net.Uri
import android.os.StatFs
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.FileInputStream
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer

/**
 * Installs a user-selected .tar.gz runtime into app-private storage.
 *
 * Extraction rejects absolute/traversal paths and symlinks escaping the staging
 * directory. The old runtime is kept until the staged payload passes validation.
 */
object RuntimeBundleInstaller {
    private const val MAX_ENTRIES = 1_000_000
    private const val MAX_EXPANDED_BYTES = 8L * 1024L * 1024L * 1024L

    @JvmStatic
    @Throws(IOException::class)
    fun install(context: Context, archiveUri: Uri, progress: Consumer<String>? = null) {
        val runtimeParent = File(context.filesDir, "runtime")
        if (!runtimeParent.exists() && !runtimeParent.mkdirs()) {
            throw IOException("Tidak dapat membuat direktori runtime.")
        }

        val availableBytes = StatFs(runtimeParent.absolutePath).availableBytes
        val stage = File(runtimeParent, "wine.installing")
        val target = File(runtimeParent, "wine")
        val backup = File(runtimeParent, "wine.previous")
        deleteTree(stage)
        if (!stage.mkdirs()) throw IOException("Tidak dapat membuat direktori staging.")

        var totalBytes = 0L
        var entries = 0
        try {
            val raw = context.contentResolver.openInputStream(archiveUri)
                ?: throw IOException("Tidak dapat membuka file runtime.")
            raw.use { input ->
                TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(input))).use { tar ->
                    var entry = tar.nextTarEntry
                    while (entry != null) {
                        entries++
                        if (entries > MAX_ENTRIES) throw IOException("Arsip memiliki terlalu banyak entri.")
                        val relative = safeRelativePath(entry.name)
                        if (relative != null) {
                            val destination = stage.toPath().resolve(relative).normalize()
                            val root = stage.toPath().toAbsolutePath().normalize()
                            if (!destination.toAbsolutePath().normalize().startsWith(root)) {
                                throw IOException("Path arsip keluar dari direktori runtime.")
                            }
                            ensureSafeParents(root, destination.parent)

                            when {
                                entry.isDirectory -> {
                                    Files.createDirectories(destination)
                                }
                                entry.isSymbolicLink -> {
                                    val link = Paths.get(entry.linkName)
                                    if (link.isAbsolute) throw IOException("Symlink absolut ditolak: ${entry.name}")
                                    val resolved = destination.parent.resolve(link).normalize()
                                    if (!resolved.toAbsolutePath().normalize().startsWith(root)) {
                                        throw IOException("Symlink keluar dari runtime ditolak: ${entry.name}")
                                    }
                                    Files.createSymbolicLink(destination, link)
                                }
                                entry.isLink -> {
                                    val linkTarget = safeRelativePath(entry.linkName)
                                        ?: throw IOException("Hardlink tidak valid: ${entry.name}")
                                    val targetPath = root.resolve(linkTarget).normalize()
                                    if (!targetPath.startsWith(root) || !Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
                                        throw IOException("Target hardlink tidak ditemukan: ${entry.linkName}")
                                    }
                                    Files.createLink(destination, targetPath)
                                }
                                entry.isFile -> {
                                    if (entry.size < 0 || totalBytes + entry.size > MAX_EXPANDED_BYTES) {
                                        throw IOException("Ukuran runtime melewati batas 8 GiB.")
                                    }
                                    if (totalBytes + entry.size > availableBytes * 9 / 10) {
                                        throw IOException("Ruang penyimpanan tidak cukup untuk memasang runtime.")
                                    }
                                    Files.newOutputStream(destination).use { output ->
                                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                        while (true) {
                                            val read = tar.read(buffer)
                                            if (read < 0) break
                                            output.write(buffer, 0, read)
                                            totalBytes += read
                                            if (totalBytes > MAX_EXPANDED_BYTES ||
                                                totalBytes > availableBytes * 9 / 10) {
                                                throw IOException("Ruang penyimpanan tidak cukup atau ukuran arsip terlalu besar.")
                                            }
                                        }
                                    }
                                    val executable = (entry.mode and 0b001001001) != 0
                                    if (executable) destination.toFile().setExecutable(true, false)
                                    destination.toFile().setReadable(true, false)
                                }
                                else -> throw IOException("Tipe entri arsip tidak didukung: ${entry.name}")
                            }
                        }
                        if (entries % 1000 == 0) {
                            progress?.accept("Mengekstrak runtime: $entries entri, ${totalBytes / (1024 * 1024)} MiB")
                        }
                        entry = tar.nextTarEntry
                    }
                }
            }

            validate(stage)
            deleteTree(backup)
            if (target.exists() && !target.renameTo(backup)) {
                throw IOException("Tidak dapat mencadangkan runtime lama.")
            }
            if (!stage.renameTo(target)) {
                if (backup.exists()) backup.renameTo(target)
                throw IOException("Tidak dapat mengaktifkan runtime baru.")
            }
            deleteTree(backup)
            progress?.accept("Runtime berhasil dipasang (${totalBytes / (1024 * 1024)} MiB).")
        } catch (t: Throwable) {
            deleteTree(stage)
            if (!target.exists() && backup.exists()) backup.renameTo(target)
            throw if (t is IOException) t else IOException("Instalasi runtime gagal: ${t.message}", t)
        }
    }

    private fun safeRelativePath(raw: String): Path? {
        if (raw.isBlank() || raw == "." || raw == "./") return null
        if (raw.startsWith("/") || raw.startsWith("\\")) {
            throw IOException("Path absolut ditolak: $raw")
        }
        val path = Paths.get(raw).normalize()
        if (path.isAbsolute || path.any { it.toString() == ".." }) {
            throw IOException("Path traversal ditolak: $raw")
        }
        return path
    }

    private fun ensureSafeParents(root: Path, parent: Path?) {
        if (parent == null) return
        val normalized = parent.toAbsolutePath().normalize()
        if (!normalized.startsWith(root)) throw IOException("Direktori induk tidak valid.")
        var current = root
        val relative = root.relativize(normalized)
        for (part in relative) {
            current = current.resolve(part)
            if (Files.isSymbolicLink(current)) throw IOException("Symlink induk arsip ditolak.")
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw IOException("Path induk bukan direktori.")
            }
        }
    }

    private fun validate(root: File) {
        val required = listOf(
            "bin/proot",
            "libexec/proot/loader",
            "rootfs/etc/os-release",
            "rootfs/usr/local/bin/box64",
            "rootfs/opt/wine/bin/wine",
            "rootfs/opt/dxvk/x64/d3d11.dll",
            "rootfs/opt/dxvk/x64/dxgi.dll"
        )
        val missing = required.filterNot { File(root, it).isFile }
        if (missing.isNotEmpty()) throw IOException("Runtime tidak lengkap: ${missing.joinToString()}")
        listOf(
            "bin/proot",
            "rootfs/usr/local/bin/box64",
            "rootfs/opt/wine/bin/wine"
        ).forEach {
            if (!File(root, it).setExecutable(true, false) && !File(root, it).canExecute()) {
                throw IOException("Tidak dapat memberi izin executable: $it")
            }
        }
        verifyRuntimeChecksums(root)
    }

    private fun verifyRuntimeChecksums(root: File) {
        val checksumFile = File(root, "provenance/RUNTIME-SHA256SUMS")
        if (!checksumFile.isFile) throw IOException("Manifest checksum runtime tidak ditemukan.")
        val required = setOf(
            "bin/proot",
            "libexec/proot/loader",
            "libexec/proot/loader32",
            "rootfs/usr/local/bin/box64",
            "rootfs/opt/wine/bin/wine",
            "rootfs/opt/dxvk/x64/d3d11.dll",
            "rootfs/opt/dxvk/x64/dxgi.dll"
        )
        val verified = mutableSetOf<String>()
        checksumFile.forEachLine { line ->
            val parts = line.trim().split(Regex("\\s+"), limit = 2)
            if (parts.size != 2 || !parts[0].matches(Regex("[0-9a-fA-F]{64}"))) {
                throw IOException("Format checksum runtime tidak valid.")
            }
            val relative = safeRelativePath(parts[1])
                ?: throw IOException("Path checksum tidak valid.")
            val name = relative.toString()
            val file = File(root, name)
            if (!file.isFile) throw IOException("File checksum tidak ditemukan: $name")
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(parts[0], ignoreCase = true)) {
                throw IOException("Checksum tidak cocok: $name")
            }
            verified += name
        }
        val missing = required - verified
        if (missing.isNotEmpty()) {
            throw IOException("Checksum wajib belum diverifikasi: ${missing.joinToString()}")
        }
    }

    private fun deleteTree(file: File) {
        if (!file.exists() && !Files.isSymbolicLink(file.toPath())) return
        if (Files.isSymbolicLink(file.toPath()) || file.isFile) {
            Files.deleteIfExists(file.toPath())
        } else {
            file.listFiles()?.forEach { deleteTree(it) }
            Files.deleteIfExists(file.toPath())
        }
    }
}
