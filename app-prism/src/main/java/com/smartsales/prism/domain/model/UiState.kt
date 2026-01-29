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
    data class PlanCard(val plan: ExecutionPlan, val completedSteps: Set<Int> = emptySet()) : UiState()
    data class Error(val message: String, val retryable: Boolean = true) : UiState()

    // Analyst Mode FSM Mapping (v2.7)
    data class AnalystParsing(val ticker: String, val progress: Float) : UiState()
    data class AnalystProposal(val plan: com.smartsales.prism.domain.pipeline.AnalystPlan) : UiState()
    data class AnalystExecuting(val planTitle: String) : UiState()
    data class AnalystResult(val artifact: com.smartsales.prism.domain.pipeline.PlanArtifact) : UiState()
}

