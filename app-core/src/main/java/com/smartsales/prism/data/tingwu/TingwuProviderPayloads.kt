package com.smartsales.prism.data.tingwu

import com.smartsales.prism.domain.tingwu.DiarizedSegment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.LinkedHashMap

private val tingwuProviderJson = Json { ignoreUnknownKeys = true }

internal fun parseMeetingAssistanceKeywords(raw: String?): List<String> {
    val root = raw.parseJsonObjectOrNull() ?: return emptyList()
    val meetingAssistance = root["MeetingAssistance"] as? JsonObject ?: root
    return (meetingAssistance["Keywords"] as? JsonArray)
        ?.mapNotNull { it.primitiveContentOrNull() }
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        .orEmpty()
}

internal fun parseTranscriptionSpeakerLabels(transcriptionRoot: JsonObject): Map<String, String> {
    val speakers = (transcriptionRoot["Speakers"] as? JsonArray).orEmpty()
    return buildMap {
        speakers.forEach { speaker ->
            val obj = speaker as? JsonObject ?: return@forEach
            val speakerId = obj["Id"].primitiveContentOrNull()
                ?: obj["SpeakerId"].primitiveContentOrNull()
                ?: return@forEach
            val label = obj["Name"].primitiveContentOrNull()
                ?: obj["SpeakerName"].primitiveContentOrNull()
                ?: return@forEach
            if (speakerId.isNotBlank() && label.isNotBlank()) {
                put(speakerId, label)
            }
        }
    }
}

internal fun parseTranscriptionDiarizedSegments(transcriptionRoot: JsonObject): List<DiarizedSegment> {
    val segmentSegments = parseSegmentDiarizedSegments(transcriptionRoot)
    if (segmentSegments.isNotEmpty()) return segmentSegments
    return parseParagraphDiarizedSegments(transcriptionRoot)
}

internal fun parseIdentityRecognitionSpeakerLabels(payload: String?): Map<String, String> {
    val rootElement = payload.parseJsonElementOrNull() ?: return emptyMap()
    val items = rootElement.extractIdentityRecognitionItems()
    return buildMap {
        items.forEach { item ->
            val speakerId = item["SpeakerId"].primitiveContentOrNull()
                ?: item["Speaker"].primitiveContentOrNull()
                ?: return@forEach
            val identityLabel = item["Identity"].extractIdentityLabel()
                ?: item["IdentityName"].primitiveContentOrNull()
                ?: item["Name"].primitiveContentOrNull()
                ?: item["Role"].primitiveContentOrNull()
                ?: return@forEach
            if (speakerId.isNotBlank() && identityLabel.isNotBlank()) {
                put(speakerId, identityLabel)
            }
        }
    }
}

internal fun mergeTingwuSpeakerLabels(
    baseSpeakerLabels: Map<String, String>,
    diarizedSegments: List<DiarizedSegment>,
    identityRecognitionLabels: Map<String, String>
): Map<String, String> {
    val merged = linkedMapOf<String, String>()
    baseSpeakerLabels.forEach { (speakerId, label) ->
        if (speakerId.isNotBlank() && label.isNotBlank()) {
            merged[speakerId] = label
        }
    }
    diarizedSegments.mapNotNull { it.speakerId }
        .distinct()
        .forEach { speakerId ->
            if (speakerId.isNotBlank() && merged[speakerId].isNullOrBlank()) {
                merged[speakerId] = speakerId
            }
        }
    identityRecognitionLabels.forEach { (speakerId, label) ->
        if (speakerId.isNotBlank() && label.isNotBlank()) {
            merged[speakerId] = label
        }
    }
    return merged
}

private fun String?.parseJsonObjectOrNull(): JsonObject? =
    parseJsonElementOrNull() as? JsonObject

private fun parseSegmentDiarizedSegments(transcriptionRoot: JsonObject): List<DiarizedSegment> {
    val segments = (transcriptionRoot["Segments"] as? JsonArray).orEmpty()
    if (segments.isEmpty()) return emptyList()
    val speakerIndexes = SpeakerIndexResolver()
    return segments.mapNotNull { element ->
        val segment = element as? JsonObject ?: return@mapNotNull null
        val text = segment["Text"].primitiveContentOrNull() ?: return@mapNotNull null
        val speaker = segment["SpeakerId"].primitiveContentOrNull()
            ?: segment["Speaker"].primitiveContentOrNull()
            ?: return@mapNotNull null
        DiarizedSegment(
            speakerId = speaker,
            speakerIndex = speakerIndexes.indexFor(speaker),
            startMs = (segment["Start"].primitiveDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0)
                .secondsToMillis(),
            endMs = (segment["End"].primitiveDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0)
                .secondsToMillis(),
            text = text.trim()
        )
    }
}

private fun parseParagraphDiarizedSegments(transcriptionRoot: JsonObject): List<DiarizedSegment> {
    val paragraphs = (transcriptionRoot["Paragraphs"] as? JsonArray).orEmpty()
    if (paragraphs.isEmpty()) return emptyList()
    val speakerIndexes = SpeakerIndexResolver()
    val segments = mutableListOf<DiarizedSegment>()

    paragraphs.forEachIndexed { paragraphIndex, paragraphElement ->
        val paragraph = paragraphElement as? JsonObject ?: return@forEachIndexed
        val paragraphSpeaker = paragraph["SpeakerId"].primitiveContentOrNull()
            ?: paragraph["Speaker"].primitiveContentOrNull()
        val words = (paragraph["Words"] as? JsonArray).orEmpty()
        if (words.isEmpty()) return@forEachIndexed

        val grouped = linkedMapOf<String, MutableList<JsonObject>>()
        for (wordIndex in 0 until words.size) {
            val word = words[wordIndex] as? JsonObject ?: continue
            val groupKey = word["SentenceId"].primitiveContentOrNull()
                ?: word["Id"].primitiveContentOrNull()
                ?: "${paragraphIndex}_$wordIndex"
            grouped.getOrPut(groupKey) { mutableListOf() }.add(word)
        }

        grouped.values.forEach { sentenceWords ->
            val sorted = sentenceWords.sortedBy { it["Start"].primitiveDoubleOrNull() ?: 0.0 }
            val text = sorted.joinToString(separator = "") { word ->
                word["Text"].primitiveContentOrNull().orEmpty()
            }.trim()
            if (text.isBlank()) return@forEach
            val speaker = sorted.firstNotNullOfOrNull { word ->
                word["SpeakerId"].primitiveContentOrNull()
                    ?: word["Speaker"].primitiveContentOrNull()
            } ?: paragraphSpeaker ?: return@forEach
            val startMs = (sorted.firstOrNull()?.get("Start").primitiveDoubleOrNull() ?: 0.0)
                .millisDoubleToLong()
            val endMs = (sorted.lastOrNull()?.get("End").primitiveDoubleOrNull()
                ?: sorted.lastOrNull()?.get("Start").primitiveDoubleOrNull()
                ?: 0.0)
                .millisDoubleToLong()
            segments += DiarizedSegment(
                speakerId = speaker,
                speakerIndex = speakerIndexes.indexFor(speaker),
                startMs = startMs,
                endMs = endMs.coerceAtLeast(startMs),
                text = text
            )
        }
    }

    return segments.sortedBy { it.startMs }
}

private fun String?.parseJsonElementOrNull(): JsonElement? {
    if (this.isNullOrBlank()) return null
    return runCatching { tingwuProviderJson.parseToJsonElement(this) }.getOrNull()
}

private fun JsonElement.extractIdentityRecognitionItems(): List<JsonObject> {
    return when (this) {
        is JsonArray -> mapNotNull { it as? JsonObject }
        is JsonObject -> {
            val dataObject = this["Data"] as? JsonObject
            val directArray = this["IdentityRecognition"] as? JsonArray
                ?: this["IdentityResults"] as? JsonArray
                ?: this["IdentityResult"] as? JsonArray
                ?: dataObject?.get("IdentityRecognition") as? JsonArray
                ?: dataObject?.get("IdentityResults") as? JsonArray
                ?: dataObject?.get("IdentityResult") as? JsonArray
            when {
                directArray != null -> directArray.mapNotNull { it as? JsonObject }
                this["IdentityRecognition"] is JsonObject -> listOfNotNull(this["IdentityRecognition"] as? JsonObject)
                dataObject?.get("IdentityRecognition") is JsonObject -> listOfNotNull(dataObject["IdentityRecognition"] as? JsonObject)
                else -> emptyList()
            }
        }
        else -> emptyList()
    }
}

private fun JsonElement?.extractIdentityLabel(): String? {
    return when (this) {
        is JsonPrimitive -> primitiveContentOrNull()
        is JsonObject -> this["Name"].primitiveContentOrNull()
            ?: this["IdentityName"].primitiveContentOrNull()
            ?: this["Role"].primitiveContentOrNull()
            ?: this["Label"].primitiveContentOrNull()
        else -> null
    }
}

private fun JsonElement?.primitiveContentOrNull(): String? =
    (this as? JsonPrimitive)
        ?.let { primitive -> runCatching { primitive.content }.getOrNull() }
        ?.takeIf { value -> value.isNotBlank() }

private fun JsonElement?.primitiveDoubleOrNull(): Double? =
    (this as? JsonPrimitive)
        ?.let { primitive -> runCatching { primitive.content.toDoubleOrNull() }.getOrNull() }

private fun Double.secondsToMillis(): Long = (this * 1000.0).toLong()

private fun Double.millisDoubleToLong(): Long = toLong().coerceAtLeast(0L)

private class SpeakerIndexResolver {
    private val indexes = LinkedHashMap<String, Int>()

    fun indexFor(speakerId: String): Int {
        return indexes.getOrPut(speakerId) { indexes.size }
    }
}
