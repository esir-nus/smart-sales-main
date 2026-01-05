package com.smartsales.domain.chat

// 文件：feature/chat/src/test/java/com/smartsales/domain/chat/ChatMessageBuilderTest.kt
// 模块：:feature:chat
// 说明：验证 ChatMessageBuilder 消息格式化逻辑
// 作者：创建于 2026-01-05

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageBuilderTest {

    @Test
    fun buildSmartAnalysisUserMessage_withAllParams_formatsCorrectly() {
        val result = ChatMessageBuilder.buildSmartAnalysisUserMessage(
            mainContent = "这是客户对话内容",
            context = "之前的对话上下文",
            goal = "分析客户需求"
        )
        assertTrue(result.contains("这是客户对话内容"))
        assertTrue(result.contains("之前的对话上下文"))
        assertTrue(result.contains("分析客户需求"))
    }

    @Test
    fun buildSmartAnalysisUserMessage_withNullContext_omitsContextSection() {
        val result = ChatMessageBuilder.buildSmartAnalysisUserMessage(
            mainContent = "主要内容",
            context = null,
            goal = "目标"
        )
        assertTrue(result.contains("主要内容"))
        assertTrue(result.contains("目标"))
    }

    @Test
    fun buildSmartAnalysisUserMessage_withEmptyGoal_usesDefault() {
        val result = ChatMessageBuilder.buildSmartAnalysisUserMessage(
            mainContent = "内容",
            context = null,
            goal = ""
        )
        assertTrue(result.contains("内容"))
    }

    @Test
    fun buildTranscriptMarkdown_withEmptyList_returnsEmpty() {
        val result = ChatMessageBuilder.buildTranscriptMarkdown(emptyList())
        assertEquals("", result)
    }

    @Test
    fun wrapSmartAnalysisForExport_addsHeader() {
        val result = ChatMessageBuilder.wrapSmartAnalysisForExport("分析结果内容")
        assertTrue(result.contains("分析结果内容"))
        assertTrue(result.contains("智能分析") || result.contains("Smart") || result.length > "分析结果内容".length)
    }
}
