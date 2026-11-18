package com.overcookedrecomp.app

import android.content.Context
import android.net.Uri
import android.os.StatFs
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.Collections
import java.io.FileInputStream
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
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
        val input = context.contentResolver.openInputStream(archiveUri)
            ?: throw IOException("Tidak dapat membuka file runtime.")
        install(context, input, progress)
    }

    /** Installs the runtime archive embedded in the APK assets. */
    @JvmStatic
    @Throws(IOException::class)
    fun installBundled(context: Context, assetName: String, progress: Consumer<String>? = null) {
        context.assets.open(assetName).use { input -> install(context, input, progress) }
    }

    /**
     * Installs a large APK-bundled archive from independently hashed chunks.
     * Splitting the compressed archive avoids buffering a multi-gigabyte asset
     * as one allocation during Android Gradle packaging.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun installBundledParts(context: Context, manifestAssetName: String, progress: Consumer<String>? = null) {
        val manifest = context.assets.open(manifestAssetName).bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
        if (manifest.isEmpty() || manifest.size > 128) {
            throw IOException("Manifest runtime parts kosong atau memiliki terlalu banyak bagian.")
        }

        val partPattern = Regex("runtime-[0-9]{4}\\.bin")
        val entries = manifest.map { line ->
            val fields = line.split(Regex("\\s+"), limit = 2)
            if (fields.size != 2 || !fields[0].matches(Regex("[0-9a-fA-F]{64}")) ||
                !partPattern.matches(fields[1])) {
                throw IOException("Baris manifest runtime tidak valid: $line")
            }
            fields[0].lowercase() to fields[1]
        }

        entries.forEachIndexed { index, entry ->
            verifyAssetChecksum(context, "runtime/parts/${entry.second}", entry.first)
            progress?.accept("Memverifikasi bagian runtime ${index + 1}/${entries.size}...")
        }

        val streams = ArrayList<InputStream>(entries.size)
        try {
            entries.forEach { entry ->
                streams.add(context.assets.open("runtime/parts/${entry.second}"))
            }
            install(context, SequenceInputStream(Collections.enumeration(streams)), progress)
        } catch (error: Throwable) {
            streams.forEach { runCatching { it.close() } }
            throw if (error is IOException) error else IOException("Gagal membuka bagian runtime: ${error.message}", error)
        }
    }

    private fun verifyAssetChecksum(context: Context, assetPath: String, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        context.assets.open(assetPath).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expected, ignoreCase = true)) {
            throw IOException("Checksum bagian runtime tidak cocok: $assetPath")
        }
    }

    /** Shared transactional installer for APK assets, downloads, and user-selected files. */
    @JvmStatic
    @Throws(IOException::class)
    fun install(context: Context, input: java.io.InputStream, progress: Consumer<String>? = null) {
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
            input.use { source ->
                TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(source))).use { tar ->
                    var entry = tar.nextTarEntry
                    while (entry != null) {
                        entries++
                        if (entries > MAX_ENTRIES) throw IOException("Arsip memiliki terlalu banyak entri.")
                        val relative = safeRelativePath(entry.name)
                        if (relative != null) {
                            val entryName = relative.toString().replace(File.separatorChar, '/')
                            if (entryName == "rootfs/usr/bin/Xvfb" || entryName == "rootfs/usr/bin/xvfb-run" ||
                                entryName.startsWith("rootfs/tmp/.X11-unix/")) {
                                throw IOException("Xvfb executable atau X11 socket tidak boleh ada di runtime pack: $entryName")
                            }
                            val destination = stage.toPath().resolve(relative).normalize()
                            val root = stage.toPath().toAbsolutePath().normalize()
                            if (!destination.toAbsolutePath().normalize().startsWith(root)) {
                                throw IOException("Path arsip keluar dari direktori runtime.")
                            }
                            ensureSafeParents(root, destination.parent)

                            when {
                                entry.isDirectory -> {
                                    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS) &&
                                        !Files.isDirectory(destination, LinkOption.NOFOLLOW_LINKS)) {
                                        Files.delete(destination)
                                    }
                                    Files.createDirectories(destination)
                                }
                                entry.isSymbolicLink -> {
                                    val rawLink = entry.linkName
                                    val resolved = if (rawLink.startsWith("/")) {
                                        // Linux absolute symlinks inside Debian rootfs are guest-root
                                        // paths (for example /usr/bin/perl), not Android host paths.
                                        // Rebase them under this archive's rootfs directory.
                                        val rootfs = root.resolve("rootfs")
                                        if (!destination.startsWith(rootfs)) {
                                            throw IOException("Symlink absolut di luar rootfs ditolak: ${entry.name}")
                                        }
                                        rootfs.resolve(rawLink.removePrefix("/")).normalize().toAbsolutePath()
                                    } else {
                                        destination.parent.resolve(rawLink).normalize().toAbsolutePath()
                                    }
                                    if (!resolved.startsWith(root)) {
                                        throw IOException("Symlink keluar dari runtime ditolak: ${entry.name}")
                                    }
                                    val relativeLink = destination.parent.toAbsolutePath().normalize().relativize(resolved)
                                    replaceConflictingEntry(destination, resolved, symbolic = true)
                                    Files.createSymbolicLink(destination, relativeLink)
                                }
                                entry.isLink -> {
                                    val linkTarget = safeRelativePath(entry.linkName)
                                        ?: throw IOException("Hardlink tidak valid: ${entry.name}")
                                    val targetPath = root.resolve(linkTarget).normalize()
                                    if (!targetPath.startsWith(root) ||
                                        !Files.isRegularFile(targetPath, LinkOption.NOFOLLOW_LINKS)) {
                                        throw IOException("Target hardlink bukan file reguler yang sudah diekstrak: ${entry.linkName}")
                                    }
                                    // Tar files can contain repeated directory metadata / link
                                    // records. Treat an identical existing hardlink as already
                                    // extracted, and safely replace a conflicting non-directory
                                    // entry according to tar's last-entry-wins semantics.
                                    replaceConflictingEntry(destination, targetPath, symbolic = false)
                                    if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                                        Files.createLink(destination, targetPath)
                                    }
                                }
                                entry.isFile -> {
                                    if (entry.size < 0 || totalBytes + entry.size > MAX_EXPANDED_BYTES) {
                                        throw IOException("Ukuran runtime melewati batas 8 GiB.")
                                    }
                                    if (totalBytes + entry.size > availableBytes * 9 / 10) {
                                        throw IOException("Ruang penyimpanan tidak cukup untuk memasang runtime.")
                                    }
                                    // Never follow a symlink left by an earlier duplicate tar entry.
                                    // Replacing the leaf is safe only after rejecting directories;
                                    // parent components were checked by ensureSafeParents().
                                    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                                        if (Files.isDirectory(destination, LinkOption.NOFOLLOW_LINKS)) {
                                            throw IOException("Arsip mencoba mengganti direktori dengan file: $destination")
                                        }
                                        Files.delete(destination)
                                    }
                                    Files.newOutputStream(
                                        destination,
                                        StandardOpenOption.CREATE_NEW,
                                        StandardOpenOption.WRITE
                                    ).use { output ->
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
                                    applySafePermissions(destination.toFile(), executable)
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
            File(stage, ".runtime-install-complete").writeText("ghost-town-runtime-v1\\n")
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

    /** Executable files are immutable and executable only by the owning app UID. */
    private fun applySafePermissions(file: File, executable: Boolean) {
        if (!file.setReadable(true, true) && !file.canRead()) {
            throw IOException("Tidak dapat mengatur izin baca file runtime: ${file.name}")
        }
        if (executable) {
            if (!file.setWritable(false, true) && file.canWrite()) {
                throw IOException("Tidak dapat mengunci file executable runtime: ${file.name}")
            }
            if (!file.setExecutable(true, true) && !file.canExecute()) {
                throw IOException("Tidak dapat mengatur izin executable runtime: ${file.name}")
            }
        } else {
            file.setExecutable(false, true)
            if (!file.setWritable(true, true) && !file.canWrite()) {
                throw IOException("Tidak dapat mengatur izin tulis file runtime: ${file.name}")
            }
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

    private fun replaceConflictingEntry(destination: Path, target: Path, symbolic: Boolean) {
        if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) return
        if (!symbolic && Files.exists(target, LinkOption.NOFOLLOW_LINKS) &&
            Files.isSameFile(destination, target)) return
        if (Files.isDirectory(destination, LinkOption.NOFOLLOW_LINKS)) {
            throw IOException("Arsip mencoba mengganti direktori dengan link: $destination")
        }
        Files.delete(destination)
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

    /** Returns true only for a fully provisioned runtime from this installer. */
    @JvmStatic
    fun isInstalled(context: Context): Boolean {
        val root = File(context.filesDir, "runtime/wine")
        if (!File(root, ".runtime-install-complete").isFile) return false
        val version = File(root, ".runtime-version")
        if (!version.isFile || version.readText().trim() != BuildConfig.VERSION_NAME) return false
        val required = listOf(
            "rootfs/etc/os-release",
            "rootfs/usr/local/bin/box64",
            "rootfs/opt/wine/bin/wine",
            "rootfs/usr/lib/x86_64-linux-gnu/dri/virtio_gpu_dri.so",
            "rootfs/usr/bin/glxinfo",
            "rootfs/usr/bin/xmessage"
        )
        return required.all { File(root, it).isFile }
    }

    private fun validate(root: File) {
        val required = listOf(
            "rootfs/etc/os-release",
            "rootfs/usr/local/bin/box64",
            "rootfs/opt/wine/bin/wine",
            "rootfs/usr/lib/x86_64-linux-gnu/dri/virtio_gpu_dri.so",
            "rootfs/usr/bin/glxinfo",
            "rootfs/usr/bin/xmessage"
        )
        val missing = required.filterNot { File(root, it).isFile }
        if (missing.isNotEmpty()) throw IOException("Runtime tidak lengkap: ${missing.joinToString()}")
        listOf(
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
            "rootfs/usr/local/bin/box64",
            "rootfs/opt/wine/bin/wine",
            "rootfs/usr/lib/x86_64-linux-gnu/dri/virtio_gpu_dri.so",
            "rootfs/usr/bin/glxinfo",
            "rootfs/usr/bin/xmessage"
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
