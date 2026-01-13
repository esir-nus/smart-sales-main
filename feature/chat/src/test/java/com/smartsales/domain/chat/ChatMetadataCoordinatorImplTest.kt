package com.smartsales.domain.chat

// File: feature/chat/src/test/java/com/smartsales/domain/chat/ChatMetadataCoordinatorImplTest.kt
// Module: :feature:chat
// Summary: Tests for ChatMetadataCoordinatorImpl M2/M3 patch logic
// Author: created on 2026-01-13

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.feature.chat.core.publisher.ArtifactStatus
import com.smartsales.feature.chat.core.publisher.V1FinalizeResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ChatMetadataCoordinatorImplTest {

    private lateinit var metaHub: InMemoryMetaHub
    private lateinit var coordinator: ChatMetadataCoordinatorImpl

    @Before
    fun setup() {
        metaHub = InMemoryMetaHub()
        coordinator = ChatMetadataCoordinatorImpl(metaHub)
    }

    // ===== processGeneralChatMetadata tests =====

    @Test
    fun `processGeneralChatMetadata with valid JSON persists metadata`() = runTest {
        val json = """
            {
                "main_person": "李总",
                "short_summary": "讨论合同细节",
                "summary_title_6chars": "合同谈判"
            }
        """.trimIndent()

        val result = coordinator.processGeneralChatMetadata(
            sessionId = "session-001",
            rawFullText = "Some response",
            metadataJson = json
        )

        assertNotNull(result)
        assertEquals("李总", result!!.mainPerson)
        assertEquals("讨论合同细节", result.shortSummary)

        // Verify persisted to MetaHub
        val stored = metaHub.getSession("session-001")
        assertNotNull(stored)
        assertEquals("李总", stored!!.mainPerson)
    }

    @Test
    fun `processGeneralChatMetadata with invalid JSON returns null`() = runTest {
        val result = coordinator.processGeneralChatMetadata(
            sessionId = "session-002",
            rawFullText = "Response text",
            metadataJson = "not valid json"
        )

        assertNull(result)

        // Verify nothing persisted
        val stored = metaHub.getSession("session-002")
        assertNull(stored)
    }

    @Test
    fun `processGeneralChatMetadata extracts JSON from rawFullText when metadataJson is null`() = runTest {
        val rawText = """
            Here is my analysis of the conversation.
            
            ```json
            {
                "main_person": "王经理",
                "short_summary": "产品演示"
            }
            ```
        """.trimIndent()

        val result = coordinator.processGeneralChatMetadata(
            sessionId = "session-003",
            rawFullText = rawText,
            metadataJson = null
        )

        assertNotNull(result)
        assertEquals("王经理", result!!.mainPerson)
    }

    @Test
    fun `processGeneralChatMetadata with empty metadata returns null`() = runTest {
        val json = """{"unrelated_field": "value"}"""

        val result = coordinator.processGeneralChatMetadata(
            sessionId = "session-004",
            rawFullText = "Response",
            metadataJson = json
        )

        // Should return null because no meaningful fields
        assertNull(result)
    }

    @Test
    fun `processGeneralChatMetadata merges with existing session`() = runTest {
        // Pre-populate existing session
        val existing = SessionMetadata(
            sessionId = "session-005",
            mainPerson = "张三",
            location = "上海"
        )
        metaHub.upsertSession(existing)

        val json = """
            {
                "main_person": "李四",
                "short_summary": "新的摘要"
            }
        """.trimIndent()

        val result = coordinator.processGeneralChatMetadata(
            sessionId = "session-005",
            rawFullText = "Response",
            metadataJson = json
        )

        assertNotNull(result)
        // New value should be merged
        assertEquals("李四", result!!.mainPerson)
        assertEquals("新的摘要", result.shortSummary)
        // Existing location preserved (if mergeWith preserves non-null fields)
        val stored = metaHub.getSession("session-005")
        assertEquals("上海", stored?.location)
    }

    // ===== processV1GeneralChatMetadata tests =====

    @Test
    fun `processV1GeneralChatMetadata with VALID artifact writes metadata`() = runTest {
        val artifactJson = """
            {
                "mode": "L3",
                "metadataPatch": {
                    "main_person": "刘总",
                    "short_summary": "V1分析结果"
                }
            }
        """.trimIndent()

        val result = coordinator.processV1GeneralChatMetadata(
            sessionId = "session-v1-001",
            result = V1FinalizeResult(
                visibleMarkdown = "Some visible content",
                artifactStatus = ArtifactStatus.VALID,
                artifactJson = artifactJson
            )
        )

        assertNotNull(result)
        assertEquals("刘总", result!!.mainPerson)
    }

    @Test
    fun `processV1GeneralChatMetadata with INVALID artifact returns null`() = runTest {
        val result = coordinator.processV1GeneralChatMetadata(
            sessionId = "session-v1-002",
            result = V1FinalizeResult(
                visibleMarkdown = "Content",
                artifactStatus = ArtifactStatus.INVALID,
                artifactJson = """{"mode": "L3", "metadataPatch": {"main_person": "Test"}}"""
            )
        )

        assertNull(result)
    }

    @Test
    fun `processV1GeneralChatMetadata with non-L3 mode returns null`() = runTest {
        val result = coordinator.processV1GeneralChatMetadata(
            sessionId = "session-v1-003",
            result = V1FinalizeResult(
                visibleMarkdown = "Content",
                artifactStatus = ArtifactStatus.VALID,
                artifactJson = """{"mode": "L1", "metadataPatch": {"main_person": "Test"}}"""
            )
        )

        assertNull(result)
    }

    @Test
    fun `processV1GeneralChatMetadata with null result returns null`() = runTest {
        val result = coordinator.processV1GeneralChatMetadata(
            sessionId = "session-v1-004",
            result = null
        )

        assertNull(result)
    }

    // ===== persistLatestAnalysisMarker tests =====

    @Test
    fun `persistLatestAnalysisMarker updates analysis tracking fields`() = runTest {
        val result = coordinator.persistLatestAnalysisMarker(
            sessionId = "session-marker-001",
            source = AnalysisSource.GENERAL_FIRST_REPLY,
            messageId = "msg-123"
        )

        assertNotNull(result)
        assertEquals("msg-123", result!!.latestMajorAnalysisMessageId)
        assertEquals(AnalysisSource.GENERAL_FIRST_REPLY, result.latestMajorAnalysisSource)
        assertNotNull(result.latestMajorAnalysisAt)
    }

    @Test
    fun `persistLatestAnalysisMarker creates session if not exists`() = runTest {
        // No pre-existing session
        val result = coordinator.persistLatestAnalysisMarker(
            sessionId = "session-new",
            source = AnalysisSource.TINGWU,
            messageId = "msg-456"
        )

        assertNotNull(result)
        val stored = metaHub.getSession("session-new")
        assertNotNull(stored)
        assertEquals("msg-456", stored!!.latestMajorAnalysisMessageId)
    }
}
