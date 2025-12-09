package com.smartsales.feature.chat.home.orchestrator

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
// 模块：:feature:chat
// 说明：Home 层 Orchestrator 实现，当前直通聊天服务并写入会话元数据
// 作者：创建于 2025-12-04

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionStage
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

private const val SMART_ANALYSIS_FAILURE_MESSAGE = "本次智能分析暂时不可用，请稍后重试。"

private val SHORT_SUMMARY_REGEX =
    Regex("\"short_summary\"\\s*:\\s*\"([^\"]+)\"")

private data class SmartAnalysisResult(
    val markdown: String,
    val metadata: SessionMetadata?
)

private data class ParsedSmartAnalysis(
    val mainPerson: String?,
    val shortSummary: String?,
    val summaryTitle6Chars: String?,
    val location: String?,
    val stage: SessionStage?,
    val riskLevel: RiskLevel?,
    val highlights: List<String>,
    val actionableTips: List<String>,
    val coreInsight: String?,
    val sharpLine: String?
)

@Singleton
class HomeOrchestratorImpl @Inject constructor(
    private val aiChatService: AiChatService,
    private val metaHub: MetaHub
) : HomeOrchestrator {
    override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
        return flow {
            aiChatService.streamChat(request).collect { event ->
                if (event is ChatStreamEvent.Completed && shouldParseMetadata(request)) {
                    val result = buildSmartAnalysisResult(request, event.fullText)
                    result.metadata?.let { runCatching { metaHub.upsertSession(it) } }
                    emit(ChatStreamEvent.Completed(result.markdown))
                } else {
                    emit(event)
                }
            }
        }
    }

    private suspend fun buildSmartAnalysisResult(
        request: ChatRequest,
        assistantText: String
    ): SmartAnalysisResult {
        val parsed = parseSmartAnalysisPayload(rawText = assistantText)
            ?: return SmartAnalysisResult(
                markdown = SMART_ANALYSIS_FAILURE_MESSAGE,
                metadata = null
            )
        val source = resolveAnalysisSource(request)
        val mergedMeta = buildMergedMetadata(
            sessionId = request.sessionId,
            parsed = parsed,
            source = source
        )
        val markdown = buildSmartAnalysisMarkdown(parsed)
        return SmartAnalysisResult(markdown = markdown, metadata = mergedMeta)
    }

    private suspend fun buildMergedMetadata(
        sessionId: String,
        parsed: ParsedSmartAnalysis,
        source: AnalysisSource?
    ): SessionMetadata {
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val base = existing ?: SessionMetadata(sessionId = sessionId)

        val newTags = buildSet {
            addAll(base.tags)
            addAll(parsed.highlights.filter { it.isNotBlank() })
            addAll(parsed.actionableTips.filter { it.isNotBlank() })
        }

        val now = System.currentTimeMillis()
        return base.copy(
            mainPerson = parsed.mainPerson ?: base.mainPerson,
            shortSummary = parsed.shortSummary ?: base.shortSummary,
            summaryTitle6Chars = parsed.summaryTitle6Chars ?: base.summaryTitle6Chars,
            location = parsed.location ?: base.location,
            stage = parsed.stage ?: base.stage,
            riskLevel = parsed.riskLevel ?: base.riskLevel,
            tags = newTags,
            latestMajorAnalysisSource = source,
            latestMajorAnalysisAt = now,
            lastUpdatedAt = now
        )
    }


    private fun shouldParseMetadata(request: ChatRequest): Boolean {
        val skillId = request.quickSkillId ?: return false
        return skillId == QuickSkillId.SMART_ANALYSIS.name
    }

    // SMART_ANALYSIS JSON 解析：只取最后一个有效 JSON 对象
    private fun parseSmartAnalysisPayload(rawText: String): ParsedSmartAnalysis? {
        val jsonObject = extractLastSmartJson(rawText) ?: return null

        // Replace optJSONObject("summary") with a safer string-based parse
        val summaryObj = runCatching {
            val rawSummary = jsonObject.optString("summary")
            if (!rawSummary.isNullOrBlank() && rawSummary.trim().startsWith("{")) {
                JSONObject(rawSummary)
            } else {
                null
            }
        }.getOrNull()

        val mainPerson = jsonObject.optString("main_person").takeIf { it.isNotBlank() }

        // 1. 先从 JSON 里拿一份（保留现有逻辑作为 fallback）
        val shortSummaryFromJson = jsonObject.optString("short_summary")
            .takeIf { it.isNotBlank() }

        // 2. 再直接从原始文本里找最后一个 "short_summary": "..."
        val shortSummaryFromText = SHORT_SUMMARY_REGEX
            .findAll(rawText)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }

        // 3. 优先使用"原始文本中的最后一个"，否则退回 JSON 的解析结果
        val shortSummary = (shortSummaryFromText ?: shortSummaryFromJson)
            ?.takeIf { it.isNotBlank() }

        val summaryTitle = jsonObject.optString("summary_title_6chars")
            .takeIf { it.isNotBlank() }
            ?.take(6)
        val location = jsonObject.optString("location").takeIf { it.isNotBlank() }

        val stage = jsonObject.optString("stage").takeIf { it.isNotBlank() }?.let { toStage(it) }
        val risk = jsonObject.optString("risk_level").takeIf { it.isNotBlank() }?.let { toRisk(it) }

        val highlights = jsonObject.optJSONArray("highlights")?.toStringList().orEmpty()
        val actionable = jsonObject.optJSONArray("actionable_tips")?.toStringList().orEmpty()

        val coreInsight = jsonObject.optString("core_insight").takeIf { it.isNotBlank() }
            ?: summaryObj?.optString("core_insight")?.takeIf { it.isNotBlank() }

        val sharpLine = jsonObject.optString("sharp_line").takeIf { it.isNotBlank() }
            ?: summaryObj?.optString("sharp_line")?.takeIf { it.isNotBlank() }

        val hasContent =
            listOf(mainPerson, shortSummary, summaryTitle, location, coreInsight, sharpLine)
                .any { !it.isNullOrBlank() } ||
            highlights.isNotEmpty() ||
            actionable.isNotEmpty() ||
            stage != null ||
            risk != null

        if (!hasContent) return null

        return ParsedSmartAnalysis(
            mainPerson = mainPerson,
            shortSummary = shortSummary,
            summaryTitle6Chars = summaryTitle,
            location = location,
            stage = stage,
            riskLevel = risk,
            highlights = highlights,
            actionableTips = actionable,
            coreInsight = coreInsight,
            sharpLine = sharpLine
        )
    }

    private fun extractLastSmartJson(text: String): JSONObject? {
        val candidates = mutableListOf<String>()
        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        fenced.findAll(text).forEach { match ->
            match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        }
        candidates.addAll(collectTopLevelJsonSlices(text))
        for (slice in candidates.asReversed()) {
            val obj = runCatching { JSONObject(slice) }.getOrNull()
            if (obj != null) return obj
        }
        return null
    }

    private fun collectTopLevelJsonSlices(text: String): List<String> {
        val startStack = ArrayDeque<Int>()
        val ranges = mutableListOf<IntRange>()
        text.forEachIndexed { index, ch ->
            when (ch) {
                '{' -> startStack.addLast(index)
                '}' -> if (startStack.isNotEmpty()) {
                    val start = startStack.removeLast()
                    if (startStack.isEmpty()) {
                        ranges.add(start..index)
                    }
                }
            }
        }
        return ranges.map { range ->
            text.substring(range.first, range.last + 1).trim()
        }
    }

    private fun toStage(value: String): SessionStage? = when (value.uppercase(Locale.getDefault())) {
        "DISCOVERY" -> SessionStage.DISCOVERY
        "NEGOTIATION" -> SessionStage.NEGOTIATION
        "PROPOSAL" -> SessionStage.PROPOSAL
        "CLOSING" -> SessionStage.CLOSING
        "POST_SALE", "POSTSALE", "POST-SALE" -> SessionStage.POST_SALE
        "UNKNOWN" -> SessionStage.UNKNOWN
        else -> null
    }

    private fun toRisk(value: String): RiskLevel? = when (value.uppercase(Locale.getDefault())) {
        "LOW" -> RiskLevel.LOW
        "MEDIUM" -> RiskLevel.MEDIUM
        "HIGH" -> RiskLevel.HIGH
        "UNKNOWN" -> RiskLevel.UNKNOWN
        else -> null
    }

    private fun formatStage(stage: SessionStage): String = when (stage) {
        SessionStage.DISCOVERY -> "探索"
        SessionStage.NEGOTIATION -> "谈判"
        SessionStage.PROPOSAL -> "方案/报价"
        SessionStage.CLOSING -> "成交推进"
        SessionStage.POST_SALE -> "售后"
        SessionStage.UNKNOWN -> "未知阶段"
    }

    private fun formatRisk(risk: RiskLevel): String = when (risk) {
        RiskLevel.LOW -> "低"
        RiskLevel.MEDIUM -> "中"
        RiskLevel.HIGH -> "高"
        RiskLevel.UNKNOWN -> "未知"
    }

    private fun resolveAnalysisSource(request: ChatRequest): AnalysisSource? {
        val mode = request.quickSkillId
        return when {
            mode == QuickSkillId.SMART_ANALYSIS.name -> AnalysisSource.SMART_ANALYSIS_USER
            mode == null && request.isFirstAssistantReply -> AnalysisSource.GENERAL_FIRST_REPLY
            else -> null
        }
    }

    private fun buildSmartAnalysisMarkdown(parsed: ParsedSmartAnalysis): String {
        // SMART_ANALYSIS 最终 Markdown 生成：完全由本地控制，避免 LLM 模板污染
        val sb = StringBuilder()
        parsed.shortSummary?.takeIf { it.isNotBlank() }?.let { summary ->
            sb.appendLine("## 会话概要")
            sb.appendLine("- ${summary.trim()}")
            sb.appendLine()
        }

        val personaLines = mutableListOf<String>()
        parsed.mainPerson?.takeIf { it.isNotBlank() }?.let { personaLines.add("- 主要联系人：${it.trim()}") }
        parsed.location?.takeIf { it.isNotBlank() }?.let { personaLines.add("- 所在地：${it.trim()}") }
        if (personaLines.isNotEmpty()) {
            sb.appendLine("## 客户画像与意图")
            personaLines.forEach { line -> sb.appendLine(line) }
            sb.appendLine()
        }

        if (parsed.highlights.isNotEmpty()) {
            sb.appendLine("## 需求与痛点")
            parsed.highlights.forEach { highlight ->
                if (highlight.isNotBlank()) {
                    sb.appendLine("- ${highlight.trim()}")
                }
            }
            sb.appendLine()
        }

        val riskLines = mutableListOf<String>()
        parsed.stage?.let { riskLines.add("- 销售阶段：${formatStage(it)}") }
        parsed.riskLevel?.let { riskLines.add("- 风险等级：${formatRisk(it)}") }
        if (riskLines.isNotEmpty()) {
            sb.appendLine("## 机会与风险")
            riskLines.forEach { line -> sb.appendLine(line) }
            sb.appendLine()
        }

        if (parsed.actionableTips.isNotEmpty()) {
            sb.appendLine("## 建议与行动")
            parsed.actionableTips.forEachIndexed { index, tip ->
                if (tip.isNotBlank()) {
                    sb.appendLine("${index + 1}. ${tip.trim()}")
                }
            }
            sb.appendLine()
        }

        parsed.coreInsight?.takeIf { it.isNotBlank() }?.let { insight ->
            sb.appendLine("## 核心洞察")
            sb.appendLine("- ${insight.trim()}")
            sb.appendLine()
        }

        parsed.sharpLine?.takeIf { it.isNotBlank() }?.let { line ->
            sb.appendLine("## 一句话话术")
            sb.appendLine("- ${line.trim()}")
            sb.appendLine()
        }

        return sb.toString().trimEnd().ifBlank { SMART_ANALYSIS_FAILURE_MESSAGE }
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            optString(i)?.takeIf { it.isNotBlank() }?.let { list.add(it.trim()) }
        }
        return list
    }
}
