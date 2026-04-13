# Legacy vs Current Scheduler Logic Differences

> **Context**: Per user's mental model, we are ignoring the voice/talk-back UI and strictly analyzing the **Business Logic** differences of how tasks are handled in the Scheduler Drawer between the Legacy code and the Current Path A code.

## 1. Conflict Detection & Resolution

### Legacy Logic (`RealScheduleBoard` + `PrismOrchestrator`)
- **Detection**: Hardcoded Kotlin overlap check (`proposedStart < slot.endAt && slot.scheduledAt < proposedEnd`). Excludes the current task ID if rescheduling (`excludeId`).
- **Creation Behavior**: If a conflict is detected, it returns `ConflictResult.Conflict(overlaps)`. The Orchestrator **creates the task anyway** (`insertTask`), but sets `hasConflict = anyConflict` (in `MultiTask` flow) or lets the UI handle overlapping rectangles visually. **It does not block insertion.**
- **Visuals**: The Compose UI draws them overlapping or paints a UI warning tag based on the boolean state. No LLM disambiguation is used to "fix" the time automatically.

### Current Path A (`FastTrackMutationEngine` + `scheduler-path-a-execution-prd.md`)
- **Detection**: Exactly the same Kotlin overlap check against `ScheduleBoard`.
- **Creation Behavior**: *"Checks the ScheduleBoard. If a conflict exists, it still creates the task, but attaches a `hasConflict=true`... to trigger the UI Attention Flow."*
- **Visuals**: Renders a **Caution Banner / Red Border** on the timeline card, requiring the user to manually tap it or drag-and-drop.

**🔥 Difference**: Practically **none** on the backend. Both detect conflicts via Kotlin math and both force-insert the task into the database anyway with a flag.

---

## 2. Vague Temporal Intents (Missing Time/Date)

### Legacy Logic (`SchedulerLinter` + `PrismOrchestrator`)
- **Behavior**: If the user omits mandatory fields like `startTime`, the Linter returns `LintResult.Incomplete`. 
- **Database Write**: **BLOCKED**. The Orchestrator does *not* insert anything into Room. It aborts and yields an `AwaitingClarification` state to the presentation layer.

### Current Path A (`FastTrackMutationEngine` + `scheduler-path-a-execution-prd.md`)
- **Behavior**: *"If something is wrong (time conflict, missing date), we still create/update the Timeline Card but flag it... with `isVague = true`"*
- **Database Write**: **INSERTED**. The task is written to Room but left off the main Kanban timeline (placed in "Purgatory" / Red-Flagged Schedulable Card form).

**🔥 Difference**: **MASSIVE.** Legacy *refuses* to write incomplete data to the DB. Current Path A *mandates* writing incomplete data to the DB to ensure "zero black holes" and instantaneous UI feedback (assertive degradation).

---

## 3. Modifying Specific Cards (Global Reschedule)

### Legacy Logic (`PrismOrchestrator` Wave 11)
- **Matching**: Lexical search against active board (`title.contains(targetTitle)`).
- **Execution Flow**: If exactly 1 match is found, it recursively triggers a **second LLM pass**: `createScheduledTask(lintResult.newInstruction, replaceItemId = match.entryId)`. 
- **Mutation Pattern**: It creates a brand new Task with a brand new ID, deletes the old `replaceItemId`, and refreshes the board.

### Current Path A (`FastTrackMutationEngine` + `scheduler-path-a-execution-prd.md`)
- **Matching**: Lexical search (`findLexicalMatch`) using same filtering logic.
- **Execution Flow**: **Single LLM pass.** The LLM generates the absolute `newStartTimeIso` upfront based on the initial query without seeing the old DB task.
- **Mutation Pattern**: It performs an atomic *Upsert* (Update the old record in place OR Delete->Insert while **forcefully inheriting the original GUID**). 

**🔥 Difference**: **MASSIVE.** Legacy uses a dual-LLM pass and mints new IDs, destroying references. Current Path A uses a single LLM pass and enforces strict **ID Conservation** to preserve Entity/CRM links.

---

## 4. Deletions

### Legacy Logic (`PrismOrchestrator` Wave 7)
- **Behavior**: Supported via voice. `LintResult.Deletion` finds the string match via `contains`. If 1 match is found: `scheduledTaskRepository.deleteItem(match.entryId)`. The task is permanently destroyed.

### Current Path A (`scheduler-path-a-execution-prd.md`)
- **Behavior**: Hard disabled. PRD states: *"Global voice deletions... are intentionally disabled to prevent premature UX failures"*

**🔥 Difference**: Legacy allowed deleting DB records via voice match. Current Path A bans it as too risky.

---

## 💡 Review Recommendation

To adopt the Legacy Business Logic within the Current Protocols, we must decide which constraints to keep.

@[/00-review-conference-[tool]] Please convene @[/01-senior-reviewr-[persona]] to review these differences and mandate the final Migration execution spec based on the user's intent to "reroll smartly to the old functioning version".
