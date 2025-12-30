package com.smartsales.feature.chat.core.v1

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/v1/V1GeneralRetryEffects.kt
// Module: :feature:chat
// Summary: V1 GENERAL retry side-effect adapter for ViewModel wiring.
// Author: created on 2025-12-30

@Suppress("UNUSED_PARAMETER")
class V1GeneralRetryEffects(
    private val resetDeduper: () -> Unit,
    private val resetPlaceholder: () -> Unit,
    private val publishTerminal: suspend (String) -> Unit,
) {
    // 仅封装副作用；决策逻辑保持在纯策略层，降低 ViewModel 复杂度
    suspend fun onRetryStart(nextAttempt: Int) {
        resetDeduper()
        resetPlaceholder()
    }

    suspend fun onTerminal(rawFullText: String, attempt: Int) {
        publishTerminal(rawFullText)
    }
}
