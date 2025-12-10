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
        assertTrue("cleaned: $cleaned", cleaned.contains("长版本"))
        assertFalse("cleaned: $cleaned", cleaned.contains("######"))
        assertTrue("cleaned: $cleaned", cleaned.contains("## 会话概要"))
        assertTrue("cleaned: $cleaned", cleaned.contains("## 核心洞察"))
        assertFalse("cleaned: $cleaned", cleaned.contains("短版本"))
        assertFalse("cleaned: $cleaned", cleaned.contains("{"))
    }

    @Test
    fun `smart markdown normalizes salesperson first person to assistant voice`() = runTest(dispatcher) {
        val aiChatService = CompletedAiChatService(
            """
            ```json
            {
              "short_summary": "我向客户介绍了新方案并确认预算",
              "highlights": ["我解答了客户的疑问", "我将提供详细报价"],
              "actionable_tips": ["我会安排后续技术评估"],
              "core_insight": "我需要跟进报价",
              "sharp_line": "我会尽快回复"
            }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, RecordingMetaHub())

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "pov",
                userMessage = "smart",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val cleaned = (events.first() as ChatStreamEvent.Completed).fullText
        // 确保不会输出"我向.../我会..."等第一人称
        assertFalse("should not contain 我向: $cleaned", cleaned.contains("我向"))
        assertFalse("should not contain 我会: $cleaned", cleaned.contains("我会"))
        assertFalse("should not contain 我将: $cleaned", cleaned.contains("我将"))
        assertFalse("should not contain 我需要: $cleaned", cleaned.contains("我需要"))
        // 验证已转换为"你"视角
        assertTrue("should contain 你向: $cleaned", cleaned.contains("你向客户"))
        assertTrue("should contain 你解答了: $cleaned", cleaned.contains("你解答了"))
        assertTrue("should contain 你会安排: $cleaned", cleaned.contains("你会安排"))
        assertTrue("should contain 你将提供: $cleaned", cleaned.contains("你将提供"))
    }

    @Test
    fun `POV validation ensures no first person in final markdown`() = runTest(dispatcher) {
        // 测试从包含各种"我…"开头的 SMART JSON 到最终 Markdown 的人称规范
        val aiChatService = CompletedAiChatService(
            """
            ```json
            {
              "short_summary": "我在会议中向客户展示了产品优势",
              "highlights": ["我给客户提供了详细报价", "我在沟通中明确了交付周期"],
              "actionable_tips": ["我会跟进客户反馈", "我将安排技术演示"],
              "core_insight": "我需要在价格和交付之间找到平衡",
              "sharp_line": "我会尽快给出最终方案"
            }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, RecordingMetaHub())

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "pov-validation",
                userMessage = "analyze",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val markdown = (events.first() as ChatStreamEvent.Completed).fullText
        // 确保最终 Markdown 中不会出现任何第一人称表达
        assertFalse("should not contain 我向: $markdown", markdown.contains("我向"))
        assertFalse("should not contain 我会: $markdown", markdown.contains("我会"))
        assertFalse("should not contain 我将: $markdown", markdown.contains("我将"))
        assertFalse("should not contain 我需要: $markdown", markdown.contains("我需要"))
        assertFalse("should not contain 我在: $markdown", markdown.contains("我在"))
        assertFalse("should not contain 我给: $markdown", markdown.contains("我给"))
        // 验证所有"我"开头的内容都已转换为"你"视角
        assertTrue("should contain 你在: $markdown", markdown.contains("你在"))
        assertTrue("should contain 你给: $markdown", markdown.contains("你给"))
        assertTrue("should contain 你会: $markdown", markdown.contains("你会"))
        assertTrue("should contain 你将: $markdown", markdown.contains("你将"))
        assertTrue("should contain 你需要: $markdown", markdown.contains("你需要"))
        // 验证具体转换结果
        assertTrue("should contain converted summary: $markdown", markdown.contains("你在会议中向客户展示了产品优势"))
        assertTrue("should contain converted highlight: $markdown", markdown.contains("你给客户提供了详细报价"))
        assertTrue("should contain converted tip: $markdown", markdown.contains("你会跟进客户反馈"))
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

    @Test
    fun `smart markdown caps bullets and dedupes per section`() = runTest(dispatcher) {
        val aiChatService = CompletedAiChatService(
            """
            ```json
            {
              "short_summary": "本次沟通主要聚焦价格和交付",
              "highlights": [
                "客户关心价格",
                "客户关心价格",
                "价格敏感度高",
                "价格敏感度高",
                "预算有限",
                "预算有限",
                "交付周期紧张",
                "交付周期紧张"
              ],
              "actionable_tips": [
                "1) 提升进店转化",
                "3) 提升成交率",
                "4)4) 重复编号"
              ]
            }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, RecordingMetaHub())

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "dedupe",
                userMessage = "go",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val markdown = (events.first() as ChatStreamEvent.Completed).fullText
        val painPoints = extractSectionLines(markdown, "需求与痛点")
        val bullets = painPoints.filter { it.trim().startsWith("-") }
        assertTrue("too many bullets: ${bullets.size}", bullets.size <= 5)
        val normalized = bullets.map { line ->
            line.removePrefix("-").trim().lowercase().replace(Regex("[\\p{Punct}\\s]+"), "")
        }
        assertEquals("duplicates still exist: $bullets", normalized.toSet().size, normalized.size)
    }

    @Test
    fun `smart markdown normalizes messy numbering`() = runTest(dispatcher) {
        val aiChatService = CompletedAiChatService(
            """
            ```json
            {
              "short_summary": "概要",
              "highlights": ["痛点一"],
              "actionable_tips": [
                "1) 提升进店转化",
                "3) 提升成交率",
                "4)4) 重复编号"
              ]
            }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, RecordingMetaHub())

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "numbering",
                userMessage = "go",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val markdown = (events.first() as ChatStreamEvent.Completed).fullText
        val tips = extractSectionLines(markdown, "建议与行动")
        val numbered = tips.filter { it.trim().matches(Regex("^\\d+\\.\\s.+")) }
        assertEquals(listOf("1. 提升进店转化", "2. 提升成交率", "3. 重复编号"), numbered)
    }

    @Test
    fun `smart markdown hides empty sections`() = runTest(dispatcher) {
        val aiChatService = CompletedAiChatService(
            """
            ```json
            {
              "short_summary": "只保留概要",
              "highlights": ["暂无", "无明显"],
              "actionable_tips": []
            }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, RecordingMetaHub())

        val events = mutableListOf<ChatStreamEvent>()
        orchestrator.streamChat(
            ChatRequest(
                sessionId = "empty-section",
                userMessage = "go",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { events.add(it) }

        val markdown = (events.first() as ChatStreamEvent.Completed).fullText
        assertTrue(markdown.contains("## 会话概要"))
        assertFalse(markdown.contains("需求与痛点"))
        assertFalse(markdown.contains("建议与行动"))
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

    private fun extractSectionLines(markdown: String, heading: String): List<String> {
        val lines = markdown.lines()
        val collected = mutableListOf<String>()
        var capture = false
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("## ")) {
                capture = trimmed == "## $heading"
                return@forEach
            }
            if (capture) {
                collected.add(line)
            }
        }
        return collected.filter { it.isNotBlank() }
    }
}
