package com.smartsales.data.prismlib.pipeline

import com.smartsales.domain.prism.core.EnhancedContext
import com.smartsales.domain.prism.core.ExecutorResult
import com.smartsales.domain.prism.core.MemoryWriter
import com.smartsales.domain.prism.core.entities.MemoryEntryEntity
import com.smartsales.domain.prism.core.repositories.MemoryEntryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Room 内存写入器 — Fire-and-Forget 后台持久化
 * @see Prism-V1.md §2.2 #5
 */
@Singleton
class RoomMemoryWriter @Inject constructor(
    private val memoryEntryRepository: MemoryEntryRepository
) : MemoryWriter {

    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun persist(context: EnhancedContext, result: ExecutorResult) {
        // Fire-and-forget: 启动后台协程，不阻塞调用方
        scope.launch {
            try {
                val entry = MemoryEntryEntity(
                    id = UUID.randomUUID().toString(),
                    workflow = context.mode,
                    title = context.userText.take(50),
                    sessionId = "", // TODO: 从当前 Session 获取
                    displayContent = result.displayContent,
                    structuredJson = result.structuredJson,
                    payloadJson = buildPayloadJson(context, result),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isArchived = false
                )
                memoryEntryRepository.insert(entry)
            } catch (e: Exception) {
                // 静默失败 — Fire-and-Forget 不应影响主流程
                // TODO: Log to analytics
            }
        }
    }

    private fun buildPayloadJson(context: EnhancedContext, result: ExecutorResult): String {
        // 简化实现：构建 JSON 字符串
        return """
            {
                "userText": "${context.userText.replace("\"", "\\\"")}",
                "mode": "${context.mode.name}"
            }
        """.trimIndent()
    }
}
