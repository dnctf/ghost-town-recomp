package com.overcookedrecomp.app

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Optional Wine/Box64 adapter.
 *
 * The Android APK does not embed the Linux runtime. A runtime must be provisioned
 * under filesDir/runtime/wine using the layout documented in
 * runtime/ANDROID_RUNTIME_LAYOUT.md. This adapter never invokes a shell and
 * never accepts arbitrary command strings.
 */
object WineRuntimeBridge {
    enum class State {
        NOT_INSTALLED,
        INCOMPLETE,
        READY_FOR_HEADLESS_PROBE,
        UNSUPPORTED
    }

    data class RuntimeStatus(
        val state: State,
        val message: String,
        val missing: List<String>,
        val runtimeRoot: String
    )

    data class ProbeResult(
        val success: Boolean,
        val output: String,
        val exitCode: Int?
    )

    private data class Layout(
        val root: File,
        val proot: File,
        val prootLoader: File,
        val rootfs: File,
        val box64: String,
        val wine: String,
        val dxvk: File
    )

    private fun layout(context: Context): Layout {
        val root = File(context.filesDir, "runtime/wine")
        val rootfs = File(root, "rootfs")
        val box64Candidates = listOf(
            "usr/local/bin/box64",
            "usr/bin/box64"
        )
        val box64 = box64Candidates.firstOrNull { File(rootfs, it).isFile }
            ?: box64Candidates.first()
        return Layout(
            root = root,
            proot = File(root, "bin/proot"),
            prootLoader = File(root, "libexec/proot/loader"),
            rootfs = rootfs,
            box64 = "/$box64",
            wine = "/opt/wine/bin/wine",
            dxvk = File(rootfs, "opt/dxvk")
        )
    }

    @JvmStatic
    fun probe(context: Context): RuntimeStatus {
        val l = layout(context)
        if (!l.root.exists()) {
            return RuntimeStatus(
                State.NOT_INSTALLED,
                "Wine/Box64 runtime belum dipasang.",
                listOf("Android ARM64 PRoot + loader", "glibc rootfs ARM64", "Box64 ARM64", "Wine x86_64", "DXVK DLL"),
                l.root.absolutePath
            )
        }

        val missing = mutableListOf<String>()
        if (!l.proot.isFile || !l.proot.canExecute()) missing += "bin/proot (executable Android ARM64)"
        if (!l.prootLoader.isFile) missing += "libexec/proot/loader (PRoot AArch64 companion loader)"
        if (!File(l.root, "libexec/proot/loader32").isFile) missing += "libexec/proot/loader32 (PRoot ARM32 companion loader)"
        if (!File(l.rootfs, "etc/os-release").isFile) missing += "rootfs/etc/os-release (glibc Linux userspace)"
        if (!File(l.rootfs, l.box64.removePrefix("/")).isFile) missing += "Box64 ARM64/glibc"
        if (!File(l.rootfs, l.wine.removePrefix("/")).isFile) missing += "Wine x86_64"
        if (!File(l.dxvk, "x64/dxgi.dll").isFile || !File(l.dxvk, "x64/d3d11.dll").isFile) {
            missing += "DXVK x64 DLL payload"
        }

        return if (missing.isEmpty()) {
            RuntimeStatus(
                State.READY_FOR_HEADLESS_PROBE,
                "Komponen dasar ditemukan. Ini belum membuktikan DXVK/Vulkan atau game dapat berjalan.",
                emptyList(),
                l.root.absolutePath
            )
        } else {
            RuntimeStatus(
                State.INCOMPLETE,
                "Runtime belum lengkap; peluncuran game tetap dinonaktifkan.",
                missing,
                l.root.absolutePath
            )
        }
    }

    /**
     * Runs only the fixed Wine version probe. It is intentionally headless and
     * does not launch a game or enable a graphics session.
     */
    @JvmStatic
    fun runWineVersionProbe(context: Context): ProbeResult {
        val status = probe(context)
        if (status.state != State.READY_FOR_HEADLESS_PROBE) {
            return ProbeResult(false, status.missing.joinToString("\n"), null)
        }

        val l = layout(context)
        val log = File(l.root, "wine-version-probe.log")
        return try {
            val command = listOf(
                l.proot.absolutePath,
                "-0",
                "-r", l.rootfs.absolutePath,
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-w", "/tmp",
                l.box64,
                l.wine,
                "--version"
            )
            val process = ProcessBuilder(command)
                .directory(l.root)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .apply {
                    environment()["HOME"] = "/root"
                    environment()["BOX64_LD_LIBRARY_PATH"] =
                        "/usr/lib/x86_64-linux-gnu:/opt/wine/lib64:/opt/wine/lib"
                    environment()["PROOT_LOADER"] = l.prootLoader.absolutePath
                    environment()["PROOT_LOADER_32"] = File(l.root, "libexec/proot/loader32").absolutePath
                    environment()["PROOT_NO_SECCOMP"] = "1"
                }
                .start()

            if (!process.waitFor(45, TimeUnit.SECONDS)) {
                process.destroy()
                if (process.isAlive) process.destroyForcibly()
                ProbeResult(false, "Wine version probe timeout (45s). Log: ${log.absolutePath}", null)
            } else {
                val output = if (log.isFile) log.readText().takeLast(12000) else ""
                ProbeResult(process.exitValue() == 0, output, process.exitValue())
            }
        } catch (t: Throwable) {
            ProbeResult(false, "${t.javaClass.simpleName}: ${t.message}", null)
        }
    }
}
