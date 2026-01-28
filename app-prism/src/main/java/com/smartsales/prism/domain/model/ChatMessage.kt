package com.smartsales.prism.domain.model

/**
 * 聊天消息实体 — 用于 UI 渲染历史记录
 */
sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long

    data class User(
        override val id: String,
        override val timestamp: Long,
        val content: String
    ) : ChatMessage()

    data class Ai(
        override val id: String,
        override val timestamp: Long,
        val uiState: UiState // 复用 UiState 来承载 AI 的响应内容 (Response, PlanCard, etc.)
    ) : ChatMessage()
}
