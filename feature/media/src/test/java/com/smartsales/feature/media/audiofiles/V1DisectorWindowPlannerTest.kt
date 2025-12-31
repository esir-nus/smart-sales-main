// File: feature/media/src/test/java/com/smartsales/feature/media/audiofiles/V1DisectorWindowPlannerTest.kt
// Description: V1DisectorWindowPlanner unit tests (2025-12-30)
package com.smartsales.feature.media.audiofiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V1DisectorWindowPlannerTest {

    private val batchDurationMs = 600_000L
    private val overlapMs = 10_000L

    @Test
    fun `merges remainder smaller than seven minutes`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = batchDurationMs, overlapMs = overlapMs)

        val windows = planner.plan(audioDurationMs = 21 * 60_000L)

        assertEquals(2, windows.size)
        assertEquals(V1TranscriptionBatchWindow(1, 0, 600_000, overlapMs, 0), windows[0])
        assertEquals(V1TranscriptionBatchWindow(2, 600_000, 1_260_000, overlapMs, 590_000), windows[1])
    }

    @Test
    fun `merges remainder under seven minutes into last batch`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = batchDurationMs, overlapMs = overlapMs)

        val windows = planner.plan(audioDurationMs = 26 * 60_000L)

        assertEquals(2, windows.size)
        assertEquals(1_560_000, windows.last().absEndMs)
        assertEquals(590_000, windows[1].captureStartMs)
    }

    @Test
    fun `creates remainder batch when remainder is seven minutes or more`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = batchDurationMs, overlapMs = overlapMs)

        val windows = planner.plan(audioDurationMs = 27 * 60_000L)

        assertEquals(3, windows.size)
        assertEquals(V1TranscriptionBatchWindow(1, 0, 600_000, overlapMs, 0), windows[0])
        assertEquals(V1TranscriptionBatchWindow(2, 600_000, 1_200_000, overlapMs, 590_000), windows[1])
        assertEquals(V1TranscriptionBatchWindow(3, 1_200_000, 1_620_000, overlapMs, 1_190_000), windows[2])
    }

    @Test
    fun `returns empty when audio duration is non-positive`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = batchDurationMs, overlapMs = overlapMs)

        val windows = planner.plan(audioDurationMs = 0)

        assertTrue(windows.isEmpty())
    }

    @Test
    fun `clamps negative overlap to zero`() {
        val planner = V1DisectorWindowPlanner(batchDurationMs = batchDurationMs, overlapMs = -10_000)

        val windows = planner.plan(audioDurationMs = 26 * 60_000L)

        assertEquals(0, windows[1].overlapMs)
        assertEquals(windows[1].absStartMs, windows[1].captureStartMs)
    }

    @Test
    fun `throws when batch duration is non-positive`() {
        try {
            V1DisectorWindowPlanner(batchDurationMs = 0, overlapMs = overlapMs).plan(audioDurationMs = 1_000)
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
