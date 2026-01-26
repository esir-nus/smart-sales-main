package com.smartsales.domain.prism.core

/**
 * 上下文构建器 — 将异构输入标准化为统一的上下文载荷
 * @see Prism-V1.md §2.2 #1
 */
interface ContextBuilder {
    /**
     * 构建增强上下文，包含用户输入、记忆命中、实体上下文等
     */
    suspend fun buildContext(userText: String, mode: Mode): EnhancedContext
}
