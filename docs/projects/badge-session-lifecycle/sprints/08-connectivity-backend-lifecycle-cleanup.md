# Sprint 08 - connectivity-backend-lifecycle-cleanup

## Header

- Project: badge-session-lifecycle
- Sprint: 08
- Slug: connectivity-backend-lifecycle-cleanup
- Date authored: 2026-04-27
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):** "Sprint 08 Contract: Connectivity Backend Lifecycle Cleanup"

Revise the cleanup plan to be narrower and more honest than a broad "clean all god files" pass. This sprint targets only the backend reconnect, registered-badge repair, and HTTP media-readiness corridor. It may rewrite tangled private/internal support blocks when preserving them is riskier than rebuilding from the Core Flow, but public contracts and runtime behavior must be preserved unless the connectivity-bridge docs are synced in the same sprint.

## Scope

Files the operator may touch:

- `docs/projects/badge-session-lifecycle/tracker.md`
- `docs/projects/badge-session-lifecycle/sprints/08-connectivity-backend-lifecycle-cleanup.md`
- `docs/projects/badge-session-lifecycle/evidence/08-connectivity-backend-lifecycle-cleanup/**`
- `docs/cerb/connectivity-bridge/spec.md`, only if states, ownership, or public contracts change
- `docs/cerb/connectivity-bridge/interface.md`, only if states, ownership, or public contracts change
- `docs/cerb/interface-map.md`, only if module ownership edges change
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerConnectionSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityService.kt`, only if directly required by the corridor
- Focused connectivity tests under `app-core/src/test/java/com/smartsales/prism/data/connectivity/**`
- Focused source-structure tests under `app-core/src/test/java/com/smartsales/prism/ui/GodStructureGuardrailTest.kt` only if target file thresholds or guardrails change

Out of scope:

- Audio sync refactors beyond preserving the active-download/media-readiness guard behavior.
- BLE GATT internals, including `GattBleGateway.kt`, unless the audit proves a transport-level blocker inside this corridor.
- Setup/onboarding ViewModel cleanup.
- Connectivity UI redesign or copy polish.
- Registry deletion, session clearing, full re-pairing, or add-device pairing as registered-badge repair.
- Harmony work or `platform/harmony` edits.
- Public API changes unless `docs/cerb/connectivity-bridge/spec.md` and `docs/cerb/connectivity-bridge/interface.md` are synced in the same iteration.

Senior rule:

- Refactor public seams and lifecycle-sensitive contracts.
- Rewrite only private/internal tangled blocks when preserving them would be more dangerous than rebuilding from `docs/core-flow/badge-connectivity-lifecycle.md`.
- Do not chase LOC alone. A smaller confusing split is failure.

## References

- `AGENTS.md`
- `.agent/rules/lessons-learned.md`
- `docs/reference/agent-lessons-details.md`, sections "Reconnect Race Condition: Fire-and-Poll", "HTTP Gate Conflating Connection Concerns", "Application-Level Coupling vs Transport-Level Serialization", and "ESP32 Active Download vs Readiness Probe"
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/specs/ship-time-checks.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/core-flow/badge-session-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/cerb/interface-map.md`
- `docs/sops/esp32-connectivity-debug.md`
- `docs/projects/badge-session-lifecycle/tracker.md`
- `docs/projects/badge-session-lifecycle/sprints/06-badge-wifi-recovery-state-machine.md`
- `docs/projects/badge-session-lifecycle/sprints/07-connectivity-dataflow-simplification.md`
- `docs/projects/badge-session-lifecycle/evidence/06-badge-wifi-recovery-state-machine/`
- `docs/projects/badge-session-lifecycle/evidence/07-connectivity-dataflow-simplification/`

## Success Exit Criteria

1. Audit artifact exists at `docs/projects/badge-session-lifecycle/evidence/08-connectivity-backend-lifecycle-cleanup/audit.txt` and classifies every private/internal rewrite or public-seam refactor against the Core Flow and connectivity-bridge docs.
2. `DeviceConnectionManagerConnectionSupport.kt` and `RealConnectivityBridge.kt` are each `<= 650` lines by `wc -l`, or the audit records a valid tracked exception explaining why one file must remain over budget.
3. Reconnect remains suspend/result-driven and contains no fixed-delay fire-and-poll path, verified by focused tests and grep/diff review for reconnect wait logic.
4. BLE connected/shared transport state is not gated on HTTP media readiness; HTTP checks remain in feature-level readiness paths such as `ConnectivityBridge.isReady()`.
5. Media recovery remains single-flight, verified by tests and grep/diff review of the media recovery owner in `RealConnectivityBridge.kt`.
6. Registered-badge repair never routes through add-device pairing, registry deletion, or session clearing, verified by focused tests plus filtered logcat for the exercised repair/reconnect branch.
7. Active `/download` work is still protected from readiness probing or saved-credential replay, verified by focused tests or retained prior evidence plus unchanged guard behavior.
8. Public connectivity APIs are unchanged, or every public contract change is synced in `docs/cerb/connectivity-bridge/spec.md` and `docs/cerb/connectivity-bridge/interface.md` in the same iteration.
9. Focused tests pass at minimum: `ConnectivityStructureTest`, `GodStructureGuardrailTest`, `RealConnectivityBridgeTest`, `RealConnectivityServiceTest`, `DefaultDeviceConnectionManagerIngressTest`, and `DeviceConnectionManagerReconnectSupportTest`.
10. `./gradlew :app-core:compileDebugUnitTestKotlin` passes.
11. `./gradlew :app-core:assembleDebug` passes.
12. `adb install -r -d app-core/build/outputs/apk/debug/app-core-debug.apk` succeeds, or the closeout records adb/device unavailability as a blocker with lowered confidence.
13. Filtered `adb logcat` evidence is captured under `docs/projects/badge-session-lifecycle/evidence/08-connectivity-backend-lifecycle-cleanup/` for app launch plus the reconnect, repair, and media-readiness path available on current hardware.

## Stop Exit Criteria

- The audit proves this corridor cannot be cleaned without touching audio sync, BLE GATT internals, setup ViewModel cleanup, or UI redesign beyond the secondary scope above.
- A proposed change requires registry deletion, session clearing, add-device pairing, or full re-pairing as registered-badge repair.
- A public contract change is needed but cannot be reconciled with `docs/cerb/connectivity-bridge/spec.md` and `docs/cerb/connectivity-bridge/interface.md`.
- Evidence shows BLE/shared connected state is again gated on HTTP readiness.
- Evidence shows reconnect has regressed to fixed-delay fire-and-poll.
- Evidence shows media recovery can run concurrently for the same active runtime path.
- Hardware/runtime claims are needed but adb/logcat evidence cannot be captured.
- Iteration bound is reached without green focused tests, compile, debug build, and sufficient L3 evidence or a documented hardware blocker.

## Iteration Bound

4 backend implementation/test iterations.

Each iteration must:

1. Audit the corridor before changing code.
2. Patch only the targeted backend lifecycle path.
3. Prefer refactoring public seams and rewrite only private/internal tangled blocks with audit justification.
4. Run the focused tests for touched behavior.
5. Re-check line counts and public API/doc sync.
6. Build, install, and capture filtered logcat when runtime behavior is affected.

## Required Evidence Format

The closeout must include:

- Audit artifact: `docs/projects/badge-session-lifecycle/evidence/08-connectivity-backend-lifecycle-cleanup/audit.txt`.
- `wc -l` output for `DeviceConnectionManagerConnectionSupport.kt`, `RealConnectivityBridge.kt`, and any tracked exception.
- Focused Gradle test command and result covering the six named tests.
- `./gradlew :app-core:compileDebugUnitTestKotlin` command and result.
- `./gradlew :app-core:assembleDebug` command and result.
- `adb install -r -d app-core/build/outputs/apk/debug/app-core-debug.apk` command and result, or explicit adb/device blocker.
- Filtered logcat artifact paths for app launch plus reconnect, repair, and media-readiness branches available on current hardware.
- Diff-review note for public connectivity APIs and `DeviceConnectionManager.kt`.
- Tracker/doc sync note naming every doc touched or explicitly confirming no public contract changed.

## Iteration Ledger

- Iteration 1: Read the Sprint 08 contract, connectivity/session Core Flow docs, connectivity-bridge spec/interface, lessons for reconnect/HTTP/media recovery, and current target code. Audited the corridor and found the core behavior already aligned: reconnect uses `reconnectAndWait()`, shared BLE/network state remains separate from HTTP readiness, registered-badge repair stays on `updateWifiConfig()`/manual confirmation, and media recovery is single-flight. Performed private/internal cleanup only: tightened repeated pairing-state and Wi-Fi-disconnected error construction in `DeviceConnectionManagerConnectionSupport.kt`, simplified request/query/replay/repair branches without changing public APIs, and trimmed `RealConnectivityBridge.kt` comments/blank-line churn. Focused tests, compile, and debug build passed. adb install/logcat verification was blocked because `adb devices` returned no attached devices.

## Closeout

- Status: blocked
- Summary: Backend lifecycle cleanup met local structure/test/build gates and reduced the two target files below 650 LOC while preserving public contracts, but the sprint cannot close successfully because adb install and filtered logcat L3 evidence were unavailable.
- Evidence:
  - Audit artifact: `docs/projects/badge-session-lifecycle/evidence/08-connectivity-backend-lifecycle-cleanup/audit.txt`
  - Line counts after cleanup: `DeviceConnectionManagerConnectionSupport.kt` 601 LOC, `RealConnectivityBridge.kt` 634 LOC, `RealConnectivityService.kt` 203 LOC.
  - Focused tests passed: `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.connectivity.ConnectivityStructureTest' --tests 'com.smartsales.prism.ui.GodStructureGuardrailTest' --tests 'com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest' --tests 'com.smartsales.prism.data.connectivity.RealConnectivityServiceTest' --tests 'com.smartsales.prism.data.connectivity.legacy.DefaultDeviceConnectionManagerIngressTest' --tests 'com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManagerReconnectSupportTest'`
  - Compile passed: `./gradlew :app-core:compileDebugUnitTestKotlin`
  - Debug build passed: `./gradlew :app-core:assembleDebug`
  - adb blocker: `adb devices` returned no attached devices; `adb install -r -d app-core/build/outputs/apk/debug/app-core-debug.apk` returned `adb: no devices/emulators found`.
  - Public API review: no domain connectivity interfaces, `DeviceConnectionManager.kt`, or connectivity-bridge docs changed.
- Lesson proposals: none.
- CHANGELOG line: none.
