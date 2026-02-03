package com.smartsales.prism.domain.analyst

/**
 * Planner Table 模型
 * 
 * Analyst 模式下的分析计划表格，作为聊天气泡内容展示。
 * 仅展示状态，不包含操作按钮。
 * 
 * @see prism-ui-ux-contract.md "Planner Table (Rich Chat Bubble)"
 */
data class PlannerTable(
    val title: String,
    val steps: List<PlannerStep>,
    val insight: String? = null,
    val readyMessage: String? = null
)

/**
 * 计划步骤
 */
data class PlannerStep(
    val index: Int,
    val task: String,
    val status: StepStatus
)

/**
 * 步骤状态
 */
enum class StepStatus {
    /** 待处理 */
    PENDING,
    /** 进行中 */
    IN_PROGRESS,
    /** 已完成 */
    COMPLETE
}
