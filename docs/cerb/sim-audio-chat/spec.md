# SIM Audio Chat Spec

> **Scope**: Audio drawer, Tingwu artifact display, readability polishing, transparent-state presentation, and simple audio-grounded chat for the standalone SIM prototype
> **Status**: SPEC_ONLY
> **Behavioral Authority Above This Doc**:
> - `docs/core-flow/sim-audio-artifact-chat-flow.md`
> - `docs/to-cerb/sim-standalone-prototype/concept.md`
> - `docs/cerb/audio-management/spec.md`
> - `docs/cerb/tingwu-pipeline/spec.md`
> - `docs/specs/modules/AudioDrawer.md`
> **Audit Evidence**:
> - `docs/reports/20260319-sim-standalone-code-audit.md`
> - `docs/reports/20260319-sim-clarification-evidence-audit.md`

---

## 1. Purpose

`SIM Audio Chat` defines the second major feature lane of the standalone prototype:

- browse audio in the Audio Drawer
- transcribe through Tingwu-backed services
- open a transcribed card and view artifacts in an informational conversation-style interface
- enter a simple audio-grounded chat as continuation of that discussion

This shard intentionally replaces the smart-agent interpretation of chat with a narrow audio-grounded product.

---

## 2. Included Scope

### Audio Drawer

- list audio cards
- support SmartBadge-synced inventory as the real product audio source
- expose a browse-mode manual sync action for badge-origin inventory refresh
- allow a best-effort automatic badge sync when the browse drawer opens and connectivity is already ready
- allow phone-local/manual audio import only as a testing convenience when explicitly enabled for QA/dev validation
- support a browse-mode transcribe action aligned to the Audio Drawer swipe UX
- support opening transcribed cards in browse mode
- support a chat-side select mode when the drawer is reopened from chat attach/upload
- in chat-side select mode, cards themselves are the action surface; no dedicated per-card bottom CTA is required
- in chat-side select mode, swipe and expansion affordances are suppressed so the surface reads as a picker rather than a gallery
- in chat-side select mode, already-transcribed cards should expose a truncated transcript preview so users can recognize the audio content before selecting it
- act as the informational, non-chat variant of the chat experience

### Artifact Display

- transcript
- summary
- chapters
- highlights
- speaker/talker-related Tingwu sections when present
- questions/answers or adjacent provider-returned sections when present
- readability polishing over Tingwu output when needed
- transcript streaming or pseudo-streaming presentation when needed

### Simple Chat

- `Ask AI` starts a plain chat session for one selected audio
- chat answers are grounded in the chosen audio's artifacts
- real free-text replies belong only to audio-grounded sessions opened from `Ask AI`; blank/new SIM chat remains a shell surface that may guide the user into a supported audio discussion, but it must not silently widen into a general assistant
- audio re-selection from chat reopens the Audio Drawer
- if an audio is already transcribed, SIM loads stored artifacts instead of rerunning Tingwu
- if an audio is still pending when selected from the chat-side drawer route, SIM binds the discussion session immediately to that audio and the same SIM Tingwu pipeline continues inside chat rather than forcing the user back through the drawer-first transcription path
- when a chat-bound pending audio completes, SIM appends the finished artifact content as a durable chat history turn rather than leaving the result only in transient progress/completion UI
- artifacts or status produced through that chat-side route must update the shared SIM audio inventory so the drawer reflects the same result without a second transcription run
- while one audio item is already pending/transcribing, its drawer-origin transcribe trigger must stay locked so the user cannot start a duplicate run for that same item
- newly appended chat artifact messages may use transcript pseudo-streaming once, but must not replay that reveal on later history reentry
- if the transcript for that chat artifact message exceeds 4 rendered lines during first reveal, SIM may mark it as a long transcript immediately, but should delay collapse briefly so the first reveal is still readable
- the default readable-reveal dwell for that first long transcript pass is about 1 second before collapse is allowed
- later history reentry must reopen long transcripts in collapsed form by default

---

## 3. Excluded Scope

- smart-agent memory system
- plugin task board
- mascot behavior
- generalized autonomous tool execution
- native file manager jump from chat-side audio selection
- treating phone-local upload as the production ingress model

---

## 4. Ownership

`SIM Audio Chat` owns:

- the standalone audio drawer interaction contract
- the informational-audio-view to discussion-chat relationship
- the standalone audio-to-chat handoff
- the simple chat context rule for one chosen audio

`SIM Audio Chat` may reuse:

- `AudioDrawer`
- most of `AudioViewModel` behavior
- most of `RealAudioRepository` behavior
- `ConnectivityBridge` only as badge-origin recording ingress and badge file-operations backend

`SIM Audio Chat` must not assume:

- current smart chat/session orchestration is safe to reuse unchanged
- current storage names are safe for the prototype
- connectivity owns SIM chat/session/artifact behavior

---

## 5. Storage Rule

The standalone prototype must namespace its audio persistence if it shares the same runtime/storage container as the smart app.

Minimum namespace targets:

- metadata file
- local audio blob naming
- artifact file naming
- audio/chat binding records if persisted
- session metadata and durable chat history if persisted

If SIM receives a separate application ID and file space, this requirement may be satisfied by that app boundary instead.

### T6.2 Accepted V1 Namespace

The current accepted V1 storage contract is now explicit and matches the shipped SIM repository path:

- metadata file: `sim_audio_metadata.json`
- local audio blobs: `sim_<audioId>.<ext>`
- artifact files: `sim_<audioId>_artifacts.json`
- audio/chat binding record: persisted as `boundSessionId` inside `sim_audio_metadata.json`

This means V1 does not require a separate binding file.

Contrast against the smart/default path:

- smart/default audio persistence still uses non-SIM names such as `audio_metadata.json`, plain `<audioId>.wav`, and plain `<audioId>_artifacts.json`
- SIM therefore does not silently share the same audio persistence filenames by default

### T6.3 Accepted V1 Session Persistence

The current accepted V1 chat persistence contract is also SIM-only:

- session metadata file: `sim_session_metadata.json`
- per-session durable history file: `sim_session_<sessionId>_messages.json`
- durable message types are limited to user text, AI response, AI audio artifacts, and AI error
- transient UI state such as input text, sending/thinking state, toast/error presentation, and transcript reveal state is not persisted
- normal runtime does not seed demo sessions
- cold start restores stored sessions, but does not auto-select an active session

### Binding Reconciliation Rule

At startup, SIM must treat the session store as the source of truth for session existence and treat audio metadata as the source of truth only for the audio-side binding pointer.

This means:

- deleting a linked SIM session must clear the audio entry's `boundSessionId`
- if audio metadata points to a missing SIM session, SIM clears that dangling audio binding
- if a persisted SIM session still links an existing audio item, SIM restores the missing audio-side binding
- if a persisted SIM session links an audio item that no longer exists, SIM unlinks that session from the missing audio
- if duplicate persisted sessions claim the same audio item, SIM keeps only the newest linked session and normalizes older sessions to `linkedAudioId = null`

---

## 6. Chat Context Rule

The chat for SIM is narrow.

It is allowed to:

- continue discussion that begins from the informational audio drawer
- load one audio's transcript and structured artifacts as context
- show transparent in-chat processing states when pending audio is selected from the chat-side upload/reselect route
- bind pending chat-side audio immediately to the discussion session before Tingwu finishes
- answer follow-up questions about that audio

For drawer-side informational browsing specifically:

- transcribed cards start collapsed by default in the inventory list
- the expanded card is the informational artifact view once the user opens it
- user-toggled expand/collapse state must remain stable during scrolling and recomposition for the current drawer-open session
- reopening the drawer may reset cards back to the collapsed default

For chat-side audio reselection specifically:

- the drawer should present as a static selector rather than as the full interactive gallery
- the title/copy should make selection intent obvious rather than teaching swipe
- the whole card is the selection target; no dedicated `在聊天中处理`-style button is required
- swipe-to-transcribe, swipe-left quick actions, expansion, and `Ask AI` are suppressed in this mode
- the currently bound audio item should render as disabled/current rather than as a selectable card
- already-transcribed cards should show a truncated transcript preview so the user can recognize content quickly
- pending and transcribing cards should explain that processing can continue inside the current chat once selected

It is not allowed to:

- behave like the current agent OS
- accumulate broad session memory beyond the immediate audio-grounded conversation
- use the plugin/tool board as its primary interaction model

### Chat Surface Reuse Boundary

The SIM chat surface may now host more than one SIM session kind, but that does not merge their contracts.

Allowed session kinds on the reused chat shell:

- audio-grounded discussion
- Wave 8 task-scoped scheduler follow-up created from badge-origin scheduler completion

Rules:

- audio-grounded context and scheduler-follow-up context must remain separate in persistence and runtime state
- scheduler follow-up must not inherit audio artifact assumptions
- audio-grounded chat must not gain scheduler mutation rights unless the shell explicitly opened a scheduler-follow-up session

### Two Entry / One Pipeline Rule

SIM audio processing has two entry surfaces:

- drawer-origin transcription from the informational audio drawer
- chat-origin audio reselection via the chat upload/attach action, which reopens the same drawer as a selector

These are not two separate Tingwu systems.

They must:

- reuse the same SIM-owned transcription/artifact persistence
- reuse stored artifacts when the chosen audio is already transcribed
- let pending audio selected from chat bind immediately to that discussion session and continue processing inside chat transparency
- append completed artifact content into durable chat history when that pending run finishes
- lock duplicate transcribe triggers for the same in-flight audio item across drawer/chat entry surfaces
- reflect completed artifacts/status back into the audio drawer inventory without requiring a second run through the drawer-origin path
- keep chat-side reselection visually distinct from browse mode so users are not invited to use suppressed swipe gestures

### Completion Ownership Rule

For the current SIM prototype, `SimShell` is the completion bridge between shared audio state and chat history:

- `SimShell` observes the shared audio entry until terminal completion/failure
- on completion, `SimShell` loads persisted artifacts from the shared SIM repository seam
- `SimShell` passes render-ready artifact content into the SIM chat owner as a durable history turn
- `SimAgentViewModel` remains the owner of chat/session history, but does not become the repository owner just to fetch artifacts

This keeps one artifact source of truth while avoiding an unnecessary second repository seam inside the SIM chat runtime.

### Product Ingress Rule

For the real product, SIM audio enters from badge synchronization:

- BLE/Wi-Fi badge connectivity remains the production source of recordings
- chat attach/reselect must reopen the SIM drawer over that SIM-owned inventory
- the product contract does not require arbitrary phone-local uploads

### Badge Sync Trigger Rule

SIM must expose badge-origin audio ingress through the audio drawer itself rather than forcing users through connectivity-only surfaces.

- browse-mode drawer exposes a visible secondary action for manual badge sync
- chat-reselect mode does not expose that manual sync action because selection should stay focused on the current inventory
- manual sync reuses the existing SIM-owned repository path that lists badge WAV files and downloads unseen recordings
- manual sync is additive-only: it must not clear existing inventory and must not duplicate or redownload already-imported badge files
- the dedupe key for this slice is the exact badge filename already present in SIM local `SMARTBADGE` inventory
- if connectivity is absent, offline, or not ready, manual sync fails explicitly in human-readable drawer-local feedback and leaves the current inventory usable

### Automatic Sync Rule

Automatic sync is allowed, but it must stay narrow and non-blocking.

- auto-sync may run only when the browse drawer becomes visible
- auto-sync runs at most once per drawer-open session, not on every recomposition
- auto-sync must not run in chat-reselect mode
- auto-sync should start only when connectivity is already ready; if readiness is absent, SIM skips the sync attempt instead of surfacing a blocking failure
- auto-sync readiness must come from `ConnectivityBridge.isReady()` or a SIM-owned repository helper wrapping that preflight rather than from shell UI `ConnectionState`
- if an auto-sync attempt begins and later fails, the failure may surface as non-blocking drawer feedback, but it must not hijack shell routing or clear existing inventory
- auto-sync and manual sync share the same repository/import path so badge-origin files enter one SIM-owned inventory
- repeated badge `/list` checks are allowed, but repeated local download/import of the same badge filename is not

Connectivity boundary for this ingress:

- connectivity may supply badge file list/download/delete behavior through `ConnectivityBridge`
- connectivity must not become the owner of SIM audio/chat session flow, artifact persistence, or Tingwu completion ownership
- if connectivity is absent or offline, manual/local QA paths and already-persisted artifacts must still remain usable

For debug-only acceptance support, SIM may expose local scenario-seeding controls inside the drawer browse surface:

- these controls are debug-only and must not redefine product ingress
- they may seed dedicated SIM test cards for failure, missing-sections, or provider-fallback validation
- they must stay local to the SIM audio lane rather than re-enabling the global debug HUD

Phone-local import may still exist for testing, but only under these rules:

- it is explicitly test-only and must be documented as such
- it must feed the same SIM-owned repository and Tingwu/artifact pipeline after ingestion
- it must not redefine the product story or lower the badge-sync requirement in downstream docs

---

## 7. Reuse Direction

### Strong Reuse Candidate

- `AudioDrawer`

### Conditional Reuse Candidate

- `AudioViewModel`
  - only after reviewing which parts are safe to keep versus replace for SIM chat/session rules

### Storage Reuse Candidate

- `RealAudioRepository`
  - only after namespacing is explicit

### Do Not Reuse Directly

- `AgentViewModel` as the audio chat runtime

---

## 8. Human Reality

### Organic UX

Users do not care whether the shell is using the same hidden runtime.
They care that:

- the audio drawer feels familiar
- opening a transcribed card is immediate and legible
- `Ask AI` feels direct and focused

### Data Reality

Tingwu artifacts arrive as structured external-service output.
SIM should remain source-led instead of pretending to synthesize them locally.
SIM may still apply a readability-polisher prompt to reduce noisy provider output before final display.

### Legacy Reality

Current code already provides partial audio-to-chat continuation seams and artifact persistence.
Those seams are reuse candidates, not proof that the current smart chat runtime should remain the SIM owner.

### Transparent State Reality

The UI may show transcript streaming, pseudo-streaming, and agent-like activity labels such as summarizing or finding speakers.

These are presentation states unless directly backed by provider-native signals.
The spec does not require fake backend stage fidelity.

For chat-side durable artifact history specifically:

- one-time transcript reveal memory belongs to the SIM chat runtime rather than the durable artifact payload itself
- the shared artifact renderer must accept host-provided transcript presentation props so chat and drawer can diverge without forking the artifact content model
- chat may add a minimum reveal dwell before auto-collapsing a long transcript, but that dwell remains presentational rather than a provider-backed progress claim
- the drawer may keep its current informational artifact presentation unless the product contract is updated later

### Failure Gravity

The worst failure is silently reintroducing the smart-agent chat runtime because `Ask AI` seems "close enough."

That would destroy the product boundary.

---

## 9. Wave Plan

| Wave | Focus | Status | Deliverable |
|------|-------|--------|-------------|
| 1 | Audio/chat contract freeze | PLANNED | SIM audio/chat spec/interface |
| 2 | Storage namespace decision | PLANNED | prototype-safe repository behavior |
| 3 | Audio drawer and artifact render | PLANNED | transcribed-card informational flow |
| 4 | Polisher and transparent-state layer | PLANNED | readability + presentation behavior |
| 5 | Simple chat and reselection | PLANNED | `Ask AI` and drawer-based audio reselection |

---

## 10. Done-When Definition

SIM audio/chat is ready only when:

- audio drawer works in the standalone app
- transcribed audio cards open and show source-led, readability-polished artifacts
- already-transcribed audio loads existing artifacts without rerunning Tingwu
- `Ask AI` opens a simple audio-grounded chat continuation surface
- selecting pending audio from chat continues the same SIM transcription pipeline inside chat with transparent waiting/progress states
- artifacts produced from the chat-side route are reflected back in the audio drawer without duplicate processing
- chat-side audio selection returns to the drawer rather than Android file manager
- audio/session persistence does not contaminate the smart app
