# Sprint 05 — command-end-emitter

## Header

- Project: firmware-protocol-intake
- Sprint: 05
- Slug: command-end-emitter
- Date authored: 2026-04-24
- Author: Claude
- Operator: **Codex** (default operator per project tracker)
- Lane: `develop` (Android-side wiring; connectivity + audio-pipeline modules; Harmony-native port is a separate future sprint under `platform/harmony`)
- Branch: `develop` (direct-to-develop per project Cross-Sprint Decisions; no feature branch)
- Worktree: none
- Ship path: one commit on `develop` at sprint close containing code + tests + docs + contract closeout. Commit ceremony is plain `git commit` — `$ship` / `$cnp` are retired; user runs `$push` end-of-day as a separate step.
- Blocked-by: none — sprint 04 (`ver-query-handler`) explicitly deferred the legacy-vs-new emission decision to this sprint, and the protocol spec now fixes the wire format as `Command#end`.

## Demand

**User ask:** Author sprint 05 for the firmware team's 2026-04-24 second-drop `Command#end` protocol item so the app emits one fire-and-forget `Command#end` signal after each `log#` and `rec#` pipeline run reaches a terminal state, including terminal errors, and retire the legacy `notifyTaskCreated` / `notifyTaskFired` / `commandend#1` code path in the same sprint.

**Interpretation:** This sprint wires the new app-to-badge completion signal at the real pipeline terminal points rather than reusing the older scheduler/reminder chime seam. The shipped behavior is:

1. `RealBadgeAudioPipeline.processFile()` emits one best-effort `Command#end` per `log#` drop after every existing terminal completion or terminal-error branch, with one signal per invocation.
2. `SimBadgeAudioAutoDownloader.downloadAndUpgrade()` emits one best-effort `Command#end` per `rec#` drop after every terminal branch, with one signal per invocation.
3. The legacy BLE payload `commandend#1` disappears from the Android/develop codebase; the legacy task-creation seam is neutralized to a no-op and is not retargeted to `Command#end`.

**Decisions fixed at authoring:**
- **Wire format:** emit `Command#end` verbatim per `docs/specs/esp32-protocol.md` §11. Capital `C`, no `#1` suffix, no ack expected.
- **Trigger semantic:** emit on all terminal states, success and terminal error, because the spec intent is to let the badge exit its "processing" UX state even when downstream app work fails.
- **Legacy task-creation seam:** do **not** retarget scheduler/onboarding success to `Command#end`. `DefaultTaskCreationBadgeSignal.kt` is neutralized by removing the BLE call only; it stays as a no-op seam unless execution-time DI audit proves the whole file/binding can be safely deleted without widening scope.

## Scope

**In scope** (all paths rooted at repo root):

1. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt` — remove `notifyTaskCreated()` and `notifyTaskFired()` from the interface and implementation; add `suspend fun notifyCommandEnd()` that writes `Command#end` through `badgeGateway.sendBadgeSignal(session, ...)` under the existing session-present + BLE-connected guard pattern and logs/swallow exceptions.
2. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/FakeDeviceConnectionManager.kt` — remove `notifyTaskCreatedCalls` / `notifyTaskFiredCalls` counters and reset lines; add `notifyCommandEndCalls` counter and reset with the same fake-test shape.
3. `app-core/src/main/java/com/smartsales/prism/data/connectivity/DefaultTaskCreationBadgeSignal.kt` — neutralize the BLE effect only by removing the `deviceConnectionManager.notifyTaskCreated()` call. Do not retarget to `notifyCommandEnd()`. Delete the file only if a full-file read plus caller/DI audit proves the type is unnecessary for compile/DI resolution; otherwise keep the file and ship the no-op seam.
4. `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt` — add `suspend fun notifyCommandEnd()` below the existing outbound bridge methods.
5. `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt` — implement `notifyCommandEnd()` by delegating to `deviceManager.notifyCommandEnd()`.
6. `app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt` — insert `connectivityBridge.notifyCommandEnd()` at each terminal branch inside `processFile()` covering the existing `PipelineEvent.Complete`, each terminal `PipelineEvent.Error`, and the catch-all exception path. Use the existing scope/coroutine pattern; do not add a constructor parameter and do not refactor the pipeline event model.
7. `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt` — insert `connectivityBridge.notifyCommandEnd()` at each terminal branch inside `downloadAndUpgrade()` covering success-stored, success-empty-removed, and error. Use `runtime.repositoryScope`; do not add a constructor parameter and do not refactor the drawer pipeline shape.
8. Unit tests — add or extend:
   - `app-core/src/test/java/com/smartsales/prism/data/connectivity/RealConnectivityBridgeTest.kt` — `notifyCommandEnd()` delegates to the manager.
   - `app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/DefaultDeviceConnectionManagerIngressTest.kt` or the existing outbound-signal test sibling discovered during execution — `notifyCommandEnd()` sends `Command#end` when connected and silently no-ops when disconnected.
   - `app-core/src/test/java/com/smartsales/prism/data/audio/RealBadgeAudioPipelineIngressTest.kt` — one case per terminal state (`success`, `download error`, `transcribe error`, `catch-all`) asserting the fake bridge increments `notifyCommandEndCalls` exactly once.
   - `app-core/src/test/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloaderTest.kt` or the existing drawer-pipeline test sibling — one case per terminal branch (`success-stored`, `success-empty`, `error`) asserting one `notifyCommandEnd()` call.
   - Any test file with an anonymous / object / inner-class `ConnectivityBridge` fake — extend with `override suspend fun notifyCommandEnd() = Unit`. Expected grep-discovered sites include `RealBadgeAudioPipelineIngressTest.kt`, `ConnectivityViewModelRepairTest.kt`, `L2DualEngineBridgeTest.kt`, and `SimAudioRepositorySyncSupportTest.kt`.
9. `docs/specs/esp32-protocol.md` §11 — rewrite the "emitter not yet wired" paragraph to the shipped call chain (`RealBadgeAudioPipeline` / `SimBadgeAudioAutoDownloader` terminal states -> `ConnectivityBridge.notifyCommandEnd()` -> `DeviceConnectionManager.notifyCommandEnd()` -> `Command#end`) and flip Implementation Status row 12 to `✅ Implemented`.
10. `docs/cerb/interface-map.md` — add or tighten the bridge ownership row for `notifyCommandEnd()` -> spec §11 and remove any row still pointing at `notifyTaskCreated`, `notifyTaskFired`, or `commandend#1`.
11. `docs/cerb/connectivity-bridge/interface.md` — add the `notifyCommandEnd()` bridge entry and remove any legacy `commandend#1` / task-chime mention.
12. `docs/cerb/notifications/spec.md` — remove or rewrite the reminder-chime passage that still references `notifyTaskFired()` / `commandend#1`. Replace it with a narrow cross-reference to `docs/specs/esp32-protocol.md` §11 for the pipeline-terminal `Command#end` emitter. Do not restructure the rest of the notifications spec.
13. `docs/projects/firmware-protocol-intake/tracker.md` — on authoring, add sprint 05 row with status `authored`, slug `command-end-emitter`, placeholder summary `Retire legacy commandend#1 emitter and wire Command#end through both pipeline terminal states per esp32-protocol.md §11`, and link to this contract file. At closeout, flip the sprint row to `done`, remove the "Future sprint — `Command#end` emitter" bullet from Inputs Pending, and add a one-line Genesis note under 2026-04-24 recording the legacy retirement.

**Out of scope** (do not touch under this sprint):

- `platforms/harmony/**` and `docs/platforms/harmony/**` — Harmony-native port is a separate future sprint
- `SD#space` handler (§12) — separate future sprint
- `docs/specs/esp32-protocol.md` §§6-7 semantic reconciliation — explicitly blocked on firmware-team clarification; do not swap log/rec routing
- Persistence, retry, or manual replay of `Command#end` on BLE failure — this sprint is best-effort fire-and-forget only
- Dedupe/debounce across rapid pipeline runs — one pipeline run equals one emission
- Any refactor of `PipelineEvent.Complete` / `PipelineEvent.Error` modeling beyond inserting the call
- Scheduler/onboarding refactors to "clean up" the now-empty `TaskCreationBadgeSignal` seam
- New lesson authoring beyond closeout proposals

## References

Operator reads these, not the whole tree:

1. `docs/specs/sprint-contract.md` — sprint schema
2. `docs/specs/project-structure.md` — project/tracker rules
3. `docs/projects/firmware-protocol-intake/tracker.md` — project context and Inputs Pending update target
4. `docs/projects/firmware-protocol-intake/sprints/04-ver-query-handler.md` — nearest tonal sibling for scope/evidence wording
5. `docs/specs/esp32-protocol.md` §11 — `Command#end` protocol SOT
6. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt` lines 80-102 and 207-254 — legacy task-chime methods plus neighboring outbound method shapes
7. `app-core/src/main/java/com/smartsales/prism/data/connectivity/DefaultTaskCreationBadgeSignal.kt` — legacy task-creation seam to neutralize, with deletion conditional on execution-time audit
8. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/FakeDeviceConnectionManager.kt` lines 127-175 — fake counters and reset logic to retire/replace
9. `app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt` — scheduler pipeline terminal branches inside `processFile()`
10. `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt` — drawer pipeline terminal branches inside `downloadAndUpgrade()`
11. `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt` — domain bridge interface
12. `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt` — bridge implementation
13. `docs/cerb/interface-map.md` — module ownership rows to tighten/remove
14. `docs/cerb/connectivity-bridge/interface.md` — bridge public-interface doc to extend
15. `docs/cerb/notifications/spec.md` — legacy reminder-chime prose to retire
16. `.agent/rules/lessons-learned.md` — reminder that interface-signature changes often require test fake sweeps

## Success Exit Criteria

Literally checkable:

- `grep -rn "commandend#1" app-core data` returns zero hits
- `grep -n 'sendBadgeSignal(session, "Command#end")' app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt` returns exactly one hit inside `notifyCommandEnd()`
- `grep -n "notifyCommandEnd" data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt` returns at least one hit
- `grep -n "notifyTaskCreated\\|notifyTaskFired" app-core data` returns zero hits
- `grep -n "notifyCommandEnd" app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt` returns at least 5 hits
- `grep -n "notifyCommandEnd" app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt` returns at least 3 hits
- `./gradlew :app-core:testDebugUnitTest --tests "com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest" --tests "com.smartsales.prism.data.connectivity.legacy.DefaultDeviceConnectionManagerIngressTest" --tests "com.smartsales.prism.data.audio.RealBadgeAudioPipelineIngressTest" --tests "com.smartsales.prism.data.audio.SimBadgeAudioAutoDownloaderTest"` exits 0, or the equivalent discovered outbound-signal test class replaces `DefaultDeviceConnectionManagerIngressTest` if the repo already uses a different test file for outbound BLE signal coverage
- `./gradlew :app:assembleDebug` exits 0
- `docs/specs/esp32-protocol.md` §11 no longer contains the phrase `emitter not yet wired`, and Implementation Status row 12 shows `✅ Implemented`
- `docs/projects/firmware-protocol-intake/tracker.md` shows sprint 05 row status `done`, and the "Future sprint — `Command#end` emitter" bullet is removed from Inputs Pending
- One commit on `develop` covers all the above, and the commit message references this contract file path

## Stop Exit Criteria

Halt and surface:

- Neutralizing `DefaultTaskCreationBadgeSignal.kt` reveals DI or compile wiring that still requires the type to remain present — stop widening there, keep the file as a no-op seam, and record the surviving DI wiring in closeout
- Removing `DefaultTaskCreationBadgeSignal.kt` proves the type is still required as a bound or parameterized dependency beyond the one BLE call path — stop, do not delete it, and leave full seam deletion for a follow-up sprint
- `RealBadgeAudioPipeline.processFile()` has terminal branches that cannot be reached or reasoned about from a clean read without restructuring the event model — stop and surface the branch shape; a tiny helper that bundles terminal event + signal is acceptable, but no broader refactor
- Compile cascade from `ConnectivityBridge.notifyCommandEnd()` reaches more than 6 test fake sites — stop at 6, surface the actual count, and propose a `BaseConnectivityBridgeFake` follow-up sprint
- `sendBadgeSignal("Command#end")` collides with the badge BLE rate-limit floor (`RateLimitedBleGateway.MIN_QUERY_INTERVAL_MS = 2s`) and deterministically drops back-to-back completion signals — stop and surface the collision; firmware-team decision needed
- Retiring `notifyTaskCreated` / `notifyTaskFired` reveals an unexpected production call site not found in the initial grep — stop and surface; decide with the user whether to leave legacy as no-op or split full retirement into a follow-up sprint
- `./gradlew :app:assembleDebug` fails with errors that require out-of-scope file edits (Harmony-owned paths or unrelated modules)
- Iteration bound is hit without all Success Exit Criteria green

## Iteration Bound

- Max 3 iterations OR 90 minutes wall-clock, whichever hits first
- One iteration = scoped code/docs edits + focused test run + assemble run + evidence capture
- This sprint is comparable to sprint 04 in breadth but includes a terminal-branch walkthrough across two pipelines plus legacy retirement, so the bound stays at 90 minutes

## Required Evidence Format

At close, operator appends to Closeout:

1. **Code-change evidence**
   - `git diff --stat` scoped to the commit
   - grep outputs showing: zero `commandend#1` hits; one `Command#end` write inside `notifyCommandEnd()`; zero `notifyTaskCreated` / `notifyTaskFired` hits; expected `notifyCommandEnd` hit counts in the two pipeline files
2. **Test evidence**
   - focused `:app-core:testDebugUnitTest` output showing the new/updated bridge, manager, scheduler-pipeline, and drawer-pipeline cases passed
3. **Build evidence**
   - `:app:assembleDebug` tail showing `BUILD SUCCESSFUL`
4. **Doc-change evidence**
   - `git diff docs/specs/esp32-protocol.md docs/cerb/interface-map.md docs/cerb/connectivity-bridge/interface.md docs/cerb/notifications/spec.md docs/projects/firmware-protocol-intake/tracker.md`
5. **Runtime evidence**
   - not required for this sprint; the signal is fire-and-forget with no direct user-visible UI surface, so unit coverage plus one successful assemble is the verification ceiling worth collecting

## Iteration Ledger

- _Seed empty. Operator appends one bullet per iteration during execution._

## Closeout

- **Status:** `success` | `stopped` | `blocked`
- **Summary for project tracker:** _(operator fills at exit)_
- **Evidence artifacts:** _(operator fills at exit)_
- **Lesson proposals:** _(optional; human-gated)_
- **CHANGELOG line:** _(optional; human-gated)_
