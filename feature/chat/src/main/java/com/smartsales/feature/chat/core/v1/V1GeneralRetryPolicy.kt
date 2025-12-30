package com.smartsales.feature.chat.core.v1

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/v1/V1GeneralRetryPolicy.kt
// Module: :feature:chat
// Summary: V1 GENERAL retry policy helpers (instruction + decision rules).
// Author: created on 2025-12-30

import com.smartsales.feature.chat.core.publisher.ArtifactStatus
import com.smartsales.feature.chat.core.stream.CompletionDecision

object V1GeneralRetryPolicy {
    // 抽取策略常量，降低 ViewModel 复杂度，便于单元测试
    fun buildRepairInstruction(): String {
        return "REPAIR: Output exactly one <visible2user>...</visible2user> and one ```json block outside <visible2user>. No other text outside those sections."
    }

    fun decide(
        artifactStatus: ArtifactStatus,
        attempt: Int,
        maxRetries: Int
    ): CompletionDecision {
        return if (artifactStatus == ArtifactStatus.VALID) {
            CompletionDecision.Accept
        } else if (attempt < maxRetries) {
            CompletionDecision.Retry
        } else {
            CompletionDecision.Terminal
        }
    }
}
