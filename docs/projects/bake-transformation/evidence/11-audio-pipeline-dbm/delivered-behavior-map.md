# Delivered Behavior Map: Audio Pipeline

Sprint: `11-audio-pipeline-dbm`
Date: 2026-04-29
Evidence class: static DBM from docs, Kotlin source, and JVM tests. No fresh
`adb logcat`, installed-device run, BLE trace, provider-network run, or live
Tingwu proof was captured in this sprint, so runtime evidence is marked as
Gap or Unknown unless current source/test evidence proves it.

## Evidence Commands

- `sed -n '1,260p' docs/core-flow/sim-audio-artifact-chat-flow.md`
- `sed -n '261,560p' docs/core-flow/sim-audio-artifact-chat-flow.md`
- `sed -n '1,260p' docs/cerb/audio-management/spec.md`
- `sed -n '1,260p' docs/cerb/tingwu-pipeline/spec.md`
- `sed -n '1,240p' docs/cerb/interface-map.md`
- `rg --files app-core/src/main/java app-core/src/test/java | rg 'Audio|Tingwu|BadgeAudio|SimAgent|SimSession|AudioDrawer|Chat'`
- `rg -n "manual sync|syncFromBadge|download|tombstone|MIN_BADGE|targeted|resume|rec#|log#|Ask AI|bindAudio|pending|local import|Tingwu|artifact|audio reselect|selectAudio" app-core/src/main/java app-core/src/test/java docs`
- `sed -n '1,980p' app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryStoreSupport.kt`
- `sed -n '1,280p' app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`
- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryTranscriptionSupport.kt`
- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryArtifactSupport.kt`
- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`
- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioPipelineIngestSupport.kt`
- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt`
- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/data/tingwu/RealTingwuPipeline.kt`
- `sed -n '1,720p' app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`
- `sed -n '1,760p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerContent.kt`
- `sed -n '1,360p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerCard.kt`
- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `sed -n '1,700p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`
- `sed -n '1,560p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`
- `sed -n '1,520p' app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`
- `sed -n '1,520p' app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupportTest.kt`
- `sed -n '1,260p' app-core/src/test/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloaderTest.kt`
- `sed -n '1,220p' app-core/src/test/java/com/smartsales/prism/data/audio/RealBadgeAudioPipelineIngressTest.kt`
- `sed -n '1,520p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModelTest.kt`
- `sed -n '1,820p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`
- `sed -n '320,430p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`

## Source Set

Primary docs: `docs/core-flow/sim-audio-artifact-chat-flow.md`,
`docs/cerb/audio-management/spec.md`,
`docs/cerb/tingwu-pipeline/spec.md`, and `docs/cerb/interface-map.md`.

Current code and tests:
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryStoreSupport.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryTranscriptionSupport.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryArtifactSupport.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioPipelineIngestSupport.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt`,
`app-core/src/main/java/com/smartsales/prism/data/tingwu/RealTingwuPipeline.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerContent.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerCard.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`,
`app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`,
`app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupportTest.kt`,
`app-core/src/test/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloaderTest.kt`,
`app-core/src/test/java/com/smartsales/prism/data/audio/RealBadgeAudioPipelineIngressTest.kt`,
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModelTest.kt`,
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`, and
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Historical references: older SIM audio/chat shards and archived review notes are
not active authority. The current core flow says deprecated SIM audio/chat
shards are migration memory only.

## Scope Boundary

Delivered behavior: this map covers audio-bound pipeline behavior only:
manual badge sync, `rec#` audio auto-download, `log#` badge pipeline ingest into
SIM inventory, SIM audio repository storage, Tingwu artifact processing, drawer
artifact display, `Ask AI`, audio reselect, pending audio selection, and
audio-grounded chat state.

Out of scope: general blank SIM chat without audio belongs to
`agent-chat-pipeline`. This map mentions blank chat only as a boundary from
`docs/core-flow/sim-audio-artifact-chat-flow.md`.

Target behavior: the core flow expects a single SIM-owned transcription/artifact
pipeline with badge sync as real product ingress, phone-local import as
test-only, drawer-origin and chat-origin selection sharing the same pipeline,
and durable artifact-backed chat history after pending completion.

## Badge Manual Sync

Delivered behavior: drawer sync is UI/manual through
`SimAudioDrawerViewModel.syncFromBadgeManually()`, which gates readiness,
emits local feedback, calls `SimAudioRepository.syncFromBadge(...)`, and sends
manual sync island events. The repository lists badge recordings through
`ConnectivityBridge.listRecordings()`, creates immediate SmartBadge
`QUEUED` placeholders, then processes one background download queue. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`,
and `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`.

Target behavior: `docs/core-flow/sim-audio-artifact-chat-flow.md` and
`docs/cerb/audio-management/spec.md` require manual drawer-side badge sync,
list-first placeholders, and no browse-open auto-sync as active product truth.

Gap: runtime evidence for physical badge `/list`, HTTP download timing,
foreground-service survival, and user-visible denial copy was not captured in
this sprint.

Unknown: whether current installed-device BLE/Wi-Fi state transitions always
match static gate branches without fresh filtered `adb logcat`.

## Placeholders, Empty Files, Delete, And Tombstone

Delivered behavior: manual sync and `rec#` create SmartBadge placeholders with
`localAvailability = QUEUED`; background queue marks them `DOWNLOADING`, then
imports successful WAVs as `READY`. Sub-1KB downloads are discarded: manual sync
uses `MIN_BADGE_WAV_SIZE_BYTES = 1024L`, and `rec#` uses
`MIN_REC_WAV_SIZE_BYTES = 1024L`. Deleting a badge-origin item cancels active
download work, removes local metadata/files/artifacts, writes a pending badge
delete tombstone, attempts remote delete, and suppresses reimport until remote
cleanup succeeds or the badge no longer reports the filename. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryStoreSupport.kt`,
and `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`.

Target behavior: the core flow requires placeholders to remain visible and
deletable while background download continues, to block transcribe/chat-pending
until local WAV exists, and to preserve tombstone behavior after delete.

Gap: the manual sync `SimBadgeSyncOutcome` type has `skippedEmptyCount`, but the
current static path does not show it being incremented when empty downloads are
removed. The spec says empty-file skip count should be shown only after empty
evidence is observed.

Unknown: remote badge delete behavior and tombstone clearing require live badge
runtime evidence.

## Targeted Resume And Device Changes

Delivered behavior: `SimAudioRepository.observeConnectivityStateChanges()`
cancels active queue and `rec#` jobs when the same ready badge moves to
`Disconnected` or `Connecting`, records filenames for targeted resume, and
calls `resumeDownloadsAfterReconnect()` when the same active badge returns to
`Ready`. This is byte-zero requeue, not HTTP Range resume. Device switch calls
`cancelAllBadgeDownloads()` and marks interrupted badge downloads as failed
with a switch-interrupted message. UI recovery states are derived by
`resolveSimBadgeDownloadRecoveryState()` and `isHoldingForResume()`. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`,
and `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`.

Target behavior: `docs/cerb/audio-management/spec.md` requires same-badge
disconnect cancellation, interrupted placeholder availability for HOLD
rendering, and targeted resume before debounce sync.

Gap: static tests cover recovery-state mapping, but no fresh runtime evidence
proves cancellation/resume under real BLE/Wi-Fi disconnects.

## `rec#` Auto-Download

Delivered behavior: `SimBadgeAudioAutoDownloader` listens to
`ConnectivityBridge.audioRecordingNotifications()`, handles `rec#` audio-ready
filenames, creates a `QUEUED` placeholder immediately, dedupes active jobs by
`(badgeMac, filename)`, downloads in background, filters sub-1KB files, imports
successful WAVs into SIM audio storage, and emits a transient
`RecFileDownloaded` island event. It does not transcribe or schedule. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`
and `app-core/src/test/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloaderTest.kt`.

Target behavior: `docs/cerb/audio-management/spec.md` defines `rec#` as
push-based auto-download with no transcription or scheduling.

Gap: live `rec#` BLE notification delivery and download completion are
unproven without fresh logcat/device evidence.

## `log#` Badge Pipeline Ingest Boundary

Delivered behavior: `RealBadgeAudioPipeline` listens to
`ConnectivityBridge.recordingNotifications()` for scheduler-path
`RecordingReady` events and explicitly ignores `AudioRecordingReady` because
`rec#` belongs to the SIM auto-downloader. The `log#` pipeline downloads a WAV,
runs short ASR, routes scheduler intent through `IntentOrchestrator`, then calls
`SimBadgeAudioPipelineIngestSupport.ingestCompletedRecording(...)` before
badge cleanup. Ingest writes a SIM SmartBadge transcribed item plus minimal
`TingwuJobArtifacts(transcriptMarkdown = transcript)`. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt`,
`app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioPipelineIngestSupport.kt`,
and `app-core/src/test/java/com/smartsales/prism/data/audio/RealBadgeAudioPipelineIngressTest.kt`.

Target behavior: `docs/cerb/audio-management/spec.md` says `log#` /
`BadgeAudioPipeline` remains the scheduler path and may feed completed items
back into the drawer inventory without redefining manual drawer sync.

Gap: these auto-ingested items are marked transcribed but use minimal ASR text
artifacts, not full Tingwu source-led artifacts. A later TCF should decide
whether this is accepted as delivered boundary behavior or needs convergence
with the long-form Tingwu artifact contract.

Unknown: scheduler pipeline hardware runtime and badge cleanup behavior were
not proven in this docs-only sprint.

## Tingwu Submit, Observe, Persist, Reuse

Delivered behavior: `SimAudioRepositoryTranscriptionSupport.startTranscription`
blocks non-READY audio, skips rerun when already transcribed and artifacts
exist, resumes an active job when status is `TRANSCRIBING`, uploads local audio
to OSS, submits a `TingwuRequest`, stores `activeJobId`, observes job progress,
writes completed artifacts to `sim_<audioId>_artifacts.json`, summarizes from
smart summary or transcript, and resets to pending with error state on failure.
Evidence:
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryTranscriptionSupport.kt`
and `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryArtifactSupport.kt`.

Delivered behavior: `RealTingwuPipeline` submits provider jobs with diarization,
chapters, summarization, meeting assistance, identity hints when available,
polls every 5 seconds, fetches provider result links, parses transcript,
diarized segments, speaker labels, summaries, Q&A, chapters, keywords, and
result links, then emits `TingwuJobState.Completed`. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/tingwu/RealTingwuPipeline.kt`
and `app-core/src/test/java/com/smartsales/prism/data/tingwu/RealTingwuPipelineTest.kt`.

Target behavior: `docs/cerb/tingwu-pipeline/spec.md` requires source-led
Tingwu artifacts to persist for later drawer/chat reuse and optional payload
failures to degrade rather than redefine the lane.

Gap: the implementation creates a fallback `TingwuSmartSummary(summary =
"智能摘要已生成。")` when summary payload is absent. A later target-flow sprint
should decide whether that copy is too strong for missing provider summary.

Unknown: live provider credentials, upload, polling, and result-link downloads
were not executed in this sprint.

## Drawer Browse And Artifact Surface

Delivered behavior: `RuntimeShellContent` opens the audio drawer in
`BROWSE` or `CHAT_RESELECT` mode. Browse mode shows the smart capsule with
connectivity and sync affordances, manual sync hooks, debug-only test import,
expanded transcribed-card artifact display, delete, swipe-to-transcribe, and
`Ask AI`. Expanded card state is owned by `SimAudioDrawerViewModel` and reset
when the drawer closes. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerContent.kt`,
and `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerCard.kt`.

Target behavior: transcribed cards should start collapsed, expand into a
source-led artifact surface, keep expansion stable while open, and avoid
fabricating absent provider sections.

Gap: static source shows artifact sections render from persisted artifacts, but
this sprint did not run Compose/UI or L3 verification for visual stability,
scroll retention, or user gestures.

## Transcribe Blocking And Local Import

Delivered behavior: `startTranscription()` throws when local audio is not
`READY`, and chat-side pending selection returns without binding if the item is
pending but not locally ready. `showTestImportAction` is only true when
`BuildConfig.DEBUG` and browse mode is active; the test import button calls
`SimAudioRepository.addLocalAudio()`, which marks the item as `PHONE`,
`READY`, and `isTestImport = true`. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryTranscriptionSupport.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerContent.kt`,
and `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryStoreSupport.kt`.

Target behavior: phone-local import is test-only and must not become the
production ingress path.

Gap: debug/release packaging behavior was not built in this sprint, so the
release absence of the local import UI is source-backed but not build-proven.

## `Ask AI`

Delivered behavior: in browse mode, expanded transcribed cards expose `Ask AI`.
`SimAudioDrawerViewModel.createDiscussion()` only allows transcribed items and
returns `SimAudioDiscussion`; `RuntimeShellContent.onAskAi` emits route
telemetry, calls `SimAgentViewModel.selectAudioForChat(...,
entersPendingFlow = false)`, binds the discussion in the audio repository, closes
the drawer, and removes pending tracking. `SimAgentViewModel` then enqueues
persisted artifacts as a durable `UiState.AudioArtifacts` message when artifacts
exist. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`,
and `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`.

Target behavior: `Ask AI` binds one selected audio and its artifacts into SIM
chat without requiring smart-agent memory.

Gap: answer quality over artifacts and live chat round-trip behavior were not
verified at runtime.

## Audio Reselect And Pending Selection

Delivered behavior: chat attach opens `RuntimeAudioDrawerMode.CHAT_RESELECT`.
Selecting a transcribed item binds/switches the audio-grounded session and
loads artifacts without rerunning Tingwu. Selecting a pending but READY item
binds the audio immediately, stores the `audioId -> sessionId` in
`trackedPendingAudioIds`, closes the drawer, and calls
`startTranscriptionForChat()`. Selecting a remote-only placeholder that is
pending but not `READY` returns without binding. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerCard.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`,
and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`.

Target behavior: chat-side audio reselect must reopen the audio drawer, bind
pending audio immediately, continue the same SIM Tingwu pipeline, and keep
remote placeholders blocked until local WAV exists.

Gap: the static path starts Tingwu from chat selection, but this sprint did not
trace the live completion observer that appends final artifacts for that
tracked pending item. The view model exposes `appendCompletedAudioArtifacts()`,
but runtime handoff from repository completion to chat history needs fresh
evidence or further source proof in the TCF/BAKE sprint.

## Audio-Bound Chat State And Durable History

Delivered behavior: audio-grounded chat sessions are SIM-local sessions with
`linkedAudioId`, `hasAudioContextHistory = true`, and
`SessionKind.AUDIO_GROUNDED`. Audio bindings are reconciled against repository
items on startup, duplicate links are normalized, and audio deletion clears the
linked session back to general. `SimSessionRepository` persists metadata in
`sim_session_metadata.json` and messages in `sim_session_<id>_messages.json`.
It serializes `UiState.AudioArtifacts` as `ai_audio_artifacts`, preserving
artifact JSON in durable chat history. Evidence:
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`
and `app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`.

Target behavior: completed chat-side audio must become durable artifact chat
history, and blank/general chat remains real but belongs to the separate
agent-chat-pipeline domain.

Gap: durable artifact serialization is source-backed and unit-tested in the
chat surface, but this sprint did not execute a full pending audio
transcription-to-history completion path.

## Telemetry And Canonical Valves

Delivered behavior: static telemetry for audio-bound pipeline is partial.
Manual badge sync emits `PipelineValve.Checkpoint.UI_STATE_EMITTED` summaries
for `SIM audio badge sync requested`, `SIM audio badge sync completed`, and
connectivity-unavailable failure. Shell audio route telemetry covers persisted
artifact open and audio-grounded chat open. Logs use tags such as
`AudioPipeline`, `SimAudioChat`, and `RealTingwuPipeline`. Evidence:
`app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`,
`app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`,
and `app-core/src/main/java/com/smartsales/prism/data/tingwu/RealTingwuPipeline.kt`.

Target behavior: `docs/core-flow/sim-audio-artifact-chat-flow.md` lists
canonical valves including `AUDIO_DRAWER_VISIBLE`, `AUDIO_LIST_LOADED`,
`BADGE_AUDIO_SYNCED`, `TRANSCRIPTION_REQUESTED`,
`TRANSCRIPTION_PROGRESS_UPDATED`, `TRANSCRIPTION_COMPLETED`,
`TRANSCRIPTION_FAILED`, `RAW_TINGWU_RESULT_READY`,
`ARTIFACT_POLISH_STARTED`, `ARTIFACT_POLISH_COMPLETED`,
`TRANSCRIBED_CARD_OPENED`, `ARTIFACTS_RENDERED`, `ASK_AI_REQUESTED`,
`AUDIO_CHAT_CONTEXT_BOUND`, `AUDIO_CHAT_VISIBLE`,
`AUDIO_RESELECT_REQUESTED`, and `AUDIO_RESELECT_RETURNED`.

Gap: delivered telemetry does not emit all canonical valve names. No canonical
provider boundary proof was captured for Tingwu submit/observe/result links.

Unknown: actual `VALVE_PROTOCOL`/logcat coverage under installed runtime,
provider network, and BLE badge scenarios because no fresh `adb logcat` was
captured.

## Tests

Delivered behavior: current test surface covers repository namespace and
recovery, sync support, `rec#` auto-downloader, badge pipeline ingest, drawer
view model copy/recovery mapping, shell handoff telemetry, transcript
presentation, Tingwu payload parsing, and selected chat binding/durable artifact
helpers. Evidence paths include
`app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositoryNamespaceTest.kt`,
`app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositoryRecoveryTest.kt`,
`app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupportTest.kt`,
`app-core/src/test/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloaderTest.kt`,
`app-core/src/test/java/com/smartsales/prism/data/audio/RealBadgeAudioPipelineIngressTest.kt`,
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModelTest.kt`,
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`,
`app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`,
and `app-core/src/test/java/com/smartsales/prism/data/tingwu/RealTingwuPipelineTest.kt`.

Target behavior: PU/L3 coverage should prove drawer-to-chat, storage isolation,
Tingwu provider, badge sync, and pending completion branches against the core
flow.

Gap: this sprint ran static verification only and did not run JVM tests, Compose
tests, installed-device tests, BLE tests, or provider-network tests.

## Known Gaps

- Gap: no runtime evidence for physical badge manual sync, `rec#`, `log#`,
  foreground download survival, provider Tingwu, or installed drawer/chat UI.
- Gap: canonical audio valves are ahead of delivered telemetry; delivered logs
  are partial and mostly use generic `UI_STATE_EMITTED` plus Android log tags.
- Gap: `log#` pipeline ingest writes minimal ASR transcript artifacts and marks
  items transcribed; it is not full Tingwu artifact processing.
- Gap: empty-file skip count exists in outcome shape but was not proven wired
  into manual sync result counts.
- Gap: pending chat-side completion has source helpers for durable artifact
  messages, but full repository-completion-to-chat-history proof was not
  captured.
- Unknown: live provider payload absence behavior, especially summary fallback
  copy, without real Tingwu responses.
