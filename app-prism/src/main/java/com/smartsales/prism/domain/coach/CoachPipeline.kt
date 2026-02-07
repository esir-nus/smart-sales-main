package com.smartsales.prism.domain.coach

import com.smartsales.prism.domain.pipeline.ChatTurn

/**
 * Coach 管道接口 — 轻量级对话处理
 * 
 * Coach 模式的核心入口。接收用户输入和会话历史，返回 CoachResponse。
 * 
 * @see docs/cerb/coach/interface.md
 */
interface CoachPipeline {
    /**
     * 处理用户输入并返回 Coach 响应
     * 
     * @param input 用户消息文本
     * @param sessionHistory 当前会话历史（可选，用于上下文）
     * @return CoachResponse（单一 Chat 变体）
     */
    suspend fun process(
        input: String,
        sessionHistory: List<ChatTurn> = emptyList()
    ): CoachResponse
}
