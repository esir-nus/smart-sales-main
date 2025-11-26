package com.smartsales.feature.chat.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.LogTags
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryViewModel.kt
// 模块：:feature:chat
// 说明：驱动会话历史列表、重命名、删除与跳转事件
// 作者：创建于 2025-11-21

data class ChatSessionUi(
    val id: String,
    val title: String,
    val lastMessagePreview: String?,
    val updatedAt: Long,
    val pinned: Boolean
)

data class ChatHistoryUiState(
    val sessions: List<ChatSessionUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedSessionId: String? = null
)

sealed class ChatHistoryEvent {
    data class NavigateToSession(val sessionId: String) : ChatHistoryEvent()
}

@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
    private val aiSessionRepository: AiSessionRepository,
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatHistoryUiState(isLoading = true))
    val uiState: StateFlow<ChatHistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatHistoryEvent>()
    val events: SharedFlow<ChatHistoryEvent> = _events.asSharedFlow()
    private var observing = false

    init {
        observeSessions()
    }

    fun loadSessions() {
        observeSessions()
    }

    fun onSessionClicked(sessionId: String) {
        _uiState.update { it.copy(selectedSessionId = sessionId) }
        viewModelScope.launch {
            _events.emit(ChatHistoryEvent.NavigateToSession(sessionId))
        }
    }

    fun onRenameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            val session = findSession(sessionId)
            if (session == null) {
                _uiState.update { it.copy(errorMessage = "会话不存在") }
                return@launch
            }
            val updated = session.copy(
                title = newTitle,
                updatedAtMillis = System.currentTimeMillis()
            )
            safeUpsert(updated)
        }
    }

    fun onDeleteSession(sessionId: String) {
        viewModelScope.launch {
            runCatching {
                aiSessionRepository.delete(sessionId)
                chatHistoryRepository.deleteSession(sessionId)
            }.onFailure { error ->
                Log.w(TAG, "删除会话失败", error)
                _uiState.update { it.copy(errorMessage = error.message ?: "删除失败") }
            }
        }
    }

    fun onPinToggle(sessionId: String) {
        viewModelScope.launch {
            val session = findSession(sessionId)
            if (session == null) {
                _uiState.update { it.copy(errorMessage = "会话不存在") }
                return@launch
            }
            val updated = session.copy(
                pinned = !session.pinned,
                updatedAtMillis = System.currentTimeMillis()
            )
            safeUpsert(updated)
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeSessions() {
        if (observing) return
        observing = true
        viewModelScope.launch {
            val summariesFlow = runCatching { aiSessionRepository.summaries }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
                .getOrNull() ?: return@launch
            summariesFlow
                .catch { error ->
                    Log.w(TAG, "加载会话列表失败", error)
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
                .collectLatest { summaries ->
                    _uiState.update {
                        it.copy(
                            sessions = summaries.toUiModels(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    private suspend fun safeUpsert(summary: AiSessionSummary) {
        runCatching { aiSessionRepository.upsert(summary) }
            .onFailure { error ->
                Log.w(TAG, "更新会话失败", error)
                _uiState.update { it.copy(errorMessage = error.message ?: "操作失败") }
            }
    }

    private suspend fun findSession(sessionId: String): AiSessionSummary? {
        val cached = _uiState.value.sessions.firstOrNull { it.id == sessionId }
        if (cached != null) return cached.toSummary()
        return aiSessionRepository.findById(sessionId)
    }

    private fun List<AiSessionSummary>.toUiModels(): List<ChatSessionUi> =
        sortedWith(
            compareByDescending<AiSessionSummary> { it.pinned }
                .thenByDescending { it.updatedAtMillis }
        ).map {
            ChatSessionUi(
                id = it.id,
                title = it.title,
                lastMessagePreview = it.lastMessagePreview,
                updatedAt = it.updatedAtMillis,
                pinned = it.pinned
            )
        }

    private fun ChatSessionUi.toSummary(): AiSessionSummary = AiSessionSummary(
        id = id,
        title = title,
        lastMessagePreview = lastMessagePreview ?: "",
        updatedAtMillis = updatedAt,
        pinned = pinned
    )

    companion object {
        private const val TAG = "${LogTags.CHAT}/HistoryVM"
    }
}
