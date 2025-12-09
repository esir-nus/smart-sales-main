package com.smartsales.feature.chat.home.orchestrator

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
// 模块：:feature:chat
// 说明：Home 层 Orchestrator 实现，当前直通聊天服务并写入会话元数据
// 作者：创建于 2025-12-04

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.CrmRow
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

private data class SmartAnalysisResult(
    val markdown: String,
    val metadata: SessionMetadata?
)

private data class ParsedSmartAnalysis(
    val metadata: SessionMetadata,
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
        val parsed = parseSmartAnalysisPayload(
            sessionId = request.sessionId,
            rawText = assistantText,
            source = resolveAnalysisSource(request)
        )
        val mergedMeta = parsed?.metadata?.let { mergeWithExisting(request.sessionId, it) }
        val markdown = if (parsed != null && mergedMeta != null) {
            buildSmartAnalysisMarkdown(
                meta = mergedMeta,
                highlights = parsed.highlights,
                actionableTips = parsed.actionableTips,
                coreInsight = parsed.coreInsight,
                sharpLine = parsed.sharpLine,
                fallbackRawText = assistantText
            )
        } else {
            cleanSmartAnalysisOutput(assistantText)
        }
        return SmartAnalysisResult(markdown = markdown, metadata = mergedMeta)
    }

    private suspend fun mergeWithExisting(
        sessionId: String,
        parsed: SessionMetadata
    ): SessionMetadata {
        // 从 MetaHub 读取已有的会话元数据
        val existing = metaHub.getSession(sessionId)

        // 使用 SessionMetadata.mergeWith 做"非空优先新值"的合并：
        // - 如果没有 existing，就直接用 parsed
        // - 如果有 existing，就让 existing.mergeWith(parsed)，保证新分析结果覆盖旧值
        return existing?.mergeWith(parsed) ?: parsed
    }

    private fun shouldParseMetadata(request: ChatRequest): Boolean {
        val skillId = request.quickSkillId ?: return false
        return skillId == QuickSkillId.SMART_ANALYSIS.name
    }

    private fun extractJsonBlock(text: String): String? {
        return firstJsonCodeBlock(text) ?: extractBareJsonObject(text)
    }

    private fun firstJsonCodeBlock(text: String): String? {
        // pattern like ```json\n...\n``` or ```\n...\n```
        val regex = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.MULTILINE)
        return regex.findAll(text).lastOrNull()?.groupValues?.getOrNull(1)?.trim()
    }

    private fun extractBareJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(start, i + 1).trim()
                    }
                }
            }
        }
        return null
    }

    private fun findStringField(text: String, field: String): String? {
        // 匹配："field": "value"
        val pattern = Regex(""""$field"\s*:\s*"([^"]*)"""")
        val match = pattern.findAll(text).lastOrNull() ?: return null
        val value = match.groupValues.getOrNull(1)?.trim()
        return value?.takeIf { it.isNotBlank() }
    }

    private fun findStringListField(text: String, field: String): List<String> {
        // 匹配："field": ["a", "b", ...]
        val arrayPattern = Regex(""""$field"\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
        val arrayMatch = arrayPattern.findAll(text).lastOrNull() ?: return emptyList()
        val inner = arrayMatch.groupValues.getOrNull(1) ?: return emptyList()

        val elementPattern = Regex(""""([^"]*)"""")
        return elementPattern.findAll(inner)
            .map { it.groupValues.getOrNull(1)?.trim().orEmpty() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun parseSmartAnalysisPayload(
        sessionId: String,
        rawText: String,
        source: AnalysisSource?
    ): ParsedSmartAnalysis? {
        // 先尝试从最后一个 fenced JSON 里找，如果失败再在整段文本里兜底
        val jsonSlice = firstJsonCodeBlock(rawText) ?: extractBareJsonObject(rawText) ?: rawText

        val mainPerson =
            findStringField(jsonSlice, "main_person")
                ?: findStringField(jsonSlice, "mainPerson")

        val shortSummary = findStringField(jsonSlice, "short_summary")
        val summaryTitle = findStringField(jsonSlice, "summary_title_6chars")
        val location = findStringField(jsonSlice, "location")

        val stage = findStringField(jsonSlice, "stage")?.let { toStage(it) }
        val risk = findStringField(jsonSlice, "risk_level")?.let { toRisk(it) }

        val highlights = findStringListField(jsonSlice, "highlights")
        val actionable = findStringListField(jsonSlice, "actionable_tips")
        val coreInsight = findStringField(jsonSlice, "core_insight")
        val sharpLine = findStringField(jsonSlice, "sharp_line")
        val crmRows = emptyList<CrmRow>() // 先不支持复杂嵌套结构，后续再扩展

        val tags = (highlights + actionable).filter { it.isNotBlank() }.toSet()

        // 如果完全解析不到任何有用字段，就视为无效结果
        if (mainPerson == null &&
            shortSummary == null &&
            summaryTitle == null &&
            location == null &&
            tags.isEmpty() &&
            coreInsight.isNullOrBlank() &&
            sharpLine.isNullOrBlank()
        ) {
            return null
        }

        val now = System.currentTimeMillis()
        val metadata = SessionMetadata(
            sessionId = sessionId,
            mainPerson = mainPerson,
            shortSummary = shortSummary,
            summaryTitle6Chars = summaryTitle,
            location = location,
            stage = stage,
            riskLevel = risk,
            tags = tags,
            lastUpdatedAt = now,
            latestMajorAnalysisMessageId = null,
            latestMajorAnalysisAt = now,
            latestMajorAnalysisSource = source,
            crmRows = crmRows
        )
        return ParsedSmartAnalysis(
            metadata = metadata,
            highlights = highlights,
            actionableTips = actionable,
            coreInsight = coreInsight,
            sharpLine = sharpLine
        )
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

    private fun resolveAnalysisSource(request: ChatRequest): AnalysisSource? {
        val mode = request.quickSkillId
        return when {
            mode == QuickSkillId.SMART_ANALYSIS.name -> AnalysisSource.SMART_ANALYSIS_USER
            mode == null && request.isFirstAssistantReply -> AnalysisSource.GENERAL_FIRST_REPLY
            else -> null
        }
    }

    private fun buildSmartAnalysisMarkdown(
        meta: SessionMetadata,
        highlights: List<String>,
        actionableTips: List<String>,
        coreInsight: String?,
        sharpLine: String?,
        fallbackRawText: String?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("智能分析结果（根据最近对话自动生成）")
        meta.shortSummary?.takeIf { it.isNotBlank() }?.let {
            sb.appendLine().appendLine("### 会话概要").appendLine("- $it")
        }
        val personaLines = mutableListOf<String>()
        meta.mainPerson?.takeIf { it.isNotBlank() }?.let { personaLines.add("主要联系人：$it") }
        meta.location?.takeIf { it.isNotBlank() }?.let { personaLines.add("所在地：$it") }
        if (personaLines.isNotEmpty()) {
            sb.appendLine().appendLine("### 客户画像与意图")
            personaLines.forEach { line -> sb.appendLine("- $line") }
        }
        if (highlights.isNotEmpty()) {
            sb.appendLine().appendLine("### 需求与痛点")
            highlights.take(5).forEach { sb.appendLine("- $it") }
        }
        meta.riskLevel?.let {
            sb.appendLine().appendLine("### 机会与风险").appendLine("- 风险等级：${it.name}")
        }
        if (actionableTips.isNotEmpty()) {
            sb.appendLine().appendLine("### 建议与下一步行动")
            actionableTips.take(5).forEachIndexed { index, tip ->
                sb.appendLine("${index + 1}) $tip")
            }
        }
        coreInsight?.takeIf { it.isNotBlank() }?.let {
            sb.appendLine().appendLine("### 核心洞察").appendLine("- $it")
        }
        sharpLine?.takeIf { it.isNotBlank() }?.let {
            sb.appendLine().appendLine("### 关键话术").appendLine("- $it")
        }
        val built = sb.toString().trimEnd()
        if (built.isNotBlank()) return renumberNumberedBlocks(built.lines()).joinToString("\n")

        val fallback = fallbackRawText.orEmpty()
        if (fallback.isBlank()) return "暂无可用的智能分析结果，请稍后重试。"
        val cleaned = cleanSmartAnalysisOutput(fallback)
        return if (cleaned.isNotBlank()) cleaned else "暂无可用的智能分析结果，请稍后重试。"
    }

    private fun cleanSmartAnalysisOutput(raw: String): String {
        val fencedJson = firstJsonCodeBlock(raw)
        val markdownPart = fencedJson
            ?.let { raw.substringBeforeLast("```json").trimEnd() }
            ?: raw
        val collapsed = collapseProgressiveLines(markdownPart)
            .filterNot { isTemplateEcho(it) }
        val renumbered = renumberNumberedBlocks(collapsed)
        return renumbered.joinToString("\n").trimEnd()
    }

    private fun collapseProgressiveLines(markdown: String): List<String> {
        val lines = markdown.lines()
        if (lines.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var index = 0
        while (index < lines.size) {
            val current = lines[index]
            if (current.isBlank()) {
                result.add(current)
                index++
                continue
            }
            var candidate = current
            var nextIndex = index + 1
            while (nextIndex < lines.size) {
                val next = lines[nextIndex]
                if (next.isBlank()) break
                val trimmed = candidate.trim()
                val nextTrim = next.trim()
                if (nextTrim.startsWith(trimmed) && nextTrim.length > trimmed.length) {
                    candidate = next
                    nextIndex++
                } else {
                    break
                }
            }
            result.add(candidate)
            index = nextIndex
        }
        return result
    }

    private fun isTemplateEcho(line: String): Boolean {
        val compact = line.trim().replace(" ", "")
        if (compact.isEmpty()) return false
        if (compact.contains("角色/公司/城市")) return true
        if (compact.endsWith("##客户画像") || compact.endsWith("#会话概要")) return true
        if (compact.contains("客##客户##客户画像")) return true
        return false
    }

    private fun renumberNumberedBlocks(lines: List<String>): List<String> {
        val pattern = Regex("^\\s*\\d+[).、]")
        val result = mutableListOf<String>()
        var counter = 1
        var inBlock = false
        for (line in lines) {
            if (pattern.containsMatchIn(line)) {
                if (!inBlock) {
                    counter = 1
                    inBlock = true
                }
                val renamed = line.replace(pattern) { match ->
                    val firstDigit = match.value.first { it.isDigit() }
                    val suffix = match.value.substringAfter(firstDigit)
                    "${counter++}${suffix}"
                }
                result.add(renamed)
            } else {
                inBlock = false
                result.add(line)
            }
        }
        return result
    }

}
