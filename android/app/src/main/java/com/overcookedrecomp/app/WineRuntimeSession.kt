package com.overcookedrecomp.app

import android.content.Context
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

/**
 * Owns the first real Wine guest bootstrap.
 *
 * This is deliberately not a game launcher: it creates an isolated 64-bit
 * prefix, installs the pinned DXVK DLLs into that prefix, and records every
 * command. Game execution remains blocked until a display backend is available.
 */
object WineRuntimeSession {
    private const val GUEST_HOME = "/home/ghosttown"
    private const val GUEST_PREFIX = "$GUEST_HOME/.wineprefixes/overcooked2"
    private const val COMMAND_TIMEOUT_SECONDS = 180L
    private val processLock = Any()

    @Volatile private var activeProcess: Process? = null

    data class Result(
        val success: Boolean,
        val message: String,
        val logPath: String
    )

    @JvmStatic
    fun prepareOvercooked2(context: Context): Result = synchronized(processLock) {
        val root = File(context.filesDir, "runtime/wine")
        if (!RuntimeBundleInstaller.isInstalled(context)) {
            return@synchronized Result(false, "Runtime bawaan belum terpasang atau checksum belum valid.", "")
        }

        val rootfs = File(root, "rootfs")
        val hostPrefix = File(context.filesDir, "wine-prefixes/overcooked2")
        val guestMount = File(rootfs, "home/ghosttown/.wineprefixes/overcooked2")
        val logDir = File(root, "logs/overcooked2")
        val sessionLog = File(logDir, "bootstrap.log")

        try {
            if (!hostPrefix.exists() && !hostPrefix.mkdirs()) {
                throw IOException("Tidak dapat membuat direktori Wine prefix.")
            }
            if (!guestMount.exists() && !guestMount.mkdirs()) {
                throw IOException("Tidak dapat membuat mount point Wine prefix.")
            }
            if (!logDir.exists() && !logDir.mkdirs()) {
                throw IOException("Tidak dapat membuat direktori log.")
            }

            sessionLog.appendText("\n=== Wine prefix bootstrap ===\n")
            sessionLog.appendText("Prefix host: ${hostPrefix.absolutePath}\n")
            sessionLog.appendText("Prefix guest: $GUEST_PREFIX\n")

            val systemReg = File(hostPrefix, "system.reg")
            if (!systemReg.isFile || systemReg.length() == 0L) {
                val boot = runGuest(
                    context, root, hostPrefix, logDir, "wineboot-init.log",
                    listOf("/opt/wine/bin/wineboot", "--init")
                )
                if (!boot.success) {
                    throw IOException("wineboot --init gagal (exit=${boot.exitCode ?: "timeout"}). ${boot.output.takeLast(1600)}")
                }
                if (!systemReg.isFile || systemReg.length() == 0L) {
                    throw IOException("wineboot selesai tetapi system.reg tidak terbentuk.")
                }
            } else {
                sessionLog.appendText("Wine prefix sudah ada; melewati wineboot --init.\n")
            }

            installDxvk(root, hostPrefix, logDir, context)
            val serverStop = stopWineServer(context, root, hostPrefix, logDir)
            if (!serverStop.success) sessionLog.appendText("WARNING: wineserver cleanup returned ${serverStop.exitCode ?: "timeout"}: ${serverStop.output.takeLast(500)}\n")
            sessionLog.appendText("Prefix siap dan DXVK DLL/registry berhasil dipasang.\n")
            Result(true, "Wine prefix 64-bit berhasil disiapkan dan DXVK sudah dipasang. Ini belum membuktikan Vulkan/D3D11 dapat tampil.", sessionLog.absolutePath)
        } catch (error: Throwable) {
            sessionLog.appendText("ERROR: ${error.javaClass.simpleName}: ${error.message}\n")
            Result(false, error.message ?: error.javaClass.simpleName, sessionLog.absolutePath)
        }
    }

    private fun installDxvk(root: File, hostPrefix: File, logDir: File, context: Context) {
        val dxvk = File(root, "rootfs/opt/dxvk/x64")
        val system32 = File(hostPrefix, "drive_c/windows/system32")
        if (!File(dxvk, "d3d11.dll").isFile || !File(dxvk, "dxgi.dll").isFile) {
            throw IOException("Payload DXVK x64 tidak lengkap.")
        }
        if (!system32.exists() && !system32.mkdirs()) {
            throw IOException("Direktori Windows system32 tidak dapat dibuat.")
        }

        listOf("d3d11.dll", "dxgi.dll", "d3d10core.dll").forEach { name ->
            val source = File(dxvk, name)
            if (source.isFile) {
                Files.copy(source.toPath(), File(system32, name).toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        val rootDir = File(context.filesDir, "runtime/wine")
        val regD3d11 = runGuest(
            context, rootDir, hostPrefix, logDir, "dxvk-reg-d3d11.log",
            listOf("/opt/wine/bin/wine", "reg", "add", "HKCU\\Software\\Wine\\DllOverrides", "/v", "d3d11", "/d", "native", "/f")
        )
        if (!regD3d11.success) throw IOException("Registrasi DXVK d3d11 gagal: ${regD3d11.output.takeLast(1000)}")

        val regDxgi = runGuest(
            context, rootDir, hostPrefix, logDir, "dxvk-reg-dxgi.log",
            listOf("/opt/wine/bin/wine", "reg", "add", "HKCU\\Software\\Wine\\DllOverrides", "/v", "dxgi", "/d", "native", "/f")
        )
        if (!regDxgi.success) throw IOException("Registrasi DXVK dxgi gagal: ${regDxgi.output.takeLast(1000)}")
    }

    private data class CommandResult(val success: Boolean, val exitCode: Int?, val output: String)

    private fun stopWineServer(context: Context, root: File, hostPrefix: File, logDir: File): CommandResult {
        return runGuest(
            context, root, hostPrefix, logDir, "wineserver-stop.log",
            listOf("/opt/wine/bin/wineserver", "-k")
        )
    }

    private fun runGuest(
        context: Context,
        root: File,
        hostPrefix: File,
        logDir: File,
        logName: String,
        guestCommand: List<String>
    ): CommandResult {
        val rootfs = File(root, "rootfs")
        val box64 = when {
            File(rootfs, "usr/local/bin/box64").isFile -> "/usr/local/bin/box64"
            File(rootfs, "usr/bin/box64").isFile -> "/usr/bin/box64"
            else -> throw IOException("Box64 tidak ditemukan di rootfs.")
        }
        val proot = File(root, "bin/proot")
        val loader = File(root, "libexec/proot/loader")
        val loader32 = File(root, "libexec/proot/loader32")
        if (!proot.canExecute() || !loader.isFile || !loader32.isFile) {
            throw IOException("PRoot atau companion loader tidak lengkap.")
        }

        val guestMount = File(rootfs, "home/ghosttown/.wineprefixes/overcooked2")
        if (!guestMount.exists() && !guestMount.mkdirs()) throw IOException("Mount point prefix guest gagal dibuat.")
        val guestExecutable = File(rootfs, guestCommand.first().removePrefix("/"))
        if (!guestExecutable.isFile) throw IOException("Program guest tidak ditemukan: ${guestCommand.first()}")
        val args = mutableListOf(
            proot.absolutePath, "-0", "-r", rootfs.absolutePath,
            "-b", "/dev", "-b", "/proc", "-b", "/sys",
            "-b", "${hostPrefix.absolutePath}:$GUEST_PREFIX",
            "-w", GUEST_HOME,
            box64
        )
        args.addAll(guestCommand)

        val log = File(logDir, logName)
        return try {
            val process = ProcessBuilder(args)
                .directory(root)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .apply {
                    val env = environment()
                    env["HOME"] = GUEST_HOME
                    env["USER"] = "ghosttown"
                    env["WINEPREFIX"] = GUEST_PREFIX
                    env["WINEARCH"] = "win64"
                    env["WINEDEBUG"] = "-all"
                    env["BOX64_LD_LIBRARY_PATH"] = "/usr/lib/x86_64-linux-gnu:/opt/wine/lib64:/opt/wine/lib"
                    env["PROOT_LOADER"] = loader.absolutePath
                    env["PROOT_LOADER_32"] = loader32.absolutePath
                    env["PROOT_NO_SECCOMP"] = "1"
                    env["PATH"] = "/usr/local/bin:/usr/bin:/bin"
                    env.remove("DISPLAY")
                    env.remove("WAYLAND_DISPLAY")
                }
                .start()
            activeProcess = process
            val finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
                if (process.isAlive) process.destroyForcibly()
                return CommandResult(false, null, "Timeout setelah ${COMMAND_TIMEOUT_SECONDS}s. Log: ${log.absolutePath}")
            }
            val output = if (log.isFile) log.readText().takeLast(6000) else ""
            CommandResult(process.exitValue() == 0, process.exitValue(), output)
        } catch (error: Throwable) {
            CommandResult(false, null, "${error.javaClass.simpleName}: ${error.message}")
        } finally {
            activeProcess = null
        }
    }

    /** Stops the currently supervised bootstrap process, if any. */
    @JvmStatic
    fun stop() {
        activeProcess?.let { process ->
            process.destroy()
            if (process.isAlive) process.destroyForcibly()
        }
        activeProcess = null
    }
}
