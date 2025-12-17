// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/PostXFyun.kt
// 模块：:data:ai-core
// 说明：讯飞转写后处理（跨说话人边界分词漂移修复：确定性检测 + LLM 仲裁 + 确定性应用）
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.xfyun

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.PostXfyunSettings
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

enum class PostXFyunAction {
    NONE,
    MOVE_TAIL_TO_NEXT,
    MOVE_HEAD_TO_PREV,
}

data class PostXFyunRepair(
    val boundaryIndex: Int,
    val action: PostXFyunAction,
    val span: String,
    val confidence: Double,
    val reason: String?,
    val gapMs: Long,
    val prevSpeakerId: String?,
    val nextSpeakerId: String?,
    val beforePrevLine: String,
    val beforeNextLine: String,
    val afterPrevLine: String,
    val afterNextLine: String,
)

data class PostXFyunResult(
    val polishedMarkdown: String,
    val repairs: List<PostXFyunRepair>,
    val debugInfo: PostXFyunDebugInfo? = null,
)

data class PostXFyunDebugInfo(
    val settings: PostXFyunSettingsDebug,
    val suspiciousBoundaries: List<PostXFyunSuspiciousBoundary>,
    val decisions: List<PostXFyunDecisionDebug>,
    val candidatesCount: Int,
    val arbitrationsAttempted: Int,
    val arbitrationBudget: Int,
    val repairsApplied: Int,
)

data class PostXFyunSettingsDebug(
    val enabled: Boolean,
    val maxRepairsPerTranscript: Int,
    val suspiciousGapThresholdMs: Long,
    val confidenceThreshold: Double,
    val modelEffective: String,
    val promptLength: Int,
    val promptPreview: String,
    val promptSha256: String? = null,
)

data class PostXFyunSuspiciousBoundary(
    val boundaryIndex: Int,
    val gapMs: Long,
    val prevSpeakerId: String?,
    val nextSpeakerId: String?,
    val prevExcerpt: String,
    val nextExcerpt: String,
)

data class PostXFyunDecisionDebug(
    val attemptIndex: Int,
    val boundaryIndex: Int,
    val gapMs: Long,
    val prevSpeakerId: String?,
    val nextSpeakerId: String?,
    val prevExcerpt: String,
    val nextExcerpt: String,
    val modelUsed: String?,
    val action: PostXFyunAction,
    val span: String,
    val confidence: Double,
    val reason: String?,
    // 重要：仅用于验证“LLM 确实被调用并返回了内容”，只保留截断预览（不落盘）。
    val rawResponsePreview: String? = null,
    val parseStatus: String = "OK",
    val errorHint: String? = null,
)

/**
 * 说明：
 * - 目的：修复“跨说话人边界”的轻微分词漂移（例如：罗/总、为/什、6/d）。
 * - 只允许确定性小修：在边界两侧移动 1~2 个字符，不做任何句子改写。
 * - LLM 仅做“仲裁”，必须输出封闭动作 JSON；任意违约都强制回退为 NONE。
 */
@Singleton
class PostXFyun @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val aiChatService: AiChatService,
    private val aiParaSettingsProvider: AiParaSettingsProvider,
) {

    suspend fun polish(
        originalMarkdown: String,
        segments: List<XfyunTranscriptSegment>,
    ): PostXFyunResult = withContext(dispatchers.io) {
        val settings = aiParaSettingsProvider.snapshot().transcription.xfyun.postXfyun
        val settingsDebug = settings.toDebug()
        val fallback = PostXFyunResult(
            polishedMarkdown = originalMarkdown,
            repairs = emptyList(),
            debugInfo = PostXFyunDebugInfo(
                settings = settingsDebug,
                suspiciousBoundaries = emptyList(),
                decisions = emptyList(),
                candidatesCount = 0,
                arbitrationsAttempted = 0,
                arbitrationBudget = 0,
                repairsApplied = 0,
            )
        )
        // 重要：PostXFyun 属于“可选增强”，任何异常都必须回退原始渲染（同时保留 debug 信息用于排查）。
        runCatching {
            if (!settings.enabled || settings.maxRepairsPerTranscript <= 0) {
                return@runCatching fallback
            }
            val bulletLines = originalMarkdown.lineSequence()
                .filter { it.startsWith("- ") }
                .toList()
            if (bulletLines.size < 2 || segments.size < 2) {
                return@runCatching fallback
            }
            // 重要：仅对“带说话人标签”的逐行转写生效；纯文本模式不做修复。
            if (!bulletLines.first().contains("发言人")) {
                return@runCatching fallback
            }

            val candidates = buildCandidates(
                lines = bulletLines,
                segments = segments,
                gapThresholdMs = settings.suspiciousGapThresholdMs,
            )
            val candidatesCount = candidates.size

            val repairs = mutableListOf<PostXFyunRepair>()
            val decisions = mutableListOf<PostXFyunDecisionDebug>()

            // 重要：可疑边界（gap-only）用于“可见证据”，不应被 maxRepairs 截断影响。
            val suspicious = candidates.mapNotNull { candidate ->
                val index = candidate.boundaryIndex
                if (index !in 0 until (bulletLines.size - 1)) return@mapNotNull null
                val prevLine = bulletLines[index]
                val nextLine = bulletLines[index + 1]
                PostXFyunSuspiciousBoundary(
                    boundaryIndex = index,
                    gapMs = candidate.gapMs,
                    prevSpeakerId = candidate.prevSpeakerId,
                    nextSpeakerId = candidate.nextSpeakerId,
                    // 重要：可疑标记仅用于 Prompt/HUD，可帮助人工确认标记策略；不会写回逐字稿，除非仲裁后真的发生修复。
                    prevExcerpt = excerptTail(splitLine(prevLine).text, 16) + PROMPT_SUSPICIOUS_MARK,
                    nextExcerpt = excerptHead(splitLine(nextLine).text, 16),
                )
            }

            val mutable = bulletLines.toMutableList()
            val repairLimit = settings.maxRepairsPerTranscript
            val arbitrationBudget = minOf(
                candidates.size,
                maxOf(30, repairLimit * 10)
            )
            var arbitrationsAttempted = 0
            for (candidate in candidates) {
                // 重要：仲裁预算与修复上限分离：
                // - repairLimit：最多允许落地多少次文本修复。
                // - arbitrationBudget：最多允许调用多少次 LLM 仲裁（即使一直返回 NONE 也会继续尝试，确保能覆盖后面的“问题边界”）。
                if (repairs.size >= repairLimit) break
                if (arbitrationsAttempted >= arbitrationBudget) break

                val index = candidate.boundaryIndex
                if (index !in 0 until (mutable.size - 1)) continue

                val prevLine = mutable[index]
                val nextLine = mutable[index + 1]
                val capturePreview = arbitrationsAttempted < MAX_LLM_PREVIEW_COUNT
                val decision = arbitrate(
                    prevLine = prevLine,
                    nextLine = nextLine,
                    boundaryMark = candidate.boundaryMark,
                    settings = settings,
                    capturePreview = capturePreview,
                )
                val attemptIndex = arbitrationsAttempted
                arbitrationsAttempted += 1
                decisions += PostXFyunDecisionDebug(
                    attemptIndex = attemptIndex,
                    boundaryIndex = index,
                    gapMs = candidate.gapMs,
                    prevSpeakerId = candidate.prevSpeakerId,
                    nextSpeakerId = candidate.nextSpeakerId,
                    prevExcerpt = excerptTail(splitLine(prevLine).text, 16) + PROMPT_SUSPICIOUS_MARK,
                    nextExcerpt = excerptHead(splitLine(nextLine).text, 16),
                    modelUsed = decision.modelUsed,
                    action = decision.action,
                    span = decision.span,
                    confidence = decision.confidence,
                    reason = decision.reason,
                    rawResponsePreview = decision.rawResponsePreview,
                    parseStatus = decision.parseStatus.name,
                    errorHint = decision.errorHint,
                )
                if (decision.action == PostXFyunAction.NONE) continue
                if (decision.confidence < settings.confidenceThreshold) continue
                if (repairs.size >= repairLimit) continue

                val applied = applyDecision(
                    boundaryIndex = index,
                    prevLine = prevLine,
                    nextLine = nextLine,
                    decision = decision,
                    candidate = candidate,
                ) ?: continue

                mutable[index] = applied.afterPrevLine
                mutable[index + 1] = applied.afterNextLine
                repairs += applied
            }

            val header = originalMarkdown.lineSequence().firstOrNull { it.startsWith("## ") } ?: "## 讯飞转写"
            val polished = if (repairs.isEmpty()) originalMarkdown else buildString {
                appendLine(header)
                mutable.forEach { appendLine(it) }
            }.trimEnd()
            PostXFyunResult(
                polishedMarkdown = polished,
                repairs = repairs,
                debugInfo = PostXFyunDebugInfo(
                    settings = settingsDebug,
                    suspiciousBoundaries = suspicious,
                    decisions = decisions,
                    candidatesCount = candidatesCount,
                    arbitrationsAttempted = arbitrationsAttempted,
                    arbitrationBudget = arbitrationBudget,
                    repairsApplied = repairs.size,
                )
            )
        }.getOrElse { fallback }
    }

    private data class Candidate(
        val boundaryIndex: Int,
        val gapMs: Long,
        val prevSpeakerId: String?,
        val nextSpeakerId: String?,
        val boundaryMark: String,
    )

    private fun buildCandidates(
        lines: List<String>,
        segments: List<XfyunTranscriptSegment>,
        gapThresholdMs: Long,
    ): List<Candidate> {
        val limit = minOf(lines.size, segments.size)
        if (limit < 2) return emptyList()
        val result = mutableListOf<Candidate>()
        for (i in 0 until (limit - 1)) {
            val prevSeg = segments[i]
            val nextSeg = segments[i + 1]
            val gapMs = computeGapMs(prevSeg.endMs, nextSeg.startMs) ?: continue
            if (gapMs > gapThresholdMs) continue

            // 重要：候选边界只用 gap 阈值来确定（确定性、可复现），是否需要修复交给 LLM 做封闭动作仲裁。
            // - 不区分同/不同说话人：同说话人也可能出现轻微切分漂移。
            // - 不做字形/标点/英文数字启发式：避免规则过拟合导致“该修不修 / 不该修乱修”的不可控。
            val prevSpeaker = prevSeg.roleId?.trim()
            val nextSpeaker = nextSeg.roleId?.trim()
            val mark = "gapMs=$gapMs, prevSpeaker=${prevSpeaker ?: "?"}, nextSpeaker=${nextSpeaker ?: "?"}"
            result += Candidate(
                boundaryIndex = i,
                gapMs = gapMs,
                // 重要：用于 HUD 证据展示：即使 roleId 为空字符串也要保留（不要转成 null）。
                prevSpeakerId = prevSpeaker,
                nextSpeakerId = nextSpeaker,
                boundaryMark = mark,
            )
        }
        return result
    }

    private fun computeGapMs(prevEndMs: Long?, nextStartMs: Long?): Long? {
        val end = prevEndMs ?: return null
        val start = nextStartMs ?: return null
        return (start - end).coerceAtLeast(0L)
    }

    private data class Decision(
        val action: PostXFyunAction,
        val span: String,
        val confidence: Double,
        val reason: String?,
        val rawResponsePreview: String? = null,
        val modelUsed: String? = null,
        val parseStatus: ParseStatus = ParseStatus.OK,
        val errorHint: String? = null,
    )

    private enum class ParseStatus {
        OK,
        STRIPPED_FENCE_OK,
        NON_JSON,
        PARSE_FAILED,
    }

    private suspend fun arbitrate(
        prevLine: String,
        nextLine: String,
        boundaryMark: String,
        settings: PostXfyunSettings,
        capturePreview: Boolean,
    ): Decision {
        // 重要：可疑标记仅用于 Prompt，让模型把注意力聚焦到边界附近；不改变最终展示的逐字稿文本。
        val prevLineForPrompt = prevLine.trimEnd() + PROMPT_SUSPICIOUS_MARK
        val prompt = settings.promptTemplate
            .replace("{{PREV_LINE}}", prevLineForPrompt)
            .replace("{{NEXT_LINE}}", nextLine.trimEnd())
            .replace("{{BOUNDARY_MARK}}", boundaryMark)
        val modelOverride = settings.model.trim().takeIf { it.isNotBlank() }
        val result = aiChatService.sendMessage(AiChatRequest(prompt = prompt, model = modelOverride))
        val response = when (result) {
            is Result.Success -> result.data.displayText
            is Result.Error -> return Decision(PostXFyunAction.NONE, span = "", confidence = 0.0, reason = null)
        }
        val preview = if (capturePreview) {
            response.trim()
                .replace("\r", "")
                .replace("\n", "\\n")
                .take(LLM_PREVIEW_MAX_CHARS)
        } else {
            null
        }
        val modelUsed = (result as? Result.Success)?.data?.modelUsed
        val parsed = parseStrictDecision(response)
        return parsed.copy(rawResponsePreview = preview, modelUsed = modelUsed)
    }

    private fun parseStrictDecision(raw: String): Decision {
        // 重要：严格模式——必须是“纯 JSON 对象”，任何前后缀都视为违约并回退 NONE。
        val trimmed = raw.trim()
        val (payload, strippedFence) = stripMarkdownFencesIfPresent(trimmed)
        val normalized = payload.trim()
        if (!normalized.startsWith("{") || !normalized.endsWith("}")) {
            return Decision(
                PostXFyunAction.NONE,
                span = "",
                confidence = 0.0,
                reason = "non-json",
                parseStatus = ParseStatus.NON_JSON,
                errorHint = "reply is not a pure JSON object"
            )
        }
        val parsed = runCatching { JsonParser.parseString(normalized) }
        val obj = parsed.getOrNull()
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: return Decision(
                PostXFyunAction.NONE,
                span = "",
                confidence = 0.0,
                reason = "parse-failed",
                parseStatus = ParseStatus.PARSE_FAILED,
                errorHint = parsed.exceptionOrNull()?.message?.take(80),
            )

        val action = obj.getString("action")
            ?.trim()
            ?.let { parseAction(it) }
            ?: PostXFyunAction.NONE
        val span = obj.getString("span")?.let { it } ?: ""
        val confidence = obj.getDouble("confidence") ?: 0.0
        val reason = obj.getString("reason")?.takeIf { it.isNotBlank() }

        val okStatus = if (strippedFence) ParseStatus.STRIPPED_FENCE_OK else ParseStatus.OK
        if (confidence !in 0.0..1.0) {
            return Decision(
                PostXFyunAction.NONE,
                span = "",
                confidence = 0.0,
                reason = "bad-confidence",
                parseStatus = okStatus
            )
        }
        if (action == PostXFyunAction.NONE) {
            return Decision(
                PostXFyunAction.NONE,
                span = "",
                confidence = confidence,
                reason = reason,
                parseStatus = okStatus
            )
        }
        if (span.length !in 1..2 || span.any { it.isWhitespace() }) {
            return Decision(
                PostXFyunAction.NONE,
                span = "",
                confidence = 0.0,
                reason = "bad-span",
                parseStatus = okStatus
            )
        }
        return Decision(action = action, span = span, confidence = confidence, reason = reason, parseStatus = okStatus)
    }

    private fun stripMarkdownFencesIfPresent(raw: String): Pair<String, Boolean> {
        // 说明：兼容 ```json ... ``` 的包装，但不放松“必须是纯 JSON 对象”的约束。
        // - 仅移除首尾 fence 行，保留中间内容原样。
        val trimmed = raw.trim()
        if (!trimmed.startsWith("```")) return trimmed to false
        val lines = trimmed.split("\n")
        if (lines.size < 3) return trimmed to false
        val first = lines.first().trim()
        val last = lines.last().trim()
        if (!first.startsWith("```") || !last.startsWith("```")) return trimmed to false
        return lines.subList(1, lines.size - 1).joinToString("\n") to true
    }

    private fun parseAction(raw: String): PostXFyunAction = when (raw.trim().uppercase()) {
        "NONE" -> PostXFyunAction.NONE
        "MOVE_TAIL_TO_NEXT" -> PostXFyunAction.MOVE_TAIL_TO_NEXT
        "MOVE_HEAD_TO_PREV" -> PostXFyunAction.MOVE_HEAD_TO_PREV
        else -> PostXFyunAction.NONE
    }

    private fun applyDecision(
        boundaryIndex: Int,
        prevLine: String,
        nextLine: String,
        decision: Decision,
        candidate: Candidate,
    ): PostXFyunRepair? {
        val prevParts = splitLine(prevLine)
        val nextParts = splitLine(nextLine)

        val (newPrevText, newNextText) = when (decision.action) {
            PostXFyunAction.MOVE_TAIL_TO_NEXT -> {
                moveTailToNext(prevParts.text, nextParts.text, decision.span) ?: return null
            }
            PostXFyunAction.MOVE_HEAD_TO_PREV -> {
                moveHeadToPrev(prevParts.text, nextParts.text, decision.span) ?: return null
            }
            PostXFyunAction.NONE -> return null
        }

        val cleaned = dedupeBoundaryPunctuation(newPrevText, newNextText)
        val afterPrev = prevParts.prefix + cleaned.first
        val afterNext = nextParts.prefix + cleaned.second
        return PostXFyunRepair(
            boundaryIndex = boundaryIndex,
            action = decision.action,
            span = decision.span,
            confidence = decision.confidence,
            reason = decision.reason,
            gapMs = candidate.gapMs,
            prevSpeakerId = candidate.prevSpeakerId,
            nextSpeakerId = candidate.nextSpeakerId,
            beforePrevLine = prevLine,
            beforeNextLine = nextLine,
            afterPrevLine = afterPrev,
            afterNextLine = afterNext,
        )
    }

    private fun moveHeadToPrev(prevText: String, nextText: String, span: String): Pair<String, String>? {
        val (prevCore, prevTrail) = splitTrailingWhitespace(prevText)
        val (nextLead, nextCore) = splitLeadingWhitespace(nextText)
        if (!nextCore.startsWith(span)) return null
        val newPrev = prevCore + span + prevTrail
        val newNext = nextLead + nextCore.removePrefix(span)
        return newPrev to newNext
    }

    private fun moveTailToNext(prevText: String, nextText: String, span: String): Pair<String, String>? {
        val (prevCore, prevTrail) = splitTrailingWhitespace(prevText)
        val (nextLead, nextCore) = splitLeadingWhitespace(nextText)
        if (!prevCore.endsWith(span)) return null
        val newPrev = prevCore.removeSuffix(span) + prevTrail
        val newNext = nextLead + span + nextCore
        return newPrev to newNext
    }

    private fun splitLeadingWhitespace(text: String): Pair<String, String> {
        val leadCount = text.takeWhile { it.isWhitespace() }.length
        return text.take(leadCount) to text.drop(leadCount)
    }

    private fun splitTrailingWhitespace(text: String): Pair<String, String> {
        val core = text.trimEnd()
        return core to text.drop(core.length)
    }

    private fun excerptHead(text: String, maxChars: Int): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return ""
        return trimmed.take(maxChars)
    }

    private fun excerptTail(text: String, maxChars: Int): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return ""
        return trimmed.takeLast(maxChars)
    }

    private fun dedupeBoundaryPunctuation(prevText: String, nextText: String): Pair<String, String> {
        val prevCore = prevText.trimEnd()
        val nextCore = nextText.trimStart()
        if (prevCore.isEmpty() || nextCore.isEmpty()) return prevText to nextText
        val last = prevCore.last()
        val first = nextCore.first()
        if (last == first && isPunctuation(last)) {
            val cleanedNext = nextText.replaceFirst(first.toString(), "")
            return prevText to cleanedNext
        }
        return prevText to nextText
    }

    private data class LineParts(
        val prefix: String,
        val text: String,
    )

    private fun splitLine(line: String): LineParts {
        val fullWidth = line.indexOf('：')
        val ascii = line.indexOf(':')
        val index = when {
            fullWidth >= 0 -> fullWidth
            ascii >= 0 -> ascii
            else -> -1
        }
        if (index < 0) return LineParts(prefix = "", text = line)
        val prefix = line.substring(0, index + 1)
        val text = line.substring(index + 1)
        return LineParts(prefix = prefix, text = text)
    }

    private fun JsonObject.getString(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.getDouble(key: String): Double? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asDouble

    private fun isPunctuation(ch: Char): Boolean = ch in PUNCTUATIONS

    private fun PostXfyunSettings.toDebug(): PostXFyunSettingsDebug {
        val template = promptTemplate
        val preview = template.trim().take(PROMPT_PREVIEW_CHARS)
        val modelEffective = model.trim().ifBlank { "(default)" }
        return PostXFyunSettingsDebug(
            enabled = enabled,
            maxRepairsPerTranscript = maxRepairsPerTranscript,
            suspiciousGapThresholdMs = suspiciousGapThresholdMs,
            confidenceThreshold = confidenceThreshold,
            modelEffective = modelEffective,
            promptLength = template.length,
            promptPreview = preview,
            promptSha256 = sha256Hex(template),
        )
    }

    private fun sha256Hex(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(((b.toInt() shr 4) and 0x0F).toString(16))
            sb.append((b.toInt() and 0x0F).toString(16))
        }
        return sb.toString()
    }

    private companion object {
        private val PUNCTUATIONS = setOf('。', '！', '？', '!', '?', '，', ',', '、', '；', ';', '：', ':', '…')
        private const val PROMPT_PREVIEW_CHARS = 120
        private const val PROMPT_SUSPICIOUS_MARK = "〔/suspicious〕"
        private const val MAX_LLM_PREVIEW_COUNT = 3
        private const val LLM_PREVIEW_MAX_CHARS = 200
    }
}
