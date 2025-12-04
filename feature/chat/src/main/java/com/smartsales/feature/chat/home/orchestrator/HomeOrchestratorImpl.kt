package com.smartsales.feature.chat.home.orchestrator

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
// 模块：:feature:chat
// 说明：Home 层 Orchestrator 实现，当前仅直通现有聊天服务
// 作者：创建于 2025-12-04

import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class HomeOrchestratorImpl @Inject constructor(
    private val aiChatService: AiChatService
) : HomeOrchestrator {
    override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
        return aiChatService.streamChat(request)
    }
}
