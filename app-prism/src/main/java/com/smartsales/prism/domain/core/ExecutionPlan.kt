package com.smartsales.prism.domain.core

/**
 * Planner LLM 输出 — Plan-Once 执行计划
 * @see Prism-V1.md §4.5
 */
data class ExecutionPlan(
    val retrievalScope: RetrievalScope,
    val toolsToInvoke: List<ToolCall> = emptyList(),
    val deliverables: List<DeliverableType> = emptyList(),
    val workflowSuggestion: Mode? = null,
    val responseType: ResponseType = ResponseType.STREAMING
)

/**
 * 检索范围 — 决定 Context Builder 查询哪些内存层
 */
enum class RetrievalScope {
    NONE,           // 无需检索
    HOT_ONLY,       // 仅热区
    HOT_AND_CEMENT, // 热区 + 水泥区
    DEEP            // 全量检索
}

/**
 * 工具调用请求
 */
data class ToolCall(
    val toolName: String,
    val arguments: Map<String, String> = emptyMap()
)

/**
 * 交付物类型 — Plan Card 展示的任务项
 */
enum class DeliverableType {
    CHAT_RESPONSE,
    CHAPTER,
    KEY_INSIGHT,
    CHART,
    SCHEDULED_TASK,
    INSPIRATION
}

/**
 * 响应类型 — 决定 Publisher 的输出模式
 */
enum class ResponseType {
    STREAMING,       // 流式输出（默认）
    BUFFERED,        // 缓冲后输出（需要 Linter 验证）
    STRUCTURED_ONLY  // 仅结构化 JSON
}
