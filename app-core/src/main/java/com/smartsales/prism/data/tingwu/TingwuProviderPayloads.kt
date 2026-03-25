package com.smartsales.prism.data.tingwu

import com.smartsales.prism.domain.tingwu.DiarizedSegment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
