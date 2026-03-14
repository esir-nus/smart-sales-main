# Scheduler UI Contract

> **Owner**: Scheduler / UI Layer
> **Context Boundary**: `docs/cerb-ui/scheduler/`
> **OS Layer**: Display (RAM Reader) & Keyboard (Event Writer)

## UI State

```kotlin
sealed class SchedulerUiState {
    object Idle : SchedulerUiState()
    object ScanningTimeline : SchedulerUiState()
    
    data class ConflictDetected(
        val existingTaskTitle: String,
        val proposedTime: String,
        val conflictReason: String
    ) : SchedulerUiState()
    
    data class TaskConfirm(
        val title: String,
        val startTime: String,
        val endTime: String,
        val urgency: String,
        val contextTags: List<String>
    ) : SchedulerUiState()
    
    object Executing : SchedulerUiState()
    
    data class Success(val message: String) : SchedulerUiState()
    
    data class Error(val message: String) : SchedulerUiState()
}
```

## UI Intents (Actions)

```kotlin
sealed interface SchedulerIntent {
    object OnConfirm : SchedulerIntent()
    object OnCancel : SchedulerIntent()
    data class OnEditField(val fieldName: String, val newValue: String) : SchedulerIntent()
    data class OnResolveConflict(val resolutionStrategy: String) : SchedulerIntent()
}
```

## OS Model Precepts
- The Scheduler UI **must not** directly invoke the `RoomDatabase` or `UnifiedPipeline`.
- It consumes `SchedulerUiState` and emits `SchedulerIntent` callbacks to its parent or ViewModel.
- It is a purely deterministic presentation component.
