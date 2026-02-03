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
