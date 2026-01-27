package com.smartsales.domain.prism.core

/**
 * Executor LLM 输出 — 包含展示内容和结构化 JSON
 * @see Prism-V1.md §2.2 #3, §5.6
 */
data class ExecutorResult(
    val displayContent: String,
    val structuredJson: String?,
    val toolResults: List<ToolResult> = emptyList(),
    val usage: TokenUsage? = null,
    val executionPlan: ExecutionPlan? = null  // Analyst mode plan card
)

/**
 * 工具执行结果
 */
data class ToolResult(
    val toolName: String,
    val output: String,
    val success: Boolean
)

/**
 * Token 用量统计
 */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int
) {
    val totalTokens: Int get() = promptTokens + completionTokens
}
