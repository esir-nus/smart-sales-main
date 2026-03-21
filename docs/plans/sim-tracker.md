# SIM Mission Tracker

> **Mission**: Standalone prototype app with two main feature lanes, Scheduler Path A and Tingwu transcription plus simple audio-grounded chat, plus a decoupled connectivity support module.
> **Status**: Wave 1 Accepted / Wave 2 Negative-Branch L3 Accepted / Wave 4 Scheduler Accepted / Wave 5 Connectivity Accepted / Wave 6 Isolation Accepted / Wave 7 Feature Acceptance Accepted / Wave 7 Isolation Acceptance Accepted / Wave 7 Closeout Synced
> **Started**: 2026-03-19
> **Current Gate**: `T7.3` doc sync and closeout is now completed on **2026-03-22** together with the accepted `T7.2` isolation slice. The tracker and main mission record are now synced to `docs/reports/tests/L3-20260322-sim-wave7-isolation-acceptance.md`, and this acceptance-only pass did not require lower-layer Cerb or `docs/cerb/interface-map.md` changes because it introduced no new module ownership edges. Wave 7 reminder acceptance separately now has both connected-device proof for the exact-alarm redirect CTA plus EARLY/DEADLINE native posting in `docs/reports/tests/L2-20260321-sim-wave7-reminder-connected-validation.md` and a later real-device visual/operator pass in `docs/reports/tests/L3-20260322-sim-wave7-reminder-visual-validation.md`. A follow-up wake/capture pass on **2026-03-22** now closes the last EARLY proof gap by showing `⏰ SIM EARLY Visual` with `15分钟后开始` on the secure lock screen while `dumpsys notification` still reported the matching active EARLY record.
> **Primary Product Doc**: `docs/to-cerb/sim-standalone-prototype/concept.md`
> **Mental Model Doc**: `docs/to-cerb/sim-standalone-prototype/mental-model.md`
> **Implementation Brief**: `docs/plans/sim_implementation_brief.md`
> **Wave 1 Execution Brief**: `docs/plans/sim-wave1-execution-brief.md`
> **Wave 2 Execution Brief**: `docs/plans/sim-wave2-execution-brief.md`
> **Wave 4 Execution Brief**: `docs/plans/sim-wave4-execution-brief.md`
> **Wave 5 Execution Brief**: `docs/plans/sim-wave5-execution-brief.md`
> **Code Audit**: `docs/reports/20260319-sim-standalone-code-audit.md`
> **Clarification Audit**: `docs/reports/20260319-sim-clarification-evidence-audit.md`
> **Wave 5 Boundary Audit**: `docs/reports/20260321-sim-wave5-boundary-audit.md`
> **Wave 1 Acceptance**: `docs/reports/tests/L3-20260319-sim-wave1-shell-acceptance.md`
> **Wave 2 Negative-Branch Acceptance**: `docs/reports/tests/L3-20260320-sim-wave2-negative-branch-validation.md`
> **Wave 4 Acceptance**: `docs/reports/tests/L3-20260320-sim-wave4-scheduler-validation.md`
> **Wave 5 Acceptance**: `docs/reports/tests/L3-20260321-sim-wave5-connectivity-validation.md`
> **Wave 5 Connectivity-Absent Acceptance**: `docs/reports/tests/L3-20260321-sim-wave5-connectivity-absent-validation.md`
> **Wave 5 UX Cleanup Smoke**: `docs/reports/tests/L3-20260321-sim-wave5-ux-cleanup-smoke.md`
> **Wave 6 Isolation Validation**: `docs/reports/tests/L1-20260321-sim-wave6-isolation-validation.md`
> **Wave 7 Audio Chat Validation**: `docs/reports/tests/L3-20260322-sim-wave7-audio-chat-validation.md`
> **Wave 7 Feature Acceptance**: `docs/reports/tests/L3-20260322-sim-wave7-feature-acceptance.md`
> **Wave 7 Isolation Acceptance**: `docs/reports/tests/L3-20260322-sim-wave7-isolation-acceptance.md`
> **Wave 7 Reminder Connected Validation**: `docs/reports/tests/L2-20260321-sim-wave7-reminder-connected-validation.md`
> **Wave 7 Reminder Visual Validation**: `docs/reports/tests/L3-20260322-sim-wave7-reminder-visual-validation.md`
> **Formal Cerb Shards**:
> - `docs/cerb/sim-shell/spec.md`
> - `docs/cerb/sim-shell/interface.md`
> - `docs/cerb/sim-scheduler/spec.md`
> - `docs/cerb/sim-scheduler/interface.md`
> - `docs/cerb/sim-audio-chat/spec.md`
> - `docs/cerb/sim-audio-chat/interface.md`
> - `docs/cerb/sim-connectivity/spec.md`
> - `docs/cerb/sim-connectivity/interface.md`
> **Core Flows**:
> - `docs/core-flow/sim-shell-routing-flow.md`
> - `docs/core-flow/sim-scheduler-path-a-flow.md`
> - `docs/core-flow/sim-audio-artifact-chat-flow.md`

---

## 1. Mission Recap

This tracker records a standalone prototype inside the current repository.

The prototype must:

- keep the Prism shell and family resemblance
- allow small UI tweaks when needed
- keep only two main user-facing capabilities
- retain connectivity as an isolated support module
- avoid smart-agent behavior, memory systems, and hidden cross-pipeline coupling
- never interfere with the current agent app

Two main allowed feature lanes:

1. Scheduler via the documented Path A pipeline
2. Tingwu transcription via the Audio Drawer, plus simple `Ask AI` chat grounded in selected audio

Supporting module:

1. connectivity via hard migration of the existing decoupled Wi-Fi/BLE badge connection module

Product mental model:

- the audio drawer is the informational non-chat variant of the chat experience
- Tingwu is the source of transcription intelligence
- final display may polish Tingwu returns for readability without inventing facts
- `Ask AI` is the continuation surface for transcription-based discussion
- transcript streaming and activity states are allowed as presentation layers
- history, new page/session, connectivity entry, and settings remain valid SIM shell practices

---

## 2. Source of Truth

### Product

- `docs/to-cerb/sim-standalone-prototype/concept.md`
- `docs/to-cerb/sim-standalone-prototype/mental-model.md`
- `docs/plans/sim_implementation_brief.md`
- `docs/plans/sim-wave1-execution-brief.md`
- `docs/plans/sim-wave2-execution-brief.md`

### SIM Core Flows

- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`

### SIM Standalone

- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`

### SIM Scheduler

- `docs/cerb/sim-scheduler/spec.md`
- `docs/cerb/sim-scheduler/interface.md`

### SIM Audio and Chat

- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`

### SIM Connectivity

- `docs/cerb/sim-connectivity/spec.md`
- `docs/cerb/sim-connectivity/interface.md`

### Scheduler

- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-uni-a/spec.md`
- `docs/cerb-ui/scheduler/contract.md`

### Audio and Tingwu

- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/cerb/audio-management/spec.md`
- `docs/specs/modules/AudioDrawer.md`

### Connectivity

- `docs/specs/connectivity-spec.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`

### Boundary Map

- `docs/cerb/interface-map.md`

---

## 3. Mission Protocol

### Delivery Law

The SIM mission follows this chain:

1. Core Flow
2. Cerb Spec / Interface
3. Code
4. PU / Acceptance
5. Fix lower layers until they match

### Docs-First Rule

- no implementation before the owning SIM core flow and Cerb shard are identified
- no boundary decision by assumption when the choice affects contamination risk
- no code wave should start if the app-entry, DI, or storage boundary is still ambiguous

### Evidence Rule

- verify reuse candidates from code and tests before declaring them safe
- treat the smart app as the protected product when in doubt
- prefer adding a prototype-only seam over mutating the smart runtime in place

### Validation Rule

- every implementation wave must end with an explicit verification slice
- acceptance is incomplete if it proves features but not isolation

### Naming and Legacy Rule

- legacy names are not binding if they blur SIM ownership or contamination boundaries
- rename only when the rename improves product truth, module ownership, or runtime isolation
- do not perform broad rename churn without a boundary reason

### Protected Product Rule

- when SIM convenience conflicts with smart-app safety, the smart app wins
- make SIM absorb adapter, wrapper, namespace, or rename cost when that is the cleaner isolation move

---

## 4. Non-Negotiable Constraints

- [ ] Standalone package/app boundary is explicit before implementation starts
- [ ] Current agent app remains behaviorally untouched by default
- [ ] Scheduler slice stays Path A only
- [ ] Audio slice uses Tingwu/API-driven intelligence rather than new local reasoning logic
- [ ] Audio drawer acts as an informational transcript/artifact surface, not as a general file-management product
- [ ] Final artifact display may polish Tingwu returns, but it must remain source-led
- [ ] Selecting already-transcribed audio never reruns Tingwu by default
- [ ] Simple chat has no Oasis-style memory system
- [ ] Chat-side audio upload/select reopens Audio Drawer instead of Android file manager
- [ ] Connectivity remains decoupled from scheduler and audio/chat business logic
- [ ] Ordinary shell practices may survive only in simplified SIM form
- [ ] Legacy names may be replaced when they hide boundary truth
- [ ] Reuse is preferred, but not at the cost of cross-contamination

---

## 5. Work Plan

### Wave 0: Documents and Boundary Constitution
> Objective: Freeze the product truth, contamination boundaries, and docs hierarchy before any code wave starts.
> **Entry Docs**:
> - `docs/to-cerb/sim-standalone-prototype/concept.md`
> - `docs/to-cerb/sim-standalone-prototype/mental-model.md`
> - `docs/plans/sim_implementation_brief.md`
> - `docs/plans/sim-wave1-execution-brief.md`
> - `docs/reports/20260319-sim-standalone-code-audit.md`
> - `docs/reports/20260319-sim-clarification-evidence-audit.md`
> - `docs/core-flow/sim-shell-routing-flow.md`
> - `docs/core-flow/sim-scheduler-path-a-flow.md`
> - `docs/core-flow/sim-audio-artifact-chat-flow.md`
> - `docs/cerb/sim-shell/spec.md`
> - `docs/cerb/sim-scheduler/spec.md`
> - `docs/cerb/sim-audio-chat/spec.md`
> - `docs/cerb/sim-connectivity/spec.md`
> **Execution Law**: No implementation starts before naming, app-entry, DI, storage, and rename boundaries are explicit enough to prevent contamination.
> **Validation Requirement**: the boundary constitution must be explainable in one unambiguous paragraph before Wave 1 begins.

- [x] **T0.1: Product Mental Model Recorded**
  - [x] shell stays Prism-like while runtime becomes standalone
  - [x] scheduler remains Path A only
  - [x] audio drawer becomes informational conversation mode
  - [x] `Ask AI` becomes transcription-discussion continuation
  - [x] connectivity remains a decoupled hard-migration support module
  - [x] Tingwu output is source-led but readability-polished
- [x] **T0.2: Docs Spine Created**
  - [x] PRD / concept doc
  - [x] code audit
  - [x] three SIM core flows
  - [x] SIM Cerb shards plus interfaces
- [x] **T0.3: Boundary Constitution Decision**
  - [x] freeze prototype naming
  - [x] freeze package/app entry strategy
  - [x] freeze storage isolation strategy
  - [x] freeze DI isolation strategy
  - [x] freeze rename policy for stale legacy seams
- [x] **T0.4: Implementation Brief Created**
  - [x] boundary constitution paragraph recorded
  - [x] allowed reuse set recorded
  - [x] forbidden direct reuse set recorded
  - [x] minimal new artifact set recorded
- [x] **Done When**
  - [x] the standalone boundary can be explained without ambiguity
  - [x] there is no remaining doubt about what is reused directly versus replaced

### Wave 1: Standalone Shell Composition
> Objective: Build the standalone app shell as a separate composition root while preserving the Prism family look.
> **Entry Docs**:
> - `docs/plans/sim_implementation_brief.md`
> - `docs/plans/sim-wave1-execution-brief.md`
> - `docs/core-flow/sim-shell-routing-flow.md`
> - `docs/cerb/sim-shell/spec.md`
> - `docs/cerb/sim-shell/interface.md`
> **Owning Shard**: `docs/cerb/sim-shell/spec.md`
> **Behavioral Authority**: `docs/core-flow/sim-shell-routing-flow.md`
> **Implementation Law**: reuse shell visuals and drawer language, but do not boot the smart shell.
> **Validation Requirement**: prove SIM launch path and smart-shell suppression before starting feature-heavy waves.

- [x] **T1.1: Flow / Entry Contract**
  - [x] create prototype-only activity / entry path
  - [x] ensure SIM launch does not route through the current smart app root
- [x] **T1.2: Spec / Shell State Model**
  - [x] define prototype-only shell state holder
  - [x] enforce one-drawer-at-a-time behavior
  - [x] preserve scheduler/audio drawer choreography
- [x] **T1.3: Code / Conversation Family Model**
  - [x] make discussion chat the main conversation surface
  - [x] make audio drawer the informational non-chat variant of that same conversation family
  - [x] define `Ask AI` transition from informational mode to discussion mode
- [x] **T1.4: Code / SIM Shell Practices**
  - [x] keep history drawer in simplified SIM form
  - [x] keep new page/session entry
  - [x] keep connectivity entry/icon
  - [x] keep settings entry for profile/metadata controls
- [x] **T1.5: Code / Smart Surface Suppression**
  - [x] block mascot overlay
  - [x] block debug HUD
  - [x] block plugin/task-board style shell behavior
  - [x] block smart-runtime-only shell meaning behind preserved SIM shell controls
- [x] **T1.6: Validation**
  - [x] `:app-core:compileDebugKotlin` passes with the standalone SIM shell slice
  - [x] prove SIM shell boot path at runtime
  - [x] prove preserved shell practices do not depend on smart runtime in normal SIM operation
  - [x] prove smart-only shell surfaces are not part of normal SIM operation
- [x] **Done When**
  - [x] SIM boots into a standalone shell
  - [x] shell visually feels like Prism
  - [x] smart-only shell surfaces are not part of normal SIM use

### Wave 2: Audio Drawer Informational Mode
> Objective: Deliver the audio drawer as the informational transcript/artifact interface, sourced from Tingwu returns and finalized through the SIM readability-polisher layer.
> **Entry Docs**:
> - `docs/plans/sim_implementation_brief.md`
> - `docs/plans/sim-wave2-execution-brief.md`
> - `docs/core-flow/sim-audio-artifact-chat-flow.md`
> - `docs/cerb/sim-audio-chat/spec.md`
> - `docs/cerb/sim-audio-chat/interface.md`
> **Owning Shard**: `docs/cerb/sim-audio-chat/spec.md`
> **Behavioral Authority**: `docs/core-flow/sim-audio-artifact-chat-flow.md`
> **Execution Law**: the UI may format and polish Tingwu output, but it must not invent local semantic substitutes.
> **Validation Requirement**: prove that the UI displays source-led artifacts faithfully, does not rerun already-transcribed audio, and does not synthesize absent sections.

- [ ] **T2.1: Boundary / Storage Namespace**
  - [ ] namespace SIM metadata, artifact files, and local audio blobs before real inventory work
  - [ ] define SIM-owned binding/storage rule for any persisted audio-to-chat edge
  - [ ] prove Wave 2 does not reuse unsafe generic audio filenames
- [ ] **T2.2: Flow / Inventory Baseline**
  - [ ] freeze Wave 2 baseline as badge-sync product ingress plus SIM-safe seeded or persisted acceptance inventory
  - [ ] allow acceptance backfill of missing SIM-safe seeded entries without clobbering persisted inventory state
  - [ ] decide separately whether SmartBadge sync is included now or deferred as a Wave 2 extension
  - [ ] record the decision in docs and implementation brief
  - [ ] treat phone-local import as test-only convenience rather than production ingress
- [ ] **T2.3: Code / Existing Artifact Reuse Path**
  - [ ] detect already-transcribed audio
  - [ ] load stored artifacts instead of rerunning Tingwu
  - [ ] keep this branch explicit in UI state and validation
- [ ] **T2.4: Spec / Artifact Surface Contract**
  - [ ] render transcript
  - [ ] render summary
  - [ ] render chapters
  - [ ] render highlights
  - [ ] render speaker/talker-related sections when Tingwu returns them
  - [ ] render provider-returned adjacent sections such as questions/answers when present
  - [ ] leave absent artifact sections absent rather than locally inventing replacements
- [ ] **T2.5: Code / Informational Drawer Behavior**
  - [ ] make user-opened expanded transcribed card the primary informational view while keeping transcribed cards collapsed by default in inventory
  - [ ] keep the drawer read-only from a conversation perspective
  - [ ] ensure the view feels like a non-chat variant of the chat experience
  - [ ] preserve manual expand/collapse state while the drawer remains open so scroll/recomposition does not reopen cards
  - [x] **2026-03-20 Drawer Collapse-State Fix**
    SIM drawer cards no longer own transcribed expansion state inside ephemeral composable memory. Expansion state is now owned by the SIM drawer viewmodel for the current drawer-open session, transcribed cards start collapsed by default, manual collapse/expand survives scroll and recomposition, and closing the drawer resets the next open back to the collapsed default. L1 verification is green with `:app-core:compileDebugKotlin`, `SimAudioDrawerViewModelTest`, and `UiSpecAlignmentTest`.
  - [x] **2026-03-20 On-Device Confirmation**
    Fresh device verification now confirms the first drawer-state bug is resolved: transcribed cards open in collapsed form by default. Keep the scroll/recomposition stability claim as a separate branch until that behavior is explicitly rechecked on device.
  - [x] **2026-03-20 On-Device Scroll Stability**
    Follow-up device verification confirms the second drawer-state branch is green too: manual expand/collapse now stays stable while scrolling the SIM audio drawer and does not reopen cards through list recomposition.
- [ ] **T2.6: Code / Transcription Lifecycle**
  - [ ] support pending -> transcribing -> transcribed flow
  - [ ] surface failure explicitly
  - [ ] support retry-ready behavior without faking artifact completion
- [ ] **T2.7: Code / Polisher and Transparent-State Layer**
  - [ ] add readability-polisher step over Tingwu output
  - [ ] keep raw/lightly formatted Tingwu output as fallback if polisher fails
  - [ ] define transcript native-streaming vs pseudo-streaming fallback
  - [ ] define cosmetic activity states without pretending backend truth
- [ ] **T2.8: Validation**
  - [ ] prove SIM audio persistence is isolated from the smart app
  - [ ] prove the baseline audio inventory works even before optional badge-sync expansion
  - [ ] prove returned Tingwu artifacts remain source-led after polishing
  - [ ] prove already-transcribed audio does not rerun Tingwu
  - [x] prove missing Tingwu sections are not locally invented
  - [x] prove polisher failure falls back to provider-led output rather than blanking the view
  - [x] prove transcription failure remains explicit
  - [ ] prove cold-start acceptance rather than warm-state-only behavior
  - [ ] **2026-03-20 On-Device Note**
    Cold-start L3 pass on phone currently shows `1` launch/audio drawer green, `2` inventory baseline green, `3` already-transcribed artifact reuse green after confirming the check means no rerun when opening an already-transcribed card, `4` transcription lifecycle functionally green but with a glitchy/non-smooth progress bar, `8` `Ask AI` handoff green, and `10` storage namespace isolation green.
    Branches `5` transcription failure and `7` polisher failure were not exercised in this run, and `6` absent-section behavior remains unproven because all returned sections were populated in the tested artifact.
  - [x] **2026-03-20 Progress UX Defer**
    The glitchy/non-smooth progress bar remains known Wave 2 UX debt, but it is now explicitly deferred as low-ROI polish rather than treated as a functional blocker. Wave 2 closeout still requires the remaining correctness proofs for transcription failure, absent-section behavior, and polisher fallback.
  - [x] **2026-03-20 Debug Closeout Aid**
    Because the remaining Wave 2 branches are hard to trigger natively on demand, the chosen closeout path is a SIM-local debug-only quick-test panel inside the audio drawer browse mode. It seeds three dedicated validation cards for: explicit transcription failure, missing optional artifact sections, and a synthetic provider-led fallback scenario. This stays out of the global `L2DebugHud` and does not change production SIM behavior.
  - [x] **2026-03-20 Debug Scenario Panel Implemented**
    The SIM-local Wave 2 quick-test panel is now implemented in code. Audio drawer browse mode can seed all three closeout scenarios on demand: explicit transcription failure, missing optional artifact sections, and provider-led raw fallback. The fixture path stays inside SIM-owned repository and drawer state, remains separate from the global `L2DebugHud`, and keeps production SIM ingress unchanged. Focused L1 verification is green with `:app-core:compileDebugKotlin`, `SimAudioDebugScenarioTest`, and `UiSpecAlignmentTest`.
  - [x] **2026-03-20 Clarification Note**
    Product SIM audio still enters from badge synchronization. Seeded inventory and any future phone-local import affordance exist only to support acceptance/developer testing and must not be mistaken for the production ingress story.
  - [x] **2026-03-20 Carry Debt**
    Wave 2 previously carried focused L3 debt for the three hard-to-trigger negative branches: `5` explicit transcription failure, `6` absent-section rendering, and `7` polisher fallback. That debt is now closed by the focused device validation recorded below.
  - [x] **2026-03-20 Focused Negative-Branch L3**
    Focused L3 device validation is now recorded in `docs/reports/tests/L3-20260320-sim-wave2-negative-branch-validation.md`. The SIM-local drawer browse-mode debug panel successfully exercised all three previously hard-to-trigger Wave 2 branches on device: explicit transcription failure, missing optional sections, and provider-led raw fallback. This also confirms the validation aid lives in the audio drawer rather than a global SIM debug HUD button.
- [ ] **Done When**
  - [ ] users can browse a real SIM audio inventory
  - [ ] transcribed cards open and display Tingwu-returned artifacts in readable form
  - [ ] already-transcribed audio reuses stored artifacts without rerun
  - [ ] the drawer clearly reads as informational mode, not as a separate smart workflow

### Parallel Delivery Roadmap
> **Execution Model**: develop by standalone feature lane, not by wave headline alone.
> **Rule**: chatting/Tingwu and scheduler may proceed in parallel as long as ownership stays clean.

- [ ] **Lane A: Chatting / Tingwu**
  - [ ] owns Wave 2 residual audio debt and all Wave 3 audio-grounded chat work
  - [ ] owns SIM audio inventory, Tingwu pipeline behavior, drawer artifact rendering, and chat completion rendering
  - [ ] primary write scope: `SimAudioRepository`, `SimAudioDrawerViewModel`, `SimAudioDrawer`, `SimAgentViewModel`, audio-chat docs
- [ ] **Lane B: Scheduler**
  - [ ] owns Wave 4 as a standalone feature lane
  - [ ] owns scheduler drawer behavior, Path A-only runtime, and scheduler docs/tests
  - [ ] primary write scope: `SimSchedulerViewModel`, scheduler drawer/runtime files, scheduler docs
- [ ] **Shared Boundary Coordination**
  - [ ] `SimShell.kt` is the coordination boundary when either lane needs shell routing or overlay changes
  - [ ] avoid parallel edits to shared SIM shell/state files without explicit ownership handoff
  - [ ] if a task can stay inside one lane's write scope, do not widen it just because the other lane is active

### Wave 3: Simple Audio-Grounded Discussion Chat
> Objective: Deliver the continuation chat surface for discussing a selected transcription.
> **Owning Shard**: `docs/cerb/sim-audio-chat/spec.md`
> **Behavioral Authority**: `docs/core-flow/sim-audio-artifact-chat-flow.md`
> **Execution Law**: chat is for continuing discussion about one transcription, not for reviving the smart-agent OS.
> **Validation Requirement**: prove that the chat behaves as continuation of the selected transcription discussion and not as a generic agent session.

- [ ] **T3.1: Flow / `Ask AI` Handoff**
  - [ ] bind `Ask AI` from audio card into chat session creation
  - [ ] make chat entry feel like continuation from the informational drawer
- [ ] **T3.2: Spec / Audio-Grounded Context Injection**
  - [ ] bind one selected audio to one discussion session
  - [ ] inject transcript plus returned/polished artifacts as discussion context
  - [ ] avoid broad session-memory behavior
- [ ] **T3.3: Code / Discussion-Only Runtime**
  - [ ] remove plugin task-board behavior from the SIM chat surface
  - [ ] remove generalized agent-memory expectations
  - [ ] preserve only the states needed for transcription discussion
- [ ] **T3.4: Code / Audio Reselect From Chat**
  - [ ] reopen audio drawer from chat
  - [ ] allow user to pick one audio item
  - [ ] keep chat-side upload/reselect and drawer-origin transcription as two entry surfaces over one shared SIM Tingwu/artifact pipeline
  - [ ] if the selected chat-side audio is already transcribed, bind chat immediately to the stored artifacts without rerun
  - [ ] if the selected chat-side audio is still pending, bind chat immediately to that audio, continue the same transcription pipeline inside chat transparency, and reflect the completed artifacts/status back into the drawer
  - [ ] lock swipe/button transcribe actions for the same audio item while it is already pending/transcribing so the user cannot start a duplicate run from the drawer
  - [ ] do not default to Android file manager for this branch
  - [ ] **2026-03-20 Design Clarification**
    The SIM audio lane now has one underlying transcription/artifact pipeline with two entry surfaces: drawer-origin transcription and chat-origin upload/reselect. The second route is not a separate pipeline. Its pending-audio branch binds chat immediately to the chosen audio, keeps the user inside chat while transparent processing states explain Tingwu progress, locks duplicate transcribe triggers for that same audio item, and updates the shared drawer inventory without a second run through the drawer-first path.
  - [ ] **2026-03-20 Ingress Clarification**
    The real product inventory is badge-origin. If SIM exposes phone-local import in this prototype, that route is test-only convenience for QA/dev coverage and must remain an explicit secondary action instead of redefining chat attach or drawer inventory as a phone-upload product.
  - [ ] **2026-03-20 Proposed Test-Support Slice**
    Add a debug/test-gated `Import Test Audio` action inside the SIM audio drawer rather than changing chat attach or product ingress. That explicit secondary action may open Android picker, ingest the chosen file through the existing SIM-owned repository seam, label the entry as test-origin in the drawer UI, and then reuse the same transcription/artifact/chat pipeline as any other SIM audio item.
  - [ ] **2026-03-20 Execution Brief**
    - gate the action with `BuildConfig.DEBUG` rather than a new runtime config surface
    - keep `onAttachClick` unchanged so chat attach still reopens the SIM drawer first
    - own the picker launcher in `SimShell`, and invoke it through an explicit drawer callback rather than embedding picker routing inside the drawer composable
    - expose `Import Test Audio` as a secondary action in both browse mode and chat-reselect mode so QA can seed either pipeline without redefining the default route
    - after import completes, keep the drawer open and let the user explicitly select the new card instead of auto-binding chat
    - persist a test-origin flag on imported entries so the drawer label survives reload and does not rely on filename conventions
    - route imported files through the existing SIM repository and Tingwu/artifact path without introducing a separate upload pipeline
  - [ ] **2026-03-20 Blocker Note**
    Current on-device behavior reopens the drawer from the chat upload/reselect entry, but the drawer is not selectable from that route. Treat this as the active blocker against the core-flow requirement that chat-side audio reselection must return through a selectable SIM audio drawer rather than dead-ending in a non-interactive state.
  - [ ] **2026-03-20 Runtime Blocker Note**
    After the selectable-drawer fix landed, the pending chat-side branch proved a second blocker on device: selecting a pending audio item from chat now auto-starts the real OSS upload and Tingwu submission path, but the SIM surface can remain stuck in processing without later progress/completion reflection. Treat this as an active T3.4 happy-path failure against Branch-S4 in `docs/core-flow/sim-audio-artifact-chat-flow.md`, because the pending chat-side run is not yet reliably completing through chat transparency and back into the shared drawer state.
  - [ ] **2026-03-20 Next Fix Slice**
    - move active pending-job ownership out of the composable launch path and into SIM-owned repository/runtime state
    - persist or otherwise durably recover the `audioId -> jobId` edge so the app can rebind after shell/activity interruption
    - resume observing in-flight Tingwu jobs on reopen instead of leaving the audio entry stranded in `TRANSCRIBING`
    - drive chat transparency and drawer reflection from the same durable repository truth until terminal completion/failure
    - surface terminal failure back into SIM chat and reset the drawer item to explicit retry-ready state rather than indefinite processing
    - clear the durable `audioId -> jobId` ownership edge on completion/failure so stale jobs cannot rebind into later chat sessions
    - treat same-audio reselection during an in-flight run as rebind/no-op rather than a fresh submission
    - validate background/resume and cold-reopen behavior for an in-flight pending chat-side job, not only the in-session happy path
  - [ ] **2026-03-20 Recovery Fix Note**
    Added orphaned-state recovery for legacy SIM entries that were persisted as `TRANSCRIBING` without a resumable `activeJobId`. On load, SIM now downgrades that impossible state to explicit retry-ready `PENDING` with recovery messaging instead of rebinding chat to a fake in-flight job forever.
  - [ ] **2026-03-20 On-Device Note**
    Debug-gated `Import Test Audio` is now proven on device as a QA-only convenience path: the imported file appears in SIM inventory with a persistent test-origin label, can be selected from chat-side reselection, and completes through the same shared SIM transcription/artifact pipeline. The resulting completion also reflects back into the drawer inventory. The remaining gap is chat-surface parity after completion: SIM chat currently shows transparency plus a lightweight completion message, but does not yet render the finished transcript/artifact surface inline the way an already-transcribed drawer card does.
  - [ ] **2026-03-20 Next Completion Slice**
    - treat completed chat-side audio artifacts as durable chat history rather than transient `uiState`
    - let `SimShell` own artifact loading on terminal completion and bridge render-ready artifacts into the SIM chat owner
    - extend the SIM chat history/state model with an artifact-capable AI turn instead of overloading plain text completion messages
    - extract the current drawer artifact renderer into a shared SIM component so drawer and chat stay source-led and aligned
    - verify both chat entry branches: already-transcribed reuse and pending-to-completion rendering
  - [ ] **2026-03-20 Execution Brief**
    - keep pending transparency as transient `uiState`, but append completed artifact content as durable chat history
    - load persisted artifacts in `SimShell` on both transcribed reuse and pending terminal completion
    - pass render-ready artifact content into `SimAgentViewModel` as a dedicated artifact-capable AI turn
    - render that durable artifact turn through a shared SIM artifact component used by both drawer and chat
    - fall back to explicit completion/error text only when stored artifacts are unexpectedly unavailable
  - [ ] **2026-03-20 Implementation Note**
    Durable chat-side artifact rendering is now implemented in code: `SimShell` bridges stored artifacts into `SimAgentViewModel`, chat history owns a dedicated artifact-capable AI turn, and drawer/chat now share the same SIM artifact renderer. L1 verification is green with `:app-core:compileDebugKotlin`, `SimAgentViewModelTest`, and `SimAudioRepositoryRecoveryTest`. The remaining acceptance step is L3 on-device proof that both chat entry branches render the durable artifact surface correctly after reuse/completion.
  - [ ] **2026-03-20 Transcript Reveal Follow-Up**
    On-device validation proved the durable artifact surface is functionally correct, but the transcript presentation still needs tightening: chat-side transcript pseudo-streaming currently replays on history reentry, and long transcripts stay visibly expanded while streaming. Tightened rule for the next slice: every newly appended chat artifact message gets at most one transcript reveal, that one-time rule applies to both transcribed reuse and pending completion, and the transcript section collapses immediately once the rendered transcript exceeds 4 lines so history reentry never replays the long-body dump.
  - [ ] **2026-03-20 Transcript Reveal Brief**
    - keep durable artifact payloads unchanged; transcript reveal memory belongs to `SimAgentViewModel`
    - key reveal-consumed and long-transcript knowledge by `ChatMessage.Ai.id`, and clean that state when the owning session is deleted
    - pass explicit transcript presentation props from the host into the shared SIM artifact renderer so chat and drawer stay separated
    - use real rendered-line measurement, not newline or character heuristics
    - stop visible transcript streaming at the moment line 5 appears, collapse immediately, and mark the message as consumed/long so later history reentry starts collapsed with no replay
  - [ ] **2026-03-20 Transcript Reveal Implementation Note**
    One-time chat transcript reveal is now implemented in code. `SimAgentViewModel` owns runtime-only reveal memory keyed by durable artifact message id, the shared SIM artifact renderer accepts host-provided transcript presentation props so drawer and chat remain separated, and chat-side transcript sections now collapse immediately once rendered content exceeds 4 lines. L1 verification is green with `:app-core:compileDebugKotlin` and `SimAgentViewModelTest`. The remaining acceptance step is L3 on-device proof that both transcribed reuse and pending completion consume the reveal once and do not replay it on history reentry.
  - [ ] **2026-03-20 Timing Fix Follow-Up**
    L3 on-device testing proved the one-time reveal contract is functionally correct, but the long-transcript collapse still fires too fast to read. Tightened rule for the next fix: line 5 may classify the transcript as long immediately, but chat should hold the first reveal open for roughly 1 second before collapsing, while later history reentry remains non-streaming and collapsed by default.
  - [x] **2026-03-20 Timing Fix Implemented**
    The readable-dwell update is now in code. Chat-side transcript presentation keeps the one-time reveal rule, but long transcripts no longer collapse as soon as line 5 appears: the renderer now remembers that the transcript is long, holds the first reveal open for a short readable dwell (default about 1 second), and only then auto-collapses. Reentry stays non-streaming and collapsed for long transcripts, while drawer rendering remains static. L1 verification is green with `:app-core:compileDebugKotlin`, `SimAgentViewModelTest`, `SimArtifactContentTest`, and `UiSpecAlignmentTest`. The remaining acceptance step is the focused L3 device pass for readable dwell timing.
  - [x] **2026-03-20 Timing Fix L3**
    Focused device validation is now green. The first long-transcript reveal stays visible long enough to read before collapsing, and reopening the same history session does not replay transcript streaming; long transcripts remain collapsed by default on reentry.
- [ ] **T3.5: Validation**
  - [ ] prove `Ask AI` opens discussion continuation for the selected transcription
  - [ ] prove the chat does not surface generic smart-agent behaviors
  - [ ] prove audio reselection works through the drawer
- [ ] **Done When**
  - [ ] `Ask AI` opens a discussion chat for the selected audio
  - [ ] the chat clearly behaves as transcription-discussion continuation
  - [ ] audio reselection works through the drawer

### Wave 4: Scheduler Path A Delivery
> Objective: Deliver the simplified scheduler lane without importing Path B or smart-memory obligations.
> **Owning Shard**: `docs/cerb/sim-scheduler/spec.md`
> **Behavioral Authority**:
> - `docs/core-flow/sim-scheduler-path-a-flow.md`
> - `docs/core-flow/scheduler-fast-track-flow.md`
> **Execution Law**: reuse the scheduler feel, but keep only the approved Path A branch set.
> **Validation Requirement**: prove scheduler success without Path B and prove safe-fail behavior for bad target branches.

- [ ] **T4.1: Flow / Scheduler UI Reuse Boundary**
  - [ ] reuse `SchedulerDrawer` and child visuals where safe
  - [ ] replace runtime brains through prototype-only viewmodel seam
- [ ] **T4.2: Spec / Approved Branch Set**
  - [ ] exact create
  - [ ] conflict-visible create
  - [ ] delete
  - [ ] reschedule
  - [ ] keep inspiration shelf visible and make shelf-card `Ask AI` open a new chat session with the card text as the first auto-submitted user turn
  - [ ] explicit safe-fail feedback
- [ ] **T4.3: Code / Suppressed Branches**
  - [ ] suppress Path B
  - [ ] suppress CRM/entity enrichment
  - [ ] suppress plugin-driven scheduler re-entry
  - [ ] defer optional extras that force contamination
  - [ ] defer only advanced inspiration follow-on behavior, not the base shelf-card `Ask AI` chat launcher
- [x] **T4.4: Code / Inspiration Shelf Ask AI Handoff**
  - [x] restore visible `Ask AI` on scheduler inspiration shelf cards in SIM
  - [x] route shelf-card clicks through a shell-owned callback rather than scheduler runtime ownership
  - [x] create a fresh SIM chat session and auto-submit the exact shelf-card text as the first user turn
  - [x] close the scheduler drawer and land in the normal SIM chat surface
  - [x] hard-disable deprecated timeline/multi-select inspiration AI behavior in the SIM drawer while keeping the base shelf-card launcher
- [x] **T4.5: Validation**
  - [x] define Path A-only tests
  - [x] define ambiguous/no-match safe-fail tests
  - [x] prove scheduler still preserves conflict-visible behavior
  - [x] prove shelf-card `Ask AI` is equivalent to manually opening a new chat, seeding the first user turn with the card text, and auto-submitting it
  - [x] prove deprecated timeline/multi-select inspiration AI behavior is unreachable in SIM V1
  - [x] **2026-03-20 Execution Note**
    Inspiration shelf remains visible in SIM for Wave 4, and the base shelf-card `Ask AI` action is in-scope as a plain seeded new-chat launcher. The old inspiration timeline/multi-select `问AI (N)` branch is now explicitly deprecated in SIM V1 rather than left as a silently reachable deferred path. Only deeper inspiration-specific upgrades stay deferred as explicit tech debt.
  - [x] **2026-03-20 Implementation Note**
    Shelf-card `Ask AI` is now wired in code through a shell-owned callback: SIM restores a visible shelf button, starts a fresh chat session, seeds the exact inspiration text as the first user turn, and auto-submits it immediately. L1 coverage exists for seeded-session behavior in `SimAgentViewModelTest`, focused Compose coverage proves shelf-button visibility and SIM multi-select suppression, and L3 drawer-to-chat proof is now recorded on device.
  - [x] **2026-03-20 Deprecation Hardening**
    SIM now hard-disables the deprecated inspiration multi-select branch at the shared drawer boundary. The old bulk `问AI (N)` action bar is no longer rendered in SIM even if selection state is forced underneath, while the smart/default drawer path keeps its existing behavior. Focused Compose coverage now proves both SIM suppression and non-SIM default visibility.
  - [x] **2026-03-20 Telemetry Hardening**
    Shelf-card `Ask AI` handoff now emits explicit `VALVE_PROTOCOL` and `Log.d` evidence on both sides of the SIM path: `SIM scheduler shelf Ask AI handoff requested` at the shell boundary and `SIM scheduler shelf seeded chat session started` at the SIM chat-session boundary. Focused unit coverage now captures this through `PipelineValve.testInterceptor`.
  - [x] **2026-03-20 UI Fit Fix**
    The inspiration shelf card now reserves horizontal space for the `Ask AI` button so long inspiration text no longer pushes the button off the card edge. The title stays single-line with ellipsis, and focused Compose coverage now includes a long-title visibility check for the button.
  - [x] **2026-03-20 On-Device Validation**
    Focused L3 device validation is now recorded in `docs/reports/tests/L3-20260320-sim-wave4-scheduler-validation.md`. Exact Path A create, conflict-visible create, unsupported voice reschedule/delete-style safe-fail, and shelf-card `Ask AI` handoff all passed on device. A focused rerun now also records the exact scheduler-to-chat request/session-start telemetry and matching `SimSchedulerShelf` log lines on device, with no `DB_WRITE_EXECUTED` evidence during the handoff window.
  - [x] **2026-03-20 Deferred-Branch Proof**
    Focused Compose coverage now proves the deprecated inspiration bulk-action branch stays unreachable in SIM V1 while the shelf-only launcher remains available. `SchedulerDrawerSimModeTest` confirms SIM does not surface `问AI (N)` even if selection state is forced underneath, and still exposes exactly one working `Ask AI` launcher from the inspiration shelf.
  - [x] **2026-03-20 Telemetry Proof**
    The shell-owned request checkpoint is now covered directly in `SimShellHandoffTest`, while `SimAgentViewModelTest` continues to prove the seeded session-start checkpoint. This closes the remaining code-level observability gap for the scheduler-to-chat handoff path without widening scheduler runtime ownership.
  - [x] **2026-03-20 Remaining L3 Note**
    The narrow observability confirmation rerun is complete. Device logs now prove both exact telemetry summaries, both matching `SimSchedulerShelf` lines, and absence of scheduler write evidence during the shelf-card handoff.
  - [x] **2026-03-20 Continuity Binding Narrow Fix**
    SIM now carries a shell-owned badge scheduler follow-up continuity binding as in-memory metadata only. The owner binds one badge-origin scheduler thread to one SIM chat session, tracks the last active shell surface, clears on explicit new session / unrelated session switch / bound-session delete, and stays outside both `ISchedulerViewModel` and SIM scheduler shelf `Ask AI`.
  - [x] **2026-03-21 Real Badge Ingress Wiring**
    SIM now starts badge scheduler follow-up continuity from the real badge pipeline ingress. `SimShell` consumes `BadgeAudioPipeline.events` and starts or replaces the metadata-only continuity binding only on `PipelineEvent.Complete` with scheduler-real outcomes (`TaskCreated` and non-empty `MultiTaskCreated`), while shelf-card `Ask AI`, scheduler dev/test mic, raw recording arrival, and non-scheduler completions remain excluded.
  - [x] **2026-03-21 Surrogate On-Device Note**
    `docs/reports/tests/L3-20260321-sim-badge-follow-up-continuity.md` is now explicitly framed as a surrogate on-device note, not a failed physical-badge L3. The run used the available in-app recording path before hardware delivery, so it does not block the shipped continuity wiring and must not be treated as negative evidence against the real badge-ingress seam.
  - [ ] **2026-03-21 Remaining Carry Debt**
    Physical-badge hardware L3 remains deferred until the badge is in hand, and current SIM chat remains placeholder-level rather than a true scheduler follow-up intelligence lane. The continuity owner is ingress-correct at code/L1 level, but it is still metadata only and does not upgrade SIM chat semantics by itself.
- [x] **Done When**
  - [x] scheduler works in the standalone shell
  - [x] Path A laws remain intact
  - [x] shelf-card `Ask AI` behaves like a plain seeded new-chat launcher
  - [x] no hidden Path B dependency is required for normal scheduler use

### Parallel Carry Lane: Scheduler Shipping Hardening
> Objective: Close the remaining scheduler shipping gaps after baseline Wave 4 delivery.
> **Owning Shard**: `docs/cerb/sim-scheduler/spec.md`
> **Behavioral Authority**:
> - `docs/core-flow/sim-scheduler-path-a-flow.md`
> - `docs/core-flow/scheduler-fast-track-flow.md`
> **Execution Law**: treat shipping polish as behavior, not decoration. Date attention, motion, and reminders must be user-truthful and legacy-aligned.
> **Validation Requirement**: each shipping hardening slice must prove both the UI cue and the downstream state/notification consequence.

- [ ] **T4.6: Date Attention Signaling**
  - [x] **2026-03-21 Audit Note**
    Shared scheduler UI already supports attention rendering through `unacknowledgedDates` and `rescheduledDates`, and date tap already acknowledges those sets. SIM currently uses that for reschedule destination marking, but create does not yet mark target dates and conflict create does not yet split normal vs warning attention treatment.
  - [x] **2026-03-21 Implementation Note**
    SIM now reuses the existing two-channel calendar attention contract for create as well as reschedule. Off-page create marks `unacknowledgedDates`, off-page conflict-create reuses `rescheduledDates` as the warning-priority amber channel, and same-page create emits no redundant calendar attention. The runtime/viewmodel path now supports per-date aggregation for multi-task create, but the shipped SIM voice ingress is still single-task for Uni-A exact and Uni-B vague-upgrade extraction, so end-to-end multi-date create is not yet wired from the live input path.
  - [x] wire create target-date attention for off-page creates
  - [x] split normal create vs conflict create calendar attention semantics
  - [x] prove first user tap on the affected date acknowledges and clears the marker
  - [x] **2026-03-21 Focused L1**
    `SimSchedulerViewModelTest` now proves off-page exact create, off-page vague create, off-page conflict-create, same-page no-attention, per-date multi-task aggregation, same-date amber escalation when any created task conflicts, and tap-to-acknowledge clearing. The multi-task coverage currently relies on an internal fast-track seam because the shipped SIM extraction path does not yet emit multi-task create payloads.
  - [x] **2026-03-21 Multi-Task Ingress Implementation**
    SIM now fronts scheduler create with `Uni-M` ordered multi-task decomposition before the single-task Uni-A / Uni-B / Uni-C chain. The live path now resolves fragments left-to-right, executes them as independent creates, preserves per-date blue/amber attention semantics, keeps one aggregate batch status in `pipelineStatus`, and downgrades clock-relative fragments after a vague date-only predecessor into vague tasks when the lawful day anchor is still available.
  - [x] **2026-03-21 Relative-Now Anchor Rule**
    Standalone `N hours/minutes later` fragments are now treated as lawful exact create by anchoring to `nowIso`, and standalone `明天/后天/tomorrow + clock` fragments inside a multi-task batch now have an explicit deterministic now-day-offset route so the live path does not depend on model-computed absolute dates for those cases.
  - [x] **2026-03-21 Focused L1 Re-Run Closeout**
    The previously noisy focused scheduler unit rerun is now clean. `:app-core:compileDebugKotlin` is green, and `:app-core:testDebugUnitTest --tests "com.smartsales.prism.ui.sim.SimSchedulerViewModelTest"` now passes after fixing three stale reschedule-motion assertions that were still using `runCurrent()` even though the fake repository's `rescheduleTask(...)` path includes a `delay(200)`. This confirms the live `Uni-M` date-attention path and the current reschedule-motion L1 contract in the repo rather than only in tracker prose.
  - [x] **2026-03-21 Focused L2 Closeout**
    Focused Compose/device validation is now green on the connected device through `:app-core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartsales.prism.ui.drawers.scheduler.SchedulerCalendarTest`. The original failures were stale test-harness issues inside `SchedulerCalendarTest` itself (non-deterministic day assumptions, duplicate broad text selectors, and an invalid second `setContent(...)` in the same test), not product failures in the shared calendar attention contract. With that harness repaired, the scheduler calendar now has honest device-level proof for normal/warning attention semantics and tap-to-acknowledge clearing.
- [x] **T4.7: Reschedule Motion Semantics**
  - [x] **2026-03-21 Audit Note**
    Shared scheduler card state already contains `isExiting` and `exitDirection`, and current SIM code now does drive source-card exit motion through the shell-reused timeline path. The remaining gap is not absence of motion wiring but acceptance closeout above L1.
  - [x] define source-card exit animation contract for future-vs-past moves
  - [x] wire destination-date attention together with source-card exit
  - [x] **2026-03-21 Focused L1**
    `SimSchedulerViewModelTest` now proves three motion branches directly: off-page reschedule arms rightward exit motion plus destination-date attention, same-day later reschedule arms rightward exit motion without redundant date attention, and same-day earlier reschedule arms leftward exit motion without redundant date attention. The earlier failures were stale timing assertions in the test harness rather than missing runtime wiring.
  - [x] prove the rescheduled move is visually legible in both visible-day and off-page cases
  - [x] **2026-03-21 Focused Compose/Device Proof**
    `:app-core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerSimModeTest` is now green on the connected device. Focused drawer-level proof covers the two required presentation branches: a visible-day exiting source card renders and then clears through the live animation window, while an off-page reschedule does not leave a stray source card on the current day and instead preserves warning-priority destination-date attention in the shared calendar surface.
- [x] **T4.8: Native Banner / Alarm Integration**
  - [x] **2026-03-21 Audit Note**
    Legacy reminder infrastructure already exists in `RealAlarmScheduler`, `TaskReminderReceiver`, `AlarmActivity`, and `RealNotificationService`. Reminder cadence is domain-owned by `UrgencyLevel.buildCascade(...)`, and visual delivery tier is split by `CascadeTier` (`EARLY` banner, `DEADLINE` full-screen alarm).
  - [x] **2026-03-21 Implementation Note**
    T4.8 is now implemented with a narrowed SIM contract. `SimSchedulerViewModel` adopts the shared `AlarmScheduler` / reminder stack only for persisted exact tasks, conflict-persisted exact tasks still arm reminders, vague tasks do not, delete and mark-done cancel reminders, reschedule cancels then rearms, and restore-from-done does not rearm in this slice. Exact-alarm permission prompting stays on the scheduler UI boundary through a process-lifetime gate, and scheduling failures degrade without rolling back task persistence.
  - [x] decide SIM adoption boundary for legacy alarm stack vs shared main-app behavior
  - [x] define which scheduler create/conflict/completion states upgrade from toast-level feedback to native banner/notification behavior in SIM
  - [x] **2026-03-21 Narrowed Decision**
    SIM does not emit immediate native create/conflict/completion notifications in T4.8. Mutation feedback remains in-drawer through `pipelineStatus`; the native OS stack is reused only at reminder time for persisted exact tasks.
  - [x] **2026-03-21 Focused L1**
    `SimSchedulerViewModelTest` now proves: exact create reminder scheduling, conflict-exact reminder scheduling, vague no-op behavior, mixed `Uni-M` exact/vague batches scheduling only exact reminders, delete cancellation, mark-done cancellation, restore-without-rearm, reschedule cancel-plus-rearm, and one-shot exact-alarm permission prompt emission while scheduling continues. `:app-core:testDebugUnitTest --tests "com.smartsales.prism.ui.sim.SimSchedulerViewModelTest"` and `:app-core:compileDebugKotlin` are green.
  - [x] **2026-03-21 OEM Guidance Hardening**
    The scheduler reminder prompt is now adaptive instead of exact-alarm-only copy. `SchedulerDrawer` derives an OEM-aware checklist from `ReminderReliabilityAdvisor` and routes the user to the nearest relevant settings page: exact alarm, battery optimization, MIUI permission editor, auto-start, or generic notification settings. The same prompt seam remains process-lifetime gated and now covers Xiaomi/HyperOS visibility risks as well as broader Chinese OEM background-delivery hardening.
  - [x] **2026-03-21 Connected Proof**
    Connected-device verification is now recorded in `docs/reports/tests/L2-20260321-sim-wave7-reminder-connected-validation.md`. `SchedulerDrawerSimModeTest` proves the exact-alarm guidance CTA path on device, and `TaskReminderReceiverDeviceTest` proves EARLY reminder posting on `prism_task_reminders_v3_early` plus DEADLINE reminder posting on `prism_task_reminders_v3_deadline` with `fullScreenIntent` configured. Focused verification is green with `:app-core:compileDebugKotlin`, the focused reminder unit pack, `:app-core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerSimModeTest,com.smartsales.prism.data.scheduler.TaskReminderReceiverDeviceTest`, the focused rerun for `TaskReminderReceiverDeviceTest`, and `adb logcat -d -s TestRunner TaskReminderReceiver AlarmActivity`.
  - [x] **2026-03-22 Visual / Operator Pass**
    Real-device operator-style validation is now recorded in `docs/reports/tests/L3-20260322-sim-wave7-reminder-visual-validation.md`. On **2026-03-22**, EARLY reminder delivery was re-proven on device through `TaskReminderReceiver` logs plus active `NotificationRecord` evidence for `⏰ SIM EARLY Visual` on channel `prism_task_reminders_v3_early` with importance `4`. A later wake/capture follow-up on the same secure-keyguard device then surfaced `⏰ SIM EARLY Visual` with `15分钟后开始` as a human-visible lock-screen card while `dumpsys notification --noredact` still showed the matching active EARLY record. DEADLINE reminder also has strong live proof on the same device: an unlocked run showed a visible `🚨 SIM DEADLINE Visual` reminder surface with `知道了`, while a later locked-device run logged both `fullScreenIntent 已设置 (DEADLINE)` and `AlarmActivity` `onCreate`.
  - [x] **2026-03-22 Reminder Closeout**
    T4.8 reminder acceptance is now fully closed for the narrowed SIM contract: exact-alarm guidance is proven, EARLY has both native-delivery and human-visible lock-screen proof, and DEADLINE has both unlocked visible-surface proof and locked-device `AlarmActivity` creation proof. No reminder-specific blocker remains for this lane.
  - [x] prove exact-alarm permission, banner delivery, and full-screen deadline behavior against the chosen SIM contract
- [x] **2026-03-22 Reminder Lane Closed**
  T4.8 boundary/implementation is now in code with focused L1 proof, connected-device proof for CTA/native posting, and a later real-device visual/operator pass that now covers EARLY human-visible delivery plus DEADLINE unlocked/locked behavior without widening SIM into immediate create/conflict/completion notifications.

### Wave 5: Connectivity Hard Migration
> Objective: Reuse the mature badge connectivity stack as a decoupled SIM support module.
> **Owning Shard**: `docs/cerb/sim-connectivity/spec.md`
> **Behavioral Authority**:
> - `docs/cerb/sim-connectivity/spec.md`
> - `docs/specs/connectivity-spec.md`
> - `docs/cerb/connectivity-bridge/spec.md`
> **Execution Law**: connectivity may be hard-migrated, but it must remain infrastructure/support behavior rather than reviving smart-agent coupling.
> **Validation Requirement**: prove connectivity entry works in SIM without coupling scheduler and audio/chat flows together.

- [x] **T5.1: Flow / Shell Entry Contract**
  - [x] define shell-level connectivity entry/icon
  - [x] define return path back into normal SIM shell use
  - [x] **2026-03-20 Execution Brief**
    Wave 5 execution is now compressed in `docs/plans/sim-wave5-execution-brief.md`. T5.1 is now locked as the full SIM connectivity flow: `NeedsSetup` opens a bootstrap modal, setup reuses the onboarding pairing subset as a full-screen SIM overlay, setup success enters a full-screen SIM connectivity manager, and later configured entry reopens the manager directly.
  - [x] **2026-03-20 Implementation Note**
    T5.1 is now implemented in code. `SimShellState` owns an explicit connectivity route with `MODAL`, `SETUP`, and `MANAGER`; the global shell scrim only applies to the bootstrap modal route; `SETUP` now reuses onboarding pairing UI/business logic instead of the legacy setup screen; setup success enters a full-screen connection-only manager surface; and configured connectivity entry resolves directly to that manager instead of reopening the bootstrap modal.
  - [x] **2026-03-21 Focused L3 Routing Proof**
    Physical-badge L3 now closes the previously blocked branch. The run entered the manager directly from `DISCONNECTED`, fell into the onboarding-derived setup flow after reconnect returned `NeedsSetup`, completed real BLE/Wi-Fi provisioning, transitioned `SETUP -> MANAGER`, and then reopened the manager directly on later configured entry. Evidence is recorded in `docs/reports/tests/L3-20260321-sim-wave5-connectivity-validation.md`.
  - [x] **2026-03-21 Acceptance Note**
    T5.1 is now accepted for the implemented routing contract. The legacy placeholder setup screen is no longer the SIM setup surface, the onboarding pairing subset is proven on device, and success lands in the manager-backed steady-state surface.
  - [ ] **2026-03-21 UX Carry Debt**
    The manager surface is currently full-screen and feels too heavy for a SIM support-module action. Functional routing is accepted, but the next UI refinement slice should reduce the manager presentation so connectivity does not dominate the shell visually.
- [x] **T5.2: Spec / Hard Migration Boundary**
  - [x] keep BLE/Wi-Fi behavior under existing connectivity contracts
  - [x] define what SIM reuses unchanged
  - [x] define what SIM wraps at shell level only
  - [x] **2026-03-21 Boundary Freeze**
    The Wave 5 boundary is now explicit across `sim-connectivity`, `sim-shell`, `sim-scheduler`, and `sim-audio-chat`. Reused unchanged: `ConnectivityBridge`, `ConnectivityService`, `ConnectivityViewModel`, `PairingService`, and the onboarding pairing subset. Shell-owned wrappers only: entry sources, `MODAL/SETUP/MANAGER` route state, overlay semantics, close-back-to-chat behavior, and SIM route telemetry.
  - [x] **2026-03-21 Evidence Audit**
    Repo evidence is recorded in `docs/reports/20260321-sim-wave5-boundary-audit.md`. It proves scheduler currently has no connectivity imports, shell owns routing only, audio consumes `ConnectivityBridge` for badge-origin ingress/file operations, and the remaining contamination points are explicit rather than hidden.
- [ ] **T5.3: Code / Connectivity Isolation**
  - [x] keep connection management isolated from scheduler and audio/chat business logic
  - [x] avoid smart-agent dependencies in connectivity entry path
  - [x] remove `PairingService -> BlePeripheral` leakage from the domain-facing pairing contract
  - [x] strip onboarding profile/account collaborator carry-over from the SIM setup path or isolate it behind a narrower pairing-only seam
  - [x] **2026-03-21 Implementation Note**
    `DiscoveredBadge` is now plain domain data, `RealPairingService` keeps discovered `BlePeripheral` state internal and snapshot-scoped, `PairingFlowViewModel` now owns the shared pairing seam, and `OnboardingViewModel` is narrowed to profile persistence only.
  - [x] **2026-03-21 Focused L1**
    Focused verification is green for `:app-core:compileDebugKotlin` and the Wave 5 unit pack covering `RealPairingServiceTest`, `PairingFlowViewModelTest`, `OnboardingViewModelTest`, `OnboardingFlowTransitionTest`, `SimConnectivityPairingFlowTest`, and `SimConnectivityRoutingTest`.
- [ ] **T5.4: Validation**
  - [x] prove connectivity entry opens usable badge connection management
  - [ ] prove scheduler and audio/chat do not require connectivity to be meaningful
  - [x] **2026-03-20 Focused L1**
    Focused L1 coverage now proves the shell-owned route logic for modal vs setup connectivity surfaces and the SIM-only ready-state projection used for auto-return behavior.
  - [x] **2026-03-21 Offline Telemetry Hardening**
    T5.4 now has dedicated SIM-local telemetry/log markers for the connectivity-absent acceptance slice. Persisted artifact open emits `SIM audio persisted artifact opened` through `VALVE_PROTOCOL` plus `SimAudioOffline`, artifact-driven grounded chat emits `SIM audio grounded chat opened from artifact` through `VALVE_PROTOCOL` plus `SimAudioChatRoute`, and disconnected badge-sync failure emits `SIM audio sync failed while connectivity unavailable` through `VALVE_PROTOCOL` plus `SimAudioOffline`.
  - [x] **2026-03-21 Focused L1 (Connectivity-Absent Slice)**
    Focused JVM coverage is now green for the new offline evidence seams: `SimShellHandoffTest` proves persisted-artifact and grounded-chat telemetry, and `SimAudioDebugScenarioTest` proves the disconnected sync-failure telemetry helper. T5.4 still requires the focused L3 device pass before closure.
  - [x] **2026-03-21 Audio Sync Ingress Implementation**
    SIM audio now exposes a browse-mode manual `sync from badge` action plus one best-effort browse-open auto-sync path. The implementation keeps the shared `syncFromDevice(): Unit` contract unchanged, keeps readiness inside the SIM repository/connectivity seam rather than shell UI connection-state mapping, and preserves additive-only filename-based badge import so repeated `/list` checks never redownload the same local `SMARTBADGE` file. Focused JVM coverage is green for the new sync helpers and browse-only auto-sync gate in `SimAudioDrawerViewModelTest`, `SimAudioDebugScenarioTest`, and `SimShellHandoffTest`.
  - [x] **2026-03-21 Focused L3 Status**
    Wave 5 device validation is now green for the manager-backed routing contract. Real badge evidence proves manager direct-entry, onboarding-backed setup, successful provisioning, `SETUP -> MANAGER`, and configured re-entry. Report: `docs/reports/tests/L3-20260321-sim-wave5-connectivity-validation.md`.
  - [x] **2026-03-21 Focused Rerun Prep**
    The remaining T5.4 disconnected acceptance slice now has an explicit device rerun plan in `docs/reports/tests/L3-20260321-sim-wave5-connectivity-absent-rerun-plan.md`. That plan is preparation only, not closure evidence.
  - [x] **2026-03-21 Focused T5.4 Device Note**
    Focused disconnected device validation is now green and recorded in `docs/reports/tests/L3-20260321-sim-wave5-connectivity-absent-validation.md`. With airplane mode enabled, scheduler use, persisted artifact reuse, and grounded `Ask AI` remained usable while disconnected, and manual `sync from badge` failed explicitly with `oss_unknown null` without breaking the rest of the SIM flow. Treat the ugly literal as UX debt, not as an acceptance blocker.
  - [x] **2026-03-21 Post-Acceptance UX Cleanup**
    Wave 5 carry debt is now cleaned up in code. `ConnectivityManagerScreen` no longer occupies the shell as a heavy full-screen page; SIM now renders it as a contained support panel overlay. Manual offline badge sync also now preflights readiness and maps raw transport failures such as `oss_unknown null` to human-readable drawer feedback. Focused L1 verification is green with `:app-core:compileDebugKotlin`, `SimAudioDebugScenarioTest`, `SimAudioDrawerViewModelTest`, and `SimConnectivityRoutingTest`. No new L3 rerun is recorded for this UI copy/presentation cleanup yet.
  - [x] **2026-03-21 UX Cleanup Device Smoke**
    A short on-device smoke check is now recorded in `docs/reports/tests/L3-20260321-sim-wave5-ux-cleanup-smoke.md`. The contained manager presentation and the human-readable offline sync message both checked green on device.
- [ ] **Done When**
  - [x] connectivity remains available in SIM
  - [x] connectivity remains decoupled from the two main feature lanes

### Wave 6: Storage and DI Isolation
> Objective: Build the contamination firewall in runtime composition and persistence.
> **Owning Shards**:
> - `docs/cerb/sim-shell/spec.md`
> - `docs/cerb/sim-audio-chat/spec.md`
> **Execution Law**: if isolation is ambiguous, treat the current smart app as higher priority and keep SIM separate.
> **Validation Requirement**: isolation proof is required before declaring SIM safe to continue.

- [ ] **T6.1: Spec / Prototype-Only DI Root**
  - [ ] define SIM modules
  - [ ] prevent default boot through the smart singleton graph
  - [ ] isolate smart-only services unless deliberately reused behind explicit seams
  - [x] **2026-03-21 Spec Freeze**
    `docs/cerb/sim-shell/spec.md` is now the owning T6.1 artifact. It freezes the SIM composition chain (`SimMainActivity -> SimShell -> Sim* runtime owners -> SIM-owned dependency assembler`), classifies current dependencies as direct reuse vs SIM-wrapped reuse vs forbidden, and explicitly forbids shared implementation edits in `SimMainActivity`, `SimShell`, and shared notification/alarm entry wiring while T4.8 remains active.
- [x] **T6.2: Code / Audio Persistence Namespace**
  - [x] namespace metadata file
  - [x] namespace local audio blob naming
  - [x] namespace artifact file naming
  - [x] namespace audio/chat binding records if needed
  - [x] **2026-03-21 Closeout Note**
    `SimAudioRepository` is now treated as the owning T6.2 artifact. Focused repository evidence and tests now prove the accepted V1 namespace contract: metadata persists in `sim_audio_metadata.json`, stored audio blobs use `sim_<audioId>.<ext>`, artifacts use `sim_<audioId>_artifacts.json`, and `boundSessionId` persists inside SIM metadata rather than requiring a separate file. Focused verification is green with `:app-core:compileDebugKotlin`, `SimAudioRepositoryNamespaceTest`, and `SimAudioRepositoryRecoveryTest`.
- [x] **T6.3: Spec / Session-Chat Persistence Review**
  - [x] decide whether session storage is shared or namespaced
  - [x] document the reason
  - [x] enforce it in code plan
  - [x] **2026-03-21 Closeout Note**
    `SimSessionRepository` is now treated as the owning T6.3 artifact. SIM chat/session persistence is file-backed and namespaced to `sim_session_metadata.json` plus `sim_session_<sessionId>_messages.json`, durable history stores only user text / AI response / AI audio artifacts / AI error turns, transient UI state remains memory-only, normal runtime no longer seeds demo sessions, and cold start restores grouped history without auto-selecting an active session. Startup reconciliation also normalizes the audio/session binding edge by clearing dangling `boundSessionId`, restoring missing audio-side bindings for valid linked sessions, unlinking sessions whose audio no longer exists, and keeping only the newest session when duplicate persisted sessions claim the same audio. Focused verification is green with `:app-core:compileDebugKotlin`, `SimAgentViewModelTest`, `SimSessionRepositoryTest`, `SimAudioRepositoryNamespaceTest`, and `SimAudioRepositoryRecoveryTest`.
- [x] **T6.4: Validation**
  - [x] prove smart and SIM runtime roots are distinct enough
  - [x] prove persistence cross-contamination is controlled
  - [x] **2026-03-21 Closeout Note**
    Focused L1 isolation validation is now recorded in `docs/reports/tests/L1-20260321-sim-wave6-isolation-validation.md`. Source-backed runtime-boundary proof now verifies that `SimMainActivity` mounts `SimShell` rather than the smart shell root, and that `SimShell` owns SIM runtime collaborators rather than routing through the smart chat root. Persistence isolation is now re-proven across audio and session storage: `SimAudioRepository` stays on `sim_audio_metadata.json` / `sim_<audioId>.<ext>` / `sim_<audioId>_artifacts.json`, while `SimSessionRepository` writes only `sim_session_metadata.json` plus `sim_session_<sessionId>_messages.json`. Focused verification is green with `:app-core:compileDebugKotlin`, `SimRuntimeIsolationTest`, `SimShellHandoffTest`, `SimAgentViewModelTest`, `SimSessionRepositoryTest`, `SimAudioRepositoryNamespaceTest`, and `SimAudioRepositoryRecoveryTest`.
- [x] **Done When**
  - [x] SIM and smart app do not silently share unsafe runtime state
  - [x] persistence cross-contamination risk is explicitly controlled

### Wave 7: Verification and Acceptance
> Objective: Prove the SIM app works while the smart app remains intact.
> **Execution Law**: SIM acceptance must include isolation proof, not only feature proof.

- [x] **T7.1: Feature Acceptance**
  - [x] shell routing proof
  - [x] audio informational-mode proof
  - [x] `Ask AI` continuation proof
  - [x] scheduler Path A proof
  - [x] connectivity support-module proof
- [x] **T7.2: Isolation Acceptance**
  - [x] smart app boot/regression check
  - [x] storage namespace isolation check
  - [x] DI/root isolation sanity check
  - [x] **2026-03-22 Closeout Note**
    `T7.2` is now accepted in `docs/reports/tests/L3-20260322-sim-wave7-isolation-acceptance.md`. This slice explicitly stayed narrower than full smart-app feature acceptance: it proved launcher/root sanity from `AndroidManifest.xml` and `AgentMainActivity.kt`, re-ran the focused Wave 6 L1 isolation pack, confirmed on device that explicit smart launch resolves to `AgentMainActivity` while explicit SIM launch resolves to `SimMainActivity`, and used `adb shell run-as com.smartsales.prism ls files` to support the existing `sim_*` storage namespace contract. No conflicting Wave 7 evidence was found against the accepted Wave 6 runtime/storage isolation proof.
- [x] **T7.3: Doc Sync and Closeout**
  - [x] update SIM tracker statuses
  - [x] sync related Cerb docs if implementation changes lower-layer details
  - [x] decide whether `docs/cerb/interface-map.md` now needs SIM registration
  - [x] **2026-03-22 Closeout Note**
    Tracker sync is now complete for the Wave 7 isolation closeout. This session updated both `docs/plans/sim-tracker.md` and `docs/plans/tracker.md` to register the new `T7.2` acceptance report. No lower-layer Cerb shard changed in this acceptance-only pass, so no additional Cerb sync was required. `docs/cerb/interface-map.md` also does not need SIM registration from this slice because no new cross-module ownership edge or public interface contract was introduced.
- [x] **Done When**
  - [x] SIM feature slice is working
  - [x] smart root still boots correctly with no new cross-contamination evidence in this acceptance slice
  - [x] docs and verification evidence are in sync

---

## 6. Delivery Sequence

The mission should execute in this order:

1. Wave 0: documents and boundary constitution
2. Wave 1: standalone shell composition
3. Wave 2: audio drawer informational mode
4. Wave 3: simple audio-grounded discussion chat
5. Wave 4: scheduler Path A delivery
6. Wave 5: connectivity hard migration
7. Wave 6: storage and DI isolation
8. Wave 7: verification and acceptance

No implementation should start before T0 boundary decisions are explicit enough to prevent contamination.

---

## 7. Success Definition

The mission is complete only when:

- [ ] a standalone prototype app path exists in the repo
- [ ] the current agent app remains intact
- [ ] scheduler works through Path A in the prototype
- [ ] audio cards open and display source-led, readability-polished Tingwu artifacts
- [ ] already-transcribed audio loads existing artifacts without rerunning Tingwu
- [ ] `Ask AI` opens a simple chat session grounded in the chosen audio
- [ ] audio selection from chat returns through the Audio Drawer
- [ ] connectivity remains available as a decoupled SIM support module

---

## 8. Main Risks

### R1: Fake Standalone

Risk:
The prototype is implemented as scattered conditionals inside the current agent app rather than a real isolated product path.

Mitigation:
Freeze package, DI, and storage boundaries before coding.

### R2: Smart-Agent Creep

Risk:
The team reuses too much of the current agent runtime and accidentally reimports memory or orchestration behavior.

Mitigation:
Treat all agent-runtime reuse as opt-in and seam-checked.

### R3: Overbuilding the Audio Chat

Risk:
The simple audio-grounded chat becomes another general AI assistant.

Mitigation:
Keep one-context audio grounding and explicitly reject memory-system growth in this mission.

### R4: Cosmetic Transparency Becoming Fake Truth

Risk:
The transparent thinking layer starts pretending to represent backend truth that does not exist.

Mitigation:
Treat transcript streaming and activity labels as presentation unless backed by real provider signals.

---

## 9. Next Expected Docs

- Wave 1 verification note after manual or adb launch proof
- verification brief once Wave 6 boundaries are implemented
- Wave 2 execution brief if the audio/chat delivery wave needs a similarly compressed handoff
- Wave 1 implementation notes if root ownership changes during coding

---

## 10. Audit-Derived Minimal Path

Locked recommendation from `docs/reports/20260319-sim-standalone-code-audit.md`:

- reuse UI skins through existing interfaces
- do not reuse the current agent activity, shell, agent viewmodel, or global DI graph directly
- introduce prototype-only shell, viewmodels, and composition root
- namespace prototype storage before reusing audio persistence logic
