package com.smartsales.feature.chat.core.v1

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/v1/V1GeneralRetryPolicyTest.kt
// Module: :feature:chat
// Summary: Unit tests for V1GeneralRetryPolicy helpers.
// Author: created on 2025-12-30

import com.smartsales.feature.chat.core.publisher.ArtifactStatus
import com.smartsales.domain.stream.CompletionDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V1GeneralRetryPolicyTest {

    @Test
    fun `buildRepairInstruction matches expected string`() {
        val instruction = V1GeneralRetryPolicy.buildRepairInstruction()

        assertTrue(instruction.contains("<visible2user>"))
        assertTrue(instruction.contains("FORMAT REPAIR ONLY"))
        assertTrue(instruction.contains("Do not add new content"))
        assertTrue(instruction.contains("```json"))
        assertTrue(instruction.contains("OUTSIDE <visible2user>"))
        assertTrue(instruction.contains("Do NOT put ```json inside <visible2user>"))
    }

    @Test
    fun `decide returns accept for valid`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.VALID,
            attempt = 0,
            maxRetries = 2
        )

        assertEquals(CompletionDecision.Accept, decision)
    }

    @Test
    fun `decide retries before max retries`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.INVALID,
            attempt = 0,
            maxRetries = 2
        )

        assertEquals(CompletionDecision.Retry, decision)
    }

    @Test
    fun `decide terminal when attempts exhausted`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.INVALID,
            attempt = 2,
            maxRetries = 2
        )

        assertEquals(CompletionDecision.Terminal, decision)
    }

    @Test
    fun `decide terminal when maxRetries is zero`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.INVALID,
            attempt = 0,
            maxRetries = 0
        )

        assertEquals(CompletionDecision.Terminal, decision)
    }

    @Test
    fun `missing visible2user is retryable`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.INVALID,
            attempt = 0,
            maxRetries = 2,
            failureReason = "missing_visible2user",
            enableReasonAware = true
        )

        assertEquals(CompletionDecision.Retry, decision)
    }

    @Test
    fun `malformed visible2user is retryable`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.INVALID,
            attempt = 0,
            maxRetries = 2,
            failureReason = "malformed_visible2user",
            enableReasonAware = true
        )

        assertEquals(CompletionDecision.Retry, decision)
    }

    @Test
    fun `empty visible2user is retryable`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.INVALID,
            attempt = 0,
            maxRetries = 2,
            failureReason = "empty_visible2user",
            enableReasonAware = true
        )

        assertEquals(CompletionDecision.Retry, decision)
    }

    @Test
    fun `missing json fence is retryable`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.INVALID,
            attempt = 0,
            maxRetries = 2,
            failureReason = "missing_json_fence",
            enableReasonAware = true
        )

        assertEquals(CompletionDecision.Retry, decision)
    }

    @Test
    fun `invalid machine artifact is retryable`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.INVALID,
            attempt = 0,
            maxRetries = 2,
            failureReason = "invalid_machine_artifact",
            enableReasonAware = true
        )

        assertEquals(CompletionDecision.Retry, decision)
    }

    @Test
    fun `reason aware still terminals after max retries`() {
        val decision = V1GeneralRetryPolicy.decide(
            artifactStatus = ArtifactStatus.INVALID,
            attempt = 2,
            maxRetries = 2,
            failureReason = "missing_visible2user",
            enableReasonAware = true
        )

        assertEquals(CompletionDecision.Terminal, decision)
    }
}
