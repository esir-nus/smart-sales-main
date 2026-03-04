package com.smartsales.prism.domain.analyst

import com.smartsales.prism.domain.pipeline.EnhancedContext

/**
 * Result of Phase 2 Planning. Represents a structured plan.
 */
sealed class PlanResult {
    data class Strategy(
        val title: String,
        val summary: String,
        val markdownContent: String
    ) : PlanResult()
    
    data class ExpertBypass(
        val workflowId: String
    ) : PlanResult()
}

/**
 * Result of Phase 3 Investigation. Represents the final analysis and workflows.
 */
data class InvestigationResult(
    val analysisContent: String,
    val suggestedWorkflows: List<WorkflowSuggestion>
)


