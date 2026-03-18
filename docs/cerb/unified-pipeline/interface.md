# Unified Pipeline Interface

> **Owner**: Core Pipeline
> **Consumers**: IntentOrchestrator

## Public Interface

```kotlin
interface UnifiedPipeline {
    /**
     * Executes the conversational pipeline: extracts intent, disambiguates entities,
     * assembles the context, and either dispatches a tool or streams a response.
     */
    suspend fun processInput(input: PipelineInput): Flow<PipelineResult>
}
```

## Data Models

```kotlin
data class PipelineInput(
    val rawText: String,
    val isVoice: Boolean = false,
    val intent: QueryQuality = QueryQuality.DEEP_ANALYSIS,
    val replaceItemId: String? = null,
    val requestedDepth: ContextDepth = ContextDepth.FULL,
    val resolvedEntityId: String? = null,
    val unifiedId: String
)

sealed interface SchedulerTaskCommand {
    data class CreateTasks(val params: CreateTasksParams) : SchedulerTaskCommand
    data class DeleteTask(val targetTitle: String) : SchedulerTaskCommand
    data class RescheduleTask(val params: RescheduleTaskParams) : SchedulerTaskCommand
}

sealed class PipelineResult {
    data class ConversationalReply(val text: String) : PipelineResult()
    data class Progress(val message: String) : PipelineResult()
    data class AudioProcessing(val stage: String) : PipelineResult()
    data class ClarificationNeeded(val question: String) : PipelineResult()
    data class TaskCommandProposal(val command: SchedulerTaskCommand) : PipelineResult()
    data class ToolDispatch(val toolId: String, val params: Map<String, Any>) : PipelineResult()
    data class DisambiguationIntercepted(val uiState: UiState) : PipelineResult()
    data class ProfileMutation(val entityId: String, val field: String, val value: String)
    data class MutationProposal(val profileMutations: List<ProfileMutation>) : PipelineResult()
    data class AutoRenameTriggered(val newTitle: String) : PipelineResult()
}
```

## Notes

- Scheduler create/delete/reschedule no longer leave the pipeline as generic `ToolDispatch`.
- Generic plugin execution remains a separate lane and is not the owner of scheduler task execution.
