# Implementation Plan: Actionable vs Factual Unification

## Goal
Establish a clear, unified architecture mapping Scheduler tasks and CRM activities into two distinct psychological feeds: **Actionable** (pending, future) and **Factual** (completed, past).

This implements the architectural rule: **"Single Source of Truth, Reactive Projections."** The system will *not* duplicate schedule data into the CRM database. It will strictly use Coroutine `Flow` combination to merge `ScheduledTaskRepository` and `MemoryRepository` streams dynamically via `ClientProfileHub`.

Additionally, this enforces the **Hardware Badge Delegation Constraint**. The *only* way to create an `actionable` item (which inherently requires time alarms and calendar integration) is via the badge pipeline writing to the `ScheduledTaskRepository`.

## The Rule: Crossing Off (State Shift)
An item shifts from **Actionable** to **Factual** the moment either condition is true:
1. `isDone == true` (User manually taps the checkbox in UI)
2. `endTime < now` (The deadline expires)

## Proposed Architecture (Client Profile Hub)

### 1. Unified Interface Definition (Raw Domain Surfacing)
Update `ClientProfileHub` to expose these dedicated feeds dynamically using **the exact original data classes**. Do not create translation wrappers.

```kotlin
interface ClientProfileHub {
    // Other existing methods...
    
    /**
     * The unified feed. 
     * Emits a reactive struct containing both actionable and factual items.
     */
    fun observeProfileActivityState(entityId: String): Flow<ProfileActivityState>
}

data class ProfileActivityState(
    // The Actionable feed is pure, unmodified Scheduler Tasks.
    val actionableItems: List<com.smartsales.prism.domain.scheduler.TimelineItemModel.Task>,
    
    // The Factual feed is pure, unmodified Memory Entries. 
    val factualItems: List<com.smartsales.prism.domain.memory.MemoryEntry>
)
```

### 2. The Reactive Pipeline (No Dual Writes)
Inside `RealClientProfileHub`, we will construct a `combine` flow. The crucial difference is that when a Task is "Crossed Off", it triggers the permanent creation of a `MemoryEntry` and the permanent deletion of the `SchedulerTask`.

```kotlin
override fun observeProfileActivityState(entityId: String): Flow<ProfileActivityState> {
    val tasksFlow = scheduledTaskRepository.observeTasksForEntity(entityId)
    val memoryFlow = memoryRepository.observeMemoryForEntity(entityId)
    
    // We also need a fast-ticking timer if we want minute-by-minute expiry UI updates,
    // OR we rely on the ViewModel's onStart sweep + day switches to re-trigger.
    // For MVP efficiency, we trigger evaluation whenever the DB flows emit.
    
    return combine(tasksFlow, memoryFlow) { tasks, memories ->
        val now = timeProvider.now
        
        // 1. Separate Active Tasks from Finished Tasks
        val actionableTasks = tasks.filter { !it.isDone && (it.endTime == null || it.endTime.isAfter(now)) }
        // Note: Finished tasks physically migrate to Memory. It is up to the ViewModel or an orchestrator
        // to call `memoryRepository.save()` and `scheduledTaskRepository.delete()` when a task completes.
        
        // 2. Assemble and Sort
        ProfileActivityState(
            actionableItems = actionableTasks.sortedBy { it.startTime }, // Earliest first
            factualItems = memories.sortedByDescending { it.timestamp } // Latest first
        )
    }
}
```

### 3. CRM Integration (Entity Display)
Because `ScheduledTask` already holds `keyPersonEntityId`, this Flow automatically fetches any schedule where the user said: "Remind me to call [Bob]".
- When Bob's CRM profile is opened, the UI subscribes to `observeProfileActivityState(bobId)`.
- If a task is crossed off, the `ScheduledTaskRepository` updates the SQLite row (`isDone = true`). 
- Room immediately emits the new `List<Task>`.
- `combine` block re-runs, classifying the task as *Factual*.
- UI instantly animates the item moving from the Actionable list to the Factual list.

## Verification Plan

### Automated Tests (L1)
- Write `RealClientProfileHubTest.kt` in `:domain:crm:test` using `FakeMemoryRepository` and `FakeScheduledTaskRepository`.
- **Test Case 1 (Creation)**: Emitting an active task in the future explicitly routes to `actionableItems`.
- **Test Case 2 (Manual Cross-off)**: Updating the fake task to `isDone = true` instantly drops it from `actionableItems` and prepends it to `factualItems`.
- **Test Case 3 (Time Expiry)**: Emitting a task where `endTime < timeProvider.now` strictly routes it to `factualItems`, even if `isDone = false`.
- **Test Case 4 (Memories)**: Emitting a raw CRM memory strictly routes it to `factualItems`.

### Architecture Contract & Spec Audit
- **Spec Examiner**: Update `docs/cerb/client-profile-hub/spec.md` to define `observeProfileActivityState` and the exact Actionable vs Factual taxonomy.
- **Contract Examiner**: Visually confirm in code reviews that `ScheduledTaskRepository.insert` handles ALL creation logic, confirming the single source of truth (no CRM DB dual writes). 

## User Review Required
Before proceeding into execution:
1. Does this architecture and taxonomy match your expectation?
2. Do you want me to proceed with updating the `client-profile-hub/spec.md` to lock in this design?
