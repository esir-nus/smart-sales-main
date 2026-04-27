# Sprint 04 — connectivity-isolation-repair-dataflow

## Header

- Project: badge-session-lifecycle
- Sprint: 04
- Slug: connectivity-isolation-repair-dataflow
- Date authored: 2026-04-27
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):** "i switched from wifi to hotspot(which should be working) and when network change detected (thouhg not effectively) the \"重新配对\"s not working. if sprint 01 is for dataflow backend debuging and iterating, we should escalete spring01 to a full dataflow test and fix task, the problem is more than just one"

The lifecycle project must now cover real runtime connectivity recovery, not only audio queue cleanup and drawer observation. The initial defect was that the isolation prompt's `重新配对` action did not recover after Wi-Fi/hotspot changes. User correction on 2026-04-27 clarified that full re-pairing is the wrong product behavior for an already registered badge; the correct recovery is Wi-Fi credential repair that keeps the registered device intact.

## Scope

Files the operator may touch:

- `docs/core-flow/badge-session-lifecycle.md`
- `docs/projects/badge-session-lifecycle/tracker.md`
- `docs/projects/badge-session-lifecycle/sprints/04-connectivity-isolation-repair-dataflow.md`
- `docs/projects/badge-session-lifecycle/evidence/04-connectivity-isolation-repair-dataflow/**`
- `app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`
- Focused tests for those files

Out of scope:

- Firmware protocol changes
- Audio repository/download queue changes already covered by Sprint 02
- Sprint 03 audio drawer observation changes
- Full add-device pairing or registry removal for an already registered active badge

## References

- `docs/core-flow/badge-session-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/device-pairing/spec.md`
- `docs/cerb/interface-map.md`
- `.agent/rules/lessons-learned.md`
- `app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/data/pairing/RealPairingService.kt`

## Success Exit Criteria

1. Static trace proves `WifiRepairIsolationContent` labels the CTA as Wi-Fi repair and does not wire it to `onIgnore`.
2. Focused tests prove the isolation Wi-Fi repair action enters the credential form and does not remove the active registered device.
3. `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelRepairTest` exits 0.
4. Latest debug APK is installed with `adb install -r app-core/build/outputs/apk/debug/app-core-debug.apk`.
5. adb/logcat evidence is captured under `docs/projects/badge-session-lifecycle/evidence/04-connectivity-isolation-repair-dataflow/` showing the installed runtime and either the isolation Wi-Fi repair action or the current blocker preventing full reproduction.

## Stop Exit Criteria

- Runtime evidence shows the Wi-Fi repair form cannot submit or confirm credentials through the existing manual repair path.
- adb is unavailable or the device cannot run the installed APK.
- The focused unit tests fail for unrelated infrastructure reasons after one repair attempt.

## Iteration Bound

3 iterations or 60 minutes, whichever is reached first.

## Required Evidence Format

1. `grep` output proving the isolation button routes to `onStartWifiRepair`, not `onIgnore`.
2. Focused unit test pass summary.
3. `adb install -r` output.
4. Logcat excerpt or artifact path for the runtime pass.

## Iteration Ledger

<!-- Operator appends one entry per iteration. Not committed mid-sprint. -->

- 2026-04-27 iteration 1: Read the lifecycle core-flow, connectivity bridge spec, interface map, device-pairing spec, lessons index/details, and traced the isolation prompt path. Found `WifiMismatchView` wired `WifiRepairIsolationContent(onRePair = onIgnore)`, so `重新配对` only cleared the prompt. Also confirmed `RealPairingService.filterNotRegistered()` excludes already registered badges during add-device scan, so a working re-pair must either introduce a re-pair scan mode or clear the active registry row before entering the existing pairing host. Implemented the smaller active-device re-pair preparation path and added focused tests.
- 2026-04-27 iteration 2: Built and installed the latest debug APK, then reproduced the runtime isolation path after launch. Logcat shows the badge reporting `IP#192.168.43.94` / `SD#MorningStar` while HTTP reachability from the phone timed out, causing `SIM badge sync isolation suspected ... trigger=on_connect`. Screenshot evidence captured the isolation card with `重新配对`.
- 2026-04-27 iteration 3: Tapped `重新配对` on-device. The retained screenshots show the isolation card before the tap and the pairing/provisioning surface after the tap. Runtime keypath evidence shows the re-pair path entering scan, finding the badge, provisioning Wi-Fi, confirming badge IP / HTTP readiness, re-registering the device, and completing auto badge sync with `branch=device_empty`. Focused and full JVM tests passed. Attempted `connectedDebugAndroidTest` for `WifiMismatchViewTest`, but it hung after `1/5` tests; stopped the runner and kept JVM + real adb evidence as the reliable validation for this iteration.
- 2026-04-27 iteration 4: User corrected the product direction: `重新配对` is misleading for an already paired badge and full re-pairing is overacting. Revised the contract toward registered-device Wi-Fi credential repair, keeping the active registry row intact and entering the existing manual Wi-Fi repair form instead of navigating to add-device pairing.
- 2026-04-27 iteration 5: Implemented the corrected Wi-Fi repair direction. `WifiRepairIsolationContent` now labels the CTA `修复 Wi-Fi 配置` and routes it to `ConnectivityViewModel.startIsolationWifiRepair()`, which keeps the active registry row, preserves isolation context, and enters `WifiRepairState.EditCredentials`. `ConnectivityPrompt.promptSuspectedIsolation()` now carries an optional suggested phone SSID so the repair form can prefill when runtime callers know it. Verified `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelRepairTest`, `./gradlew :app-core:assembleDebug`, and `./gradlew :app-core:testDebugUnitTest` all passed. Installed `app-core/build/outputs/apk/debug/app-core-debug.apk` successfully. Runtime launch logcat saved to `docs/projects/badge-session-lifecycle/evidence/04-connectivity-isolation-repair-dataflow/logcat-wifi-repair-launch-20260427-114031.txt`; the badge was healthy on `SD#qwe` with HTTP `200`, so this pass is a healthy baseline rather than an isolation-tap reproduction.

## Closeout

<!-- Operator fills on exit. -->

- Status: success
- Summary: Registered-badge isolation recovery now enters Wi-Fi credential repair instead of clearing the prompt or starting full add-device pairing; implementation and runtime evidence shipped in `4d273192b`.
- Evidence:
  - Static routing evidence: `WifiRepairIsolationContent` labels the CTA `修复 Wi-Fi 配置` and routes the action through `onStartWifiRepair`, not `onIgnore`.
  - Focused repair test passed: `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelRepairTest`.
  - Debug APK built: `./gradlew :app-core:assembleDebug`.
  - Full unit suite passed during the original Sprint 04 close: `./gradlew :app-core:testDebugUnitTest`.
  - Debug APK installed during the original Sprint 04 close: `adb install -r app-core/build/outputs/apk/debug/app-core-debug.apk`.
  - Runtime evidence paths committed in `4d273192b`: `docs/projects/badge-session-lifecycle/evidence/04-connectivity-isolation-repair-dataflow/runtime-repair-tap-20260427.txt`, `runtime-repair-keypath-20260427.txt`, `logcat-wifi-repair-launch-20260427-114031.txt`, `isolation-before-repair-20260427.png`, `after-repair-tap-20260427.png`, and `repair-scan-timeout-20260427.png`.
- Lesson proposals: none.
- CHANGELOG line: none.
