package com.smartsales.feature.chat.core

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/core/DefaultAiChatService.kt
// 模块：:feature:chat
// 说明：适配数据层 AiChatService 的默认实现，将特性层请求映射到真实流式服务
// 作者：创建于 2025-12-10

import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService as DataAiChatService
import com.smartsales.data.aicore.AiChatStreamEvent as DataAiChatStreamEvent
import com.smartsales.feature.chat.home.HomeScreenBindings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultAiChatService @Inject constructor(
    private val dataAiChatService: DataAiChatService
) : AiChatService {

    override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
        val prompt = HomeScreenBindings.buildPromptWithHistory(request)
        val aiRequest = AiChatRequest(
            prompt = prompt,
            skillTags = request.quickSkillId?.let { setOf(it) } ?: emptySet()
        )
        return dataAiChatService.streamMessage(aiRequest).map { event ->
            when (event) {
                is DataAiChatStreamEvent.Chunk -> ChatStreamEvent.Delta(event.content)
                is DataAiChatStreamEvent.Completed -> {
                    val fullText = event.response.structuredMarkdown
                        ?.takeIf { it.isNotBlank() }
                        ?: event.response.displayText
                    ChatStreamEvent.Completed(fullText)
                }
                is DataAiChatStreamEvent.Error -> ChatStreamEvent.Error(event.throwable)
            }
        }
    }
}
