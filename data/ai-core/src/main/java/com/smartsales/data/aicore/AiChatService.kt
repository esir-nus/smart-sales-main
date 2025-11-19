package com.smartsales.data.aicore

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

// 文件路径: data/ai-core/src/main/java/com/smartsales/data/aicore/AiChatService.kt
// 文件作用: 定义AI聊天服务接口及默认假实现
// 文件作者: Codex
// 最近修改: 2025-02-14
data class AiAttachment(
    val name: String,
    val mimeType: String,
    val uri: String
)

data class AiChatRequest(
    val prompt: String,
    val skillTags: Set<String> = emptySet(),
    val transcriptMarkdown: String? = null,
    val attachments: List<AiAttachment> = emptyList()
)

data class AiChatResponse(
    val displayText: String,
    val structuredMarkdown: String?,
    val references: List<String> = emptyList()
)

sealed class AiChatStreamEvent {
    data class Chunk(val content: String) : AiChatStreamEvent()
    data class Completed(val response: AiChatResponse) : AiChatStreamEvent()
    data class Error(val throwable: Throwable) : AiChatStreamEvent()
}

interface AiChatService {
    suspend fun sendMessage(request: AiChatRequest): Result<AiChatResponse>

    fun streamMessage(request: AiChatRequest): Flow<AiChatStreamEvent> = flow {
        when (val result = sendMessage(request)) {
            is Result.Success -> emit(AiChatStreamEvent.Completed(result.data))
            is Result.Error -> emit(AiChatStreamEvent.Error(result.throwable))
        }
    }
}

@Singleton
class FakeAiChatService @Inject constructor(
    private val dispatchers: DispatcherProvider
) : AiChatService {
    override suspend fun sendMessage(request: AiChatRequest): Result<AiChatResponse> =
        withContext(dispatchers.io) {
            delay(120)
            val content = buildString {
                appendLine("Echo: ${request.prompt}")
                if (request.skillTags.isNotEmpty()) {
                    appendLine("技能: ${request.skillTags.joinToString()}")
                }
                if (!request.transcriptMarkdown.isNullOrBlank()) {
                    appendLine("含有转写摘要。")
                }
                if (request.attachments.isNotEmpty()) {
                    appendLine("附件: ${request.attachments.joinToString { it.name }}")
                }
            }.trim()
            val structured = request.transcriptMarkdown ?: """
                $content

                ## 分析结果
                - 输入: ${request.prompt}
                - 技能: ${request.skillTags.joinToString().ifEmpty { "默认" }}
                - 附件数: ${request.attachments.size}
                - 推荐下一步: 跟进客户并导出 PDF/CSV。
            """.trimIndent()
            Result.Success(
                AiChatResponse(
                    displayText = content,
                    structuredMarkdown = structured,
                    references = List(request.attachments.size) { index ->
                        "${request.attachments[index].name}#${Random.nextInt(100)}"
                    }
                )
            )
        }

    override fun streamMessage(request: AiChatRequest): Flow<AiChatStreamEvent> = flow {
        when (val result = sendMessage(request)) {
            is Result.Success -> {
                val chunks = result.data.displayText.chunked(24).ifEmpty { listOf(result.data.displayText) }
                chunks.forEach { emit(AiChatStreamEvent.Chunk(it)) }
                emit(AiChatStreamEvent.Completed(result.data))
            }
            is Result.Error -> emit(AiChatStreamEvent.Error(result.throwable))
        }
    }
}
