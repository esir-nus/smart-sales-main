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
**Status**: ✅ CONFIRMED 2026-02-05

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
**Status**: ✅ CONFIRMED 2026-02-05

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
**Status**: ✅ CONFIRMED 2026-02-05

---

### Reconnect Race Condition: Fire-and-Poll — 2026-02-07

**Symptom**: `reconnect()` returns `DeviceNotFound` despite badge being powered on and nearby  
**Root Cause**: **Fire-and-poll pattern with insufficient delay** — `forceReconnectNow()` + `delay(1500ms)` + poll `state.value`. BLE GATT timeout is ~10s, so 1.5s was always too early → state was still `AutoReconnecting` → mapped to `DeviceNotFound`.  
**Wrong Approach**: Increasing the delay (fragile), polling in a loop (wasteful)  
**Correct Fix**: Replace fire-and-poll with `suspend fun reconnectAndWait()` that directly calls `connectUsingSession()` and returns the actual outcome. No delay, no polling.  
**File(s)**:  
- [DeviceConnectionManager.kt](file:///home/cslh-frank/main_app/feature/connectivity/src/main/java/com/smartsales/feature/connectivity/DeviceConnectionManager.kt) — new `reconnectAndWait()` 
- [RealConnectivityService.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityService.kt) — calls `reconnectAndWait()`  
**Pattern**: **Never fire-and-poll async operations with fixed delays.** Use suspend functions that return actual results. If you see `fire() → delay(N) → poll`, it's a race condition waiting to happen.  
**Status**: ✅ CONFIRMED 2026-02-07

---

### HTTP Gate Conflating Connection Concerns — 2026-02-07

**Symptom**: Reconnect returns `EndpointUnreachable("设备服务不可达")` even though BLE connects and badge reports valid IP  
**Root Cause**: **`connectUsingSession()` gates on HTTP reachability** — it does BLE GATT → query WiFi → get IP → HTTP HEAD to `:8088`. If HTTP server isn't running (firmware state), reconnect fails. But HTTP is only needed for WAV file operations, not for BLE connectivity.  
**Wrong Approach**: Treating HTTP unreachable as "badge not connected"  
**Correct Fix**: Remove HTTP gate from `connectUsingSession()` — check only for valid IP (not `0.0.0.0`). HTTP check stays in `ConnectivityBridge.isReady()` which is the spec-defined pre-flight for media operations.  
**Key Insight**: **The spec already had the right separation** — `isReady()` was designed for BLE+HTTP pre-flight. The HTTP check was in the wrong layer.  
**Diagnostic**: `adb logcat | grep "RX \[NetworkResponse\]"` → `IP#192.168.0.106` proved BLE worked, HTTP was the sole blocker.  
**File(s)**:  
- [DeviceConnectionManager.kt L366-384](file:///home/cslh-frank/main_app/feature/connectivity/src/main/java/com/smartsales/feature/connectivity/DeviceConnectionManager.kt#L366-L384)  
**Pattern**: **Don't gate connection state on downstream service availability.** BLE connected ≠ HTTP server running. Separate "can I talk to the device?" from "can I download files?"  
**Heuristic**: If a function gates on multiple protocols (BLE + HTTP), ask: *"Do ALL callers need ALL these checks?"* If not, the function is conflating concerns.  
**Status**: ✅ CONFIRMED 2026-02-07

---

### LLM Fabricates History When Memory Is Empty — 2026-02-08

**Symptom**: Coach mode invents specific dates, quotes, and past conversations that never happened. When challenged ("你确定?"), doubles down with more fabricated details.  
**Root Cause**: **System prompt had no anti-hallucination instruction + empty memory context provided no grounding.** LLM fills the vacuum with plausible fiction.  
- `buildCoachSystemPrompt()` said "you're a sales coach" but never said "don't make up history"
- `buildPrompt()` logged `no memory hits` but didn't inject that fact into the LLM prompt
- With zero `memoryHits`, the LLM had no guardrails against fabrication  
**Wrong Approach**: Assuming the LLM would naturally say "I don't have records" when context is empty  
**Correct Fix**: Two-layer defense:
1. System prompt: Add explicit rule `绝不编造历史 — 不要引用或捏造任何以前的对话内容`
2. Runtime: When `memoryHits.isEmpty()`, inject `注意: 当前没有任何历史对话记录` into the prompt body  
**File(s)**: [DashscopeExecutor.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/data/real/DashscopeExecutor.kt) — L193 (runtime), L232 (Coach), L255 (Analyst)  
**Pattern**: **Every LLM system prompt must include anti-hallucination rules for empty context.** Don't assume the model will be honest about what it doesn't know.  
**Heuristic**: When adding a new LLM mode/pipeline, ask: "What happens when context is empty?" If the answer is "LLM freestyles" → add guardrail.  
**Status**: ⏳ PENDING L3 — 2026-02-08

---

### Fake→Real Swap Removes Implicit Behavior — 2026-02-08

**Symptom**: Same as above — Coach worked fine with Fakes but hallucinated after Room swap  
**Root Cause**: **FakeMemoryRepository had seed data. RoomMemoryRepository starts empty.** The Fake's seed data was an implicit guardrail — the LLM always had *some* context to reference, so it never needed to fabricate. When swapped to Real (empty DB), the LLM had zero grounding data.  
**Deeper Insight**: Fakes don't just provide test data — they implicitly define behavior boundaries. When you remove a Fake, you're also removing whatever implicit behavior its data provided.  
**Correct Fix**: When swapping Fake→Real, audit:
1. What seed data did the Fake provide?
2. Does any downstream consumer assume data is always present?
3. What happens when the Real implementation returns empty?  
**Pattern**: **Fake→Real swap checklist:**
- [ ] What does the consumer do with empty results?
- [ ] Does the LLM behave differently with empty vs populated context?
- [ ] Are there UI states for "no data"?  
**File(s)**: Applies broadly — any `Fake*Repository` → `Room*Repository` swap  
**Status**: ⏳ PENDING L3 — 2026-02-08

---

### Entity-Domain Mapping Gap (Computed Fields) — 2026-02-09

**Symptom**: Scheduler card expanded view showed blank date range, despite UI code and data model supporting it.
**Root Cause**: **Computed field `dateRange` was hardcoded to `""` in `ScheduledTaskEntity.toDomain()`**. 
- Entity doesn't store computed fields (good), but Mapper failed to reconstruct them (bad).
- Pipeline layers (LLM, Linter, UI) were correct; only the persistence mapper dropped the data.
**Wrong Approach**: Assuming UI bug or LLM extraction failure without checking the mapper.
**Correct Fix**: Reconstruct `dateRange` in `toDomain()` using stored timestamps, duplicating the formatting logic from `SchedulerLinter`.
**File(s)**: [ScheduledTaskEntity.kt](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/data/persistence/ScheduledTaskEntity.kt)
**Pattern**: When a specific field is missing in UI but present in data model, **check the Mapper** (`toDomain`/`toUiState`) first.
**Status**: ✅ CONFIRMED 2026-02-09

### Application-Level Coupling vs Transport-Level Serialization — 2026-02-09

**Symptom**: `tim#get` auto-response interfered with WiFi provisioning writes → `写入特征失败`  
**Root Cause**: Two coroutines calling `GattContext.writeCharacteristic()` concurrently on the same BLE characteristic. Android BLE rejects concurrent GATT writes.  
**Wrong Approach (3 iterations)**:
1. `sessionLock.tryLock()` in `respondToTimeSync()` — failed (timing gaps between coroutine dispatches)
2. `AtomicBoolean` flag `isExecutingOperation` — worked but over-coupled (application logic gating a transport concern)
3. `AtomicBoolean` + deferred `pendingTimeSync` flush — worked but even MORE over-coupled (added a second flag)

**Why All 3 Were Wrong**: They solved a **transport problem** (concurrent BLE writes) with **application logic** (provisioning-aware flags in `respondToTimeSync`). This created false coupling between two independent protocol flows:
- Time sync: `Badge → tim#get → Phone → tim#YYYYMMDDHHMMSS`  
- Provisioning: `Phone → SD#ssid → Phone → PD#password`  

These flows are **logically independent**. The only shared resource is the BLE write pipe.  
**Correct Fix**: Add `writeLock = Mutex()` inside `GattContext.writeCharacteristic()` — serialize ALL BLE writes at the transport layer. Then `respondToTimeSync()` becomes a simple, standalone handler with zero awareness of provisioning.  
**Result**: Removed ~40 lines of flag/defer complexity. Net simpler, net more correct.  
**File(s)**: [GattBleGateway.kt](file:///home/cslh-frank/main_app/feature/connectivity/src/main/java/com/smartsales/feature/connectivity/gateway/GattBleGateway.kt) — `GattContext.writeLock`  
**Pattern**: **When two independent flows share a resource, serialize at the resource level, not the flow level.** If you're adding flags like `isDoingX` to block `Y`, ask: "Is the real problem that X and Y share a pipe?" If yes, lock the pipe, not the logic.  
**Heuristic**: If your fix requires one feature to "know about" another feature → you're coupling at the wrong layer. Push the lock DOWN to the shared resource.  
**Corollary (from Frank)**: "Why should we respond tim# when badge sends SD#?" — if your mental model requires explaining why Feature A cares about Feature B's state, you've coupled them wrong.  
**Status**: ✅ CONFIRMED 2026-02-09

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
