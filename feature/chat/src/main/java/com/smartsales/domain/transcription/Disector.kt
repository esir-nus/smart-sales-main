// File: feature/chat/src/main/java/com/smartsales/domain/transcription/Disector.kt
// Module: :feature:chat
// Summary: Batch splitting logic per Orchestrator-V1 Appendix A (deterministic, reproducible)
// Author: created on 2026-01-05

package com.smartsales.domain.transcription

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor

/**
 * Disector: deterministic batch splitter per Orchestrator-V1 Appendix A.
 * 
 * Input: audio total duration (totalMs)
 * Output: DisectorPlan (macro windows + capture window + overlap)
 * 
 * Rules:
 * - If duration <= 20 minutes: single batch
 * - If duration > 20 minutes: split into 10-minute batches
 * - Remainder < 7 minutes: merge into last batch
 * - Remainder >= 7 minutes: create separate batch
 * - Pre-roll overlap: 10 seconds (first batch: no overlap)
 */
@Singleton
class Disector @Inject constructor() {

    fun createPlan(
        totalMs: Long,
        audioAssetId: String,
        recordingSessionId: String
    ): DisectorPlan {
        // If <= 20 minutes, no split
        if (totalMs <= TWENTY_MIN_MS) {
            return DisectorPlan(
                disectorPlanId = generatePlanId(audioAssetId, totalMs),
                audioAssetId = audioAssetId,
                recordingSessionId = recordingSessionId,
                totalMs = totalMs,
                batches = listOf(
                    DisectorBatch(
                        batchIndex = 1,
                        batchAssetId = generateBatchAssetId(audioAssetId, 1),
                        absStartMs = 0,
                        absEndMs = totalMs,
                        captureStartMs = 0,
                        captureEndMs = totalMs
                    )
                )
            )
        }

        // Split flow
        val full = floor(totalMs.toDouble() / TEN_MIN_MS).toInt()
        val remMs = totalMs % TEN_MIN_MS

        val batches = mutableListOf<DisectorBatch>()
        
        // Generate full 10-minute batches
        for (i in 0 until full) {
            val absStart = i * TEN_MIN_MS
            val absEnd = absStart + TEN_MIN_MS
            val captureStart = if (i == 0) 0L else maxOf(0, absStart - OVERLAP_MS)
            
            batches += DisectorBatch(
                batchIndex = i + 1,
                batchAssetId = generateBatchAssetId(audioAssetId, i + 1),
                absStartMs = absStart,
                absEndMs = absEnd,
                captureStartMs = captureStart,
                captureEndMs = absEnd
            )
        }

        // Handle remainder
        if (remMs < SEVEN_MIN_MS) {
            // Merge into last batch
            val last = batches.removeLast()
            batches += last.copy(
                absEndMs = totalMs,
                captureEndMs = totalMs
            )
        } else {
            // Create remainder batch
            val absStart = full * TEN_MIN_MS
            batches += DisectorBatch(
                batchIndex = full + 1,
                batchAssetId = generateBatchAssetId(audioAssetId, full + 1),
                absStartMs = absStart,
                absEndMs = totalMs,
                captureStartMs = maxOf(0, absStart - OVERLAP_MS),
                captureEndMs = totalMs
            )
        }

        return DisectorPlan(
            disectorPlanId = generatePlanId(audioAssetId, totalMs),
            audioAssetId = audioAssetId,
            recordingSessionId = recordingSessionId,
            totalMs = totalMs,
            batches = batches
        )
    }

    private fun generatePlanId(audioAssetId: String, totalMs: Long): String =
        "plan_${audioAssetId}_${totalMs}_v${RULE_VERSION}"

    private fun generateBatchAssetId(audioAssetId: String, batchIndex: Int): String =
        "${audioAssetId}_b${batchIndex}"

    companion object {
        private const val TEN_MIN_MS = 600_000L
        private const val SEVEN_MIN_MS = 420_000L
        private const val TWENTY_MIN_MS = 1_200_000L
        private const val OVERLAP_MS = 10_000L
        private const val RULE_VERSION = 1
    }
}

/**
 * DisectorPlan: deterministic batch plan output.
 */
data class DisectorPlan(
    val disectorPlanId: String,
    val audioAssetId: String,
    val recordingSessionId: String,
    val totalMs: Long,
    val batches: List<DisectorBatch>
)

/**
 * DisectorBatch: single batch specification.
 * 
 * Macro window: [absStartMs, absEndMs) - authoritative timeline
 * Capture window: [captureStartMs, captureEndMs) - submitted audio range
 */
data class DisectorBatch(
    val batchIndex: Int,
    val batchAssetId: String,
    val absStartMs: Long,
    val absEndMs: Long,
    val captureStartMs: Long,
    val captureEndMs: Long
)
