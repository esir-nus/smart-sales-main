// File: feature/chat/src/main/java/com/smartsales/domain/chat/InputClassifier.kt
// Module: :feature:chat
// Summary: Pure classification functions for user input analysis
// Author: created on 2026-01-05

package com.smartsales.domain.chat

import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi
import java.util.Locale

/**
 * Input classification result buckets.
 */
enum class InputBucket { NOISE, SHORT_RELEVANT, RICH }

/**
 * Analysis target containing content and its source.
 */
data class AnalysisTarget(val content: String, val source: String)

// Constants for classification thresholds
private const val LONG_CONTENT_THRESHOLD = 240
private const val CONTEXT_LENGTH_LIMIT = 800
private const val CONTEXT_MESSAGE_LIMIT = 5

/**
 * InputClassifier: Pure helper functions for classifying user input.
 *
 * Extracted from HomeScreenViewModel to domain layer.
 * All functions are stateless and operate purely on input data.
 */
object InputClassifier {

    /**
     * Classify user input based on content, keywords, and conversation history.
     */
    fun classifyUserInput(
        text: String,
        history: List<ChatMessageUi>
    ): InputBucket {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return InputBucket.NOISE
        val normalized = trimmed.lowercase(Locale.getDefault())
        val keywords = listOf("客户", "会议", "报价", "合同", "跟进", "电话", "录音", "邮件", "沟通", "销售")
        val hasKeyword = keywords.any { normalized.contains(it) }
        val hasLongHistory = history.any { it.content.length >= LONG_CONTENT_THRESHOLD }
        val hasTranscript = history.any { it.role == ChatMessageRole.ASSISTANT && it.content.contains("通话分析") }
        if (normalized.length <= 4 && !hasKeyword) {
            return InputBucket.NOISE
        }
        if (normalized.length <= 8 && !hasKeyword && !hasLongHistory) {
            return InputBucket.NOISE
        }
        if (trimmed.length >= LONG_CONTENT_THRESHOLD || hasLongHistory || hasTranscript) {
            return InputBucket.RICH
        }
        if (hasKeyword) return InputBucket.SHORT_RELEVANT
        return InputBucket.SHORT_RELEVANT
    }

    /**
     * Check if input is too low-info for smart analysis (e.g., "分析", "看看").
     */
    fun isLowInfoAnalysisInput(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.getDefault())
        if (normalized.isEmpty()) return true
        if (normalized.length <= 3) {
            val stop = setOf("的", "啊", "么", "吗", "吧", "哦", "呢", "hi", "hi!", "ok", "?", "？", "分析", "看看")
            if (stop.contains(normalized)) return true
        }
        val greetings = setOf("你好", "您好", "hello", "hi", "你是谁", "嘿", "早", "下午好", "晚上好")
        if (greetings.contains(normalized)) return true
        val filler = setOf("分析", "看看", "随便", "随便看看", "简单看看", "简单分析")
        if (filler.contains(normalized)) return true
        if (normalized.length <= 4 && normalized.all { it.isWhitespace() || it in listOf('的', '啊', '吗', '呢', '?', '？') }) {
            return true
        }
        return false
    }

    /**
     * Check if input is too low-info for general chat (e.g., "嗯", "好的").
     */
    fun isLowInfoGeneralChatInput(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.getDefault())
        if (normalized.isBlank()) return true
        val acknowledgements = setOf("是", "好的", "好", "嗯", "嗯嗯", "ok", "okay", "收到")
        return acknowledgements.contains(normalized)
    }

    /**
     * Find primary content for smart analysis from user input or conversation history.
     */
    fun findSmartAnalysisPrimaryContent(
        currentInput: String,
        messages: List<ChatMessageUi>
    ): AnalysisTarget? {
        val trimmed = currentInput.trim()
        if (trimmed.length >= LONG_CONTENT_THRESHOLD) {
            return AnalysisTarget(trimmed, "user_input")
        }
        val reversed = messages.asReversed()
        val transcript = reversed.firstOrNull { msg ->
            msg.role == ChatMessageRole.ASSISTANT &&
                msg.content.length >= LONG_CONTENT_THRESHOLD &&
                (msg.content.contains("转写") || msg.content.contains("通话") || msg.content.contains("录音"))
        }
        if (transcript != null) return AnalysisTarget(transcript.content, "transcript")
        val longHistory = reversed.firstOrNull { it.content.length >= LONG_CONTENT_THRESHOLD }
        return longHistory?.let { AnalysisTarget(it.content, "history_long") }
    }

    /**
     * Find context messages for analysis, excluding the primary content.
     */
    fun findContextForAnalysis(
        primaryContent: String,
        messages: List<ChatMessageUi>
    ): String? {
        val reversed = messages.asReversed()
        val contextChunks = mutableListOf<String>()
        var contextLength = 0
        reversed.forEach { msg ->
            val text = msg.content.trim()
            if (text.isEmpty() || text == primaryContent) return@forEach
            if (contextChunks.size >= CONTEXT_MESSAGE_LIMIT || contextLength >= CONTEXT_LENGTH_LIMIT) return@forEach
            val toAdd = text.take(CONTEXT_LENGTH_LIMIT - contextLength)
            contextChunks += toAdd
            contextLength += toAdd.length
        }
        return contextChunks.asReversed().joinToString("\n").takeIf { it.isNotBlank() }
    }
}
