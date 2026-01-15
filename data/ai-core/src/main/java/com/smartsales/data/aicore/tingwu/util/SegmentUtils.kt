package com.smartsales.data.aicore.tingwu.util

import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import java.util.LinkedHashMap
import kotlin.math.max

/**
 * Builds DiarizedSegments from Tingwu transcription with a time offset.
 *
 * @param transcription The Tingwu transcription data
 * @param baseOffsetMs Time offset to apply (for multi-batch stitching)
 * @param shouldMerge Predicate to determine if consecutive segments should be merged
 */
internal fun buildRecordingOriginSegmentsWithOffset(
    transcription: TingwuTranscription?,
    baseOffsetMs: Long,
    shouldMerge: (DiarizedSegment, DiarizedSegment) -> Boolean
): List<DiarizedSegment> {
    if (transcription == null) return emptyList()
    val segments = transcription.segments.orEmpty()
    val hasUsableSegments = segments.any { !it.text.isNullOrBlank() && !it.speaker.isNullOrBlank() }
    if (!hasUsableSegments) return emptyList()

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

    // 说明：baseOffsetMs 用于 per-window 切片锚定；录音起点绝对时间 = baseOffsetMs + 相对时间。
    val diarized = sortedSegments.map { segment ->
        val (speakerId, speakerIndex) = resolveSpeaker(segment.speaker!!)
        val rawStart = segment.start ?: 0.0
        val rawEnd = segment.end ?: segment.start ?: 0.0
        val startMs = (rawStart * 1000).toLong() + baseOffsetMs
        val endMs = (rawEnd * 1000).toLong() + baseOffsetMs
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
        if (last != null && shouldMerge(last, segment)) {
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
