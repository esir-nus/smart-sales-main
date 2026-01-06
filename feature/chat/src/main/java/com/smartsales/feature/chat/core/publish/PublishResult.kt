package com.smartsales.feature.chat.core.publish

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publish/PublishResult.kt
// Module: :feature:chat
// Summary: Result data class for ChatResponsePublisher.
// Author: created on 2026-01-06

import com.smartsales.feature.chat.core.publisher.V1FinalizeResult

/**
 * Result from publishing a chat response.
 */
data class PublishResult(
    val displayText: String,
    val v1Result: V1FinalizeResult?,
    val isSmartFailure: Boolean,
)
