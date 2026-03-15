package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeHistoryRepository : HistoryRepository {
    private val sessions = mutableListOf<SessionPreview>()
    private val messages = mutableMapOf<String, MutableList<ChatMessage>>()
    
    private val sessionsFlow = MutableStateFlow<List<SessionPreview>>(emptyList())

    override fun getGroupedSessions(): Map<String, List<SessionPreview>> {
        return sessions.groupBy { it.summary.takeUnless { s -> s.isBlank() } ?: "未分类" }
    }

    override fun getGroupedSessionsFlow(): Flow<Map<String, List<SessionPreview>>> {
        return sessionsFlow.map { list -> list.groupBy { it.summary.takeUnless { s -> s.isBlank() } ?: "未分类" } }
    }

    override fun getSessions(): List<SessionPreview> = sessions.toList()

    override fun togglePin(sessionId: String) {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index != -1) {
            val session = sessions[index]
            sessions[index] = session.copy(isPinned = !session.isPinned)
            sessionsFlow.value = sessions.toList()
        }
    }

    override fun renameSession(sessionId: String, newClientName: String, newSummary: String) {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index != -1) {
            sessions[index] = sessions[index].copy(clientName = newClientName, summary = newSummary)
            sessionsFlow.value = sessions.toList()
        }
    }

    override fun deleteSession(sessionId: String) {
        sessions.removeAll { it.id == sessionId }
        messages.remove(sessionId)
        sessionsFlow.value = sessions.toList()
    }

    override fun getSession(sessionId: String): SessionPreview? {
        return sessions.find { it.id == sessionId }
    }

    override fun createSession(clientName: String, summary: String, linkedAudioId: String?): String {
        val id = "session-${sessions.size + 1}"
        sessions.add(SessionPreview(id, clientName, summary, System.currentTimeMillis(), false, linkedAudioId))
        messages[id] = mutableListOf()
        sessionsFlow.value = sessions.toList()
        return id
    }

    override fun saveMessage(sessionId: String, isUser: Boolean, content: String, orderIndex: Int) {
        val list = messages.getOrPut(sessionId) { mutableListOf() }
        val msg = if (isUser) ChatMessage.User("msg-${list.size}", System.currentTimeMillis(), content)
        else ChatMessage.Ai("msg-${list.size}", System.currentTimeMillis(), com.smartsales.prism.domain.model.UiState.Response(content))
        list.add(msg)
    }

    override fun getMessages(sessionId: String): List<ChatMessage> {
        return messages[sessionId]?.toList() ?: emptyList()
    }

    override fun clearMessages(sessionId: String) {
        messages[sessionId]?.clear()
    }
}
