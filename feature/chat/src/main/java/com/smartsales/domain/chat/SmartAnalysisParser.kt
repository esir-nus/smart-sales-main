// File: feature/chat/src/main/java/com/smartsales/domain/chat/SmartAnalysisParser.kt
// Module: :feature:chat
// Summary: V1 SmartAnalysis module (§3.1.2) - pure JSON parsing for L3 MachineArtifact
// Author: created on 2026-01-06

package com.smartsales.domain.chat

import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionStage
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private const val SMART_ANALYSIS_FAILURE_MESSAGE = "本次智能分析暂时不可用，请稍后重试。"

private val SHORT_SUMMARY_REGEX =
    Regex("\"short_summary\"\\s*:\\s*\"([^\"]+)\"")

/**
 * Section: Internal structure for markdown post-processing
 */
private data class Section(
    val title: String,
    val lines: MutableList<String> = mutableListOf()
)

/**
 * SmartAnalysisResult: Final output with markdown + metadata
 */
data class SmartAnalysisResult(
    val markdown: String,
    val metadata: SessionMetadata?
)

/**
 * ParsedSmartAnalysis: Structured fields extracted from L3 MachineArtifact
 */
data class ParsedSmartAnalysis(
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

/**
 * SmartAnalysisParser: Pure parsing functions for V1 SmartAnalysis module.
 *
 * Extracted from HomeOrchestratorImpl per M1.1a (Core Parsing Extraction).
 * Markdown generation deferred to M1.1b.
 */
object SmartAnalysisParser {

    /**
     * Parse SmartAnalysis JSON payload from LLM raw output.
     *
     * Strategy:
     * 1. Extract last fenced JSON block or top-level JSON object
     * 2. Parse fields with last-value-wins for short_summary
     * 3. Fallback to text-based extraction if JSON malformed
     *
     * @param rawText LLM raw output containing JSON
     * @return Parsed fields or null if no content extracted
     */
    fun parse(rawText: String): ParsedSmartAnalysis? {
        val jsonObject = extractLastSmartJson(rawText) ?: return parseSmartFromText(rawText)

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

    /**
     * Text-based fallback parser for malformed JSON.
     *
     * Extracts core fields (short_summary, core_insight, highlights) using regex.
     * Only used when JSON parsing fails.
     */
    private fun parseSmartFromText(rawText: String): ParsedSmartAnalysis? {
        val shortSummary = SHORT_SUMMARY_REGEX.findAll(rawText)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }

        val coreInsight = Regex("\"core_insight\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(rawText)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }

        val highlights = Regex("\"highlights\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
            .findAll(rawText)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.split(",")
            ?.map { it.replace("\"", "").trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val hasContent = !shortSummary.isNullOrBlank() || !coreInsight.isNullOrBlank() || highlights.isNotEmpty()
        if (!hasContent) return null

        return ParsedSmartAnalysis(
            mainPerson = null,
            shortSummary = shortSummary,
            summaryTitle6Chars = null,
            location = null,
            stage = null,
            riskLevel = null,
            highlights = highlights,
            actionableTips = emptyList(),
            coreInsight = coreInsight,
            sharpLine = null
        )
    }

    /**
     * Extract last JSON object from fenced blocks or top-level braces.
     */
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

    /**
     * Collect all top-level JSON object slices using brace matching.
     */
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

    /**
     * Parse and build complete SmartAnalysis result.
     *
     * Combines JSON parsing + markdown generation in one call.
     * This is the primary API for M1.1b onwards.
     *
     * @param rawText LLM raw output
     * @param sessionId Session ID for metadata construction
     * @param metaHub MetaHub for merging with existing metadata
     * @param source Analysis source for provenance
     * @return Complete result with markdown + metadata, or failure
     */
    suspend fun parseAndBuild(
        rawText: String,
        sessionId: String,
        metaHub: com.smartsales.core.metahub.MetaHub,
        source: com.smartsales.core.metahub.AnalysisSource?
    ): SmartAnalysisResult {
        val parsed = parse(rawText) ?: return SmartAnalysisResult(
            markdown = SMART_ANALYSIS_FAILURE_MESSAGE,
            metadata = null
        )
        val rawMarkdown = buildMarkdown(parsed)
        val processedMarkdown = postProcessSmartMarkdown(rawMarkdown)
        val metadata = buildMetadata(sessionId, parsed, source, metaHub)
        return SmartAnalysisResult(markdown = processedMarkdown, metadata = metadata)
    }

    /**
     * Build markdown from parsed fields.
     */
    private fun buildMarkdown(parsed: ParsedSmartAnalysis): String {
        val sb = StringBuilder()
        parsed.shortSummary?.takeIf { it.isNotBlank() }?.let { summary ->
            sb.appendLine("## 会话概要")
            sb.appendLine("- ${normalizePov(summary)}")
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
                    sb.appendLine("- ${normalizePov(highlight)}")
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
                    sb.appendLine("${index + 1}. ${normalizePov(tip)}")
                }
            }
            sb.appendLine()
        }

        parsed.coreInsight?.takeIf { it.isNotBlank() }?.let { insight ->
            sb.appendLine("## 核心洞察")
            sb.appendLine("- ${normalizePov(insight)}")
            sb.appendLine()
        }

        parsed.sharpLine?.takeIf { it.isNotBlank() }?.let { line ->
            sb.appendLine("## 一句话话术")
            sb.appendLine("- ${normalizePov(line)}")
            sb.appendLine()
        }

        return sb.toString().trimEnd().ifBlank { SMART_ANALYSIS_FAILURE_MESSAGE }
    }

    /**
     * Build metadata from parsed fields.
     */
    private suspend fun buildMetadata(
        sessionId: String,
        parsed: ParsedSmartAnalysis,
        source: com.smartsales.core.metahub.AnalysisSource?,
        metaHub: com.smartsales.core.metahub.MetaHub
    ): com.smartsales.core.metahub.SessionMetadata {
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val base = existing ?: com.smartsales.core.metahub.SessionMetadata(sessionId = sessionId)

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

    private fun formatStage(stage: com.smartsales.core.metahub.SessionStage): String = when (stage) {
        com.smartsales.core.metahub.SessionStage.DISCOVERY -> "探索"
        com.smartsales.core.metahub.SessionStage.NEGOTIATION -> "谈判"
        com.smartsales.core.metahub.SessionStage.PROPOSAL -> "方案/报价"
        com.smartsales.core.metahub.SessionStage.CLOSING -> "成交推进"
        com.smartsales.core.metahub.SessionStage.POST_SALE -> "售后"
        com.smartsales.core.metahub.SessionStage.UNKNOWN -> "未知阶段"
    }

    private fun formatRisk(risk: com.smartsales.core.metahub.RiskLevel): String = when (risk) {
        com.smartsales.core.metahub.RiskLevel.LOW -> "低"
        com.smartsales.core.metahub.RiskLevel.MEDIUM -> "中"
        com.smartsales.core.metahub.RiskLevel.HIGH -> "高"
        com.smartsales.core.metahub.RiskLevel.UNKNOWN -> "未知"
    }

    /**
     * POV normalization: Convert first-person sales perspective to second-person assistant voice.
     *
     * Ensures markdown maintains "AI assistant → sales advisor" tone, not "I = salesperson".
     * Transforms common patterns like "我在/我向/我给" to "你在/你向/你给".
     */
    private fun normalizePov(text: String): String {
        val trimmed = text.trim()
        val lower = trimmed.lowercase(Locale.getDefault())
        val replacements = listOf(
            "我在" to "你在",
            "我向" to "你向",
            "我给" to "你给",
            "我将" to "你将",
            "我会" to "你会",
            "我需要" to "你需要"
        )
        replacements.forEach { (prefix, target) ->
            if (lower.startsWith(prefix)) {
                return target + trimmed.removePrefix(prefix)
            }
        }
        if (lower.startsWith("我")) {
            return "你" + trimmed.drop(1)
        }
        return trimmed
    }

    /**
     * Post-process markdown: dedupe, cap bullets, normalize numbering, hide empty sections.
     *
     * Rules from V4.1.0 SMART analysis card specifications.
     */
    private fun postProcessSmartMarkdown(markdown: String): String {
        if (markdown == SMART_ANALYSIS_FAILURE_MESSAGE) return markdown
        val sections = mutableListOf<Section>()
        var current: Section? = null
        markdown.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("## ")) {
                val section = Section(title = trimmed.removePrefix("## ").trim())
                sections.add(section)
                current = section
            } else if (current != null) {
                current?.lines?.add(line)
            }
        }

        val globalSeen = mutableSetOf<String>()
        val processed = sections.mapNotNull { section ->
            processSection(section, globalSeen)
        }

        val builder = StringBuilder()
        processed.forEachIndexed { index, section ->
            if (section.lines.isEmpty()) return@forEachIndexed
            builder.appendLine("## ${section.title}")
            section.lines.forEach { builder.appendLine(it) }
            if (index != processed.lastIndex) builder.appendLine()
        }

        return builder.toString().trimEnd()
    }

    private fun processSection(
        section: Section,
        globalSeen: MutableSet<String>
    ): Section? {
        val maxBullets = 5
        val bullets = mutableListOf<String>()
        val paragraphs = mutableListOf<String>()
        val seenLocal = mutableSetOf<String>()

        section.lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach
            val bulletContent = extractBulletContent(trimmed)
            if (bulletContent != null) {
                val normalized = normalizeSentence(bulletContent)
                if (normalized.isEmpty()) return@forEach
                if (isMeaninglessContent(bulletContent)) return@forEach
                if (seenLocal.contains(normalized)) return@forEach
                if (globalSeen.contains(normalized)) return@forEach
                bullets.add(bulletContent)
                seenLocal.add(normalized)
                globalSeen.add(normalized)
                return@forEach
            }

            val normalized = normalizeSentence(trimmed)
            if (normalized.isEmpty()) return@forEach
            if (isMeaninglessContent(trimmed)) return@forEach
            if (seenLocal.contains(normalized)) return@forEach
            if (globalSeen.contains(normalized)) return@forEach
            paragraphs.add(trimmed)
            seenLocal.add(normalized)
            globalSeen.add(normalized)
        }

        val cappedBullets = bullets.take(maxBullets)
        val hasContent = cappedBullets.isNotEmpty() || paragraphs.isNotEmpty()
        if (!hasContent) return null

        val outputLines = mutableListOf<String>()
        paragraphs.forEach { outputLines.add(it) }
        if (section.title.contains("建议与行动")) {
            cappedBullets.forEachIndexed { index, bullet ->
                outputLines.add("${index + 1}. ${stripBulletPrefix(bullet)}")
            }
        } else {
            cappedBullets.forEach { outputLines.add("- ${stripBulletPrefix(it)}") }
        }
        return Section(title = section.title, lines = outputLines)
    }

    private fun extractBulletContent(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.startsWith("-")) {
            return trimmed.removePrefix("-").trim()
        }
        val numbered = Regex("^\\d+[\\.)）]*\\s*(.+)$")
        val match = numbered.find(trimmed)
        if (match != null) return match.groupValues[1].trim()
        return null
    }

    private fun stripBulletPrefix(text: String): String {
        var result = text.trim()
        val prefixRegex = Regex("^\\d+[\\.)）]*\\s*")
        while (prefixRegex.containsMatchIn(result)) {
            result = result.replaceFirst(prefixRegex, "").trim()
        }
        return result
    }

    private fun normalizeSentence(sentence: String): String {
        return sentence.lowercase(Locale.getDefault())
            .replace(Regex("[\\p{Punct}\\s]+"), "")
    }

    private fun isMeaninglessContent(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true
        val lower = trimmed.lowercase(Locale.getDefault())
        val meaningless = listOf("暂无", "无特别", "无明显", "无显著", "无特别补充", "无补充")
        return meaningless.any { lower.contains(it) }
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            optString(i)?.takeIf { it.isNotBlank() }?.let { list.add(it.trim()) }
        }
        return list
    }
}
