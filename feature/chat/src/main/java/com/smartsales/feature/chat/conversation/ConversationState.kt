package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi

/**
 * Immutable state for conversation feature.
 * 
 * This is the "Portable Brain" - pure Kotlin with zero Android dependencies.
 * Can be transferred to iOS/HarmonyOS/Web without modification.
 */
data class ConversationState(
    val messages: List<ChatMessageUi> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isStreaming: Boolean = false,
    val errorMessage: String? = null
)
