// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuSuspiciousBoundaryDetector.kt
// 模块：:data:ai-core
// 说明：基于 Tingwu 转写文本解析可疑边界（确定性、可复现）
// 作者：创建于 2025-12-24
package com.smartsales.data.aicore.tingwu

import com.smartsales.core.metahub.SuspiciousBoundary
import com.smartsales.data.aicore.params.PostXfyunSettings

internal object TingwuSuspiciousBoundaryDetector {
    private val defaultGapThresholdMs: Long = PostXfyunSettings().suspiciousGapThresholdMs

    fun detect(
        transcriptMarkdown: String,
        gapThresholdMs: Long = defaultGapThresholdMs
    ): List<SuspiciousBoundary> = runCatching {
        val utterances = parseUtterances(transcriptMarkdown)
        if (utterances.size < 2) return@runCatching emptyList()
        val threshold = gapThresholdMs.coerceAtLeast(0L)
        val boundariesByIndex = LinkedHashMap<Int, SuspiciousBoundary>()
        for (i in 0 until (utterances.size - 1)) {
            val prev = utterances[i]
            val next = utterances[i + 1]
            val boundaryIndex = i + 1
            val gapBoundary = buildGapBoundary(boundaryIndex, prev.endMs, next.startMs, threshold)
            if (gapBoundary != null) {
                boundariesByIndex[boundaryIndex] = gapBoundary
                continue
            }
            val speakerBoundary = buildSpeakerBoundary(boundaryIndex, prev.speakerId, next.speakerId)
            if (speakerBoundary != null) {
                boundariesByIndex[boundaryIndex] = speakerBoundary
            }
        }
        boundariesByIndex.values.sortedBy { it.index }
    }.getOrDefault(emptyList())

    private fun buildGapBoundary(
        index: Int,
        prevEndMs: Long?,
        nextStartMs: Long?,
        threshold: Long
    ): SuspiciousBoundary? {
        if (prevEndMs == null || nextStartMs == null) return null
        val gapMs = (nextStartMs - prevEndMs).coerceAtLeast(0L)
        if (gapMs < threshold) return null
        return SuspiciousBoundary(
            index = index,
            reason = REASON_GAP,
            detail = "gapMs=$gapMs"
        )
    }

    private fun buildSpeakerBoundary(
        index: Int,
        prevSpeakerId: String?,
        nextSpeakerId: String?
    ): SuspiciousBoundary? {
        if (prevSpeakerId.isNullOrBlank() || nextSpeakerId.isNullOrBlank()) return null
        if (prevSpeakerId == nextSpeakerId) return null
        return SuspiciousBoundary(
            index = index,
            reason = REASON_SPEAKER_CHANGE,
            detail = "prevSpeaker=$prevSpeakerId,nextSpeaker=$nextSpeakerId"
        )
    }

    private fun parseUtterances(markdown: String): List<Utterance> {
        // 说明：仅解析带时间戳的逐句行，避免误把标题或空行当作内容。
        val utterances = mutableListOf<Utterance>()
        markdown.lineSequence().forEach { line ->
            parseUtterance(line)?.let { utterances += it }
        }
        return utterances
    }

    private fun parseUtterance(line: String): Utterance? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("- [")) return null
        val closingIndex = trimmed.indexOf(']')
        if (closingIndex <= 2) return null
        val timeBlock = trimmed.substring(3, closingIndex).trim()
        val timePair = parseTimeRange(timeBlock) ?: return null
        val after = trimmed.substring(closingIndex + 1).trimStart()
        val speakerId = parseSpeakerId(after)
        return Utterance(
            startMs = timePair.first,
            endMs = timePair.second,
            speakerId = speakerId
        )
    }

    private fun parseTimeRange(raw: String): Pair<Long, Long>? {
        val parts = raw.split("-").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val startMs = parseTimeToMs(parts[0]) ?: return null
        val endMs = if (parts.size >= 2) {
            parseTimeToMs(parts[1]) ?: startMs
        } else {
            startMs
        }
        return startMs to endMs
    }

    private fun parseTimeToMs(raw: String): Long? {
        val parts = raw.trim().split(":")
        val numbers = parts.map { it.toLongOrNull() ?: return null }
        return when (numbers.size) {
            2 -> {
                val minutes = numbers[0]
                val seconds = numbers[1]
                if (seconds !in 0..59) return null
                (minutes * 60 + seconds) * 1000
            }
            3 -> {
                val hours = numbers[0]
                val minutes = numbers[1]
                val seconds = numbers[2]
                if (minutes !in 0..59 || seconds !in 0..59) return null
                (hours * 3600 + minutes * 60 + seconds) * 1000
            }
            else -> null
        }
    }

    private fun parseSpeakerId(text: String): String? {
        val splitIndex = text.indexOf('：')
        if (splitIndex <= 0) return null
        return text.substring(0, splitIndex).trim().takeIf { it.isNotBlank() }
    }

    private data class Utterance(
        val startMs: Long,
        val endMs: Long,
        val speakerId: String?
    )

    private const val REASON_GAP = "gap"
    private const val REASON_SPEAKER_CHANGE = "speaker-change"
}
