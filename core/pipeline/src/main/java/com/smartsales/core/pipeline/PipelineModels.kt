package com.smartsales.core.pipeline

import com.smartsales.core.context.ContextDepth


/**
 * Raw input details from the user.
 */
data class PipelineInput(
    val rawText: String,
    val isVoice: Boolean = false,
    val intent: QueryQuality = QueryQuality.DEEP_ANALYSIS,
    val replaceItemId: String? = null,
    val requestedDepth: ContextDepth = ContextDepth.FULL
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
        val uiState: com.smartsales.prism.domain.model.UiState
    ) : PipelineResult()
    
    /**
     * A singular CRM task was created successfully.
     */
    data class SchedulerTaskCreated(
        val taskId: String,
        val title: String,
        val dayOffset: Int,
        val scheduledAtMillis: Long,
        val durationMinutes: Int,
        val isReschedule: Boolean = false
    ) : PipelineResult()
    
    /**
     * Multiple CRM tasks were created successfully.
     */
    data class SchedulerMultiTaskCreated(
        val tasks: List<SchedulerTaskCreated>,
        val hasConflict: Boolean
    ) : PipelineResult()

    /**
     * Wave 4: Auto-Renaming
     * Triggered when the pipeline successfully parses an intent and generates a new session title.
     */
    data class AutoRenameTriggered(
        val newTitle: String
    ) : PipelineResult()
}
