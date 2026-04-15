# 🧔 Senior Engineer Review: Reintegration Plan (Actionable vs Factual)

**Context**: We need to implement the "Raw Domain Surfacing" architecture for the `ClientProfileHub`. A "one-go" PR here is highly dangerous because it spans Domain Contracts (interfaces), Data Mappers (Fakes), and UX logic (Flow combinations). 

Here is the strictly phased, anti-hallucination reintegration plan. We will not move to the next phase until the previous one is mechanically proven to compile and pass tests.

---

### 🧱 Phase 1: The Domain Contract Purge (Interface Only)
**Goal:** Rip out the legacy `UnifiedActivity` data classes and update the interfaces so the compiler forces us to find all the loose ends.
1. **Target**: `domain/crm/src/main/java/com/smartsales/prism/domain/crm/ClientProfileModels.kt`
   - **Action**: Delete `UnifiedActivity` and `ActivityType`.
   - **Action**: Add `ProfileActivityState` data class (taking `TimelineItemModel.Task` and `MemoryEntry`).
   - **Action**: Update `EntitySnapshot`, `FocusedContext`, and `QuickContext` to use `MemoryEntry` instead of `UnifiedActivity`.
2. **Target**: `domain/crm/src/main/java/com/smartsales/prism/domain/crm/ClientProfileHub.kt`
   - **Action**: Change `getUnifiedTimeline` to `observeProfileActivityState(entityId): Flow<ProfileActivityState>`.
3. **Target**: `app-core/src/main/java/com/smartsales/prism/data/fakes/FakeClientProfileHub.kt`
   - **Action**: Delete the `.toUnifiedActivity()` mapping logic.
   - **Action**: Fix compilation errors by stubbing `observeProfileActivityState` with a basic `flowOf()` or `MutableStateFlow` that returns the pure `ProfileActivityState`.
4. **Verification**: 
   - Run `./gradlew :domain:crm:assemble`
   - Run `./gradlew :app-core:assembleDebug`
   - **Gate**: Zero compiler errors. We prove the contract is sound before writing implementation logic.

---

### ⚙️ Phase 2: The Reactive Engine (`RealClientProfileHub`)
**Goal:** Build the real SQLite-backed aggregator that combines the Scheduler and Memory domains in real-time.
1. **Target**: `app-core/src/main/java/com/smartsales/prism/data/crm/RealClientProfileHub.kt` (Create if missing)
   - **Action**: Inject `ScheduledTaskRepository` and `MemoryRepository`.
   - **Action**: Implement `observeProfileActivityState()` using `combine(tasksFlow, memoryFlow)`.
   - **Action**: Implement the time-based classification: `actionableTasks` = `!isDone && endTime > now`.
2. **Target**: `domain/crm/src/test/java/com/smartsales/prism/domain/crm/RealClientProfileHubTest.kt`
   - **Action**: Write an L1 unit test using `FakeScheduledTaskRepository` and `FakeMemoryRepository`.
   - **Action**: Assert that emitting an overdue task automatically shifts it from the actionable list to the factual list in the combined output.
3. **Verification**:
   - Run `./gradlew :domain:crm:testDebugUnitTest --tests *RealClientProfileHubTest*`
   - **Gate**: Tests pass, proving the logic correctly sorts raw domains without UI-layer mapping.

---

### 🔄 Phase 3: The Cross-Off Lifecycle (Data Migration)
**Goal:** Implement the physical movement of data when a task is completed. A finished task is permanently converted to a `MemoryEntry` and deleted from the `ScheduledTaskRepository`.
1. **Target**: `SchedulerViewModel` (or a dedicated `TaskObserver`)
   - **Action**: When `toggleDone(taskId)` is triggered, instead of just marking it done, explicitly call `MemoryRepository.save(completedTask.toMemory())` and then `ScheduledTaskRepository.delete(taskId)`.
2. **Verification**:
   - **Gate**: Run an L2 Simulation Test to prove that crossing off a task removes it from the actionable feed and immediately surfaces it in the factual feed via the `ClientProfileHub`.

---

### Next Steps
If you approve this structured approach, I will execute **Phase 1: The Domain Contract Purge** immediately. This will intentionally break the build until I update the `FakeClientProfileHub`, forcing me to respect the compiler boundaries. 

Shall I proceed with Phase 1?
