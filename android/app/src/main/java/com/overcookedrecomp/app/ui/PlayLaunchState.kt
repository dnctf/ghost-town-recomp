package com.overcookedrecomp.app.ui

enum class PlayLaunchState {
    READY,
    BLOCKED_RUNTIME,
    BLOCKED_DISPLAY,
    PROCESSING
}

/** Pure state resolver shared by the dashboard and its unit tests. */
object PlayLaunchPolicy {
    fun resolve(runtimeInstalled: Boolean, displaySelfTestPassed: Boolean, processing: Boolean = false): PlayLaunchState {
        if (processing) return PlayLaunchState.PROCESSING
        if (!runtimeInstalled) return PlayLaunchState.BLOCKED_RUNTIME
        if (!displaySelfTestPassed) return PlayLaunchState.BLOCKED_DISPLAY
        return PlayLaunchState.READY
    }
}
