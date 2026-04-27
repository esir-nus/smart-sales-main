# Sprint 07 - connectivity-dataflow-simplification

## Header

- Project: badge-session-lifecycle
- Sprint: 07
- Slug: connectivity-dataflow-simplification
- Date authored: 2026-04-27
- Revision date: 2026-04-27
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):** "Sprint Contract Plan: Connectivity Data-Flow Weight Loss"

Operate a same-surface cleanup sprint after Sprint 06's working recovery model. The sprint is code weight loss: remove deprecated or duplicated connectivity data-flow layers only when evidence proves they are stale, and simplify patch stacks into clearer single-owner flow. No new user-visible capability is authorized, except evidence-backed bug fixes discovered during the on-device loop.

## Scope

Files the operator may touch:

- `docs/projects/badge-session-lifecycle/tracker.md`
- `docs/projects/badge-session-lifecycle/sprints/07-connectivity-dataflow-simplification.md`
- `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/**`
- `docs/cerb/connectivity-bridge/spec.md`, only if states, ownership, or public contracts change
- `docs/cerb/connectivity-bridge/interface.md`, only if states, ownership, or public contracts change
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/**`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`, only where transient UI state duplicates, masks, or fights real connectivity state
- `app-core/src/test/java/com/smartsales/prism/data/connectivity/**`
- Focused `app-core/src/test/java/**` tests for `DeviceConnectionManager*`, `RealConnectivityBridge`, `BadgeEndpointRecoveryCoordinator`, `RealConnectivityService`, `ConnectivityViewModel`, and `SimAudioRepositorySyncSupport`

Named implementation targets include:

- `DeviceConnectionManager*`
- `RealConnectivityBridge`
- `BadgeEndpointRecoveryCoordinator`
- `RealConnectivityService`
- focused connectivity tests

Out of scope:

- New UX features or visible product-surface expansion.
- `ConnectivityModal` visual redesign.
- Firmware protocol changes.
- Harmony work or `platform/harmony` edits.
- Registry deletion, session clearing, full re-pairing, or add-device pairing as registered-badge repair.
- Deleting registry concepts, device-management contracts, or public connectivity APIs without replacing an equivalent removed owner.
- Broad audio refactors beyond the active-download sync guard exception in `SimAudioRepositorySyncSupport.kt`.

## References

- `AGENTS.md`
- `.agent/rules/lessons-learned.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/specs/ship-time-checks.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/core-flow/badge-session-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/cerb/interface-map.md`
- `docs/projects/badge-session-lifecycle/tracker.md`
- `docs/projects/badge-session-lifecycle/sprints/06-badge-wifi-recovery-state-machine.md`
- `docs/projects/badge-session-lifecycle/evidence/06-badge-wifi-recovery-state-machine/`

## Success Exit Criteria

1. `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/audit.txt` exists and lists each removed or simplified branch with Core Flow, grep, focused test, or logcat justification.
2. Remaining phone Wi-Fi / SSID uses are classified in the audit as diagnostic, prompt prefill, or removable, verified with `rg -n "currentPhoneSsid|phoneSsid|PhoneWifi|PhoneWifiSnapshot|currentNormalizedSsid"`.
3. Registered-badge recovery paths do not route through add-device pairing, registry deletion, or session clearing; this is proven by grep/diff review and L3 logcat for the exercised recovery branch.
4. Active-download guard remains present, verified by `rg -n "badge-download-active" app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt app-core/src/test/java`.
5. Single-flight media recovery remains present or is replaced by an equally tested single owner, verified by `rg -n "mediaRecoveryInFlight|mediaRecoveryMutex|joining in-flight replay" app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt` or by an updated audit entry naming the replacement owner and tests.
6. No new public connectivity API is added unless replacing a removed equivalent, verified by diff review of domain connectivity interfaces and `DeviceConnectionManager.kt`.
7. Focused Gradle tests pass for every touched connectivity or audio sync path.
8. `./gradlew :app-core:assembleDebug` passes.
9. `adb install -r -d app-core/build/outputs/apk/debug/app-core-debug.apk` succeeds, or the closeout records adb unavailability as a blocker with lowered confidence.
10. L3 logcat evidence under `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/` covers reconnect, Wi-Fi repair or HTTP-delayed recovery, active-download manual sync, and multi-device switch when hardware allows.
11. `git diff --stat` or `wc -l` before/after evidence is captured for at least one simplified file.
12. Before/after notes prove the simplified path still emits enough log tags to diagnose reconnect, media recovery, active-download guard, and active-device switch failures.
13. User-visible surface remains functionally unchanged except evidence-backed bug fixes discovered during the on-device loop.

## Stop Exit Criteria

- Evidence shows cross-badge IP, endpoint, list, download, progress, or recovery leakage after a simplification.
- A valid Core Flow branch is removed, flattened, or made unreachable.
- The implementation requires firmware changes to satisfy the current lifecycle.
- Hardware claims are needed but adb/logcat evidence cannot be captured.
- A proposed cleanup requires `ConnectivityModal` redesign, Harmony work, registry deletion, session clearing, full re-pairing, or add-device pairing as registered-badge repair.
- Iteration bound is reached without green focused tests plus enough L3 evidence to prove the affected runtime branches.

## Iteration Bound

3 implementation/test iterations.

Each iteration must:

1. Audit the candidate branch before deleting or simplifying it.
2. Patch only branches proven stale, duplicated, or fighting the Core Flow.
3. Run focused tests for the touched path.
4. Build and install the APK when runtime behavior is affected.
5. Capture or update filtered logcat evidence before claiming runtime behavior.

## Required Evidence Format

The closeout must include:

- Audit artifact: `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/audit.txt`.
- Focused Gradle unit test command and result.
- Debug APK build command and result.
- Debug APK install command and result, or explicit adb blocker.
- Filtered logcat artifact paths under `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/`.
- `git diff --stat` or `wc -l` before/after evidence for at least one simplified file.
- Before/after diagnostic-log notes for the simplified path.
- Diff-review note for public connectivity interfaces and `DeviceConnectionManager.kt`.

## Iteration Ledger

- Iteration 1: Audited Sprint 06 recovery model, current Core Flow, connectivity bridge spec/interface, lessons for reconnect/HTTP/media download, and current code. Removed stale `WifiDisconnectedReason` values for phone-Wi-Fi-derived and HTTP-unreachable reconnect branches that production code no longer emits after Sprint 06; simplified `RealConnectivityService` and setup error mappings; deleted synthetic tests for those impossible branches. Focused connectivity/audio/ViewModel tests passed; debug APK built and installed. Filtered logcat was captured after install, but branch-specific L3 evidence for reconnect, repair/HTTP-delayed recovery, active-download manual sync, and multi-device switch was not exercised in this turn, so the sprint stops short of success.
- Iteration 2: Continued the on-device loop after the user confirmed one badge was available and authorized skipping hard manual-collaboration checks. Restored adb visibility, started the installed debug app, captured bounded one-badge reconnect evidence, and confirmed the registered badge path stayed on auto-reconnect/BLE detection with GATT timeout rather than add-device pairing, registry deletion, or session clearing. Multi-device switch was skipped because a second badge is unavailable. Active-download manual sync and Wi-Fi repair / HTTP-delayed recovery were skipped because they require manual timing or prerequisite badge GATT/HTTP readiness that the available badge did not reach during the bounded loop. Code grep, focused tests, and preserved diagnostic logs remain the acceptance evidence for those non-exercised paths.

## Closeout

- Status: done
- Summary: Removed stale phone-Wi-Fi-derived and HTTP-unreachable reconnect error branches from the legacy connectivity data flow with green focused tests, debug build/install, and constrained one-badge L3 evidence. Hardware/manual-only branches were explicitly skipped under the user's one-badge and no-hard-manual-collaboration constraints; no user-visible surface or public connectivity API was added.
- Evidence:
  - Audit artifact: `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/audit.txt`
  - Filtered install-smoke logcat: `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/logcat-install-smoke-20260427.txt`
  - One-badge reconnect L3 logcat: `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/l3-loop-20260427-184931.txt`
  - Additional bounded L3 captures:
    - `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/l3-one-badge-reconnect-20260427-185235.txt`
    - `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/l3-one-badge-loop-20260427-185459.txt`
    - `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/l3-package-audio-drawer-20260427-185812.txt`
    - `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/sprint07-current-screen.png`
  - Focused tests passed: `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.connectivity.RealConnectivityServiceTest' --tests 'com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest' --tests 'com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManagerReconnectSupportTest' --tests 'com.smartsales.prism.data.connectivity.legacy.DefaultDeviceConnectionManagerIngressTest' --tests 'com.smartsales.prism.data.audio.SimAudioRepositorySyncSupportTest' --tests 'com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest' --tests 'com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelRepairTest'`
  - Debug APK built: `./gradlew :app-core:assembleDebug`
  - Debug APK installed: `adb install -r -d app-core/build/outputs/apk/debug/app-core-debug.apk`
  - Device available: `adb devices` reported `fc8ede3e device`
  - Line-count evidence: measured files dropped from 1753 lines to 1543 lines, net 210-line reduction.
  - One-badge L3 reconnect evidence: `ConnectivityVM` delegated auto-reconnect to `ConnectivityService`, BLE detected `CHLE_Intelligent (1C:DB:D4:9B:8F:96)`, auto-reconnect triggered for that MAC, then GATT timed out. This proves the exercised registered-badge recovery branch did not route through add-device pairing, registry deletion, or session clearing.
  - Skipped L3 paths: multi-device switch because only one badge is available; active-download manual sync because it requires manual timing of an active badge download; Wi-Fi repair / HTTP-delayed recovery because the available badge did not reach the prerequisite GATT/HTTP state during the bounded loop.
- Lesson proposals: none.
- CHANGELOG line: none.
