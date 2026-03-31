# SIM Standalone Prototype PRD

> **Status**: Draft
> **Date**: 2026-03-19
> **Type**: Product PRD at concept stage
> **Purpose**: Define a standalone prototype app inside this repo that keeps the existing Prism shell feel while stripping the smart-agent system down to two main feature lanes plus a decoupled connectivity support module.
> **Mental Model Companion**: `docs/to-cerb/sim-standalone-prototype/mental-model.md`
> **Unification Authority**: `docs/specs/base-runtime-unification.md`

---

## 1. Mental Model Recap

This mission is not "make the current agent app simpler."

This mission is:

- build a separate prototype app inside the same repository
- preserve the existing Prism visual shell so the product still looks like the current system
- remove the smart-agent identity and heavy agent orchestration
- keep only two main user-facing capabilities:
  - scheduler
  - general SIM chat plus Tingwu-based transcription/audio context
- retain the connectivity module as a supporting, hard-migrated system module for badge connection management

The product should feel like the same family of app, but operationally it is no longer an intelligent agent system.

It becomes a narrow, reusable, low-risk prototype focused on two straight pipelines, with connectivity retained as infrastructure rather than as a smart lane.

Unification note on **2026-03-31**:

- this concept still explains why the SIM path was created
- for future non-Mono delivery, the repo should now treat the current SIM-led shell/scheduler/audio direction as the best available **base-runtime baseline**
- Mono remains the later deeper architecture layer rather than a second shell/product truth
- the separate SIM entry/runtime boundary remains real implementation scaffolding, but it must not be read as a second non-Mono product line

---

## 2. Product Goal

Deliver a working prototype with minimum viable functionality for:

1. scheduling through the documented Scheduler Path A pipeline
2. normal SIM chatting backed by persona, user metadata, and local session history, plus long-form audio transcription/artifact display through the documented Tingwu/audio flow
3. standalone badge connectivity management through the existing decoupled connectivity contracts

The prototype must prove the user journey end to end without depending on agent memory, agent tooling, or the current multi-lane smart runtime.

---

## 3. In Scope

### 3.1 Feature A: Scheduler

The scheduler feature must reuse the existing documented Path A scheduler behavior.

Required constraints:

- use the existing scheduler shell and visual language as much as possible
- use Scheduler Path A as the only scheduling execution lane
- keep scheduler isolated from unrelated pipelines
- do not depend on CRM, Path B enrichment, plugin writeback, or agent-memory behavior

Behavioral authority:

- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-uni-a/spec.md`
- `docs/cerb-ui/scheduler/contract.md`

### 3.2 Feature B: Tingwu Transcription

The transcription feature must reuse the existing audio drawer and Tingwu pipeline concepts.

Required constraints:

- entry point is the Audio Drawer
- the drawer shows multiple audio cards
- the audio drawer is the informational, non-chat variant of the chat interface
- user can select an audio card and view existing intelligence artifacts
- artifacts may include transcript, chapters, summary, highlights, speaker/talker separation, questions/answers, and other Tingwu-returned structured results
- when a user opens a transcribed audio card, that implies the transcription job already exists or has completed
- selecting an already-transcribed audio must load the existing transcription/artifacts rather than rerunning Tingwu
- all source intelligence generation is API-driven through Aliyun capability rather than locally designed reasoning
- the final display may pass Tingwu output through a readability-polisher prompt, but it must not invent facts absent from Tingwu output
- transcript reveal may use native streaming if available, with pseudo-streaming fallback if native streaming is not viable

Behavioral authority:

- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/cerb/audio-management/spec.md`
- `docs/specs/modules/AudioDrawer.md`

### 3.3 Feature B2: General Chat and Audio Context

The SIM home/chat flow must work before any audio is selected, and audio may later enrich that same session.

Required constraints:

- blank/new SIM chat is a real conversation surface rather than a guidance-only placeholder
- baseline chat uses system persona plus user metadata plus local session history
- `Ask AI` starts or reuses a plain AI chat session with the chosen audio artifact attached as context
- the same chat session may receive audio attachment/reselection later without losing prior turns
- this is not the current agent runtime
- no Oasis-style memory system
- no smart agent identity
- no multi-tool agent planning layer
- the selected audio's transcript/artifacts act as additional chat context rather than the only legal chat basis

Upload behavior constraint:

- when the user taps audio upload inside chat, the app must reopen the Audio Drawer and ask the user to select one audio item
- the selected audio should enrich the current session instead of forcing a separate audio-only thread
- it must not jump into the native Android file manager from chat

### 3.4 Supporting Module: Connectivity

The connectivity module remains part of the prototype as a supporting, decoupled system module.

Required constraints:

- BLE and Wi-Fi connection management may take a hard migration approach
- the module must stay isolated from SIM scheduler and SIM audio/chat business logic
- the module exists to connect the badge and the app, not to revive smart-agent behavior
- SIM should expose a shell-level entry/icon for badge connection management

Behavioral authority:

- `docs/specs/connectivity-spec.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`

---

## 4. Out of Scope

The standalone prototype must not ship any of the following:

- agent memory system
- smart-agent orchestration
- CRM/entity enrichment
- Path B scheduler enrichment
- plugin-driven autonomous behavior
- badge-driven full auto pipeline as a required dependency
- any implementation that mutates or destabilizes the current agent app by default
- bespoke local AI analysis that replaces Tingwu as the source artifact producer

---

## 5. Product Principles

### 5.1 Same Shell, Smaller Brain

The shell should look like Prism.

Allowed:

- reuse the same main shell, chat shell, drawer language, and visual components
- make small UI adjustments to reflect the reduced product scope
- retain ordinary shell practices such as history, new page/session, connectivity entry, and settings entry when they do not reintroduce smart-agent behavior

Not allowed:

- redesign the app into a visibly different product family

### 5.2 Standalone Means Real Isolation

This prototype must live inside the repo but remain isolated from the current agent app.

The design target is:

- separate package or app target
- separate DI assembly/root composition
- separate persistence namespace where needed
- no accidental behavior regression in the main app

### 5.3 Reuse First

The prototype should aggressively reuse:

- existing UI shell components
- existing scheduler drawer UI
- existing audio drawer UI
- existing Tingwu and audio repository concepts
- existing connectivity bridge/service contracts
- existing scheduler Path A contracts

It should only fork or wrap code where reuse would pull unwanted smart-agent behavior into the prototype.

### 5.4 Two Pipelines, No Hidden Coupling

Scheduler and transcription are both first-class features, but they must stay independent.

The prototype is not allowed to secretly reconstruct the old "agent OS" through side channels.

### 5.5 Source-Led Audio, Polished Display

Tingwu remains the source system for transcription artifacts.

The prototype may:

- reformat Tingwu output for readability
- apply an additional prompt layer to polish noisy Tingwu output
- stage output through a temporary buffer before final display

The prototype may not:

- invent unsupported transcript structure
- silently replace missing Tingwu sections with fabricated content
- imply that local reasoning authored the source artifacts

### 5.6 Transparent State Is Presentation, Not False Backend Truth

The prototype may show transparent "thinking" states to make transcription progress legible.

Allowed examples:

- transcript unfolding then collapsing
- "transcribing", "summarizing", "matching chapters", "finding speakers", "highlighting"
- provider-native trace display when available

These states are cosmetic/presentation-oriented unless backed by real provider signals.
They must not claim nonexistent local pipeline stages as authoritative backend truth.

### 5.7 Naming Follows Boundary, Not Legacy Habit

Legacy names are not automatically protected.

The prototype should:

- keep existing names when they still describe clean ownership
- rename classes, modules, or seams when current names would hide contamination or misstate product behavior
- prefer explicit SIM-prefixed ownership where the current smart-app naming would blur runtime boundaries

The prototype should not:

- keep misleading legacy names only because they already exist
- introduce broad rename churn that does not materially improve ownership clarity

### 5.8 Protected Product First

When reuse convenience conflicts with safety, the current smart app is the protected product.

Decision rule:

- preserve smart-app stability first
- let SIM pay the extra wrapper, adapter, or rename cost when needed
- treat old architecture assumptions as candidates for re-verification, not as standing truth

---

## 6. Primary User Flows

### Flow 1: Schedule Something

1. User enters the familiar Prism shell.
2. User opens the scheduler surface.
3. User creates or updates a schedule item through the documented Path A route.
4. The system persists the scheduler result and reflects it in the scheduler UI.
5. No other pipeline is required to complete the action.

### Flow 2: Browse and Open Audio

1. User opens the Audio Drawer.
2. User sees audio cards.
3. User selects a transcribed card.
4. The app opens the audio detail/expanded card state.
5. The app displays transcript plus available Tingwu intelligence artifacts in an informational, non-chat conversation-style interface.

### Flow 3: Ask AI About One Audio

1. User opens a transcribed audio card.
2. User taps `Ask AI`.
3. The app opens chat with that audio pre-attached as context.
4. The user continues discussing the transcript, summary, chapters, highlights, or related returned artifacts.
5. The session answers from normal SIM chat context plus the selected audio context.

### Flow 3B: Select Already-Transcribed Audio

1. User opens the Audio Drawer.
2. User selects an audio card that already shows transcription preview.
3. The app loads existing persisted transcript/artifacts.
4. The app does not rerun Tingwu for that selection.

### Flow 3C: Transcribe New Audio

1. User opens the Audio Drawer.
2. User selects an audio item that is not yet transcribed.
3. The app starts Tingwu transcription.
4. The UI may show transcript streaming or pseudo-streaming plus cosmetic activity states.
5. The app polishes the returned artifact bundle for final display without inventing missing facts.

### Flow 4: Add Audio From Chat

1. User is inside a SIM chat session that may already contain normal chat turns.
2. User taps the audio upload/select affordance.
3. The app reopens the Audio Drawer.
4. The user selects one audio item.
5. The current session keeps its prior turns and adds or switches the active audio context instead of launching the Android file manager.

### Flow 5: Manage Badge Connectivity

1. User taps the connectivity entry/icon from the shell.
2. The app opens badge connection management.
3. The user can inspect connection state and perform setup/reconnect actions.
4. The flow runs through the decoupled connectivity module without coupling scheduler and audio/chat lanes together.

---

## 7. Architecture Boundaries

### 7.1 Standalone App Boundary

The prototype must be implemented as a standalone package/app path inside this repository.

Baseline boundary rules:

- no default DI sharing that makes the agent app runtime depend on the prototype
- no prototype shortcuts that edit agent-specific flows in place without a seam
- no data storage collisions unless intentionally shared and explicitly documented
- no UI routing changes that break the current agent version
- no cross-contamination between SIM shell support surfaces and smart-agent runtime-only surfaces

### 7.2 Scheduler Boundary

The scheduler lane may reuse current Path A infrastructure, but it must stop at Path A.

Specifically excluded from the prototype scheduler slice:

- Path B memory highway
- CRM/entity writeback
- unrelated plugin flows
- agent-specific contextual reasoning beyond what Path A already needs

### 7.3 Audio and Chat Boundary

The audio lane may reuse Tingwu/audio-management building blocks.

The chat lane must remain simple:

- one audio-bound context source
- no persistent agent memory requirement
- no hidden reuse of the full agent orchestration stack unless it can be sandboxed behind a prototype-only adapter
- no rerun of Tingwu when a selected audio is already transcribed
- no final artifact display that bypasses the readability-polisher contract

### 7.4 Connectivity Boundary

The connectivity module may be hard-migrated into SIM as an isolated support module.

Boundary rules:

- reuse existing connectivity contracts instead of re-specifying BLE/Wi-Fi behavior locally
- keep connectivity UI entry in the shell, but keep connectivity business logic decoupled from scheduler and audio/chat flows
- treat connectivity as supporting infrastructure, not as a third smart-agent lane

---

## 8. Prototype Acceptance Definition

The mission is successful when all of the following are true:

- the repo contains a standalone prototype app path that does not contaminate the current agent app
- the prototype visually matches the current Prism shell closely enough to feel like the same product family
- scheduler works through the documented Path A lane
- audio drawer supports selecting audio and opening transcribed results
- Tingwu artifacts are visible from the audio detail flow
- already-transcribed audio loads existing artifacts without rerunning Tingwu
- final artifact display is source-led but readability-polished
- blank/new SIM chat works without requiring audio first
- `Ask AI` works as a fast path that enters chat with one audio already attached
- chat-side audio selection reopens the Audio Drawer instead of the native Android picker
- badge connectivity management remains available through the decoupled connectivity module

---

## 9. Implementation Strategy

### Phase 0: Product and Boundary Lock

- freeze scope to the two main feature lanes plus the decoupled connectivity support module
- define the standalone package/app boundary
- identify exact reuse seams for shell, scheduler, audio drawer, simple chat, and connectivity entry

### Phase 1: Shell Extraction

- reuse `AgentIntelligenceScreen`, drawer surfaces, and related shell components only where they can be safely isolated
- preserve ordinary shell practices such as history, new page/session, connectivity entry, and settings where they align with SIM
- remove or bypass smart-agent specific states from the standalone flow

### Phase 2: Scheduler Slice

- wire only the Path A scheduler route
- preserve current scheduler UI behavior where it already aligns with spec
- block non-Path-A dependencies from entering the prototype

### Phase 3: Audio Slice

- reuse Audio Drawer behavior and card model
- bind audio cards to Tingwu-backed artifact display
- ensure transcribed-card open behavior is first-class
- add the readability-polisher layer between raw Tingwu returns and final artifact display
- define transcript streaming vs pseudo-streaming fallback

### Phase 4: Simple Chat Slice

- create general SIM chat without agent-memory features
- support persona + user metadata + local session history as the base context
- support `Ask AI` from audio detail as a fast-path with audio pre-attached
- support drawer-based audio re-selection from chat without breaking the current session

### Phase 4B: Connectivity Hard Migration

- keep the existing connectivity bridge/contracts as the behavioral source
- expose a shell-level connectivity entry/icon
- keep connection management isolated from scheduler and audio/chat execution

### Phase 5: Packaging and Safety

- prove no regression in the current agent app
- prove standalone entry and storage boundaries
- define verification slices for prototype-only behavior

---

## 10. Open Questions

These do not block PRD creation, but they must be resolved before implementation planning is frozen.

1. Should the standalone chat use an existing executor/model path with a prototype-only adapter, or a narrower provider-specific chat client?
2. Should the prototype share any persistence tables with the current app, or should all prototype data be namespaced?
3. Should badge/manual sync be included in the first prototype cut, or should the first cut assume audio items already exist in the drawer?
4. Which exact shell states from `AgentIntelligenceScreen` are preserved, and which are removed or replaced for the simple chat flow?
5. How much of the provider-native Tingwu trace is usable directly for transparent-state display before pseudo-state fallback is needed?

---

## 11. Initial Reuse Targets

Likely code anchors for later planning:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt`
- connectivity bridge and existing connectivity modal/viewmodel stack

These are reuse candidates, not automatic approvals for in-place modification.

---

## 12. Next Documents

This PRD should feed the next layer:

- `docs/plans/sim-tracker.md`
- prototype-specific Cerb shards for shell, scheduler, audio, simple chat, and connectivity
- later interface-map updates only after the standalone boundary is concretely designed
