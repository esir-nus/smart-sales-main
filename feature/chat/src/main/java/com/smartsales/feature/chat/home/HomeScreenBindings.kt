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
                    val delta = when {
                        // 正常情况：新内容以旧内容开头，提取增量
                        content.startsWith(lastContent) -> {
                            content.substring(lastContent.length)
                        }
                        // 如果新内容比旧内容短，可能是重置，使用全部内容
                        content.length < lastContent.length -> {
                            content
                        }
                        // 如果新内容不以旧内容开头，可能是 LLM 返回了累积内容，尝试提取增量
                        lastContent.isNotEmpty() && content.length > lastContent.length -> {
                            // 尝试找到 lastContent 在新内容中的位置
                            val index = content.indexOf(lastContent)
                            if (index >= 0) {
                                content.substring(index + lastContent.length)
                            } else {
                                // 如果找不到，使用全部内容（可能是全新的内容）
                                content
                            }
                        }
                        // 默认情况：使用全部内容
                        else -> content
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
        val builder = StringBuilder()
        if (request.history.isNotEmpty()) {
            val historyText = request.history.joinToString(separator = "\n") { item ->
                val role = when (item.role) {
                    ChatRole.USER -> "用户"
                    ChatRole.ASSISTANT -> "助手"
                }
                "$role：${item.content}"
            }
            builder.appendLine("历史对话：")
            builder.appendLine(historyText)
            builder.appendLine()
        }
        builder.append("最新问题：${request.userMessage}")
        // SMART_ANALYSIS 使用专门的 prompt，普通聊天使用不同的格式
        if (request.quickSkillId == null) {
            builder.appendLine()
            builder.appendLine()
            builder.appendLine("请按下面格式回复（总长不超过 200 字）：")
            builder.appendLine("1) 用 1-2 句话复述用户问题；如信息不足，直接说明需要更多细节")
            builder.appendLine("2) 用 2-3 条要点给出核心洞察")
            builder.appendLine("3) 用 2-4 条可执行建议")
            builder.appendLine("4) 如需深度拆解，可提醒使用「智能分析」获取完整版并可导出 PDF/CSV")
            builder.appendLine()
            builder.append("重要：不要重复前面的内容，每个编号只写一次，不要累积重复。")
        }
        return builder.toString()
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
