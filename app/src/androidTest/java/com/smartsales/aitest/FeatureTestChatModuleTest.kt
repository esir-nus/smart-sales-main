package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/FeatureTestChatModuleTest.kt
// 模块：:app
// 说明：验证测试替换模块返回的假 AiChatService 能输出非空回复
// 作者：创建于 2025-12-10

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureTestChatModuleTest {

    @Test
    fun generalChatReturnsNonEmptyReply() = runBlocking {
        val service = FeatureTestChatModule.provideAiChatService()
        val events = service.streamChat(
            ChatRequest(
                sessionId = "s-1",
                userMessage = "你好，帮我总结一下今天的会话"
            )
        ).toList()

        val completed = events.filterIsInstance<ChatStreamEvent.Completed>()
        assertTrue(completed.isNotEmpty())
        assertTrue(completed.first().fullText.isNotBlank())
    }
}
