# L3 On-Device Test Record: SIM Wave 9 Hardware Validation

**Date**: 2026-03-22
**Tester**: Agent
**Target Build**: `./gradlew :app-core:installDebug`
**Verdict**: Blocked

---

## 1. Test Context & Contract Freeze

* **Objective**: Execute the Wave 9 physical-badge-only validation slice defined by `docs/plans/sim-wave9-execution-brief.md`, `docs/core-flow/sim-shell-routing-flow.md`, `docs/cerb/sim-shell/spec.md`, `docs/cerb/sim-scheduler/spec.md`, and `docs/cerb/sim-audio-chat/spec.md`.
* **Locked Validation Law**:
  * only real badge-origin ingress counts
  * no `SimMainActivity` debug follow-up extras may be used
  * single-task proof requires visible prompt/chip plus correct downstream `完成` mutation evidence
  * multi-task proof requires no-selection mutation safe-fail plus no unintended write
* **Device**:
  * adb serial: `fc8ede3e`
  * reported model: `2410DPN6CC`
  * Android version: `16`

## 2. Preflight Evidence

* **Repo-side verification**:
  * `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest --tests com.smartsales.prism.ui.sim.SimAgentViewModelTest`
  * Result: passed
* **Install / launch**:
  * `./gradlew :app-core:installDebug`
  * `adb shell am start -n com.smartsales.prism/.SimMainActivity`
  * Result: app installed and launched on the connected device
* **No-debug-extra guard**:
  * launch command used the plain `SimMainActivity` component only
  * no `sim_debug_followup_single` or `sim_debug_followup_multi` extras were supplied
* **Idle shell UI proof**:
  * `adb shell uiautomator dump /sdcard/t9_wave9_ui.xml`
  * `adb shell cat /sdcard/t9_wave9_ui.xml`
  * Result: SIM standalone shell rendered with `SIM`, `欢迎进入 SIM`, and the normal composer placeholder `输入消息，或长按工牌说话...`
* **Follow-up log surface**:
  * `adb logcat -d -s SimBadgeFollowUp SimBadgeFollowUpChat RealBadgeAudioPipeline`
  * Result: no Wave 9 follow-up ingress, follow-up session, or badge pipeline completion logs were captured during this session

## 3. Expected vs Actual Results

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **Single-task hardware ingress** | Real badge event reaches `RealBadgeAudioPipeline`, emits scheduler completion, and SIM shows the follow-up prompt/chip | Not exercised. No real badge-origin completion was triggered or captured in-session, and the SIM follow-up log surface remained empty | BLOCKED |
| **Single-task bound mutation** | Opening the prompt and tapping `完成` mutates the correct bound task with downstream write evidence | Not exercised because no real single-task hardware ingress was available to create the bound session honestly | BLOCKED |
| **Multi-task hardware safe-fail** | Real badge multi-task ingress opens the prompt/session, and no-selection `改期` blocks without any write | Not exercised because no real multi-task hardware ingress was available in-session | BLOCKED |
| **Boundary guard** | SIM runs through the real standalone shell path without falling back to debug follow-up ingress | Confirmed. The app launched normally into SIM and no debug extras were used | ✅ |

## 4. Blocking Constraint

The phone-side environment was ready, but the required physical-badge-origin fixtures were not available in this session.

That means the run did **not** capture:

* a real single-task badge recording that completed through the scheduler path
* a real multi-task badge recording that completed through the scheduler path
* the follow-up prompt/chip from true hardware ingress
* downstream mutation or no-write evidence tied to those real fixtures

Per the Wave 9 brief, this prevents honest acceptance. The debug-assisted Wave 8 path was intentionally not reused because it does not satisfy the hardware-origin proof requirement.

## 5. Final Verdict

**Blocked**.

This session proves:

* the targeted SIM follow-up unit coverage remains green
* the current debug build installs and launches on the connected device
* the standalone SIM shell can be entered without debug follow-up extras

This session does **not** prove:

* physical-badge single-task follow-up ingress
* correct bound-task `完成` mutation from hardware-origin follow-up
* physical-badge multi-task no-selection safe-fail

Wave 9 remains open until a real badge-origin single-task fixture and a real badge-origin multi-task fixture are executed and captured on device.
