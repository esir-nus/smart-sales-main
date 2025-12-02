package com.smartsales.aitest.ui.screens.home.model

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/home/model/ChatMessageMappers.kt
// 模块：:app
// 说明：底部导航 Home 使用的聊天与快捷技能 UI 映射工具
// 作者：创建于 2025-12-02

import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi
import com.smartsales.feature.chat.home.QuickSkillUi

fun ChatMessageUi.toUiMessage(): ChatMessage = ChatMessage(
    id = id,
    role = when (role) {
        ChatMessageRole.USER -> MessageRole.USER
        ChatMessageRole.ASSISTANT -> MessageRole.ASSISTANT
    },
    content = content,
    timestamp = timestampMillis,
    isError = hasError
)

fun List<ChatMessageUi>.toUiMessages(): List<ChatMessage> = map { it.toUiMessage() }

fun QuickSkillUi.toSkillSuggestion(): SkillSuggestion = SkillSuggestion(
    id = id,
    label = label,
    prompt = null
)
