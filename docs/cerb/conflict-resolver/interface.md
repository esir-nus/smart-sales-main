# Conflict Resolver Interface

> **Blackbox contract** — For consumers (SchedulerTimeline, SchedulerDrawer). Don't read implementation.

---

## You Can Call

### RealConflictResolver

```kotlin
class RealConflictResolver @Inject constructor(
    private val dashscopeExecutor: DashscopeExecutor
) {
    suspend fun resolve(
        userMessage: String,
        taskA: ScheduleItem,
        taskB: ScheduleItem
    ): ConflictResolution
}
```

---

## Input/Output Types

### ConflictResolution

```kotlin
data class ConflictResolution(
    val actions: List<ConflictAction>,
    val reply: String
)
```

### ConflictAction

```kotlin
data class ConflictAction(
    val action: ActionType,
    val taskToRemove: String?,       // ID for KEEP_A/KEEP_B
    val taskToReschedule: String?,   // ID for RESCHEDULE
    val rescheduleText: String?      // Natural language time ("明天下午3点")
)

enum class ActionType {
    KEEP_A,      // Keep first task, remove second
    KEEP_B,      // Keep second task, remove first  
    RESCHEDULE,  // Reschedule one of the tasks
    COEXIST,     // Keep both tasks, clear warning
    NONE         // No action / parsing failed
}
```

### ScheduleItem (from Memory Center)

```kotlin
data class ScheduleItem(
    val entryId: String,
    val title: String,
    val scheduledAt: Long,       // Epoch millis
    val durationMinutes: Int,
    val conflictPolicy: ConflictPolicy
)
```

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `resolve()` | Returns valid ConflictAction (never throws) |
| `ActionType.COEXIST` | Clears conflict state, preserves both tasks |
| `ActionType.NONE` | Returned on LLM failure with fallback reply |
| Reply text | Always non-empty (Chinese user-facing message) |

---

## You Should NOT

- ❌ Parse LLM JSON manually — use `RealConflictResolver`
- ❌ Call DashscopeExecutor directly for conflicts
- ❌ Access ScheduleBoard for conflict data — that's upstream
- ❌ Assume synchronous resolution — always suspend

---

## Consumer Integration

### In SchedulerViewModel

```kotlin
fun handleConflictResolution(action: ConflictAction) {
    viewModelScope.launch {
        when (action.action) {
            KEEP_A -> taskRepository.deleteItem(action.taskToRemove!!)
            KEEP_B -> taskRepository.deleteItem(action.taskToRemove!!)
            RESCHEDULE -> onReschedule(action.taskToReschedule!!, action.rescheduleText!!)
            COEXIST -> clearConflictWarning()
            NONE -> { /* Show error toast */ }
        }
        scheduleBoard.refresh()
    }
}
```

### UI Wiring Path

```
ConflictCard → onResolve(action) 
    → TimelineRow → onConflictResolve
    → SchedulerTimeline → onConflictResolve  
    → SchedulerDrawer → viewModel.handleConflictResolution(action)
```

---

## When to Read Full Spec

Read `spec.md` only if:
- Modifying LLM prompt for conflict resolution
- Changing the ActionType enum
- Adding new resolution strategies

Otherwise, **trust this interface**.
