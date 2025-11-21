package com.smartsales.feature.chat

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.feature.chat.persistence.AiSessionDao
import com.smartsales.feature.chat.persistence.AiSessionEntity
import com.smartsales.feature.chat.persistence.RoomAiSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/RoomAiSessionRepositoryTest.kt
// 模块：:feature:chat
// 说明：验证 Room 仓库的排序与映射逻辑
// 作者：创建于 2025-11-16
@OptIn(ExperimentalCoroutinesApi::class)
class RoomAiSessionRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = InMemoryDao()
    private val repository = RoomAiSessionRepository(
        dao = dao,
        dispatchers = FakeDispatcherProvider(dispatcher)
    )

    @Test
    fun upsert_persistsAndSortsSummaries() = runTest(dispatcher) {
        repository.upsert(
            AiSessionSummary(
                id = "s1",
                title = "第一条",
                lastMessagePreview = "预览1",
                updatedAtMillis = 1000L
            )
        )
        repository.upsert(
            AiSessionSummary(
                id = "s2",
                title = "第二条",
                lastMessagePreview = "预览2",
                updatedAtMillis = 2000L
            )
        )

        val summaries = repository.summaries.first()
        assertEquals(2, summaries.size)
        assertEquals("s2", summaries.first().id)
    }

    private class InMemoryDao : AiSessionDao {
        private val flow = MutableStateFlow<List<AiSessionEntity>>(emptyList())

        override fun observeSummaries(limit: Int): MutableStateFlow<List<AiSessionEntity>> = flow

        override suspend fun upsert(entity: AiSessionEntity) {
            flow.value = flow.value.filterNot { it.id == entity.id } + entity
        }

        override suspend fun deleteById(sessionId: String) {
            flow.value = flow.value.filterNot { it.id == sessionId }
        }

        override suspend fun findById(sessionId: String): AiSessionEntity? =
            flow.value.firstOrNull { it.id == sessionId }
    }
}
