package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/publisher/ChatPublisherImplTest.kt
// Module: :feature:chat
// Summary: Unit tests for deterministic V1 chat publisher extraction.
// Author: created on 2025-12-29

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPublisherImplTest {

    private val publisher = ChatPublisherImpl()
    private val validArtifactJson =
        """{"artifactType":"MachineArtifact","schemaVersion":1,"mode":"L3","provenance":{"chatSessionId":"cs1","turnId":"t1","createdAtMs":0}}"""
    private val validArtifactJsonAlt =
        """{"artifactType":"MachineArtifact","schemaVersion":1,"mode":"L3","provenance":{"chatSessionId":"cs2","turnId":"t2","createdAtMs":1}}"""
    private val invalidArtifactMissingProvenance =
        """{"artifactType":"MachineArtifact","schemaVersion":1,"mode":"L3"}"""
    @Test
    fun `happy path extracts visible2user and json fence`() {
        val raw = """
            <visible2user>Hello</visible2user>
            ```json
            $validArtifactJson
            ```
        """.trimIndent()

        val result = publisher.publish(raw)

        assertEquals("Hello", result.displayMarkdown)
        assertEquals(validArtifactJson, result.machineArtifactJson)
        assertEquals(ArtifactStatus.VALID, result.artifactStatus)
        assertNull(result.failureReason)
    }

    @Test
    fun `first complete visible2user wins`() {
        val raw = """
            <visible2user>First</visible2user>
            <visible2user>Second</visible2user>
            ```json
            $validArtifactJson
            ```
        """.trimIndent()

        val result = publisher.publish(raw)

        assertEquals("First", result.displayMarkdown)
        assertEquals(ArtifactStatus.VALID, result.artifactStatus)
    }

    @Test
    fun `json fence inside visible2user is ignored`() {
        val raw = """
            <visible2user>
            Hello
            ```json
            $validArtifactJson
            ```
            </visible2user>
        """.trimIndent()

        val result = publisher.publish(raw)

        assertTrue(result.displayMarkdown.contains("Hello"))
        assertNull(result.machineArtifactJson)
        assertEquals(ArtifactStatus.INVALID, result.artifactStatus)
        assertEquals("missing_json_fence", result.failureReason)
    }

    @Test
    fun `missing visible2user yields invalid`() {
        val raw = """
            Hello
            ```json
            $validArtifactJson
            ```
        """.trimIndent()

        val result = publisher.publish(raw)

        assertEquals("", result.displayMarkdown)
        assertEquals(ArtifactStatus.INVALID, result.artifactStatus)
        assertEquals("missing_visible2user", result.failureReason)
    }

    @Test
    fun `malformed visible2user yields invalid`() {
        val raw = """
            <visible2user>Hello
            ```json
            $validArtifactJson
            ```
        """.trimIndent()

        val result = publisher.publish(raw)

        assertEquals("", result.displayMarkdown)
        assertEquals(validArtifactJson, result.machineArtifactJson)
        assertEquals(ArtifactStatus.INVALID, result.artifactStatus)
        assertEquals("malformed_visible2user", result.failureReason)
    }

    @Test
    fun `l3 missing json fence yields invalid`() {
        val raw = """
            <visible2user>Hello</visible2user>
        """.trimIndent()

        val result = publisher.publish(raw)

        assertEquals("Hello", result.displayMarkdown)
        assertNull(result.machineArtifactJson)
        assertEquals(ArtifactStatus.INVALID, result.artifactStatus)
        assertEquals("missing_json_fence", result.failureReason)
    }

    @Test
    fun `l1 missing json fence does not invalidate`() {
        val raw = """
            <visible2user>Hello</visible2user>
        """.trimIndent()

        val result = publisher.publish(raw)

        assertEquals("Hello", result.displayMarkdown)
        assertNull(result.machineArtifactJson)
        assertEquals(ArtifactStatus.INVALID, result.artifactStatus)
        assertEquals("missing_json_fence", result.failureReason)
    }

    @Test
    fun `multiple fences choose first outside visible2user`() {
        val raw = """
            ```json
            $validArtifactJson
            ```
            <visible2user>Hello</visible2user>
            ```json
            $validArtifactJsonAlt
            ```
        """.trimIndent()

        val result = publisher.publish(raw)

        assertEquals(validArtifactJson, result.machineArtifactJson)
        assertEquals(ArtifactStatus.VALID, result.artifactStatus)
    }

    @Test
    fun `invalid artifact missing provenance yields invalid`() {
        val raw = """
            <visible2user>Hello</visible2user>
            ```json
            $invalidArtifactMissingProvenance
            ```
        """.trimIndent()

        val result = publisher.publish(raw)

        assertEquals(ArtifactStatus.INVALID, result.artifactStatus)
        assertEquals(
            MachineArtifactValidator.REASON_MISSING_REQUIRED_FIELD,
            result.failureReason
        )
    }
}
