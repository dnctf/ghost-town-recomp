package com.overcookedrecomp.app.ui

import android.content.Context
import com.overcookedrecomp.app.RuntimeBundleInstaller
import java.io.File

/**
 * Single UI launch gate. The display marker may only be written by a successful
 * end-to-end display self-test. It is intentionally absent in this preview.
 */
object RuntimeLaunchGate {
    const val DISPLAY_READY_ENV = "GHOST_TOWN_DISPLAY_READY"
    private const val DISPLAY_MARKER = "runtime/display/GHOST_TOWN_DISPLAY_READY"

    data class Snapshot(
        val runtimeInstalled: Boolean,
        val displaySelfTestPassed: Boolean,
        val displayMarkerPath: String
    ) {
        val canLaunch: Boolean get() = runtimeInstalled && displaySelfTestPassed
    }

    fun read(context: Context): Snapshot {
        val marker = File(context.filesDir, DISPLAY_MARKER)
        val displayReady = marker.isFile && marker.readText().trim() == "1"
        return Snapshot(
            runtimeInstalled = runCatching { RuntimeBundleInstaller.isInstalled(context) }.getOrDefault(false),
            displaySelfTestPassed = displayReady,
            displayMarkerPath = marker.absolutePath
        )
    }
}
