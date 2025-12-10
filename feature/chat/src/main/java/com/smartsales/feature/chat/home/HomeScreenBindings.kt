package com.smartsales.feature.chat.home

import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.feature.chat.core.SystemPromptBuilder
import com.smartsales.feature.chat.core.SystemPromptContext
import com.smartsales.feature.usercenter.SalesPersona

object HomeScreenBindings {
    fun buildPromptWithHistory(
        request: ChatRequest,
        persona: SalesPersona? = null,
        isFirstGeneralAssistantReply: Boolean = false
    ): String {
        val builder = StringBuilder()

        val systemPrompt = SystemPromptBuilder.buildForHomeChat(
            SystemPromptContext(
                persona = persona,
                quickSkillId = request.quickSkillId,
                isFirstGeneralAssistantReply = isFirstGeneralAssistantReply
            )
        )
        builder.appendLine(systemPrompt)
        builder.appendLine()

        if (request.history.isNotEmpty()) {
            builder.appendLine("历史对话：")
            request.history.forEach { item ->
                val roleLabel = when (item.role) {
                    ChatRole.USER -> "用户"
                    ChatRole.ASSISTANT -> "助手"
                    else -> "其他"
                }
                builder.appendLine("$roleLabel：${item.content}")
            }
            builder.appendLine()
        }

        builder.append("最新问题：${request.userMessage}")

        return builder.toString()
    }
}
