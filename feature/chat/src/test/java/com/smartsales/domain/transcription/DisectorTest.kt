package com.smartsales.domain.transcription

// File: feature/chat/src/test/java/com/smartsales/domain/transcription/DisectorTest.kt
// Module: :feature:chat
// Summary: Unit tests for Disector (V1 Appendix A batch splitting rules)
// Author: created on 2026-01-06

import org.junit.Assert.assertEquals
import org.junit.Test

class DisectorTest {

    private val disector = Disector()

    // ===== Single batch tests (≤20 min) =====

    @Test
    fun createPlan_under20Minutes_createsSingleBatch() {
        val plan = disector.createPlan(
            totalMs = 15 * 60 * 1000L,  // 15 minutes
            audioAssetId = "audio123",
            recordingSessionId = "session456"
        )

        assertEquals(1, plan.batches.size)
        val batch = plan.batches[0]
        assertEquals(0L, batch.absStartMs)
        assertEquals(15 * 60 * 1000L, batch.absEndMs)
        // Single batch: capture = abs (no overlap)
        assertEquals(0L, batch.captureStartMs)
        assertEquals(15 * 60 * 1000L, batch.captureEndMs)
    }

    @Test
    fun createPlan_exactly20Minutes_createsSingleBatch() {
        val plan = disector.createPlan(
            totalMs = 20 * 60 * 1000L,  // 20 minutes
            audioAssetId = "audio123",
            recordingSessionId = "session456"
        )

        assertEquals(1, plan.batches.size)
    }

    // ===== Split tests (>20 min) - V1 Appendix A examples =====

    @Test
    fun createPlan_21Minutes_splitsToBatches_10_11() {
        val plan = disector.createPlan(
            totalMs = 21 * 60 * 1000L,  // 21 minutes
            audioAssetId = "audio123",
            recordingSessionId = "session456"
        )

        assertEquals(2, plan.batches.size)

        // Batch 1 (index=1): abs [0, 10min), capture [0, 10min) - no overlap
        val batch1 = plan.batches[0]
        assertEquals(1, batch1.batchIndex)
        assertEquals(0L, batch1.absStartMs)
        assertEquals(10 * 60 * 1000L, batch1.absEndMs)
        assertEquals(0L, batch1.captureStartMs)
        assertEquals(10 * 60 * 1000L, batch1.captureEndMs)

        // Batch 2 (index=2): abs [10min, 21min), but 21-20=1min < 7min so merged
        // Actually creates 2 batches: (10min, 11min merged)
        val batch2 = plan.batches[1]
        assertEquals(2, batch2.batchIndex)
        assertEquals(10 * 60 * 1000L, batch2.absStartMs)
        assertEquals(21 * 60 * 1000L, batch2.absEndMs)
        assertEquals(10 * 60 * 1000L - 10000L, batch2.captureStartMs)  // 10s overlap
        assertEquals(21 * 60 * 1000L, batch2.captureEndMs)
    }

    @Test
    fun createPlan_27Minutes_splitsToBatches_10_10_7() {
        val plan = disector.createPlan(
            totalMs = 27 * 60 * 1000L,  // 27 minutes
            audioAssetId = "audio123",
            recordingSessionId = "session456"
        )

        assertEquals(3, plan.batches.size)

        // Batch 1 (index=1): [0, 10min)
        val batch1 = plan.batches[0]
        assertEquals(1, batch1.batchIndex)
        assertEquals(10 * 60 * 1000L, batch1.absEndMs - batch1.absStartMs)
        assertEquals(batch1.absStartMs, batch1.captureStartMs)

        // Batch 2 (index=2): [10min, 20min), with 10s overlap
        val batch2 = plan.batches[1]
        assertEquals(2, batch2.batchIndex)
        assertEquals(10 * 60 * 1000L, batch2.absEndMs - batch2.absStartMs)
        assertEquals(10 * 60 * 1000L - 10000L, batch2.captureStartMs)

        // Batch 3 (index=3): [20min, 27min), with 10s overlap
        val batch3 = plan.batches[2]
        assertEquals(3, batch3.batchIndex)
        assertEquals(7 * 60 * 1000L, batch3.absEndMs - batch3.absStartMs)
        assertEquals(20 * 60 * 1000L - 10000L, batch3.captureStartMs)
    }

    @Test
    fun createPlan_30Minutes_splitsToBatches_10_10_10() {
        val plan = disector.createPlan(
            totalMs = 30 * 60 * 1000L,  // 30 minutes
            audioAssetId = "audio123",
            recordingSessionId = "session456"
        )

        assertEquals(3, plan.batches.size)
        plan.batches.forEach { batch ->
            assertEquals(10 * 60 * 1000L, batch.absEndMs - batch.absStartMs)
        }
    }

    // ==== Overlap calculation tests =====

    @Test
    fun createPlan_multipleBatches_haveCorrectOverlap() {
        val plan = disector.createPlan(
            totalMs = 25 * 60 * 1000L,
            audioAssetId = "audio123",
            recordingSessionId = "session456"
        )

        // Only batches after the first should have overlap
        plan.batches.forEachIndexed { index, batch ->
            if (index == 0) {
                // First batch: no overlap
                assertEquals(batch.absStartMs, batch.captureStartMs)
            } else {
                // Subsequent batches: 10s pre-roll overlap
                val overlap = batch.absStartMs - batch.captureStartMs
                assertEquals(10000L, overlap)
            }
        }
    }

    @Test
    fun createPlan_firstBatch_hasNoOverlap() {
        val plan = disector.createPlan(
            totalMs = 35 * 60 * 1000L,
            audioAssetId = "audio123",
            recordingSessionId = "session456"
        )

        val firstBatch = plan.batches[0]
        assertEquals(firstBatch.absStartMs, firstBatch.captureStartMs)
        assertEquals(firstBatch.absEndMs, firstBatch.captureEndMs)
    }

    // ===== Metadata tests =====

    @Test
    fun createPlan_preservesAudioAssetId() {
        val plan = disector.createPlan(
            totalMs = 15 * 60 * 1000L,
            audioAssetId = "audio-123-abc",
            recordingSessionId = "session-456-def"
        )

        plan.batches.forEach { batch ->
            // Each batch should have the audio asset ID embedded in batchAssetId
            assert(batch.batchAssetId.contains("audio-123-abc"))
        }
    }

    @Test
    fun createPlan_batchIndicesAreSequential() {
        val plan = disector.createPlan(
            totalMs = 35 * 60 * 1000L,
            audioAssetId = "audio123",
            recordingSessionId = "session456"
        )

        // batchIndex is 1-indexed
        plan.batches.forEachIndexed { index, batch ->
            assertEquals(index + 1, batch.batchIndex)
        }
    }
}
