// File: feature/media/src/test/java/com/smartsales/feature/media/audiofiles/TranscriptionBatchPlannerV1WindowsTest.kt
// Description: TranscriptionBatchPlanner V1 window plan tests (2025-12-30)
package com.smartsales.feature.media.audiofiles

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptionBatchPlannerV1WindowsTest {

    @Test
    fun `planWithWindows returns deterministic window plan`() {
        val markdown = "line1\nline2\nline3"
        val audioDurationMs = 2_500L
        val batchDurationMs = 1_000L
        val overlapMs = 200L

        val expectedWindows = V1DisectorWindowPlanner(
            batchDurationMs = batchDurationMs,
            overlapMs = overlapMs
        ).plan(audioDurationMs)

        val result = TranscriptionBatchPlanner.planWithWindows(
            markdown = markdown,
            audioDurationMs = audioDurationMs,
            batchDurationMs = batchDurationMs,
            overlapMs = overlapMs,
            batchSize = 2
        )

        assertEquals(expectedWindows, result.windows)
        assertEquals(2, result.plan.batchSize)
        assertEquals(2, result.plan.totalBatches)
    }
}
