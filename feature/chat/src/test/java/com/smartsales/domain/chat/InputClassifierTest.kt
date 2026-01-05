package com.smartsales.domain.chat

// 文件：feature/chat/src/test/java/com/smartsales/domain/chat/InputClassifierTest.kt
// 模块：:feature:chat
// 说明：验证 InputClassifier 用户输入分类逻辑
// 作者：创建于 2026-01-05

import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputClassifierTest {

    // ===== classifyUserInput tests =====
    
    @Test
    fun classifyUserInput_emptyText_returnsNoise() {
        val result = InputClassifier.classifyUserInput("", emptyList())
        assertEquals(InputBucket.NOISE, result)
    }

    @Test
    fun classifyUserInput_shortTextNoKeyword_returnsNoise() {
        val result = InputClassifier.classifyUserInput("你好", emptyList())
        assertEquals(InputBucket.NOISE, result)
    }

    @Test
    fun classifyUserInput_withSalesKeyword_returnsRelevant() {
        val result = InputClassifier.classifyUserInput("客户需要报价", emptyList())
        assertEquals(InputBucket.SHORT_RELEVANT, result)
    }

    @Test
    fun classifyUserInput_longContent_returnsRich() {
        val longText = "这是一段很长的客户对话内容，" + "x".repeat(300)
        val result = InputClassifier.classifyUserInput(longText, emptyList())
        assertEquals(InputBucket.RICH, result)
    }

    // ===== isLowInfoAnalysisInput tests =====

    @Test
    fun isLowInfoAnalysisInput_empty_returnsTrue() {
        assertTrue(InputClassifier.isLowInfoAnalysisInput(""))
    }

    @Test
    fun isLowInfoAnalysisInput_greeting_returnsTrue() {
        assertTrue(InputClassifier.isLowInfoAnalysisInput("你好"))
        assertTrue(InputClassifier.isLowInfoAnalysisInput("hello"))
    }

    @Test
    fun isLowInfoAnalysisInput_filler_returnsTrue() {
        assertTrue(InputClassifier.isLowInfoAnalysisInput("分析"))
        assertTrue(InputClassifier.isLowInfoAnalysisInput("看看"))
    }

    @Test
    fun isLowInfoAnalysisInput_meaningfulInput_returnsFalse() {
        assertFalse(InputClassifier.isLowInfoAnalysisInput("分析客户的购买意向"))
    }

    // ===== isLowInfoGeneralChatInput tests =====

    @Test
    fun isLowInfoGeneralChatInput_blank_returnsTrue() {
        assertTrue(InputClassifier.isLowInfoGeneralChatInput(""))
        assertTrue(InputClassifier.isLowInfoGeneralChatInput("   "))
    }

    @Test
    fun isLowInfoGeneralChatInput_acknowledgement_returnsTrue() {
        assertTrue(InputClassifier.isLowInfoGeneralChatInput("好的"))
        assertTrue(InputClassifier.isLowInfoGeneralChatInput("嗯"))
        assertTrue(InputClassifier.isLowInfoGeneralChatInput("ok"))
    }

    @Test
    fun isLowInfoGeneralChatInput_question_returnsFalse() {
        assertFalse(InputClassifier.isLowInfoGeneralChatInput("这个价格合适吗"))
    }

    // ===== findSmartAnalysisPrimaryContent tests =====

    @Test
    fun findSmartAnalysisPrimaryContent_longInput_returnsUserInput() {
        val longInput = "x".repeat(300)
        val result = InputClassifier.findSmartAnalysisPrimaryContent(longInput, emptyList())
        assertNotNull(result)
        assertEquals("user_input", result?.source)
    }

    @Test
    fun findSmartAnalysisPrimaryContent_shortInputNoHistory_returnsNull() {
        val result = InputClassifier.findSmartAnalysisPrimaryContent("短", emptyList())
        assertNull(result)
    }

    @Test
    fun findSmartAnalysisPrimaryContent_withTranscript_returnsTranscript() {
        val transcript = ChatMessageUi(
            content = "这是一段很长的通话转写内容，" + "x".repeat(300),
            role = ChatMessageRole.ASSISTANT,
            timestampMillis = 0L
        )
        val result = InputClassifier.findSmartAnalysisPrimaryContent("分析", listOf(transcript))
        assertNotNull(result)
        assertEquals("transcript", result?.source)
    }

    // ===== findContextForAnalysis tests =====

    @Test
    fun findContextForAnalysis_emptyHistory_returnsNull() {
        val result = InputClassifier.findContextForAnalysis("主内容", emptyList())
        assertNull(result)
    }

    @Test
    fun findContextForAnalysis_withHistory_returnsContext() {
        val messages = listOf(
            ChatMessageUi(content = "这是一条历史消息", role = ChatMessageRole.USER, timestampMillis = 0L),
            ChatMessageUi(content = "这是助手回复", role = ChatMessageRole.ASSISTANT, timestampMillis = 0L)
        )
        val result = InputClassifier.findContextForAnalysis("主要分析内容", messages)
        assertNotNull(result)
        assertTrue(result!!.contains("历史消息") || result.contains("助手回复"))
    }
}
