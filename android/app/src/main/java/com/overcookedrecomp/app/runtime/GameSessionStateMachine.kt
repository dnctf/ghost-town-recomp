package com.overcookedrecomp.app.runtime

/**
 * Platform-independent lifecycle model for a dedicated game session.
 * Runtime adapters must report progress through validated transitions.
 */
enum class GameSessionState {
    IDLE, PREPARING_GAME, CHECKING_FILES, INITIALIZING_RUNTIME,
    INITIALIZING_GPU, LOADING_GAME, STARTING_SESSION, RUNNING,
    STOPPING, STOPPED, FAILED, CRASHED
}

data class GameSessionSnapshot(
    val state: GameSessionState,
    val message: String? = null,
    val failureStage: GameSessionState? = null
) {
    val isActive: Boolean get() = state in ACTIVE_STATES
    val canRetry: Boolean get() = state in RETRY_STATES

    companion object {
        private val ACTIVE_STATES = setOf(
            GameSessionState.PREPARING_GAME, GameSessionState.CHECKING_FILES,
            GameSessionState.INITIALIZING_RUNTIME, GameSessionState.INITIALIZING_GPU,
            GameSessionState.LOADING_GAME, GameSessionState.STARTING_SESSION,
            GameSessionState.RUNNING, GameSessionState.STOPPING
        )
        private val RETRY_STATES = setOf(
            GameSessionState.STOPPED, GameSessionState.FAILED, GameSessionState.CRASHED
        )
    }
}

data class GameSessionTransitionResult(
    val accepted: Boolean,
    val snapshot: GameSessionSnapshot,
    val reason: String? = null
)

/**
 * Thread-safe state reducer. It validates lifecycle transitions but does not launch
 * processes, create surfaces, or initialize graphics; those remain runtime adapters.
 */
class GameSessionStateMachine(initial: GameSessionState = GameSessionState.IDLE) {
    private val lock = Any()
    private var snapshot = GameSessionSnapshot(initial)

    fun current(): GameSessionSnapshot = synchronized(lock) { snapshot }

    fun transition(next: GameSessionState, message: String? = null): GameSessionTransitionResult =
        synchronized(lock) {
            if (next !in allowedFrom(snapshot.state)) {
                return@synchronized GameSessionTransitionResult(
                    false, snapshot, "Illegal transition: ${snapshot.state} -> $next"
                )
            }
            val previous = snapshot.state
            snapshot = GameSessionSnapshot(
                state = next,
                message = message,
                failureStage = if (next == GameSessionState.FAILED || next == GameSessionState.CRASHED) previous else null
            )
            GameSessionTransitionResult(true, snapshot)
        }

    private fun allowedFrom(state: GameSessionState): Set<GameSessionState> = when (state) {
        GameSessionState.IDLE -> setOf(GameSessionState.PREPARING_GAME)
        GameSessionState.PREPARING_GAME -> setOf(GameSessionState.CHECKING_FILES, GameSessionState.FAILED, GameSessionState.STOPPING)
        GameSessionState.CHECKING_FILES -> setOf(GameSessionState.INITIALIZING_RUNTIME, GameSessionState.FAILED, GameSessionState.STOPPING)
        GameSessionState.INITIALIZING_RUNTIME -> setOf(GameSessionState.INITIALIZING_GPU, GameSessionState.FAILED, GameSessionState.STOPPING)
        GameSessionState.INITIALIZING_GPU -> setOf(GameSessionState.LOADING_GAME, GameSessionState.FAILED, GameSessionState.STOPPING)
        GameSessionState.LOADING_GAME -> setOf(GameSessionState.STARTING_SESSION, GameSessionState.FAILED, GameSessionState.STOPPING)
        GameSessionState.STARTING_SESSION -> setOf(GameSessionState.RUNNING, GameSessionState.FAILED, GameSessionState.CRASHED, GameSessionState.STOPPING)
        GameSessionState.RUNNING -> setOf(GameSessionState.CRASHED, GameSessionState.STOPPING)
        GameSessionState.STOPPING -> setOf(GameSessionState.STOPPED, GameSessionState.FAILED)
        GameSessionState.STOPPED, GameSessionState.FAILED, GameSessionState.CRASHED -> setOf(GameSessionState.PREPARING_GAME)
    }
}
