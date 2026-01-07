// File: feature/chat/src/main/java/com/smartsales/domain/chat/ChatCoordinator.kt
// Module: :feature:chat
// Summary: Chat orchestration coordinator - owns chat/streaming business logic
// Author: created on 2026-01-07

package com.smartsales.domain.chat

import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.domain.error.ChatError
import com.smartsales.feature.usercenter.SalesPersona
import kotlinx.coroutines.flow.Flow

/**
 * ChatCoordinator: Domain-layer coordinator for chat and streaming orchestration.
 *
 * Responsibilities:
 * - Build chat requests from user input
 * - Orchestrate streaming responses via StreamingCoordinator
 * - Extract and persist metadata (first reply only)
 * - Handle V1 retry logic
 * - Emit events for UI consumption
 *
 * Design:
 * - Zero Android imports (portable)
 * - Event-based (Flow<ChatEvent>)
 * - Stateless (except for active stream tracking)
 */
interface ChatCoordinator {
    
    /**
     * Chat events stream - HomeViewModel collects and maps to UI state.
     */
    val chatEvents: Flow<ChatEvent>
    
    /**
     * Send a regular chat message.
     */
    fun sendMessage(params: SendMessageParams)
    
    /**
     * Send a SmartAnalysis request.
     */
    fun sendSmartAnalysis(params: SmartAnalysisParams)
    
    /**
     * Reset streaming state (on session switch or error recovery).
     */
    fun resetStream()
}

/**
 * Chat events emitted by ChatCoordinator.
 */
sealed class ChatEvent {
    /**
     * Streaming started - assistant message placeholder created.
     */
    data class StreamStarted(val assistantId: String) : ChatEvent()
    
    /**
     * Streaming delta - token received from LLM.
     */
    data class StreamDelta(val assistantId: String, val token: String) : ChatEvent()
    
    /**
     * Streaming completed - final result ready for UI.
     */
    data class StreamCompleted(val result: ChatCompletionResult) : ChatEvent()
    
    /**
     * Streaming error - display error to user.
     */
    data class StreamError(val assistantId: String, val error: ChatError) : ChatEvent()
}

/**
 * Final chat completion result with all extracted data.
 */
data class ChatCompletionResult(
    val assistantId: String,
    val rawFullText: String,
    val displayText: String,
    val metadata: SessionMetadata?,  // Nullable, only for first reply
    val titleCandidate: TitleCandidate?,  // Nullable, extracted from <Rename> tag
    val isSmartAnalysis: Boolean
)

/**
 * Title candidate extracted from <Rename> block.
 */
data class TitleCandidate(
    val name: String?,
    val title6: String?,
    val source: TitleSource,
    val createdAt: Long
)

enum class TitleSource {
    GENERAL,
    SMART_ANALYSIS
}

/**
 * Parameters for sending a regular message.
 */
data class SendMessageParams(
    val sessionId: String,
    val userMessage: String,
    val skillId: String?,  // QuickSkillId mapped to mode string
    val audioContext: AudioContextSummary?,
    val chatHistory: List<ChatHistoryMessage>,
    val isFirstAssistantReply: Boolean,
    val persona: SalesPersona?,
    val timestamp: Long
)

/**
 * Parameters for sending SmartAnalysis.
 */
data class SmartAnalysisParams(
    val sessionId: String,
    val goal: String,
    val chatHistory: List<ChatHistoryMessage>,
    val persona: SalesPersona?,
    val timestamp: Long
)

/**
 * Chat history message for context building.
 */
data class ChatHistoryMessage(
    val role: MessageRole,
    val content: String
)

enum class MessageRole {
    USER,
    ASSISTANT
}

/**
 * Audio context summary for chat requests.
 */
data class AudioContextSummary(
    val readyClipCount: Int,
    val pendingClipCount: Int,
    val hasTranscripts: Boolean,
    val note: String?
)
