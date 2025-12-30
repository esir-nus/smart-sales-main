package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publisher/ChatPublisherImpl.kt
// Module: :feature:chat
// Summary: Deterministic V1 publisher with no heuristic extraction.
// Author: created on 2025-12-29

class ChatPublisherImpl : ChatPublisher {
    private val validator = MachineArtifactValidator()

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

        // 按 V1 schema 做确定性校验，避免“能解析就算合法”
        val validation = jsonText?.let { validator.validate(it) }
        val status = if (validation?.isValid == true) ArtifactStatus.VALID else ArtifactStatus.INVALID
        // 透传确定性的失败原因，便于重试与排查
        val failureReason = when {
            jsonText == null -> "missing_json_fence"
            validation?.isValid == true -> null
            else -> validation?.reason ?: "invalid_machine_artifact"
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
