# Sprint Contract: 04-a-connectivity-tcf-delta-readiness-queue-cleanup

## 1. Header

- **Slug**: 04-a-connectivity-tcf-delta-readiness-queue-cleanup
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Prior-agent delta validation plan
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Validate whether the BAKE practice can produce cleaner code, not
just better docs, by closing two small connectivity TCF deltas with code,
focused tests, and device evidence.

**Codex interpretation**: This sprint attaches to Sprint 02/03/04 connectivity
lineage. It closes the HTTP-delayed readiness flattening gap and the manual
badge-download queue ownership gap without changing `ConnectivityBridge`
method signatures, then records which runtime evidence is proven or blocked.

## 3. Scope

Allowed write scope:

- `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/BadgeManagerStatus.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryRuntime.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
- Focused tests for the touched connectivity/audio paths under `app-core/src/test/java/`
- `docs/bake-contracts/connectivity-badge-session.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/04-a-connectivity-tcf-delta-readiness-queue-cleanup.md`
- `docs/projects/bake-transformation/evidence/04-a-connectivity-tcf-delta-readiness-queue-cleanup/`

Out of scope:

- `ConnectivityBridge` method signature changes.
- New DBM/TCF expansion beyond this delta validation.
- Physical Cerb archival or broad doc reorganization.
- Existing dirty `docs/projects/dashboard/index.html`.

## 4. References

- `docs/projects/bake-transformation/sprints/02-connectivity-dbm.md`
- `docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`
- `docs/projects/bake-transformation/sprints/03-connectivity-tcf.md`
- `docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/core-flow/badge-session-lifecycle.md`
- `docs/bake-contracts/connectivity-badge-session.md`
- `docs/specs/device-loop-protocol.md`
- `.agent/rules/lessons-learned.md`

## 5. Success Exit Criteria

1. `BadgeManagerStatus.HttpDelayed(badgeIp: String? = null, ssid: String? = null, baseUrl: String)`
   exists and `WifiProvisionedHttpDelayed` maps to it rather than
   `BadgeManagerStatus.Ready`.
   Verify: `rg -n "HttpDelayed|WifiProvisionedHttpDelayed" data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/BadgeManagerStatus.kt app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt`

2. Connectivity manager UI has a distinct `ConnectivityManagerState.HTTP_DELAYED`
   and focused ViewModel coverage proves shell transport `CONNECTED` can coexist
   with manager `HTTP_DELAYED`.
   Verify: `rg -n "HTTP_DELAYED|managerState maps http delayed" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelTest.kt`

3. Manual queued badge downloads carry normalized filename plus owner badge MAC,
   and focused tests prove stale queued work does not call `downloadRecording()`
   through the wrong badge.
   Verify: `rg -n "SimBadgeQueuedDownload|ownerBadgeMac|queued badge download owner prevents wrong badge" app-core/src/main/java/com/smartsales/prism/data/audio app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupportTest.kt`

4. Focused L1/L2 tests pass:
   `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest --tests com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest --tests com.smartsales.prism.data.audio.SimAudioRepositorySyncSupportTest`

5. Affected-module regression runs:
   `./gradlew :app-core:testDebugUnitTest`

6. Android L3 evidence is captured under
   `docs/projects/bake-transformation/evidence/04-a-connectivity-tcf-delta-readiness-queue-cleanup/`
   or explicitly recorded as blocked with `adb devices` output.

7. `docs/bake-contracts/connectivity-badge-session.md` and
   `docs/projects/bake-transformation/tracker.md` reflect the delta close state.

## 6. Stop Exit Criteria

- **Signature blocker**: Closing the delta requires changing
  `ConnectivityBridge` method signatures.
- **Scope blocker**: The implementation requires broad registry, BLE gateway, or
  audio auto-downloader rewrites outside this validation slice.
- **Test blocker**: Focused tests or full `:app-core:testDebugUnitTest` fail for
  this change after two fix iterations.
- **Device blocker**: No Android device/emulator is visible, install fails for
  environment reasons, or hardware state prevents exercising the declared
  connectivity/audio path. Record blocker evidence rather than claiming L3 pass.
- **Iteration bound hit**: Stop rather than grind past the bound.

## 7. Iteration Bound

2 implementation/test iterations plus 1 device-evidence attempt.

## 8. Required Evidence Format

Closeout must include:

1. Focused L1/L2 Gradle command and result.
2. Full `:app-core:testDebugUnitTest` command and result, or the exact failure.
3. `./gradlew :app-core:assembleDebug` result.
4. `adb devices` output.
5. Saved L3 artifacts or explicit blocked note under
   `docs/projects/bake-transformation/evidence/04-a-connectivity-tcf-delta-readiness-queue-cleanup/`.
6. `git diff --stat` for scoped code/docs.

## 9. Iteration Ledger

- Iteration 1 — Read repo rules, Smart Sales skills, sprint-contract and
  device-loop specs, BAKE tracker, Sprint 02/03/04 connectivity lineage,
  connectivity/session core flows, the connectivity BAKE contract, and relevant
  lessons for HTTP readiness, reconnect, and active downloads. Implemented
  `BadgeManagerStatus.HttpDelayed`, manager `HTTP_DELAYED`, HTTP-delayed runtime
  key coverage, and badge-owned manual queue items. Added focused bridge,
  ViewModel, and sync-support tests. First focused Gradle run failed because
  `SimAudioRepositoryRuntime.queuedBadgeDownloads` publicly exposed an internal
  queue item type; changed the property to `internal`.
- Iteration 2 — Re-ran the focused L1/L2 test set successfully. Synced the
  connectivity BAKE contract to mark the HTTP-delayed manager state, delayed
  runtime identity, and badge-owned manual queue deltas closed. The first full
  `:app-core:testDebugUnitTest` run exposed test-harness drift: repository
  namespace tests created `SimAudioRepository` with unstubbed mock flows, and
  the drawer live-observation guardrail no longer recognized the current eager
  `combine(...)` entries shape while also catching battery notifications still
  using `WhileSubscribed`. Stubbed the mock flows, widened the structure check
  for eager `combine(...)`, and made badge battery observation eager.
- Device evidence attempt — Built and installed `app-core-debug.apk` on
  `fc8ede3e`, cleared app data, launched `MainActivity`, and captured filtered
  logcat, UI XML, and screenshot. The run proves clean launch/auto-reconnect
  scheduling with no registered badges. Hardware badge restore, HTTP-delayed
  readiness, and live audio-sync branches remain blocked by no registered badge
  after `pm clear`; no hardware-ready claim is made.
- Device evidence resume — After the user confirmed the device and badge were
  connected, resumed the L3 loop without clearing app data. Cold launch restored
  the registered badge session, established BLE/GATT, observed badge IP/SSID,
  proved HTTP readiness with a 200 reachability probe, ran auto audio sync,
  listed one badge file, and started a badge-owned download with
  `badgeMac=1C:DB:D4:9B:8F:96`. The bounded capture did not observe terminal
  import; the file was still downloading when the after-sync snapshot was
  captured. HTTP-delayed and active-device switch hardware branches were not
  naturally exercised.
- Device evidence re-sync — After the user allowed a re-sync attempt, tapped
  manual sync while `2speakerTesting.wav` was already downloading. The tap
  reached the manual sync ingress, detected `badge-download-active`, and skipped
  duplicate sync/download work. A later 45-second bounded wait still showed the
  original active transfer (`downloading=1 queued=0 holding=0`) and no terminal
  import success/failure; terminal import remains unverified rather than
  claimed.
- Device evidence terminal-import follow-up — Rechecked the drawer after the
  long-running badge download. `2speakerTesting.wav` now appears as a normal
  imported row with `右滑开始转写 >>>`, and the drawer count is `20 项`. The exact
  terminal import log line had aged out of filtered logcat, so the proof is UI
  terminal state plus the prior run-02 through run-04 lineage for the same file.
- Device evidence switch-branch check — Opened badge management to see whether
  active-device switch could be exercised. The surface shows only one registered
  badge row (`CHLE_Intelligent`, `默认`) and no second registered badge to switch
  to; it also shows disconnected/isolation state (`不在范围内`, `设备断开 — 网络可能正在隔离设备`).
  Active-device switch remains blocked without pairing/restoring a second badge.
  HTTP-delayed remains unproven because the visible state is disconnected, not
  transport-confirmed/media-delayed.
- Device evidence HTTP-readiness probe — Ran the badge-management `isReady`
  debug probe. It checked `http://192.168.0.102:8088`, received HTTP 200, and
  rendered `media readiness: true`. This confirms the current hardware state is
  HTTP-ready, not HTTP-delayed, so the HTTP-delayed branch remains unproven
  rather than failed.
- Device evidence active-switch closure — After the user restored two badges,
  attempted the active-device switch path. Logcat proves registry switch from
  `...8F:96` to `...E3:F6`, cancellation of outgoing badge downloads for
  `1C:DB:D4:9B:8F:96`, restored target session
  `14:C1:9F:D7:E3:F6`, persistent GATT, and transport-connected
  `ip=192.168.0.100 ssid=MstRobot`. This closes active-device switch L3 and
  confirms the badge-owned queue cleanup is not only a unit-test claim.
- Device evidence HTTP-abnormal follow-up — The same switch exposed a real
  transport-confirmed/HTTP-failed state: network status was connected with
  `192.168.0.100`, but HTTP reachability to `http://192.168.0.100:8088` failed
  and audio sync entered saved-credential replay/grace handling. A later UI dump
  showed `连接后网络检测异常`, `徽章已接入网络，但无法通过 HTTP 访问`, and
  `徽章 IP：192.168.0.100`. This gives L3 evidence for the user-facing
  HTTP-abnormal branch, while the exact manager enum `HTTP_DELAYED` remains
  L1/L2-proven rather than directly logged on-device.
- Device evidence reconnect after HTTP-abnormal — From the HTTP-abnormal surface,
  tapped `reconnect`. The ViewModel started at `effectiveState=WIFI_MISMATCH`,
  `ConnectivityService.reconnect()` returned connected, and the ViewModel ended
  with `effective=CONNECTED`. Audio sync stayed fenced with
  `reason=known_http_unreachable`, proving shell reconnection and HTTP media
  readiness stay separate during recovery.

## 10. Closeout

- **Status**: success
- **Tracker summary**: Closed HTTP-delayed readiness and badge-owned manual queue
  deltas with focused/full unit tests; L3 restored-badge, terminal import,
  active-device switch fencing, HTTP-abnormal UI, and reconnect-vs-media
  fencing evidence captured. The exact on-device manager enum name remains
  covered by L1/L2 tests rather than direct runtime telemetry.
- **Files changed**:
  - `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/BadgeManagerStatus.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt`
  - `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`
  - `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryRuntime.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
  - `app-core/src/test/java/com/smartsales/prism/data/connectivity/RealConnectivityBridgeTest.kt`
  - `app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelTest.kt`
  - `app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupportTest.kt`
  - `app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositoryNamespaceTest.kt`
  - `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAudioDrawerLiveObservationTest.kt`
  - `docs/bake-contracts/connectivity-badge-session.md`
  - `docs/projects/bake-transformation/tracker.md`
  - `docs/projects/bake-transformation/sprints/04-a-connectivity-tcf-delta-readiness-queue-cleanup.md`
  - `docs/projects/bake-transformation/evidence/04-a-connectivity-tcf-delta-readiness-queue-cleanup/`
- **Evidence**:

```text
$ ./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest --tests com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest --tests com.smartsales.prism.data.audio.SimAudioRepositorySyncSupportTest
BUILD SUCCESSFUL in 48s

$ ./gradlew :app-core:testDebugUnitTest
BUILD SUCCESSFUL in 11s

$ ./gradlew :app-core:assembleDebug
BUILD SUCCESSFUL in 14s

$ adb devices
List of devices attached
fc8ede3e	device

$ adb install -r app-core/build/outputs/apk/debug/app-core-debug.apk
Performing Streamed Install
Success

$ rg -n "HttpDelayed|WifiProvisionedHttpDelayed" data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/BadgeManagerStatus.kt app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt
data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/BadgeManagerStatus.kt:35:    data class HttpDelayed(
app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt:349:            is ConnectionState.WifiProvisionedHttpDelayed -> BadgeManagerStatus.HttpDelayed(

$ rg -n "HTTP_DELAYED|managerState maps http delayed" app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelTest.kt
app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt:293:            is BadgeManagerStatus.HttpDelayed -> ConnectivityManagerState.HTTP_DELAYED
app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt:602:    HTTP_DELAYED,
app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelTest.kt:121:    fun `managerState maps http delayed while shell transport remains connected`() = runTest {

$ rg -n "SimBadgeQueuedDownload|queued badge download owner prevents wrong badge" app-core/src/main/java/com/smartsales/prism/data/audio app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupportTest.kt
app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryRuntime.kt:47:    internal val queuedBadgeDownloads = LinkedHashSet<SimBadgeQueuedDownload>()
app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryRuntime.kt:80:internal data class SimBadgeQueuedDownload(
app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupportTest.kt:560:    fun `queued badge download owner prevents wrong badge download after active switch`() = runTest {

$ ls -la docs/projects/bake-transformation/evidence/04-a-connectivity-tcf-delta-readiness-queue-cleanup
run-01-adb-devices.txt
run-01-install.txt
run-01-launch-logcat.txt
run-01-launch-ui.xml
run-01-launch.png
run-01-launch.txt
run-01-l3-verdict.md
run-01-pm-clear.txt
run-02-adb-devices.txt
run-02-after-sync-logcat.txt
run-02-after-sync-ui.xml
run-02-after-sync.png
run-02-launch-logcat.txt
run-02-launch-ui.xml
run-02-launch.png
run-02-launch.txt
run-02-l3-verdict.md
run-03-before-resync-ui.xml
run-03-l3-verdict.md
run-03-resync-tap-logcat.txt
run-03-resync-tap-ui.xml
run-03-resync-tap.png
run-04-l3-verdict.md
run-04-post-resync-wait-dump.txt
run-04-post-resync-wait-logcat.txt
run-04-post-resync-wait-pull.txt
run-04-post-resync-wait-ui.xml
run-04-post-resync-wait.png
run-05-l3-verdict.md
run-05-terminal-import-dump.txt
run-05-terminal-import-logcat.txt
run-05-terminal-import-pull.txt
run-05-terminal-import-ui.xml
run-05-terminal-import.png
run-06-badge-management-open-dump.txt
run-06-badge-management-open-logcat.txt
run-06-badge-management-open-pull.txt
run-06-badge-management-open-ui.xml
run-06-badge-management-open.png
run-06-l3-verdict.md
run-07-isready-probe-dump.txt
run-07-isready-probe-logcat.txt
run-07-isready-probe-pull.txt
run-07-isready-probe-ui.xml
run-07-isready-probe.png
run-07-l3-verdict.md
run-08-active-switch-attempt-dump.txt
run-08-active-switch-attempt-logcat.txt
run-08-active-switch-attempt-pull.txt
run-08-active-switch-attempt-ui.xml
run-08-active-switch-attempt.png
run-08-l3-verdict.md
run-09-post-switch-isready-dump.txt
run-09-post-switch-isready-logcat.txt
run-09-post-switch-isready-pull.txt
run-09-post-switch-isready-ui.xml
run-09-post-switch-isready.png
run-09-l3-verdict.md
run-10-adb-devices.txt
run-10-entry-pull.txt
run-10-entry-ui.xml
run-10-entry.png
run-10-reconnect-logcat.txt
run-10-reconnect-pull.txt
run-10-reconnect-ui.xml
run-10-reconnect.png
run-10-l3-verdict.md
```

- **Hardware evidence**: Resumed and partially accepted. `run-01-*` proves clean
  install/launch with no registered badge after `pm clear`. `run-02-*` proves
  restored badge session, BLE/GATT, badge network IP/SSID, HTTP readiness, badge
  `/list`, and badge-owned audio-sync ingress/download start for
  `badgeMac=1C:DB:D4:9B:8F:96`. `run-03-*` proves manual re-sync is fenced while
  the badge download is already active: the ingress was tapped, the active
  download was detected, and duplicate sync/download work was skipped.
  `run-04-*` proves the active transfer remained in progress after another
  bounded wait. `run-05-*` proves terminal UI import for `2speakerTesting.wav`.
  `run-06-*` records an earlier active-switch blocker with one registered row.
  `run-07-*` proves the earlier current badge endpoint was HTTP-ready
  (`code=200`, `media readiness: true`). `run-08-*` closes the active-device
  switch branch after the user restored two badges, including outgoing-download
  cancellation and target reconnect. `run-09-*` captures the human-visible
  HTTP-abnormal panel for badge IP `192.168.0.100`. `run-10-*` proves reconnect
  can restore shell connectivity while audio sync remains fenced by
  `known_http_unreachable`.
- **Code delta transparency**:

| Area | Summary |
|---|---|
| Contract delta | HTTP-delayed readiness became an explicit domain/UI state instead of being flattened into ready; manual queued downloads now carry explicit badge ownership instead of relying on filename-only identity. |
| Behavior delta | UI can distinguish transport-connected/media-delayed from fully ready; manual badge sync fences duplicate work while a badge-owned download is already active. |
| Simplification delta | Queue identity is easier to reason about because owner MAC travels with the queued filename. The readiness model is clearer because HTTP delay is a named state rather than a hidden variant of ready. |
| Drift corrected | Corrected TCF/contract drift for HTTP-delayed readiness and manual queue ownership. Also repaired test-harness drift around repository mock flows and live drawer observation. |
| Assumption killed | Killed "transport connected means manager ready" and "filename alone identifies queued badge work." |
| Duplication/dead code | No dead-code removal claim is made. Filename-only queue identity was replaced, but no broad sync dedupe rewrite or reachability sweep was attempted. |
| Blast radius | Touched the connectivity domain/bridge/ViewModel and SIM audio repository sync path plus focused tests. Large-file pressure was contained, not decomposed; `SimAudioRepositorySyncSupport.kt` grew to make ownership explicit. |
| Tests added/changed | Bridge test protects HTTP-delayed mapping; ViewModel test protects shell-connected plus manager-HTTP-delayed coexistence; sync-support test protects wrong-badge fencing; namespace/live-observation tests were repaired to match current harness behavior. |
| Runtime evidence | L3 proved restored badge session, BLE/GATT, HTTP-ready `/list`, badge-owned download start, duplicate manual re-sync fencing, terminal UI import, active-device switch fencing, HTTP-abnormal UI, and reconnect-vs-media fencing. |
| Residual risk/debt | The exact `HTTP_DELAYED` manager enum is still L1/L2-proven rather than directly logged on-device. The HTTP-abnormal UI and reachability-failure logs prove the branch behavior, but not the internal enum name at runtime. |
| Net judgment | Cleaner: two implicit runtime contracts were made explicit with focused tests and partial-but-real device evidence, without widening into a broad connectivity/audio rewrite. |

- **Pre-BAKE codebase score**: 3/5. The incoming connectivity/audio slice was
  functional and battle-tested enough to map, but it hid important contract
  truth by flattening HTTP-delayed readiness and relying on filename-only queue
  identity plus late active-device checks.
- **Work score**: 4/5. The sprint closed the two intended deltas with focused
  code changes, targeted tests, full unit regression, build verification, and
  bounded L3 evidence including terminal import, active switch, HTTP-abnormal UI,
  and reconnect fencing; not 5/5 because the exact `HTTP_DELAYED` manager enum
  was not directly logged on-device.
- **Baked-codebase score**: 4/5. The resulting connectivity/audio slice is
  cleaner because readiness and queue ownership are explicit, but it is not
  5/5 because large-file pressure remains and the internal manager enum still
  needs easier runtime observability.
- **Lesson proposals**: none
- **CHANGELOG line**: none proposed; internal BAKE validation sprint.
