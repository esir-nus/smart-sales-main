package com.smartsales.core.llm


/**
 * 执行器 — LLM 调用策略
 * @see Prism-V1.md §2.2 #3
 */
interface Executor {
    /**
     * 执行 LLM 推理
     * @param profile 使用的 LLM 策略模版配置
     * @param prompt 格式化后的提示词
     * @return 执行结果
     */
    suspend fun execute(profile: LlmProfile, prompt: String): ExecutorResult
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
