package com.smartsales.prism.ui.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimArtifactContentTest {

    @Test
    fun `line threshold only marks long transcript after line 4`() {
        assertFalse(
            hasExceededTranscriptCollapseThreshold(
                renderedLineCount = 4,
                collapseAfterRenderedLines = 4
            )
        )
        assertTrue(
            hasExceededTranscriptCollapseThreshold(
                renderedLineCount = 5,
                collapseAfterRenderedLines = 4
            )
        )
    }

    @Test
    fun `remaining dwell waits until readable reveal window is satisfied`() {
        assertEquals(
            400L,
            remainingTranscriptRevealDwellMillis(
                revealStartedAtMillis = 100L,
                nowMillis = 700L,
                minRevealMillis = 1000L
            )
        )
        assertEquals(
            0L,
            remainingTranscriptRevealDwellMillis(
                revealStartedAtMillis = 100L,
                nowMillis = 1200L,
                minRevealMillis = 1000L
            )
        )
    }
}
