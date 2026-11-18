package com.overcookedrecomp.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayLaunchPolicyTest {
    @Test fun playIsReadyOnlyWhenRuntimeAndDisplayPass() {
        assertEquals(PlayLaunchState.READY, PlayLaunchPolicy.resolve(true, true))
    }

    @Test fun playIsBlockedWhenRuntimeIsMissing() {
        assertEquals(PlayLaunchState.BLOCKED_RUNTIME, PlayLaunchPolicy.resolve(false, true))
    }

    @Test fun playIsBlockedWhenDisplaySelfTestHasNotPassed() {
        assertEquals(PlayLaunchState.BLOCKED_DISPLAY, PlayLaunchPolicy.resolve(true, false))
    }

    @Test fun processingStateIsExplicit() {
        assertEquals(PlayLaunchState.PROCESSING, PlayLaunchPolicy.resolve(true, true, processing = true))
    }
}
