package com.smartsales.feature.chat.conversation

// File: feature/chat/src/main/java/com/smartsales/feature/chat/conversation/V1RetryConfig.kt
// Module: :feature:chat
// Summary: V1 retry configuration for streaming.
// Author: created on 2026-01-06

/**
 * V1 retry configuration for streaming.
 * 
 * @param maxRetries Maximum number of retry attempts (0 = no retry)
 * @param enableReasonAware Enable reason-aware retry logic
 */
data class V1RetryConfig(
    val maxRetries: Int = 2,
    val enableReasonAware: Boolean = true,
)
