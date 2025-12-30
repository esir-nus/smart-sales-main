package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/publisher/GeneralChatV1FinalizerTest.kt
// Module: :feature:chat
// Summary: Unit tests for GeneralChatV1Finalizer seam behavior.
// Author: created on 2025-12-29

import org.junit.Assert.assertEquals
import org.junit.Test

class GeneralChatV1FinalizerTest {

    @Test
    fun `finalizer returns visible2user and valid artifact`() {
        val publisher = ChatPublisherImpl()
        val finalizer = GeneralChatV1Finalizer(publisher)
        val raw = """
            <visible2user>Hi</visible2user>
            ```json
            {"a":1}
            ```
        """.trimIndent()

        val result = finalizer.finalize(raw)

        assertEquals("Hi", result.visibleMarkdown)
        assertEquals(ArtifactStatus.VALID, result.artifactStatus)
        assertEquals("{\"a\":1}", result.artifactJson)
    }

    @Test
    fun `finalizer falls back when visible2user missing`() {
        val publisher = ChatPublisherImpl()
        val finalizer = GeneralChatV1Finalizer(publisher)
        val raw = """
            hello
            ```json
            {"a":1}
            ```
        """.trimIndent()

        val result = finalizer.finalize(raw)

        assertEquals(publisher.fallbackMessage(), result.visibleMarkdown)
        assertEquals(ArtifactStatus.VALID, result.artifactStatus)
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
