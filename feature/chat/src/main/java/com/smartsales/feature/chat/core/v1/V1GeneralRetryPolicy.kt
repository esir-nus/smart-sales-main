package com.smartsales.feature.chat.core.v1

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/v1/V1GeneralRetryPolicy.kt
// Module: :feature:chat
// Summary: V1 GENERAL retry policy helpers (instruction + decision rules).
// Author: created on 2025-12-30

import com.smartsales.feature.chat.core.publisher.ArtifactStatus
import com.smartsales.feature.chat.core.publisher.GeneralChatV1Finalizer
import com.smartsales.feature.chat.core.publisher.V1FinalizeResult
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

    fun decide(
        artifactStatus: ArtifactStatus,
        attempt: Int,
        maxRetries: Int,
        failureReason: String?,
        enableReasonAware: Boolean
    ): CompletionDecision {
        if (!enableReasonAware) {
            return decide(artifactStatus, attempt, maxRetries)
        }
        if (artifactStatus == ArtifactStatus.VALID) {
            return CompletionDecision.Accept
        }
        if (failureReason == "missing_json_fence") {
            // 缺失 fenced JSON 无法靠重试补齐，直接终止并走 terminal
            return CompletionDecision.Terminal
        }
        return if (attempt < maxRetries) {
            CompletionDecision.Retry
        } else {
            CompletionDecision.Terminal
        }
    }
}

data class V1CompletionEval(
    val decision: CompletionDecision,
    val finalizeResult: V1FinalizeResult,
)

class V1GeneralCompletionEvaluator(
    private val finalizer: GeneralChatV1Finalizer,
) {
    // 把“验收/决策”集中在纯逻辑层，降低 ViewModel 复杂度，便于测试（行为不变）
    fun evaluate(
        rawFullText: String,
        attempt: Int,
        maxRetries: Int,
        enableReasonAwareRetry: Boolean = false
    ): V1CompletionEval {
        val finalizeResult = finalizer.finalize(rawFullText)
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = finalizeResult.artifactStatus,
            attempt = attempt,
            maxRetries = maxRetries,
            failureReason = finalizeResult.failureReason,
            enableReasonAware = enableReasonAwareRetry
        )
        return V1CompletionEval(
            decision = decision,
            finalizeResult = finalizeResult
        )
    }
}
