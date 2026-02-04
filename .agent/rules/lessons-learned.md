---
description: Accumulated learnings from debugging sessions - DO NOT REPEAT THESE MISTAKES
trigger: always_on
---

# Agent Lessons Learned

> **Growing Document**: Append new learnings here after USER confirms fix.  
> **Purpose**: Prevent agents from repeating the same mistakes.  
> **Update Rule**: Only add/update entries when **USER explicitly says "problem fixed"**.  
> ⚠️ **BUILD SUCCESSFUL ≠ Problem Fixed.** Wait for user confirmation.

---

## Entry Format

When adding a new lesson (after USER confirms "problem fixed"):
```markdown
### [SHORT TITLE] — [DATE]

**Symptom**: What the user reported  
**Root Cause**: The actual problem  
**Wrong Approach**: What didn't work  
**Correct Fix**: What worked  
**File(s)**: Where the fix was applied
```

---

## Lessons

### Compose Scrim Inside AnimatedVisibility — 2026-02-02

**Symptom**: Grey screen covers drawer, clicking anywhere dismisses  
**Root Cause**: **Double scrim conflict** — PrismShell has global scrim at `zIndex(Scrim)`, but SchedulerDrawer had its own internal scrim AND was not wrapped in `zIndex(Drawer)`.  
**Quick Fix**: 1) Remove internal scrim from drawer (rely on PrismShell global scrim), 2) Wrap drawer call in `Box(zIndex(Drawer))`.  
**Deep Dive**: [compose-scrim-drawer-pattern.md](file:///home/cslh-frank/main_app/.agent/rules/compose-scrim-drawer-pattern.md)  
**Status**: ✅ CONFIRMED 2026-02-02

---

### UI Element Not Appearing → Check Pipeline Logs — 2026-02-02

**Symptom**: Scheduler card not appearing in timeline after tapping simulation button (toast appeared ✓, but no task card)  
**Diagnostic**: `adb logcat -s SchedulerVM:D` → revealed `Pipeline result: Error(message=JSON 解析失败...)`  
**Root Cause**: Pipeline silently returning `UiState.Error` — UI correctly showed nothing because there was no data.  
**Key Insight**: **When UI doesn't render expected elements, the bug is often upstream in the data pipeline, not the UI itself.**  
**Correct Fix**: Added `buildSchedulerSystemPrompt()` with explicit JSON schema.  
**File(s)**: [DashscopeExecutor.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/data/real/DashscopeExecutor.kt#L227-L268)  
**Pattern**: Always instrument ViewModel with tagged logs (`Log.d("SchedulerVM", "Pipeline result: $result")`) to trace data flow.  
**Status**: ✅ CONFIRMED 2026-02-02

---

### Sealed Class Data Gap — 2026-02-03

**Symptom**: Scheduler task created on wrong date (always "tomorrow" instead of parsed date like "next Friday")  
**Root Cause**: **Parsed data exists upstream but sealed class doesn't propagate it**  
- `LintResult.Success.task.startTime` had correct parsed date
- `UiState.Response(content: String)` only passed title string
- `SchedulerViewModel` hardcoded `dayOffset = 1` instead of using actual value  
**Diagnostic Pattern**: Log showed `structuredJson=null` — missing data, not wrong data  
**Correct Fix**:  
1. Add new sealed class variant: `UiState.SchedulerTaskCreated(title, dayOffset)`
2. Producer calculates: `ChronoUnit.DAYS.between(today, taskDate)`
3. Consumer uses: `result.dayOffset` instead of hardcoded value
4. Don't forget exhaustive `when` handling in all consumers  
**File(s)**:  
- [UiState.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/domain/model/UiState.kt)
- [PrismOrchestrator.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/data/real/PrismOrchestrator.kt)
- [SchedulerViewModel.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt)  
**Pattern**: When adding fields to sealed class, grep for all `when` expressions that match on it  
**Status**: ✅ CONFIRMED 2026-02-03

---

### Post-Insert Self-Conflict — 2026-02-03

**Symptom**: Every new task triggers conflict warning, even with empty schedule  
**Root Cause**: **refresh() → new item in index → checkConflict() finds itself**  
- After `insertTask()`, we call `refresh()` which adds the new task to `ScheduleBoard`
- `checkConflict(startTime, duration)` then finds the task we just inserted → false positive  
**Wrong Approach**: Checking conflict AFTER insert without exclusion  
**Correct Fix**:  
1. Add `excludeId: String?` parameter to `checkConflict()`
2. Add `taskId: String` field to `UiState.SchedulerTaskCreated`
3. Pass `excludeId = result.taskId` in conflict check  
**File(s)**:  
- [ScheduleBoard.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/domain/memory/ScheduleBoard.kt)
- [RealScheduleBoard.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/data/memory/RealScheduleBoard.kt)
- [UiState.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/domain/model/UiState.kt)
- [SchedulerViewModel.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt)  
**Pattern**: Any "check for duplicates/conflicts after insert" needs **exclusion logic** for the just-inserted entity  
**Status**: ✅ CONFIRMED 2026-02-03

### Spec Drift: Inventing UI Features — 2026-02-03

**Symptom**: Implementation plan included "Swipe Right to Reschedule" gesture  
**Root Cause**: Agent saw a gap (how to trigger reschedule?) and **invented** a UI pattern without spec backing  
**Wrong Approach**: Making up features to fill perceived gaps  
**Correct Fix**:  
1. Check if spec covers the interaction
2. If not covered, **flag as spec gap** — don't invent
3. Ask user: "Spec doesn't define how to trigger reschedule. Should I propose a spec update?"  
**Pattern**: **Docs > Code > Guessing** — Never invent features, always verify against spec  
**Status**: ✅ CONFIRMED 2026-02-03

---

### Spec Invention from Wave Titles — 2026-02-03

**Symptom**: Wave 3 implementation plan prescribed Kotlin location conflict logic that user rejected  
**Root Cause**: **Quoted wave TITLE as if it were behavior SPEC**  
- Tracker said "Extend overlap logic to shared resources (Meeting Rooms)"
- I invented string equality matching conflict detection
- I assumed "location conflict" meant blocking like time conflicts  
**Wrong Approach**: 
- Treating roadmap milestones as implementation specs
- Prescribing business logic when spec was silent
- Assuming semantic matching could be done in Kotlin  
**Correct Fix**:
1. Wave titles/milestones are NOT behavior specs
2. If only a title exists → spec doesn't exist for this behavior
3. Ask USER before implementing unspecified behavior
4. Let RelevancyLib/LLM decide semantic questions, don't prescribe in Kotlin  
**File(s)**: `.agent/workflows/feature-dev-planner-[tool].md` (added Anti-Invention Gate)  
**Pattern**: **Faithfully pull relevant info. Don't prescribe, don't assume.**  
**Status**: ✅ CONFIRMED 2026-02-03

---

### Dead Flow Reference in flatMapLatest — 2026-02-04

**Symptom**: UI requires manual refresh (navigate away/back) to see changes after insert/update  
**Root Cause**: **SharedFlow referenced but not combined** — line exists but has no effect  
```kotlin
// BROKEN: This line does nothing
flatMapLatest { offset ->
    _refreshTrigger.asSharedFlow()  // ← DEAD CODE
    taskRepository.getTimelineItems(offset)
}
```
**Why It's Subtle**: Code compiles, builds pass, no runtime errors. Only visible in L2 testing.  
**Correct Fix**: Use `combine()` to actually merge the flows:  
```kotlin
combine(_activeDayOffset, _refreshTrigger.asSharedFlow()) { offset, _ -> offset }
    .flatMapLatest { offset -> taskRepository.getTimelineItems(offset) }
```
**File(s)**: [SchedulerViewModel.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt)  
**Pattern**: In reactive chains, a Flow reference on its own line **does nothing**. Must be combined/collected.  
**Heuristic**: If you see a Flow/SharedFlow reference that isn't assigned, collected, or combined → **DEAD CODE**.  
**Status**: ⏳ AWAITING L2 CONFIRMATION

---

### Ghost UI After Update ≠ Persistence Bug — 2026-02-04

**Symptom**: Old card persists after reschedule; looks like persistence is broken  
**Initial Hypothesis**: `updateEvent()` not working, CalendarProvider issue  
**Actual Root Cause**: **UI stale, not data stale** — persistence was fine, UI just didn't refresh  
**Diagnostic Pattern**: Don't assume persistence bug first. Always verify:  
1. Check logs: Did update actually go through?  
2. Query DB directly: Is old data still there?  
3. Force refresh: Does UI update after?  
**Correlation**: Ghost card symptom **disappeared after fixing refresh trigger** (see above)  
**Pattern**: When UI shows stale data, check refresh flow BEFORE debugging persistence layer  
**Status**: ⏳ AWAITING L2 CONFIRMATION

---

### LLM Prompt-Linter Data Gap — 2026-02-04

**Symptom**: Inspiration cards show only 💡 emoji, no title text visible  
**Root Cause**: **Prompt told LLM to omit content for inspiration classification**  
- DashscopeExecutor L263: `如果是 "inspiration"，只需返回 classification 字段，tasks 可省略`
- LLM returns `{"classification": "inspiration"}` with NO title/content field
- SchedulerLinter extracts `json.optString("title", "")` → empty string
- InspirationRepository stores empty string → UI renders empty text  
**Wrong Approach**: Assuming LLM would include the inspiration text automatically  
**Correct Fix**:  
1. Update prompt to **require** `inspirationText` field for inspiration classification
2. Add example: `{"classification": "inspiration", "inspirationText": "以后想学吉他"}`
3. Update Linter to read `inspirationText` with fallback chain  
**File(s)**:  
- [DashscopeExecutor.kt L262-274](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/data/real/DashscopeExecutor.kt#L262-L274)
- [SchedulerLinter.kt L37-40](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt#L37-L40)  
**Pattern**: **When LLM classifies + extracts data, ensure prompt REQUIRES all fields that downstream code expects.** Don't rely on optional fields.  
**Heuristic**: If Linter extracts field X from LLM JSON, grep prompt for requirement of field X.  
**Status**: ⏳ AWAITING L2 CONFIRMATION

---

<!-- Add new lessons above this line -->

### SwipeToDismiss Background Visibility — 2026-02-02

**Symptom**: Red delete background persists after swipe, requires navigating away to clear  
**Root Cause**: Used `dismissDirection != null` which is always true inside `backgroundContent` lambda  
**Wrong Approach**: `dismissState.progress > 0f` — also unreliable  
**Correct Fix**: Use `dismissState.targetValue != SwipeToDismissBoxValue.Settled`  
**File(s)**: [SchedulerTimeline.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerTimeline.kt)  
**Pattern**: Inside SwipeToDismissBox lambdas, check `targetValue` state, not `dismissDirection`  
**Status**: ✅ CONFIRMED 2026-02-02

---

### DateRange Format Mismatch — 2026-02-02

**Symptom**: Tasks saved with wrong time (defaulted to "now" instead of scheduled time)  
**Root Cause**: Linter outputs `"2026-02-03 03:00 - ..."` but `parseDateRange()` expected `~` delimiter with 2 parts  
**Correct Fix**: Handle 3 formats: `~` (full range), `- ...` (open-ended), ` - ` (same-day)  
**File(s)**: [RealScheduledTaskRepository.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/data/scheduler/RealScheduledTaskRepository.kt)  
**Pattern**: When parsing formats between layers, test with ALL format variants the upstream can produce  
**Status**: ✅ CONFIRMED 2026-02-02

---

### Modal Drawer Click Passthrough — 2026-02-02

**Symptom**: Tapping empty area in drawer dismisses it (clicks pass through to scrim)  
**Root Cause**: Drawer content Column doesn't consume pointer events  
**Correct Fix**: Add `pointerInput(Unit) { awaitPointerEventScope { while(true) { awaitPointerEvent() } } }`  
**File(s)**: [SchedulerDrawer.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt)  
**Pattern**: Modal content must explicitly consume events to prevent passthrough  
**Reference**: [compose-scrim-drawer-pattern.md](file:///home/cslh-frank/main_app/.agent/rules/compose-scrim-drawer-pattern.md)  
**Status**: ✅ CONFIRMED 2026-02-02

---

### Calendar Selected vs Today State — 2026-02-02

**Symptom**: Tapped date has no visual highlight; only "today" is highlighted  
**Root Cause**: Only tracked `isToday`, never passed `selectedDayOfMonth` down to CalendarRow  
**Correct Fix**: 
- Calculate `selectedDayOfMonth = todayDayOfMonth + activeDay`  
- Pass to CalendarRow, apply border ring for selected (not today)  
- Visual: Today = filled circle, Selected = border ring  
**File(s)**: [SchedulerCalendar.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerCalendar.kt)  
**Pattern**: Date pickers need 3 visual states: normal, today, selected (and selected+today)  
**Status**: ✅ CONFIRMED 2026-02-02

---
