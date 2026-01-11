package com.smartsales.data.aicore.tingwu

import com.smartsales.data.aicore.disector.DisectorBatch
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import com.smartsales.data.aicore.tingwu.api.TingwuTranscriptSegment
import com.smartsales.data.aicore.tingwu.api.TingwuSpeaker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MultiBatchStitcher].
 * 
 * These tests verify pure domain logic: timestamp correction, segment filtering,
 * speaker deduplication, and text concatenation.
 */
class MultiBatchStitcherTest {

    private val stitcher = MultiBatchStitcher()

    // --- Helpers ---

    private fun batch(
        id: String,
        absStartMs: Long,
        absEndMs: Long,
        captureStartMs: Long = absStartMs,
        captureEndMs: Long = absEndMs
    ) = DisectorBatch(
        batchAssetId = id,
        batchIndex = 0,
        absStartMs = absStartMs,
        absEndMs = absEndMs,
        captureStartMs = captureStartMs,
        captureEndMs = captureEndMs
    )

    private fun segment(
        id: Int = 1,
        start: Double,
        end: Double = start + 1.0,
        text: String = "Test",
        speaker: String? = "spk_1"
    ) = TingwuTranscriptSegment(
        id = id,
        start = start,
        end = end,
        text = text,
        speaker = speaker
    )

    private fun transcription(
        segments: List<TingwuTranscriptSegment> = emptyList(),
        speakers: List<TingwuSpeaker>? = null,
        text: String? = null,
        language: String = "zh"
    ) = TingwuTranscription(
        text = text,
        segments = segments,
        speakers = speakers,
        language = language,
        duration = null,
        url = null
    )

    // --- Test Cases ---

    @Test
    fun stitch_twoBatches_correctsTimestamps() {
        // Batch 0: 0-60s, Batch 1: 60-120s
        val batches = listOf(
            batch("b0", absStartMs = 0, absEndMs = 60_000),
            batch("b1", absStartMs = 60_000, absEndMs = 120_000)
        )
        val transcriptions = listOf(
            transcription(segments = listOf(segment(start = 30.0, end = 35.0))),
            transcription(segments = listOf(segment(start = 15.0, end = 20.0))) // Should become 75-80s
        )

        val result = stitcher.stitch(batches, transcriptions)

        assertEquals(2, result.segments?.size)
        // Batch 0: offset 0 -> 30.0s
        assertEquals(30.0, result.segments!![0].start!!, 0.001)
        // Batch 1: offset 60s -> 15 + 60 = 75.0s
        assertEquals(75.0, result.segments!![1].start!!, 0.001)
        assertEquals(80.0, result.segments!![1].end!!, 0.001)
    }

    @Test
    fun stitch_twoBatches_filtersSegmentsOutsideWindow() {
        // Batch 0: 0-60s window
        val batches = listOf(
            batch("b0", absStartMs = 0, absEndMs = 60_000)
        )
        val transcriptions = listOf(
            transcription(segments = listOf(
                segment(id = 1, start = 30.0),  // 30s -> inside [0, 60)
                segment(id = 2, start = 65.0)   // 65s -> outside window, should be filtered
            ))
        )

        val result = stitcher.stitch(batches, transcriptions)

        assertEquals(1, result.segments?.size)
        assertEquals(1, result.segments!![0].id)
    }

    @Test
    fun stitch_twoBatches_deduplicatesSpeakers() {
        val batches = listOf(
            batch("b0", absStartMs = 0, absEndMs = 60_000),
            batch("b1", absStartMs = 60_000, absEndMs = 120_000)
        )
        val transcriptions = listOf(
            transcription(
                segments = listOf(segment(start = 10.0)),
                speakers = listOf(TingwuSpeaker(id = "spk_1", name = "Alice"))
            ),
            transcription(
                segments = listOf(segment(start = 10.0)),
                speakers = listOf(
                    TingwuSpeaker(id = "spk_1", name = "Alice"),  // Duplicate
                    TingwuSpeaker(id = "spk_2", name = "Bob")
                )
            )
        )

        val result = stitcher.stitch(batches, transcriptions)

        assertEquals(2, result.speakers?.size)
        assertTrue(result.speakers!!.any { it.id == "spk_1" })
        assertTrue(result.speakers!!.any { it.id == "spk_2" })
    }

    @Test
    fun stitch_twoBatches_concatenatesText() {
        val batches = listOf(
            batch("b0", absStartMs = 0, absEndMs = 60_000),
            batch("b1", absStartMs = 60_000, absEndMs = 120_000)
        )
        val transcriptions = listOf(
            transcription(text = "First batch text"),
            transcription(text = "Second batch text")
        )

        val result = stitcher.stitch(batches, transcriptions)

        assertEquals("First batch text\nSecond batch text", result.text)
    }

    @Test
    fun stitch_emptySegments_returnsEmptyResult() {
        val batches = listOf(batch("b0", absStartMs = 0, absEndMs = 60_000))
        val transcriptions = listOf(transcription(segments = emptyList()))

        val result = stitcher.stitch(batches, transcriptions)

        assertTrue(result.segments?.isEmpty() == true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stitch_mismatchedCounts_throwsException() {
        val batches = listOf(
            batch("b0", absStartMs = 0, absEndMs = 60_000),
            batch("b1", absStartMs = 60_000, absEndMs = 120_000)
        )
        val transcriptions = listOf(transcription()) // Only 1, but 2 batches

        stitcher.stitch(batches, transcriptions)
    }

    @Test
    fun stitch_calculatesTotalDuration() {
        val batches = listOf(
            batch("b0", absStartMs = 0, absEndMs = 60_000),
            batch("b1", absStartMs = 60_000, absEndMs = 120_000)
        )
        val transcriptions = listOf(transcription(), transcription())

        val result = stitcher.stitch(batches, transcriptions)

        assertEquals(120.0, result.duration!!, 0.001) // 120_000ms / 1000 = 120s
    }

    // --- Option C: stitchSegments Tests ---

    private fun diarizedSegment(
        startMs: Long,
        endMs: Long,
        text: String = "Test",
        speakerId: String = "spk_1"
    ) = com.smartsales.data.aicore.DiarizedSegment(
        speakerId = speakerId,
        speakerIndex = 1,
        startMs = startMs,
        endMs = endMs,
        text = text
    )

    @Test
    fun stitchSegments_twoBatches_correctsTimestamps() {
        val batchResults = listOf(
            batch("b0", absStartMs = 0, absEndMs = 60_000) to listOf(
                diarizedSegment(startMs = 30_000, endMs = 35_000, text = "First")
            ),
            batch("b1", absStartMs = 60_000, absEndMs = 120_000) to listOf(
                diarizedSegment(startMs = 15_000, endMs = 20_000, text = "Second") // Should become 75-80s
            )
        )

        val result = stitcher.stitchSegments(batchResults)

        assertEquals(2, result.size)
        assertEquals(30_000L, result[0].startMs) // Batch 0: offset 0
        assertEquals(75_000L, result[1].startMs) // Batch 1: 15 + 60 = 75s
        assertEquals("First", result[0].text)
        assertEquals("Second", result[1].text)
    }

    @Test
    fun stitchSegments_filtersOutOfWindow() {
        val batchResults = listOf(
            batch("b0", absStartMs = 0, absEndMs = 60_000) to listOf(
                diarizedSegment(startMs = 30_000, endMs = 35_000, text = "Inside"),
                diarizedSegment(startMs = 65_000, endMs = 70_000, text = "Outside") // 65 >= 60, filtered
            )
        )

        val result = stitcher.stitchSegments(batchResults)

        assertEquals(1, result.size)
        assertEquals("Inside", result[0].text)
    }
}
