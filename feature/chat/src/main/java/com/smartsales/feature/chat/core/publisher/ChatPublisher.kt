package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publisher/ChatPublisher.kt
// Module: :feature:chat
// Summary: Contract for deterministic V1 chat publishing.
// Author: created on 2025-12-29

interface ChatPublisher {
    fun publish(rawText: String, retryCount: Int = 0): PublishedChatTurnV1

    fun fallbackMessage(): String
}
