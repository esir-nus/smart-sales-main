package com.smartsales.feature.chat.core.v1

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/v1/V1GeneralRetryPolicy.kt
// Module: :feature:chat
// Summary: V1 GENERAL retry policy helpers (instruction + decision rules).
// Author: created on 2025-12-30

import android.util.Log
import com.smartsales.feature.chat.core.publisher.ArtifactStatus
import com.smartsales.feature.chat.core.publisher.GeneralChatV1Finalizer
import com.smartsales.feature.chat.core.publisher.V1FinalizeResult
import com.smartsales.feature.chat.core.stream.CompletionDecision

object V1GeneralRetryPolicy {
    // 抽取策略常量，降低 ViewModel 复杂度，便于单元测试
    fun buildRepairInstruction(): String {
        // 仅允许格式修复：保持原意不新增内容，避免语义漂移
        // 禁止在 <visible2user> 内放 ```json，防止泄露到 UI，便于稳定提取
        return buildString {
            append("FORMAT REPAIR ONLY: ")
            append("Wrap the SAME user-visible content in lowercase <visible2user>...</visible2user>. ")
            append("Do not add new content or change meaning. ")
            append("Do NOT put ```json inside <visible2user>. ")
            append("For L3 only, include exactly one ```json block OUTSIDE <visible2user>. ")
            append("L1/L2 do not need JSON.")
        }
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
        // 格式类失败允许重试，避免过早进入终态
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
        // 这里只记录 reason code 与重试决策，严禁输出用户内容/模型原文，避免隐私泄露
        Log.d(
            LOG_TAG,
            "event=v1_general_retry_decision, " +
                "artifactStatus=${finalizeResult.artifactStatus}, " +
                "failureReason=${finalizeResult.failureReason}, " +
                "attempt=$attempt, " +
                "maxRetries=$maxRetries, " +
                "decision=$decision, " +
                "reasonAware=$enableReasonAwareRetry"
        )
        return V1CompletionEval(
            decision = decision,
            finalizeResult = finalizeResult
        )
    }
}

private const val LOG_TAG = "V1General"
