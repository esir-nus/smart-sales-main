// File: feature/chat/src/main/java/com/smartsales/feature/chat/sessionlist/SessionListViewModel.kt
// Module: :feature:chat
// Purpose: ViewModel for session list management (history drawer)
// Created: 2026-01-07

package com.smartsales.feature.chat.sessionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.domain.sessions.SessionsManager
import com.smartsales.feature.chat.home.SessionListItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for session list (history drawer).
 * 
 * Responsibilities:
 * - Observe session list from SessionsManager
 * - Handle UI actions (longPress, pin, rename, delete)
 * - Emit events for session switches that HomeViewModel handles
 * 
 * Does NOT handle:
 * - Current session ID (HomeViewModel owns this)
 * - Session content loading (HomeViewModel handles)
 * - Message list (HomeViewModel handles)
 */
@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val sessionsManager: SessionsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionListUiState())
    val uiState: StateFlow<SessionListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SessionListEvent>()
    val events = _events.asSharedFlow()

    init {
        observeSessionList()
        observeManagerUiState()
    }

    // ========== Observation ==========

    private fun observeSessionList() {
        viewModelScope.launch {
            sessionsManager.sessionList.collectLatest { list ->
                _uiState.update { it.copy(sessions = list ?: emptyList()) }
            }
        }
    }

    private fun observeManagerUiState() {
        viewModelScope.launch {
            sessionsManager.uiState.collectLatest { managerState ->
                _uiState.update {
                    it.copy(
                        actionSession = managerState.historyActionSession,
                        showRenameDialog = managerState.showHistoryRenameDialog,
                        renameText = managerState.historyRenameText
                    )
                }
            }
        }
    }

    // ========== Public Actions ==========

    fun onSessionSelected(sessionId: String) {
        val session = _uiState.value.sessions.firstOrNull { it.id == sessionId } ?: return
        viewModelScope.launch {
            _events.emit(
                SessionListEvent.SwitchToSession(
                    id = session.id,
                    title = session.title,
                    isTranscription = session.isTranscription
                )
            )
        }
    }

    fun onSessionLongPress(sessionId: String) {
        viewModelScope.launch {
            sessionsManager.onHistorySessionLongPress(sessionId)
        }
    }

    fun onActionDismiss() {
        sessionsManager.onHistoryActionDismiss()
    }

    fun onActionRenameStart() {
        sessionsManager.onHistoryActionRenameStart()
    }

    fun onRenameTextChange(text: String) {
        sessionsManager.onHistoryRenameTextChange(text)
    }

    fun onPinToggle(sessionId: String) {
        viewModelScope.launch {
            sessionsManager.onHistorySessionPinToggle(sessionId)
        }
    }

    fun onRenameConfirmed(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            val updatedTitle = sessionsManager.onHistorySessionRenameConfirmed(sessionId, newTitle)
            // Emit event so HomeViewModel can sync currentSession.title if needed
            if (updatedTitle != null) {
                _events.emit(SessionListEvent.TitleRenamed(sessionId, updatedTitle))
            }
        }
    }

    fun onDelete(sessionId: String, currentSessionId: String) {
        viewModelScope.launch {
            val result = sessionsManager.onHistorySessionDelete(sessionId, currentSessionId)
            // If current session was deleted, emit switch event
            if (result is com.smartsales.domain.sessions.SessionsManager.DeleteResult.CurrentSessionDeleted) {
                val next = result.nextSession
                _events.emit(
                    SessionListEvent.SwitchToSession(
                        id = next.id,
                        title = next.title,
                        isTranscription = next.isTranscription
                    )
                )
            }
        }
    }

    /**
     * Update the current session marker in the list.
     * Called by HomeViewModel when the current session changes.
     */
    fun setCurrentSessionId(sessionId: String) {
        _uiState.update { state ->
            state.copy(
                sessions = state.sessions.map { item ->
                    item.copy(isCurrent = item.id == sessionId)
                }
            )
        }
    }
}
