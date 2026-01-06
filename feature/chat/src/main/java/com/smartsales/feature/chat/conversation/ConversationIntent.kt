package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.home.ChatMessageUi

/**
 * User intents for conversation feature.
 * 
 * Pure Kotlin sealed interface - platform-agnostic.
 */
sealed interface ConversationIntent {
    /**
     * User is typing in the input field.
     */
    data class InputChanged(val text: String) : ConversationIntent
    
    /**
     * User wants to send the current input as a message.
     * @param timestamp Injected timestamp for deterministic Reducer behavior.
     */
    data class SendMessage(val timestamp: Long) : ConversationIntent
    
    /**
     * A new message (user or assistant) has been received.
     */
    data class MessageReceived(val message: ChatMessageUi) : ConversationIntent
    
    // P3.1.B2: Streaming intents
    /**
     * Streaming delta received during assistant response.
     * @param assistantId ID of the assistant message being updated
     * @param token New text token to append
     */
    data class StreamDelta(val assistantId: String, val token: String) : ConversationIntent
    
    /**
     * Streaming completed successfully.
     * @param assistantId ID of the assistant message being finalized
     * @param fullText Complete response text
     */
    data class StreamCompleted(val assistantId: String, val fullText: String) : ConversationIntent
    
    /**
     * Streaming error occurred.
     * @param error Error message
     */
    data class StreamError(val error: String) : ConversationIntent
    
    /**
     * User wants to run SmartAnalysis on current conversation.
     * @param timestamp Injected timestamp for deterministic Reducer behavior.
     * @param goal Optional analysis goal (e.g., "总结要点")
     */
    data class SendSmartAnalysis(
        val timestamp: Long,
        val goal: String = ""
    ) : ConversationIntent
}
