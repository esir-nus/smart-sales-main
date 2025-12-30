// File: app/src/test/java/com/smartsales/aitest/audio/DefaultAudioTranscriptionCoordinatorV1WindowsTest.kt
// Description: DefaultAudioTranscriptionCoordinator window plan tests (2025-12-30)
package com.smartsales.aitest.audio

import com.smartsales.feature.media.audiofiles.V1DisectorWindowPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultAudioTranscriptionCoordinatorV1WindowsTest {

    @Test
    fun `buildBatchPlanWithWindows returns null without duration`() {
        val result = buildBatchPlanWithWindowsForTingwu(
            markdown = "line1\nline2",
            audioDurationMs = null,
            batchDurationMs = 1_000L,
            overlapMs = 200L
        )

        assertNull(result)
    }

    @Test
    fun `buildBatchPlanWithWindows returns windows when duration exists`() {
        val durationMs = 2_500L
        val batchDurationMs = 1_000L
        val overlapMs = 200L
        val expectedWindows = V1DisectorWindowPlanner(
            batchDurationMs = batchDurationMs,
            overlapMs = overlapMs
        ).plan(durationMs)

        val result = buildBatchPlanWithWindowsForTingwu(
            markdown = "line1\nline2\nline3",
            audioDurationMs = durationMs,
            batchDurationMs = batchDurationMs,
            overlapMs = overlapMs
        )

        assertEquals(expectedWindows, result?.windows)
    }
}
