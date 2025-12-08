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
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class HomeOrchestratorImpl @Inject constructor(
    private val aiChatService: AiChatService,
    private val metaHub: MetaHub
) : HomeOrchestrator {
    override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
        return flow {
            aiChatService.streamChat(request).collect { event ->
                if (event is ChatStreamEvent.Completed && shouldParseMetadata(request)) {
                    runCatching { maybeUpsertSessionMetadata(request, event.fullText) }
                    val cleaned = cleanSmartAnalysisOutput(event.fullText)
                    emit(ChatStreamEvent.Completed(cleaned))
                } else {
                    emit(event)
                }
            }
        }
    }

    private suspend fun maybeUpsertSessionMetadata(
        request: ChatRequest,
        assistantText: String
    ) {
        // 直接在完整 markdown 文本上做鲁棒解析，而不是依赖 JSON 反序列化一定成功
        val parsed = parseSessionMetadataFromText(
            sessionId = request.sessionId,
            rawText = assistantText,
            source = resolveAnalysisSource(request)
        ) ?: return

        val merged = mergeWithExisting(request.sessionId, parsed)
        runCatching { metaHub.upsertSession(merged) }
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

    private fun parseSessionMetadataFromText(
        sessionId: String,
        rawText: String,
        source: AnalysisSource?
    ): SessionMetadata? {
        // 先尝试从第一个 fenced JSON 里找，如果失败再在整段文本里兜底
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
        val crmRows = emptyList<CrmRow>() // 先不支持复杂嵌套结构，后续再扩展

        val tags = (highlights + actionable).filter { it.isNotBlank() }.toSet()

        // 如果完全解析不到任何有用字段，就视为无效结果
        if (mainPerson == null &&
            shortSummary == null &&
            summaryTitle == null &&
            location == null &&
            crmRows.isEmpty() &&
            tags.isEmpty()
        ) {
            return null
        }

        val now = System.currentTimeMillis()
        return SessionMetadata(
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
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            optString(i)?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        }
        return list
    }

    private fun JSONArray.toCrmRows(): List<CrmRow> {
        val rows = mutableListOf<CrmRow>()
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            val row = CrmRow(
                client = obj.optString("client"),
                region = obj.optString("region"),
                stage = obj.optString("stage"),
                progress = obj.optString("progress"),
                nextStep = obj.optString("next_step"),
                owner = obj.optString("owner")
            )
            rows += row
        }
        return rows
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

    private fun cleanSmartAnalysisOutput(raw: String): String {
        val fencedJson = firstJsonCodeBlock(raw)
        val markdownPart = fencedJson
            ?.let { raw.substringBeforeLast("```json").trimEnd() }
            ?: raw
        val collapsed = collapseProgressiveLines(markdownPart)
            .filterNot { isTemplateEcho(it) }
        val renumbered = renumberNumberedBlocks(collapsed)
        val markdown = renumbered.joinToString("\n").trimEnd()
        val cleanedJson = fencedJson?.let { buildCleanJsonSlice(raw) }
        return if (cleanedJson.isNullOrBlank()) {
            markdown
        } else {
            listOf(markdown, "```json", cleanedJson, "```")
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
                .trimEnd()
        }
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

    private fun buildCleanJsonSlice(raw: String): String? {
        val slice = firstJsonCodeBlock(raw) ?: extractBareJsonObject(raw) ?: return null
        val mainPerson = findStringField(slice, "main_person")
            ?: findStringField(slice, "mainPerson")
        val shortSummary = findStringField(slice, "short_summary")
        val summaryTitle = findStringField(slice, "summary_title_6chars")
        val location = findStringField(slice, "location")
        val highlights = findStringListField(slice, "highlights")
        val actionable = findStringListField(slice, "actionable_tips")
        val coreInsight = findStringField(slice, "core_insight")
        val sharpLine = findStringField(slice, "sharp_line")

        val summaryObj = JSONObject()
        coreInsight?.takeIf { it.isNotBlank() }?.let { summaryObj.put("core_insight", it) }
        sharpLine?.takeIf { it.isNotBlank() }?.let { summaryObj.put("sharp_line", it) }

        val obj = JSONObject()
        mainPerson?.takeIf { it.isNotBlank() }?.let { obj.put("main_person", it) }
        shortSummary?.takeIf { it.isNotBlank() }?.let { obj.put("short_summary", it) }
        summaryTitle?.takeIf { it.isNotBlank() }?.let { obj.put("summary_title_6chars", it) }
        location?.let { obj.put("location", it) }
        if (highlights.isNotEmpty()) obj.put("highlights", JSONArray(highlights))
        if (actionable.isNotEmpty()) obj.put("actionable_tips", JSONArray(actionable))
        if (summaryObj.length() > 0) obj.put("summary", summaryObj)

        if (obj.length() == 0) return null
        return obj.toString(2)
    }
}
