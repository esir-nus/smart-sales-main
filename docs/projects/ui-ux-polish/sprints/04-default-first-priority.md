# Sprint 04 - default-first-priority

## Header

- Project: ui-ux-polish
- Sprint: 04
- Slug: default-first-priority
- Date authored: 2026-04-29
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none
- Dependency: blocked until `docs/projects/ui-ux-polish/sprints/03-registry-gated-reconnect.md` closes successfully

## Demand

**Superseded product decision (2026-04-29):** The default-first automatic
selection objective below is rejected. Automatic recovery must remain
active-only; switching between registered badges requires explicit manual user
action.

**User ask (verbatim):**

> Author UI-UX Polish Sprints 03 and 04. Sequence Android `develop` work for registry-gated reconnect first, then default-first BLE priority, with Core Flow alignment made explicit.

**Interpretation:**

After stale removed-MAC reconnects are guarded, BLE detection should prefer the registered default badge when both the current active badge and the default badge are advertising. This sprint changes only that priority rule, preserves passive `setDefault()`, and syncs Core Flow wording if the rule intentionally relaxes the current active-only reconnect target.

## Scope

Files the operator may touch:

- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/DeviceRegistry.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/SharedPrefsDeviceRegistry.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/InMemoryDeviceRegistry.kt`
- `app-core/src/test/java/com/smartsales/prism/data/connectivity/registry/*`
- `docs/projects/ui-ux-polish/tracker.md`
- `docs/projects/ui-ux-polish/sprints/04-default-first-priority.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/bake-contracts/connectivity-badge-session.md`

Out of scope:

- Sprint 03 stale-session or removed-MAC guard work.
- Immediate active-device switching inside `setDefault()`.
- Harmony or `platform/harmony`.
- UI redesign.
- Public manager API changes unless implementation proves no contained internal alternative exists.

## References

- `docs/specs/sprint-contract.md`
- `docs/projects/ui-ux-polish/tracker.md`
- `docs/projects/ui-ux-polish/sprints/01-connectivity-modal-state.md`
- `docs/projects/ui-ux-polish/sprints/02-audio-card-hold-state-fix.md`
- `docs/projects/ui-ux-polish/sprints/03-registry-gated-reconnect.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/bake-contracts/connectivity-badge-session.md`
- `docs/sops/esp32-connectivity-debug.md`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt`

## Success Exit Criteria

All criteria must pass before close:

1. With active non-default badge B disconnected while default badge A and active badge B both advertise, the BLE detection monitor selects A and emits `[DefaultPriority] switch`.
2. A default device with `manuallyDisconnected=true` is not auto-selected.
3. Single-candidate behavior remains unchanged: when only one registered eligible candidate advertises, the monitor selects that candidate according to the preexisting reconnect rules.
4. `setDefault()` remains passive: calling it updates default metadata but does not immediately switch `activeDevice`, seed `SessionStore`, or call reconnect.
5. If this sprint changes the current Core Flow rule that auto reconnect targets only the active badge, `docs/core-flow/badge-connectivity-lifecycle.md` is updated in this sprint and lower docs cite the revised rule instead of contradicting it.
6. Focused tests cover default-first dual-candidate selection, manually-disconnected default suppression, single-candidate unchanged behavior, and passive `setDefault()`.
7. Runtime evidence uses a cleared logcat window and includes `[DefaultPriority] switch` for the dual-candidate case.
8. `./gradlew :app-core:testDebugUnitTest` exits 0.
9. `./gradlew :app:assembleDebug` exits 0.

## Stop Exit Criteria

- Sprint 03 is not closed successfully; keep this sprint blocked.
- Default-first priority conflicts with `docs/core-flow/badge-connectivity-lifecycle.md` and the user does not approve changing the active-only reconnect rule.
- BLE scanner evidence cannot distinguish dual-candidate advertising from single-candidate advertising after three iterations; stop with the missing evidence named.
- The fix would make `setDefault()` switch devices immediately or reconnect without a separate user or monitor event; stop because that violates the passive default requirement.
- `./gradlew :app-core:testDebugUnitTest` or `./gradlew :app:assembleDebug` fails in an unrelated area and the failure cannot be isolated or fixed within the iteration bound.

## Iteration Bound

3 implementation iterations or 90 minutes, whichever hits first.

Each iteration must:

1. Confirm Sprint 03 is closed successfully before editing code.
2. Re-read the Core Flow auto-reconnect wording and decide whether doc sync is required.
3. Implement or refine the default-first candidate selection rule.
4. Run focused registry-manager tests.
5. Run the required full test and assemble commands before close.
6. Capture cleared-window logcat for the dual-candidate priority scenario before claiming runtime success.

## Required Evidence Format

Closeout must include:

- Focused test command output for the new or changed default-priority tests.
- `./gradlew :app-core:testDebugUnitTest` result.
- `./gradlew :app:assembleDebug` result.
- Cleared-window runtime evidence using:

```bash
adb logcat -c
adb logcat -d -s SmartSalesConn
```

The excerpt must include `[DefaultPriority] switch` for the dual-candidate case and evidence that `manuallyDisconnected=true` default was skipped.

## Iteration Ledger

### Iteration 1 - 2026-04-29

- Confirmed dependency: Sprint 03 is closed on `develop` at `d423437fa`.
- Re-read Core Flow active-only reconnect wording and found doc sync required.
- Implemented BLE detection candidate handling in `RealDeviceRegistryManager`: live registered candidates are marked `bleDetected`, eligible default candidate wins first, manually disconnected default is skipped, active or single eligible candidate remains the fallback, and active same-device detection calls reconnect directly.
- Added focused `RealDeviceRegistryManagerTest` coverage for default-first dual candidate, manually disconnected default suppression, single eligible candidate reconnect, and passive `setDefault()`.
- Ran focused and full app-core unit verification plus `:app:assembleDebug`; all passed.
- Installed updated `app-core-debug.apk` on `fc8ede3e` and captured a cleared SmartSalesConn launch window. The device showed only one restored registered badge (`Remaining Badge (...55:66)`) and no `[DefaultPriority] switch`, so the required dual-badge L3 branch remains blocked.

### L3 Device Loop Attempt - 2026-04-29

- Preserved app data because this sprint requires existing badge pairing and registry state.
- Captured multiple `fc8ede3e` manual-collaboration loops under `docs/projects/ui-ux-polish/evidence/04-default-first-priority/`.
- User-reported stale connected/power-cut behavior was not proven in a clean loop: `run-03-stale-after-powercut-logcat.txt` and `run-04-powercut-card-tap-logcat.txt` still showed live `Bat#...` notifications from the connected badge after the declared power-cut window.
- `run-05-adb-tap-powercut-card-logcat.txt` proved manual card switching was landing, but both badges answered with fresh `IP#192.168.0.102` and `IP#192.168.0.101`; no powered-off false positive was captured.
- `run-06-settled-tail-logcat.txt` captured a `[DefaultPriority] switch`, followed by reconnect and fresh `IP#192.168.0.101`, but this cannot close the sprint because `run-as com.smartsales.prism cat shared_prefs/device_registry.xml` showed only one persisted registered device: `14:C1:9F:D7:E4:06`.
- Negative manually-disconnected-default L3 evidence was not attempted after discovering the missing second persisted registry row; the branch remains covered only by unit tests until two registered physical badges are restored.

### Debug Simulation Loop - 2026-04-29

- Added debug-only connectivity modal controls: `seed dual`, `sim default`, and `sim manual default`.
- The controls are visible only in debug builds and route through the same registry BLE detection candidate handler used by the production monitor. Logs are prefixed with `[DebugSim]` so simulated evidence cannot be mistaken for physical scanner evidence.
- Built and installed `app-core-debug.apk` on `fc8ede3e`; first `adb install` attempt hung until `adb kill-server`, second install completed with `Success`.
- `run-07-debug-buttons-ui.xml` shows the debug controls rendered in the connectivity modal.
- `run-08-debug-sim-logcat.txt` and `run-08-debug-sim-registry.xml` prove the seed control created the two CHLE registry rows.
- `run-09-debug-sim-detect-logcat.txt` proves the simulated positive and negative selector branches:
  - `[DebugSim] BLE detection candidates default=14:C1:9F:D7:E3:EE active=14:C1:9F:D7:E4:06 manuallyDisconnectedDefault=false`
  - `[DefaultPriority] switch 14:C1:9F:D7:E4:06 -> 14:C1:9F:D7:E3:EE`
  - `[DebugSim] BLE detection candidates default=14:C1:9F:D7:E3:EE active=14:C1:9F:D7:E4:06 manuallyDisconnectedDefault=true`
  - `[DefaultPriority] skipped manuallyDisconnected default ...E3:EE`
- `run-10-debug-reset-registry.xml` confirms the debug seed reset both rows to `manuallyDisconnected=false` after the negative simulation.
- Verification after debug-button implementation:
  - `./gradlew :app-core:testDebugUnitTest --tests "*RealDeviceRegistryManagerTest*" --tests "*ConnectivityViewModelTest*"` -> `BUILD SUCCESSFUL in 18s`.
  - `./gradlew :app-core:assembleDebug` -> `BUILD SUCCESSFUL in 10s`.
  - `./gradlew :app-core:testDebugUnitTest` -> `BUILD SUCCESSFUL in 14s`.
- Status remains blocked for formal Sprint 04 close: debug simulation validates the selection path, but it is not physical dual-advertising L3 evidence from the BLE scanner.

### Debug Mode Visibility Slice - 2026-04-29

- Added a persisted debug-build-only `调试模式` toggle in the full-app User Center and SIM User Center drawer.
- Connectivity modal simulation controls now require both `BuildConfig.DEBUG` and the persisted debug mode to be enabled before rendering.
- The toggle only changes debug UI visibility; it does not seed registry state, switch active devices, reconnect, or alter release-build behavior.
- Focused verification after the toggle slice:
  - `./gradlew :app-core:testDebugUnitTest --tests "*DebugModeStoreTest*" --tests "*ConnectivityViewModelTest*" --tests "*ConnectivityViewModelRepairTest*" --tests "*UserCenterViewModelTest*"` -> `BUILD SUCCESSFUL in 17s`.
  - `./gradlew :app-core:testDebugUnitTest --tests "*UserCenterViewModelTest*"` -> `BUILD SUCCESSFUL in 10s` after adding the setter coverage.
  - `./gradlew :app-core:testDebugUnitTest` -> `BUILD SUCCESSFUL in 7s`.
  - `./gradlew :app-core:assembleDebug :app:assembleDebug` -> `BUILD SUCCESSFUL in 4s`.
- Installed `app-core-debug.apk` on `fc8ede3e` with preserved app data.
- Cleared-window device-loop evidence for the visibility gate:
  - `run-13-debug-mode-off-ui.xml` contains no `Debug probes`, `seed dual`, `sim default`, or `sim manual default`; `run-13-debug-mode-off-logcat.txt` captured the same launch window.
  - `run-14-debug-mode-on-ui.xml` contains `Debug probes`, `seed dual`, `sim default`, and `sim manual default`; `run-14-debug-mode-on-logcat.txt` captured the same launch window.

### L2.5 Deterministic Debug Ingress - 2026-04-29

- Escalated the connectivity debug-button approach to L2.5: deterministic device-installed synthetic ingress that enters the same `DeviceRegistryManager.handleBleDetectionCandidates` boundary as the BLE detection monitor.
- Added fixed scenario IDs and structured pass/fail results:
  - `CONNECTIVITY_DEFAULT_PRIORITY_DUAL_ADVERTISE`
  - `CONNECTIVITY_MANUAL_DEFAULT_SUPPRESSION`
- Renamed the UI buttons to `L2.5 default` and `L2.5 manual default`.
- Added required L2.5 telemetry:
  - `[L2.5][BEGIN]`
  - `[L2.5][ASSERT]`
  - `[L2.5][END] ... result=PASS evidenceClass=L2.5 authenticity=synthetic_not_physical_ble`
- Added focused unit coverage for both deterministic L2.5 scenarios.
- Synced the L2.5 evidence-class contract into `docs/specs/device-loop-protocol.md` and `docs/specs/harness-manifesto.md`.
- Verification:
  - `./gradlew :app-core:testDebugUnitTest --tests "*RealDeviceRegistryManagerTest*"` -> `BUILD SUCCESSFUL in 15s`.
  - `./gradlew :app-core:testDebugUnitTest` -> `BUILD SUCCESSFUL in 7s`.
  - `./gradlew :app-core:assembleDebug` -> `BUILD SUCCESSFUL in 4s`.
  - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL in 2s`.
- Installed `app-core-debug.apk` on `fc8ede3e` with preserved app data.
- L2.5 device-loop evidence:
  - `run-15-l25-before-ui.xml` proves the debug modal exposed `L2.5 default` and `L2.5 manual default`.
  - `run-20-l25-default-logcat.txt` proves `CONNECTIVITY_DEFAULT_PRIORITY_DUAL_ADVERTISE` entered the production candidate handler, emitted `[DefaultPriority] switch`, and ended with `[L2.5][END] ... result=PASS`.
  - `run-20-l25-default-ui.xml` shows `L2.5 CONNECTIVITY_DEFAULT_PRIORITY_DUAL_ADVERTISE: PASS selected=14:C1:9F:D7:E3:EE`.
  - `run-19-l25-manual-logcat.txt` proves `CONNECTIVITY_MANUAL_DEFAULT_SUPPRESSION` entered the production candidate handler, skipped the manually disconnected default, and ended with `[L2.5][END] ... result=PASS`.
  - `run-19-l25-manual-ui.xml` shows `L2.5 CONNECTIVITY_MANUAL_DEFAULT_SUPPRESSION: PASS selected=14:C1:9F:D7:E4:06`.
- Boundary: L2.5 now closes the app-side deterministic dataflow branch, but it remains synthetic ingress and does not replace physical dual-advertising BLE L3 evidence.

### Verification Pass - 2026-04-29

- Operator: Codex, using `smart-sales-device-loop` plus acceptance review.
- Verification gates:
  - `./gradlew :app-core:testDebugUnitTest --tests "*RealDeviceRegistryManagerTest*" --tests "*DeviceConnectionManagerReconnectSupportTest*"` -> `BUILD SUCCESSFUL in 16s`.
  - `./gradlew :app-core:testDebugUnitTest` -> `BUILD SUCCESSFUL in 8s`.
  - `./gradlew :app-core:assembleDebug` -> `BUILD SUCCESSFUL in 16s`.
  - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL in 7s`.
  - `adb -s fc8ede3e install -r app-core/build/outputs/apk/debug/app-core-debug.apk` -> `Success` after restarting adb for one hung install attempt.
- Fresh L2.5 evidence:
  - `run-verify-l25-before-ui.xml` proves `Debug probes`, `seed dual`, `L2.5 default`, and `L2.5 manual default` were visible in the installed debug APK.
  - `run-verify-l25-default-logcat.txt` contains `[L2.5][BEGIN]`, `[DefaultPriority] switch 14:C1:9F:D7:E4:06 -> 14:C1:9F:D7:E3:EE`, `[L2.5][ASSERT] ... pass=true`, and `[L2.5][END] ... result=PASS`.
  - `run-verify-l25-default-ui.xml` shows `L2.5 CONNECTIVITY_DEFAULT_PRIORITY_DUAL_ADVERTISE: PASS selected=14:C1:9F:D7:E3:EE`.
  - `run-verify-l25-manual-logcat.txt` contains `[L2.5][BEGIN]`, `[DefaultPriority] skipped manuallyDisconnected default ...E3:EE`, `[L2.5][ASSERT] ... pass=true`, and `[L2.5][END] ... result=PASS`.
  - `run-verify-l25-manual-ui.xml` shows `L2.5 CONNECTIVITY_MANUAL_DEFAULT_SUPPRESSION: PASS selected=14:C1:9F:D7:E4:06`.
- Fresh physical L3 attempt:
  - `run-verify-l3-pre-registry.xml` and `run-verify-l3-pre-session.xml` captured the restored real registry/session before the physical attempt.
  - `run-verify-l3-dual-badge-attempt-logcat.txt`, `run-verify-l3-dual-badge-attempt-ui.xml`, and `run-verify-l3-dual-badge-attempt.png` captured a 90-second physical window. The log contains no `[DefaultPriority] switch` and no `[DebugSim]`/`[L2.5]` contamination.
  - `run-verify-l3-card-switch-attempt-logcat.txt`, `run-verify-l3-card-switch-attempt-ui.xml`, and `run-verify-l3-card-switch-attempt.png` captured a second 90-second physical window after selecting the non-default card. The log proves both physical badges can establish GATT and HTTP-ready sessions (`14:C1:9F:D7:E3:EE` at `192.168.0.102`, `14:C1:9F:D7:E4:06` at `192.168.0.101`), but it still contains no scanner-monitor `[DefaultPriority] switch`.
  - `run-verify-l3-card-switch-post-registry.xml` and `run-verify-l3-card-switch-post-session.xml` show both badges registered, both `manuallyDisconnected=false`, and the final stored session on `14:C1:9F:D7:E4:06`.
- Verdict: app-side `platform-runtime/L2.5` is accepted, but formal Sprint 04 remains `blocked` because fresh physical L3 did not prove the required scanner-monitor default-priority branch.

### L3 Manual Collaboration Re-entry - 2026-04-29

- Rule consolidation: physical L3 cannot rely on implicit hardware choreography. The operator must declare the human manual collaboration test items before the capture window, then record whether each item was performed or blocked.
- Evidence class: physical `L3`.
- Scenario: default-first BLE scanner priority, with the production BLE scanner observing the default badge A and active badge B in the same scan window while active B is disconnected.
- Manual collaboration test items:

| Item | Human action owner | Device identity | Action and timing | Expected evidence | Pass/fail/block condition |
|---|---|---|---|---|---|
| L3-04-A | Human operator | Default badge A `14:C1:9F:D7:E3:EE` | Before `adb logcat -c`, ensure badge A is registered, powered on, advertising, in range, and not manually disconnected. | Registry shows A as `default_device_mac`; logcat or UI shows A eligible/in range. | Block if A cannot be powered/advertising/in range or remains `manuallyDisconnected=true`. |
| L3-04-B | Human operator | Active badge B `14:C1:9F:D7:E4:06` | Before `adb logcat -c`, ensure badge B is registered, powered on, advertising, in range, active, and not manually disconnected. | Registry/session shows B as active/stored session; logcat or UI shows B eligible/in range. | Block if B cannot be active, advertising, or in range. |
| L3-04-C | Human operator | Badges A + B | Immediately after `adb logcat -c`, keep both badges advertising in the same physical scan window and do not tap debug controls. If needed, physically bring both badges near the phone and wait up to 90 seconds. | `SmartSalesConn` contains real scanner/GATT evidence with no `[DebugSim]` or `[L2.5]`. | Pass only if the same window emits scanner-monitor `[DefaultPriority] switch ...E4:06 -> ...E3:EE`; block if the same-window dual-advertising condition cannot be confirmed. |
| L3-04-D | Human operator | App connectivity modal | During the capture window, only use real app controls needed to expose the connectivity surface; do not press `seed dual`, `L2.5 default`, or `L2.5 manual default`. | UI XML/screenshot shows physical device rows; logcat has no debug-sim markers. | Fail if debug controls are used; block if the physical device rows cannot be surfaced. |

- Re-entry verdict: blocked pending a human-confirmed L3 run that performs items L3-04-A through L3-04-D. Existing `run-verify-l3-*` artifacts remain valid negative attempts, but they did not declare these manual items before capture and therefore cannot close the physical L3 branch.
- L3-04-A check, 2026-04-29: blocked before capture. Device `fc8ede3e` registry shows default badge A `14:C1:9F:D7:E3:EE` as `default_device_mac` and `bleDetected=true`, but still `manuallyDisconnected=true`; session remains on active badge B `14:C1:9F:D7:E4:06`. Saved evidence: `run-l3-04-a-blocked-registry.xml`, `run-l3-04-a-blocked-session.xml`. Human operator must clear the manual-disconnect state through real app/device flow and confirm A is powered, advertising, and in range before rerunning `adb logcat -c`.
- L3-04-A recheck, 2026-04-29: performed with human confirmation that all badges were powered on and badge A was tapped through the real UI, which showed A connected. Evidence: `run-l3-04-a-confirmed-registry.xml` shows default badge A `14:C1:9F:D7:E3:EE` as `default_device_mac` with `manuallyDisconnected=false`; `run-l3-04-a-confirmed-session.xml` stores active session `peripheral_id=14:C1:9F:D7:E3:EE`; `run-l3-04-a-confirmed-ui.xml` shows the default row connected. Status: `L3-04-A` passed as a manual-collaboration precondition. Next blocker: `L3-04-B` still needs active badge B `14:C1:9F:D7:E4:06` restored through the real app/device flow before the dual-badge capture.
- L3-04-B recheck, 2026-04-29: observed the preserved physical device state after active badge B `14:C1:9F:D7:E4:06` had been restored through the real connectivity UI; no `seed dual`, `L2.5 default`, `L2.5 manual default`, or other debug precondition control was pressed in this check. Evidence: `run-l3-04-b-confirmed-registry.xml` shows badge B registered with `manuallyDisconnected=false` and badge A still present as `default_device_mac`; `run-l3-04-b-confirmed-session.xml` stores active session `peripheral_id=14:C1:9F:D7:E4:06` with `secure_token`, `wifi_password`, and embedded known-network password redacted; `run-l3-04-b-confirmed-ui.xml` shows the second physical row connected as `已连接 · 1.0.0.1` while the default row is not active. Status: `L3-04-B` passed as a manual-collaboration precondition. Next required item: `L3-04-C` must clear logcat, keep badges A and B advertising in the same physical scan window, capture real scanner/GATT evidence with no `[DebugSim]` or `[L2.5]` contamination, and pass only if the same window emits scanner-monitor `[DefaultPriority] switch ...E4:06 -> ...E3:EE`.
- L3-04-C monitor, 2026-04-29: cleared logcat and captured a physical dual-badge scan/GATT window without pressing `seed dual`, `L2.5 default`, `L2.5 manual default`, or other debug controls. Evidence: `run-l3-04-c-dual-scan-monitor-logcat.txt` shows both physical badges advertising in the same scan burst at `21:57:37` (`14:C1:9F:D7:E4:06` at rssi `-18` and `14:C1:9F:D7:E3:EE` at rssi `-41`), no `[DebugSim]` or `[L2.5]` contamination, GATT establishment for both badges, and HTTP reachability for badge B at `192.168.0.101`; `run-l3-04-c-dual-scan-monitor-registry.xml` shows both badges registered with `manuallyDisconnected=false`; `run-l3-04-c-dual-scan-monitor-session.xml` stores final session `peripheral_id=14:C1:9F:D7:E4:06` with secrets redacted; `run-l3-04-c-dual-scan-monitor-ui.xml` captures the post-window UI. Verdict: `L3-04-C` remains blocked because the window did not emit the required scanner-monitor `[DefaultPriority] switch ...E4:06 -> ...E3:EE`; the observed scan fallback selected badge B after the default badge A reachability path failed.
- L3-04-C resumed monitor, 2026-04-29: device `fc8ede3e` reconnected and the operator cleared logcat for a guided retest. The run was stopped as precondition-failed because the flow entered real `Select peripheral` / provisioning for badge B `14:C1:9F:D7:E4:06`, logged `Skipping auto-reconnect: user manually disconnected`, and ended with `default_device_mac=14:C1:9F:D7:E4:06` while the stored session was `peripheral_id=14:C1:9F:D7:E3:EE`. Evidence: `run-l3-04-c-resume-monitor-logcat.txt`, `run-l3-04-c-resume-monitor-registry.xml`, `run-l3-04-c-resume-monitor-session.xml`, and `run-l3-04-c-resume-monitor-ui.xml`. Verdict: invalid for the target default-first branch; before the next `L3-04-C` capture, restore the precondition so badge A `14:C1:9F:D7:E3:EE` is the default, badge B `14:C1:9F:D7:E4:06` is active/connected, and both rows have `manuallyDisconnected=false`.
- UI/session ghosting fix, 2026-04-29: the connectivity modal could show a stale registry active row as `connected` after scan fallback updated the stored/runtime `BleSession` to a different registered badge. Fix: `RealDeviceRegistryManager` now keeps `activeDevice` synchronized with registered session-bearing connection states (`Connected`, `WifiProvisioned`, `WifiProvisionedHttpDelayed`, `Syncing`) without changing the default badge. Regression evidence: `./gradlew :app-core:testDebugUnitTest --tests "*RealDeviceRegistryManagerTest*"` -> `BUILD SUCCESSFUL in 10s`; `./gradlew :app-core:assembleDebug` -> `BUILD SUCCESSFUL`; installed on `fc8ede3e` with preserved data. After-fix artifacts: `run-l3-ui-ghosting-after-fix-logcat.txt`, `run-l3-ui-ghosting-after-fix-registry.xml`, `run-l3-ui-ghosting-after-fix-session.xml`, and `run-l3-ui-ghosting-after-fix-ui.xml`. Runtime snapshot shows manager `Ready/CONNECTED`, session `peripheral_id=14:C1:9F:D7:E3:EE`, and default A `14:C1:9F:D7:E3:EE`; modal-row L3 proof still requires a human-surfaced physical connectivity modal window and does not close `L3-04-C`.
- L3-04-C modal-row monitor, 2026-04-29: preserved app data and captured the human-confirmed `B connected` pre-state before clearing logs. Evidence: `run-l3-04-c-modal-row-pre-registry.xml` shows A `14:C1:9F:D7:E3:EE` remains `default_device_mac`, both A and B are registered, and both have `manuallyDisconnected=false`; `run-l3-04-c-modal-row-pre-session.xml` stores active session `peripheral_id=14:C1:9F:D7:E4:06`; `run-l3-04-c-modal-row-pre-ui.xml` shows the real connectivity modal with A marked `默认`/`不在范围内`, B shown as `已连接 · 1.0.0.1`, and no `Debug probes`, `seed dual`, or `L2.5` controls. The operator then ran `adb -s fc8ede3e logcat -c`, waited 90 seconds with no debug-control taps, and saved `run-l3-04-c-modal-row-monitor-logcat.txt`, `run-l3-04-c-modal-row-monitor-full-logcat.txt`, `run-l3-04-c-modal-row-monitor-registry.xml`, `run-l3-04-c-modal-row-monitor-session.xml`, and `run-l3-04-c-modal-row-monitor-ui.xml`. Post-window state stayed stable with A default, B active, and the modal still showing B connected. Verdict: modal-row ghosting proof passed for the B-active/A-default UI state, but `L3-04-C` remains blocked because the app-filtered cleared log emitted only connected-badge battery notifications and did not emit `BT311Scan` scanner evidence or `[DefaultPriority] switch ...E4:06 -> ...E3:EE`; the full log only shows platform BLE/RSSI activity for connected B and no `[DebugSim]` or `[L2.5]` contamination.
- L3-04-C non-manual drop preflight, 2026-04-29: preserved app data and rechecked the corrected target pre-state before the next physical L3 retry. Evidence: `run-l3-04-c-nonmanual-drop-pre-registry.xml` shows default badge A `14:C1:9F:D7:E3:EE`, active badge B `14:C1:9F:D7:E4:06`, both registered, both `manuallyDisconnected=false`, and both `bleDetected=false`; `run-l3-04-c-nonmanual-drop-pre-session.xml` stores active session `peripheral_id=14:C1:9F:D7:E4:06`; `run-l3-04-c-nonmanual-drop-pre-ui.xml` shows the real modal with A marked `默认`/`不在范围内`, B shown as `已连接 · 1.0.0.1`, and no `Debug probes`, `seed dual`, or `L2.5` controls. Verdict: pre-state is ready for the corrected L3 choreography, but the capture is blocked until the human operator can, after `adb logcat -c`, non-manually drop B's GATT connection by physical power-cycle or equivalent, let B resume advertising, keep A advertising, and avoid the modal `断开连接` button because that sets manual-disconnect and suppresses scanning.
- L3-04-C non-manual drop monitor, 2026-04-29: operator cleared logcat at `23:16:04 CST`; human operator power-cut active badge B `14:C1:9F:D7:E4:06` without tapping the modal `断开连接` button. Evidence: `run-l3-04-c-nonmanual-drop-monitor-logcat.txt`, `run-l3-04-c-nonmanual-drop-monitor-full-logcat.txt`, `run-l3-04-c-nonmanual-drop-monitor-registry.xml`, `run-l3-04-c-nonmanual-drop-monitor-session.xml`, `run-l3-04-c-nonmanual-drop-monitor-ui.xml`, and `run-l3-04-c-nonmanual-drop-monitor.png`. Classification: real `Disconnected` transition was observed at `23:16:27.203`; BLE detection monitor scheduled and `BT311Scan` started for B; scanner saw default badge A `14:C1:9F:D7:E3:EE` repeatedly, but did not see B in the app-filtered registered candidate window; no `[DebugSim]` or `[L2.5]` contamination was present; no `[DefaultPriority] switch ...E4:06 -> ...E3:EE` was emitted. The runtime session moved to A through the lower scan fallback path (`Stored MAC ...E4:06 unreachable, attempting scan fallback` then `Scan fallback: found ...E3:EE`) rather than through the registry-manager default-priority selector. Verdict: still blocked for formal L3 because this was a single-candidate physical scanner window, not same-window A+B selector evidence.
- Diagnostic logging slice, 2026-04-29: after the corrected physical retry still failed to produce selector evidence, added temporary diagnostic logs only. `RealDeviceRegistryManager` now logs monitor state transitions, scan skip/stop/schedule reasons, known registered MACs, scan ticks, raw candidates, and selector inputs. `AndroidBleScanner` now logs stop results and delivered matched-device lists. No public API, schema, UI, or selector behavior changed. Verification:
  - `./gradlew :app-core:assembleDebug` -> `BUILD SUCCESSFUL in 21s`.
  - First parallel focused test attempt failed with a Gradle intermediate class read error while `:app-core:assembleDebug` was running concurrently; rerun serially.
  - `./gradlew :app-core:testDebugUnitTest --tests "*RealDeviceRegistryManagerTest*"` -> `BUILD SUCCESSFUL in 5s`.
  - `adb -s fc8ede3e install -r app-core/build/outputs/apk/debug/app-core-debug.apk` -> `Success`, with app data preserved for the next L3 diagnostic retry.
- Active-only replacement L3, 2026-04-29: captured the superseding product rule with default badge A `14:C1:9F:D7:E3:EE` and active badge B `14:C1:9F:D7:E4:06`, both registered and `manuallyDisconnected=false`. Precondition evidence: `run-l3-active-only-pre-registry.xml`, `run-l3-active-only-pre-session.xml`, and `run-l3-active-only-pre-ui.xml` show A as default, B as active/stored session, and B connected. Human operator then power-cut active badge B without tapping the modal disconnect control while keeping A powered/advertising, and repowered B inside the capture window. Evidence: `run-l3-active-only-monitor-logcat.txt` shows a real non-manual `Disconnected` transition at `23:41:31.172`, active-only monitor scheduling target `14:C1:9F:D7:E4:06`, repeated physical scanner sightings of default badge A without switching to A, then active-target scan fallback finding B at `23:41:39.155` while both A and B were in the delivered matched-device list, followed by `Persistent GATT session established: 14:C1:9F:D7:E4:06` and `Session refreshed for active MAC 14:C1:9F:D7:E4:06`. Post evidence: `run-l3-active-only-post-session.xml` keeps `peripheral_id=14:C1:9F:D7:E4:06`; `run-l3-active-only-post-registry.xml` keeps A as `default_device_mac` with A only `bleDetected=true`; `run-l3-active-only-post-ui.xml` shows A as `已检测到 · 点击连接` and B as the active disconnected/reconnectable row. Verdict: physical L3 passed for the active-only replacement rule: A did not silently become active, `SessionStore` was not rewritten to A, and B was the only automatic reconnect target.

## Closeout

- Status: `rejected`
- Supersession: On 2026-04-29 the product rule changed to latest-connected active-badge recovery. Sprint 04's default-first objective is product-rejected: accidental/non-manual disconnect of active badge B must not silently switch or reconnect to default badge A. Non-active/default badges may be marked `bleDetected` for UI proximity and may show reconnectable copy, but they become active only through explicit manual user action.
- Replacement rule: reconnect fallback scans only for the latest connected/current active session MAC for up to 60 seconds. If only another registered badge is found, `SessionStore` and `activeDevice` remain on the active badge. Manual disconnect of active badge B suppresses auto-reconnect to B and auto-connect to nearby badge A until explicit user action. The default badge remains cosmetic/passive UI metadata.
- One-liner for tracker: Product rule superseded the Sprint 04 default-first objective; automatic recovery now targets the latest connected active badge only.
- Evidence artifacts:
  - Focused test: `./gradlew :app-core:testDebugUnitTest --tests "*RealDeviceRegistryManagerTest*"` -> `BUILD SUCCESSFUL in 10s`, 12 registry-manager tests passed.
  - Full app-core unit suite: `./gradlew :app-core:testDebugUnitTest` -> `BUILD SUCCESSFUL in 14s`.
  - App debug build: `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL in 2s`.
  - Device visibility: `adb devices` -> `fc8ede3e device`.
  - Updated APK install: `adb -s fc8ede3e install -r app-core/build/outputs/apk/debug/app-core-debug.apk` -> `Success`.
  - Cleared runtime log attempt:

```text
adb -s fc8ede3e logcat -c
adb -s fc8ede3e shell monkey -p com.smartsales.prism -c android.intent.category.LAUNCHER 1
sleep 30
adb -s fc8ede3e logcat -d -s SmartSalesConn

04-29 19:56:39.939 26471 26471 D SmartSalesConn: Restored session: 11:22:33:44:55:66 knownNetworks=0
04-29 19:56:39.940 26471 26471 D SmartSalesConn: Registry: default device Remaining Badge (...55:66)
04-29 19:56:39.940 26471 26471 D SmartSalesConn: Restored session: 11:22:33:44:55:66 knownNetworks=0
04-29 19:56:39.942 26471 28961 I SmartSalesConn: BLE disconnected
04-29 19:56:39.942 26471 28961 D SmartSalesConn: Soft disconnect (session preserved)
04-29 19:56:39.943 26471 28961 D SmartSalesConn: Persistent GATT session closed
```

  - Negative evidence: the cleared log does not contain `[DefaultPriority] switch` because only one restored registered badge appeared; no dual-candidate advertising window was available to prove the branch.
- Lesson proposals: none.
- CHANGELOG line: none while blocked; user approval required before any product changelog entry.
