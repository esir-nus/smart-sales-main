package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/publisher/GeneralChatV1FinalizerTest.kt
// Module: :feature:chat
// Summary: Unit tests for GeneralChatV1Finalizer seam behavior.
// Author: created on 2025-12-29

import org.junit.Assert.assertEquals
import org.junit.Test

class GeneralChatV1FinalizerTest {
    private val validArtifactJson =
        """{"artifactType":"MachineArtifact","schemaVersion":1,"mode":"L1","provenance":{"chatSessionId":"cs1","turnId":"t1","createdAtMs":0}}"""

    @Test
    fun `finalizer returns visible2user and valid artifact`() {
        val publisher = ChatPublisherImpl()
        val finalizer = GeneralChatV1Finalizer(publisher)
        val raw = """
            <visible2user>Hi</visible2user>
            ```json
            $validArtifactJson
            ```
        """.trimIndent()

        val result = finalizer.finalize(raw)

        assertEquals("Hi", result.visibleMarkdown)
        assertEquals(ArtifactStatus.VALID, result.artifactStatus)
        assertEquals(validArtifactJson, result.artifactJson)
    }

    @Test
    fun `finalizer does not fallback when visible2user missing`() {
        val publisher = ChatPublisherImpl()
        val finalizer = GeneralChatV1Finalizer(publisher)
        val raw = """
            hello
            ```json
            $validArtifactJson
            ```
        """.trimIndent()

        val result = finalizer.finalize(raw)

        assertEquals("", result.visibleMarkdown)
        assertEquals(ArtifactStatus.INVALID, result.artifactStatus)
    }

    @Test
    fun `terminal fallback strips json and marks failed`() {
        val publisher = ChatPublisherImpl()
        val finalizer = GeneralChatV1Finalizer(publisher)
        val raw = """
            hello
            ```json
            $validArtifactJson
            ```
        """.trimIndent()

        val result = finalizer.finalize(raw, isTerminal = true)

        assertEquals("hello", result.visibleMarkdown)
        assertEquals(ArtifactStatus.FAILED, result.artifactStatus)
        assertEquals(true, result.visibleMarkdown.contains("```").not())
        assertEquals(true, result.visibleMarkdown.contains("artifactType").not())
    }

    @Test
    fun `finalizer keeps human draft when artifact invalid`() {
        val publisher = ChatPublisherImpl()
        val finalizer = GeneralChatV1Finalizer(publisher)
        val raw = """
            <visible2user>Hi</visible2user>
            ```json
            {invalid}
            ```
        """.trimIndent()

        val result = finalizer.finalize(raw)

        assertEquals("Hi", result.visibleMarkdown)
        assertEquals(ArtifactStatus.INVALID, result.artifactStatus)
    }
}
