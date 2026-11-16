package com.overcookedrecomp.app

import java.io.File

/**
 * Architectural boundary for an optional external Wine + Box64 runtime.
 *
 * This class is intentionally inactive: this repository does not package Wine,
 * Box64/Box86, a Linux userspace, DXVK, or VKD3D-Proton. Do not expose a working
 * "Run EXE" action until a separately versioned backend is installed and its
 * process, ABI, graphics, security, and licensing gates have passed.
 *
 * This stub never starts a process and never executes user-provided commands.
 */
object WineRuntimeBridge {

    enum class Availability {
        AVAILABLE,
        NOT_INSTALLED,
        UNSUPPORTED
    }

    data class RuntimeStatus(
        val availability: Availability,
        val message: String
    )

    sealed class LaunchResult {
        data class Rejected(val reason: String) : LaunchResult()
    }

    /**
     * Reports the current repository state. A future external runtime adapter
     * may replace this implementation after installation and validation.
     */
    @JvmStatic
    fun probe(): RuntimeStatus = RuntimeStatus(
        availability = Availability.NOT_INSTALLED,
        message = "Wine/Box64 runtime belum terpasang atau terintegrasi."
    )

    /**
     * Safe placeholder: deliberately refuses execution. It does not invoke
     * Runtime.exec, ProcessBuilder, a shell, Termux, or another application.
     */
    @JvmStatic
    fun requestLaunch(executable: File): LaunchResult.Rejected {
        val detail = if (executable.isFile) {
            "Executable terdeteksi, tetapi backend Wine/Box64 belum tersedia."
        } else {
            "File executable tidak ditemukan."
        }
        return LaunchResult.Rejected(detail)
    }
}
