# API Contracts (V1-Verified)

> **Source of Truth**: [Orchestrator-V1.md](file:///home/cslh-frank/main_app/docs/specs/Orchestrator-V1.md)  
> **Data Schemas**: [orchestrator-v1.schema.json](file:///home/cslh-frank/main_app/docs/specs/orchestrator-v1.schema.json)  
> **Last Verified**: 2026-01-08

This document describes the **actual implemented** domain coordinator interfaces. For V1 artifact schemas (DisectorPlan, TingwuBatchArtifact, MachineArtifact, etc.), see the JSON schema.

---

## 1) Domain Coordinators

### 1.1 ChatCoordinator

**File**: [ChatCoordinator.kt](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/domain/chat/ChatCoordinator.kt)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `chatEvents` | `Flow<ChatEvent>` | Chat events stream for UI consumption |
| `sendMessage()` | `fun sendMessage(params: SendMessageParams)` | Send regular chat message |
| `sendSmartAnalysis()` | `fun sendSmartAnalysis(params: SmartAnalysisParams)` | Send SmartAnalysis request |
| `resetStream()` | `fun resetStream()` | Reset streaming state |

**ChatEvent sealed class:**

| Variant | Payload | When Emitted |
|---------|---------|--------------|
| `StreamStarted` | `assistantId: String` | Streaming begins, placeholder created |
| `StreamDelta` | `assistantId, token: String` | Token received from LLM |
| `StreamCompleted` | `result: ChatCompletionResult` | Final result ready |
| `StreamError` | `assistantId, error: ChatError` | Error occurred |

---

### 1.2 TranscriptionCoordinator

**File**: [TranscriptionCoordinator.kt](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/domain/transcription/TranscriptionCoordinator.kt)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `state` | `StateFlow<TranscriptionUiState>` | Transcription state for UI |
| `observeJob()` | `fun observeJob(jobId): Flow<AudioTranscriptionJobState>` | Observe job state |
| `observeProcessedBatches()` | `fun observeProcessedBatches(jobId): Flow<ProcessedBatch>` | Observe processed batches (gate + filter) |
| `runTranscription()` | `suspend fun runTranscription(...)` | Run transcription with callbacks |
| `reset()` | `fun reset()` | Reset state for new transcription |
| `markFinal()` | `fun markFinal()` | Mark transcription as final |

---

### 1.3 DebugCoordinator

**File**: [DebugCoordinator.kt](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/domain/debug/DebugCoordinator.kt)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `debugState` | `StateFlow<DebugUiState>` | Debug state for HUD panel |
| `toggleDebugPanel()` | `fun toggleDebugPanel()` | Toggle visibility |
| `refreshDebugSnapshot()` | `suspend fun refreshDebugSnapshot(...)` | Refresh HUD snapshot |
| `refreshSessionMetadata()` | `suspend fun refreshSessionMetadata(...)` | Refresh session metadata |
| `refreshTraces()` | `fun refreshTraces()` | Refresh Xfyun/Tingwu traces |

**DebugUiState:**
- `visible: Boolean` — Panel visibility
- `snapshot: DebugSnapshot?` — HUD three-block snapshot
- `xfyunTrace: XfyunTraceSnapshot?`, `tingwuTrace: TingwuTraceSnapshot?`

---

### 1.4 ExportCoordinator

**File**: [ExportCoordinator.kt](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/domain/export/ExportCoordinator.kt)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `exportState` | `StateFlow<ExportUiState>` | Export state for UI |
| `checkExportGate()` | `suspend fun checkExportGate(sessionId): ExportGateState` | Check if export allowed |
| `performExport()` | `suspend fun performExport(...): Result<Unit>` | Execute PDF/CSV export |
| `clearSnackbar()` | `fun clearSnackbar()` | Clear snackbar message |

**ExportGateState:**
- `ready: Boolean` — Export allowed
- `reason: String` — Gate reason (e.g., "SmartAnalysis required")
- `resolvedName: String`, `nameSource: ExportNameSource`

---

## 2) Error Semantics

### 2.1 Tingwu Retry Rules (per V1 Section 8.1)

| Error | Retryable | Backoff |
|-------|-----------|---------|
| 429 (Rate Limit) | ✅ Yes | Exponential (60s, 120s, 300s) |
| Other 4xx | ❌ No | — |
| 5xx / Network | ✅ Yes | Deterministic |

### 2.2 Chat Retry Rules

- `maxRetries = 2`
- On MachineArtifact validation failure: retry with same prompt
- If retries exhausted: publish `displayMarkdown` if extractable, else fallback message
- Set `artifactStatus = FAILED`, record trace

---

## 3) Data Schemas

For V1 artifact types, use the JSON schema directly:

| Schema | Purpose |
|--------|---------|
| [orchestrator-v1.schema.json](file:///home/cslh-frank/main_app/docs/specs/orchestrator-v1.schema.json) | DisectorPlan, TingwuBatchArtifact, PublishedTranscript, MachineArtifact, M1-M4 |
| [orchestrator-v1.examples.json](file:///home/cslh-frank/main_app/docs/orchestrator-v1.examples.json) | Payload examples |
| [source-repo.schema.json](file:///home/cslh-frank/main_app/docs/source-repo.schema.json) | Third-party provider registry |

### When to Read the Schema

| Scenario | Read Schema? | Why |
|----------|--------------|-----|
| Generating `MachineArtifact` from LLM output | ✅ Yes | Validate structure before emitting |
| Parsing Tingwu response | ✅ Yes | Use `TingwuBatchArtifact` shape |
| Understanding coordinator interfaces | ❌ No | Use this doc (api-contracts.md) |
| UI behavior questions | ❌ No | Use ux-contract.md |
| V1 behavioral invariants | ❌ No | Use Orchestrator-V1.md |

> **Tip**: The schema has an `xAgentGuidance` block at the top with `whenToRead`, `whenNotToRead`, and `keyTypes` for quick navigation.

---

## 4) Cross-References

| Document | Role |
|----------|------|
| [Orchestrator-V1.md](file:///home/cslh-frank/main_app/docs/specs/Orchestrator-V1.md) | V1 spec (behavioral invariants) |
| [ux-contract.md](file:///home/cslh-frank/main_app/docs/specs/ux-contract.md) | UX contracts (what UI must show) |
| [tracker.md](file:///home/cslh-frank/main_app/docs/plans/tracker.md) | Architecture realization status |
