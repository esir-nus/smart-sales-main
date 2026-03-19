# SIM Mission Tracker

> **Mission**: Standalone prototype app with two main feature lanes, Scheduler Path A and Tingwu transcription plus simple audio-grounded chat, plus a decoupled connectivity support module.
> **Status**: Wave 1 Implemented / Runtime Verification Pending
> **Started**: 2026-03-19
> **Current Gate**: `SimMainActivity` boot path is proven; remaining Wave 1 gate is support-surface runtime verification and final closeout
> **Primary Product Doc**: `docs/to-cerb/sim-standalone-prototype/concept.md`
> **Mental Model Doc**: `docs/to-cerb/sim-standalone-prototype/mental-model.md`
> **Implementation Brief**: `docs/plans/sim_implementation_brief.md`
> **Wave 1 Execution Brief**: `docs/plans/sim-wave1-execution-brief.md`
> **Code Audit**: `docs/reports/20260319-sim-standalone-code-audit.md`
> **Clarification Audit**: `docs/reports/20260319-sim-clarification-evidence-audit.md`
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

- [ ] **T1.1: Flow / Entry Contract**
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
- [ ] **T1.6: Validation**
  - [x] `:app-core:compileDebugKotlin` passes with the standalone SIM shell slice
  - [x] prove SIM shell boot path at runtime
  - [ ] prove preserved shell practices do not depend on smart runtime
  - [ ] prove smart-only shell surfaces are not part of normal SIM operation
- [ ] **Done When**
  - [ ] SIM boots into a standalone shell
  - [ ] shell visually feels like Prism
  - [ ] smart-only shell surfaces are not part of normal SIM use

### Wave 2: Audio Drawer Informational Mode
> Objective: Deliver the audio drawer as the informational transcript/artifact interface, sourced from Tingwu returns and finalized through the SIM readability-polisher layer.
> **Owning Shard**: `docs/cerb/sim-audio-chat/spec.md`
> **Behavioral Authority**: `docs/core-flow/sim-audio-artifact-chat-flow.md`
> **Execution Law**: the UI may format and polish Tingwu output, but it must not invent local semantic substitutes.
> **Validation Requirement**: prove that the UI displays source-led artifacts faithfully, does not rerun already-transcribed audio, and does not synthesize absent sections.

- [ ] **T2.1: Flow / Audio Inventory Strategy**
  - [ ] decide first-cut inventory source: local/manual only vs include SmartBadge sync
  - [ ] make the decision explicit in docs and implementation brief
- [ ] **T2.2: Spec / Artifact Surface Contract**
  - [ ] render transcript
  - [ ] render summary
  - [ ] render chapters
  - [ ] render highlights
  - [ ] render speaker/talker-related sections when Tingwu returns them
  - [ ] render provider-returned adjacent sections such as questions/answers when present
  - [ ] leave absent artifact sections absent rather than locally inventing replacements
- [ ] **T2.3: Code / Informational Drawer Behavior**
  - [ ] make expanded transcribed card the primary informational view
  - [ ] keep the drawer read-only from a conversation perspective
  - [ ] ensure the view feels like a non-chat variant of the chat experience
- [ ] **T2.4: Code / Transcription Lifecycle**
  - [ ] support pending -> transcribing -> transcribed flow
  - [ ] reuse existing artifacts when audio is already transcribed
  - [ ] surface failure explicitly
  - [ ] support retry-ready behavior without faking artifact completion
- [ ] **T2.5: Code / Polisher and Transparent-State Layer**
  - [ ] add readability-polisher step over Tingwu output
  - [ ] define transcript native-streaming vs pseudo-streaming fallback
  - [ ] define cosmetic activity states without pretending backend truth
- [ ] **T2.6: Validation**
  - [ ] prove returned Tingwu artifacts remain source-led after polishing
  - [ ] prove already-transcribed audio does not rerun Tingwu
  - [ ] prove missing Tingwu sections are not locally invented
  - [ ] prove transcription failure remains explicit
- [ ] **Done When**
  - [ ] users can browse audio in SIM
  - [ ] transcribed cards open and display Tingwu-returned artifacts in readable form
  - [ ] the drawer clearly reads as informational mode, not as a separate smart workflow

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
  - [ ] do not default to Android file manager for this branch
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
  - [ ] explicit safe-fail feedback
- [ ] **T4.3: Code / Suppressed Branches**
  - [ ] suppress Path B
  - [ ] suppress CRM/entity enrichment
  - [ ] suppress plugin-driven scheduler re-entry
  - [ ] defer optional extras that force contamination
- [ ] **T4.4: Validation**
  - [ ] define Path A-only tests
  - [ ] define ambiguous/no-match safe-fail tests
  - [ ] prove scheduler still preserves conflict-visible behavior
- [ ] **Done When**
  - [ ] scheduler works in the standalone shell
  - [ ] Path A laws remain intact
  - [ ] no hidden Path B dependency is required for normal scheduler use

### Wave 5: Connectivity Hard Migration
> Objective: Reuse the mature badge connectivity stack as a decoupled SIM support module.
> **Owning Shard**: `docs/cerb/sim-connectivity/spec.md`
> **Behavioral Authority**:
> - `docs/cerb/sim-connectivity/spec.md`
> - `docs/specs/connectivity-spec.md`
> - `docs/cerb/connectivity-bridge/spec.md`
> **Execution Law**: connectivity may be hard-migrated, but it must remain infrastructure/support behavior rather than reviving smart-agent coupling.
> **Validation Requirement**: prove connectivity entry works in SIM without coupling scheduler and audio/chat flows together.

- [ ] **T5.1: Flow / Shell Entry Contract**
  - [ ] define shell-level connectivity entry/icon
  - [ ] define return path back into normal SIM shell use
- [ ] **T5.2: Spec / Hard Migration Boundary**
  - [ ] keep BLE/Wi-Fi behavior under existing connectivity contracts
  - [ ] define what SIM reuses unchanged
  - [ ] define what SIM wraps at shell level only
- [ ] **T5.3: Code / Connectivity Isolation**
  - [ ] keep connection management isolated from scheduler and audio/chat business logic
  - [ ] avoid smart-agent dependencies in connectivity entry path
- [ ] **T5.4: Validation**
  - [ ] prove connectivity entry opens usable badge connection management
  - [ ] prove scheduler and audio/chat do not require connectivity to be meaningful
- [ ] **Done When**
  - [ ] connectivity remains available in SIM
  - [ ] connectivity remains decoupled from the two main feature lanes

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
- [ ] **T6.2: Code / Audio Persistence Namespace**
  - [ ] namespace metadata file
  - [ ] namespace local audio blob naming
  - [ ] namespace artifact file naming
  - [ ] namespace audio/chat binding records if needed
- [ ] **T6.3: Spec / Session-Chat Persistence Review**
  - [ ] decide whether session storage is shared or namespaced
  - [ ] document the reason
  - [ ] enforce it in code plan
- [ ] **T6.4: Validation**
  - [ ] prove smart and SIM runtime roots are distinct enough
  - [ ] prove persistence cross-contamination is controlled
- [ ] **Done When**
  - [ ] SIM and smart app do not silently share unsafe runtime state
  - [ ] persistence cross-contamination risk is explicitly controlled

### Wave 7: Verification and Acceptance
> Objective: Prove the SIM app works while the smart app remains intact.
> **Execution Law**: SIM acceptance must include isolation proof, not only feature proof.

- [ ] **T7.1: Feature Acceptance**
  - [ ] shell routing proof
  - [ ] audio informational-mode proof
  - [ ] `Ask AI` continuation proof
  - [ ] scheduler Path A proof
  - [ ] connectivity support-module proof
- [ ] **T7.2: Isolation Acceptance**
  - [ ] smart app boot/regression check
  - [ ] storage namespace isolation check
  - [ ] DI/root isolation sanity check
- [ ] **T7.3: Doc Sync and Closeout**
  - [ ] update SIM tracker statuses
  - [ ] sync related Cerb docs if implementation changes lower-layer details
  - [ ] decide whether `docs/cerb/interface-map.md` now needs SIM registration
- [ ] **Done When**
  - [ ] SIM feature slice is working
  - [ ] smart app remains unaffected
  - [ ] docs and verification evidence are in sync

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
