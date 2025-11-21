package com.smartsales.feature.chat.persistence

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/persistence/RoomAiSessionRepository.kt
// 模块：:feature:chat
// 说明：使用 Room 落地聊天会话摘要
// 作者：创建于 2025-11-16
class RoomAiSessionRepository(
    private val dao: AiSessionDao,
    private val dispatchers: DispatcherProvider
) : AiSessionRepository {
    override val summaries: Flow<List<AiSessionSummary>> =
        dao.observeSummaries()
            .map { entities ->
                entities
                    .map { it.toSummary() }
                    .sortedWith(
                        compareByDescending<AiSessionSummary> { it.pinned }
                            .thenByDescending { it.updatedAtMillis }
                    )
            }

    override suspend fun upsert(summary: AiSessionSummary) {
        withContext(dispatchers.io) {
            dao.upsert(summary.toEntity())
        }
    }

    override suspend fun delete(id: String) {
        withContext(dispatchers.io) {
            dao.deleteById(id)
        }
    }

    override suspend fun findById(id: String): AiSessionSummary? = withContext(dispatchers.io) {
        dao.findById(id)?.toSummary()
    }

    private fun AiSessionEntity.toSummary(): AiSessionSummary = AiSessionSummary(
        id = id,
        title = title,
        lastMessagePreview = preview,
        updatedAtMillis = updatedAtMillis,
        pinned = pinned
    )

    private fun AiSessionSummary.toEntity(): AiSessionEntity = AiSessionEntity(
        id = id,
        title = title,
        preview = lastMessagePreview,
        updatedAtMillis = updatedAtMillis,
        pinned = pinned
    )
}
