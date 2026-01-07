// File: feature/chat/src/main/java/com/smartsales/domain/sessions/SessionsManager.kt
// Module: :feature:chat
// Summary: Session list management, history operations (rename/delete/pin)
// Author: created on 2026-01-05

package com.smartsales.domain.sessions

import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.Provenance
import com.smartsales.core.metahub.RenamingTarget
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.home.SessionListItemUi
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.core.metahub.setM3AcceptedName
import com.smartsales.feature.chat.home.sessions.SessionsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

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
@Singleton
class SessionsManager @Inject constructor(
    private val sessionRepository: AiSessionRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val metaHub: MetaHub
) {

    // Internal UI state (dialogs, selected items, etc.)
    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState = _uiState.asStateFlow()

    // Session list data flow - directly mapped from Repository to UI model
    // Note: SharingStarted.Eagerly removed - caller should collect in their scope
    val sessionList = sessionRepository.summaries
        .map { summaries ->
            summaries.map { summary ->
                SessionListItemUi(
                    id = summary.id,
                    title = summary.title,
                    lastMessagePreview = summary.lastMessagePreview,
                    updatedAtMillis = summary.updatedAtMillis,
                    isCurrent = false, // Handled by HomeViewModel
                    isTranscription = summary.isTranscription,
                    pinned = summary.pinned
                )
            }
        }

    suspend fun onHistorySessionLongPress(sessionId: String) {
        // Find from current list
        val target = sessionList.first().firstOrNull { it.id == sessionId } ?: return
        _uiState.update {
            it.copy(
                historyActionSession = target,
                showHistoryRenameDialog = false,
                historyRenameText = target.title
            )
        }
    }

    fun onHistoryActionDismiss() {
        _uiState.update {
            it.copy(
                historyActionSession = null,
                showHistoryRenameDialog = false,
                historyRenameText = ""
            )
        }
    }

    fun onHistoryActionRenameStart() {
        val target = _uiState.value.historyActionSession ?: return
        _uiState.update {
            it.copy(
                showHistoryRenameDialog = true,
                historyRenameText = target.title
            )
        }
    }

    fun onHistoryRenameTextChange(text: String) {
        _uiState.update { it.copy(historyRenameText = text) }
    }

    suspend fun onHistorySessionPinToggle(sessionId: String) {
        val existing = sessionRepository.findById(sessionId) ?: return
        val toggled = existing.copy(pinned = !existing.pinned)
        sessionRepository.upsert(toggled)
        // List update triggered automatically by Flow
        onHistoryActionDismiss()
    }

    suspend fun onHistorySessionRenameConfirmed(sessionId: String, newTitle: String): String? {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return null

        sessionRepository.updateTitle(sessionId, trimmed, isUserEdited = true)
        
        // Write to MetaHub
        metaHub.setM3AcceptedName(
            sessionId = sessionId,
            target = RenamingTarget.SESSION_TITLE,
            name = trimmed,
            prov = Provenance(
                source = "user_rename",
                updatedAt = System.currentTimeMillis()
            )
        )

        // Update local UI state
        _uiState.update { state ->
            state.copy(
                historyActionSession = state.historyActionSession?.takeIf { it.id == sessionId }?.copy(title = trimmed),
                showHistoryRenameDialog = false,
                historyRenameText = ""
            )
        }
        return trimmed
    }

    sealed class DeleteResult {
        object Success : DeleteResult()
        data class CurrentSessionDeleted(val nextSession: AiSessionSummary) : DeleteResult()
    }

    /**
     * Delete session. If current session deleted, return CurrentSessionDeleted to indicate caller should switch.
     */
    suspend fun onHistorySessionDelete(sessionId: String, currentSessionId: String): DeleteResult {
        runCatching { sessionRepository.delete(sessionId) }
        runCatching { chatHistoryRepository.deleteSession(sessionId) }
        
        onHistoryActionDismiss()

        if (sessionId == currentSessionId) {
            // If current session deleted, caller needs to switch to latest session or create new
            // For simplicity, create a new safe fallback
            val next = sessionRepository.createNewChatSession()
            return DeleteResult.CurrentSessionDeleted(next)
        }
        
        return DeleteResult.Success
    }

    suspend fun createNewSession(): AiSessionSummary {
        return sessionRepository.createNewChatSession()
    }
}
