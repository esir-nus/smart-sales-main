package com.smartsales.aitest.ui.screens.home.model

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/home/model/ChatMessage.kt
// 模块：:app
// 说明：Home 底部导航页的 UI 专用聊天模型（仅本地模拟，无后端依赖）
// 作者：创建于 2025-12-02

import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

data class SkillSuggestion(
    val id: String,
    val label: String,
    val prompt: String
)
