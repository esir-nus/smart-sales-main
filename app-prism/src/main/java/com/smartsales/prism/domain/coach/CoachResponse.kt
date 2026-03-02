package com.smartsales.prism.domain.coach


/**
 * Coach 响应密封类 — 简化的单一变体
 * 
 * 当前只有 Chat 变体。Memory 命中信息作为元数据嵌入，不作为独立变体。
 * 
 * @see docs/cerb/coach/interface.md
 */
sealed class CoachResponse {
    data class Chat(
        val content: String,
        val suggestAnalyst: Boolean = false
    ) : CoachResponse()
}
