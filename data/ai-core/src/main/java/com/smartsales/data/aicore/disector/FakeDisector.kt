// File: data/ai-core/src/main/java/com/smartsales/data/aicore/disector/FakeDisector.kt
// Module: :data:ai-core
// Summary: Fake Disector for testing - returns configurable batch plans
// Author: created on 2026-01-14

package com.smartsales.data.aicore.disector

import javax.inject.Inject
import javax.inject.Singleton

/**
 * FakeDisector: Test double for Disector interface.
 * 
 * By default returns a single-batch plan. Set [stubPlan] to override.
 */
@Singleton
class FakeDisector @Inject constructor() : Disector {
    
    /** Override this to return a custom plan in tests. */
    var stubPlan: DisectorPlan? = null
    
    /** Track calls for verification. */
    val createPlanCalls = mutableListOf<CreatePlanCall>()
    
    data class CreatePlanCall(
        val totalMs: Long,
        val audioAssetId: String,
        val recordingSessionId: String
    )
    
    override fun createPlan(
        totalMs: Long,
        audioAssetId: String,
        recordingSessionId: String
    ): DisectorPlan {
        createPlanCalls.add(CreatePlanCall(totalMs, audioAssetId, recordingSessionId))
        
        return stubPlan ?: DisectorPlan(
            disectorPlanId = "fake_plan_${audioAssetId}",
            audioAssetId = audioAssetId,
            recordingSessionId = recordingSessionId,
            totalMs = totalMs,
            batches = listOf(
                DisectorBatch(
                    batchIndex = 1,
                    batchAssetId = "${audioAssetId}_b1",
                    absStartMs = 0,
                    absEndMs = totalMs,
                    captureStartMs = 0,
                    captureEndMs = totalMs
                )
            )
        )
    }
    
    /** Reset state between tests. */
    fun reset() {
        stubPlan = null
        createPlanCalls.clear()
    }
}
