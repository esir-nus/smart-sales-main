// File: feature/chat/src/main/java/com/smartsales/domain/chat/ChatMessageBuilder.kt
// Module: :feature:chat
// Summary: Pure helper functions for building chat messages and export formats
// Author: created on 2026-01-05

package com.smartsales.domain.chat

import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi

/**
 * Pure utility functions for building chat message content.
 * No side effects, no state access.
 */
object ChatMessageBuilder {
    
    private const val ANALYSIS_CONTENT_LIMIT = 2400

    /**
     * Builds a structured user message for smart analysis.
     */
    fun buildSmartAnalysisUserMessage(
        mainContent: String,
        context: String?,
        goal: String
    ): String {
        val cappedContent = mainContent.trim().take(ANALYSIS_CONTENT_LIMIT)
        val builder = StringBuilder()
        builder.appendLine("分析目标：").appendLine(goal.trim()).appendLine()
        builder.appendLine("主体内容（供重点分析）：").appendLine(cappedContent).appendLine()
        context?.takeIf { it.isNotBlank() }?.let {
            builder.appendLine("最近上下文：").appendLine(it.trim()).appendLine()
        }
        builder.appendLine("请基于上述主体内容完成结构化智能分析。")
        return builder.toString().trim()
    }

    /**
     * Builds markdown transcript from chat messages for export.
     */
    fun buildTranscriptMarkdown(messages: List<ChatMessageUi>): String {
        if (messages.isEmpty()) return ""
        val builder = StringBuilder()
        builder.append("# 对话记录\n\n")
        messages.forEach { msg ->
            val role = if (msg.role == ChatMessageRole.USER) "用户" else "助手"
            val text = msg.sanitizedContent ?: msg.content
            builder.append("- **$role**：$text\n")
        }
        return builder.toString()
    }

    /**
     * Wraps smart analysis body with export header.
     */
    fun wrapSmartAnalysisForExport(body: String): String =
        buildString {
            append("智能分析结果\n\n")
            append(body.trim())
        }.trim()
}
