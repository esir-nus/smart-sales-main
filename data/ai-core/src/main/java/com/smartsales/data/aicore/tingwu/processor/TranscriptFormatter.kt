// File: data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptFormatter.kt
// Module: :data:ai-core
// Summary: Transcript formatting logic extracted from TingwuRunner (V1 spec §3.2.3 Sanitizer)
// Author: created on 2026-01-07

package com.smartsales.data.aicore.tingwu.processor

import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import java.text.DecimalFormat
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * TranscriptFormatter: formats Tingwu transcription data into displayable markdown.
 * 
 * Implements V1 spec §3.2.3 Sanitizer functionality within data layer.
 * Ensures display safety and consistency; no semantic changes to transcription content.
 */
@Singleton
class TranscriptFormatter @Inject constructor() {

    private val timeFormatter = DecimalFormat("00")

    /**
     * Build markdown transcript from Tingwu transcription data.
     * Priority: diarized segments > raw text > individual segments
     */
    fun buildMarkdown(
        transcription: TingwuTranscription?,
        diarizedSegments: List<DiarizedSegment> = emptyList(),
        speakerLabels: Map<String, String> = emptyMap(),
    ): String {
        if (diarizedSegments.isNotEmpty()) {
            val sorted = diarizedSegments.sortedBy { it.startMs }
            val builder = StringBuilder()
            builder.append("## 逐字稿\n")
            sorted.forEach { segment ->
                val label = segment.speakerId?.let { id ->
                    speakerLabels[id]?.takeIf { it.isNotBlank() } ?: id
                }
                val begin = formatTimeMs(segment.startMs)
                val end = formatTimeMs(segment.endMs)
                val hasValidRange = segment.endMs > segment.startMs &&
                    segment.endMs - segment.startMs <= MAX_SUBTITLE_DURATION_MS
                builder.append("- ")
                if (segment.startMs > 0 || segment.endMs > 0) {
                    builder.append("[")
                        .append(begin)
                    if (hasValidRange) {
                        builder.append(" - ").append(end)
                    }
                    builder.append("] ")
                }
                label?.let { builder.append(it).append("：") }
                builder.append(segment.text.ifBlank { "（空白）" }).append("\n")
            }
            return builder.toString().trimEnd()
        }
        transcription?.text?.takeIf { it.isNotBlank() }?.let { raw ->
            return buildString {
                append("## 逐字稿（无说话人分离数据）\n")
                append(raw.trim())
            }
        }
        val segments = transcription?.segments.orEmpty()
        if (segments.isEmpty()) {
            return "暂无可用的转写结果。"
        }
        return buildString {
            append("## 逐字稿\n")
            segments.forEach { segment ->
                val begin = formatTime(segment.start)
                val end = formatTime(segment.end)
                val content = segment.text?.trim().orEmpty()
                append("- ")
                if (!begin.isNullOrBlank() || !end.isNullOrBlank()) {
                    append("[")
                    append(begin)
                    append(" - ")
                    append(end)
                    append("] ")
                }
                append(content.ifBlank { "（空白）" }).append("\n")
            }
        }.trimEnd()
    }

    /**
     * Build diarized segments with speaker separation and subtitle merging.
     */
    fun buildDiarizedSegments(transcription: TingwuTranscription?): List<DiarizedSegment> {
        if (transcription == null) return emptyList()
        
        val segments = transcription.segments.orEmpty()
        val hasUsableSegments = segments.any { !it.text.isNullOrBlank() && !it.speaker.isNullOrBlank() }

        if (!hasUsableSegments) {
            // No diarized material; let caller fall back to transcription.text or "暂无可用..."
            return emptyList()
        }

        // If we have usable segments, do NOT disable diarization just because text looks formatted.

        val speakerOrder = LinkedHashMap<String, Int>()
        transcription.speakers?.forEachIndexed { index, speaker ->
            speakerOrder[speaker.id] = index + 1
        }
        var nextIndex = speakerOrder.size + 1
        fun resolveSpeaker(idRaw: String): Pair<String, Int> {
            val key = idRaw.trim()
            val idx = speakerOrder.getOrPut(key) { nextIndex++ }
            return key to idx
        }
        val sortedSegments = transcription.segments.orEmpty()
            .filter { !it.text.isNullOrBlank() && !it.speaker.isNullOrBlank() }
            .sortedBy { it.start ?: 0.0 }
        if (sortedSegments.isEmpty()) {
            return emptyList()
        }

        val baseStartSeconds = sortedSegments.minOfOrNull { it.start ?: 0.0 }?.coerceAtLeast(0.0) ?: 0.0
        val diarized = sortedSegments.map { segment ->
            val (speakerId, speakerIndex) = resolveSpeaker(segment.speaker!!)
            val rawStart = (segment.start ?: 0.0) - baseStartSeconds
            val rawEnd = (segment.end ?: segment.start ?: 0.0) - baseStartSeconds
            val startMs = (rawStart * 1000).toLong()
            val endMs = (rawEnd * 1000).toLong()
            val normalizedStart = max(startMs, 0)
            val normalizedEnd = max(endMs, 0)
            val safeEnd = if (normalizedEnd >= normalizedStart) normalizedEnd else normalizedStart
            DiarizedSegment(
                speakerId = speakerId,
                speakerIndex = speakerIndex,
                startMs = normalizedStart,
                endMs = safeEnd,
                text = segment.text?.trim().orEmpty()
            )
        }
        val merged = mutableListOf<DiarizedSegment>()
        diarized.forEach { segment ->
            val last = merged.lastOrNull()
            if (last != null && shouldMergeAsSubtitle(last, segment)) {
                val combined = last.copy(
                    endMs = max(last.endMs, segment.endMs),
                    text = (last.text + " " + segment.text).trim()
                )
                merged[merged.lastIndex] = combined
            } else {
                merged += segment
            }
        }
        return merged
    }

    /**
     * Build speaker labels from Tingwu transcription metadata.
     */
    fun buildSpeakerLabels(
        transcription: TingwuTranscription?,
        segments: List<DiarizedSegment>,
    ): Map<String, String> {
        if (transcription == null || segments.isEmpty()) return emptyMap()
        val speakerIdsInOrder = segments
            .mapNotNull { it.speakerId }
            .distinct()
        if (speakerIdsInOrder.isEmpty()) return emptyMap()
        val speakersById = transcription.speakers.orEmpty().associateBy { it.id }
        val labels = LinkedHashMap<String, String>()
        speakerIdsInOrder.forEach { id ->
            val fromName = speakersById[id]?.name?.takeIf { it.isNotBlank() }
            val fallback = id
            labels[id] = fromName ?: fallback
        }
        return labels
    }

    private fun formatTime(value: Double?): String {
        if (value == null) return "00:00"
        val totalSeconds = max(value.toInt(), 0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
    }

    private fun formatTimeMs(value: Long): String {
        if (value <= 0) return "00:00"
        val totalSeconds = (value / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "${timeFormatter.format(hours)}:${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
        } else {
            "${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
        }
    }

    /** 判断两段是否可以安全合并为一行字幕。 */
    private fun shouldMergeAsSubtitle(previous: DiarizedSegment, next: DiarizedSegment): Boolean {
        if (previous.speakerIndex != next.speakerIndex) return false
        val gapMs = next.startMs - previous.endMs
        if (gapMs < 0 || gapMs > MAX_SUBTITLE_GAP_MS) return false
        val combinedDuration = max(next.endMs, previous.endMs) - min(previous.startMs, next.startMs)
        if (combinedDuration > MAX_SUBTITLE_DURATION_MS) return false
        val combinedLength = previous.text.length + 1 + next.text.length
        if (combinedLength > MAX_SUBTITLE_TEXT_LENGTH) return false
        return true
    }

    companion object {
        private const val MAX_SUBTITLE_GAP_MS = 2_000L
        private const val MAX_SUBTITLE_DURATION_MS = 10_000L
        private const val MAX_SUBTITLE_TEXT_LENGTH = 100
    }
}
