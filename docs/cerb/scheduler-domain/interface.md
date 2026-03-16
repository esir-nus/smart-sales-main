# Scheduler Domain (Dedicated Mutation Module) Interface

> **Owner**: `:domain:scheduler`
> **Consumers**: Intent Orchestrator, Unified Pipeline, UI Components

## Public Interface

```kotlin
interface ScheduledTaskRepository {
    /**
     * Retrieves the upcoming items on the ScheduleBoard for temporal/lexical matching
     */
    fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>>
    
    /**
     * Inserts a new task atomically.
     * Evaluates conflicts first. If conflict exists, sets hasConflict=true.
     */
    suspend fun insertTask(task: ScheduledTask): String

    /**
     * Reschedules an existing task.
     * 1. Looks up task by ID or Lexical Match.
     * 2. Wraps Delete(old) and Insert(new, inheriting old GUID) in a @Transaction.
     */
    suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask)
    
    /**
     * Batch inserts multiple tasks atomically inside a single transaction.
     */
    suspend fun batchInsertTasks(tasks: List<ScheduledTask>): List<String>
    
    // ... basic CRUD (deleteItem, updateTask, getTask)
}

interface ScheduleBoard {
    /**
     * Evaluates if a given time block conflicts with existing active tasks on the board.
     */
    suspend fun checkConflict(startTime: Instant, durationMinutes: Int): Boolean
    
    /**
     * Finds the closest matching active task based on a lexical query.
     */
    suspend fun findLexicalMatch(targetQuery: String): ScheduledTask?
}
```

## Data Models

```kotlin
data class ScheduledTask(
    val id: String, // Inherited during reschedule
    val title: String,
    val startTime: Instant,
    val durationMinutes: Int,
    val hasConflict: Boolean = false,
    val isVague: Boolean = false,
    // ... other existing fields
)
```
