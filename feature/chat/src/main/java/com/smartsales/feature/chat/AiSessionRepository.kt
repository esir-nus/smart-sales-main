package com.smartsales.feature.chat

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// 文件路径: feature/chat/src/main/java/com/smartsales/feature/chat/AiSessionRepository.kt
// 文件作用: 维护聊天会话摘要供抽屉使用
// 最近修改: 2025-11-14
data class AiSessionSummary(
    val id: String,
    val title: String,
    val lastMessagePreview: String,
    val updatedAtMillis: Long
)

interface AiSessionRepository {
    val summaries: Flow<List<AiSessionSummary>>
    suspend fun upsert(summary: AiSessionSummary)
}

@Singleton
class InMemoryAiSessionRepository @Inject constructor() : AiSessionRepository {
    private val mutex = Mutex()
    private val internal = MutableStateFlow<List<AiSessionSummary>>(emptyList())
    override val summaries: Flow<List<AiSessionSummary>> = internal.asStateFlow()

    override suspend fun upsert(summary: AiSessionSummary) {
        mutex.withLock {
            internal.update { existing ->
                val withoutCurrent = existing.filterNot { it.id == summary.id }
                (withoutCurrent + summary).sortedByDescending { it.updatedAtMillis }
            }
        }
    }
}
