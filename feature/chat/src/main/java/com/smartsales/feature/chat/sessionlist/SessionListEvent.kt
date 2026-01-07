// File: feature/chat/src/main/java/com/smartsales/feature/chat/sessionlist/SessionListEvent.kt
// Module: :feature:chat
// Purpose: Events emitted by SessionListViewModel for HomeViewModel to handle
// Created: 2026-01-07

package com.smartsales.feature.chat.sessionlist

/**
 * Events that SessionListViewModel emits for session-level operations
 * that require coordination with HomeViewModel.
 */
sealed class SessionListEvent {
    /**
     * User selected a different session from the history list.
     * HomeViewModel should switch to this session and load its messages.
     */
    data class SwitchToSession(
        val id: String,
        val title: String,
        val isTranscription: Boolean
    ) : SessionListEvent()
}
