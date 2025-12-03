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
                    val delta = extractDelta(content, lastContent)
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

    /**
     * 使用最长公共前缀（LCP）算法提取增量内容
     * 更健壮地处理 DashScope 返回的累积内容
     */
    private fun extractDelta(newContent: String, lastContent: String): String {
        if (lastContent.isEmpty()) {
            return newContent
        }
        
        // 情况1：新内容以旧内容开头（最常见的情况）
        if (newContent.startsWith(lastContent)) {
            return newContent.substring(lastContent.length)
        }
        
        // 情况2：新内容比旧内容短，可能是重置
        if (newContent.length < lastContent.length) {
            return newContent
        }
        
        // 情况3：使用最长公共前缀算法找到匹配的前缀
        val lcp = findLongestCommonPrefix(newContent, lastContent)
        if (lcp.length >= lastContent.length * 0.8) {
            // 如果公共前缀足够长（>= 80%），认为匹配成功
            return newContent.substring(lcp.length)
        }
        
        // 情况4：尝试在新内容中查找旧内容的位置
        val index = newContent.indexOf(lastContent)
        if (index >= 0) {
            return newContent.substring(index + lastContent.length)
        }
        
        // 情况5：尝试查找旧内容的子串（处理格式变化）
        // 如果旧内容足够长，尝试查找其大部分内容
        if (lastContent.length > 20) {
            val searchKey = lastContent.substring(0, lastContent.length / 2)
            val foundIndex = newContent.indexOf(searchKey)
            if (foundIndex >= 0 && foundIndex < newContent.length / 2) {
                // 如果找到的位置在内容前半部分，可能是累积
                val remaining = newContent.substring(foundIndex + searchKey.length)
                // 检查剩余部分是否包含旧内容的后续部分
                val nextKey = lastContent.substring(searchKey.length)
                if (remaining.startsWith(nextKey)) {
                    return remaining.substring(nextKey.length)
                }
            }
        }
        
        // 情况6：无法确定增量，返回全部内容（可能是全新的内容）
        return newContent
    }
    
    /**
     * 找到两个字符串的最长公共前缀
     */
    private fun findLongestCommonPrefix(str1: String, str2: String): String {
        val minLength = minOf(str1.length, str2.length)
        var commonLength = 0
        
        for (i in 0 until minLength) {
            if (str1[i] == str2[i]) {
                commonLength++
            } else {
                break
            }
        }
        
        return str1.substring(0, commonLength)
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
            if (request.isFirstAssistantReply) {
                builder.appendLine("你是销售助手。若这是本会话的首条助手回复，请严格以以下格式开头：")
                builder.appendLine("【客户】<主要客户或联系人名称，无法识别时写“未知客户”>")
                builder.appendLine("【场景】<简短的销售场景/任务名，如 报价跟进、会议纪要、客户异议处理、跟进邮件、销售话术优化、通用销售咨询>")
                builder.appendLine()
                builder.appendLine("（如有误，请简单说明客户和场景，我会更新理解。）")
                builder.appendLine()
                builder.appendLine("在上述固定开头后，继续给出完整、有帮助的回答。")
            } else {
                builder.appendLine("请按下面格式回复（总长不超过 200 字）：")
                builder.appendLine("1) 用 1-2 句话复述用户问题；如信息不足，直接说明需要更多细节")
                builder.appendLine("2) 用 2-3 条要点给出核心洞察")
                builder.appendLine("3) 用 2-4 条可执行建议")
                builder.appendLine("4) 如需深度拆解，可提醒使用「智能分析」获取完整版并可导出 PDF/CSV")
                builder.appendLine()
                builder.append("重要：不要重复前面的内容，每个编号只写一次，不要累积重复。")
            }
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
