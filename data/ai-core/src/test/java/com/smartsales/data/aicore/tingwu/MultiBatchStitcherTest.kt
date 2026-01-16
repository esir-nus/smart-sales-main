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

    // --- V1 §B.1: Anchor Duplication Tests ---

    /**
     * Verifies that segments in the post-roll overlap region are intentionally
     * duplicated at batch boundaries per Orchestrator-V1 §B.1.
     *
     * Scenario:
     * - Batch 0: abs [0, 60s), capture [0, 70s) — has 10s post-roll
     * - Batch 1: abs [60s, 120s), capture [50s, 120s) — has 10s pre-roll
     * - Anchor sentence at 55s (in batch 0's post-roll and batch 1's pre-roll)
     *
     * Expected: The anchor sentence appears in BOTH batches' stitched output.
     */
    @Test
    fun stitchSegments_postRollOverlap_allowsAnchorDuplication() {
        // Batch 0: abs [0, 60s), capture [0, 70s) — post-roll extends 10s beyond absEnd
        // Batch 1: abs [60s, 120s), capture [50s, 120s) — pre-roll starts 10s before absStart
        val batch0 = batch("b0",
            absStartMs = 0,
            absEndMs = 60_000,
            captureStartMs = 0,
            captureEndMs = 70_000  // Post-roll: 10s beyond absEnd
        )
        val batch1 = batch("b1",
            absStartMs = 60_000,
            absEndMs = 120_000,
            captureStartMs = 50_000,  // Pre-roll: 10s before absStart
            captureEndMs = 120_000
        )

        // Batch 0 transcription: anchor segment at 55s (relative to capture start 0)
        // This falls within batch 0's capture window [0, 70_000)
        val batch0Segments = listOf(
            diarizedSegment(startMs = 55_000, endMs = 58_000, text = "Anchor from batch 0")
        )

        // Batch 1 transcription: same anchor segment at 5s (relative to capture start 50s)
        // Absolute: 50_000 + 5_000 = 55_000ms = 55s
        // This falls within batch 1's capture window [50_000, 120_000)
        val batch1Segments = listOf(
            diarizedSegment(startMs = 5_000, endMs = 8_000, text = "Anchor from batch 1")
        )

        val batchResults = listOf(
            batch0 to batch0Segments,
            batch1 to batch1Segments
        )

        val result = stitcher.stitchSegments(batchResults)

        // Both anchor segments should be present (intentional duplication)
        assertEquals(2, result.size)
        
        // Both should have same absolute timestamp (55s)
        assertEquals(55_000L, result[0].startMs)
        assertEquals(55_000L, result[1].startMs)
        
        // Different text proves they came from different batch transcriptions
        assertEquals("Anchor from batch 0", result[0].text)
        assertEquals("Anchor from batch 1", result[1].text)
    }

    /**
     * Verifies that segments outside the capture window are still filtered,
     * even when post-roll overlap is present.
     */
    @Test
    fun stitchSegments_postRollOverlap_stillFiltersOutsideCaptureWindow() {
        val batch0 = batch("b0",
            absStartMs = 0,
            absEndMs = 60_000,
            captureStartMs = 0,
            captureEndMs = 70_000  // Post-roll
        )

        val batch0Segments = listOf(
            diarizedSegment(startMs = 65_000, endMs = 68_000, text = "Inside post-roll"),
            diarizedSegment(startMs = 75_000, endMs = 78_000, text = "Outside capture window") // 75s >= 70s
        )

        val batchResults = listOf(batch0 to batch0Segments)

        val result = stitcher.stitchSegments(batchResults)

        assertEquals(1, result.size)
        assertEquals("Inside post-roll", result[0].text)
        assertEquals(65_000L, result[0].startMs)
    }

    // --- Integration Test: DisectorImpl + MultiBatchStitcher ---

    /**
     * End-to-end integration test using real DisectorImpl.
     * Verifies that the post-roll generated by DisectorImpl is correctly
     * handled by MultiBatchStitcher.
     */
    @Test
    fun integration_disectorPlan_withPostRoll_stitchesCorrectly() {
        // Create real DisectorImpl and generate a plan for 21 minutes
        val disector = com.smartsales.data.aicore.disector.DisectorImpl()
        val plan = disector.createPlan(
            totalMs = 21 * 60 * 1000L,  // 21 minutes
            audioAssetId = "test_audio",
            recordingSessionId = "test_session"
        )

        // Verify DisectorImpl creates 2 batches with correct overlap
        assertEquals(2, plan.batches.size)
        
        val batch1 = plan.batches[0]
        val batch2 = plan.batches[1]
        
        // Batch 1: abs [0, 10min), capture [0, 10:10) — has 10s post-roll
        assertEquals(0L, batch1.absStartMs)
        assertEquals(10 * 60 * 1000L, batch1.absEndMs)
        assertEquals(0L, batch1.captureStartMs)
        assertEquals(10 * 60 * 1000L + 10_000L, batch1.captureEndMs)  // Post-roll!

        // Batch 2: abs [10min, 21min), capture [9:50, 21min) — has pre-roll, no post-roll (last batch)
        assertEquals(10 * 60 * 1000L, batch2.absStartMs)
        assertEquals(21 * 60 * 1000L, batch2.absEndMs)
        assertEquals(10 * 60 * 1000L - 10_000L, batch2.captureStartMs)  // Pre-roll!
        assertEquals(21 * 60 * 1000L, batch2.captureEndMs)  // No post-roll (last batch)

        // Simulate transcription segments from each batch
        // Anchor sentence at 9:55 (595s) — in batch 1's post-roll AND batch 2's pre-roll
        val batch1Segments = listOf(
            diarizedSegment(startMs = 595_000, endMs = 598_000, text = "Anchor B1")
        )
        // Batch 2's capture starts at 590s, so anchor at 595s is at relative 5s
        val batch2Segments = listOf(
            diarizedSegment(startMs = 5_000, endMs = 8_000, text = "Anchor B2")
        )

        val batchResults = listOf(
            batch1 to batch1Segments,
            batch2 to batch2Segments
        )

        // Stitch
        val result = stitcher.stitchSegments(batchResults)

        // Both anchors should be present (intentional duplication per §B.1)
        assertEquals(2, result.size)
        
        // Both should map to absolute 595s
        assertEquals(595_000L, result[0].startMs)
        assertEquals(595_000L, result[1].startMs)
    }
}
