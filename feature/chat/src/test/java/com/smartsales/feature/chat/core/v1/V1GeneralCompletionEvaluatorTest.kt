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

    private fun evaluatorWithStatus(status: ArtifactStatus): V1GeneralCompletionEvaluator {
        val publisher = FakePublisher(status)
        val finalizer = GeneralChatV1Finalizer(publisher)
        return V1GeneralCompletionEvaluator(finalizer)
    }

    private fun expectedFinalize(status: ArtifactStatus): V1FinalizeResult {
        return V1FinalizeResult(
            visibleMarkdown = "ok",
            artifactStatus = status,
            artifactJson = "{\"a\":1}",
        )
    }

    private class FakePublisher(
        private val status: ArtifactStatus,
    ) : ChatPublisher {
        override fun publish(rawText: String, retryCount: Int): PublishedChatTurnV1 {
            return PublishedChatTurnV1(
                displayMarkdown = "ok",
                machineArtifactJson = "{\"a\":1}",
                artifactStatus = status,
                retryCount = retryCount,
                failureReason = null,
            )
        }

        override fun fallbackMessage(): String = "fallback"
    }
}
