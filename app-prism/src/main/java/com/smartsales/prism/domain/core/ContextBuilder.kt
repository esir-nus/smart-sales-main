package com.smartsales.prism.domain.core

/**
 * 上下文构建器 — 多模态输入归一化
 * @see Prism-V1.md §2.2 #1
 */
interface ContextBuilder {
    /**
     * 构建增强上下文
     * @param userText 用户输入文本
     * @param mode 当前模式
     * @return 归一化的 EnhancedContext
     */
    suspend fun build(userText: String, mode: Mode): EnhancedContext
}
