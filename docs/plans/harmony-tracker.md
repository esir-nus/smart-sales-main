# Harmony Native Tracker

> **Purpose**: Active program-summary tracker for the current bounded Harmony-native lanes, with one durable surface for capability limits, backend/dataflow evidence posture, UI-lane routing, and restore state.
> **Status**: Active
> **Last Updated**: 2026-04-11
> **Primary Laws**:
> - `docs/sops/tracker-governance.md`
> - `docs/specs/platform-governance.md`
> **Related Docs**:
> - `docs/platforms/harmony/native-development-framework.md`
> - `docs/platforms/harmony/tingwu-container.md`
> - `docs/platforms/harmony/ui-verification.md`
> - `docs/platforms/harmony/test-signing-ledger.md`
> - `docs/plans/harmony-ui-translation-tracker.md`
> - `docs/reference/platform-targets.md`
> - `docs/plans/tracker.md`

---

## 1. Operating Posture

This tracker now acts as the Harmony program-summary surface.

Current rule:

- Android remains the main product-development line
- Harmony work is active now, but it is still a transient platform lane
- Stage 2 Harmony tracking is now active because the repo has separate backend/dataflow and ArkUI page-native rewrite lanes
- page-by-page ArkUI execution detail lives in `docs/plans/harmony-ui-translation-tracker.md`
- backend/dataflow proof, capability limits, restore state, and cross-lane honesty stay summarized here

---

## 2. Status Model

| Status | Meaning |
|---|---|
| `Proposed` | Defined, but not actively being built |
| `Active` | Current Harmony work is in progress |
| `Blocked` | Waiting on capability, tooling, signing, or contract clarification |
| `Accepted` | The bounded Harmony slice is documented, evidenced, and stable enough for current use |
| `Deferred` | Parked intentionally without claiming parity or completeness |

---

## 3. Entry Contract

Every active Harmony program-summary entry must include:

- `ID`
- `Title`
- `Work Class`
- `Platform Lane`
- `Capability Class`
- `Owner`
- `Status`
- `Source of Truth`
- `Required Evidence`
- `UI Translation / Native Rewrite`
- `Backend / Dataflow Evidence`
- `Supported Set`
- `Disabled Set`
- `User-visible Limitation`
- `Does Not Own`
- `Branch Restore Snapshot`
- `Last Updated`
- `Notes / Drift`

---

## 4. Active Board

### H-01: Transient Tingwu Container

- `ID`: `H-01`
- `Title`: `tingwu-container` transient Harmony app
- `Work Class`: `harmony-native`
- `Platform Lane`: `harmony`
- `Capability Class`: `curated-container`
- `Owner`: Harmony-native transient container lane
- `Status`: `Active`
- `Source of Truth`:
  - `docs/platforms/harmony/native-development-framework.md`
  - `docs/platforms/harmony/tingwu-container.md`
  - shared Tingwu/audio contracts referenced by the overlay
- `Required Evidence`:
  - Harmony overlay docs stay aligned with the reduced capability set
  - Harmony-native runtime/config seams stay documented under the Harmony-owned root
  - backend/dataflow verification is recorded whenever native protocol or storage/runtime seams change
- `UI Translation / Native Rewrite`:
  - the public Harmony surface stays a native Tingwu/audio container rather than a Compose-structure port
  - this root does not own the broader ArkUI page-by-page rewrite lane
  - scheduler-related entrypoints, onboarding promises tied to scheduler, chat lanes, and badge hardware affordances must stay removed, blocked, or hidden
- `Backend / Dataflow Evidence`:
  - current backend posture is a contract-preserving native rewrite for document-picker ingress, Harmony-local file persistence, OSS upload, Tingwu polling, artifact reopen, and Harmony-owned runtime config generation
  - every future protocol or payload rewrite must record the mapping back to the shared behavior source before the lane can move to `Accepted`
  - the scheduler backend mini-lab remains the current bounded dataflow proof surface inside this root
- `Supported Set`:
  - local or phone-owned audio import
  - Tingwu submission and progress observation
  - transcript and available artifact rendering
  - persisted artifact reopen without rerunning Tingwu by default
  - Harmony-owned packaging, lifecycle, storage, and runtime wiring needed for this reduced app
- `Disabled Set`:
  - scheduler create, reschedule, follow-up, reminder, and scheduler discovery surfaces
  - scheduler-related onboarding promises or handoff flows
  - Ask-AI, audio-grounded chat, and session-binding lanes
  - badge pairing, badge sync, badge download, `ConnectivityBridge`, and `BadgeAudioPipeline` hardware flows
- `User-visible Limitation`:
  - this is a Tingwu container, not a parity Harmony version of the Android app
- `Does Not Own`:
  - shared scheduler semantics
  - shared onboarding truth
  - the internal ArkUI UI verification package
  - Android compatibility behavior on Huawei/Honor/Harmony devices
  - repo-wide branch governance outside this Harmony lane
- `Branch Restore Snapshot`:
  - `Branch`: `harmony/tingwu-container`
  - `Purpose`: transient Harmony-native Tingwu container for quick restore and bounded delivery
  - `Capability Class`: `curated-container`
  - `Baseline Commit or Tag`: `401772ab`
  - `Current Head Snapshot`: `adbecd0a`
  - `Restore Procedure Reference`: this tracker entry plus `docs/specs/platform-governance.md`
  - `Current Restore Confidence`: manual git restore is possible now; CI-backed restore confidence remains deferred until a Harmony lane exists in CI
- `Last Updated`: `2026-04-11`
- `Notes / Drift`:
  - Stage 2 tracking is active, but this root still keeps its public capability boundary unchanged
  - the dedicated ArkUI UI rewrite lane now lives in a separate internal root and separate page tracker rather than widening this app silently

### H-02: Scheduler Backend-First Verification

- `ID`: `H-02`
- `Title`: Harmony scheduler backend-first phase 1
- `Work Class`: `harmony-native`
- `Platform Lane`: `harmony`
- `Capability Class`: `translation`
- `Owner`: Harmony scheduler backend verification lane inside the Harmony-owned root
- `Status`: `Active`
- `Source of Truth`:
  - `docs/platforms/harmony/scheduler-backend-first.md`
  - `docs/core-flow/scheduler-fast-track-flow.md`
  - `docs/cerb/scheduler-path-a-spine/spec.md`
  - `docs/cerb/scheduler-path-a-spine/interface.md`
  - `docs/cerb-ui/scheduler/contract.md`
- `Required Evidence`:
  - critical-joint telemetry exists for ingress, classification, commit, and local persistence
  - local Harmony snapshots prove that dataflow is observable without claiming reminder or UI parity
  - docs stay explicit that this is an operator-only backend verification lane
  - toolchain/device proof for the backend mini-lab must stay aligned with `docs/platforms/harmony/test-signing-ledger.md`, including the mini-lab evidence note for the signed `smartsales.HOS.test` lane
- `UI Translation / Native Rewrite`:
  - Phase 1 intentionally avoids broad scheduler UI translation in the mini-lab root
  - only a minimal operator/debug surface is allowed to run the mini-lab and inspect traces
  - the separate `ui-verification` root may host hidden mock-backed page rewrites, but that does not widen this backend-first lane into public parity
- `Backend / Dataflow Evidence`:
  - current visible mini-lab verifies exact create and vague create only
  - conflict, reschedule, and null safe-fail backend completion may run only through a collapsed internal scaffold verification section
  - local Harmony task snapshots and trace events persist under the Harmony root so reloads do not hide pipeline state
  - ingress owner, owner chain, and Path A commit visibility are part of the proof surface
  - reminder/alarm delivery remains deferred because that is a platform-adapter lane, not part of the current backend-first proof slice
- `Supported Set`:
  - operator-seeded exact-create execution
  - operator-seeded vague-create execution
  - collapsed internal-only conflict, reschedule, and null safe-fail verification triggers for backend completion
  - local persistence of Harmony scheduler verification tasks
  - local persistence of Harmony scheduler trace events
  - visible ingress-owner, owner-chain, and Path A commit inspection
- `Disabled Set`:
  - user-facing scheduler parity
  - reminder and alarm delivery parity
  - onboarding quick-start scheduler handoff
  - badge-driven scheduler ingress
- `User-visible Limitation`:
  - this is an operator-only scheduler backend verification sandbox, not the real Harmony scheduler product surface
- `Does Not Own`:
  - shared scheduler semantics
  - Harmony reminder/alarm adapter work
  - onboarding scheduler promises
  - Android scheduler implementation shape
- `Branch Restore Snapshot`:
  - `Branch`: `harmony/tingwu-container`
  - `Purpose`: bounded scheduler backend verification inside the transient Harmony root
  - `Capability Class`: `translation`
  - `Baseline Commit or Tag`: `adbecd0a`
  - `Current Head Snapshot`: working tree scaffold after scheduler backend-first slice
  - `Restore Procedure Reference`: this tracker entry plus `docs/plans/harmony-scheduler-backend-phase1-brief.md`
  - `Current Restore Confidence`: source-level restore is straightforward; build/device confidence remains unverified until Harmony toolchain runs locally
- `Last Updated`: `2026-04-11`
- `Notes / Drift`:
  - this slice exists because backend/dataflow certainty is more valuable than UI parity at the current Harmony scheduler stage
  - broader scaffold cases such as conflict/reschedule/null-fail are intentionally hidden behind collapsed internal verification controls so backend completion can proceed without widening the default visible Harmony scheduler scope
  - the current successful compile -> signing -> `hdc` deploy -> launch -> `hilog` chain is recorded in `docs/platforms/harmony/test-signing-ledger.md`; do not transfer that proof to `smartsales.HOS.ui` until the UI package has its own signing lane
  - if reminder/alarm delivery starts, reclassify the new slice as platform-adapter or divergence instead of quietly widening this row

### H-03: ArkUI UI Verification Lane

- `ID`: `H-03`
- `Title`: Harmony ArkUI page-native verification package
- `Work Class`: `harmony-native`
- `Platform Lane`: `harmony`
- `Capability Class`: `curated-container`
- `Owner`: Harmony internal UI verification lane under the Harmony-owned root
- `Status`: `Active`
- `Source of Truth`:
  - `docs/platforms/harmony/ui-verification.md`
  - `docs/plans/harmony-ui-translation-tracker.md`
  - `docs/specs/prism-ui-ux-contract.md`
  - relevant shared `docs/cerb-ui/**` and `docs/core-flow/**` docs for each page
- `Required Evidence`:
  - every page pass is recorded in the Harmony UI tracker
  - mock-vs-docked status stays explicit for every page
  - build/install/log evidence is recorded before any page is called `Pass`
- `UI Translation / Native Rewrite`:
  - this lane rewrites ArkUI pages natively, page by page, instead of growing UI out of the backend mini-lab
  - the current package is internal-only and keeps scheduler preview pages behind an internal gate
  - page-native rewrite progress lives in the Harmony UI tracker rather than being mixed into backend proof rows
- `Backend / Dataflow Evidence`:
  - this lane does not own backend truth or backend verification
  - real docking is deferred until page-level adapters are ready and backend lanes can satisfy the same page-facing contract honestly
  - the HUI-02 Tingwu docking contract is now defined at `docs/platforms/harmony/hui-02-tingwu-docking-contract.md`, documenting the exact projection from backend snapshot to page-facing adapter state
  - HUI-03 scheduler docking contract is deferred until the scheduler backend graduates from operator-only mode
- `Supported Set`:
  - internal ArkUI shell and route scaffold
  - internal Tingwu page-native verification through a dedicated page adapter and repository-shaped seed input
  - hidden scheduler preview page-native verification with mock data
  - dedicated bundle identity and Harmony-owned package scaffold for future on-device UI checks
- `Disabled Set`:
  - public scheduler parity
  - reminder/alarm parity
  - public onboarding scheduler handoff
  - any claim that mock-backed pages are real backend-complete delivery
- `User-visible Limitation`:
  - this is an internal Harmony UI verification package, not the public Harmony product app
- `Does Not Own`:
  - shared product semantics
  - backend/dataflow proof
  - public capability widening for the Tingwu container
  - Android compatibility behavior on Huawei/Honor/Harmony devices
- `Branch Restore Snapshot`:
  - `Branch`: `harmony/tingwu-container`
  - `Purpose`: internal Harmony ArkUI verification root for page-native rewrite and future device UI checks
  - `Capability Class`: `curated-container`
  - `Baseline Commit or Tag`: `adbecd0a`
  - `Current Head Snapshot`: working tree scaffold after Stage 2 Harmony UI split
  - `Restore Procedure Reference`: this tracker entry plus `docs/platforms/harmony/ui-verification.md`
  - `Current Restore Confidence`: source-level restore is straightforward; local signed-HAP generation is now repeatable, the AGC preflight lane is wired, but full device-install confidence remains blocked until a real `smartsales.HOS.ui` AGC asset set exists
- `Last Updated`: `2026-04-12`
- `Notes / Drift`:
  - page-pass detail is intentionally delegated to `docs/plans/harmony-ui-translation-tracker.md`
  - bundle identity is now separate from the mini-lab so later on-device UI checks can stay isolated
  - HUI-02 no longer uses inline mock cards; it now runs through a dedicated Tingwu page adapter seam
  - the HUI-02 docking contract defines a future direction toward shell evolution, contingent on signing gate clearance and successful docking
  - local UI-lane signed HAP proof now exists, and the script now refuses missing or wrong-bundle AGC assets before install, but device-accepted install proof is still pending

### H-04: Complete Native App (smartsales-app)

- `ID`: `H-04`
- `Title`: Complete native HarmonyOS ArkTS/ArkUI app
- `Work Class`: `harmony-native`
- `Platform Lane`: `harmony`
- `Capability Class`: `complete-native`
- `Owner`: Harmony-native complete app lane
- `Status`: `Active` (Phase 2B device-verified)
- `Source of Truth`:
  - `docs/platforms/harmony/app-architecture.md`
  - `docs/platforms/harmony/native-development-framework.md`
  - `docs/specs/cross-platform-sync-contract.md`
  - all shared `docs/core-flow/**`, `docs/cerb/**`, and `docs/specs/**` docs
- `Required Evidence`:
  - app scaffold builds locally from the Harmony-owned root
  - each feature phase records what is build-verified versus device-verified
  - domain model translations stay aligned to Kotlin `domain/` semantics
  - signing and `hdc` proof is recorded before the lane claims device parity
- `UI Translation / Native Rewrite`:
  - complete native ArkTS/ArkUI implementation, not a Compose port
  - absorbs Tingwu container patterns as foundation
  - page-native scheduler UI is now wired into the public shell instead of a placeholder
- `Backend / Dataflow Evidence`:
  - state management follows the repository pub-sub shape proven in the Tingwu container
  - Phase 2B now includes manual exact-task create/list/complete/delete plus local JSON persistence
  - reminder publish/cancel is explicitly deferred to HS-011 when Harmony adapter provisioning is not yet ready
- `Supported Set`:
  - Phase 1: app shell, navigation, lifecycle, config generation
  - Phase 2A: audio pipeline (import, upload, poll, artifacts)
  - Phase 2B: manual scheduler create/list/complete/delete with cold-launch restore
- `Disabled Set`:
  - LLM/voice scheduler extraction
  - conflict/reschedule UI
  - real Harmony reminder adapter delivery
  - Phase 2C/2D/2E surfaces beyond placeholders
- `User-visible Limitation`:
  - the app only claims the slices that are locally implemented and verified; reminder adapter delivery remains deferred to HS-011 even though the reminder-attempt log now exists
- `Does Not Own`:
  - shared product semantics
  - Android implementation shape
  - cross-platform governance outside the Harmony-owned root
- `Branch Restore Snapshot`:
  - `Branch`: `harmony/scheduler-phase-2b`
  - `Purpose`: complete native HarmonyOS app for phased parity delivery
  - `Capability Class`: `complete-native`
  - `Baseline Commit or Tag`: `platform/harmony` plus imported `smartsales-app` scaffold
  - `Current Head Snapshot`: Phase 2B scheduler slice added on top of the complete app scaffold
  - `Restore Procedure Reference`: this tracker entry plus `docs/platforms/harmony/app-architecture.md`
  - `Current Restore Confidence`: local build plus AGC signing/install/device proof now exist for create -> restore -> complete -> delete using the shared `smartsales.HOS.test` lane
- `Last Updated`: `2026-04-20`
- `Notes / Drift`:
  - the complete-native app scaffold was restored into this branch before HS-006 implementation because the current `platform/harmony` branch did not yet carry it as tracked content
  - `Scheduler.ets` now mirrors the Android task semantics with Harmony-owned `createdAt` / `updatedAt` metadata
  - `SchedulerRepository.ets` mirrors the audio repository persistence pattern and writes `smartsales_scheduler_tasks.json`
  - reminder registration is intentionally logged as deferred rather than faked until a real Harmony reminder adapter seam is available
  - the HS-006 device harness now uses a deterministic form preset button so the create path still flows through `handleCreate()` / `SchedulerRepository.addTask()` / `FileStore.saveTasks()` without relying on unstable Harmony IME automation
  - the scheduler list row key now includes `updatedAt` and completion state because the first device pass exposed ArkUI row reuse that left the `Complete` button visually stale after a successful completion
  - on-device evidence on connect key `4NY0225613001090` now includes `loadTasks success count=1`, `initialize restored count=1`, `Completed · 2030-01-15 09:30`, `loadTasks success count=0`, and `initialize restored count=0`
