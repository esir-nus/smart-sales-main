# 📋 Review Conference Report: Actionable vs Factual Data Class Unification

**Subject**: Architecture Proposal — Should the `ClientProfileHub` unify `Actionable` and `Factual` data by directly surfacing the `ScheduledTask` data class instead of mapping it to a duplicate CRM activity?
**Panel**: 
1. `/01-senior-reviewr` (Chair, System Design & Pragmatism)
2. `/17-lattice-review` (Data Contracts & OS Isolation)

---

### Panel Input Summary

#### 🤖 Lattice / OS Auditor — Data Contracts
- **Key insight 1**: Exposing `ScheduledTask` to the `ClientProfileHub` does not violate OS layer isolation, because `ClientProfileHub` is explicitly documented as a Layer 5 Intelligence boundary that aggregates across standard Layer 4 features.
- **Key insight 2**: Creating artificial mapping classes (e.g., `CrmActionableItem` -> `ScheduledTask`) creates hidden translation debt. The Single Source of Truth architecture demands that the data representation matches the domain that owns the data.

#### 🧔 Senior Engineer — System Design
- **Key insight 1**: "They conceptually align but the code doesn't translate for guaranteed" — User Frank. This is the exact definition of "Translation Drift." The User's instinct here is 100% correct.
- **Key insight 2**: The UI developer shouldn't have to build a "CRM Task Card" and a "Scheduler Task Card." By surfacing the exact `TimelineItemModel.Task` data class, the Compose UI can literally just render the existing `@Composable SchedulerTaskCard(task)`. It saves hundreds of lines of duplicate UI code.

---

### 🔴 Hard No (Consensus)
**Translation Layers.** Do not create a new `UnifiedActivity.Actionable` class just to make it "fit" the CRM profile visually. Mapping `TimelineItemModel.Task` -> `UnifiedActivity` every time the Flow emits just to throw away data (like the `urgency` or the `alarmCascade`) actively harms the UI's ability to render the real context of the task.

### 🟡 Yellow Flags
**Polymorphic UI State.** When you mix `Factual` (which includes pure Memories and Notes) and `Actionable` (Tasks), the Compose `LazyColumn` needs to handle the different data types cleanly. You cannot force them into a single `List<Any>`.

### 🟢 Good Calls
**The User's Final Proposal.** "Unifying the data class so the CRM uses `schedule` as well." This is the pinnacle of the "Data-Oriented OS." If it is a Task, it mathematically IS a Task in RAM.

---

### 💡 Senior's Synthesis (What I'd Actually Do)

**Adopt Raw Domain Surfacing globally.**

Here is the exact mathematically proven data contract for the `ClientProfileHub` moving forward:

```kotlin
data class ProfileActivityState(
    // The Actionable feed is pure, unmodified Scheduler Tasks. 
    // They bring all their alarms, deadlines, and UI colors natively.
    val actionableItems: List<com.smartsales.prism.domain.scheduler.TimelineItemModel.Task>,
    
    // The Factual feed is pure, unmodified Memory Entries. 
    // *When a Task crosses off, it is converted to a MemoryEntry via .toMemory() and stored permanently.*
    val factualItems: List<com.smartsales.prism.domain.memory.MemoryEntry>
)
```

**The Transformation Lifecycle (The "Cross Off"):**
When a Task is checked off (`isDone = true` or `endTime < now`):
1. The `SchedulerViewModel` triggers normal task completion.
2. The `ClientProfileHub` detects it is finished.
3. It emits a ONE-TIME signal to the `MemoryRepository` to write a `MemoryEntry` (Summary: "Completed Task: Call Bob").
4. The Task is deleted from the `ScheduledTaskRepository` entirely (it has served its purpose, it is dead).
5. The UI dynamically shifts the item from the top list (`actionableItems`) into the bottom list (`factualItems`).

This is the ultimate evolution of Project Mono. Data isn't mapped; it physically moves across domains as it ages from the Future (Scheduler) to the Past (Memory).

---

### 🔧 Prescribed Tools
Based on this review, run these next:
1. Update `docs/reports/review_conferences/20260314_actionable_factual_plan.md` to establish this Data Class Unification.
2. Update `docs/cerb/client-profile-hub/spec.md` with the new finalized Domain Models.
