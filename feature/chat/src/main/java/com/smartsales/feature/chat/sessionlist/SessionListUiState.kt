// File: feature/chat/src/main/java/com/smartsales/feature/chat/sessionlist/SessionListUiState.kt
// Module: :feature:chat
// Purpose: UI state for session list (history drawer)
// Created: 2026-01-07

package com.smartsales.feature.chat.sessionlist

import com.smartsales.feature.chat.home.SessionListItemUi

/**
 * UI state for the session list (history drawer).
 * Manages session list display and action sheet state.
 */
data class SessionListUiState(
    val sessions: List<SessionListItemUi> = emptyList(),
    val actionSession: SessionListItemUi? = null,
    val showRenameDialog: Boolean = false,
    val renameText: String = ""
)
