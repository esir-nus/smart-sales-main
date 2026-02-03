# Scheduler Interface

> **Blackbox contract** — For consumers (Chat, PrismShell). Don't read implementation.

---

## You Can Call

### ScheduledTaskRepository

```kotlin
interface ScheduledTaskRepository {
    fun getTimelineItems(dayOffset: Int): Flow<List<TimelineItemModel>>
    fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<TimelineItemModel>>
    suspend fun insertTask(task: TimelineItemModel.Task): String
    suspend fun updateTask(task: TimelineItemModel.Task)
    suspend fun deleteItem(id: String)
}
```

### SchedulerLinter

```kotlin
class SchedulerLinter {
    fun lint(llmOutput: String): LintResult
}

sealed class LintResult {
    data class Success(
        val task: TimelineItemModel.Task,
        val reminderType: ReminderType?,
        val taskTypeHint: TaskTypeHint,
        val parsedClues: ParsedClues = ParsedClues()  // Phase 1 → Phase 2 bridge
    ) : LintResult()
    
    data class Incomplete(
        val missingField: String,  // "startTime" | "duration"
        val question: String,      // User-facing question
        val partialClues: ParsedClues
    ) : LintResult()
    
    data class Error(val message: String) : LintResult()
    
    // Wave 4.0: Input Classification
    data class NonIntent(val reason: String) : LintResult()
    data class Inspiration(val content: String) : LintResult()
}

/**
 * Phase 1 frozen clues — passed to Phase 2 for LLM entity resolution
 */
data class ParsedClues(
    val person: String? = null,
    val location: String? = null,
    val briefSummary: String? = null,
    val durationMinutes: Int? = null
)
```

### AlarmScheduler

```kotlin
interface AlarmScheduler {
    suspend fun scheduleAlarm(taskId: String, triggerAt: Long)
    suspend fun cancelAlarm(taskId: String)
    suspend fun reschedule(taskId: String, newTriggerAt: Long)
}
```

---

## Input/Output Types

### TimelineItemModel

```kotlin
sealed class TimelineItemModel {
    abstract val id: String
    abstract val timeDisplay: String
    
    data class Task(
        override val id: String,
        override val timeDisplay: String,
        val title: String,
        val isDone: Boolean = false,
        val hasAlarm: Boolean = false,
        val isSmartAlarm: Boolean = false,
        val startTime: Instant,
        val endTime: Instant? = null,
        val durationMinutes: Int = 30,
        val durationSource: DurationSource = DurationSource.DEFAULT,
        val conflictPolicy: ConflictPolicy = ConflictPolicy.EXCLUSIVE,
        val dateRange: String = "",
        val location: String? = null,
        val notes: String? = null,
        val keyPerson: String? = null,
        val highlights: String? = null,
        val alarmCascade: List<String>? = null
    ) : TimelineItemModel()
    
    data class Inspiration(
        override val id: String,
        override val timeDisplay: String,
        val title: String
    ) : TimelineItemModel()
    
    data class Conflict(
        override val id: String,
        override val timeDisplay: String,
        val conflictText: String
    ) : TimelineItemModel()
}
```

### Supporting Types

```kotlin
enum class TaskTypeHint { MEETING, CALL, URGENT, PERSONAL }
enum class ReminderType { SINGLE, SMART_CASCADE }
```

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `insertTask` | Returns generated ID, triggers Flow emission |
| `getTimelineItems(0)` | Today's tasks, sorted by startTime |
| `lint()` | Validates JSON, returns parsed Task or Error |
| `scheduleAlarm` | Alarm fires at triggerAt ± 1 min |

---

## You Should NOT

- ❌ Read `RealScheduledTaskRepository.kt` implementation
- ❌ Read `SchedulerDAO.kt` (Room layer)
- ❌ Access database directly
- ❌ Parse LLM JSON manually — use `SchedulerLinter`
- ❌ Implement your own conflict logic — use `ScheduleBoard` from Memory Center

---

## Memory Center Integration

Scheduler **consumes** Memory Center's `ScheduleBoard` for conflict detection.

```kotlin
// From MemoryCenter interface:
interface ScheduleBoard {
    val upcomingItems: StateFlow<List<ScheduleItem>>
    suspend fun checkConflict(
        proposedStart: Long, 
        durationMinutes: Int,
        excludeId: String? = null  // Exclude self to avoid false positives
    ): ConflictResult
    suspend fun refresh()
}
```

**Rule**: Use `ScheduleBoard` interface only. Don't implement conflict logic in Scheduler.

---

## When to Read Full Spec

Read `spec.md` only if:
- You are working **ON** Scheduler itself (not just consuming it)
- You need to understand LLM prompt construction
- You are modifying linting rules or alarm cascade logic

Otherwise, **trust this interface**.
