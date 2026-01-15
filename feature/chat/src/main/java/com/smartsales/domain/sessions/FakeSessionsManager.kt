package com.smartsales.domain.sessions

import com.smartsales.feature.chat.home.SessionListItemUi
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.home.sessions.SessionsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation for testing. Supports stubbing and call tracking.
 */
class FakeSessionsManager : SessionsManager {
    private val _uiState = MutableStateFlow(SessionsUiState())
    override val uiState: StateFlow<SessionsUiState> = _uiState
    
    private val _sessions = MutableStateFlow<List<SessionListItemUi>>(emptyList())
    override val sessionList: Flow<List<SessionListItemUi>> = _sessions

    var stubDeleteResult: SessionsManager.DeleteResult = SessionsManager.DeleteResult.Success
    var stubNewSession: AiSessionSummary = AiSessionSummary(
        id = "fake-session",
        title = "New Session",
        lastMessagePreview = "",
        updatedAtMillis = System.currentTimeMillis()
    )
    var stubRenameResult: String? = null

    val longPressCalls = mutableListOf<String>()
    var dismissCalls = 0
    var renameStartCalls = 0
    val renameTextCalls = mutableListOf<String>()
    val pinToggleCalls = mutableListOf<String>()
    val renameConfirmCalls = mutableListOf<Pair<String, String>>()
    val deleteCalls = mutableListOf<Pair<String, String>>()
    var createNewSessionCalls = 0

    override suspend fun onHistorySessionLongPress(sessionId: String) {
        longPressCalls.add(sessionId)
    }

    override fun onHistoryActionDismiss() {
        dismissCalls++
    }

    override fun onHistoryActionRenameStart() {
        renameStartCalls++
    }

    override fun onHistoryRenameTextChange(text: String) {
        renameTextCalls.add(text)
    }

    override suspend fun onHistorySessionPinToggle(sessionId: String) {
        pinToggleCalls.add(sessionId)
    }

    override suspend fun onHistorySessionRenameConfirmed(sessionId: String, newTitle: String): String? {
        renameConfirmCalls.add(sessionId to newTitle)
        return stubRenameResult
    }

    override suspend fun onHistorySessionDelete(sessionId: String, currentSessionId: String): SessionsManager.DeleteResult {
        deleteCalls.add(sessionId to currentSessionId)
        return stubDeleteResult
    }

    override suspend fun createNewSession(): AiSessionSummary {
        createNewSessionCalls++
        return stubNewSession
    }

    fun setSessions(list: List<SessionListItemUi>) {
        _sessions.value = list
    }

    fun reset() {
        longPressCalls.clear()
        dismissCalls = 0
        renameStartCalls = 0
        renameTextCalls.clear()
        pinToggleCalls.clear()
        renameConfirmCalls.clear()
        deleteCalls.clear()
        createNewSessionCalls = 0
    }
}
