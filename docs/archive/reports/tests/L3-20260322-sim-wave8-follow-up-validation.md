# L3 On-Device Test Record: SIM Wave 8 Follow-Up Validation

**Date**: 2026-03-22
**Tester**: Agent + user
**Target Build**: `:app-core:assembleDebug`

---

## 1. Test Context & Entry State

* **Objective**: Validate the active Wave 8 SIM task-scoped scheduler follow-up slice against `docs/core-flow/sim-shell-routing-flow.md`, `docs/cerb/sim-shell/spec.md`, `docs/cerb/sim-scheduler/spec.md`, and `docs/plans/sim-wave8-execution-brief.md`.
* **Testing Medium**: L3 physical device test with debug-assisted follow-up ingress and focused `adb logcat` monitoring.
* **Initial Device State**: Connected debug device unlocked to the SIM shell. Runs used `SimMainActivity` debug extras `sim_debug_followup_single` and `sim_debug_followup_multi` to trigger the follow-up lane without waiting on physical badge hardware.

## 2. Execution Plan

* **Trigger Action**: Run two focused debug-assisted device checks:
  * single-task follow-up ingress -> visible prompt -> prompt tap -> in-session action-strip visibility
  * multi-task follow-up ingress -> visible prompt -> prompt tap -> no-selection `改期` safe-fail
* **Input Payload**:
  * single-task debug transcript: `提醒我一会儿回访客户`
  * multi-task debug transcript: `安排两个客户回访`

## 3. Expected vs Actual Results

### T1: Single-Task Prompt-First Follow-Up Session

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | Badge-origin follow-up stays prompt-first instead of force-opening chat | Device screenshot and UI dump showed `工牌已创建新日程`, `点击进入任务级跟进会话`, and `继续跟进` while the shell remained on the SIM home/chat surface | ✅ |
| **Telemetry (GPS)** | Ingress creates/rebinds a follow-up session and starts the shell owner without auto-jumping | Focused logs showed `SIM badge scheduler continuity ingress accepted`, `SIM badge scheduler follow-up session created`, `SIM badge scheduler follow-up owner started`, and owner surface update to `CHAT` after launch | ✅ |
| **Prompt Tap Result** | Tapping the prompt opens the bound follow-up session | A second prompt tap landed and the device UI switched into session `客户回访`, showing the follow-up summary plus action strip `任务级跟进 / 客户回访 / 说明 / 状态 / 改期 / 完成 / 删除` | ✅ |
| **Mutation Path Proof** | A selected single-task follow-up action should run against the bound task | After the debug seeding fix, device logs showed `DB_WRITE_EXECUTED` for `debug_follow_up_single` followed by `SIM badge scheduler follow-up action completed` with `action=mark_done taskId=debug_follow_up_single`, and the device UI showed `已标记完成：客户回访` | ✅ |

### T2: Multi-Task No-Selection Safe-Fail

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | Multi-task follow-up session opens with multiple target chips and no forced mutation target | Device screenshot and UI dump showed session `工牌日程跟进` with task chips `客户A回访` and `客户B回访`, plus the shared follow-up action strip | ✅ |
| **Telemetry (GPS)** | No-selection mutation attempt should block with explicit feedback and no completion log | Focused logs showed `SIM badge scheduler follow-up action blocked` with payload `请先选择要改期的日程` after tapping `改期` without selecting a task | ✅ |
| **Visible Failure State** | User sees explicit safe-fail guidance rather than silent mutation | Device screenshot showed banner/toast text `请先选择要改期的日程` and no mutation-success message | ✅ |
| **Negative Check** | No task should mutate before selection | No action-completed telemetry or visible mutation result appeared in the filtered log window | ✅ |

## 4. Deviation & Resolution Log

* **Initial debug-assisted gap**:
  * **Observed issue**: the first T8.0 debug-assisted run proved prompt visibility and multi-task safe-fail, but the single-task mutation path could not resolve a live repository task.
  * **Concrete repo cause**: the debug follow-up path created synthetic follow-up metadata without seeding matching scheduler records.
  * **Resolution in this session**: the debug follow-up path now upserts repository-backed tasks before creating the follow-up session, and the focused device rerun proved the single-task `完成` mutation path end to end.

## 5. Final Verdict

**Accepted for `T8.0`**.

This device run gives concrete acceptance evidence that:

* badge-origin follow-up ingress creates a follow-up session
* the in-shell follow-up prompt/chip is visibly rendered without auto-jumping
* tapping the prompt opens the bound follow-up session
* multi-task no-selection `改期` safe-fails correctly on device

This run now gives honest L3 proof that:

* single-task follow-up mutation executes against a real repository-backed bound task
* multi-task no-selection mutation remains explicitly blocked until the user selects a task

Residual debt remains outside this acceptance slice:

* physical-badge hardware L3 is still deferred; this validation remains debug-assisted rather than true hardware-origin proof
