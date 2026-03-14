# Scheduler Drawer UI Contract

> **Owner**: `:app-core` / UI Layer
> **ViewModel**: `SchedulerViewModel`

## UI State

```kotlin
sealed class UiState {
    data class SchedulerTaskCreated(
        val taskId: String,
        val title: String,
        val dayOffset: Int,
        val isReschedule: Boolean = false
    ) : UiState()
    // Note: Ambiguity/Clarification states are NO LONGER mapped as a global UiState. 
    // They are handled dynamically per-task via `TimelineItem.clarificationState` 
    // to support the Dual-Path (Town and Highway) concurrent scheduling model.
}

// Visual Domain Mapping (Wave 13 & 14)
sealed class TimelineItem {
    // Defines visual tasks, inspirations, and conflicts natively for Compose 
    // without coupling directly to ScheduledTask SSD entities.
    // Represents both completed memories and active tasks uniformly.
    // **Wave 14 Rule**: Must contain an unwrapped mapping of `ClarificationState` 
    // so the Compose UI card can dynamically resolve Path B ambiguity inline.
}
```

## UI Intents (Actions)

```kotlin
sealed interface SchedulerIntent {
    data class OnToggleTaskComplete(val taskId: String) : SchedulerIntent()
    data class OnReschedule(val instruction: String, val replaceItemId: String) : SchedulerIntent()
    data class OnVoiceInputSubmitted(val audioFile: java.io.File) : SchedulerIntent() // Delegated hardware
    data object OnDismiss : SchedulerIntent()
}
```

## You Should NOT
- ❌ Provide JSON schemas or NLP logic here.
- ❌ Implement Audio Recording hardware calls inside `@Composable` functions. (Must Emit `OnVoiceInputSubmitted`).
