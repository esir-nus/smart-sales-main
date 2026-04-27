# Sprint 01 — lifecycle-design

## Header

- Project: badge-session-lifecycle
- Sprint: 01
- Slug: lifecycle-design
- Date authored: 2026-04-27
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):** "we may think from the TOP that what should be the lifecycle of one device, and multiple device, how they should be switched, managed, screened, refreshed, etc. this can be an overall because the feature for the audio and badge connectivity has been adding up incrementally not designed clean in the first place that had everything in mind. so that `add-up` may be a source of problem, so we may consider a clean refactoring"

**Problem narrative:**

The audio and connectivity features in Smart Sales were built through successive sprints, each adding a new capability: first single-device pairing, then multi-device registry, then audio sync, then per-file cancel, then manual disconnect intent, then BLE detection. Each sprint was internally coherent, but they were never designed against a shared lifecycle model. The result is a patchwork:

- The `DeviceConnectionManager` cancels BLE heartbeat and notification listener on switch, but nothing else knows a switch happened.
- The audio download worker runs in a global `@Singleton` coroutine scope that lives for the entire app lifetime — no concept of "this work belongs to device X."
- The UI observation model was chosen for resource efficiency (`WhileSubscribed`), without considering that badge-originated updates arrive asynchronously on a background thread long after the user has closed the relevant surface.

The concrete symptom that triggered this project: after pairing Badge B and switching back to Badge A, Badge A's audio sync fails because Badge B's download worker is still running and using Badge A's connection to request Badge B's files. Badge A's own sync never starts.

This sprint does not fix any code. Its job is to author `docs/core-flow/badge-session-lifecycle.md` — a design document that defines how badge sessions and audio sync *should* behave across single-device and multi-device scenarios. This document becomes the north star for Sprints 02 and 03. The operator's job is to read the existing code (connectivity bridge, registry manager, audio repository, audio ViewModel) and write down the correct behavior — not simply document the current behavior.

**Important:** this contract is a starting hypothesis. The operator should treat the outlined doc structure as a guide, not a rigid template. If reading the code reveals constraints or patterns that suggest a different framing, update the design accordingly and note the reasoning in the ledger.

## Scope

Files the operator may touch:

- `docs/core-flow/badge-session-lifecycle.md` (new — this is the primary output)
- `docs/projects/badge-session-lifecycle/sprints/01-lifecycle-design.md` (this file — operator appends ledger + closeout)
- `docs/projects/badge-session-lifecycle/tracker.md` (flip sprint row to `done` on close)

**Read-only reference files the operator should study:**
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt` — `switchToDevice()`, `initializeOnLaunch()`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt` — `connectUsingSession()`, `cancelActiveTransportJobs()`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryRuntime.kt` — `repositoryScope`, `queuedBadgeDownloads`, `badgeDownloadWorkerJob`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt` — `cancelBadgeDownload()`, `enqueueBadgeDownloads()`, `processBadgeDownloadQueue()`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/badge-audio-pipeline/spec.md`

**Out of scope:** any code changes, configuration changes, or new test files.

## References

- `docs/specs/sprint-contract.md` — contract schema
- `docs/specs/project-structure.md` — project folder layout
- `docs/cerb/connectivity-bridge/spec.md` — connectivity bridge ownership
- `docs/cerb/badge-audio-pipeline/spec.md` — audio pipeline ownership

## Success Exit Criteria

1. `docs/core-flow/badge-session-lifecycle.md` exists.
2. The document contains all five sections (see Guidance below).
3. Each section includes at least one explicit invariant marked in **bold** or as a bullet starting with "MUST" or "MUST NOT".
4. `wc -l docs/core-flow/badge-session-lifecycle.md` returns ≥ 80 lines.
5. Sprints 02 and 03 are referenced at the end of the document as "implementing against this contract."

## Stop Exit Criteria

- Reading the existing code reveals that the current architecture is fundamentally incompatible with the proposed lifecycle (e.g., the session model is already device-scoped in a way that contradicts the hypothesis) — stop, surface findings, do not write a design that contradicts reality.
- The operator cannot reconcile `docs/cerb/connectivity-bridge/spec.md` with the proposed lifecycle model — stop and surface the conflict.

## Iteration Bound

2 iterations or 30 minutes, whichever is reached first.

## Required Evidence Format

- `ls -la docs/core-flow/badge-session-lifecycle.md` confirming the file exists.
- `wc -l docs/core-flow/badge-session-lifecycle.md` output.
- Inline in the closeout: the operator's one-paragraph assessment of whether the prescribed fixes in Sprints 02 and 03 look correct after reading the code, or whether any adjustment is needed.

## Guidance: Document Structure

The operator may adapt this structure after reading the code, but the following five areas must be covered:

**Section 1 — Single-device lifecycle**
Define the states: Unprovisioned, Provisioned, Connecting, Active, Disconnected, Reconnecting. For each state, define: (a) what triggered the transition into this state, (b) what invariants must hold (what is running, what is not), (c) what transitions are valid out of this state.

**Section 2 — Multi-device: pairing a second badge**
What happens to the currently active device when the user pairs a new one? Does pairing immediately switch active device? What state should the old device be in after a new one is paired? What in-flight work (if any) should be allowed to complete vs. cancelled?

**Section 3 — Multi-device: device switch**
This is the critical section. Define the teardown sequence for the outgoing device and the setup sequence for the incoming device. The teardown list must include: audio download queue, any ongoing sync, BLE heartbeat, notification listener, UI overrides. For each item, state whether it is already handled, not yet handled, or should be left running.

**Section 4 — Audio sync lifecycle**
When does badge audio sync start? What device is it bound to? What events cancel it? The key invariant: the audio download worker MUST be bound to a specific device MAC and MUST NOT use another device's connection. How should this binding be enforced?

**Section 5 — UI observation contract**
What is the rule for choosing `SharingStarted.WhileSubscribed` vs `SharingStarted.Eagerly` for StateFlows that carry badge-originated data? The current assumption (surface must be open to receive updates) breaks for async badge events. Define when each is appropriate.

## Iteration Ledger

<!-- Operator appends one entry per iteration. Not committed mid-sprint. -->

## Closeout

<!-- Operator fills on exit. -->

- Status:
- Summary:
- Evidence:
- Operator assessment of Sprints 02 and 03:
- Lesson proposals:
- CHANGELOG line:
