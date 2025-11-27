package com.smartsales.feature.chat.home

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenBindings.kt
// 模块：:feature:chat
// 说明：HomeScreen 所需依赖的 Hilt 绑定与默认实现
// 作者：创建于 2025-11-21

import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService as DataAiChatService
import com.smartsales.data.aicore.AiChatStreamEvent
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.AudioContextSummary
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.DefaultQuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillCatalog
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/** 简单仓库实现：暂不持久化历史记录。 */
@Singleton
class InMemoryHomeSessionRepository @Inject constructor() : AiSessionRepository {
    override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> = emptyList()
}

/** 将 data 层的聊天服务转换为 Home 所需的 streaming 接口。 */
@Singleton
class DelegatingHomeAiChatService @Inject constructor(
    private val delegate: DataAiChatService
) : AiChatService {
    override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flow {
        val dataRequest = AiChatRequest(
            prompt = buildPromptWithHistory(request),
            skillTags = request.quickSkillId?.let { setOf(it) } ?: emptySet(),
            transcriptMarkdown = request.audioContextSummary?.toMarkdown()
        )
        var lastContent = ""
        delegate.streamMessage(dataRequest).collect { event ->
            when (event) {
                is AiChatStreamEvent.Chunk -> {
                    val content = event.content
                    val delta = if (content.startsWith(lastContent)) {
                        content.substring(lastContent.length)
                    } else {
                        content
                    }
                    if (delta.isNotEmpty()) {
                        emit(ChatStreamEvent.Delta(delta))
                    }
                    lastContent = content
                }
                is AiChatStreamEvent.Completed -> {
                    lastContent = ""
                    emit(ChatStreamEvent.Completed(event.response.displayText))
                }
                is AiChatStreamEvent.Error -> {
                    lastContent = ""
                    emit(ChatStreamEvent.Error(event.throwable))
                }
            }
        }
    }

    private fun buildPromptWithHistory(request: ChatRequest): String {
        if (request.history.isEmpty()) return request.userMessage
        val historyText = request.history.joinToString(separator = "\n") { item ->
            val role = when (item.role) {
                ChatRole.USER -> "用户"
                ChatRole.ASSISTANT -> "助手"
            }
            "$role：${item.content}"
        }
        return buildString {
            appendLine("历史对话：")
            appendLine(historyText)
            appendLine()
            append("最新问题：${request.userMessage}")
        }
    }

    private fun AudioContextSummary.toMarkdown(): String = buildString {
        appendLine("### 音频上下文")
        appendLine("- 已准备片段：$readyClipCount 条")
        appendLine("- 待上传片段：$pendingClipCount 条")
        appendLine("- 含转写：${if (hasTranscripts) "是" else "否"}")
        note?.takeIf { it.isNotBlank() }?.let {
            appendLine("- 备注：$it")
        }
    }.trim()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeScreenBindingsModule {
    @Binds
    @Singleton
    abstract fun bindHomeAiChatService(impl: DelegatingHomeAiChatService): AiChatService

    @Binds
    @Singleton
    abstract fun bindHomeSessionRepository(impl: InMemoryHomeSessionRepository): AiSessionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object HomeScreenProvidesModule {
    @Provides
    @Singleton
    fun provideQuickSkillCatalog(): QuickSkillCatalog = DefaultQuickSkillCatalog()
}
