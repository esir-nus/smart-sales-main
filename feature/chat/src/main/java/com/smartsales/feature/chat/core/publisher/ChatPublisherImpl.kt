package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publisher/ChatPublisherImpl.kt
// Module: :feature:chat
// Summary: Deterministic V1 publisher with no heuristic extraction.
// Author: created on 2025-12-29

class ChatPublisherImpl : ChatPublisher {
    private val validator = MachineArtifactValidator()

    override fun publish(rawText: String, retryCount: Int): PublishedChatTurnV1 {
        val visibleSpan = findFirstVisibleSpan(rawText)
        val displayMarkdown = visibleSpan?.innerText.orEmpty()
        val visibleOk = visibleSpan?.failureReason == null && displayMarkdown.isNotBlank()

        // JSON fence 必须在 <visible2user> 区块之外，避免泄露到 UI 并保持确定性
        val jsonText = findFirstJsonFenceOutside(rawText, visibleSpan?.range)

        val validation = jsonText?.let { validator.validate(it) }
        val jsonRequiredOk = jsonText != null && validation?.isValid == true
        val status = if (visibleOk && jsonRequiredOk) ArtifactStatus.VALID else ArtifactStatus.INVALID
        // 禁止启发式 JSON 兜底，必须走 fenced block，避免非确定性解析
        val failureReason = when {
            !visibleOk -> visibleSpan?.failureReason ?: "missing_visible2user"
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
        private const val VISIBLE_START = "<visible2user>"
        private const val VISIBLE_END = "</visible2user>"
        private const val JSON_FENCE_START = "```json"
        private const val FENCE = "```"
    }

    private data class VisibleSpan(
        val range: IntRange,
        val innerText: String,
        val failureReason: String? = null,
    )

    private fun findFirstVisibleSpan(rawText: String): VisibleSpan? {
        val start = rawText.indexOf(VISIBLE_START)
        if (start < 0) {
            return VisibleSpan(IntRange.EMPTY, "", "missing_visible2user")
        }
        val end = rawText.indexOf(VISIBLE_END, start + VISIBLE_START.length)
        if (end < 0) {
            return VisibleSpan(IntRange.EMPTY, "", "malformed_visible2user")
        }
        val innerStart = start + VISIBLE_START.length
        val inner = rawText.substring(innerStart, end).trim()
        if (inner.isBlank()) {
            return VisibleSpan(IntRange.EMPTY, "", "empty_visible2user")
        }
        return VisibleSpan(
            range = start..(end + VISIBLE_END.length - 1),
            innerText = inner
        )
    }

    private fun findFirstJsonFenceOutside(
        rawText: String,
        visibleRange: IntRange?
    ): String? {
        var searchIndex = 0
        while (true) {
            val fenceStart = rawText.indexOf(JSON_FENCE_START, searchIndex)
            if (fenceStart < 0) return null
            if (visibleRange != null && fenceStart in visibleRange) {
                searchIndex = fenceStart + JSON_FENCE_START.length
                continue
            }
            val contentStart = rawText.indexOf('\n', fenceStart)
                .takeIf { it >= 0 } ?: return null
            val fenceEnd = rawText.indexOf(FENCE, contentStart + 1)
            if (fenceEnd < 0) return null
            val payload = rawText.substring(contentStart + 1, fenceEnd).trim()
            if (payload.isNotBlank()) return payload
            searchIndex = fenceEnd + FENCE.length
        }
    }
}
