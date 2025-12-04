package com.smartsales.feature.chat.home.orchestrator

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestrator.kt
// 模块：:feature:chat
// 说明：Home 层 Orchestrator 接口，当前仅代理现有聊天服务
// 作者：创建于 2025-12-04

import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import kotlinx.coroutines.flow.Flow

/**
 * Home 层的 Orchestrator，后续负责模式与元数据编排。
 * T-Task 2 阶段仅做直通代理，不更改请求或事件。
 */
interface HomeOrchestrator {
    fun streamChat(request: ChatRequest): Flow<ChatStreamEvent>
}
