package com.smartsales.data.prismlib.publishers

import com.smartsales.domain.prism.core.ExecutionPlan
import com.smartsales.domain.prism.core.ExecutorResult
import com.smartsales.domain.prism.core.UiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyst 模式发布器 — 展示 Plan Card 并流式输出分析章节
 * @see Prism-V1.md §2.2 #4, §4.2 Analyze Mode, §4.6 Analyst Planner-Centric Paradigm
 */
@Singleton
class AnalystPublisher @Inject constructor() : BaseModePublisher() {

    // 当前执行计划（用于 PlanCard 状态追踪）
    private var currentPlan: ExecutionPlan? = null
    private val completedSteps = mutableSetOf<Int>()

    override suspend fun publish(result: ExecutorResult) {
        // 1. 设置 Thinking 状态
        setThinking("正在分析...")

        // 2. 如果有执行计划，先展示 PlanCard
        result.executionPlan?.let { plan ->
            currentPlan = plan
            completedSteps.clear()
            _uiState.value = UiState.PlanCard(plan, emptySet())
        }

        // 3. 模拟流式输出内容
        if (result.displayContent.isNotEmpty()) {
            simulateStreaming(result.displayContent)
        }

        // 4. 发布最终响应
        _uiState.value = UiState.Response(
            content = result.displayContent,
            structuredJson = result.structuredJson
        )
    }

    /**
     * 标记计划步骤完成（供 Orchestrator 调用）
     */
    fun markStepComplete(stepIndex: Int) {
        completedSteps.add(stepIndex)
        currentPlan?.let { plan ->
            _uiState.value = UiState.PlanCard(plan, completedSteps.toSet())
        }
    }

    /**
     * 重置计划状态
     */
    fun resetPlan() {
        currentPlan = null
        completedSteps.clear()
    }
}
