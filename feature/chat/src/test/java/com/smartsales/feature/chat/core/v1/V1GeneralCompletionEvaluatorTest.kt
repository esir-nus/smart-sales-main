package com.smartsales.feature.chat.core.v1

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/v1/V1GeneralCompletionEvaluatorTest.kt
// Module: :feature:chat
// Summary: Unit tests for V1GeneralCompletionEvaluator.
// Author: created on 2025-12-30

import com.smartsales.feature.chat.core.publisher.ArtifactStatus
import com.smartsales.feature.chat.core.publisher.ChatPublisher
import com.smartsales.feature.chat.core.publisher.GeneralChatV1Finalizer
import com.smartsales.feature.chat.core.publisher.PublishedChatTurnV1
import com.smartsales.feature.chat.core.publisher.V1FinalizeResult
import com.smartsales.feature.chat.core.stream.CompletionDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class V1GeneralCompletionEvaluatorTest {
    @Test
    fun `valid artifact returns accept`() {
        val evaluator = evaluatorWithStatus(ArtifactStatus.VALID)

        val eval = evaluator.evaluate(rawFullText = "raw", attempt = 0, maxRetries = 2)

        assertEquals(CompletionDecision.Accept, eval.decision)
        assertEquals(expectedFinalize(ArtifactStatus.VALID), eval.finalizeResult)
    }

    @Test
    fun `invalid artifact retries before max`() {
        val evaluator = evaluatorWithStatus(ArtifactStatus.INVALID)

        val eval = evaluator.evaluate(rawFullText = "raw", attempt = 0, maxRetries = 2)

        assertEquals(CompletionDecision.Retry, eval.decision)
        assertEquals(expectedFinalize(ArtifactStatus.INVALID), eval.finalizeResult)
    }

    @Test
    fun `invalid artifact terminal at max retry`() {
        val evaluator = evaluatorWithStatus(ArtifactStatus.INVALID)

        val eval = evaluator.evaluate(rawFullText = "raw", attempt = 2, maxRetries = 2)

        assertEquals(CompletionDecision.Terminal, eval.decision)
        assertEquals(expectedFinalize(ArtifactStatus.INVALID), eval.finalizeResult)
    }

    @Test
    fun `reason aware missing fence retries`() {
        val evaluator = evaluatorWithStatus(
            status = ArtifactStatus.INVALID,
            failureReason = REASON_MISSING_JSON_FENCE
        )

        val eval = evaluator.evaluate(
            rawFullText = "raw",
            attempt = 0,
            maxRetries = 2,
            enableReasonAwareRetry = true
        )

        assertEquals(CompletionDecision.Retry, eval.decision)
        assertEquals(
            expectedFinalize(ArtifactStatus.INVALID, REASON_MISSING_JSON_FENCE),
            eval.finalizeResult
        )
    }

    @Test
    fun `reason aware disabled keeps legacy behavior`() {
        val evaluator = evaluatorWithStatus(
            status = ArtifactStatus.INVALID,
            failureReason = REASON_MISSING_JSON_FENCE
        )

        val eval = evaluator.evaluate(rawFullText = "raw", attempt = 0, maxRetries = 2)

        assertEquals(CompletionDecision.Retry, eval.decision)
        assertEquals(
            expectedFinalize(ArtifactStatus.INVALID, REASON_MISSING_JSON_FENCE),
            eval.finalizeResult
        )
    }

    @Test
    fun `reason aware retries for other invalid reasons`() {
        val evaluator = evaluatorWithStatus(
            status = ArtifactStatus.INVALID,
            failureReason = "type_mismatch"
        )

        val eval = evaluator.evaluate(
            rawFullText = "raw",
            attempt = 0,
            maxRetries = 2,
            enableReasonAwareRetry = true
        )

        assertEquals(CompletionDecision.Retry, eval.decision)
        assertEquals(
            expectedFinalize(ArtifactStatus.INVALID, "type_mismatch"),
            eval.finalizeResult
        )
    }

    private fun evaluatorWithStatus(
        status: ArtifactStatus,
        failureReason: String? = null
    ): V1GeneralCompletionEvaluator {
        val publisher = FakePublisher(status, failureReason)
        val finalizer = GeneralChatV1Finalizer(publisher)
        return V1GeneralCompletionEvaluator(finalizer)
    }

    private fun expectedFinalize(
        status: ArtifactStatus,
        failureReason: String? = null
    ): V1FinalizeResult {
        return V1FinalizeResult(
            visibleMarkdown = "ok",
            artifactStatus = status,
            artifactJson = VALID_ARTIFACT_JSON,
            failureReason = failureReason,
        )
    }

    private class FakePublisher(
        private val status: ArtifactStatus,
        private val failureReason: String?,
    ) : ChatPublisher {
        override fun publish(rawText: String, retryCount: Int): PublishedChatTurnV1 {
            return PublishedChatTurnV1(
                displayMarkdown = "ok",
                machineArtifactJson = VALID_ARTIFACT_JSON,
                artifactStatus = status,
                retryCount = retryCount,
                failureReason = failureReason,
            )
        }

        override fun fallbackMessage(): String = "fallback"
    }

    private companion object {
        const val VALID_ARTIFACT_JSON =
            """{"artifactType":"MachineArtifact","schemaVersion":1,"mode":"L1","provenance":{"chatSessionId":"test-session","turnId":"turn-1","createdAtMs":0}}"""
        const val REASON_MISSING_JSON_FENCE = "missing_json_fence"
    }
}
