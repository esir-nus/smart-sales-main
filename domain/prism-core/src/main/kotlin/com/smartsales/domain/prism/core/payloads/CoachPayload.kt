package com.smartsales.domain.prism.core.payloads

/**
 * Coach 模式消息气泡
 * @see Prism-V1.md §5.7
 */
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Coach 模式载荷 — 存储在 MemoryEntryEntity.payloadJson
 */
data class CoachPayload(
    val messages: List<ChatMessage>,
    val topic: String?,
    val summaryGenerated: Boolean = false
)
