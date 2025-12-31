package com.smartsales.feature.media.audiofiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V1WindowIndexedBatchReleasedBuilderTest {

    @Test
    fun `builds window indexed batch releases with safe fallback chunk`() {
        val windows = listOf(
            V1TranscriptionBatchWindow(1, 0, 1_000, 10_000, 0),
            V1TranscriptionBatchWindow(2, 1_000, 2_000, 10_000, 0),
            V1TranscriptionBatchWindow(3, 2_000, 3_000, 10_000, 0)
        )
        val timedSegments = listOf(
            V1TimedTextSegment(startMs = 0, endMs = 500, text = "a"),
            V1TimedTextSegment(startMs = 1_000, endMs = 1_500, text = "b")
        )
        val transcriptMarkdown = "full transcript"

        val releases = V1WindowIndexedBatchReleasedBuilder.build(
            jobId = "job-1",
            windows = windows,
            timedSegments = timedSegments,
            transcriptMarkdown = transcriptMarkdown,
            v1BatchPlanRule = "v1_windowed",
            v1BatchDurationMs = 600_000L,
            v1OverlapMs = 10_000L
        )

        assertEquals(3, releases.size)
        assertEquals(listOf(1, 2, 3), releases.map { it.batchIndex })
        assertEquals(listOf(3, 3, 3), releases.map { it.totalBatches })
        assertEquals(listOf(windows[0], windows[1], windows[2]), releases.map { it.v1Window })
        assertTrue(releases.all { it.timedSegments === timedSegments })
        assertEquals(transcriptMarkdown, releases.first().markdownChunk)
        assertTrue(releases.drop(1).all { it.markdownChunk.isEmpty() })
        assertEquals(listOf("v1_windowed", "v1_windowed", "v1_windowed"), releases.map { it.v1BatchPlanRule })
        assertEquals(listOf(600_000L, 600_000L, 600_000L), releases.map { it.v1BatchDurationMs })
        assertEquals(listOf(10_000L, 10_000L, 10_000L), releases.map { it.v1OverlapMs })
        assertEquals(listOf(3, 3, 3), releases.map { it.v1TotalBatches })
        assertEquals(listOf(1, 2, 3), releases.map { it.v1CurrentBatchIndex })
    }
}
