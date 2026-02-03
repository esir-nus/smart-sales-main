package com.smartsales.prism.domain.model

import com.smartsales.prism.domain.pipeline.ExecutionPlan

/**
 * Pipeline UI 状态密封类
 * @see Prism-V1.md §2.2
 */
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Thinking(val hint: String? = null) : UiState()
    data class Streaming(val partialContent: String) : UiState()
    data class Response(val content: String, val structuredJson: String? = null) : UiState()
    data class SchedulerTaskCreated(val title: String, val dayOffset: Int) : UiState()
    data class PlanCard(val plan: ExecutionPlan, val completedSteps: Set<Int> = emptySet()) : UiState()
    data class Error(val message: String, val retryable: Boolean = true) : UiState()

    // Analyst Mode V2 State
    data class PlannerTableState(val table: com.smartsales.prism.domain.analyst.PlannerTable) : UiState()
}
