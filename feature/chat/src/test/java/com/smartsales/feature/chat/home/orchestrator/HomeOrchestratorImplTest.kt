package com.smartsales.feature.chat.home.orchestrator

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImplTest.kt
// 模块：:feature:chat
// 说明：验证 Orchestrator 在 SMART_ANALYSIS 下解析 JSON 并写入 MetaHub
// 作者：创建于 2025-12-04

import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.TokenUsage
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeOrchestratorImplTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `smart analysis parses metadata and keeps completed event`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        val aiChatService = CompletedAiChatService(
            """
            ## 分析
            内容
            ```json
            {
              "main_person": "罗总",
              "short_summary": "会议纪要",
              "summary_title_6chars": "报价跟进",
              "location": "北京",
              "highlights": ["报价讨论"],
              "actionable_tips": ["跟进报价"],
              "summary": {
                "core_insight": "客户关注价格",
                "sharp_line": "锁定价格优势"
              }
            }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "session-1",
                userMessage = "请做智能分析",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        assertEquals(1, events.size)
        val completed = events.first() as ChatStreamEvent.Completed
        assertTrue(completed.fullText.contains("罗总"))

        val stored = metaHub.lastSession
        assertEquals("罗总", stored?.mainPerson)
        assertEquals("报价跟进", stored?.summaryTitle6Chars)
        assertTrue(stored?.tags?.contains("报价讨论") == true)
    }

    private class CompletedAiChatService(
        private val response: String
    ) : AiChatService {
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flow {
            emit(ChatStreamEvent.Completed(response))
        }
    }

    private class RecordingMetaHub : MetaHub {
        var lastSession: SessionMetadata? = null
        override suspend fun upsertSession(metadata: SessionMetadata) {
            lastSession = metadata
        }

        override suspend fun getSession(sessionId: String): SessionMetadata? = lastSession
        override suspend fun upsertTranscript(metadata: TranscriptMetadata) {}
        override suspend fun getTranscriptBySession(sessionId: String): TranscriptMetadata? = null
        override suspend fun upsertExport(metadata: ExportMetadata) {}
        override suspend fun getExport(sessionId: String): ExportMetadata? = null
        override suspend fun logUsage(usage: TokenUsage) {}
    }
}
