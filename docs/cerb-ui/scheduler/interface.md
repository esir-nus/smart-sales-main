# Scheduler UI Contract

> **Cerb-compliant spec** — Self-contained, all content inline.  
> **OS Model**: Consumer of RAM (`IAgentViewModel` / `UiState`)
> **State**: SPEC_ONLY

---

## Architecture Role

The Scheduler UI is a pure presentation layer (Skin). It observes a unified `StateFlow<List<SchedulerTimelineItem>>` and `StateFlow<UiState>`, and dispatches simple intents (e.g., `onToggleDone(taskId)`) back to the ViewModel.
It does **not** contain any business logic, conflict resolution algorithms, or raw pipeline execution orchestration.

## The Reactive Unification (Actionable + Factual)

The Timeline displays a unified chronological feed:
1. **Actionable Tasks**: Stored in `ScheduledTaskRepository`. `isDone = false`.
2. **Factual Memory**: Completed tasks migrated to `MemoryRepository` (`MemoryEntryType.SCHEDULE_ITEM`). Mapped back to `Task(isDone = true)` for display.
3. **Inspirations**: Stored in `InspirationRepository`.

The UI layer is agnostic to this separation. The ViewModel's `timelineItems` Flow is responsible for combining and mapping these three disparate data sources into a single stream.

## Contracts

### `ISchedulerViewModel.kt`

```kotlin
interface ISchedulerViewModel {
    // 1. Data Streams
    val activeDayOffset: StateFlow<Int>
    val timelineItems: StateFlow<List<SchedulerTimelineItem>>
    val unacknowledgedDates: StateFlow<Set<Int>>
    val rescheduledDates: StateFlow<Set<Int>>
    
    // 2. Feedback Streams
    val pipelineStatus: StateFlow<String?>
    val conflictWarning: StateFlow<String?>
    val conflictedTaskIds: StateFlow<Set<String>>
    val causingTaskId: StateFlow<String?>
    
    // 3. User Intents
    fun onDateSelected(dayOffset: Int)
    fun acknowledgeDate(dayOffset: Int)
    fun onToggleDone(taskId: String)
    fun onDeleteItem(taskId: String)
    fun onReschedule(taskId: String, text: String)
}
```

## Fake ViewModel Behavior

The `FakeSchedulerViewModel` MUST be capable of exercising all the UI states above natively in `@Preview` without any Dagger/Hilt or Room database dependencies.

```kotlin
class FakeSchedulerViewModel : ISchedulerViewModel {
    // Driven by hardcoded MutableStateFlows for Compose UI tweaking
    // Provides dummy ScheduledTask entries (both isDone = true and false)
}
```
