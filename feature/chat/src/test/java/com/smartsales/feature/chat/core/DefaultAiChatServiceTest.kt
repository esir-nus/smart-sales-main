package com.smartsales.feature.chat.core

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/core/DefaultAiChatServiceTest.kt
// 模块：:feature:chat
// 说明：验证 DefaultAiChatService 对数据层流事件的映射与 Prompt 构造
// 作者：创建于 2025-12-10

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService as DataAiChatService
import com.smartsales.data.aicore.AiChatStreamEvent as DataAiChatStreamEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAiChatServiceTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `maps chunk and completed events to feature stream`() = runTest(dispatcher) {
        val completedResponse = AiChatResponse(
            displayText = "display-only",
            structuredMarkdown = "structured-markdown",
            references = emptyList()
        )
        val fake = RecordingDataAiChatService(
            events = listOf(
                DataAiChatStreamEvent.Chunk("片段"),
                DataAiChatStreamEvent.Completed(completedResponse)
            )
        )
        val service = DefaultAiChatService(fake)

        val events = service.streamChat(
            ChatRequest(
                sessionId = "s-123",
                userMessage = "你好",
                history = listOf(ChatHistoryItem(ChatRole.USER, "历史问题")),
                persona = com.smartsales.feature.usercenter.SalesPersona(
                    role = "经理",
                    industry = "汽车",
                    mainChannel = "微信",
                    experienceLevel = "中级",
                    stylePreference = "跳跃"
                )
            )
        ).toList()

        val prompt = fake.lastRequest?.prompt.orEmpty()
        assertTrue(prompt.contains("历史对话："))
        assertTrue(prompt.contains("岗位：经理"))
        assertTrue(prompt.contains("行业：汽车"))
        assertFalse(prompt.contains("你所在的行业"))
        assertTrue(events.first() is ChatStreamEvent.Delta)
        val completed = events.filterIsInstance<ChatStreamEvent.Completed>().first()
        assertEquals("structured-markdown", completed.fullText)
    }

    @Test
    fun `maps error events to feature stream`() = runTest(dispatcher) {
        val fake = RecordingDataAiChatService(
            events = listOf(DataAiChatStreamEvent.Error(IllegalStateException("boom")))
        )
        val service = DefaultAiChatService(fake)

        val events = service.streamChat(
            ChatRequest(sessionId = "s-err", userMessage = "test")
        ).toList()

        assertTrue(events.single() is ChatStreamEvent.Error)
    }

    private class RecordingDataAiChatService(
        private val events: List<DataAiChatStreamEvent>
    ) : DataAiChatService {
        var lastRequest: AiChatRequest? = null

        override suspend fun sendMessage(request: AiChatRequest): Result<AiChatResponse> {
            return Result.Error(IllegalStateException("sendMessage not used"))
        }

        override fun streamMessage(request: AiChatRequest): Flow<DataAiChatStreamEvent> = flow {
            lastRequest = request
            events.forEach { emit(it) }
        }
    }
}
