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

## Closeout

- Status: `blocked`
- One-liner for tracker: Implementation and L1/L2 verification are green, but L3 dual-badge proof is blocked because device `fc8ede3e` currently exposes only one restored registered badge in cleared SmartSalesConn logs.
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
