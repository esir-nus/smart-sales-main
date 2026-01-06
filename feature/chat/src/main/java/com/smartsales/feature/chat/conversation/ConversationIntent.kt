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
}
