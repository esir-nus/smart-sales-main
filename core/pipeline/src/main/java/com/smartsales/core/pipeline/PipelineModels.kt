package com.smartsales.core.pipeline

import com.smartsales.core.context.ContextDepth
import com.smartsales.prism.domain.scheduler.CreateTasksParams
import com.smartsales.prism.domain.scheduler.RescheduleTaskParams
import com.smartsales.prism.domain.scheduler.ScheduledTask

sealed interface SchedulerTaskCommand {
    data class CreateTasks(
        val params: CreateTasksParams
    ) : SchedulerTaskCommand

    data class DeleteTask(
        val targetTitle: String
    ) : SchedulerTaskCommand

    data class RescheduleTask(
        val params: RescheduleTaskParams
    ) : SchedulerTaskCommand
}

/**
 * Raw input details from the user.
 */
data class PipelineInput(
    val rawText: String,
    val isVoice: Boolean = false,
    val isBadge: Boolean = false,
    val intent: QueryQuality = QueryQuality.DEEP_ANALYSIS,
    val replaceItemId: String? = null,
    val requestedDepth: ContextDepth = ContextDepth.FULL,
    val resolvedEntityId: String? = null,
    val unifiedId: String // Wave 14: Dual-Path sync token (No default to enforce determinism)
)

/**
 * Assembled Context Payload during the ETL Phase.
 * Populated progressively by parallel fetchers.
 */
data class PipelineContext(
    val entitySufficient: Boolean = true,
    val finalEnhancedContextText: String = "" // In a fully mapped system, this contains User Metadata, Habits, Memory, Scheduler
)

/**
 * Outcomes of the Unified Pipeline matching the spec.
 */
sealed class PipelineResult {
    /**
     * Shared Path A optimistic write committed through the single scheduler spine.
     */
    data class PathACommitted(
        val task: ScheduledTask
    ) : PipelineResult()

    /**
     * Shared Path A inspiration write committed through the single inspiration owner.
     */
    data class InspirationCommitted(
        val id: String,
        val content: String
    ) : PipelineResult()

    /**
     * Standard conversational output (verdict or chat).
     */
    data class ConversationalReply(val text: String) : PipelineResult()

    /**
     * Intermediate UI progress state for Transparent Mind (Wave 6).
     */
    data class Progress(val message: String) : PipelineResult()

    /**
     * CRM "Two-Ask" clarification request ("Did you mean Acme Corp?").
     */
    data class ClarificationNeeded(val question: String) : PipelineResult()

    /**
     * Explicit typed scheduler/task command proposal.
     * This stays outside the generic plugin lane.
     */
    data class TaskCommandProposal(
        val command: SchedulerTaskCommand
    ) : PipelineResult()

    /**
     * Explicit trigger of a plugin (Expert Bypass or Post-Verdict Execution).
     */
    data class ToolDispatch(
        val toolId: String,
        val params: Map<String, Any>
    ) : PipelineResult()
    
    /**
     * Disambiguation intercepted the flow, UI state needs to be rendered.
     */
    data class DisambiguationIntercepted(
        // Wave 5 T1: Pass the ambiguous candidates straight to the UI State
        val uiState: com.smartsales.prism.domain.model.UiState
    ) : PipelineResult()
    
    /**
     * T4 Mascot Flow Control: Explicitly signals the UI that the pipeline 
     * halted to route noise/greetings to the Mascot Service.
     */
    object MascotIntercepted : PipelineResult()
    
    /**
     * Wave 6: Explicitly signals the UI that a scheduling intent was intercepted
     * and the pipeline halted to route a hardware delegation hint.
     */
    object BadgeDelegationIntercepted : PipelineResult()
    
    data class ProfileMutation(
        val entityId: String,
        val field: String,
        val value: String
    )

    /**
     * T3 Open-Loop Defense: Proposal to mutate the database, requiring user confirmation.
     */
    data class MutationProposal(
        val profileMutations: List<ProfileMutation> = emptyList()
    ) : PipelineResult() {
        init {
            require(profileMutations.isNotEmpty()) {
                "MutationProposal must contain at least one profile mutation"
            }
        }
    }
    
    /**
     * T1: LLM recommended workflows for the user to optionally execute.
     */
    data class ToolRecommendation(
        val recommendations: List<com.smartsales.prism.domain.core.WorkflowRecommendation>
    ) : PipelineResult()

    /**
     * T1: Plugin system execution tracking events.
     */
    data class PluginExecutionStarted(val toolId: String) : PipelineResult()
    data class PluginExecutionEmittedState(val uiState: com.smartsales.prism.domain.model.UiState) : PipelineResult()

    /**
     * Wave 4: Auto-Renaming
     * Triggered when the pipeline successfully parses an intent and generates a new session title.
     */
    data class AutoRenameTriggered(
        val newTitle: String
    ) : PipelineResult()
}
