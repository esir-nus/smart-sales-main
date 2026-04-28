# Sprint 10 - download-reconnect-dependency

## Header

- Project: badge-session-lifecycle
- Sprint: 10
- Slug: download-reconnect-dependency
- Date authored: 2026-04-28
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):** "Sprint 10 - Badge Download Reconnect Dependency Rule"

Fix same-badge disconnect during badge audio download so transport loss cancels active HTTP work immediately, preserves interrupted files for visual HOLD/resume, and targeted-resumes only those filenames after the same badge reconnects. Cover both manual `/list` queue downloads and `rec#` auto-download jobs.

## Scope

Files the operator may touch:

- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`
- Focused tests under `app-core/src/test/java/com/smartsales/prism/data/audio/**`
- Constructor call sites required by the repository dependency addition
- `docs/projects/badge-session-lifecycle/tracker.md`
- `docs/projects/badge-session-lifecycle/sprints/10-download-reconnect-dependency.md`
- `docs/cerb/audio-management/spec.md`
- `docs/bake-contracts/connectivity-badge-session.md`
- `CHANGELOG.md`

Out of scope:

- `ConnectivityBridge` API or domain interface changes.
- Connectivity transport ownership changes.
- UI rendering changes beyond relying on existing HOLD semantics.
- Harmony work or `platform/harmony` edits.
- Badge firmware Range/resumable HTTP support.

## References

- `AGENTS.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/specs/ship-time-checks.md`
- `docs/core-flow/badge-session-lifecycle.md`
- `docs/cerb/interface-map.md`
- `docs/cerb/audio-management/spec.md`
- `docs/bake-contracts/connectivity-badge-session.md`
- `docs/projects/badge-session-lifecycle/tracker.md`
- `.agent/rules/lessons-learned.md`

## Success Exit Criteria

1. Same-badge `BadgeManagerStatus.Ready -> Disconnected` or `Connecting` cancels active manual HTTP download work immediately.
2. Disconnect cancellation preserves interrupted same-badge entries in their current availability, normally `DOWNLOADING`, so the existing HOLD UI can render during transport loss.
3. Disconnect cancellation captures active, queued, and active `rec#` filenames into a per-badge pending resume set.
4. Same-badge reconnect to `Ready` drains only that pending resume set and requeues those filenames, restarting download from byte 0.
5. `BlePairedNetworkUnknown`, `BlePairedNetworkOffline`, and `BleDetected` do not cancel downloads.
6. Active-device switch remains a separate path: switch-interrupted files are marked `FAILED` and pending same-badge resume is cleared.
7. `SimBadgeAudioAutoDownloader` tracks active jobs by `(badgeMac, filename)`, cancels all active jobs for the disconnected badge, returns their filenames for targeted resume, and suppresses duplicate active `rec#` launches.
8. `ConnectivityBridge` public API is unchanged.
9. Focused audio tests cover ordinary cancel, disconnect preserve/resume, switch clearing, rec# active tracking, duplicate suppression, and repository observer status branches.
10. `./gradlew :app-core:testDebugUnitTest` passes, or closeout records unrelated test-suite blockers.
11. `./gradlew :app:assembleDebug` passes.
12. Hardware L3 start-download, power-cut, HOLD, power-restore, targeted-resume, and `adb logcat -s AudioPipeline` evidence is captured, or closeout records device unavailability/lack of hardware evidence with lowered confidence.

## Stop Exit Criteria

- The behavior requires a public `ConnectivityBridge` interface change.
- Correct same-badge resume cannot be distinguished from active-device switch with existing registry and manager-status signals.
- Unit or build failures are caused by touched files and cannot be resolved within the iteration bound.
- Hardware evidence contradicts local unit behavior.

## Iteration Bound

3 implementation/test iterations.

Each iteration must patch only the scoped audio lifecycle path, run focused tests when possible, and update docs before closeout.

## Required Evidence Format

The closeout must include:

- Focused Gradle command and result for the audio repository / auto-downloader tests.
- `./gradlew :app:assembleDebug` command and result.
- Explicit note whether `ConnectivityBridge` APIs changed.
- Hardware L3 evidence path or explicit blocker.
- Tracker, spec, BAKE contract, and CHANGELOG sync note.

## Iteration Ledger

- Iteration 1: Read the badge-session lifecycle core flow, project tracker, sprint schema, interface map, lessons index, audio-management spec, and BAKE contract. Implemented disconnect-specific cancellation in `SimAudioRepositorySyncSupport`, active `rec#` tracking/cancellation in `SimBadgeAudioAutoDownloader`, and repository manager-status observation in `SimAudioRepository`. Added focused tests for disconnect preserve/resume, switch clearing, ignored partial BLE states, same-badge observer resume, rec# disconnect cancellation, and duplicate active rec suppression. Targeted test task reached main-source compile but failed in unrelated existing test sources that reference missing `FakePipelineTelemetry` / `FakeAudioRepository`.

## Closeout

- Status: blocked
- Summary: Same-badge disconnect now cancels manual and active `rec#` HTTP download jobs, preserves interrupted entries for HOLD, and targeted-resumes only those filenames after the same badge returns to `Ready`; hardware L3 and full unit-test proof remain blocked in this environment.
- Evidence:
  - Main-source compile passed: `./gradlew :app-core:compileDebugKotlin`
  - Focused test command attempted: `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.audio.SimAudioRepositorySyncSupportTest' --tests 'com.smartsales.prism.data.audio.SimBadgeAudioAutoDownloaderTest'`
  - Result: main code compiled, but `:app-core:compileDebugUnitTestKotlin` failed before running focused tests because unrelated existing test files under `app-core/src/test/java/com/smartsales/prism/data/real/**` reference unresolved `FakePipelineTelemetry`, and `app-core/src/test/java/com/smartsales/prism/ui/AgentViewModelTest.kt` references unresolved `FakeAudioRepository`.
  - Debug APK build passed: `./gradlew :app:assembleDebug`
  - Install blocked: `adb devices` returned no attached devices and `./gradlew :app:installDebug` failed with `DeviceException: No connected devices!`.
  - `ConnectivityBridge` API review: no public connectivity interface/API changes were made.
  - Hardware L3: not captured because no device/emulator was attached.
- Lesson proposals: none.
- CHANGELOG line:
  `- **[修复] 工牌断连下载即时 HOLD 与定向续传** — 工牌音频下载中断连时立即取消 HTTP 下载，保留卡片等待恢复状态；同一工牌重连后只重新下载被中断的文件，避免等待 TCP 超时或全量同步。`

## Doc Sync

| Doc | Action |
|---|---|
| `docs/projects/badge-session-lifecycle/sprints/10-download-reconnect-dependency.md` | This file. |
| `docs/projects/badge-session-lifecycle/tracker.md` | Add Sprint 10 row at close. |
| `docs/cerb/audio-management/spec.md` | Sync same-badge disconnect and targeted resume behavior under active inventory sync. |
| `docs/bake-contracts/connectivity-badge-session.md` | Update implementation record and gap list for rec# explicit cancellation / same-badge reconnect resume. |
| `CHANGELOG.md` | Add the user-visible fix line. |
