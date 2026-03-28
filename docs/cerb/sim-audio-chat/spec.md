# SIM Audio Chat Spec

> **Scope**: Audio drawer, Tingwu artifact display, readability polishing, transparent-state presentation, and general SIM chat with optional audio context for the standalone SIM prototype
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

- provide normal SIM chat directly from the home/chat surface
- browse audio in the Audio Drawer
- transcribe through Tingwu-backed services
- open a transcribed card and view artifacts in an informational conversation-style interface
- enter chat with audio already attached or attach audio mid-session

This shard intentionally replaces the smart-agent interpretation of chat with a SIM-local persona-backed chat product that may optionally carry audio context.

---

## 2. Included Scope

### Audio Drawer

- list audio cards
- support SmartBadge-synced inventory as the real product audio source
- expose a browse-mode manual sync action for badge-origin inventory refresh
- allow a best-effort automatic badge sync when the browse drawer opens and connectivity is already ready
- allow phone-local/manual audio import only as a testing convenience when explicitly enabled for QA/dev validation
- support a browse-mode right-swipe transcribe action aligned to the Audio Drawer swipe UX
- support a browse-mode left-swipe delete action for removing collapsed pending or collapsed transcribed audio items
- support opening transcribed cards in browse mode
- support a chat-side select mode when the drawer is reopened from chat attach/upload
- in chat-side select mode, cards themselves are the action surface; no dedicated per-card bottom CTA is required
- in chat-side select mode, swipe and expansion affordances are suppressed so the surface reads as a picker rather than a gallery
- in chat-side select mode, delete and any other destructive behavior are suppressed
- in chat-side select mode, already-transcribed cards should expose a truncated transcript preview so users can recognize the audio content before selecting it
- act as the informational, non-chat variant of the chat experience

### Artifact Display

- transcript
- summary
- speaker-summary recap when present
- question/answer recap when present
- chapters
- highlights
- speaker/talker-related Tingwu sections when present
- provider-resolved speaker identity labels when present
- provider-returned keywords / `KeyInformation` when present
- questions/answers or adjacent provider-returned sections when present
- readability polishing over Tingwu output when needed
- transcript streaming or pseudo-streaming presentation when needed

### Simple Chat

- blank/new SIM chat supports real free-text replies
- baseline chat uses SIM system persona plus user metadata plus local session history
- blank/new SIM chat may use device speech recognition from the composer to draft text locally, but it must still require explicit send
- `Ask AI` starts or reuses a chat session with one selected audio attached as context
- chat answers may use the chosen audio's artifacts when audio is attached, but audio is not required for a normal SIM chat turn
- the SIM composer uses mic only while the draft is blank; once typed or recognized draft text exists, the same trailing action becomes send
- speech-recognition success writes transcript into the existing input field instead of appending a user message directly
- no-match, cancellation, permission denial, or recognizer failure must reset calmly back to editable chat without auto-send or history mutation
- audio re-selection from chat reopens the Audio Drawer
- if an audio is already transcribed, SIM loads stored artifacts instead of rerunning Tingwu
- if an audio is already transcribed when attached into chat, SIM appends the shared artifact card into durable chat history rather than relying on a long plain-text preview dump
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
- widening scheduler-follow-up voice mutation through the general SIM chat composer

---

## 4. Ownership

`SIM Audio Chat` owns:

- the standalone audio drawer interaction contract
- the informational-audio-view to discussion-chat relationship
- the standalone audio-to-chat handoff
- the simple chat context rule for persona-backed chat plus optional attached audio

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

The chat for SIM is general-chat-first but still SIM-scoped.

It is allowed to:

- start from a blank/new SIM chat session with no audio selected yet
- use system persona plus user metadata plus local SIM session history as the baseline context
- continue discussion that begins from the informational audio drawer
- load one selected audio's transcript and structured artifacts as additional context
- let the same session gain or switch active audio context later through chat-side upload/reselect
- show transparent in-chat processing states when pending audio is selected from the chat-side upload/reselect route
- bind pending chat-side audio immediately to the discussion session before Tingwu finishes
- answer follow-up questions about attached audio while preserving the same session's prior turns

For drawer-side informational browsing specifically:

- transcribed cards start collapsed by default in the inventory list
- the expanded card is the informational artifact view once the user opens it
- user-toggled expand/collapse state must remain stable during scrolling and recomposition for the current drawer-open session
- reopening the drawer may reset cards back to the collapsed default
- collapsed pending cards use browse-mode directional swipe actions: right swipe starts transcription and left swipe deletes the item
- collapsed transcribed cards may also use browse-mode left swipe delete, but expanded artifact cards must not expose delete swipe
- transcribing cards must not expose delete swipe

For chat-side audio reselection specifically:

- the drawer should present as a static selector rather than as the full interactive gallery
- the title/copy should make selection intent obvious rather than teaching swipe
- the whole card is the selection target; no dedicated `在聊天中处理`-style button is required
- browse-mode swipe actions, expansion, delete, and `Ask AI` are suppressed in this mode
- the currently bound audio item should render as disabled/current rather than as a selectable card
- already-transcribed cards should show a truncated transcript preview so the user can recognize content quickly
- pending and transcribing cards should explain that processing can continue inside the current chat once selected
- select-mode cards should use a compact one-line header layout: filename plus timestamp plus star
- select-mode state should be conveyed mainly by the row-body preview/copy rather than by extra status pills
- only the current bound item should keep an explicit inline marker such as `当前讨论中`
- select-mode cards should not add badge-vs-phone source iconography or source-label chrome just to explain state

It is not allowed to:

- behave like the current agent OS
- accumulate smart-runtime memory beyond SIM-local session history
- use the plugin/tool board as its primary interaction model

### Chat Surface Reuse Boundary

The SIM chat surface may now host more than one SIM session kind, but that does not merge their contracts.

Allowed session kinds on the reused chat shell:

- general discussion
- audio-grounded discussion
- Wave 8 task-scoped scheduler follow-up created from badge-origin scheduler completion

Rules:

- general discussion, audio context, and scheduler-follow-up context must remain separate in persistence and runtime state even when they reuse one shell
- a general discussion session may become audio-enriched later without becoming a scheduler-follow-up session
- scheduler follow-up must not inherit audio artifact assumptions
- general or audio-enriched chat must not gain scheduler mutation rights unless the shell explicitly opened a scheduler-follow-up session

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

### Provider Identity And Keyword Rule

For this slice, SIM treats Tingwu as the source of truth for enriched speaker labeling and keyword extraction when that data is returned.

- the client may send user-metadata-derived identity hints upstream to Tingwu, but SIM must not treat local metadata as a direct speaker-label override
- speaker labels shown in the artifact surface should prefer provider-returned identity-recognition labels over plain diarization ids when available
- if provider identity output is missing, the UI should fall back to diarization/raw speaker ids rather than inventing names locally
- normalized provider keywords may be rendered as small chips on SIM detail surfaces
- the expanded artifact body remains text-first; keyword chips are a supplement rather than a replacement for transcript/summary content
- provider result links remain available for debug/backend handling, but are not a SIM user-facing section
- provider speaker recap and question/answer recap should render as standalone sections rather than being folded into `摘要`

### Completion Ownership Rule

For the current SIM prototype, `SimShell` remains the completion bridge for pending-to-complete runs, while the chat owner handles already-transcribed attach:

- `SimShell` observes the shared audio entry until terminal completion/failure
- on completion, `SimShell` loads persisted artifacts from the shared SIM repository seam
- `SimShell` passes render-ready artifact content into the SIM chat owner as a durable history turn
- when the user attaches an already-transcribed audio item, the SIM chat owner may load persisted artifacts immediately and append the same durable artifact card without waiting for shell completion callbacks

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
- if connectivity manager diagnostics say BLE is held but network status is pending or offline, manual sync must stop before repository download work begins and surface a state-specific message instead of collapsing to the generic unavailable copy
- those BLE-held manager diagnostics remain observational only; SIM manual sync still requires strict transport readiness from `ConnectivityBridge.isReady()`

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

- normal chatting is available immediately
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

The worst failure is silently reintroducing the smart-agent chat runtime because general chat or `Ask AI` seems "close enough."

That would destroy the product boundary.

---

## 9. Wave Plan

| Wave | Focus | Status | Deliverable |
|------|-------|--------|-------------|
| 1 | Audio/chat contract freeze | PLANNED | SIM audio/chat spec/interface |
| 2 | Storage namespace decision | PLANNED | prototype-safe repository behavior |
| 3 | Audio drawer and artifact render | PLANNED | transcribed-card informational flow |
| 4 | Polisher and transparent-state layer | PLANNED | readability + presentation behavior |
| 5 | General chat plus audio reselection | PLANNED | persona-backed SIM chat, `Ask AI`, and drawer-based audio reselection |

---

## 10. Done-When Definition

SIM audio/chat is ready only when:

- audio drawer works in the standalone app
- transcribed audio cards open and show source-led, readability-polished artifacts
- already-transcribed audio loads existing artifacts without rerunning Tingwu
- blank/new SIM chat answers normally without requiring audio first
- `Ask AI` opens chat with one audio already attached
- selecting pending audio from chat continues the same SIM transcription pipeline inside chat with transparent waiting/progress states
- artifacts produced from the chat-side route are reflected back in the audio drawer without duplicate processing
- chat-side audio selection returns to the drawer rather than Android file manager
- audio/session persistence does not contaminate the smart app
