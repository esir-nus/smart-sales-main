---
protocol: BAKE
version: 1.0
domain: audio-pipeline
layer: L1-L4 audio corridor
runtime: base-runtime-active
owner:
  - app-core SIM audio repository
  - app-core badge audio pipeline
  - app-core Tingwu pipeline
  - app-core drawer/chat surfaces
core-flow-doc:
  - docs/core-flow/sim-audio-artifact-chat-flow.md
last-verified: 2026-04-29
---

# Audio Pipeline BAKE Contract

This contract records the delivered implementation for the base-runtime audio
pipeline. The SIM Audio Artifact Chat core-flow remains the behavioral north
star above this document. Scoped Cerb audio docs remain supporting reference
beneath this verified BAKE implementation record.

## Pipeline Contract

### Inputs

- Manual sync from the audio drawer against the active badge recording list.
- Badge `rec#` audio-ready notifications routed to SIM audio auto-download.
- Badge `log#` scheduler-pipeline recordings that may be ingested into SIM
  audio storage after scheduler processing succeeds.
- Debug/test phone-local import in browse mode when enabled by build state.
- Drawer-origin transcription requests for locally ready audio.
- Chat-origin audio reselect from the drawer, including already-transcribed
  audio and pending audio that has a local WAV ready.
- `Ask AI` from an expanded transcribed drawer card.
- Tingwu provider submit, observe, result-link fetch, and artifact persistence
  through the shared long-form audio lane.

### Outputs / Guarantees

- Manual sync is drawer-owned and list-first: new badge files create immediate
  SmartBadge placeholders, while WAV downloads continue in a repository-owned
  background queue.
- Remote-only placeholders remain visible and deletable, but cannot enter
  transcribe, `Ask AI`, or chat-side pending flows until local availability is
  `READY`.
- `rec#` creates or resumes SIM SmartBadge placeholders, downloads WAV files,
  filters empty-file candidates below 1 KB, and does not transcribe or schedule.
- `log#` remains the scheduler-owned BadgeAudioPipeline path. On successful
  completion it may ingest a transcribed SIM drawer item with minimal ASR text
  artifacts before badge cleanup.
- Drawer-origin transcription blocks non-ready audio, uploads local audio to
  OSS, submits Tingwu, observes provider progress, persists source-led
  artifacts, and reuses existing transcribed artifacts instead of rerunning.
- `Ask AI` binds one selected transcribed audio item and its persisted artifacts
  into SIM chat without requiring the smart-agent memory system.
- Audio reselect reopens the audio drawer from chat. Already-transcribed items
  bind stored artifacts without rerun; ready pending items bind immediately and
  continue the same SIM Tingwu path from chat-side transparent state.
- SIM audio/chat history is namespaced. Audio-grounded sessions persist
  `linkedAudioId`, audio context metadata, and `UiState.AudioArtifacts` messages
  through durable session storage.
- Summary fallback must be honest degradation. If provider summary payloads are
  absent, fallback copy must not imply that Tingwu or smart summary content was
  returned.

### Invariants

- MUST keep badge sync as the real product ingress and phone-local import as a
  test-only convenience path.
- MUST keep browse-open auto-sync retired; user-visible drawer sync remains
  manual even though `rec#`, reconnect fallback, and `log#` ingest can add
  inventory through separate paths.
- MUST keep the long-form artifact lane source-led by Tingwu/provider data.
  Drawer or chat polish may improve readability but must not invent facts,
  sections, speaker roles, or smart-agent provenance.
- MUST keep `rec#` and `log#` separate: `rec#` belongs to SIM audio
  auto-download, while `log#` belongs to the scheduler BadgeAudioPipeline.
- MUST block transcription and chat-pending selection for placeholders without a
  local WAV.
- MUST reuse stored artifacts for already-transcribed selections.
- MUST bind ready pending chat-side audio immediately to the selected discussion
  before Tingwu finishes.
- MUST preserve completed pending durable history once the final artifacts are
  appended to chat; current static evidence leaves full completion handoff as a
  gap, not a proven runtime claim.
- MUST keep audio storage isolated from the smart app and Mono memory systems.
- MUST require fresh runtime evidence before claiming physical badge, BLE
  reconnect/resume, provider-network Tingwu, installed drawer/chat UI, or live
  telemetry behavior.

### Error Paths

- Badge not ready for manual sync: deny locally and do not invent auto-sync.
- Empty badge list: report no recordings only after the badge list is actually
  empty.
- Duplicate, tombstoned, or already-present recording: keep local inventory
  stable and avoid duplicate placeholder/import work.
- Empty-file download below 1 KB: delete local temp data and skip import;
  manual sync skip counts remain a gap until increment wiring is proven.
- Same-badge disconnect during manual or `rec#` download: cancel active HTTP
  work, keep interrupted placeholder recovery state, and target-resume from
  byte zero after reconnect.
- Device switch during manual or `rec#` download: cancel active outgoing badge
  downloads and mark interrupted placeholders as failed for the old active
  badge.
- Transcription requested for non-ready audio: fail safely without a Tingwu job.
- Tingwu submit, observe, upload, result-link fetch, or provider payload failure:
  preserve explicit failure/degraded state and do not fabricate artifacts.
- Missing provider summary: fall back to transcript/diarization-derived copy
  honestly rather than claiming a provider or smart summary exists.
- Already-transcribed audio with stored artifacts: skip rerun and reopen stored
  artifacts.
- Chat-side placeholder without local WAV: stay in the drawer and do not bind a
  fake pending chat run.
- Audio deletion: remove local metadata/files/artifacts, clear or reconcile
  linked sessions, persist badge tombstone where needed, and avoid reimport
  until remote cleanup succeeds or the badge stops reporting the file.

## Telemetry Joints

- [INPUT_RECEIVED]: manual sync, `rec#` notification, `log#` scheduler
  recording, drawer transcribe, `Ask AI`, audio reselect, delete, and Tingwu
  submit request.
- [ROUTER_DECISION]: manual sync eligibility, reconnect/targeted resume,
  `rec#` versus `log#` ownership, ready versus remote-only placeholder, reuse
  stored artifacts versus start Tingwu, browse mode versus chat reselect, and
  scheduler-pipeline ingest boundary.
- [STATE_TRANSITION]: placeholder queued/downloading/ready/failed, transcribing
  progress, transcribed artifact persisted, pending chat binding, discussion
  session linked/unlinked, tombstone written/cleared, and drawer expansion.
- [OUTPUT_EMITTED]: drawer inventory loaded, badge sync outcome, empty-file
  skip language when proven, artifact sections rendered, `Ask AI` chat context
  opened, audio-grounded chat visible, and durable artifact message persisted.
- [ERROR_CAPTURED]: connectivity unavailable, empty-file skip, duplicate or
  tombstoned recording, provider submit/poll/result failure, missing optional
  Tingwu payload, placeholder blocked from chat, and runtime evidence gap.

Target canonical valves from the core-flow include `AUDIO_DRAWER_VISIBLE`,
`AUDIO_LIST_LOADED`, `BADGE_AUDIO_SYNCED`, `TRANSCRIPTION_REQUESTED`,
`TRANSCRIPTION_PROGRESS_UPDATED`, `TRANSCRIPTION_COMPLETED`,
`TRANSCRIPTION_FAILED`, `RAW_TINGWU_RESULT_READY`,
`ARTIFACT_POLISH_STARTED`, `ARTIFACT_POLISH_COMPLETED`,
`TRANSCRIBED_CARD_OPENED`, `ARTIFACTS_RENDERED`, `ASK_AI_REQUESTED`,
`AUDIO_CHAT_CONTEXT_BOUND`, `AUDIO_CHAT_VISIBLE`,
`AUDIO_RESELECT_REQUESTED`, and `AUDIO_RESELECT_RETURNED`.

Delivered telemetry is partial. Static evidence includes manual sync
`UI_STATE_EMITTED` summaries, shell route telemetry for artifact/chat opening,
and Android log tags such as `AudioPipeline`, `SimAudioChat`, and
`RealTingwuPipeline`. It does not prove all canonical valves, provider boundary
logs, or installed logcat delivery.

## UI Docking Surface

UI attaches through the RuntimeShell audio drawer and SIM chat surfaces. The
drawer owns browse, manual sync, delete, transcribe, artifact expansion, and
`Ask AI` entry. Chat owns audio reselect, pending transparent state, and
audio-grounded discussion history. UI may present polished source-led artifacts,
but pipeline truth remains in SIM audio repository, BadgeAudioPipeline, Tingwu,
and session persistence.

## Core-Flow Gap

- Gap: `log#` scheduler-pipeline drawer ingest writes minimal ASR transcript
  artifacts into SIM audio storage. This is accepted as delivered boundary
  behavior for the scheduler path, not proof of full long-form Tingwu artifact
  processing.
- Gap: manual sync empty-file outcome shape exists, but static Sprint 11
  evidence did not prove `skippedEmptyCount` increments after sub-1KB downloads
  are removed.
- Gap: pending chat-side selection binds ready audio immediately and durable
  artifact serialization exists, but the full repository-completion-to-chat
  history handoff for pending durable history was not proven end to end.
- Gap: summary fallback copy currently can imply a generated smart/provider
  summary when provider summary is absent; target behavior requires honest
  transcript-derived degradation language.
- Gap: canonical valves remain ahead of delivered telemetry. Delivered logs do
  not yet prove every core-flow checkpoint or provider boundary.
- Gap: runtime evidence has proven physical badge manual sync/list/download
  ingress and installed drawer terminal import in Sprint 04-a, but is still
  missing for live `rec#` notification auto-download, `log#`, BLE
  disconnect/resume, foreground download survival, provider-network Tingwu
  behavior, and installed drawer/chat UI beyond the badge-sync surface.

## Test Contract

- Evidence class: contract-test for this docs-only BAKE write; platform-runtime
  for future physical badge, BLE, provider-network, installed UI, or telemetry
  claims.
- Existing source/test coverage includes SIM audio namespace and recovery,
  manual sync support, `rec#` auto-downloader, `log#` pipeline ingest,
  drawer ViewModel copy/recovery mapping, shell handoff telemetry, transcript
  presentation, Tingwu payload parsing, selected chat binding, and durable
  artifact helpers.
- Minimum verification for this contract: prove BAKE frontmatter and required
  sections exist, prove manual sync, `rec#`, `log#`, Tingwu, drawer, `Ask AI`,
  audio reselect, pending, durable, empty-file, summary fallback, canonical
  valves, runtime evidence, and provider gaps are recorded, prove interface-map
  and scoped Cerb docs cite this contract as authority/reference, and run
  `git diff --check` for scoped docs.
- Minimum future runtime closure: fresh filtered `adb logcat` plus command
  evidence for the exact claimed branch, including physical badge `/list`,
  `rec#`, `log#`, BLE disconnect/resume, provider submit/poll/result-link
  behavior, installed drawer/chat rendering, and emitted telemetry joints.

## Cross-Platform Notes

- This contract is shared product truth for the audio pipeline. Android code is
  implementation evidence, not a HarmonyOS or iOS delivery prescription.
- Native platforms must preserve the same behavior: manual drawer sync, visible
  placeholders, blocked remote-only transcription/chat, separate `rec#` and
  `log#` ownership, source-led Tingwu artifacts, `Ask AI` binding, drawer-based
  audio reselect, pending immediate binding, and durable artifact history.
- Badge hardware transfer, foreground service, notification, OSS client,
  provider SDK/HTTP, local file storage, logging, and UI framework mechanics are
  platform-owned adapters.
- The transient Harmony Tingwu container may consume the source-led Tingwu and
  artifact persistence contract, but it does not inherit Android badge hardware,
  scheduler, reminder, or SIM chat capability unless a Harmony sprint proves
  those native surfaces separately.
