package com.smartsales.prism.ui.sim

import com.smartsales.core.telemetry.PipelineValve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimShellHandoffTest {

    @Test
    fun `emitSchedulerShelfHandoffTelemetry emits request summary and log`() {
        val checkpoints = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        val logs = mutableListOf<String>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            checkpoints += checkpoint to summary
        }

        try {
            emitSchedulerShelfHandoffTelemetry("i want to learn guitar") { message ->
                logs += message
            }

            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_SCHEDULER_SHELF_HANDOFF_REQUEST_SUMMARY
                )
            )
            assertEquals(
                listOf("scheduler shelf handoff requested: i want to learn guitar"),
                logs
            )
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `handleSchedulerShelfAskAiHandoff starts session and closes drawer`() {
        val emittedPrompts = mutableListOf<String>()
        val startedPrompts = mutableListOf<String>()
        var closeCount = 0

        handleSchedulerShelfAskAiHandoff(
            promptText = "i want to learn guitar",
            startSession = { startedPrompts += it },
            closeDrawer = { closeCount += 1 },
            emitTelemetry = { emittedPrompts += it }
        )

        assertEquals(listOf("i want to learn guitar"), emittedPrompts)
        assertEquals(listOf("i want to learn guitar"), startedPrompts)
        assertEquals(1, closeCount)
    }

    @Test
    fun `handleSchedulerShelfAskAiHandoff ignores blank prompt`() {
        var started = false
        var closed = false
        var emitted = false

        handleSchedulerShelfAskAiHandoff(
            promptText = "   ",
            startSession = { started = true },
            closeDrawer = { closed = true },
            emitTelemetry = { emitted = true }
        )

        assertFalse(emitted)
        assertFalse(started)
        assertFalse(closed)
    }
}
