# Handoff: DTQ-03 Connectivity and OEM Hardening

> **Lane ID**: `DTQ-03`
> **Registry Lane ID**: `DTQ-03`
> **Branch**: `Unassigned in registry; create or attach a dedicated lane branch before resuming feature edits`
> **Recommended Worktree**: `Unassigned in registry; create or attach a dedicated lane worktree before resuming feature edits`
> **Scope**: Connectivity truth, OEM reminder hardening, and compatibility-guidance lane for the current dirty tree.

## Scope

This handoff owns the transport/OEM seam for the current dirty tree: connectivity bridge/service behavior, OEM compatibility guidance, reminder hardening, and device-specific operational notes for Huawei/Honor/Harmony-device compatibility and other aggressive OEM ROMs.

## Owned Paths

- `app-core/src/main/java/com/smartsales/prism/data/connectivity/**`
- `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/**`
- `app-core/src/main/java/com/smartsales/prism/data/notification/OemCompat.kt`
- reminder-hardening files such as `app-core/src/main/java/com/smartsales/prism/data/notification/ReminderReliabilityAdvisor.kt` and `app-core/src/main/java/com/smartsales/prism/data/scheduler/TaskReminderReceiver.kt` when the change is OEM-delivery-specific
- connectivity/OEM tests under `app-core/src/test/**` and `app-core/src/androidTest/**`
- `docs/cerb/connectivity-bridge/**`
- `docs/cerb/sim-connectivity/spec.md`
- `docs/specs/connectivity-spec.md`
- `docs/cerb/notifications/spec.md` when the edit is OEM/reminder-delivery-specific
- `docs/projects/connectivity-bug-triage/tracker.md`
- OEM SOP / plan docs such as `docs/sops/oem-alarm-notification-checklist.md`, `docs/sops/oem-alarm-notification-control-plane.md`, and `docs/plans/oem-alarm-hardening-plan.md`

## Current Repo State / Implementation Truth

The current dirty tree already contains live connectivity and OEM hardening work, not just a diagnosis memo. This includes connectivity bridge/service changes, OEM settings guidance, reminder reliability checks, and Android-on-Huawei/Harmony compatibility handling.

The report below remains useful diagnostic evidence, but current truth must be taken from the live connectivity/OEM code and the owning docs listed here.

## What Is Finished

- The lane already has a bounded connectivity/OEM scope in the quarantine tracker.
- The historical diagnosis remains preserved as concrete investigative context.
- This handoff now records the lane ID, current drift state, and exact owning docs so future passes do not need to rediscover the contract.

## What Is Still Open

- Keep reminder hardening, connectivity truth, and OEM guidance aligned across code and docs.
- Capture runtime/device evidence for OEM-specific behavior rather than promoting inference as proof.
- Do not mark the lane `Accepted` until the refreshed connectivity/OEM verification includes device/runtime evidence.

## Doc-Code Alignment

- **Owning source-of-truth docs**:
  - `docs/specs/connectivity-spec.md`
  - `docs/cerb/connectivity-bridge/spec.md`
  - `docs/cerb/connectivity-bridge/interface.md`
  - `docs/cerb/sim-connectivity/spec.md`
  - `docs/cerb/notifications/spec.md`
  - `docs/projects/connectivity-bug-triage/tracker.md`
  - `docs/sops/oem-alarm-notification-checklist.md`
  - `docs/sops/oem-alarm-notification-control-plane.md`
  - `docs/plans/oem-alarm-hardening-plan.md`
- **Current alignment state**: `Aligned`
- **Docs still needing sync or final confirmation before `Accepted`**:
  - none; doc-code alignment is currently restored for the audited bridge contract
- **Rule**: no connectivity/OEM code in this lane should be treated as landed while these docs still trail or contradict the implementation.

## 2026-04-04 Audit Result

- **Docs reviewed**:
  - `docs/specs/connectivity-spec.md`
  - `docs/cerb/connectivity-bridge/spec.md`
  - `docs/cerb/connectivity-bridge/interface.md`
  - `docs/cerb/sim-connectivity/spec.md`
  - `docs/cerb/notifications/spec.md`
  - `docs/projects/connectivity-bug-triage/tracker.md`
  - `docs/sops/oem-alarm-notification-checklist.md`
  - `docs/sops/oem-alarm-notification-control-plane.md`
  - `docs/plans/oem-alarm-hardening-plan.md`
- **Code reviewed**:
  - `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityService.kt`
  - `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/notification/OemCompat.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/notification/ReminderReliabilityAdvisor.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/scheduler/TaskReminderReceiver.kt`
- **Focused verification passed**:
  - `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest' --tests 'com.smartsales.prism.data.connectivity.RealConnectivityServiceTest' --tests 'com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest' --tests 'com.smartsales.prism.data.notification.ReminderReliabilityAdvisorTest' --tests 'com.smartsales.prism.data.notification.OemCompatTest'`
- **Concrete update**:
  - `RealConnectivityBridge.recordingNotifications()` now gates raw `DeviceConnectionManager.recordingReadyEvents` inside the bridge-owned collector, so disconnected and BLE-only-not-ready events are dropped before they can reach Prism consumers.
  - `RealConnectivityBridgeTest` now proves both sides of the contract: disconnected events stay suppressed, and the next event is emitted only after transport-ready connectivity is established.
- **Runtime evidence gap**:
  - April 4, 2026 device pass on Xiaomi `2410DPN6CC` (Android 16 / SDK 36, serial `fc8ede3e`) captured partial DTQ-03 runtime evidence:
    - package snapshot showed `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, and `SCHEDULE_EXACT_ALARM` granted
    - `TaskReminderReceiver` logged `收到任务提醒`, `fullScreenIntent 已设置 (DEADLINE)`, and `通知已显示` for task `a3476252-efa3-4c00-8c17-5ee7545bc133` at `15:07:54`
    - connectivity logs still showed repeated `notification listener inactive`, unreadable phone SSID (`raw=<unknown ssid>`), and `BadgeHttpClient.isReachable` failures against the badge endpoint
  - The same device pass did not capture any badge `recording ready` notification, so the bridge's on-device suppression/forwarding branch is still not proven in runtime.
- **Audit disposition**:
  - keep `DTQ-03` at `Active`
  - treat alignment as `Aligned`
  - do not promote the lane until the recording-ready bridge branch is exercised on-device and the remaining connectivity runtime gaps are understood

## Required Evidence / Verification

- Focused connectivity/OEM tests for the touched bridge, service, modal, and reminder-hardening branches.
- `adb logcat` evidence whenever diagnosis involves runtime behavior, lifecycle, alarms, notifications, OEM settings, or background execution.
- Concrete device evidence before claiming Harmony/Huawei/HyperOS behavior is proven.
- Ready-to-run operator helper: `scripts/dtq03_device_verify.sh [all|connectivity|oem|snapshot] [serial]`

## Device Verification Procedure

1. Connect exactly one DTQ-03 target device over `adb`.
2. Run `scripts/dtq03_device_verify.sh all [serial]`.
3. Keep that terminal running while you execute the real repro:
   - connectivity branch: one recording-ready event before transport-ready, then one after transport-ready
   - OEM branch: one near-future DEADLINE reminder with the screen locked
4. Save the resulting logcat transcript as DTQ-03 runtime evidence before changing the lane state.

### Minimum expected DTQ-03 evidence

- Connectivity branch:
  - `SmartSalesConn` shows the persistent BLE / badge-notification path
  - `AudioPipeline` shows the blocked pre-ready event or the post-ready accepted event
- OEM reminder branch:
  - `RealAlarmScheduler` shows scheduling
  - `TaskReminderReceiver` shows receiver entry and full-screen-intent/fallback branch
  - `NotificationService` or `TaskReminderReceiver` shows notification display result

## Safe Next Actions

- Continue only inside the connectivity/OEM-owned paths listed above.
- Sync the owning connectivity/OEM docs in the same session as any behavior change.
- Split shared scheduler semantics back to `DTQ-02` and onboarding flow logic back to `DTQ-01` if the work starts to broaden.

## Do Not Touch / Collision Notes

- Do not absorb shared scheduler-routing ownership from `DTQ-02`.
- Do not absorb onboarding flow or quick-start ownership from `DTQ-01`.
- Do not use shell chrome or governance docs as a backdoor place to park OEM logic.

## Detailed Historical Notes

### Scope
This note cross-checks the current Smart Sales alarm/reminder stack for two related but distinct problems:

1. HarmonyOS scheduler-drawer create flow crash risk after task persistence
2. Screen-off DEADLINE alarm delivery risk on Android 14+ and aggressive OEM ROMs

It is not a single-cause root-cause report. The current repo evidence supports different primary risks for those two problems.

### Current Repo State

#### A. HarmonyOS create-time crash risk
The live runtime shell uses `SimSchedulerViewModel`, not the older shared scheduler host:
- `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`

After a successful create mutation, the SIM scheduler path does this in order:
1. loads the created task from the repository
2. emits reminder hardening guidance if needed
3. schedules reminder alarms for exact tasks

Relevant code:
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerMutationCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerReminderSupport.kt`

This ordering matters. If the app crashes after reopen but the created task is already visible, that means persistence likely completed before the failing branch. The strongest repo-local crash candidates are therefore the reminder-guidance snapshot path, not the core task-create path.

Most likely crash seam:
- `RealExactAlarmPermissionGate.shouldPromptForExactAlarm()` calls `ReminderReliabilityAdvisor.fromContext(...)`
- `SchedulerDrawer` calls `reminderGuideProvider(context)` again when the prompt event is collected
- `ReminderReliabilityAdvisor.collectSnapshot(...)` performs multiple framework and OEM checks without local exception isolation

Relevant code:
- `app-core/src/main/java/com/smartsales/prism/data/notification/RealExactAlarmPermissionGate.kt`
- `app-core/src/main/java/com/smartsales/prism/data/notification/ReminderReliabilityAdvisor.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt`

Inference: if HarmonyOS throws from one of the notification, exact-alarm, battery, or OEM state checks, the app can crash immediately after create while still leaving the task saved in the repository.

#### B. Screen-off DEADLINE alarm delivery risk
The DEADLINE alarm path does already:
- declare `USE_FULL_SCREEN_INTENT` in the manifest
- check `NotificationManager.canUseFullScreenIntent()` on Android 14+
- attach `fullScreenIntent` only when allowed
- fall back to `SCREEN_BRIGHT_WAKE_LOCK` when FSI is unavailable

Relevant code:
- `app-core/src/main/AndroidManifest.xml`
- `app-core/src/main/java/com/smartsales/prism/data/scheduler/TaskReminderReceiver.kt`

This means Android 14 full-screen-intent access is a real hardening gap, but it is separate from the more likely HarmonyOS create-time crash risk above.

### Cross-Check Table

| Topic | Current Repo Status | Result |
|---|---|---|
| Manifest declares `USE_FULL_SCREEN_INTENT` | Present in `AndroidManifest.xml` | Yes |
| Android 14 `canUseFullScreenIntent()` check | Present in `TaskReminderReceiver` | Yes |
| Android 14 FSI settings redirect helper | No `OemCompat.openFullScreenIntentSettings(...)` | Missing |
| Xiaomi lock-screen / floating / background AppOps checks | Present in `OemCompat` | Yes |
| Huawei notification settings routing | `openHuaweiNotificationSettings(...)` exists | Partial yes |
| Huawei lock-screen-specific deep link | No dedicated Huawei lock-screen-toggle helper | Missing |
| Reminder guidance collection crash-hardening | `ReminderReliabilityAdvisor.collectSnapshot(...)` is not exception-safe | Missing |
| Scheduler create flow survives scheduling exceptions | `scheduleCascade(...)` is wrapped in `runCatching` | Yes |

### Findings

#### 1. Critical: HarmonyOS crash risk is more likely in reminder snapshot collection than in missing alarm permission
The current code does not gate task creation on alarm or notification permission.

`SimSchedulerReminderSupport.scheduleReminderIfExact(...)` wraps alarm scheduling in `runCatching`, so a plain exact-alarm permission miss should degrade to logging rather than crash the app.

By contrast, reminder-guide snapshot collection is unguarded:
- `RealExactAlarmPermissionGate` calls `ReminderReliabilityAdvisor.fromContext(...)` directly
- `SchedulerDrawer` rebuilds the guide from context directly when the prompt event arrives
- `ReminderReliabilityAdvisor.collectSnapshot(...)` calls system-service checks directly

For the reported HarmonyOS symptom - immediate crash after ASR/create, task visible after relaunch - this is the strongest repo-backed failure candidate.

#### 2. High: Android 14+ full-screen-intent hardening is incomplete
The repo checks FSI availability but does not guide the user to grant it.

Current behavior in `TaskReminderReceiver`:
- if `canUseFullScreenIntent()` is true -> attach `fullScreenIntent`
- if false -> log and degrade to banner plus wake-lock fallback

Missing behavior:
- no `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` helper
- no reminder guidance action for FSI
- no user-center or onboarding surface for this permission

This is a real gap for screen-off DEADLINE alarm delivery on Android 14+, including some HyperOS/Huawei-family devices.

#### 3. Medium: Huawei/HarmonyOS support is guidance-heavy rather than deeply inspectable
The current repo already has Huawei-specific notification routing:
- `OemCompat.openHuaweiNotificationSettings(...)`

What it does not have:
- a Huawei lock-screen-toggle-specific deep link
- Huawei-specific runtime inspection comparable to Xiaomi AppOps checks

This matches the current spec direction: Huawei/Honor is treated as a launch-management-first reliability problem rather than a deep-link-and-detect-everything path.

#### 4. Medium: Wake-lock fallback is still a risk, but not yet proven inadequate by repo-local evidence
The current spec still explicitly treats `SCREEN_BRIGHT_WAKE_LOCK` as the intended fallback on aggressive OEMs:
- `docs/cerb/notifications/spec.md`

So the current statement should be:
- likely limitation on modern OEM ROMs, especially when background activity is restricted
- not yet proven failure inside this repo without direct device/runtime evidence

### Already Handled Well
- Xiaomi / HyperOS AppOps coverage for lock-screen display, floating notifications, and background local notifications is implemented in `OemCompat`
- Huawei/Honor notification settings routing exists via `openHuaweiNotificationSettings(...)`
- notification-disabled devices are prioritized first in reminder guidance
- exact-alarm state is already part of reminder guidance
- scheduler reminder scheduling is non-blocking and wrapped against direct scheduling exceptions

### Recommended Fix Order

1. Crash-proof the reminder snapshot path
   - make `ReminderReliabilityAdvisor.collectSnapshot(...)` exception-safe per check
   - guard `RealExactAlarmPermissionGate.shouldPromptForExactAlarm()` against snapshot failures
   - guard the `SchedulerDrawer` reminder-guide rebuild path against snapshot failures

2. Add Android 14+ FSI hardening support
   - add `OemCompat.openFullScreenIntentSettings(context)` using `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`
   - add a reminder-guidance action for FSI when Android 14+ reports `canUseFullScreenIntent() == false`

3. Keep Huawei/HarmonyOS guidance realistic
   - retain Huawei as a launch-management-first branch
   - add clearer lock-screen notification wording to SOP/docs
   - do not claim native HarmonyOS reminder APIs or hidden Huawei automation that the repo does not implement

4. Update docs after code changes
   - `docs/cerb/notifications/spec.md`
   - `docs/sops/oem-alarm-notification-checklist.md`

### Confidence and Limits
- High confidence: the report's old FSI-centric framing is incomplete for the HarmonyOS create-time crash symptom
- Medium confidence: the most likely HarmonyOS crash seam is reminder snapshot collection
- Low confidence: exact HarmonyOS framework exception class, because direct HarmonyOS logs are not available in the current evidence set
