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
    fun `smart analysis parses metadata and renders formatted markdown`() = runTest(dispatcher) {
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
        assertTrue(completed.fullText.contains("### 会话概要"))
        assertTrue(completed.fullText.contains("### 建议与下一步行动"))
        assertTrue(completed.fullText.contains("1)"))

        val stored = metaHub.lastSession
        assertEquals("罗总", stored?.mainPerson)
        assertEquals("报价跟进", stored?.summaryTitle6Chars)
        assertTrue(stored?.tags?.contains("报价讨论") == true)
    }

    @Test
    fun `uses last json block when multiple fenced blocks exist`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        val aiChatService = CompletedAiChatService(
            """
            ```json
            { "main_person": "客户一", "summary_title_6chars": "标题一" }
            ```
            无关文本
            ```json
            { "main_person": "客户二", "summary_title_6chars": "标题二" }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        orchestrator.streamChat(
            ChatRequest(
                sessionId = "s-first",
                userMessage = "hi",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { /* no-op */ }

        assertEquals("客户二", metaHub.lastSession?.mainPerson)
        assertEquals("标题二", metaHub.lastSession?.summaryTitle6Chars)
    }

    @Test
    fun `parses minimal metadata even when optional blocks missing`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        val aiChatService = CompletedAiChatService(
            """
            结果如下：
            ```json
            { "main_person": "王总" }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "s-min",
                userMessage = "hi",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        assertEquals("王总", metaHub.lastSession?.mainPerson)
        val completed = events.first() as ChatStreamEvent.Completed
        assertTrue(completed.fullText.contains("王总"))
    }

    @Test
    fun `smart analysis formatter removes progressive scaffolding and renumbers list`() = runTest(dispatcher) {
        val messy = """
            ###### 客## 客户## 客户画像
            客户对产品有兴趣
            客户对产品有兴趣，询问价格和交付周期。
            1) 初步介绍
            3) 提供报价
            4) 跟进确认
            ```json
            {
              "short_summary": "短版本",
              "short_summary": "长版本",
              "highlights": ["a"],
              "highlights": ["a","b"],
              "summary": {
                "core_insight": "短",
                "core_insight": "长"
              }
            }
            ```
        """.trimIndent()
        val aiChatService = CompletedAiChatService(messy)
        val orchestrator = HomeOrchestratorImpl(aiChatService, RecordingMetaHub())

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "clean-1",
                userMessage = "go",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val completed = events.first() as ChatStreamEvent.Completed
        val cleaned = completed.fullText
        assertTrue(cleaned.contains("长版本"))
        assertTrue(!cleaned.contains("###### 客"))
        assertTrue(cleaned.contains("1)"))
        assertTrue(cleaned.contains("2)"))
        assertTrue(cleaned.contains("3)"))
        assertTrue(!cleaned.contains("短版本"))
    }

    @Test
    fun `metadata parser prefers last value when duplicates present`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        val aiChatService = CompletedAiChatService(
            """
            ```json
            { "short_summary": "短版本", "highlights": ["a"] }
            ```
            ```json
            { "short_summary": "长版本", "highlights": ["a","b"] }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        orchestrator.streamChat(
            ChatRequest(
                sessionId = "dup",
                userMessage = "hi",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { /* no-op */ }

        assertEquals("长版本", metaHub.lastSession?.shortSummary)
        assertEquals(listOf("a", "b").toSet(), metaHub.lastSession?.tags)
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
