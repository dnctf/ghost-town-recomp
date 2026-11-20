package com.overcookedrecomp.app.runtime

import org.junit.Assert.*
import org.junit.Test

private class FakeRuntimeDriver(var failAt: String? = null) : RuntimeSessionDriver {
    override fun checkFiles() = failAt != "files"
    override fun initializeRuntime() = failAt != "runtime"
    override fun initializeGraphics(selection: GraphicsSelection) = failAt != "gpu"
    override fun loadGame(executablePath: String) = failAt != "load"
    override fun startSession() = failAt != "start"
    override fun stopSession() = failAt != "stop"
}

class GameRuntimeControllerTest {
    private val game = GameInstallation("g", "Game", "/g", "/g/game.exe", "/g/Data", "x86_64")
    private val report = ValidationReport("g", ValidationCode.READY, "ok", "hash")
    private val graphics = GraphicsSelection(GraphicsBackend.WINED3D, "fallback", false)

    @Test fun successfulDriverWaitsForFirstFrameBeforeRunning() {
        val controller = GameRuntimeController(driver = FakeRuntimeDriver(), graphics = graphics)
        assertTrue(controller.start(game, report).started)
        assertEquals(GameSessionState.WAITING_FOR_FIRST_FRAME, controller.current().state)
        assertTrue(controller.confirmFirstFramePresented().started)
        assertEquals(GameSessionState.RUNNING, controller.current().state)
        assertFalse(controller.stop().started)
        assertEquals(GameSessionState.STOPPED, controller.current().state)
    }

    @Test fun firstFrameConfirmationCannotSkipLaunch() {
        val controller = GameRuntimeController(driver = FakeRuntimeDriver(), graphics = graphics)
        assertFalse(controller.confirmFirstFramePresented().started)
        assertEquals(GameSessionState.IDLE, controller.current().state)
    }

    @Test fun stopIsAllowedBeforeFirstFrame() {
        val controller = GameRuntimeController(driver = FakeRuntimeDriver(), graphics = graphics)
        assertTrue(controller.start(game, report).started)
        assertEquals(GameSessionState.WAITING_FOR_FIRST_FRAME, controller.current().state)
        assertFalse(controller.stop().started)
        assertEquals(GameSessionState.STOPPED, controller.current().state)
    }

    @Test fun failedDriverStopsAtFailureAndAllowsRetry() {
        val driver = FakeRuntimeDriver("gpu")
        val controller = GameRuntimeController(driver = driver, graphics = graphics)
        assertFalse(controller.start(game, report).started)
        assertEquals(GameSessionState.FAILED, controller.current().state)
        assertEquals(GameSessionState.INITIALIZING_GPU, controller.current().failureStage)
        driver.failAt = null
        assertTrue(controller.start(game, report).started)
        assertEquals(GameSessionState.WAITING_FOR_FIRST_FRAME, controller.current().state)
    }
}
