package com.smartsales.feature.chat.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.LogTags
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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
    val groups: List<ChatHistoryGroupUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedSessionId: String? = null,
    val searchQuery: String = ""
)

data class ChatHistoryGroupUi(
    val label: String,
    val items: List<ChatSessionUi>
)

sealed class ChatHistoryEvent {
    data class NavigateToSession(val sessionId: String) : ChatHistoryEvent()
}

@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
    private val aiSessionRepository: AiSessionRepository,
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel() {

    private var nowProvider: () -> Long = { System.currentTimeMillis() }
    private val _uiState = MutableStateFlow(ChatHistoryUiState(isLoading = true))
    val uiState: StateFlow<ChatHistoryUiState> = _uiState.asStateFlow()
    private var baseGroups: List<ChatHistoryGroupUi> = emptyList()

    private val _events = MutableSharedFlow<ChatHistoryEvent>()
    val events: SharedFlow<ChatHistoryEvent> = _events.asSharedFlow()
    private var observing = false

    init {
        observeSessions()
    }

    fun onSearchQueryChanged(query: String) {
        val normalized = query
        _uiState.update {
            it.copy(
                searchQuery = normalized,
                groups = applySearch(normalized, baseGroups)
            )
        }
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
            val updated = session.copy(title = newTitle)
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
            val updated = session.copy(pinned = !session.pinned)
            safeUpsert(updated)
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    @VisibleForTesting
    fun overrideNowProvider(provider: () -> Long) {
        nowProvider = provider
        viewModelScope.launch { refreshFromRepo() }
    }

    private fun observeSessions() {
        if (observing) return
        observing = true
        viewModelScope.launch {
            aiSessionRepository.summaries
                .catch { error ->
                    Log.w(TAG, "加载会话列表失败", error)
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
                .collectLatest { summaries ->
                    applySessions(summaries, nowProvider.invoke())
                }
        }
    }

    private fun applySessions(summaries: List<AiSessionSummary>, nowMillis: Long) {
        val sessions = summaries.toUiModels()
        val grouped = sessions.groupByBucket(nowMillis).map { (label, items) ->
            ChatHistoryGroupUi(
                label = label,
                items = items
                    .sortedWith(
                        compareByDescending<ChatSessionUi> { it.pinned }
                            .thenByDescending { it.updatedAt }
                    )
            )
        }.sortedBy { bucketOrder(it.label) }
        baseGroups = grouped
        _uiState.update {
            it.copy(
                groups = applySearch(it.searchQuery, baseGroups),
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun bucketOrder(label: String): Int = when (label) {
        "7天内" -> 0
        "30天内" -> 1
        "更早" -> 2
        else -> 3
    }

    private fun List<ChatSessionUi>.groupByBucket(nowMillis: Long): Map<String, List<ChatSessionUi>> {
        val sevenDays = 7L * 24 * 60 * 60 * 1000
        val thirtyDays = 30L * 24 * 60 * 60 * 1000
        return this.groupBy { session ->
            val diff = (nowMillis - session.updatedAt).coerceAtLeast(0)
            when {
                diff <= sevenDays -> "7天内"
                diff <= thirtyDays -> "30天内"
                else -> "更早"
            }
        }.filterValues { it.isNotEmpty() }
    }

    private suspend fun safeUpsert(summary: AiSessionSummary) {
        runCatching { aiSessionRepository.upsert(summary) }
            .onSuccess { refreshFromRepo() }
            .onFailure { error ->
                Log.w(TAG, "更新会话失败", error)
                _uiState.update { it.copy(errorMessage = error.message ?: "操作失败") }
            }
    }

    private suspend fun refreshFromRepo() {
        runCatching { aiSessionRepository.summaries.first() }
            .onSuccess { applySessions(it, nowProvider.invoke()) }
            .onFailure { error ->
                Log.w(TAG, "刷新会话列表失败", error)
                _uiState.update { it.copy(errorMessage = error.message ?: "刷新失败") }
            }
    }

    private suspend fun findSession(sessionId: String): AiSessionSummary? {
        val cached = baseGroups.flatMap { it.items }.firstOrNull { it.id == sessionId }
        if (cached != null) return cached.toSummary()
        return aiSessionRepository.findById(sessionId)
    }

    private fun applySearch(query: String, groups: List<ChatHistoryGroupUi>): List<ChatHistoryGroupUi> {
        if (query.isBlank()) return groups
        val lowered = query.lowercase()
        return groups.mapNotNull { group ->
            val filtered = group.items.filter { session ->
                session.title.lowercase().contains(lowered) ||
                    session.lastMessagePreview.orEmpty().lowercase().contains(lowered)
            }
            if (filtered.isEmpty()) null else group.copy(items = filtered)
        }
    }

    private fun List<AiSessionSummary>.toUiModels(): List<ChatSessionUi> =
        sortedByDescending { it.updatedAtMillis }.map {
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
