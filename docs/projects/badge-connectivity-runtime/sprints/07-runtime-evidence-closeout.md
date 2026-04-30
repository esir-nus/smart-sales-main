# Soft Deprecation Notice

- Status: `soft-deprecated`
- Authority: historical/local reference only; this sprint is non-authoritative for current connectivity behavior.
- Blocker impact: none; the findings below are non-blocking future reference.
- Reason: post-wifi-gate-checking evidence was superseded by rollback and safer north-star realignment.
- Note: the original contract sections below are preserved for context and are not active operator instructions.
- Active sources of truth:
  - `docs/core-flow/badge-connectivity-lifecycle.md`
  - `docs/bake-contracts/connectivity-badge-session.md`
  - `docs/specs/esp32-protocol.md`

# Sprint 07 - runtime-evidence-closeout

## Header

- Project: badge-connectivity-runtime
- Sprint: 07
- Slug: runtime-evidence-closeout
- Date authored: 2026-04-30
- Author: Claude plan, Codex-authored contract from user-provided plan
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):** "Implement the plan in a fresh context."

Close the runtime evidence gap left by Sprints 03-05 without expanding the architecture delivered by Sprint 06. Gather final Android device/runtime proof for BLE-first connection truth, final media-window command order, queued remote delete timing, and same-badge scoping; close Sprints 03-05 only when the proof is strong enough, or explicitly block the hardware-dependent claims.

## Scope

Docs and evidence scope:

- `docs/projects/badge-connectivity-runtime/tracker.md`
- `docs/projects/badge-connectivity-runtime/sprints/03-ble-first-connection-state.md`
- `docs/projects/badge-connectivity-runtime/sprints/04-scoped-media-power-window.md`
- `docs/projects/badge-connectivity-runtime/sprints/05-queued-remote-delete-window.md`
- `docs/projects/badge-connectivity-runtime/sprints/07-runtime-evidence-closeout.md`
- `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/**`

Narrow code scope only if runtime evidence disproves an existing Sprint 03-05 claim:

- `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/**`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/**`
- `app-core/src/main/java/com/smartsales/prism/data/audio/**`
- `app-core/src/test/java/com/smartsales/prism/data/connectivity/**`
- `app-core/src/test/java/com/smartsales/prism/data/audio/**`
- focused fake, constructor, or debug-ingress call sites needed to prove the same runtime path

Out of scope:

- New media-runner architecture, UI redesign, Harmony lane edits, firmware implementation, changing Sprint 06's strict-gate production default, promoting opportunistic policy beyond testability, and broad fixes outside connectivity/audio runtime behavior.

## References

- `AGENTS.md`
- `.agent/rules/lessons-learned.md`
- `docs/reference/agent-lessons-details.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/specs/device-loop-protocol.md`
- `docs/specs/ship-time-checks.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/bake-contracts/connectivity-badge-session.md`
- `docs/cerb/interface-map.md`
- `docs/projects/badge-connectivity-runtime/tracker.md`
- `docs/projects/badge-connectivity-runtime/sprints/03-ble-first-connection-state.md`
- `docs/projects/badge-connectivity-runtime/sprints/04-scoped-media-power-window.md`
- `docs/projects/badge-connectivity-runtime/sprints/05-queued-remote-delete-window.md`
- `docs/projects/badge-connectivity-runtime/sprints/06-media-session-runner-fakes.md`
- `docs/sops/esp32-connectivity-debug.md`
- `scripts/device-verify-connectivity.sh`
- `scripts/ble_debug_full.sh`
- `scripts/ble_traffic.sh`

## Success Exit Criteria

1. Sprint 03 has fresh runtime evidence proving `BadgeConnectionState.Connected` can represent active registered-badge BLE GATT plus notification listener while `badgeIp` and `ssid` are nullable media hints.
2. Sprint 03 evidence proves idle media unavailability or `wifi#off` does not trigger reconnect, repair, saved-credential replay, or shared connection demotion.
3. Sprint 04 has fresh runtime or valid L2.5 evidence proving final product-path order after the WAV ack patch: `download#ready` -> `download#ok` -> media work -> WAV terminal -> success-only `download#end`.
4. Sprint 04 evidence proves failure or cancellation omits `download#end`.
5. Sprint 05 has fresh runtime or valid L2.5 evidence proving user delete removes local state immediately, queues remote tombstones, and flushes only inside the next runner-owned same-badge media session.
6. Badge scoping is proven: tombstones and endpoints for badge A do not flush or leak while badge B is active.
7. Sprint 06 policy boundary remains intact: production default is strict-gate, opportunistic remains testable/internal, and UI/repository code does not branch on policy names.
8. `./gradlew :app-core:testDebugUnitTest` passes or closeout records unrelated compiler/test output.
9. `./gradlew :app-core:assembleDebug` passes or closeout records unrelated compiler output.
10. `git diff --check` passes.
11. `adb devices` is captured in the evidence folder.
12. Filtered `adb logcat` artifacts are captured for any physical BLE/GATT, firmware command-order, endpoint reachability, or real queued-delete timing claim.
13. Sprint 03/04/05 closeout sections are changed to `success` only for claims whose required evidence is captured; otherwise they remain `blocked` or `in-progress` with exact blocker wording.
14. Project tracker rows for Sprint 03/04/05/07 and the required `Ongoing-justification` note are synchronized.

## Stop Exit Criteria

- Physical badge, pairing state, deterministic debug ingress, permissions, or app entry state cannot be established for the declared runtime proof.
- Runtime evidence disproves a Sprint 03-05 claim and the needed fix is broader than connectivity/audio runtime behavior.
- Runtime evidence remains ambiguous after targeted log capture and one rerun.
- Build or test failures block app installation and are not local to this sprint's narrow scope.
- The iteration bound is reached without enough evidence to close the hardware-dependent claims.

## Iteration Bound

2 evidence iterations, plus 1 narrow fix iteration only if evidence exposes a connectivity/audio runtime defect inside this sprint's code scope.

## Required Evidence Format

The closeout must include:

- Command output artifacts under `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/` for:
  - `./gradlew :app-core:testDebugUnitTest`
  - `./gradlew :app-core:assembleDebug`
  - `git diff --check`
  - `adb devices`
- Runtime artifacts under the same evidence folder for any attempted device scenario:
  - filtered `adb logcat` captures
  - `uiautomator` XML when an installed app scenario is run
  - explicit hardware/manual-action blocker notes when physical L3 cannot be performed
- Inline closeout excerpts naming the exact pass/fail/block condition for Sprint 03, Sprint 04, Sprint 05, and Sprint 07.

## Iteration Ledger

- Iteration 1 (2026-04-30): Read AGENTS/project rules, Sprint 03-06 contracts, project tracker, sprint schema, project-structure six-sprint rule, device-loop protocol, core flow, Cerb connectivity spec, ship-time checks, lessons index, and relevant lesson details for `HTTP Gate Conflating Connection Concerns`, `ESP32 Active Download vs Readiness Probe`, and `Reconnect Race Condition`. Authored Sprint 07 as a closeout/evidence sprint, created the evidence folder, and started static/build verification.
- Iteration 1 evidence (2026-04-30): `./gradlew :app-core:testDebugUnitTest` passed, `./gradlew :app-core:assembleDebug` passed, `git diff --check` passed, `adb devices` listed `fc8ede3e	device`, and `scripts/device-verify-connectivity.sh snapshot fc8ede3e` captured device/package permissions.
- Iteration 2 (2026-04-30): Installed `app-core-debug.apk` over the existing app without clearing data so existing badge/session state remained available. Launched `com.smartsales.prism/.MainActivity` and captured filtered logcat. The run proved a real badge session existed for active badge `14:C1:9F:D7:E4:06`: GATT connected, services discovered, persistent listener started, `tim#get` arrived, later `IP#192.168.0.103` / `SD#MstRobot` arrived, and the app sent `download#ready` during auto sync.
- Iteration 2 evaluation (2026-04-30, historical): Runtime proof blocked full Sprint 03-05 closure during the aborted post-backup verification run. Sprint 03 captured an initial `IP#0.0.0.0` branch that demoted/disconnected and scheduled reconnect, so nullable-media-hint/no-repair semantics were not physically closed in that run. Sprint 04 captured `download#ready` and failure omission of `download#end`, but firmware did not emit `download#ok`, so success order was not closed in that run. Sprint 05 had no valid delete-flush scenario because media-window open timed out before delete flush could be exercised. These are historical blockers from superseded local evidence, not current project blockers.

## Closeout

Status: soft-deprecated

Historical closeout status: blocked during aborted post-backup verification.

Tracker summary: This local Sprint 07 record captured real Android device and badge logs during a superseded post-wifi-gate-checking verification pass. Its blocked findings are retained as historical reference only: the observed `IP#0.0.0.0` demotion/reconnect branch, missing `download#ok`, and unavailable delete-flush scenario do not block current development or shipping. Current connectivity behavior is governed by `docs/core-flow/badge-connectivity-lifecycle.md`, `docs/bake-contracts/connectivity-badge-session.md`, and `docs/specs/esp32-protocol.md`.

Evidence artifacts:

- Unit verification: `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/testDebugUnitTest.txt` -> `BUILD SUCCESSFUL in 19s`.
- Debug build: `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/assembleDebug.txt` -> `BUILD SUCCESSFUL in 3s`.
- Static check: `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/git-diff-check.txt` -> `exit_code=0`.
- Device availability: `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/adb-devices.txt` -> `fc8ede3e	device`.
- Device/package snapshot: `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/device-snapshot.txt` -> Xiaomi `2410DPN6CC`, Android 16 / SDK 36, Bluetooth and notification permissions granted for `com.smartsales.prism`.
- Install/launch: `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/adb-install.txt` -> `Success`; `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/adb-launch.txt` -> launched `com.smartsales.prism/.MainActivity`.
- Runtime launch logcat: `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/run-01-launch-logcat.txt`.
- Extended runtime logcat: `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/run-02-extended-logcat.txt`.
- UI dumps: `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/run-01-launch-ui.xml` and `docs/projects/badge-connectivity-runtime/evidence/07-runtime-evidence-closeout/run-02-extended-ui.xml`.

Key runtime excerpts:

- Sprint 03 positive physical evidence: `BluetoothGatt onClientConnectionState connected=true`, `onSearchComplete status=0`, `setCharacteristicNotification()`, `Persistent GATT session established: 14:C1:9F:D7:E4:06`, `Starting persistent notification listener`, and `RX [Notification]: tim#get`.
- Sprint 03 historical blocker evidence from the aborted run: `RX [Notification]: IP#0.0.0.0`, `state=OFFLINE ip=0.0.0.0`, `badge IP#0.0.0.0 branch: prompt manual Wi-Fi repair, skip silent replay`, `state=Error`, `state=Disconnected`, and `scheduleAutoReconnect() delegating to deviceManager`.
- Sprint 04 positive failure-path evidence: `SIM audio badge sync requested: trigger=auto`, `TX [BadgeSignal]: download#ready`, `Badge media window ready timed out waiting for download#ok`, `media window open failed; download#end will not be sent`.
- Sprint 04 historical blocker evidence from the aborted run: no `download#ok`, no WAV terminal, and no success `download#end` appeared in the captured runtime window.
- Sprint 05 historical blocker evidence from the aborted run: no physical or L2.5 delete action was available, and media-window open timed out before any same-badge queued-delete flush could run.

Sprints updated:

- Sprint 03 closeout was set to `blocked` during the aborted post-backup verification run; this is historical and non-blocking.
- Sprint 04 closeout was set to `blocked` during the aborted post-backup verification run; this is historical and non-blocking.
- Sprint 05 closeout was set to `blocked` during the aborted post-backup verification run; this is historical and non-blocking.
- Project tracker row synchronization described here belongs to the superseded local run and is not authoritative after rollback.

Optional lesson proposals: none. The historical blockers are superseded local evidence gaps rather than confirmed fixed lessons or current project blockers.

Optional CHANGELOG line: none requested.
