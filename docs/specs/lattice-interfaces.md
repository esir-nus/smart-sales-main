# Lattice Interface Catalog

> **Architecture**: [Orchestrator-Lattice.md](./Orchestrator-Lattice.md)  
> **Canonical Pattern**: [`AiChatService.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/AiChatService.kt)  
> **Data Schemas**: [orchestrator-v1.schema.json](./orchestrator-v1.schema.json)  
> **Last Updated**: 2026-01-14

This document catalogs the Lattice Service interfaces (boxes) and Orchestrators. For extraction progress, see [`tracker.md §7.1`](../plans/tracker.md#71-lattice-module-extraction-status).

---

## 1) Chatter Layer ✅

### 1.1 AiChatService

**File**: [`AiChatService.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/AiChatService.kt)  
**Status**: ✅ Extracted (canonical example for Lattice pattern)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `sendMessage()` | `suspend fun sendMessage(request: AiChatRequest): Result<AiChatResponse>` | Send chat message |
| `streamMessage()` | `fun streamMessage(request: AiChatRequest): Flow<AiChatStreamEvent>` | Stream chat tokens |

**DTOs** (in same file):
- `AiChatRequest` — prompt, model, skillTags, transcriptMarkdown, attachments
- `AiChatResponse` — displayText, structuredMarkdown, references, modelUsed
- `AiChatStreamEvent` — Chunk, Completed, Error

**Fake**: `FakeAiChatService` — in same file, `@Singleton`

**Impl**: [`DashscopeAiChatService.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/DashscopeAiChatService.kt)

---

## 2) Pipeline Layer

> **Status**: Extraction in progress

### 2.0 Disector ✅

**File**: [`Disector.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/disector/Disector.kt)  
**Status**: ✅ Extracted (first Pipeline Layer box)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `createPlan()` | `fun createPlan(totalMs: Long, audioAssetId: String, recordingSessionId: String): DisectorPlan` | V1 Appendix A batch planning |

**DTOs** (in same file):
- `DisectorPlan` — planId, audioAssetId, recordingSessionId, totalMs, batches
- `DisectorBatch` — batchIndex, batchAssetId, absStartMs, absEndMs, captureStartMs, captureEndMs

**Fake**: [`FakeDisector.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/disector/FakeDisector.kt) — stubPlan override, call tracking, reset

**Impl**: [`DisectorImpl.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/disector/DisectorImpl.kt)

---

### 2.1 TingwuSubmissionService ✅

**File**: [`TingwuSubmissionService.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/submission/TingwuSubmissionService.kt)  
**Status**: ✅ Extracted from TingwuRunner

| Member | Signature | Purpose |
|--------|-----------|---------|
| `submit()` | `suspend fun submit(input: SubmissionInput): Result<SubmissionOutput>` | Call Tingwu createTranscriptionTask API |

**DTOs**: `SubmissionInput` (fileUrl, taskKey, sourceLanguage, diarizationEnabled), `SubmissionOutput` (taskId, requestId)

**Fake**: `FakeTingwuSubmissionService` — stubOutput override, call tracking, reset

**Impl**: [`RealTingwuSubmissionService.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/submission/RealTingwuSubmissionService.kt)

---

### 2.2 PollingLoop ✅

**File**: [`TingwuPollingLoop.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/polling/TingwuPollingLoop.kt)  
**Status**: ✅ Interface added (high locality pattern)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `poll()` | `suspend fun poll(jobId, stateFlow, onTerminal)` | Poll Tingwu status until terminal state |

**Fake**: [`FakePollingLoop.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/polling/FakePollingLoop.kt) — stubTerminalState, call tracking, reset

---

### 2.3 AudioPreparerService 🔲

**Responsibility**: Audio slicing and OSS upload

| Member | Signature | Purpose |
|--------|-----------|---------|
| `prepare()` | `suspend fun prepare(input: AudioPrepareInput): Result<AudioPrepareOutput>` | Slice + upload audio |

**DTOs**: TBD during extraction

### 2.2 TranscriptionService 🔲

**Responsibility**: Tingwu API orchestration

### 2.3 MetadataExtractorService 🔲

**Responsibility**: LLM-derived metadata (chapters, summary)

### 2.4 PublisherService 🔲

**Responsibility**: Format and persist transcripts

---

## 3) Memory Layer 🔲

> **Status**: Planned

### 3.1 SessionMemoryService 🔲

**Responsibility**: Current session M2/M3 access (wraps MetaHub)

### 3.2 LongTermMemoryService 🔲

**Responsibility**: Cross-session recall (local index)

### 3.3 KnowledgeBaseService 🔲

**Responsibility**: User doc search (Bailian API)

---

## 4) Connectivity Layer 🔲

> **Status**: To formalize — logic exists, needs interface wrapper

### 4.1 BleService 🔲

**Responsibility**: BLE transport management

### 4.2 WiFiService 🔲

**Responsibility**: WiFi provisioning

### 4.3 BadgeSyncService 🔲

**Responsibility**: State sync with hardware badge

### 4.4 AudioTransferService 🔲

**Responsibility**: High-speed audio data retrieval

---

## 5) Legacy Coordinators (V1)

> [!WARNING]
> **Migrating to Lattice**: These V1 Coordinators are being replaced by Lattice Orchestrators + Services.
> 
> - `TranscriptionCoordinator` → `TranscriptionOrchestrator` + Pipeline Services
> - `ChatCoordinator` → `ChatterOrchestrator` + `AiChatService`
> - `DebugCoordinator`, `ExportCoordinator` — may remain as UI-layer coordinators

### 5.1 TranscriptionCoordinator (Legacy)

**File**: [`TranscriptionCoordinator.kt`](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/domain/transcription/TranscriptionCoordinator.kt)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `state` | `StateFlow<TranscriptionUiState>` | Transcription state for UI |
| `runTranscription()` | `suspend fun runTranscription(...)` | Run transcription with callbacks |

### 5.2 DebugCoordinator (UI Coordinator)

**File**: [`DebugCoordinator.kt`](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/domain/debug/DebugCoordinator.kt)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `debugState` | `StateFlow<DebugUiState>` | Debug state for HUD panel |
| `toggleDebugPanel()` | `fun toggleDebugPanel()` | Toggle visibility |

### 5.3 ExportCoordinator (UI Coordinator)

**File**: [`ExportCoordinator.kt`](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/domain/export/ExportCoordinator.kt)

| Member | Signature | Purpose |
|--------|-----------|---------|
| `exportState` | `StateFlow<ExportUiState>` | Export state for UI |
| `checkExportGate()` | `suspend fun checkExportGate(sessionId): ExportGateState` | Check if export allowed |

---

## 6) Error Semantics

### 6.1 Tingwu Retry Rules (per V1 Appendix D)

| Error | Retryable | Backoff |
|-------|-----------|---------|
| 429 (Rate Limit) | ✅ Yes | Exponential (60s, 120s, 300s) |
| Other 4xx | ❌ No | — |
| 5xx / Network | ✅ Yes | Deterministic |

### 6.2 Chat Retry Rules

- `maxRetries = 2`
- On MachineArtifact validation failure: retry with same prompt
- If retries exhausted: publish `displayMarkdown` if extractable, else fallback message

---

## 7) Cross-References

| Document | Role |
|----------|------|
| [Orchestrator-Lattice.md](./Orchestrator-Lattice.md) | Architecture spec |
| [Orchestrator-V1.md](./Orchestrator-V1.md) | Algorithm appendices only |
| [ux-contract.md](./ux-contract.md) | UX contracts |
| [tracker.md §7.1](../plans/tracker.md#71-lattice-module-extraction-status) | Extraction status |
