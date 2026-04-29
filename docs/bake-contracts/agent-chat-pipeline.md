---
protocol: BAKE
version: 1.0
domain: agent-chat-pipeline
layer: L3-L4 SIM chat/session corridor
runtime: partially-shipped
owner:
  - app-core SIM agent chat
  - app-core SIM session repository
  - app-core SIM voice draft coordinator
  - app-core SIM follow-up coordinator
core-flow-doc:
  - docs/core-flow/sim-audio-artifact-chat-flow.md
  - docs/core-flow/sim-shell-routing-flow.md
last-verified: 2026-04-29
---

# Agent Chat Pipeline BAKE Contract

This contract records the delivered implementation for the base agent-chat
slice: blank/general SIM chat, local session continuity, composer send, FunASR
voice draft, scheduler-shaped pre-route, and badge scheduler follow-up hosting.
Audio-specific behavior remains under `docs/bake-contracts/audio-pipeline.md`;
shell route arbitration remains under `docs/bake-contracts/shell-routing.md`.

## Pipeline Contract

### Inputs

- Blank/general SIM chat entry from the RuntimeShell chat surface.
- Composer text submitted by explicit send.
- User profile metadata for prompt context, including display name, role,
  industry, experience level, experience years, communication platform, and
  preferred language.
- SIM-local session history from the active session, excluding transient UI
  states.
- SIM session metadata and durable message files from the SIM-only repository.
- FunASR realtime voice draft events for partial, final, cancel, no-match,
  permission-denied, failure, and capture-limit outcomes.
- Scheduler-shaped general-chat text that may pre-route to scheduler-owned
  Path A logic before general chat fallback.
- Badge scheduler completion summaries that may create task-scoped follow-up
  sessions and prompt/action surfaces.

### Outputs / Guarantees

- Blank/general SIM chat is available before audio attachment and does not
  require an audio item, Mono memory, or the smart-agent system.
- A new general SIM chat session is created only after non-blank composer send
  when no current session is selected.
- General chat prompts include the SIM persona, user metadata, and bounded
  local session history. They must not claim hidden tools, database writes,
  task execution, smart-agent capability, or invisible memory.
- Composer send trims input, ignores empty text, appends the user turn, clears
  the input after append, sets sending/thinking state while work runs, and
  appends either a response or error state.
- SIM session persistence is SIM-only: session metadata is stored in
  `sim_session_metadata.json`, and per-session durable history is stored in
  `sim_session_<sessionId>_messages.json`.
- Durable message types are limited to user text, AI response, AI audio
  artifacts, and AI error. Thinking, streaming, voice draft, input text,
  transcript reveal, toast, and presentation states are not durable messages.
- Cold start restores stored sessions and grouped history without
  auto-selecting an active session and without normal demo-session seeding.
- FunASR voice draft writes recognized text into the editable composer draft
  only. It must require explicit send before a durable user turn is appended.
- Scheduler-shaped general-chat text may delegate to scheduler-owned Path A
  routing before LLM fallback. Scheduler storage, reminders, conflict truth,
  alarm scheduling, and mutations remain outside generic SIM chat.
- Badge scheduler follow-up is task-scoped. SIM may host the follow-up session,
  prompt, selected-task UI, and action strip, while scheduler-owned
  collaborators keep task mutation truth.

### Invariants

- MUST keep the base agent-chat slice shippable without Mono.
- MUST keep general SIM chat separate from audio-grounded chat and
  scheduler-follow-up sessions even when they reuse one chat shell.
- MUST keep SIM session persistence namespaced and isolated from the smart
  runtime.
- MUST keep durable history limited to the supported message types.
- MUST require explicit send for composer text and FunASR voice draft text.
- MUST keep FunASR draft cancellation, no-match, permission denial, recognizer
  failure, and capture-limit handling from mutating durable history by
  themselves.
- MUST keep scheduler-shaped pre-route narrow and scheduler-owned; fallback to
  general chat is allowed only when scheduler handling does not commit or emit
  handled feedback.
- MUST keep badge follow-up task mutation, alarm scheduling, conflict checks,
  and active-task resolution in scheduler-owned collaborators.
- MUST not read Kernel memory, CRM/entity loading, Path B enrichment,
  PluginRegistry/tool runtime, mascot state, or task-board state for the base
  agent-chat prompt.
- MUST require fresh runtime evidence before claiming installed UI,
  provider/network, live voice draft, microphone, FunASR auth/session,
  scheduler side effects, follow-up prompt/action strip, or logcat behavior.

### Error Paths

- Empty composer content: ignore without creating a session or appending a
  durable user turn.
- General executor failure: append a retryable or nonretryable error state
  rather than fabricating an assistant response.
- Title generation failure or unsuitable title: keep safe fallback title
  behavior without blocking the chat turn.
- Missing or blank user metadata: use safe defaults and continue with bounded
  persona context.
- No current session at send: create a general session before appending the
  user turn; if session creation cannot be proven, do not claim durable send.
- Session delete or missing persisted messages: clear or skip active state
  safely rather than resurrecting stale messages.
- FunASR permission denial, cancellation, no-match, timeout, recognizer
  failure, or late callback after cancel: leave durable chat history untouched
  and return to editable composer state.
- Scheduler-shaped input not handled by Path A: fall back to normal general
  chat with the same user message.
- Audio-grounded session receives scheduler wording: keep the session
  audio-grounded and do not mutate scheduler state through the audio chat path.
- Badge follow-up target unresolved or invalid: fail safely inside
  scheduler-owned follow-up copy and avoid mutating by UI selection accident.

## Telemetry Joints

- [INPUT_RECEIVED]: composer send, voice draft start/finish/cancel/failure,
  session create/switch/delete/restore, scheduler-shaped text, and badge
  follow-up creation/action request.
- [ROUTER_DECISION]: general chat versus audio-grounded chat versus
  scheduler-follow-up, scheduler pre-route versus general LLM fallback,
  voice-draft allowed versus blocked, and follow-up action routing.
- [STATE_TRANSITION]: session created, current session changed, input cleared,
  sending/thinking/error/response state emitted, voice draft listening/live
  transcript/finished/canceled, follow-up owner bound/cleared, and session
  persisted/deleted.
- [OUTPUT_EMITTED]: assistant response, user-visible error, composer draft
  text, grouped history projection, follow-up prompt/action strip, scheduler
  pre-route feedback, and toast/blocking copy.
- [ERROR_CAPTURED]: provider/network failure, title generation failure,
  voice-draft permission/no-match/cancel/failure, persistence failure, missing
  runtime evidence, and logcat gap.

Delivered telemetry is partial. Static evidence found scheduler shelf handoff,
badge follow-up, persisted audio artifact opening, and history route telemetry
through `PipelineValve` plus Android tags such as `SimSchedulerShelf`,
`SimBadgeFollowUpChat`, and `SimAudioChat`. General chat send, executor
request/response, title generation, voice draft lifecycle, session
create/delete/restore, provider/network failure, and a canonical
agent-chat-pipeline valve family remain telemetry gaps. No fresh `adb logcat`
was captured for this contract.

## UI Docking Surface

UI attaches through RuntimeShell's SIM chat surface and shared
AgentIntelligence UI seam. The shell provides composer text, trailing
mic/send behavior, voice draft state, history/session actions, audio reselect
entry, scheduler drawer suppression, and follow-up prompt/action visibility.
Presentation details stay outside this pipeline contract; durable chat/session
truth stays in SIM chat/session collaborators.

## Core-Flow Gap

- Gap: there is no dedicated agent-chat core-flow file. Sprint 15 aligned the
  existing SIM audio artifact chat and shell routing flows instead, so this
  contract records implementation truth against those companion docs.
- Gap: installed UI behavior is unproven for blank chat, composer send, grouped
  history restore/delete, scheduler pre-route, voice draft, and follow-up
  prompt/action strip.
- Gap: provider/network behavior is unproven for general executor calls,
  assistant responses, retry behavior, title generation, and user-visible
  failure copy.
- Gap: live voice draft behavior is unproven for microphone permission,
  NativeNui SDK initialization, FunASR auth, realtime partial/final callbacks,
  cancellation timing, and capture-limit timing.
- Gap: scheduler side effects are unproven from the chat composer without fresh
  runtime evidence for alarm scheduling, reminder state, conflict checks, and
  logcat delivery.
- Gap: badge-origin follow-up creation, foreground prompt timing, action strip
  visibility, and follow-up mutation side effects remain static-only.
- Gap: telemetry lacks a complete canonical valve family for the
  agent-chat-pipeline.

## MONO Extension Surface

The base agent-chat contract is deliberately complete without Mono. Future Mono
work may attach only at declared extension surfaces:

- Kernel memory may extend the bounded local session-history prompt context,
  but must not replace SIM-only durable session persistence.
- CRM/entity loading may enrich prompt context through a named context assembly
  seam, but general chat must not invent database reads or writes before that
  seam is implemented and evidenced.
- Path B enrichment may extend scheduler-shaped routing after scheduler-owned
  contracts declare the handoff; it must not widen generic SIM chat into
  scheduler mutation ownership.
- Plugin/tool runtime may attach through an explicit capability gateway, but
  base chat must not imply tool execution or hidden autonomous actions.
- Mascot and task-board behavior remain deferred intelligence surfaces. The
  base agent-chat slice may show disabled/toast behavior, but does not read or
  operate mascot/task-board state.

## Test Contract

- Evidence class: contract-test for this docs-only BAKE write; platform-runtime
  for future installed UI, provider/network, microphone, FunASR, scheduler
  side-effect, follow-up action, or logcat claims.
- Existing source/test coverage includes cold empty start, profile greeting,
  general persona-backed send, title fallback/retry behavior, voice draft
  success/partial/permission/capture-limit/no-match/cancel, SIM session
  metadata and durable message persistence, transient-state dropping, grouped
  history, session switch/delete, scheduler-shaped pre-route, audio-bound
  scheduler mutation exclusion, badge follow-up session creation/actions, and
  shell follow-up handoff helpers.
- Minimum verification for this contract: prove BAKE frontmatter and required
  sections exist; prove blank/general SIM chat, persona, user metadata,
  session history, session persistence, durable message types, composer send,
  FunASR voice draft, scheduler-shaped pre-route, follow-up, Mono exclusion,
  telemetry gaps, runtime evidence gaps, provider/network gaps, installed UI
  gaps, and logcat gaps are recorded; prove interface-map and scoped supporting
  docs cite this contract; and run `git diff --check` for scoped docs.
- Minimum future runtime closure: fresh filtered `adb logcat` plus command or
  test evidence for the exact claimed branch, including installed blank chat,
  composer send, provider success/failure, voice draft microphone/FunASR
  behavior, scheduler pre-route side effects, follow-up prompt/action strip,
  and emitted telemetry joints.

## Cross-Platform Notes

- This contract is shared product truth for the base agent-chat pipeline.
  Android Kotlin source is implementation evidence, not a HarmonyOS or iOS
  delivery prescription.
- Native platforms must preserve the same product behavior: blank/general chat
  before audio, explicit composer send, SIM-local session history, SIM-only
  persistence, durable message type limits, draft-only realtime speech, narrow
  scheduler pre-route, task-scoped follow-up hosting, and Mono exclusion.
- Platform-specific speech SDKs, microphone permission flows, local file
  storage, LLM provider clients, scheduler alarm APIs, logging, and UI
  frameworks are adapter concerns.
- Harmony-native or iOS parity must not be claimed from this Android/static
  contract alone. Each platform needs platform-native runtime evidence for
  microphone, provider/network, scheduler side effects, follow-up prompts, and
  logs.
