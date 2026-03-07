package com.smartsales.core.context

import com.smartsales.prism.domain.model.Mode

/**
 * 上下文构建器 — 多模态输入归一化
 * @see Prism-V1.md §2.2 #1
 */
interface ContextBuilder {
    /**
     * 构建增强上下文
     * @param userText 用户输入文本
     * @param mode 当前模式
     * @param resolvedEntityIds 通过 InputParser 获取的实体ID列表
     * @param depth 加载深度，默认为 ContextDepth.FULL (保留向后兼容)
     * @return 归一化的 EnhancedContext
     */
    suspend fun build(
        userText: String, 
        mode: Mode, 
        resolvedEntityIds: List<String> = emptyList(),
        depth: ContextDepth = ContextDepth.FULL
    ): EnhancedContext
    
    /**
     * 获取当前会话历史
     */
    fun getSessionHistory(): List<ChatTurn>
    
    /**
     * 记录用户消息到历史
     */
    suspend fun recordUserMessage(content: String)
    
    /**
     * 记录助手消息到历史
     */
    suspend fun recordAssistantMessage(content: String)
    
    /**
     * 重置会话 (Wave 3)
     * 清空当前 SessionWorkingSet，重置轮次计数，并不自动持久化旧会话
     */
    fun resetSession()

    /**
     * 获取当前活跃会话 ID (Wave 4)
     */
    fun getActiveSessionId(): String

    /**
     * 加载历史会话到 RAM (Wave 4)
     * 恢复会话历史和轮次计数
     */
    fun loadSession(sessionId: String, history: List<ChatTurn>)
    
    /**
     * 加载临时文档上下文到 RAM
     * 用于跨流传递大文本（例如音频分析时的转写结果）
     */
    fun loadDocumentContext(payload: String)
}
