package com.smartsales.aitest.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V1WindowKeyTest {
    @Test
    fun buildWindowKey_isDeterministicAndSafe() {
        val key = V1WindowKey.build(
            sessionIdOrJobId = "session-01/alpha beta",
            batchIndex = 2,
            requestedCaptureStartMs = 1000L,
            captureEndMs = 2000L
        )
        assertEquals("rs_session-01_alpha_beta_b2_1000_2000", key)
        assertTrue(key.none { it.isWhitespace() })
    }

    @Test
    fun anchorToRecordingOrigin_offsetsByActualCaptureStart() {
        val relative = listOf(
            RelativeTimedSegment(startMs = 0L, endMs = 500L, text = "a"),
            RelativeTimedSegment(startMs = 700L, endMs = 900L, text = "b")
        )
        val anchored = V1SegmentAnchoring.anchorToRecordingOrigin(
            actualCaptureStartMs = 1000L,
            relativeSegments = relative
        )
        assertEquals(2, anchored.size)
        assertEquals(1000L, anchored[0].startMs)
        assertEquals(1500L, anchored[0].endMs)
        assertEquals(1700L, anchored[1].startMs)
        assertEquals(1900L, anchored[1].endMs)
    }
}
