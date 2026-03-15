# 🚀 PRD: Scheduler Fast-Track (Path A Execution)

> **Context**: This is the North Star PRD for decoupling the Scheduler capabilities away from the monolithic pipeline and migrating them into the System III Dual-Path Architecture.
> **Scope**: Path A (The Town) - Optimistic UI Execution.

## 1. Core Philosophy

The Scheduler Path A is designed for **Sub-second Optimistic Execution** with **Assertive Degradation**.
- **Dedicated Mutation Engine**: A single, isolated entity (e.g., the plugin itself, or an injected `TaskMutationHandler`) owns all atomic operations (Single, Batch, Reschedule, Delete).
- **The "Small Attention Flow"**: We never block the pipeline. If something is wrong (time conflict, missing date), we still create/update the Timeline Card but flag it in the DB/UI with a `NeedsClarification` state. We use native Android mechanics (sounds, banners) to demand user attention.

## 2. Component Responsibility

### A. The Dedicated Mutation Module
A pure Kotlin domain module responsible for executing the One-Currency LLM intents.
- **Batch Create**: Handles arrays of tasks atomically.
- **Single Reschedule**: Enforces the atomic `Insert New -> Delete Old` transaction to prevent data loss. *(Note: Global voice deletions and batch rescheduling are intentionally disabled to prevent premature UX failures).*
- **Conflict Evaluation**: Checks the `ScheduleBoard`. If a conflict exists, it **still creates the task**, but attaches a `hasConflict=true` or similar flag to trigger the UI Attention Flow.

### B. The User Experience (The Small Attention Flow)
When Path A completes an execution, the UI reacts based on the entity state.

| Scenario | Background Action | UI / Visual Feedback |
|----------|-------------------|----------------------|
| **Perfect Creation** | `ScheduledTask` inserted normally. | Timeline updates. System emits a soft "Success" sound. `isReschedule=true` triggers Amber Glow. |
| **Missing Date (Temporal Vague)** | User says "明天开会" but skips the time. | `CreateTasksParams` fires, but NLP omits `startTimeIso`. Timeline inserts a **Red-Flagged Schedulable Card**. Native Android Pop-up/Toast fires: *"请问明天几点？"* (What time tomorrow?). |
| **Timeless Query (Inspiration)** | User says "早上好" or "以后想学吉他". | Evaluates as `Inspiration`. Saved to `InspirationRepository` (not the main schedule). UI renders a distinct Inspiration Note card (not a schedulable task block). |
| **Time Conflict** | Mutation Module detects Kanban overlap. | Task inserted. Timeline renders task with **Caution Banner / Red Border**. UI requires user interaction to accept or manually drag-and-drop. |
| **Path B Vague Name (Future)** | Path B resolves "李总" to 3 different IDs. | Path B updates the existing Card state. Card turns Red-Flagged. User taps card to open inline resolution picker. |

## 3. The Execution Flow

```text
[Badge / App Rec Button] "把会推迟到跟他重叠的时间"
      │ 
      ▼
┌───────────────┐
│ FastTrack     │───► Fast NLP parses JSON: `RescheduleParams(target="会", time="[ConflictTime]")`
│ Parser        │
└───────┬───────┘
        ▼
┌───────────────┐
│ Dedicated     │───► 1. Lexical search finds original task.
│ Mutation      │───► 2. Detects time overlap on ScheduleBoard.
│ Module        │───► 3. STILL CREATES the new task, but sets `conflict_flag = true`.
└───────┬───────┘───► 4. Deletes old task.
        ▼
┌───────────────┐
│ Timeline UI   │───► DB emits new task layer.
│ (Compose)     │───► UI detects `conflict_flag`.
└───────┬───────┘───► Renders RED CAUTION CARD.
        ▼
┌───────────────┐
│ Native OS     │───► Triggers error/alert Sound.
│ Triggers      │───► Displays Native Banner/Popup: "⚠️ 该时间段已有安排，请确认"
└───────────────┘
        ▼
   [ User Taps Card ] ──► Opens Small Attention Flow (Inline resolution/edit).
```

## 4. Global Task Management & Lexical Matching Flows

The true power of Path A is **Global Voice Execution**. The user does not need to explicitly open a specific card or context to modify it. The user can press the physical Badge (or the Rec button) from anywhere in the OS and say "把下午的开会推迟两小时".

### A. The Lexical Fuzzy Match Protocol (Anti-Hallucination)
Path A fundamentally distrusts LLM-generated IDs for global targeting. When executing a `Reschedule` intent, the LLM emits a `targetTaskQuery` (e.g., "下午的开会"). The Dedicated Mutation Module executes a deterministic Kotlin lexical search against the `ScheduleBoard.upcomingItems` (which explicitly filters out completed/expired tasks).

**GUID Inheritance Rule**: During a Reschedule (which is an atomic Create + Delete), the newly inserted task MUST forcefully inherit the exact `id` (the code-generated GUID) of the original matched task. This ensures any downstream links (like Path B CRM bindings) remain unbroken.

**Match Evaluation Rules:**
- **0 Matches**: Abort operations. System emits Toast/Voice prompt: *"⚠️ 未找到匹配的日程，请更具体一些。"* (Prevents hallucinated modifications).
- **1 Exact Match**: Happy Path. The transaction (Create + Delete under the hood) proceeds atomically.
- **2+ Matches (Ambiguity)**: Abort operations. System emits Toast/Voice prompt: *"⚠️ 找到多个匹配的日程，请进入日程面板手动调整。"* (Prevents destroying the wrong task).

### B. Auto-Expiry and Scope Limiting
To ensure the Lexical Fuzzy Matcher remains lightning fast and accurate, the active search pool (`ScheduleBoard.upcomingItems`) must be aggressively pruned.
- **Manual Completion**: User taps the checkbox on the UI. `isDone = true`. The task is immediately filtered out of the active voice scope.
- **Auto-Expiry Sweep**: The `SchedulerViewModel` initializes a sweep of past-due tasks (where `startTime + duration` < Now) on load, implicitly migrating them to historical memory and immediately removing them from the global voice scope.

## 5. The Data Contracts (The "One Currency" Schema)

The FastTrack Parser must output these exact explicit DTOs. 

### Tool 1: `CreateTasksParams`
Handles single and multi-task scheduling.

```kotlin
@Serializable
data class CreateTasksParams(
    val unifiedId: String? = null,      // Sourced from ASR Orchestrator
    val tasks: List<TaskDefinition>
)

@Serializable
data class TaskDefinition(
    val title: String,
    val startTimeIso: String,           // Enforce absolute ISO-8601 strings (e.g. "2026-03-16T14:00:00Z")
    val durationMinutes: Int, 
    val urgency: UrgencyEnum            // L1_CRITICAL, L2_IMPORTANT, L3_NORMAL, FIRE_OFF
)
```

### Tool 2: `RescheduleTaskParams`
Evaluates fuzzy matches against the active timeline. Handles Date shifts natively because `newStartTimeIso` is an absolute anchor.

```kotlin
@Serializable
data class RescheduleTaskParams(
    val unifiedId: String? = null,
    val targetTaskQuery: String,        // Lexical fuzzy search term (e.g., "两点的开会")
    val newStartTimeIso: String,        // The absolute new ISO-8601 target
    val newDurationMinutes: Int? = null 
)
```

### Tool 3: `CreateInspirationParams` (Timeless Intent)
A separate tool strictly for timeless queries, aspirations, or general notes that don't belong on a strict ISO-bounded timeline layout.

```kotlin
@Serializable
data class CreateInspirationParams(
    val unifiedId: String? = null,
    val content: String                 // The raw timeless note (e.g., "想学吉他")
)
```

## 6. Why This Wins (Architectural Defense)
1. **Zero Black Holes**: The user always gets visual proof their audio was heard. Ghosting is entirely eliminated.
2. **Re-usability**: By cementing the "Red-Flagged Card" pattern for Path A conflicts now, Path B can perfectly recycle this exact same UI component to solve CRM disambiguation later. It unifies the error-handling experience across the entire App.
3. **Decoupled Logic**: The UI doesn't evaluate conflicts. The UI just renders whatever the Dedicated Mutation Module saves to the SSD.
4. **Resilient Matching**: Lexical match limits LLM hallucination scope. If the LLM gets confused, the rigid Kotlin scope checks prevent collateral data damage.
