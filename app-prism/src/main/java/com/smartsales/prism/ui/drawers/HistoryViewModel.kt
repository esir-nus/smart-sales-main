package com.smartsales.prism.ui.drawers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 历史抽屉 ViewModel
 *
 * 响应式架构: 通过 Room Flow 自动观察数据库变更
 * 任何模块写入 sessions 表后，UI 自动刷新（无需手动 reload）
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    /** 响应式分组会话 — Room 表变更时自动更新 */
    val groupedSessions: StateFlow<Map<String, List<SessionPreview>>> =
        historyRepository.getGroupedSessionsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun togglePin(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.togglePin(sessionId)
        }
    }

    fun renameSession(sessionId: String, newClientName: String, newSummary: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.renameSession(sessionId, newClientName, newSummary)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.deleteSession(sessionId)
        }
    }
}
