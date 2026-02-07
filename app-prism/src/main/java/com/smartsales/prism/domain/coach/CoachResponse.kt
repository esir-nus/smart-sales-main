package com.smartsales.prism.domain.coach

import com.smartsales.prism.domain.pipeline.MemoryHit

/**
 * Coach 响应密封类 — 简化的单一变体
 * 
 * 当前只有 Chat 变体。Memory 命中信息作为元数据嵌入，不作为独立变体。
 * 
 * @see docs/cerb/coach/interface.md
 */
sealed class CoachResponse {
    /**
     * 标准聊天响应
     * 
     * @param content Coach 回复文本
     * @param suggestAnalyst 是否建议切换到 Analyst 模式
     * @param memoryHits 上下文中使用的记忆条目（可选，用于透明度）
     */
    data class Chat(
        val content: String,
        val suggestAnalyst: Boolean = false,
        val memoryHits: List<MemoryHit> = emptyList()
    ) : CoachResponse()
}
