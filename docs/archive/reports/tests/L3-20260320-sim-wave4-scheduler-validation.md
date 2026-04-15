# L3 On-Device Test Record: SIM Wave 4 Scheduler Validation

**Date**: 2026-03-20
**Tester**: Agent + user
**Target Build**: `:app-core:assembleDebug`

---

## 1. Test Context & Entry State

* **Objective**: Validate the active Wave 4 SIM scheduler acceptance slice against `docs/core-flow/sim-scheduler-path-a-flow.md` and `docs/cerb/sim-scheduler/spec.md`.
* **Testing Medium**: L3 physical device test with live `adb logcat` monitoring.
* **Initial Device State**: Fresh `:app-core:installDebug` rerun completed on the connected device. Runtime permissions were granted over `adb` so `SimMainActivity` could open directly into the standalone SIM shell instead of stopping at the Android permission controller.

## 2. Execution Plan

* **Trigger Action**: Run four focused scheduler checks on device: exact Path A create, conflict-visible create, safe-fail unsupported voice action, and inspiration shelf `Ask AI` handoff.
* **Input Payload**:
  * T1 exact create: exact schedulable speech input
  * T2 conflict-visible create: overlapping schedulable speech input
  * T3 safe-fail: unsupported voice reschedule/delete-style input
  * T4 shelf handoff: tap `Ask AI` on one inspiration shelf card

## 3. Expected vs Actual Results

### T1: Exact Path A Create

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | New task appears in scheduler with successful create state | User marked T1 green; exact create rendered in SIM scheduler timeline | ✅ |
| **Telemetry (GPS)** | `SIM_SCHEDULER_ENTERED -> SIM_PATH_A_REQUESTED -> TASK_EXTRACTED -> CONFLICT_EVALUATED -> DB_WRITE_EXECUTED -> SIM_SCHEDULER_RENDERED` | Observed downstream proof: `DB_WRITE_EXECUTED` then `UI_STATE_EMITTED` | ✅ |
| **Log Evidence** | ASR transcription logs plus conflict-clear evaluation | `18:51:44-18:51:46 FunAsrService` async task / wait finished, `18:51:47 ScheduleBoard` `checkConflict: CLEAR`, `18:51:47 VALVE_PROTOCOL` `DB_WRITE_EXECUTED`, then `UI_STATE_EMITTED` | ✅ |
| **Negative Check** | Must not open chat or silently drop the request | Task write completed in scheduler lane; no crash observed | ✅ |

### T2: Conflict-Visible Create

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | Overlapping task still appears and conflict remains visible | User marked T2 green; device UI showed the overlapping task with visible conflict styling | ✅ |
| **Telemetry (GPS)** | `SIM_SCHEDULER_ENTERED -> SIM_PATH_A_REQUESTED -> TASK_EXTRACTED -> CONFLICT_EVALUATED -> DB_WRITE_EXECUTED -> SIM_SCHEDULER_RENDERED` | Observed downstream proof: conflict evaluation path still wrote the task and emitted scheduler UI state | ✅ |
| **Log Evidence** | ASR transcription logs plus conflict log | `18:52:08-18:52:10 FunAsrService` transcription, `18:52:11 ScheduleBoard` `checkConflict: 1 conflicts ([和客户开会])`, `18:52:11 VALVE_PROTOCOL` `DB_WRITE_EXECUTED`, then `UI_STATE_EMITTED` | ✅ |
| **Negative Check** | Conflict must not silently reject creation | Task write occurred despite conflict; no silent rejection evidence | ✅ |

### T3: Safe-Fail Unsupported Voice Reschedule/Delete

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | Explicit failure banner and no mutation | User marked T3 green; device UI showed `已记录缺口：全局跟进式改期仍待接入共享上下文 owner` while existing tasks remained intact | ✅ |
| **Telemetry (GPS)** | `SIM_SCHEDULER_ENTERED -> SIM_PATH_A_REQUESTED -> SIM_FAST_FAIL_RETURNED` | No downstream write evidence followed the unsupported voice attempts | ✅ |
| **Log Evidence** | ASR transcription logs without scheduler write logs | `18:52:23-18:52:24 FunAsrService` and `18:52:44-18:52:45 FunAsrService` completed transcription, with no subsequent `ScheduleBoard checkConflict` or `VALVE_PROTOCOL DB_WRITE_EXECUTED` attributable to those safe-fail attempts | ✅ |
| **Negative Check** | No existing task should move or disappear | User reported green and the scheduler timeline remained stable after the failure banner | ✅ |

### T4: Inspiration Shelf `Ask AI` Handoff

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | Drawer closes, fresh SIM chat opens, inspiration text is auto-seeded and auto-submitted | User marked T4 green; on device the chat opened with the inspiration text `我想学吉他。` auto-seeded and sent | ✅ |
| **Telemetry (GPS)** | Scheduler UI handoff into seeded chat session | Focused rerun observed exact `VALVE_PROTOCOL` summaries at `20:07:07`: `SIM scheduler shelf Ask AI handoff requested` followed immediately by `SIM scheduler shelf seeded chat session started` | ✅ |
| **Log Evidence** | Prefer dedicated scheduler-to-chat handoff logs | Focused rerun observed matching `SimSchedulerShelf` log lines at `20:07:07` for both request and session start, with payload `我想学游泳。` | ✅ |
| **Negative Check** | Must not create or mutate a scheduler task | No `DB_WRITE_EXECUTED` evidence appeared in the filtered handoff log window, so the branch stayed launcher-only with no scheduler mutation | ✅ |

## 4. Deviation & Resolution Log

* **Focused rerun requirement**: The first device pass proved the UI branch before dedicated scheduler-to-chat telemetry landed.
  - **Impact**: Acceptance still needed one narrow L3 rerun for exact request/session-start log capture.
  - **Resolution**: The focused rerun is now complete. Device logs recorded both new telemetry summaries and both `SimSchedulerShelf` lines without any `DB_WRITE_EXECUTED` evidence in the handoff window.

* **Out-of-scope logic note**: After the T4 handoff, chat content still behaves like the current SIM placeholder/standalone chat layer rather than a fully wired scheduler follow-up intelligence path.
  - **Impact**: This is not a blocker for the base shelf-card launcher contract.
  - **Resolution**: Treat deeper scheduler-origin chat intelligence as separate follow-up work, not as a failure of Wave 4 handoff equivalence.

## 5. Final Verdict

**✅ SHIPPED (Wave 4 validation slice)**.

This device run gives acceptance evidence for the current active Wave 4 validation targets:

* exact Path A create is green
* conflict-visible create is green
* unsupported voice reschedule/delete-style input safe-fails without mutation
* inspiration shelf `Ask AI` launcher is green on device for the required UI contract

Residual product debt remains:

* keep the physical-badge-origin global follow-up owner as explicit carry debt until it is completed across interfaces
