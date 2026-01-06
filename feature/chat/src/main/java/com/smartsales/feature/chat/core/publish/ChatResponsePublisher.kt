package com.smartsales.feature.chat.core.publish

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publish/ChatResponsePublisher.kt
// Module: :feature:chat
// Summary: Handles post-stream chat response publishing logic.
// Author: created on 2026-01-06

import com.smartsales.domain.chat.ChatPublisher
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.publisher.GeneralChatV1Finalizer

private const val SMART_ANALYSIS_FAILURE_TEXT = "本次智能分析暂时不可用，请稍后重试。"

/**
 * Publishes completed chat responses.
 * 
 * Extracted from HomeScreenViewModel.handleStreamCompleted().
 * Pure Kotlin logic for display text resolution and V1 finalization.
 */
class ChatResponsePublisher(
    private val v1Finalizer: GeneralChatV1Finalizer,
) {
    /**
     * Publish a completed chat response.
     * 
     * @param rawFullText Raw LLM response text
     * @param request Original chat request
     * @param enableV1Publisher Whether to use V1 publisher logic
     * @param isSmartAnalysis Whether this is a SMART_ANALYSIS request
     * @param isFirstGeneralReply Whether this is the first assistant reply in general chat
     * @return PublishResult with display text and V1 result
     */
    fun publish(
        rawFullText: String,
        request: ChatRequest,
        enableV1Publisher: Boolean,
        isSmartAnalysis: Boolean,
        isFirstGeneralReply: Boolean
    ): PublishResult {
        val isGeneralChat = request.quickSkillId == null
        val useV1Publisher = enableV1Publisher && isGeneralChat && !isSmartAnalysis
        
        // V1 finalization
        val v1Result = if (useV1Publisher) {
            v1Finalizer.finalize(rawFullText)
        } else null
        
        // Resolve display text
        val displayText = resolveDisplayText(
            rawFullText = rawFullText,
            useV1Publisher = useV1Publisher,
            v1VisibleMarkdown = v1Result?.visibleMarkdown,
            isSmartAnalysis = isSmartAnalysis
        )
        
        // Check for SmartAnalysis failure
        val isSmartFailure = isSmartAnalysis && displayText.trim() == SMART_ANALYSIS_FAILURE_TEXT
        
        return PublishResult(
            displayText = displayText,
            v1Result = v1Result,
            isSmartFailure = isSmartFailure
        )
    }
    
    private fun resolveDisplayText(
        rawFullText: String,
        useV1Publisher: Boolean,
        v1VisibleMarkdown: String?,
        isSmartAnalysis: Boolean
    ): String {
        return when {
            useV1Publisher && v1VisibleMarkdown != null -> v1VisibleMarkdown
            isSmartAnalysis -> {
                // SmartAnalysis uses raw text or fallback
                if (rawFullText.isBlank()) SMART_ANALYSIS_FAILURE_TEXT else rawFullText
            }
            else -> rawFullText
        }
    }
}
