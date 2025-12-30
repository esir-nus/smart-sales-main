package com.smartsales.feature.chat.core.v1

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/v1/V1GeneralRetryPolicyTest.kt
// Module: :feature:chat
// Summary: Unit tests for V1GeneralRetryPolicy helpers.
// Author: created on 2025-12-30

import com.smartsales.feature.chat.core.publisher.ArtifactStatus
import com.smartsales.feature.chat.core.stream.CompletionDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class V1GeneralRetryPolicyTest {

    @Test
    fun `buildRepairInstruction matches expected string`() {
        val expected = "REPAIR: Output exactly one <visible2user>...</visible2user> and one ```json block outside <visible2user>. No other text outside those sections."

        assertEquals(expected, V1GeneralRetryPolicy.buildRepairInstruction())
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
}
