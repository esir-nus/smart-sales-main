package com.smartsales.domain.chat

// 文件：feature/chat/src/test/java/com/smartsales/domain/chat/ChatPublisherTest.kt
// 模块：:feature:chat
// 说明：验证 ChatPublisher V1对齐的发布逻辑
// 作者：创建于 2026-01-05

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPublisherTest {

    // ===== extractHumanDraft tests =====

    @Test
    fun extractHumanDraft_withValidTag_extractsContent() {
        val raw = "prefix <visible2user>这是用户可见内容</visible2user> suffix"
        val result = ChatPublisher.extractHumanDraft(raw)
        assertEquals("这是用户可见内容", result)
    }

    @Test
    fun extractHumanDraft_withoutTag_returnsNull() {
        val raw = "这里没有标签"
        val result = ChatPublisher.extractHumanDraft(raw)
        assertNull(result)
    }

    @Test
    fun extractHumanDraft_caseInsensitive_works() {
        val raw = "<VISIBLE2USER>大写标签</VISIBLE2USER>"
        val result = ChatPublisher.extractHumanDraft(raw)
        assertEquals("大写标签", result)
    }

    @Test
    fun extractHumanDraft_emptyContent_returnsNull() {
        val raw = "<visible2user>   </visible2user>"
        val result = ChatPublisher.extractHumanDraft(raw)
        assertNull(result)
    }

    // ===== extractMachineArtifact tests =====

    @Test
    fun extractMachineArtifact_withFencedJson_extractsObject() {
        val raw = """
            <visible2user>用户内容</visible2user>
            ```json
            {"schemaVersion": 1, "mode": "L3"}
            ```
        """.trimIndent()
        val result = ChatPublisher.extractMachineArtifact(raw)
        assertNotNull(result)
        assertEquals(1, result?.getInt("schemaVersion"))
        assertEquals("L3", result?.getString("mode"))
    }

    @Test
    fun extractMachineArtifact_withoutFencedBlock_returnsNull() {
        val raw = """<visible2user>内容</visible2user> {"mode": "L1"}"""
        val result = ChatPublisher.extractMachineArtifact(raw)
        // V1 contract: Must be in fenced block, heuristic extraction forbidden
        assertNull(result)
    }

    @Test
    fun extractMachineArtifact_invalidJson_returnsNull() {
        val raw = """
            <visible2user>内容</visible2user>
            ```json
            {not valid json}
            ```
        """.trimIndent()
        val result = ChatPublisher.extractMachineArtifact(raw)
        assertNull(result)
    }

    // ===== validateArtifact tests =====

    @Test
    fun validateArtifact_withRequiredFields_returnsTrue() {
        val artifact = org.json.JSONObject("""{"schemaVersion": 1, "mode": "L3"}""")
        assertTrue(ChatPublisher.validateArtifact(artifact))
    }

    @Test
    fun validateArtifact_missingMode_returnsFalse() {
        val artifact = org.json.JSONObject("""{"schemaVersion": 1}""")
        assertEquals(false, ChatPublisher.validateArtifact(artifact))
    }

    // ===== publish tests =====

    @Test
    fun publish_validResponse_returnsValidStatus() {
        val raw = """
            <visible2user>用户可见内容</visible2user>
            ```json
            {"schemaVersion": 1, "mode": "L3"}
            ```
        """.trimIndent()
        val result = ChatPublisher.publish(raw)
        assertEquals("用户可见内容", result.displayMarkdown)
        assertEquals("L3", result.mode)
        assertEquals(ChatPublisher.ArtifactStatus.VALID, result.artifactStatus)
    }

    @Test
    fun publish_noArtifact_returnsInvalidStatus() {
        val raw = "<visible2user>只有可见内容</visible2user>"
        val result = ChatPublisher.publish(raw)
        assertEquals("只有可见内容", result.displayMarkdown)
        assertEquals(ChatPublisher.ArtifactStatus.INVALID, result.artifactStatus)
    }

    @Test
    fun publish_fallbackToRaw_whenNoTag() {
        val raw = "裸文本没有标签"
        val result = ChatPublisher.publish(raw, fallbackToRaw = true)
        assertEquals("裸文本没有标签", result.displayMarkdown)
    }

    // ===== extractDisplayText tests =====

    @Test
    fun extractDisplayText_withTag_extractsVisible() {
        val raw = "<visible2user>可见</visible2user>"
        assertEquals("可见", ChatPublisher.extractDisplayText(raw))
    }

    @Test
    fun extractDisplayText_withoutTag_fallsBackToRaw() {
        val raw = "原始文本"
        assertEquals("原始文本", ChatPublisher.extractDisplayText(raw))
    }
}
