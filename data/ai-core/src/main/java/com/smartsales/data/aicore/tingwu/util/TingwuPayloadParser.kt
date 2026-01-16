// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/util/TingwuPayloadParser.kt
// Module: :data:ai-core
// Summary: Pure parsing functions for Tingwu API payloads
// Author: Extracted from TingwuRunner 2026-01-15

package com.smartsales.data.aicore.tingwu.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuSmartSummary
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import com.smartsales.data.aicore.DiarizedSegment
import java.util.LinkedHashMap
import kotlin.math.max

/**
 * TingwuPayloadParser: Pure parsing functions for Tingwu API responses.
 *
 * Extracted from TingwuRunner for vibe coding clarity.
 * All functions are pure (no side effects, no dependencies).
 */
internal object TingwuPayloadParser {

    /**
     * Parses AutoChapters JSON payload into TingwuChapter list.
     * Handles multiple JSON formats from Tingwu API.
     */
    fun parseAutoChapters(json: String): List<TingwuChapter> {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return emptyList()
        val array: JsonArray? = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> {
                val obj = root.asJsonObject
                obj.getAsJsonArray("AutoChapters")
                    ?: obj.getAsJsonArray("Chapters")
                    ?: obj.getAsJsonArray("chapters")
                    ?: obj.getAsJsonArray("Items")
                    ?: obj.getAsJsonArray("items")
            }
            else -> null
        }
        val target = array ?: return emptyList()
        return target.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val obj = element.asJsonObject
            val headline = obj.getPrimitiveString("Headline")
            val title = obj.getPrimitiveString("Title")
                ?: obj.getPrimitiveString("title")
                ?: obj.getPrimitiveString("Name")
                ?: obj.getPrimitiveString("name")
            val summary = obj.getPrimitiveString("Summary")
            val startRaw = obj.getPrimitiveNumber("Start")
                ?: obj.getPrimitiveNumber("StartTime")
                ?: obj.getPrimitiveNumber("StartMs")
            val endRaw = obj.getPrimitiveNumber("End")
                ?: obj.getPrimitiveNumber("EndTime")
                ?: obj.getPrimitiveNumber("EndMs")
            val startMs = startRaw?.let { toMillis(it) }
            val endMs = endRaw?.let { toMillis(it) }
            val displayTitle = headline ?: title
            if (displayTitle.isNullOrBlank() || startMs == null) return@mapNotNull null
            TingwuChapter(
                title = displayTitle,
                startMs = startMs,
                endMs = endMs,
                headline = headline,
                summary = summary
            )
        }
    }

    /**
     * Parses SmartSummary JSON payload.
     */
    fun parseSmartSummary(json: String): TingwuSmartSummary? {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return null
        if (!root.isJsonObject) return null
        val obj = root.asJsonObject
        val summary = obj.getPrimitiveString("Summary")
            ?: obj.getPrimitiveString("Abstract")
            ?: obj.getPrimitiveString("Summarization")
        val keyPoints = obj.getAsJsonArray("KeyPoints")
            ?: obj.getAsJsonArray("Highlights")
            ?: obj.getAsJsonArray("Keypoints")
        val actionItems = obj.getAsJsonArray("ActionItems")
            ?: obj.getAsJsonArray("Todos")
            ?: obj.getAsJsonArray("Tasks")
        val keys = keyPoints?.mapNotNull { it.asStringOrNull() } ?: emptyList()
        val actions = actionItems?.mapNotNull { it.asStringOrNull() } ?: emptyList()
        if (summary.isNullOrBlank() && keys.isEmpty() && actions.isEmpty()) return null
        return TingwuSmartSummary(
            summary = summary,
            keyPoints = keys,
            actionItems = actions
        )
    }

    /**
     * Builds diarized segments from transcription with time offset.
     * Used for multi-batch stitching.
     */
    fun buildDiarizedSegments(
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

    // --- JSON Helpers (internal for shared use) ---

    internal fun JsonObject.getPrimitiveString(key: String): String? =
        this.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    internal fun JsonObject.getPrimitiveNumber(key: String): Number? =
        this.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asNumber

    internal fun JsonElement.asStringOrNull(): String? =
        takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
    
    internal fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        takeIf { it.isJsonObject }?.asJsonObject

    internal fun JsonElement.asLongOrNull(): Long? {
        return runCatching {
            when {
                isJsonPrimitive && this.asJsonPrimitive.isNumber -> this.asLong
                isJsonPrimitive && this.asJsonPrimitive.isString -> this.asJsonPrimitive.asString.toLongOrNull()
                else -> null
            }
        }.getOrNull()
    }

    private fun toMillis(number: Number): Long {
        val value = number.toDouble()
        return if (value > 100000) value.toLong() else (value * 1000).toLong()
    }

    /**
     * Formats milliseconds to time string (HH:MM:SS).
     * Always uses full HH:MM:SS format for consistency.
     */
    fun formatTimeMs(value: Long): String {
        if (value <= 0) return "00:00:00"
        val totalSeconds = (value / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
