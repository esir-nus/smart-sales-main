package com.smartsales.prism.domain.pipeline

/**
 * Analyst 模式状态层级
 * 
 * 用于 FSM 状态管理，纯数据类保留在 domain 层。
 * 
 * @see Prism-V1.md §4.6.1
 */
sealed interface AnalystState : FlowState {
    /** 空闲 - 等待用户输入 */
    data object Idle : AnalystState

    /** 解析中 - 感知阶段 (Ticker) */
    data class Parsing(
        val currentTask: String,
        val progress: Float
    ) : AnalystState

    /** 规划中 - 认知阶段 (Thinking Trace) */
    data class Planning(
        val trace: List<String>
    ) : AnalystState

    /** 提议 - 等待用户确认 (Plan Card) */
    data class Proposal(
        val plan: AnalystPlan,
        val queue: List<String> = emptyList()
    ) : AnalystState

    /** 执行中 - 工具调用 */
    data class Executing(
        val plan: AnalystPlan,
        val currentStepId: String
    ) : AnalystState

    /** 完成 - 展示结果 (Artifact Card) */
    data class Result(
        val artifact: PlanArtifact
    ) : AnalystState
}

// ============================================================================
// 数据类
// ============================================================================

data class AnalystPlan(
    val context: String,
    val goal: String,
    val highlights: List<String>,
    val deliverables: List<PlanDeliverable>
)

data class PlanDeliverable(
    val id: String,
    val label: String
)

data class PlanArtifact(
    val title: String,
    val type: String,
    val previewText: String
)
