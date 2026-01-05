// File: feature/chat/src/main/java/com/smartsales/domain/chat/MetadataParser.kt
// Module: :feature:chat
// Summary: Pure metadata JSON parsing helpers
// Author: created on 2026-01-05

package com.smartsales.domain.chat

import com.smartsales.core.metahub.SessionMetadata

/**
 * MetadataParser: Pure JSON parsing utilities for metadata extraction.
 *
 * Extracted from HomeScreenViewModel to isolate pure parsing logic.
 */
object MetadataParser {

    /**
     * JSON block container with position info.
     */
    data class JsonBlock(val text: String, val startIndex: Int)

    /**
     * Parse general chat metadata from JSON text.
     *
     * @param jsonText Raw JSON string
     * @param sessionId Session ID for the metadata
     * @return Parsed SessionMetadata or null if parsing fails
     */
    fun parseGeneralChatMetadata(jsonText: String, sessionId: String): SessionMetadata? = runCatching {
        val obj = org.json.JSONObject(jsonText)
        val summary6 = obj.optString("summary_title_6chars").takeIf { it.isNotBlank() }?.take(6)
        val summary8 = obj.optString("summary_title_8chars").takeIf { it.isNotBlank() }?.take(8)
        SessionMetadata(
            sessionId = sessionId,
            mainPerson = obj.optString("main_person").takeIf { it.isNotBlank() },
            shortSummary = obj.optString("short_summary").takeIf { it.isNotBlank() },
            summaryTitle6Chars = summary6 ?: summary8?.take(6),
            location = obj.optString("location").takeIf { it.isNotBlank() }
        )
    }.getOrNull()

    /**
     * Find the last JSON block in text.
     *
     * Strategy:
     * 1. First try fenced ```json blocks
     * 2. Then try any fenced ``` blocks
     * 3. Finally search for trailing {...} object
     *
     * @param text Input text to search
     * @return JsonBlock with content and position, or null if not found
     */
    fun findLastJsonBlock(text: String): JsonBlock? {
        // 优先提取 fenced JSON（```json ... ```），否则尝试抓取末尾裸 JSON 对象
        val fencedRegex = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val fenced = fencedRegex.findAll(text).lastOrNull()
        val fencedContent = fenced?.groupValues?.getOrNull(1)?.trim()
        if (!fencedContent.isNullOrBlank() && fencedContent.startsWith("{") && fencedContent.endsWith("}")) {
            val braceStart = text.indexOf('{', fenced.range.first)
            return JsonBlock(fencedContent, if (braceStart >= 0) braceStart else fenced.range.first)
        }

        val anyFenceRegex = Regex("```\\s*([\\s\\S]*?)```")
        val anyFence = anyFenceRegex.findAll(text).lastOrNull()
        val anyContent = anyFence?.groupValues?.getOrNull(1)?.trim()
        if (!anyContent.isNullOrBlank() && anyContent.startsWith("{") && anyContent.endsWith("}")) {
            val braceStart = text.indexOf('{', anyFence.range.first)
            return JsonBlock(anyContent, if (braceStart >= 0) braceStart else anyFence.range.first)
        }

        val trimmed = text.trimEnd()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return JsonBlock(trimmed, text.lastIndexOf('{'))

        // 从末尾向前寻找最后一对完整的大括号，确保 JSON 位于文本末尾
        fun findMatchingEnd(content: String, start: Int): Int {
            var depth = 0
            for (i in start until content.length) {
                when (content[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }
            return -1
        }

        var start = text.lastIndexOf('{')
        while (start >= 0) {
            val end = findMatchingEnd(text, start)
            if (end > start && text.substring(end + 1).trim().isEmpty()) {
                val block = text.substring(start, end + 1)
                return JsonBlock(block, start)
            }
            start = text.lastIndexOf('{', start - 1)
        }
        return null
    }
}
