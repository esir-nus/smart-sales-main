# L3 On-Device Test Record: Wave 19 Explicit-Clock Conflict Regression

**Date**: 2026-03-19
**Tester**: Codex + User
**Target Build**: `:app-core:installDebug`

---

## 1. Test Context & Entry State
* **Objective**: Validate that the resumed explicit-clock Path A fix still preserves conflict-visible behavior when the utterance lands on an already-occupied exact slot.
* **Testing Medium**: L3 Physical Device Test (real app on attached Android device).
* **Initial Device State**: Scheduler day strip on March 21, 2026 was visible, with existing 21:00 cards already present on that date.

## 2. Execution Plan
* **Trigger Action**: Used live voice input in the app and spoke `后天晚上九点去接李总`.
* **Input Payload**: Relative-day exact schedulable utterance with explicit clock cue. With device date on March 19, 2026 in Asia/Shanghai, `后天` should resolve to March 21, 2026.

## 3. Expected vs Actual Results

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | Exact task should appear on March 21, 2026 at 21:00 and show conflict-visible caution because same-slot cards already exist. | Exact task `去接李总` appeared on March 21, 2026 at 21:00, but it rendered as a clean exact card instead of conflict-visible state. | ❌ |
| **Telemetry (GPS)** | `INPUT_RECEIVED -> ROUTER_DECISION -> TASK_EXTRACTED -> CONFLICT_EVALUATED(conflict) -> PATH_A_DB_WRITTEN(conflict-visible) -> UI_STATE_EMITTED` | `INPUT_RECEIVED -> ROUTER_DECISION -> PATH_A_PARSED -> TASK_EXTRACTED -> DB_WRITE_EXECUTED -> CONFLICT_EVALUATED(Uni-A conflict clear) -> PATH_A_DB_WRITTEN(Uni-A exact task persisted) -> UI_STATE_EMITTED` | ❌ |
| **Log Evidence** | `IntentOrchestrator` should log a conflict-visible exact commit such as `Uni-D overlap detected` / `Uni-D conflict-visible task persisted`. | `CreateTasks(... startTimeIso=2026-03-21T21:00+08:00, durationMinutes=0 ...)`, then `CONFLICT_EVALUATED | Uni-A conflict clear`, `PATH_A_DB_WRITTEN | Uni-A exact task persisted`, and `IntentOrchestrator: Uni-A exact Path A committed ...`. | ❌ |
| **Negative Check**| Must not emit clean exact persistence when same-slot tasks already exist; must not silently skip conflict surfacing. | Conflict surfacing was skipped; the live exact task carried `durationMinutes=0`, so the app persisted a clean exact task with no caution evidence. | ❌ |

---

## 4. Deviation & Resolution Log
* **Attempt 1 Failure**: Anchoring was correct, but overlap detection did not trigger even though the screenshot showed existing 21:00 items on March 21, 2026.
  - **Root Cause**: Evidence indicates the live exact extraction persisted `durationMinutes=0`. The conflict path then passed that zero duration into `ScheduleBoard.checkConflict()`, so `proposedEnd == proposedStart` and the overlap predicate treated the event as zero-length, yielding `Uni-A conflict clear`.
  - **Resolution**: Not fixed in this test session. Requires code repair so exact creates that should participate in conflict detection do not enter the overlap math as zero-duration intervals.

## 5. Final Verdict
**❌ FAILED**.
