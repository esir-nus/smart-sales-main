package com.smartsales.prism.domain.analyst

import kotlinx.coroutines.flow.StateFlow

/**
 * The Orchestrator Contract for Analyst Mode.
 * OS Model: RAM Application
 */
interface AnalystPipeline {
    /**
     * Observe the current orchestrator state.
     * UI uses this to decide what to render (chat, planner table, task board).
     */
    val state: StateFlow<AnalystState>

    /**
     * Handle any user input in Analyst Mode.
     * The orchestrator decides the current phase and routes accordingly.
     *
     * @param input User message text
     * @param sessionHistory Prior messages in current session
     * @return AnalystResponse (one of the sealed variants)
     */
    suspend fun handleInput(
        input: String,
        sessionHistory: List<ChatTurn> = emptyList()
    ): AnalystResponse
}

enum class AnalystState {
    IDLE,           // Waiting for user input
    CONSULTING,     // Phase 1: Evaluating intent
    PROPOSAL,       // Plan generated, waiting for user confirmation
    INVESTIGATING,  // Phase 3: LLM reading EnhancedContext, reasoning
    RESULT          // Analysis delivered, Task Board mounted
}

sealed class AnalystResponse {
    /**
     * Phase 1: Conversational clarification.
     * Consumer renders as normal chat bubble.
     */
    data class Chat(
        val content: String
    ) : AnalystResponse()

    /**
     * Phase 2: Structured plan for user confirmation.
     * Consumer renders as PlannerTable bubble.
     * User must confirm before investigation begins.
     */
    data class Plan(
        val title: String,
        val summary: String,
        val markdownContent: String
    ) : AnalystResponse()

    /**
     * Phase 3 complete: Final analysis with optional follow-up actions.
     * Consumer renders analysis in chat + dynamic Task Board.
     */
    data class Analysis(
        val content: String,
        val suggestedWorkflows: List<WorkflowSuggestion> = emptyList()
    ) : AnalystResponse()
}



data class WorkflowSuggestion(
    val workflowId: String,
    val label: String
)
