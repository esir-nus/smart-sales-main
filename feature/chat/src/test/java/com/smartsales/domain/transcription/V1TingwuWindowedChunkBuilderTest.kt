package com.smartsales.domain.transcription

import com.smartsales.feature.media.audiofiles.V1TimedTextSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class V1TingwuWindowedChunkBuilderTest {

    @Test
    fun `half-open boundaries are excluded`() {
        val segments = listOf(
            V1TimedTextSegment(startMs = 0, endMs = 1000, text = "A"),
            V1TimedTextSegment(startMs = 2000, endMs = 3000, text = "B")
        )

        val result = V1TingwuWindowedChunkBuilder.buildWindowedMarkdownChunkResult(
            absStartMs = 1000,
            absEndMs = 2000,
            timedSegments = segments
        )

        assertEquals("", result.chunk)
        assertEquals(2, result.segmentsInCount)
        assertEquals(0, result.segmentsOutCount)
    }

    @Test
    fun `partial overlap is included`() {
        val segments = listOf(
            V1TimedTextSegment(startMs = 900, endMs = 1100, text = "X")
        )

        val result = V1TingwuWindowedChunkBuilder.buildWindowedMarkdownChunkResult(
            absStartMs = 1000,
            absEndMs = 2000,
            timedSegments = segments
        )

        assertEquals("X", result.chunk)
        assertEquals(1, result.segmentsInCount)
        assertEquals(1, result.segmentsOutCount)
    }

    @Test
    fun `invalid window returns empty`() {
        val segments = listOf(
            V1TimedTextSegment(startMs = 100, endMs = 200, text = "A")
        )

        val result = V1TingwuWindowedChunkBuilder.buildWindowedMarkdownChunkResult(
            absStartMs = 2000,
            absEndMs = 2000,
            timedSegments = segments
        )

        assertEquals("", result.chunk)
        assertEquals(1, result.segmentsInCount)
        assertEquals(0, result.segmentsOutCount)
    }

    @Test
    fun `invalid segment is skipped`() {
        val segments = listOf(
            V1TimedTextSegment(startMs = 200, endMs = 100, text = "bad"),
            V1TimedTextSegment(startMs = 300, endMs = 400, text = "ok")
        )

        val result = V1TingwuWindowedChunkBuilder.buildWindowedMarkdownChunkResult(
            absStartMs = 0,
            absEndMs = 1000,
            timedSegments = segments
        )

        assertEquals("ok", result.chunk)
        assertEquals(2, result.segmentsInCount)
        assertEquals(1, result.segmentsOutCount)
    }
}
