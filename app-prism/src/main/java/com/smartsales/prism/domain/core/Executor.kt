package com.smartsales.prism.domain.core

/**
 * 执行器 — LLM 调用策略
 * @see Prism-V1.md §2.2 #3
 */
interface Executor {
    /**
     * 执行 LLM 推理
     * @param context 增强上下文
     * @return 执行结果
     */
    suspend fun execute(context: EnhancedContext): ExecutorResult
}

/**
 * 执行结果
 */
sealed class ExecutorResult {
    /**
     * 成功 — 返回文本响应
     */
    data class Success(
        val content: String,
        val tokenUsage: TokenUsage = TokenUsage()
    ) : ExecutorResult()
    
    /**
     * 失败 — 返回错误信息
     */
    data class Failure(
        val error: String,
        val retryable: Boolean = true
    ) : ExecutorResult()
}

/**
 * Token 用量统计
 */
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0
) {
    val totalTokens: Int get() = promptTokens + completionTokens
}
