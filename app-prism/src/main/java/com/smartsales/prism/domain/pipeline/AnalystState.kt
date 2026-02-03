package com.smartsales.prism.domain.pipeline

import com.smartsales.prism.domain.analyst.PlannerTable

/**
 * Analyst 模式状态层级 (V2: Three-Layer Intelligence)
 * 
 * 三阶段：对话 → 结构化 → 执行
 * 
 * @see prism-ui-ux-contract.md "Analyst Mode (V2: Three-Layer Intelligence)"
 */
sealed interface AnalystState : FlowState {
    /** 空闲 - 等待用户输入 */
    data object Idle : AnalystState

    /** 
     * Phase 1: 对话阶段 (Conversational Planner)
     * 
     * 用户与 AI 对话，AI 使用 Thinking Trace 展示推理过程。
     */
    data class Conversing(
        val trace: List<String> = emptyList()
    ) : AnalystState

    /**
     * Phase 1 完成: 对话响应 (Plain Text Response)
     * 
     * LLM 返回纯文本（非结构化），需要渲染到聊天历史。
     */
    data class Responded(
        val text: String
    ) : AnalystState

    /** 
     * Phase 2: 结构化阶段 (Plan Formalization)
     * 
     * AI 生成 Planner Table，展示分析计划。
     */
    data class Structured(
        val table: PlannerTable
    ) : AnalystState

    /** 
     * Phase 3: 执行阶段 (Execution)
     * 
     * 用户从 Task Board 选择工作流，AI 执行并更新状态。
     */
    data class Executing(
        val table: PlannerTable,
        val currentStepIndex: Int
    ) : AnalystState
}
