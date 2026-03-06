package com.smartsales.prism.data.persistence

import android.util.Log
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 实现的历史仓库
 *
 * 分组算法:
 * 1. 📌 置顶
 * 2. 📅 今天
 * 3. 🗓️ 最近30天
 * 4. 📁 YYYYMM (按月归档)
 */
@Singleton
class RoomHistoryRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao
) : HistoryRepository {

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyyMM")

    override fun getGroupedSessions(): Map<String, List<SessionPreview>> {
        return groupSessions(sessionDao.getAll().map { it.toDomain() })
    }

    override fun getGroupedSessionsFlow(): Flow<Map<String, List<SessionPreview>>> {
        return sessionDao.getAllFlow().map { entities ->
            groupSessions(entities.map { it.toDomain() })
        }
    }

    /** 分组算法 — 同步和响应式共用 */
    private fun groupSessions(all: List<SessionPreview>): Map<String, List<SessionPreview>> {
        val result = linkedMapOf<String, MutableList<SessionPreview>>()
        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)

        for (session in all) {
            val sessionDate = Instant.ofEpochMilli(session.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val group = when {
                session.isPinned -> "📌 置顶"
                sessionDate == today -> "📅 今天"
                sessionDate.isAfter(thirtyDaysAgo) -> "🗓️ 最近30天"
                else -> "📁 ${sessionDate.format(monthFormatter)}"
            }

            result.getOrPut(group) { mutableListOf() }.add(session)
        }

        return result
    }

    override fun getSessions(): List<SessionPreview> {
        return sessionDao.getAll().map { it.toDomain() }
    }

    override fun getSession(sessionId: String): SessionPreview? {
        return sessionDao.getById(sessionId)?.toDomain()
    }

    override fun createSession(
        clientName: String,
        summary: String,
        linkedAudioId: String?
    ): String {
        val id = UUID.randomUUID().toString()
        val entity = SessionEntity(
            sessionId = id,
            clientName = clientName,
            summary = summary,
            timestamp = System.currentTimeMillis(),
            isPinned = false,
            linkedAudioId = linkedAudioId
        )
        sessionDao.insert(entity)
        return id
    }

    override fun togglePin(sessionId: String) {
        val entity = sessionDao.getById(sessionId) ?: return
        sessionDao.update(entity.copy(isPinned = !entity.isPinned))
    }

    override fun renameSession(sessionId: String, newClientName: String, newSummary: String) {
        val entity = sessionDao.getById(sessionId) ?: return
        sessionDao.update(entity.copy(clientName = newClientName, summary = newSummary))
    }

    override fun deleteSession(sessionId: String) {
        sessionDao.delete(sessionId)
    }

    // === 消息持久化 ===

    override fun saveMessage(sessionId: String, isUser: Boolean, content: String, orderIndex: Int) {
        messageDao.insert(MessageEntity(
            sessionId = sessionId,
            isUser = isUser,
            content = content,
            timestamp = System.currentTimeMillis(),
            orderIndex = orderIndex
        ))
    }

    override fun getMessages(sessionId: String): List<ChatMessage> {
        val entities = messageDao.getBySession(sessionId)
        Log.d("RoomHistoryRepository", "getMessages: sessionId=$sessionId, count=${entities.size}")
        return entities.map { entity ->
            if (entity.isUser) {
                ChatMessage.User(
                    id = entity.id.toString(),
                    timestamp = entity.timestamp,
                    content = entity.content
                )
            } else {
                ChatMessage.Ai(
                    id = entity.id.toString(),
                    timestamp = entity.timestamp,
                    uiState = UiState.Response(entity.content)
                )
            }
        }
    }

    override fun clearMessages(sessionId: String) {
        messageDao.deleteBySession(sessionId)
    }
}
