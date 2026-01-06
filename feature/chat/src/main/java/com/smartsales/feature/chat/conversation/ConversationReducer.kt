package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi

/**
 * Pure business logic for conversation feature.
 * 
 * This is the "Portable Brain" - no Android imports, no side effects, 100% testable.
 * 
 * Design:
 * - `reduce()` is a pure function: (State, Intent) -> State
 * - Deterministic: same inputs always produce same outputs
 * - Side effects (streaming, persistence) handled by ViewModel
 */
object ConversationReducer {
    
    /**
     * Main reducer function.
     * Transforms state based on user intent.
     */
    fun reduce(state: ConversationState, intent: ConversationIntent): ConversationState =
        when (intent) {
            is ConversationIntent.InputChanged -> handleInputChanged(state, intent)
            is ConversationIntent.SendMessage -> handleSendMessage(state, intent)
            is ConversationIntent.MessageReceived -> handleMessageReceived(state, intent)
        }
    
    private fun handleInputChanged(
        state: ConversationState,
        intent: ConversationIntent.InputChanged
    ): ConversationState {
        return state.copy(inputText = intent.text)
    }
    
    private fun handleSendMessage(
        state: ConversationState,
        intent: ConversationIntent.SendMessage
    ): ConversationState {
        // Guard: don't send if already sending or input is blank
        if (state.isSending || state.inputText.isBlank()) {
            return state
        }
        
        // Create user message
        val userMessage = ChatMessageUi(
            role = ChatMessageRole.USER,
            content = state.inputText.trim(),
            timestampMillis = intent.timestamp
        )
        
        // Update state: add message, clear input, mark as sending
        return state.copy(
            messages = state.messages + userMessage,
            inputText = "",
            isSending = true,
            errorMessage = null
        )
    }
    
    private fun handleMessageReceived(
        state: ConversationState,
        intent: ConversationIntent.MessageReceived
    ): ConversationState {
        return state.copy(
            messages = state.messages + intent.message,
            isSending = false
        )
    }
}
