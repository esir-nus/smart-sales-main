package com.smartsales.prism.domain.analyst

import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.EnhancedContext

/**
 * Analyst Architect Service
 * Handles Phase 2 (Planning) and Phase 3 (Investigation) of Analyst Mode.
 */
interface ArchitectService {
    /**
     * Phase 2: Generates a structured investigation plan.
     * 
     * @param input User's proven analytical intent
     * @param context RAM snapshot (EnhancedContext) loaded by Kernel
     * @param sessionHistory Previous conversation turns
     * @return Structured plan mapping to the Markdown Strategy bubble
     */
    suspend fun generatePlan(
        input: String,
        context: EnhancedContext,
        availableTools: List<AnalystTool>,
        sessionHistory: List<ChatTurn> = emptyList()
    ): PlanResult

    /**
     * Phase 3: Executes the investigation based on the plan and context.
     * 
     * @param plan The user-confirmed PlanResult from Phase 2
     * @param context RAM snapshot (EnhancedContext) loaded by Kernel
     * @param sessionHistory Previous conversation turns
     * @return Final analysis text and any suggested actionable workflows
     */
    suspend fun investigate(
        plan: PlanResult,
        context: EnhancedContext,
        sessionHistory: List<ChatTurn> = emptyList()
    ): InvestigationResult
}
