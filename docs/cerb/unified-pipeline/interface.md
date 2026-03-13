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
    val requestedDepth: ContextDepth = ContextDepth.FULL
)

sealed class PipelineResult {
    data class ConversationalReply(val text: String) : PipelineResult()
    data class Progress(val message: String) : PipelineResult()
    data class AudioProcessing(val stage: String) : PipelineResult()
    data class ClarificationNeeded(val question: String) : PipelineResult()
    data class ToolDispatch(val toolId: String, val params: Map<String, Any>) : PipelineResult()
    data class DisambiguationIntercepted(val uiState: UiState) : PipelineResult()
    data class SchedulerTaskCreated(val taskId: String, val title: String, val dayOffset: Int, val scheduledAtMillis: Long, val durationMinutes: Int, val isReschedule: Boolean) : PipelineResult()
    data class SchedulerMultiTaskCreated(val tasks: List<SchedulerTaskCreated>, val hasConflict: Boolean) : PipelineResult()
    data class AutoRenameTriggered(val newTitle: String) : PipelineResult()
}
```
