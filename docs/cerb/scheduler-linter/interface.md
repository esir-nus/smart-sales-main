# Scheduler Linter Interface

> **Owner**: `:core:pipeline` (or specific `:domain:scheduler-linter` module)
> **Consumers**: `IntentOrchestrator`, `UnifiedPipeline`

## Public Interface

```kotlin
interface SchedulerLinter {
    /**
     * Parses real-time ASR audio intents into exact one-currency DTOs for Path A optimistic execution.
     * Operates purely on lexical/semantic input without requiring an LLM if local rules match, 
     * or via a fast LLM extraction pass.
     */
    suspend fun parseFastTrackIntent(input: String): FastTrackResult
}
```

## Data Models

```kotlin
sealed class FastTrackResult {
    data class CreateTasks(val params: CreateTasksParams) : FastTrackResult()
    data class RescheduleTask(val params: RescheduleTaskParams) : FastTrackResult()
    data class CreateInspiration(val params: CreateInspirationParams) : FastTrackResult()
    data class NoMatch(val reason: String) : FastTrackResult()
}

@Serializable
data class CreateTasksParams(
    val unifiedId: String? = null,
    val tasks: List<TaskDefinition>
)

@Serializable
data class TaskDefinition(
    val title: String,
    val startTimeIso: String,
    val durationMinutes: Int, 
    val urgency: UrgencyEnum
)

enum class UrgencyEnum {
    L1_CRITICAL, L2_IMPORTANT, L3_NORMAL, FIRE_OFF
}

@Serializable
data class RescheduleTaskParams(
    val unifiedId: String? = null,
    val targetTimeIso: String? = null,
    val targetQuery: String? = null,
    val newStartTimeIso: String,
    val newDurationMinutes: Int? = null 
)

@Serializable
data class CreateInspirationParams(
    val unifiedId: String? = null,
    val content: String 
)
```
