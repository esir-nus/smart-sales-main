// File: feature/chat/src/main/java/com/smartsales/domain/sessions/SessionsManager.kt
// Module: :feature:chat
// Summary: Session list management interface - history operations
// Author: created on 2026-01-05

package com.smartsales.domain.sessions

import com.smartsales.feature.chat.home.SessionListItemUi
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.home.sessions.SessionsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sessions Manager: handles session list, history operations (rename/delete/pin).
 *
 * Responsibilities:
 * - Session list data flow
 * - Rename/delete/pin operations
 * - MetaHub integration for session metadata
 *
 * Design:
 * - Stateful coordinator for session management
 * - Exposes StateFlow for session list and UI state
 */
interface SessionsManager {

    val uiState: StateFlow<SessionsUiState>
    val sessionList: Flow<List<SessionListItemUi>>

    suspend fun onHistorySessionLongPress(sessionId: String)
    
    fun onHistoryActionDismiss()
    
    fun onHistoryActionRenameStart()
    
    fun onHistoryRenameTextChange(text: String)
    
    suspend fun onHistorySessionPinToggle(sessionId: String)
    
    suspend fun onHistorySessionRenameConfirmed(sessionId: String, newTitle: String): String?

    /**
     * Delete session. If current session deleted, return CurrentSessionDeleted to indicate caller should switch.
     */
    suspend fun onHistorySessionDelete(sessionId: String, currentSessionId: String): DeleteResult
    
    suspend fun createNewSession(): AiSessionSummary

    sealed class DeleteResult {
        object Success : DeleteResult()
        data class CurrentSessionDeleted(val nextSession: AiSessionSummary) : DeleteResult()
    }
}
