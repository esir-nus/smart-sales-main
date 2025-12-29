package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publisher/ChatPublisherImpl.kt
// Module: :feature:chat
// Summary: Deterministic V1 publisher with no heuristic extraction.
// Author: created on 2025-12-29

import org.json.JSONObject

class ChatPublisherImpl : ChatPublisher {

    override fun publish(rawText: String, retryCount: Int): PublishedChatTurnV1 {
        // 仅使用首个完整的 <visible2user>...</visible2user> 作为 HumanDraft
        val visibleMatch = VISIBLE_TAG_REGEX.find(rawText)
        val visibleRange = visibleMatch?.range
        val humanDraft = visibleMatch
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val displayMarkdown = humanDraft ?: ""

        // JSON fence 必须在 <visible2user> 区块之外
        val jsonMatch = JSON_FENCE_REGEX
            .findAll(rawText)
            .firstOrNull { match ->
                visibleRange == null || match.range.last < visibleRange.first || match.range.first > visibleRange.last
            }
        val jsonText = jsonMatch
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        // 只接受合法 JSON 对象，不做括号猜测或兜底解析
        val isValidJson = jsonText?.let { runCatching { JSONObject(it) }.isSuccess } ?: false

        val status = if (isValidJson) ArtifactStatus.VALID else ArtifactStatus.INVALID
        val failureReason = when {
            jsonText == null -> "missing_json_fence"
            isValidJson -> null
            else -> "invalid_json"
        }

        return PublishedChatTurnV1(
            displayMarkdown = displayMarkdown,
            machineArtifactJson = jsonText,
            artifactStatus = status,
            retryCount = retryCount,
            failureReason = failureReason
        )
    }

    override fun fallbackMessage(): String = FALLBACK_MESSAGE

    companion object {
        const val DEFAULT_MAX_RETRIES = 2

        private const val FALLBACK_MESSAGE = "Sorry, I couldn't generate a displayable reply. Please retry."
        private val VISIBLE_TAG_REGEX = Regex(
            "<\\s*visible2user\\s*>([\\s\\S]*?)<\\s*/\\s*visible2user\\s*>",
            RegexOption.IGNORE_CASE
        )
        private val JSON_FENCE_REGEX = Regex(
            "```\\s*json\\s*([\\s\\S]*?)```",
            RegexOption.IGNORE_CASE
        )
    }
}
