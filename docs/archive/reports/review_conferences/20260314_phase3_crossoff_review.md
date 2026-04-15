# 📋 Review Conference Report

**Subject**: Phase 3 (The Cross-Off Lifecycle) Implementation Plan
**Panel**: 
1. `/01-senior-reviewr` (Chair)
2. `/17-lattice-review` (Architecture & Boundaries)

---

### Panel Input Summary

#### `/17-lattice-review` — Architecture & Boundaries
- **Insight 1 (OS Layer Violation Risk)**: The plan proposes that `SchedulerViewModel` (UI Layer) directly calls `MemoryRepository.save(memoryEntry)` and `ScheduledTaskRepository.deleteItem(taskId)` (SSD Layer). While the OS model allows ViewModels to trigger repository mutations, the `interface-map.md` explicitly states: *"Memory queries go through MemoryRepository"*, but it doesn't explicitly forbid the ViewModel from writing to `MemoryRepository`.
- **Insight 2 (Entity Linkage Issue)**: The plan manually constructs the `structuredJson` (`{"entityId": "${task.keyPersonEntityId}"}`). This is fragile. If the `MemoryEntry` JSON schema changes in the future, this hardcoded string builder in the UI layer will break. It misses the opportunity to use a proper serializer or domain factory method.

#### `/01-senior-reviewr` — Pragmatism & Vibe Coding (Chair)
- **Insight 1 (Scattered Logic)**: Putting the "Task -> Memory" conversion logic straight into `SchedulerViewModel.toggleDone()` tightly couples the UI to the exact conversion schema. If another system (e.g., a background service or the LLM) needs to complete a task, the conversion logic is trapped in the Scheduler UI.
- **Insight 2 (Transaction Safety)**: Calling `memoryRepository.save()` followed by `taskRepository.deleteItem()` in two separate, non-transactional Coroutine calls risks a partial failure state (e.g., task is saved to memory, but app crashes before deletion = duplicated data in both actionable and factual feeds). However, since we are using separate Room databases (`memory` vs `schedule`), a true ACID transaction is impossible without a dedicated Coordinator/Saga pattern. Given the app's current scale, a simple `try-catch` or sequential execution in the `viewModelScope` is an acceptable pragmatic tradeoff, but the plan doesn't mention error handling.

---

### 🔴 Hard No (Consensus)
- **Hardcoded JSON in ViewModel**: Building `{"entityId": "${task.keyPersonEntityId}"}` natively inside the `SchedulerViewModel` is a hard no. The ViewModel should not know how `MemoryEntry` structures its internal JSON payload. Use a domain extension function (e.g., `Task.toMemoryEntry()`) to encapsulate the data mapping.

### 🟡 Yellow Flags
- **Missing Error Handling / Transaction Risk**: Deleting the task *after* saving the memory without a `try-catch` means if the memory save hangs or throws, the task remains actionable. The plan needs to ensure the UI updates only if the DB operations succeed, or use a resilient sequence.

### 🟢 Good Calls
- **Data De-duplication**: Deleting the `ScheduledTask` entirely stops "ghost" tasks matching in pipeline queries and cleanly enforces the Future (Actionable) vs Past (Factual) divide.
- **L2 Component Test**: Explicitly demanding an `L2CrossOffLifecycleTest` using fakes is exactly how we prevent UI regressions without needing a physical device.

### 💡 Senior's Synthesis & Recommendation
The core concept (move Actionable item to Factual DB on completion) is mathematically sound according to the Actionable vs Factual blueprint. However, the *execution execution point* is sloppy. 

**What I'd Actually Do:**
1. **Domain Extraction**: Create a Kotlin extension function in the `domain/scheduler` or `domain/crm` module: `fun TimelineItemModel.Task.toMemoryEntry(sessionId: String): MemoryEntry`. This hides the ugly JSON construction (`structuredJson`) from the UI layer.
2. **ViewModel Flow**: In `SchedulerViewModel.toggleDone()`:
   ```kotlin
   val memoryEntry = task.toMemoryEntry(sessionId = "SYSTEM_CROSS_OFF")
   // Sequential, predictable execution
   memoryRepository.save(memoryEntry)
   taskRepository.deleteItem(taskId)
   ```
3. **Draft the test FIRST**: The plan states writing `L2CrossOffLifecycleTest.kt`. Write that test immediately and let it drive the implementation of `toMemoryEntry`.

---

### 🔧 Prescribed Tools
1. **Revise the Implementation Plan**: Update `implementation_plan.md` to move the conversion logic into a pure domain extension function `TimelineItemModel.Task.toMemoryEntry()` and update the ViewModel section to call it.
2. **Execute Phase 3**: After revising the plan, write the tests and implementation.
