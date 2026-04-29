# Delivered Behavior Map: Agent Chat Pipeline

Sprint: `14-agent-chat-pipeline-dbm`
Date: 2026-04-29
Evidence class: static DBM from docs, Kotlin source, and JVM/source tests. No
fresh `adb logcat`, installed UI run, live voice draft capture, provider/network
LLM run, FunASR realtime session, or L3 validation was captured in this sprint,
so runtime evidence is marked as Gap or Unknown unless current source/test
evidence proves it.

## Evidence Commands

- `sed -n '1,260p' docs/specs/bake-protocol.md`
- `sed -n '1,280p' docs/cerb/interface-map.md`
- `sed -n '1,260p' docs/core-flow/sim-audio-artifact-chat-flow.md`
- `sed -n '1,280p' docs/cerb/sim-audio-chat/spec.md`
- `sed -n '1,260p' docs/cerb/sim-audio-chat/interface.md`
- `sed -n '1,760p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `sed -n '1,820p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`
- `sed -n '1,760p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`
- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentVoiceDraftCoordinator.kt`
- `sed -n '1,760p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt`
- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`
- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizer.kt`
- `sed -n '1,760p' app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `sed -n '1,620p' core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`
- `sed -n '150,1320p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`
- `sed -n '1,180p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelStructureTest.kt`
- `sed -n '1,360p' app-core/src/test/java/com/smartsales/prism/data/session/SimSessionRepositoryTest.kt`
- `sed -n '1,160p' app-core/src/test/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizerConfigTest.kt`
- `sed -n '1,720p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`
- `rg -n "blank chat|general SIM chat|persona|user metadata|session history|composer|voice draft|FunASR|session persistence|durable|scheduler|follow-up|audio boundary|Mono|telemetry|runtime evidence" docs app-core/src/main/java app-core/src/test/java core/pipeline/src/main/java`

## Source Set

Primary docs: `docs/specs/bake-protocol.md`,
`docs/cerb/interface-map.md`, and
`docs/core-flow/sim-audio-artifact-chat-flow.md`.

Historical reference docs:
`docs/cerb/sim-audio-chat/spec.md` and
`docs/cerb/sim-audio-chat/interface.md`. These are explicitly marked as
historical redirect/migration memory, not active authority.

Supporting shell docs: `docs/cerb/sim-shell/spec.md` and
`docs/cerb/sim-shell/interface.md`.

Current code and tests:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentVoiceDraftCoordinator.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt`,
`app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizer.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`,
`core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`,
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`,
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelStructureTest.kt`,
`app-core/src/test/java/com/smartsales/prism/data/session/SimSessionRepositoryTest.kt`,
`app-core/src/test/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizerConfigTest.kt`,
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

## Scope Boundary

Delivered behavior: this map covers agent-chat-pipeline base behavior for the
SIM chat shell outside the audio-specific pipeline: blank chat, general SIM
chat, composer/send, persona and user metadata prompt context, local session
history, session list/create/delete/restore persistence, durable message
types, FunASR voice draft, scheduler-shaped top-level routing, badge scheduler
follow-up hosting, telemetry, tests, and runtime evidence gaps.

Audio boundary: audio context is excluded except where needed to separate the
general chat lane from audio-grounded sessions. Sprint 11 already mapped the
audio-pipeline, and `docs/core-flow/sim-audio-artifact-chat-flow.md` remains
the behavioral north star for audio drawer, Tingwu artifact, `Ask AI`, audio
reselect, and audio-bound durable artifact history.

Target behavior: `docs/core-flow/sim-audio-artifact-chat-flow.md` says general
SIM chat exists before audio and uses system persona, user metadata, and local
SIM session history; it does not require the smart-agent memory system. The
historical SIM audio-chat docs preserve the same target language, but they are
Historical reference only.

## Blank And General SIM Chat

Delivered behavior: a cold start with an empty SIM store loads no seeded
sessions, leaves `currentSessionId` null, and shows empty history. A blank chat
becomes a durable general SIM chat session only when `send()` sees non-blank
composer text and no current session, then calls
`SimAgentSessionCoordinator.createGeneralSession()`. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`,
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Delivered behavior: general sends append the user turn, set a thinking state
for the supported discussion range, and either pre-route scheduler-shaped input
or call `SimAgentChatCoordinator.handleGeneralSend(...)`. General chat uses
`Executor.execute(ModelRegistry.COACH, prompt)` and appends either
`UiState.Response` or a retryable/nonretryable `UiState.Error`. The test
`general send uses persona backed reply instead of audio only guidance` proves
the lane is not audio-entry-only. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Target behavior: SIM chat should be a directly available conversation surface
even before audio is selected. It should not require Mono or the smart-agent
memory system.

Gap: no installed UI or provider/network evidence was captured to prove the
current composer, wait state, and final response on a device.

Unknown: live LLM latency, retry behavior, and exact user-visible fallback copy
under provider/network failure remain unproven.

## Persona, User Metadata, And Session History Context

Delivered behavior: `buildGeneralChatPrompt(...)` defines the SIM persona as a
lightweight, direct, trustworthy Chinese assistant running inside the SIM shell.
It tells the model not to claim hidden tool, database, task execution, or smart
agent capability. The prompt includes user metadata from `UserProfileRepository`
such as display name, role, industry, experience level, experience years,
communication platform, and preferred language. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`.

Delivered behavior: general and audio prompts include local session history via
`buildAudioConversationContext(record.messages)`, which keeps the last eight
user/assistant response/error turns and excludes transient states such as
thinking, streaming, and audio artifact payloads from the plain conversation
context. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`.

Delivered behavior: hero greeting uses the current profile display name and
falls back to `SmartSales 用户` for blank display names. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt` and
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Target behavior: historical SIM audio-chat docs and the active core flow both
expect blank/general chat to use system persona, user metadata, and local SIM
session history.

Gap: there is no static proof that prompts are safe for all user metadata edge
cases, and this sprint did not perform prompt-evaluation or provider/network
validation.

## Composer Send Behavior

Delivered behavior: `send()` cancels any voice draft, trims input, ignores empty
content, routes scheduler-follow-up sessions to the follow-up coordinator, and
otherwise creates or reuses the current session. It clears the composer input
after appending the user turn and sets `isSending` while work is running.
Evidence: `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`.

Delivered behavior: current session kind controls routing:
`SessionKind.GENERAL` pre-routes scheduler-shaped text before chat fallback,
`SessionKind.AUDIO_GROUNDED` stays audio-grounded, and
`SessionKind.SCHEDULER_FOLLOW_UP` routes to follow-up handling. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`.

Delivered behavior: `RuntimeShellContent` passes the SIM voice draft state and
callbacks into `AgentIntelligenceScreen`, enables the attach action to reopen
the audio drawer in `CHAT_RESELECT`, and hides the bottom composer while the
scheduler drawer is open. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`.

Gap: current static evidence proves ViewModel behavior but not touch handling,
IME behavior, visual button state, or installed UI layout.

Unknown: installed UI behavior for rapid send taps, IME transitions, process
death during send, and low-connectivity provider/network failures remains
unproven without runtime evidence.

## Session Persistence, Durable Types, And Restore

Delivered behavior: SIM session persistence is file-backed and namespaced:
metadata is stored in `sim_session_metadata.json`, and per-session messages are
stored in `sim_session_<sessionId>_messages.json`. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`
and `app-core/src/test/java/com/smartsales/prism/data/session/SimSessionRepositoryTest.kt`.

Delivered behavior: durable message types are limited to user text,
AI response, AI audio artifacts, and AI error. Transient UI states such as
thinking and streaming are dropped when writing durable history. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`
and `app-core/src/test/java/com/smartsales/prism/data/session/SimSessionRepositoryTest.kt`.

Delivered behavior: `SimAgentSessionCoordinator.loadPersistedSessions()` loads
metadata and messages without auto-selecting a session. It groups history by
pinned, today, recent 30 days, and month; supports create, switch, pin, rename,
delete, and new-session reset; and clears active state when deleting the active
session. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Delivered behavior: audio binding reconciliation exists but belongs to the
audio boundary: dangling audio bindings are cleared, missing bindings are
restored from persisted linked sessions, duplicate audio links keep the newest
session, and deleting a linked session clears audio binding. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Target behavior: historical SIM audio-chat interface requires SIM-namespaced
session persistence, no demo seed on normal cold start, no auto-select after
restore, and durable message types limited to user text, AI response, AI audio
artifacts, and AI error.

Gap: no filesystem corruption, disk-full, concurrent write, or process-death
runtime test was run in this sprint.

## Voice Draft And FunASR Realtime Boundary

Delivered behavior: the composer voice draft is a draft-only lane. Starting a
voice draft calls `SimRealtimeSpeechRecognizer.startListening()` with the
default `SIM_DRAFT` profile. Partial and final recognition events update
`SimVoiceDraftUiState.liveTranscript`; finishing writes recognized text into
the input field but does not append a user message or auto-send. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentVoiceDraftCoordinator.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizer.kt`,
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Delivered behavior: voice draft is blocked while already recording, processing,
sending, inside a scheduler follow-up context, or while the recognizer is
already listening. Permission denial writes an error/toast. Cancellation
invalidates late recognizer results and clears live transcript. Capture limit
events auto-finish the current session. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentVoiceDraftCoordinator.kt`
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Delivered behavior: `FunAsrRealtimeSpeechRecognizer` uses direct DashScope
credentials, the FunASR realtime WebSocket URL, 16 kHz mono PCM `AudioRecord`,
and NativeNui callbacks. `SIM_DRAFT` keeps default sentence silence while the
onboarding profile sets six-second silence. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizer.kt`
and `app-core/src/test/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizerConfigTest.kt`.

Target behavior: SIM-owned FunASR realtime recognition should draft editable
composer text locally, require explicit send, and leave durable history
untouched on cancel, no-match, permission denial, or recognizer failure.

Gap: live voice draft behavior is unproven. This sprint did not run microphone
permission flows, NativeNui SDK auth, live FunASR network sessions, logcat
capture, or installed UI verification.

Unknown: device-specific mic contention, SDK callback timing, capture-limit
timing, and auth-failure UI copy need runtime evidence.

## Scheduler-Shaped Routing Boundary

Delivered behavior: general SIM chat has a narrow scheduler-shaped input
pre-route. `looksLikeTopLevelSchedulerInput(...)` checks Chinese/English
scheduler keywords or time/date cues. If matched, `handleTopLevelSchedulerSend`
calls `IntentOrchestrator.processInput(content, isVoice = true)` and treats
`PipelineResult.PathACommitted` or scheduler pre-route feedback replies as
handled. If not handled, the same user message falls back to normal general
chat. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt` and
`core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`.

Delivered behavior: tests prove natural Chinese event-time text and reschedule
wording route through `IntentOrchestrator` before general-chat fallback and do
not call the LLM executor when Path A commits. Tests also prove audio-grounded
reschedule wording does not mutate scheduler state because audio-grounded chat
stays in its own session kind. Evidence:
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Target behavior: the interface map says SIM chat may host chat/session surfaces
but must not become generic scheduler storage or a second memory lane.

Gap: static tests cover mocked routing branches, not installed UI confirmation,
alarm scheduling side effects, reminder surfacing, or logcat telemetry for
actual scheduler commits from the chat composer.

Unknown: exact product boundary for ambiguous scheduler-shaped general chat
fallback still needs a later TCF/BAKE decision if the target core-flow for
agent-chat-pipeline wants stricter routing.

## Badge Scheduler Follow-Up Hosting Boundary

Delivered behavior: badge scheduler follow-up is a separate
`SessionKind.SCHEDULER_FOLLOW_UP`. `SimAgentFollowUpCoordinator` creates
task-scoped sessions from badge scheduler completion summaries without
auto-selecting them, stores `SchedulerFollowUpContext` in session metadata,
and supports explain, status, prefill reschedule, mark done, delete, and
reschedule actions. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt`,
`app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`,
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Delivered behavior: `RuntimeShellContent` can host a follow-up prompt and
action strip only when shell surfaces are clear enough. History switch, new
session, and delete actions clear follow-up owner state only when they leave or
delete the bound session. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt` and
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Delivered behavior: follow-up routing uses scheduler-owned collaborators:
`ScheduledTaskRepository`, `ScheduleBoard`, `ActiveTaskRetrievalIndex`,
`AlarmScheduler`, `SchedulerIntelligenceRouter`, and extraction services.
SIM hosts the conversation surface and selected task UI; scheduler truth stays
outside generic SIM chat. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt`.

Target behavior: `docs/cerb/interface-map.md` says SIM chat may host the
follow-up conversation surface but must not own generic scheduler storage or a
second memory lane.

Gap: runtime evidence is missing for badge-origin follow-up creation,
foreground prompt timing, action strip visibility, alarm cancellation/schedule
side effects, and logcat proof.

## Smart-Agent And Mono Memory Exclusion

Delivered behavior: general SIM chat builds context from SIM-local messages and
user profile metadata, not from ContextBuilder Kernel memory, CRM/entity memory,
PluginRegistry, or smart-agent task-board memory. The prompt explicitly says
SIM is not the smart agent system and should not pretend to use hidden tools,
database writes, task execution, or hidden systems. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`.

Delivered behavior: `SimAgentViewModel.selectTaskBoardItem(...)` only surfaces
a toast that SIM Wave 1 does not enable the task-board entry, and mascot
interaction is disabled. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`.

Target behavior: `docs/specs/bake-protocol.md` separates BAKE base-runtime
contracts from Mono intelligence augmentation. `docs/cerb/interface-map.md`
says bounded local/session continuity belongs to base runtime while Kernel
session memory, CRM/entity loading, Path B enrichment, and plugin/tool runtime
remain Mono-only.

Gap: this sprint did not perform a full code audit for every possible indirect
Mono dependency through injected collaborators; the mapped delivered behavior
is scoped to current SIM chat send/session/voice/follow-up paths.

## Telemetry And Logs

Delivered behavior: telemetry exists around scheduler shelf handoff,
badge scheduler follow-up creation/action/blocking/shadow metrics, persisted
audio artifact opening, and history route events through `PipelineValve` plus
Android `Log.d` tags such as `SimSchedulerShelf`, `SimBadgeFollowUpChat`, and
`SimAudioChat`. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt`,
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Delivered behavior: `IntentOrchestrator` emits broader pipeline valves such as
`INPUT_RECEIVED`, `ROUTER_DECISION`, Path A parse/extract/DB write checkpoints,
and mutation commit/proposal checkpoints, but those belong to the scheduler
pipeline once SIM delegates scheduler-shaped input. Evidence:
`core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`.

Gap: there is no complete canonical telemetry set for agent-chat-pipeline
itself. General chat send, executor request/response, title generation, voice
draft start/finish/failure, session create/delete/restore, and provider/network
failure are not all proven by a unified valve family.

Unknown: logcat behavior is unproven. No fresh `adb logcat` was captured for
general chat, scheduler pre-route, follow-up, or FunASR voice draft.

## Current Test Surface

Delivered behavior: `SimAgentViewModelTest` covers cold empty start, profile
greeting, voice draft success/partial/permission/capture-limit/no-match/cancel,
pending audio binding, seeded sessions, general persona-backed send, title
generation and retry, scheduler-shaped pre-route, audio-bound scheduler
mutation exclusion, audio artifact history fallback, follow-up session
creation, session switch/delete, scheduler shelf telemetry, and durable
artifact restore.

Delivered behavior: `SimSessionRepositoryTest` covers durable session metadata,
supported message types, dropping transient UI states, delete, SIM namespacing,
and legacy audio flag backfill.

Delivered behavior: `SimRealtimeSpeechRecognizerConfigTest` covers FunASR
profile parameter differences and onboarding/SIM profile mapping.

Delivered behavior: `SimAgentViewModelStructureTest` source-checks that the
host ViewModel delegates to session, chat, follow-up, and voice-draft
coordinators. `SimShellHandoffTest` covers shell follow-up/handoff helpers and
telemetry helper behavior.

Gap: tests are mostly JVM/source tests. No Compose screenshot, instrumentation,
provider/network, microphone, NativeNui, or L3 runtime evidence was run in
this sprint.

## Delivered Gaps And Unknowns For Next Sprint

- Gap: agent-chat-pipeline needs a target core-flow/TCF layer that separates
  general SIM chat, audio-grounded chat, scheduler-shaped pre-route, and badge
  scheduler follow-up as named branches.
- Gap: telemetry lacks a complete chat-pipeline valve family for input
  received, session created/restored, executor request, executor success/error,
  title generated/rejected, voice draft lifecycle, and session persisted.
- Gap: installed UI behavior is unproven for blank chat, composer send, history
  drawer session restore/delete, voice draft, scheduler pre-route, and
  follow-up prompt/action strip.
- Gap: provider/network behavior is unproven for general chat executor calls
  and auto-title generation.
- Gap: live voice draft behavior is unproven for microphone permission,
  NativeNui SDK init, FunASR auth, partial/final callbacks, cancellation, and
  capture-limit timing.
- Gap: logcat evidence is missing for all runtime branches mapped here.
- Unknown: whether a later BAKE contract should accept current scheduler-shaped
  general-chat routing as part of agent-chat-pipeline or move it fully under a
  scheduler extension surface.
- Unknown: whether follow-up sessions should remain in the same SIM session
  store long term or gain a stricter separate persistence namespace.
- Unknown: whether general chat should ever read bounded session continuity
  beyond local SIM history before Mono is reintroduced.
