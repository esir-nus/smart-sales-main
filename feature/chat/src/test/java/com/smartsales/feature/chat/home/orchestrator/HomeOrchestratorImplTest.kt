package com.smartsales.feature.chat.home.orchestrator

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImplTest.kt
// 模块：:feature:chat
// 说明：验证 Orchestrator 在 SMART_ANALYSIS 下解析 JSON 并写入 MetaHub
// 作者：创建于 2025-12-04

import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.TokenUsage
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
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
        assertTrue(completed.fullText.contains("## 会话概要"))
        assertTrue(completed.fullText.contains("## 建议与行动"))
        assertTrue(completed.fullText.contains("1."))
        assertFalse(completed.fullText.contains("{"))

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

    @Ignore("TODO[orchestrator-v4]: messy SMART_ANALYSIS payload currently falls back to failure message; formatter not applied yet.")
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
        assertFalse(cleaned.contains("######"))
        assertTrue(cleaned.contains("## 会话概要"))
        assertTrue(cleaned.contains("## 核心洞察"))
        assertFalse(cleaned.contains("短版本"))
        assertFalse(cleaned.contains("{"))
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

    @Test
    fun `malformed json returns failure and skips meta updates`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        val aiChatService = CompletedAiChatService("{ \"main_person\": \"客户\"") // 缺失大括号导致解析失败
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "broken",
                userMessage = "go",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val completed = events.first() as ChatStreamEvent.Completed
        assertEquals("本次智能分析暂时不可用，请稍后重试。", completed.fullText)
        assertEquals(null, metaHub.lastSession)
    }

    @Test
    fun `markdown omits empty sections and hides json braces`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        val aiChatService = CompletedAiChatService(
            """
            ```json
            { "sharp_line": "一句话话术" }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "omit",
                userMessage = "go",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val text = (events.first() as ChatStreamEvent.Completed).fullText
        assertFalse(text.contains("{"))
        assertFalse(text.contains("}"))
        assertFalse(text.contains("需求与痛点"))
        assertFalse(text.contains("建议与行动"))
        assertTrue(text.contains("## 一句话话术"))
    }

    @Test
    fun `returns friendly failure markdown when json is missing`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        val aiChatService = CompletedAiChatService("没有生成结构化结果")
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "no-json",
                userMessage = "go",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val completed = events.first() as ChatStreamEvent.Completed
        assertEquals("本次智能分析暂时不可用，请稍后重试。", completed.fullText)
        assertEquals(null, metaHub.lastSession)
    }

    @Test
    fun `actionable tips are renumbered locally`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        val aiChatService = CompletedAiChatService(
            """
            ```json
            {
              "main_person": "林总",
              "actionable_tips": ["跟进报价", "安排试驾", "回访决策人"]
            }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "tips",
                userMessage = "go",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val completed = events.first() as ChatStreamEvent.Completed
        val numbered = completed.fullText.lines()
            .map { it.trim() }
            .filter { it.matches(Regex("^\\d+\\.\\s.+")) }
        assertEquals(listOf("1. 跟进报价", "2. 安排试驾", "3. 回访决策人"), numbered)
    }

    @Test
    fun `partial smart analysis keeps existing metadata and adds tags`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        metaHub.lastSession = SessionMetadata(
            sessionId = "merge",
            mainPerson = "初始客户",
            shortSummary = "旧摘要",
            summaryTitle6Chars = "旧标题",
            stage = com.smartsales.core.metahub.SessionStage.NEGOTIATION,
            riskLevel = com.smartsales.core.metahub.RiskLevel.HIGH,
            tags = setOf("旧标签")
        )
        val aiChatService = CompletedAiChatService(
            """
            ```json
            {
              "summary_title_6chars": "新标题",
              "actionable_tips": ["新增行动"]
            }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        orchestrator.streamChat(
            ChatRequest(
                sessionId = "merge",
                userMessage = "go",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { /* no-op */ }

        val saved = metaHub.lastSession!!
        assertEquals("初始客户", saved.mainPerson)
        assertEquals("旧摘要", saved.shortSummary)
        assertEquals("新标题", saved.summaryTitle6Chars)
        assertEquals(com.smartsales.core.metahub.SessionStage.NEGOTIATION, saved.stage)
        assertEquals(com.smartsales.core.metahub.RiskLevel.HIGH, saved.riskLevel)
        assertTrue(saved.tags.contains("新增行动"))
        assertEquals(AnalysisSource.SMART_ANALYSIS_USER, saved.latestMajorAnalysisSource)
        assertTrue(saved.latestMajorAnalysisAt != null)
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
