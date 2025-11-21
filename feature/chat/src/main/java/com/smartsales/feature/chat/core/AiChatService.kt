package com.smartsales.feature.chat.core

import kotlinx.coroutines.flow.Flow

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/core/AiChatService.kt
// 模块：:feature:chat
// 说明：定义与 DashScope 对接的聊天 streaming 契约（不含实现）
// 作者：创建于 2025-11-20

/** 输入历史：只保留角色 + 文本，供 LLM 回顾上下文。 */
data class ChatHistoryItem(
    val role: ChatRole,
    val content: String
)

/** LLM 聊天角色，映射到 USER/ASSISTANT。 */
enum class ChatRole { USER, ASSISTANT }

/** 可选音频上下文，后续整合 Tingwu/录音摘要时使用。 */
data class AudioContextSummary(
    val readyClipCount: Int,
    val pendingClipCount: Int,
    val hasTranscripts: Boolean,
    val note: String? = null
)

/** Chat 请求模型，封装 session、用户输入、技能标签与历史。 */
data class ChatRequest(
    val sessionId: String,
    val userMessage: String,
    val quickSkillId: String? = null,
    val audioContextSummary: AudioContextSummary? = null,
    val history: List<ChatHistoryItem> = emptyList()
)

/** Streaming 事件模型：token 增量、完成、错误。 */
sealed class ChatStreamEvent {
    data class Delta(val token: String) : ChatStreamEvent()
    data class Completed(val fullText: String) : ChatStreamEvent()
    data class Error(val throwable: Throwable) : ChatStreamEvent()
}

/** 与 DashScope 对接的 streaming 聊天接口（无实现，仅契约）。 */
interface AiChatService {
    /**
     * 发起 streaming 聊天请求。实现会调用 DashScope 并不断推送事件。
     * @return Flow<ChatStreamEvent> 供 ViewModel 逐个消费。
     */
    fun streamChat(request: ChatRequest): Flow<ChatStreamEvent>
}
