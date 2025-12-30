// File: feature/media/src/test/java/com/smartsales/feature/media/audiofiles/V1DisectorWindowPlannerTest.kt
// Description: V1DisectorWindowPlanner unit tests (2025-12-30)
package com.smartsales.feature.media.audiofiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V1DisectorWindowPlannerTest {

    @Test
    fun `plans windows with overlap pre-roll`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = 1_000, overlapMs = 200)

        val windows = planner.plan(audioDurationMs = 2_500)

        assertEquals(3, windows.size)
        assertEquals(V1TranscriptionBatchWindow(1, 0, 1_000, 200, 0), windows[0])
        assertEquals(V1TranscriptionBatchWindow(2, 1_000, 2_000, 200, 800), windows[1])
        assertEquals(V1TranscriptionBatchWindow(3, 2_000, 2_500, 200, 1_800), windows[2])
    }

    @Test
    fun `clamps captureStartMs to zero when overlap exceeds absStart`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = 1_000, overlapMs = 1_500)

        val windows = planner.plan(audioDurationMs = 1_500)

        assertEquals(2, windows.size)
        assertEquals(0, windows[1].captureStartMs)
    }

    @Test
    fun `handles exact multiple duration`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = 1_000, overlapMs = 0)

        val windows = planner.plan(audioDurationMs = 3_000)

        assertEquals(3, windows.size)
        assertEquals(3_000, windows.last().absEndMs)
        assertEquals(2_000, windows.last().absStartMs)
    }

    @Test
    fun `returns empty when audio duration is non-positive`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = 1_000, overlapMs = 200)

        val windows = planner.plan(audioDurationMs = 0)

        assertTrue(windows.isEmpty())
    }

    @Test
    fun `clamps negative overlap to zero`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = 1_000, overlapMs = -50)

        val windows = planner.plan(audioDurationMs = 1_500)

        assertEquals(0, windows[1].overlapMs)
        assertEquals(windows[1].absStartMs, windows[1].captureStartMs)
    }

    @Test
    fun `throws when batch duration is non-positive`() {
        try {
            V1DisectorWindowPlanner(batchDurationMs = 0, overlapMs = 200).plan(audioDurationMs = 1_000)
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
