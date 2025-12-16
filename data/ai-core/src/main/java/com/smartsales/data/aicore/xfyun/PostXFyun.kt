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
        if (!settings.enabled || settings.maxRepairsPerTranscript <= 0) {
            return@withContext PostXFyunResult(originalMarkdown, emptyList())
        }
        val bulletLines = originalMarkdown.lineSequence()
            .filter { it.startsWith("- ") }
            .toList()
        if (bulletLines.size < 2 || segments.size < 2) {
            return@withContext PostXFyunResult(originalMarkdown, emptyList())
        }
        // 重要：仅对“带说话人标签”的逐行转写生效；纯文本模式不做修复。
        if (!bulletLines.first().contains("发言人")) {
            return@withContext PostXFyunResult(originalMarkdown, emptyList())
        }

        val candidates = buildCandidates(
            lines = bulletLines,
            segments = segments,
            gapThresholdMs = settings.suspiciousGapThresholdMs,
        )
        if (candidates.isEmpty()) {
            return@withContext PostXFyunResult(originalMarkdown, emptyList())
        }

        val mutable = bulletLines.toMutableList()
        val repairs = mutableListOf<PostXFyunRepair>()
        for (candidate in candidates) {
            if (repairs.size >= settings.maxRepairsPerTranscript) break
            val index = candidate.boundaryIndex
            if (index !in 0 until (mutable.size - 1)) continue

            val prevLine = mutable[index]
            val nextLine = mutable[index + 1]
            val decision = arbitrate(
                prevLine = prevLine,
                nextLine = nextLine,
                boundaryMark = candidate.boundaryMark,
                settings = settings,
            )
            if (decision.action == PostXFyunAction.NONE) continue
            if (decision.confidence < settings.confidenceThreshold) continue

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

        if (repairs.isEmpty()) {
            return@withContext PostXFyunResult(originalMarkdown, emptyList())
        }

        val header = originalMarkdown.lineSequence().firstOrNull { it.startsWith("## ") } ?: "## 讯飞转写"
        val polished = buildString {
            appendLine(header)
            mutable.forEach { appendLine(it) }
        }.trimEnd()
        PostXFyunResult(polishedMarkdown = polished, repairs = repairs)
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
            val prevSpeaker = prevSeg.roleId?.trim()
            val nextSpeaker = nextSeg.roleId?.trim()
            if (prevSpeaker.isNullOrBlank() || nextSpeaker.isNullOrBlank()) continue
            if (prevSpeaker == nextSpeaker) continue
            val gapMs = computeGapMs(prevSeg.endMs, nextSeg.startMs) ?: continue
            if (gapMs > gapThresholdMs) continue

            val prevText = splitLine(lines[i]).text
            val nextText = splitLine(lines[i + 1]).text
            if (!looksLikeSplitTokenDrift(prevText, nextText, gapMs)) continue

            val mark = "gapMs=$gapMs, prevSpeaker=$prevSpeaker, nextSpeaker=$nextSpeaker"
            result += Candidate(
                boundaryIndex = i,
                gapMs = gapMs,
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

    private fun looksLikeSplitTokenDrift(prevTextRaw: String, nextTextRaw: String, gapMs: Long): Boolean {
        // 重要：gap-based + token-shape 的确定性筛选，宁可多标记一些，再交给 LLM 做封闭动作仲裁。
        if (gapMs < 0) return false
        val prev = prevTextRaw.trimEnd()
        val next = nextTextRaw.trimStart()
        if (prev.isBlank() || next.isBlank()) return false

        val prevLast = prev.last()
        val nextFirst = next.first()
        if (!isWordish(prevLast) || !isWordish(nextFirst)) return false
        if (isTerminalPunctuation(prevLast) || isLeadingPunctuation(nextFirst)) return false

        val alnumTail = prev.takeLastWhile { it.isLetterOrDigit() }
        val prevHasShortAlnumTail = alnumTail.length in 1..2
        val nextStartsWithAlnum = nextFirst.isLetterOrDigit()
        val nextStartsWithCjk = isCjk(nextFirst)
        val prevEndsWithCjk = isCjk(prevLast)

        // “续写特征”：
        // - prev 尾部是 1~2 位字母数字（6/d、A1/B2）；
        // - 或 prev/next 都是“字母数字/中文”，且 gap 极小（跨说话人边界像同一句被切开）。
        return prevHasShortAlnumTail ||
            nextStartsWithAlnum ||
            (prevEndsWithCjk && nextStartsWithCjk)
    }

    private data class Decision(
        val action: PostXFyunAction,
        val span: String,
        val confidence: Double,
        val reason: String?,
    )

    private suspend fun arbitrate(
        prevLine: String,
        nextLine: String,
        boundaryMark: String,
        settings: PostXfyunSettings,
    ): Decision {
        val prompt = settings.promptTemplate
            .replace("{{PREV_LINE}}", prevLine)
            .replace("{{NEXT_LINE}}", nextLine)
            .replace("{{BOUNDARY_MARK}}", boundaryMark)
        val response = when (val result = aiChatService.sendMessage(AiChatRequest(prompt = prompt))) {
            is Result.Success -> result.data.displayText
            is Result.Error -> return Decision(PostXFyunAction.NONE, span = "", confidence = 0.0, reason = null)
        }
        return parseStrictDecision(response)
    }

    private fun parseStrictDecision(raw: String): Decision {
        // 重要：严格模式——必须是“纯 JSON 对象”，任何前后缀都视为违约并回退 NONE。
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return Decision(PostXFyunAction.NONE, span = "", confidence = 0.0, reason = "non-json")
        }
        val obj = runCatching { JsonParser.parseString(trimmed) }.getOrNull()
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: return Decision(PostXFyunAction.NONE, span = "", confidence = 0.0, reason = "parse-failed")

        val action = obj.getString("action")
            ?.trim()
            ?.let { parseAction(it) }
            ?: PostXFyunAction.NONE
        val span = obj.getString("span")?.let { it } ?: ""
        val confidence = obj.getDouble("confidence") ?: 0.0
        val reason = obj.getString("reason")?.takeIf { it.isNotBlank() }

        if (confidence !in 0.0..1.0) return Decision(PostXFyunAction.NONE, span = "", confidence = 0.0, reason = "bad-confidence")
        if (action == PostXFyunAction.NONE) return Decision(PostXFyunAction.NONE, span = "", confidence = confidence, reason = reason)
        if (span.length !in 1..2 || span.any { it.isWhitespace() }) {
            return Decision(PostXFyunAction.NONE, span = "", confidence = 0.0, reason = "bad-span")
        }
        return Decision(action = action, span = span, confidence = confidence, reason = reason)
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

    private fun isWordish(ch: Char): Boolean = ch.isLetterOrDigit() || isCjk(ch)

    private fun isCjk(ch: Char): Boolean {
        val block = Character.UnicodeBlock.of(ch)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
            block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
    }

    private fun isPunctuation(ch: Char): Boolean = ch in PUNCTUATIONS

    private fun isTerminalPunctuation(ch: Char): Boolean = ch in TERMINAL_PUNCTUATIONS

    private fun isLeadingPunctuation(ch: Char): Boolean = ch in LEADING_PUNCTUATIONS

    private companion object {
        private val PUNCTUATIONS = setOf('。', '！', '？', '!', '?', '，', ',', '、', '；', ';', '：', ':', '…')
        private val TERMINAL_PUNCTUATIONS = setOf('。', '！', '？', '!', '?', '…')
        private val LEADING_PUNCTUATIONS = setOf('。', '！', '？', '!', '?', '，', ',', '、', '；', ';', '：', ':', '…')
    }
}

