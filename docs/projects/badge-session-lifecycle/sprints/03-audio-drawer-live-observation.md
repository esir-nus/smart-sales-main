# Sprint 03 — audio-drawer-live-observation

## Header

- Project: badge-session-lifecycle
- Sprint: 03
- Slug: audio-drawer-live-observation
- Date authored: 2026-04-27
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):** "another issue that should be included in the refactoring that the UI seems not refreshed automatically so for new audio files we need reopen the drawer to actually be able to be transcribed through swiping, and to see the downloading animation (because UI does not refresh). My description of problem could be not accurate but you get my mental mode that suspect incremental building naturally creates debts"

**Problem narrative:**

The audio drawer has two specific symptoms: (1) new audio files that appeared during badge sync are not visible until the drawer is closed and reopened, and (2) the download progress animation for an in-progress download does not update while the drawer is open — or at least the initial render after re-opening looks correct while an open drawer might miss intermediate progress updates.

The root is a mismatch between how badge-originated updates are delivered and how the UI subscribes to them.

The audio repository's `audioFiles` field is a `MutableStateFlow`. When the download worker completes a file or updates progress, it calls `runtime.audioFiles.update { ... }`. This happens on a background coroutine in `repositoryScope`. The update emits a new value into the StateFlow.

The drawer's ViewModel (`SimAudioDrawerViewModel`, `AudioViewModel`) wraps this with `.stateIn(started = SharingStarted.WhileSubscribed(5000))`. The `WhileSubscribed(5000)` policy means: keep the upstream subscription alive while at least one collector exists, and for 5 more seconds after the last collector drops. When the drawer closes, the composable stops calling `collectAsState()`. After 5 seconds, the ViewModel's internal subscription to the repository's `audioFiles` drops. Background updates from that point forward still update the `MutableStateFlow` — but the ViewModel has no active collector, so the StateFlow's value updates silently, with no recomposition.

When the user reopens the drawer, a new `collectAsState()` call starts. The ViewModel resubscribes. The very first emission it sees is the *current value* of `audioFiles` — which by now reflects all the updates that happened while the drawer was closed. This is why reopening the drawer appears to trigger sync: the drawer suddenly sees all the files that accumulated while it was closed.

The fundamental problem: `WhileSubscribed` was chosen as a resource-efficient default, but it is the wrong policy for data that arrives asynchronously from a hardware device. Badge audio files and download progress arrive on their own schedule, not in response to user actions. The drawer needs to reflect this state continuously, not just when it is open.

This is not just a UI bug — it reflects the incremental buildup pattern the user identified: the `WhileSubscribed` default was reasonable when audio was a simple list-on-open surface, but it became problematic once the feature added real-time download progress and badge-push notifications.

**Note:** the user acknowledged their description may not be fully accurate. Before applying the fix, read the actual ViewModel code and the composable to verify that `WhileSubscribed` is indeed the culprit. If the issue is elsewhere (e.g., the composable uses `remember { }` instead of `collectAsState()`, the StateFlow is never actually updated during downloads, or the only reproducible failure happens while the drawer is already open and actively collecting), diagnose and address the real cause. Document the actual finding in the ledger. This contract is a hypothesis based on static analysis, not a confirmed diagnosis.

## Scope

Files the operator may touch:

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt`
- Any test files for these ViewModels if they need updating
- `docs/projects/badge-session-lifecycle/sprints/03-audio-drawer-live-observation.md` (this file)
- `docs/projects/badge-session-lifecycle/tracker.md`

**Out of scope:**
- The audio repository or runtime — the source of truth is already a `MutableStateFlow`; this sprint only fixes the subscription policy
- The composable files (`SimAudioDrawer.kt`, `AudioDrawer.kt`) unless investigation shows the composable itself is the problem (e.g., missing `collectAsState`, using a snapshot instead of a live flow)
- Any data layer, domain layer, or connectivity layer files

## References

- `docs/core-flow/badge-session-lifecycle.md` (Sprint 01 output) — "UI observation contract" section defines when `Eagerly` vs `WhileSubscribed` is appropriate
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt:57` — `SharingStarted.WhileSubscribed(5000)` line
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt:57` — same
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt` — composable collection pattern
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt` — composable collection pattern

## Success Exit Criteria

1. `grep -n "WhileSubscribed" SimAudioDrawerViewModel.kt AudioViewModel.kt` returns no hits.
2. `grep -n "Eagerly" SimAudioDrawerViewModel.kt AudioViewModel.kt` returns a hit in each file.
3. `./gradlew :app-core:testDebugUnitTest` exits 0.
4. Screenshot pair committed to `docs/projects/badge-session-lifecycle/evidence/03-audio-drawer-live-observation/`:
   - Screenshot A: badge download in progress, drawer closed
   - Screenshot B: drawer reopened — shows current (non-zero) download progress without user having to do anything else
   
   (If this cannot be captured via ADB without a device, describe the test scenario and provide logcat evidence that `audioFiles` StateFlow emitted during the closed period.)

## Stop Exit Criteria

- Investigation reveals that `WhileSubscribed` is NOT the cause (e.g., the composable uses `remember { audioItems }` and is not collecting from a flow at all) — stop executing the prescribed fix, diagnose the real root cause, and surface it for a revised contract.
- Changing to `Eagerly` causes a memory regression or test failures in unrelated areas — stop and surface.

## Iteration Bound

2 iterations or 30 minutes, whichever is reached first.

## Required Evidence Format

1. `grep` output for criteria 1 and 2.
2. `./gradlew :app-core:testDebugUnitTest` pass summary.
3. Screenshot pair or logcat confirming that the drawer reflects updates that occurred while it was closed.

## Prescribed Fix (operator should verify diagnosis first)

In both `SimAudioDrawerViewModel.kt` and `AudioViewModel.kt`, find the `.stateIn(...)` call on the audio file list flow and change:

```kotlin
started = SharingStarted.WhileSubscribed(5000)
```

to:

```kotlin
started = SharingStarted.Eagerly
```

Do not change `WhileSubscribed` on other StateFlows that are not audio-file or download-progress flows — those may legitimately benefit from resource-saving subscription policies.

## Iteration Ledger

<!-- Operator appends one entry per iteration. Not committed mid-sprint. -->

## Closeout

<!-- Operator fills on exit. -->

- Status:
- Summary:
- Evidence:
- Lesson proposals:
- CHANGELOG line:
