package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
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

    @Test
    fun `resolveSimArtifactKeywords prefers normalized artifact keywords`() {
        val artifacts = TingwuJobArtifacts(
            keywords = listOf("需求变更", "交互设计", "高优")
        )

        assertEquals(
            listOf("需求变更", "交互设计", "高优"),
            resolveSimArtifactKeywords(artifacts)
        )
    }

    @Test
    fun `resolveSimArtifactKeywords falls back to MeetingAssistance raw payload`() {
        val artifacts = TingwuJobArtifacts(
            meetingAssistanceRaw = """
                {
                  "MeetingAssistance": {
                    "Keywords": ["需求变更", "交互设计", "高优"]
                  }
                }
            """.trimIndent()
        )

        assertEquals(
            listOf("需求变更", "交互设计", "高优"),
            resolveSimArtifactKeywords(artifacts)
        )
    }
}
