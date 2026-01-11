// File: data/ai-core/src/main/java/com/smartsales/data/aicore/disector/Disector.kt
// Module: :data:ai-core
// Summary: Batch planning interface for Tingwu transcription per Orchestrator-V1 Appendix A
// Author: created on 2026-01-11

package com.smartsales.data.aicore.disector

/**
 * Disector: deterministic batch planner per Orchestrator-V1 Appendix A.
 * 
 * Lives in ai-core to be accessible from TingwuRunner. The domain implementation
 * in feature:chat delegates to this interface.
 */
interface Disector {
    /**
     * Create batch plan from audio duration.
     * 
     * Rules (V1 Appendix A):
     * - If duration <= 20 minutes: single batch
     * - If duration > 20 minutes: split into 10-minute batches
     * - Remainder < 7 minutes: merge into last batch
     * - Remainder >= 7 minutes: create separate batch
     * - Pre-roll overlap: 10 seconds (first batch: no overlap)
     */
    fun createPlan(
        totalMs: Long,
        audioAssetId: String,
        recordingSessionId: String
    ): DisectorPlan
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
