package com.overcookedrecomp.app.runtime

interface RuntimeSessionDriver {
    fun checkFiles(): Boolean
    fun initializeRuntime(): Boolean
    fun initializeGraphics(selection: GraphicsSelection): Boolean
    fun loadGame(executablePath: String): Boolean
    fun startSession(): Boolean
    fun stopSession(): Boolean
}

data class RuntimeStartResult(
    val started: Boolean,
    val state: GameSessionSnapshot,
    val reason: String? = null
)

/**
 * Serialized launch orchestration. A successful process/session start is not
 * equivalent to a rendered game: RUNNING is gated on an explicit renderer callback.
 */
class GameRuntimeController(
    private val machine: GameSessionStateMachine = GameSessionStateMachine(),
    private val driver: RuntimeSessionDriver,
    private val graphics: GraphicsSelection
) {
    @Synchronized
    fun current() = machine.current()

    @Synchronized
    fun start(game: GameInstallation, validation: ValidationReport): RuntimeStartResult {
        if (!validation.ready || validation.installationId != game.id) {
            return reject("Validation failed or mismatched game id.")
        }
        if (!move(GameSessionState.PREPARING_GAME, "Preparing game")) return reject()
        if (!move(GameSessionState.CHECKING_FILES, "Checking game files")) return reject()
        if (!driver.checkFiles()) return fail("Game file check failed.")
        if (!move(GameSessionState.INITIALIZING_RUNTIME, "Initializing runtime")) return reject()
        if (!driver.initializeRuntime()) return fail("Runtime initialization failed.")
        if (!move(GameSessionState.INITIALIZING_GPU, "Initializing graphics")) return reject()
        if (graphics.backend == GraphicsBackend.BLOCKED || !driver.initializeGraphics(graphics)) {
            return fail(graphics.reason)
        }
        if (!move(GameSessionState.LOADING_GAME, "Loading executable")) return reject()
        if (!driver.loadGame(game.executablePath)) return fail("Game loading failed.")
        if (!move(GameSessionState.STARTING_SESSION, "Starting session")) return reject()
        if (!driver.startSession()) return fail("Session start failed.")
        if (!move(GameSessionState.WAITING_FOR_FIRST_FRAME, "Session started; waiting for renderer first-frame confirmation")) {
            return reject()
        }
        // started=true means the driver accepted the session start. The game is
        // deliberately not marked RUNNING until confirmFirstFramePresented().
        return RuntimeStartResult(true, machine.current())
    }

    /** Call only from the actual game renderer after its first frame was presented. */
    @Synchronized
    fun confirmFirstFramePresented(): RuntimeStartResult {
        if (!move(GameSessionState.RUNNING, "First game frame presented")) {
            return reject("First-frame confirmation is not valid in the current state.")
        }
        return RuntimeStartResult(true, machine.current())
    }

    @Synchronized
    fun stop(): RuntimeStartResult {
        val state = machine.current().state
        if (state != GameSessionState.RUNNING && state != GameSessionState.WAITING_FOR_FIRST_FRAME) {
            return reject("Session is not active.")
        }
        if (!move(GameSessionState.STOPPING, "Stopping session")) return reject()
        if (!driver.stopSession()) return fail("Session stop failed.")
        move(GameSessionState.STOPPED, "Session stopped")
        return RuntimeStartResult(false, machine.current())
    }

    private fun fail(message: String): RuntimeStartResult {
        move(GameSessionState.FAILED, message)
        return RuntimeStartResult(false, machine.current(), message)
    }

    private fun move(state: GameSessionState, message: String) =
        machine.transition(state, message).accepted

    private fun reject(message: String = "Illegal lifecycle transition.") =
        RuntimeStartResult(false, machine.current(), message)
}
