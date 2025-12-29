# API Contracts (Aligned with Orchestrator-V1)

> Notes (Precedence):
> - This document only describes the **UI <-> Orchestrator-V1 Facade** boundary and event/object contracts.
> - The only current spec is `docs/Orchestrator-V1.md`.
> - V7 is archived (ARCHIVED): historical reference only, not an implementation basis.
> - Stable data objects (Published* / Plan / Artifacts / Metadata layers) follow `docs/orchestrator-v1.schema.json`.

---

## 1) Facade: UI-callable interfaces (recommended set)

### 1.1 Chat

- `streamChat(ChatRequest) -> Flow<ChatEvent>`
  - Primary event stream for UI chat rendering.
  - **Hard rule**: UI must only show Publisher output (see 2).

- `getPublishedChatTurn(chatSessionId, turnId) -> PublishedChatTurn`
  - For history recovery or reconnect backfill.

- `getThinkingTrace(chatSessionId, turnId?) -> ThinkingTraceSnapshot`
  - For optional "Thinking Trace" panel (no chain-of-thought).

- `getDebugSnapshot(chatSessionId, turnId?) -> DebugSnapshot`
  - For HUD (three copy/paste blocks).

### 1.2 Recording / Transcription

- `startTranscription(RecordingRequest) -> RecordingHandle`
  - Returns: `audioAssetId` + `recordingSessionId` (optional `disectorPlanId`).
  - Cache hit: if `audioAssetId` exists, Tingwu artifacts may be reused; still allow new `recordingSessionId` for this processing instance.

- `observeRecordingSession(recordingSessionId) -> Flow<RecordingEvent>`
  - UI shows "processing / pseudo-stream updates / completed".

- `getPublishedTranscript(recordingSessionId) -> PublishedTranscript`
  - UI shows only Publisher continuous prefix (b1..bk).

- `getPublishedAnalysis(recordingSessionId) -> PublishedAnalysis`
  - UI view for "chapters/summary/speaker map" (chapter-level timeline).

- `getDebugSnapshot(recordingSessionId, batchIndex?) -> DebugSnapshot`
  - HUD (three copy/paste blocks).

### 1.3 Metadata Hub (UI read-only)

- `getSessionOverview(chatSessionId) -> SessionOverview`
  - For UI badges/titles/tags (e.g., M3-derived titles, M2 tags).
  - UI does not write M1/M2/M2B/M3/M4 (writes are by LLM Parser/system modules).

---

## 2) Chat Event Stream (ChatEvent)

### 2.1 Event types

- `ChatEvent.DisplayDelta(deltaText)` (optional)
  - For "pseudo-stream" display.

- `ChatEvent.Completed(publishedTurn: PublishedChatTurn)`
  - Final result for this turn.

- `ChatEvent.Retrying(reason, attempt, maxRetries)`
  - Only when MachineArtifact validation fails (Strategy B).

- `ChatEvent.Error(userFacingMessage, retryable)`

### 2.2 Hard constraints (must satisfy)

- **UI shows only** `PublishedChatTurn.displayMarkdown`.
- `DisplayDelta` (if enabled) must be projected from `<visible2user>` content; do not stream text outside `<visible2user>` to UI.
- `PublishedChatTurn.machineArtifact` is for system/debug only, not the UI render source.
- Publisher MUST follow the extraction algorithm in Section 5.2 (HumanDraft via first `<visible2user>`, MachineArtifact via first ```json fenced block outside `<visible2user>`; no heuristics).
- If UI renders L3 structured cards, use Publisher output `smartAnalysisCard` (if present), not direct JSON parsing.

---

## 3) Recording Event Stream (RecordingEvent)

### 3.1 Event types (recommended)

- `RecordingEvent.PlanReady(plan: DisectorPlan)`
- `RecordingEvent.BatchSubmitted(batchIndex, attempt)`
- `RecordingEvent.BatchSucceeded(batchIndex)`
- `RecordingEvent.BatchFailed(batchIndex, reason, retryable)`
- `RecordingEvent.PublishedPrefixAdvanced(publishedPrefixBatchIndex)`
- `RecordingEvent.Completed(recordingSessionId)`
- `RecordingEvent.Error(userFacingMessage, retryable)`

### 3.2 Hard constraints

- UI must not assemble out-of-order batches; consume `PublishedTranscript` only.
- Chapters/summary only from `PublishedAnalysis` (chapter-level timeline; no per-line timestamp polishing in V1).

---

## 4) Debug / HUD Snapshot (mandatory three copy/paste blocks)

### 4.1 DebugSnapshot

`getDebugSnapshot(...)` must return three copyable text blocks:

1) **Effective Run Snapshot**
   - Key config and state (cache hit, retry count, plan/version, batch progress)

2) **Raw Output**
   - Chat: raw LLM output (includes `<visible2user>` and MachineArtifact text)
   - Transcription: Tingwu raw output or reference

3) **Publisher-ready / Published Snapshot**
   - Chat: extracted `displayMarkdown` + artifact validation summary
   - Transcription: DisectorPlan summary + published prefix state + chapter timeline summary

> Constraint: normal bubbles/views **never show raw JSON**; JSON is allowed only in HUD.

---

## 5) Error Semantics (minimum constraints)

### 5.1 Tingwu Runner

Retry rules (see `docs/Orchestrator-V1.md` Section 8.1):
- 429: retryable with longer backoff.
- Other 4xx (except 429): not retryable by default.
- 5xx/network timeouts: retryable (deterministic backoff).

### 5.2 Chat (MachineArtifact validation failure)

- `maxRetries = 2` (default recommendation).
- Publisher extraction algorithm (see `docs/Orchestrator-V1.md` Section 5.2):
  1) Extract HumanDraft: first complete `<visible2user>...</visible2user>` innerText
  2) Extract MachineArtifact: first ```json fenced block outside `<visible2user>`
  3) If fenced block missing or JSON parse/validation fails: set `artifactStatus = INVALID`, trigger retry
  4) **Explicitly forbidden**: heuristic "guess JSON / regex extract JSON" from non-fenced text
- If retries exhausted (see `docs/Orchestrator-V1.md` Section 8):
  - HumanDraft extractable when: correct `<visible2user>` pairing exists, innerText trim length > 0
  - If extractable: publish that `displayMarkdown`
  - Else: publish a fixed fallback message (example: `Sorry, I could not generate a displayable response. Please retry.`)
  - `artifactStatus = FAILED`
  - Do not write Metadata Hub
  - Must record Trace/HUD events (include failure reason + retry count)

---

## 6) Compatibility and Migration

- New features must use `docs/Orchestrator-V1.md` as the sole basis.
- If legacy UI still depends on V7, use a compatibility layer to convert events/objects to V1 objects (do not back-propagate V7 into V1 contracts).
