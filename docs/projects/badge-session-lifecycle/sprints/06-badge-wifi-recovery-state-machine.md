# Sprint 06 — badge-wifi-recovery-state-machine

## Header

- Project: badge-session-lifecycle
- Sprint: 06
- Slug: badge-wifi-recovery-state-machine
- Date authored: 2026-04-27
- Revision date: 2026-04-27
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):** "Sprint 06 Contract Plan: Badge Wi-Fi Recovery State Machine"

Implement the badge connectivity recovery model where BLE disconnection is a transport problem, while BLE-connected Wi-Fi/media failure is a bounded repair path. Wi-Fi success is judged by badge HTTP audio sync (`/list`, `/download`, `/delete`), not by phone SSID, and repair must preserve registered badge ownership without routing users through add-device pairing.

## Scope

Files the operator may touch:

- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/core-flow/badge-session-lifecycle.md`
- `docs/specs/esp32-protocol.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/projects/badge-session-lifecycle/tracker.md`
- `docs/projects/badge-session-lifecycle/sprints/06-badge-wifi-recovery-state-machine.md`
- `docs/projects/badge-session-lifecycle/evidence/06-badge-wifi-recovery-state-machine/**`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/**`
- `app-core/src/main/java/com/smartsales/prism/data/audio/**`
- `app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/**`
- Relevant focused tests under `app-core/src/test/java/com/smartsales/prism/data/connectivity/**` and `app-core/src/test/java/com/smartsales/prism/data/audio/**`

Out of scope:

- Firmware protocol changes.
- Broad refactors or god-file splits.
- Registry deletion, session clearing, or unregistering as a repair path.
- Routing registered-badge repair through add-device pairing.
- Harmony/platform branch work.

## References

- `AGENTS.md`
- `docs/specs/sprint-contract.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/core-flow/badge-session-lifecycle.md`
- `docs/specs/esp32-protocol.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/projects/badge-session-lifecycle/tracker.md`
- `docs/projects/badge-session-lifecycle/evidence/06-badge-wifi-recovery-state-machine/`

## Success Exit Criteria

1. BLE disconnected is logged and handled as transport recovery, not Wi-Fi/media repair.
2. BLE connected + badge `IP#0.0.0.0` prompts for Wi-Fi credentials without deleting registry rows or clearing session.
3. BLE connected + solid badge IP + HTTP sync failure silently replays the latest saved successful credential with at most 3 attempts.
4. Saved credential replay is single-flight for the active runtime and does not launch overlapping replay loops.
5. Manual credential repair is bounded to 3 attempts before hotspot/simple-network fallback guidance.
6. Phone SSID is not used as a decision input; it remains diagnostic only.
7. Multi-badge switching preserves active badge ownership: cached IP/list/download results from one badge cannot drive another badge.
8. ESP32 protection is visible in logs: no continuous background BLE Wi-Fi polling, no repeated `/list` hammering, and sync/readiness probing is skipped while a badge HTTP download is active.
9. Focused unit tests pass for recovery bridge, reconnect support, BLE gateway cache scoping, and badge audio sync ownership.
10. On-device logcat evidence covers multi-badge A/B/A switching and active-download sync taps.

## Stop Exit Criteria

- Hardware evidence shows cross-badge IP/download mismatch after the ownership guard.
- Hardware evidence shows overlapping unbounded credential replay loops.
- Hardware evidence shows registered-badge repair deleting registry/session state or routing through add-device pairing.
- The implementation requires firmware changes to satisfy the model.
- Iteration bound is reached without a green on-device multi-badge pass.

## Iteration Bound

3 implementation/test iterations or one hardware collaboration session, whichever hits first.

Each iteration must:

1. Reproduce or inspect logcat evidence.
2. Patch only the branch proven by evidence.
3. Run focused unit tests.
4. Install the APK when hardware validation is needed.
5. Capture and inspect fresh logcat evidence.

## Required Evidence Format

The closeout must include:

- Focused Gradle unit test command and result.
- Debug APK build/install command and result.
- Logcat artifact paths under `docs/projects/badge-session-lifecycle/evidence/06-badge-wifi-recovery-state-machine/`.
- Evidence notes for active badge identity/IP, saved credential replay, active-download skip, and registry/session preservation.

## Iteration Ledger

- Iteration 1: Implemented docs and recovery model alignment; added saved-credential replay after solid-IP HTTP media failure; added logging for transport/media branches and repair attempts.
- Iteration 2: Multi-badge evidence showed cross-badge ownership risk. Scoped badge audio placeholders, queued downloads, imports, progress, and stale list handling to the active runtime badge; keyed BLE network cache by peripheral ID.
- Iteration 3: Hardware evidence showed active ESP32 `/download` could make readiness probes timeout and falsely trigger Wi-Fi recovery. Added active-download guard so manual sync/readiness probing returns already-running without HTTP reachability or credential replay during an in-flight badge download.

## Closeout

- Status: success
- Summary: Badge Wi-Fi recovery now separates BLE transport loss from BLE-connected media failure, replays saved credentials in a bounded single-flight path, scopes audio sync/download work to the active badge, and skips sync readiness probes while an ESP32 download is active.
- Evidence:
  - Focused tests passed: `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.audio.SimAudioRepositorySyncSupportTest' --tests 'com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest' --tests 'com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManagerReconnectSupportTest' --tests 'com.smartsales.prism.data.connectivity.legacy.gateway.RateLimitedBleGatewayTest'`
  - Debug APK built: `./gradlew :app-core:assembleDebug`
  - Debug APK installed: `adb install -r -d app-core/build/outputs/apk/debug/app-core-debug.apk`
  - On-device multi-badge evidence: `docs/projects/badge-session-lifecycle/evidence/06-badge-wifi-recovery-state-machine/multi-badge-singleflight-clean-20260427-171124.txt`
  - On-device active-download guard evidence: `docs/projects/badge-session-lifecycle/evidence/06-badge-wifi-recovery-state-machine/multi-badge-active-download-guard-check-20260427-172318.txt`
  - Evidence summary: Badge A resolved `1C:DB:D4:9B:8F:96` at `192.168.0.107`; Badge B resolved `14:C1:9F:D7:E3:F6` at `192.168.0.108`; B `/list` succeeded with `count=20`; A `/list` succeeded with `count=1`; device switches logged cache skips and session preservation; saved credential replay joined in-flight work rather than launching overlapping loops; manual sync taps during active download logged `readiness probe skipped reason=badge-download-active` and `sync skipped trigger=manual reason=badge-download-active`.
  - User acceptance: user confirmed the final large-file download was intentionally canceled and approved closing Sprint 06.
- Lesson proposals: none.
- CHANGELOG line: none.
