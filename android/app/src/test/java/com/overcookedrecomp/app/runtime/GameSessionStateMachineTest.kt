package com.overcookedrecomp.app.runtime

import org.junit.Assert.*
import org.junit.Test

class GameSessionStateMachineTest {
    @Test fun acceptsExpectedLaunchSequence() {
        val machine = GameSessionStateMachine()
        listOf(GameSessionState.PREPARING_GAME, GameSessionState.CHECKING_FILES,
            GameSessionState.INITIALIZING_RUNTIME, GameSessionState.INITIALIZING_GPU,
            GameSessionState.LOADING_GAME, GameSessionState.STARTING_SESSION,
            GameSessionState.RUNNING).forEach { assertTrue(machine.transition(it).accepted) }
        assertEquals(GameSessionState.RUNNING, machine.current().state)
    }

    @Test fun rejectsSkippedStages() {
        val machine = GameSessionStateMachine()
        assertTrue(machine.transition(GameSessionState.PREPARING_GAME).accepted)
        assertFalse(machine.transition(GameSessionState.RUNNING).accepted)
        assertEquals(GameSessionState.PREPARING_GAME, machine.current().state)
    }

    @Test fun failureTracksStageAndSupportsRetry() {
        val machine = GameSessionStateMachine()
        machine.transition(GameSessionState.PREPARING_GAME)
        machine.transition(GameSessionState.CHECKING_FILES)
        val failure = machine.transition(GameSessionState.FAILED, "Missing files")
        assertTrue(failure.accepted)
        assertEquals(GameSessionState.CHECKING_FILES, failure.snapshot.failureStage)
        assertTrue(failure.snapshot.canRetry)
        assertTrue(machine.transition(GameSessionState.PREPARING_GAME).accepted)
    }

    @Test fun crashFromRunningSupportsRetry() {
        val machine = GameSessionStateMachine()
        listOf(GameSessionState.PREPARING_GAME, GameSessionState.CHECKING_FILES,
            GameSessionState.INITIALIZING_RUNTIME, GameSessionState.INITIALIZING_GPU,
            GameSessionState.LOADING_GAME, GameSessionState.STARTING_SESSION,
            GameSessionState.RUNNING, GameSessionState.CRASHED).forEach {
            assertTrue("Transition to $it", machine.transition(it).accepted)
        }
        assertEquals(GameSessionState.RUNNING, machine.current().failureStage)
        assertTrue(machine.transition(GameSessionState.PREPARING_GAME).accepted)
    }

    @Test fun stopMustPassThroughStopping() {
        val machine = GameSessionStateMachine()
        machine.transition(GameSessionState.PREPARING_GAME)
        assertFalse(machine.transition(GameSessionState.STOPPED).accepted)
        assertTrue(machine.transition(GameSessionState.STOPPING).accepted)
        assertTrue(machine.transition(GameSessionState.STOPPED).accepted)
    }

    @Test fun idleCannotRunOrCrash() {
        val machine = GameSessionStateMachine()
        assertFalse(machine.transition(GameSessionState.RUNNING).accepted)
        assertFalse(machine.transition(GameSessionState.CRASHED).accepted)
        assertEquals(GameSessionState.IDLE, machine.current().state)
    }
}
