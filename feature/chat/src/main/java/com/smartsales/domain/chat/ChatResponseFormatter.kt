// File: feature/chat/src/main/java/com/smartsales/domain/chat/ChatResponseFormatter.kt
// Module: :feature:chat
// Summary: Formats AI chat responses into structured markdown for display
// Author: created on 2026-01-07

package com.smartsales.domain.chat

/**
 * ChatResponseFormatter: Formats AI chat responses into structured markdown.
 *
 * Responsibility: Presentation formatting of AI responses.
 * Layer: Domain (formatting is business logic, not I/O)
 *
 * Extracted from DashscopeAiChatService to enforce layer purity:
 * - Data layer should only do I/O (HTTP, parsing)
 * - Formatting belongs in domain layer
 */
object ChatResponseFormatter {

    /**
     * Build structured markdown from AI response and request context.
     *
     * @param displayText Raw AI response text
     * @param prompt Original user prompt
     * @param skillTags List of skill tags used
     * @param attachmentCount Number of attachments in request
     * @param transcriptMarkdown Optional transcript markdown
     * @return Formatted markdown string
     */
    fun buildStructuredMarkdown(
        displayText: String,
        prompt: String,
        skillTags: List<String> = emptyList(),
        attachmentCount: Int = 0,
        transcriptMarkdown: String? = null
    ): String {
        val builder = StringBuilder()
        if (displayText.isNotBlank()) {
            builder.append(displayText.trim()).append("\n\n")
        }
        builder.append("## 输入摘要\n")
            .append("- 原始提问：")
            .append(prompt.trim())
            .append("\n")
            .append("- 技能：")
            .append(skillTags.takeIf { it.isNotEmpty() }?.joinToString("、") ?: "默认")
            .append("\n")
            .append("- 附件数：")
            .append(attachmentCount)
            .append("\n")

        transcriptMarkdown?.takeIf { it.isNotBlank() }?.let {
            builder.append("\n## 会议纪要节选\n")
                .append(it.trim())
                .append("\n")
        }
        return builder.toString().trim()
    }
}
