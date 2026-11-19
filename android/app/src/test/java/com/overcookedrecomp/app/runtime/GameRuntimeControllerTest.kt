package com.overcookedrecomp.app.runtime
import org.junit.Assert.*
import org.junit.Test
private class FakeRuntimeDriver(var failAt:String?=null):RuntimeSessionDriver{
 override fun checkFiles()=failAt!="files"
 override fun initializeRuntime()=failAt!="runtime"
 override fun initializeGraphics(selection:GraphicsSelection)=failAt!="gpu"
 override fun loadGame(executablePath:String)=failAt!="load"
 override fun startSession()=failAt!="start"
 override fun stopSession()=failAt!="stop"
}
class GameRuntimeControllerTest{
 private val game=GameInstallation("g","Game","/g","/g/game.exe","/g/Data","x86_64")
 private val report=ValidationReport("g",ValidationCode.READY,"ok","hash")
 private val graphics=GraphicsSelection(GraphicsBackend.WINED3D,"fallback",false)
 @Test fun successfulDriverAdvancesToRunningAndStops(){val c=GameRuntimeController(driver=FakeRuntimeDriver(),graphics=graphics);assertTrue(c.start(game,report).started);assertEquals(GameSessionState.RUNNING,c.current().state);assertFalse(c.stop().started);assertEquals(GameSessionState.STOPPED,c.current().state)}
 @Test fun failedDriverStopsAtFailureAndAllowsRetry(){val d=FakeRuntimeDriver("gpu");val c=GameRuntimeController(driver=d,graphics=graphics);assertFalse(c.start(game,report).started);assertEquals(GameSessionState.FAILED,c.current().state);assertEquals(GameSessionState.INITIALIZING_GPU,c.current().failureStage);d.failAt=null;assertTrue(c.start(game,report).started);assertEquals(GameSessionState.RUNNING,c.current().state)}
}