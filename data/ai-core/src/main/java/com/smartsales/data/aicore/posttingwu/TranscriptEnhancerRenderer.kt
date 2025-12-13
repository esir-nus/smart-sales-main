// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/posttingwu/TranscriptEnhancerRenderer.kt
// 模块：:data:ai-core
// 说明：应用增强补丁并渲染 Markdown，保持时间顺序与时标约束
// 作者：创建于 2025-12-13
package com.smartsales.data.aicore.posttingwu

import java.text.DecimalFormat
import kotlin.math.max

private const val MIN_CONFIDENCE = 0.55
private val timeFormatter = DecimalFormat("00")
private val fillerRegex = Regex("^(?:[嗯啊呃额然后、，,。\\s]+)+")

data class RenderedLine(
    val timestampMs: Long?,
    val speaker: String,
    val text: String
)

/**
 * 应用 LLM 返回的补丁，保持原顺序，不新增时间戳。
 */
fun applyEnhancerOutput(
    utterances: List<EnhancerUtterance>,
    output: EnhancerOutput?,
    baseSpeakerLabels: Map<String, String> = emptyMap()
): List<RenderedLine> {
    if (utterances.isEmpty()) return emptyList()
    val rosterById = output?.speakerRoster.orEmpty().associateBy { it.sourceSpeakerId }
    val editsByIndex = output?.utteranceEdits.orEmpty()
        .sortedBy { it.index }
        .associateBy { it.index }
    val lines = mutableListOf<RenderedLine>()
    utterances.forEach { utterance ->
        val edit = editsByIndex[utterance.index]
        val baseText = cleanupText(utterance.text)
        val split = edit?.split.orEmpty()
        if (split.isNotEmpty()) {
            split.forEachIndexed { idx, splitLine ->
                val label = resolveSpeakerLabel(
                    speakerId = utterance.speakerId,
                    baseSpeakerLabels = baseSpeakerLabels,
                    roster = rosterById,
                    overrideLabel = splitLine.speakerLabel,
                    confidence = edit?.confidence
                )
                val text = cleanupText(
                    splitLine.text.ifBlank { edit?.newText ?: baseText }
                )
                lines += RenderedLine(
                    timestampMs = if (idx == 0) utterance.startMs else null,
                    speaker = label,
                    text = text
                )
            }
        } else {
            val label = resolveSpeakerLabel(
                speakerId = utterance.speakerId,
                baseSpeakerLabels = baseSpeakerLabels,
                roster = rosterById,
                overrideLabel = edit?.newSpeakerLabel,
                confidence = edit?.confidence
            )
            val text = cleanupText(edit?.newText ?: baseText)
            lines += RenderedLine(
                timestampMs = utterance.startMs,
                speaker = label,
                text = text
            )
        }
    }
    return lines
}

/**
 * 渲染 Markdown，首行保留原时标，分裂出的后续行无时标但缩进两格。
 */
fun renderEnhancedMarkdown(lines: List<RenderedLine>): String {
    if (lines.isEmpty()) return "暂无可用的转写结果。"
    val builder = StringBuilder()
    builder.append("## 逐字稿\n")
    lines.forEach { line ->
        if (line.timestampMs != null) {
            builder.append("- [")
                .append(formatTimeMs(line.timestampMs))
                .append("] ")
        } else {
            builder.append("  ")
        }
        builder.append(line.speaker.ifBlank { "说话人?" })
            .append("：")
            .append(line.text.ifBlank { "（空白）" })
            .append("\n")
    }
    return builder.toString().trimEnd()
}

private fun resolveSpeakerLabel(
    speakerId: String?,
    baseSpeakerLabels: Map<String, String>,
    roster: Map<String, SpeakerLabel>,
    overrideLabel: String? = null,
    confidence: Double? = null
): String {
    val baseLabel = speakerId?.let { baseSpeakerLabels[it]?.takeIf { label -> label.isNotBlank() } }
    val normalizedOverride = overrideLabel?.trim().orEmpty()
    if (normalizedOverride.isNotBlank() && isConfident(confidence)) {
        return normalizedOverride
    }
    val rosterLabel = roster[speakerId]?.label?.takeIf { it.isNotBlank() }
    val rosterConfidence = roster[speakerId]?.confidence
    if (!rosterLabel.isNullOrBlank() && isConfident(rosterConfidence)) {
        return rosterLabel
    }
    if (normalizedOverride.isNotBlank() && baseLabel.isNullOrBlank()) {
        return normalizedOverride
    }
    if (!rosterLabel.isNullOrBlank() && baseLabel.isNullOrBlank()) {
        return rosterLabel
    }
    return baseLabel ?: buildFallbackLabel(speakerId)
}

private fun isConfident(confidence: Double?): Boolean {
    return confidence == null || confidence >= MIN_CONFIDENCE
}

private fun buildFallbackLabel(speakerId: String?): String {
    val trimmed = speakerId?.trim().orEmpty()
    return if (trimmed.isNotBlank()) "说话人$trimmed" else "说话人?"
}

private fun cleanupText(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return trimmed
    val withoutFiller = trimmed.replace(fillerRegex, "").trim()
    return if (withoutFiller.isNotEmpty()) withoutFiller else trimmed
}

private fun formatTimeMs(value: Long): String {
    if (value <= 0) return "00:00"
    val totalSeconds = max(value / 1000, 0)
    val hours = (totalSeconds / 3600).toInt()
    val minutes = ((totalSeconds % 3600) / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return if (hours > 0) {
        "${timeFormatter.format(hours)}:${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
    } else {
        "${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
    }
}
