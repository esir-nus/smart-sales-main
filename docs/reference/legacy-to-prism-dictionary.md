# Legacy → Prism Reference Dictionary

> **Purpose**: Quick lookup for agents when encountering legacy concepts or code.
> **Usage**: Search by legacy term to find Prism equivalent and working code references.

> [!CAUTION]
> **CRITICAL: These references are MICRO-FUNCTIONAL ONLY.**
> - Legacy code patterns do **NOT** automatically fit into Prism architecture
> - Use references to understand **WHAT** something does, not **HOW** to build it
> - When implementing for Prism, **REWRITE from spec**, don't copy-paste legacy
> - The dictionary is for **terminology mapping**, not architecture guidance
> - **Architecture.md is the SOT** — legacy code is just a learning reference

---

## 1. Terminology Mapping

### Architecture Eras

| Era | Spec | Status |
|-----|------|--------|
| **Orchestrator-V1** | `docs/archived/Orchestrator-V1.md` | ❌ Deprecated |
| **Lattice** | `docs/archived/Orchestrator-Lattice.md` | ❌ Deprecated |
| **Prism** | `docs/specs/Architecture.md` | ✅ Current SOT |

### Core Concepts

| Legacy Term | Prism Equivalent | Prism Spec Reference |
|-------------|------------------|---------------------|
| **M1** (raw chat segments) | Hot Zone entry (`isArchived=false`) | §5.1 Two-Zone Model |
| **M2** (TranscriptMetadata) | Session Cache + Hot Zone | §2.2.1b Session Cache |
| **M2B** (ChapterMeta) | `RelevancyEntry.metricsHistoryJson` | §5.2 |
| **M3** (SessionMetadata) | `RelevancyEntry` per entity | §5.2 Relevancy Library |
| **MetaHub** | Memory Writer + Relevancy Library | §2.2.5 Memory Writer |
| **Box pattern** (Lattice) | Strategy pattern (Prism) | §2.1 Unified Pipeline |
| **Thin Orchestrator** | Executor (mode-specific) | §4 Mode Pipelines |

### Pipeline Components

| Legacy | Prism | Notes |
|--------|-------|-------|
| Disector | Context Builder input tool | Still valid concept |
| Sanitizer | TranscriptProcessor | Formatting logic |
| ChatPublisher | ChatPublisher (Mode Strategy) | Unchanged |
| TingwuCoordinator | Part of Pipeline (Audio tool) | §2.2.1 Input Normalization |

---

## 2. Working Code References

### Domain Layer (Pure Kotlin, Zero Android)

> **Location**: `app-core/src/main/java/com/smartsales/prism/domain/`

| Interface | File | Status | Use As Reference For |
|-----------|------|--------|----------------------|
| `ChatCoordinator` | `chat/ChatCoordinator.kt` | ✅ Working | Coordinator pattern |
| `ChatMetadataCoordinator` | `chat/ChatMetadataCoordinator.kt` | ✅ Working | Metadata extraction |
| `TranscriptionCoordinator` | `transcription/TranscriptionCoordinator.kt` | ✅ Working | Batch orchestration |
| `DebugCoordinator` | `debug/DebugCoordinator.kt` | ✅ Working | HUD data management |
| `ExportCoordinator` | `export/ExportCoordinator.kt` | ✅ Working | Export gate pattern |
| `SessionsManager` | `sessions/SessionsManager.kt` | ✅ Working | Session CRUD |
| `DomainModule` | `DomainModule.kt` | ✅ Working | Hilt binding pattern |

### Data Layer (Provider/Integration)

> **Location**: `data/ai-core/src/main/java/com/smartsales/data/aicore/`

| Interface | File | Status | Use As Reference For |
|-----------|------|--------|----------------------|
| `AiChatService` | `AiChatService.kt` | ✅ Canonical | Lattice Box pattern |
| `TingwuCoordinator` | `TingwuCoordinator.kt` | ✅ Working | Complex orchestration |
| `TranscriptProcessor` | `tingwu/processor/TingwuTranscriptProcessor.kt` | ✅ Working | Transform pipeline |
| `Publisher` | `tingwu/publisher/TranscriptPublisher.kt` | ✅ Working | Artifact extraction |
| `PollingLoop` | `tingwu/polling/TingwuPollingLoop.kt` | ✅ Working | Async polling pattern |
| `TingwuJobStore` | `tingwu/store/TingwuJobStore.kt` | ✅ Working | Job persistence |
| `PostTingwuTranscriptEnhancer` | `posttingwu/PostTingwuTranscriptEnhancer.kt` | ✅ Working | Post-processing |
| `PipelineTracer` | `debug/PipelineTracer.kt` | ✅ Working | Debug instrumentation |

### MetaHub (Legacy Data Types)

> **Location**: `core/util/src/main/java/com/smartsales/core/metahub/`
> **Note**: These are LEGACY types. Prism uses RelevancyEntry instead.

| File | Legacy Role | Prism Mapping |
|------|-------------|---------------|
| `MetaHub.kt` | Hub interface |→ Memory Writer |
| `SessionMetadata.kt` | M3 data class | → RelevancyEntry entities |
| `TranscriptMetadata.kt` | M2 data class | → Hot Zone + metricsHistoryJson |
| `ConversationDerivedState.kt` | M2 derived state | → Session Cache |
| `InMemoryMetaHub.kt` | Hub impl | → RelevancyLibrary impl |

---

## 3. Pattern References

### Lattice Box Pattern → Prism Strategy

**Canonical Example**: [AiChatService.kt](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/AiChatService.kt)

```
Interface + Impl + Fake = testable box
```

### Fire-and-Forget Persistence

**Prism §2.2.5**: Memory writes are async, UI never blocks.

**Reference**: `MetaHubWriter` in `tingwu/metadata/`

### Session Cache Pattern

**Prism §2.2.1b**: In-memory cache for fast tool execution.

**Reference**: Not yet implemented (Prism concept)

---

## 4. What to Reference vs What to Ignore

### ✅ Reference (Micro-practices that work)

| Pattern | Where to Find |
|---------|---------------|
| Interface + Impl + Fake | Any Lattice box |
| Hilt DI bindings | `DomainModule.kt` |
| Coordinator delegation | `HomeViewModel` → coordinators |
| Transcript formatting | `TranscriptFormatter.kt` |
| Polling with backoff | `TingwuPollingLoop.kt` |

### ❌ Ignore (Legacy patterns)

| Pattern | Why |
|---------|-----|
| M1/M2/M3 naming | Prism uses Hot/Cement/Relevancy |
| MetaHub blocking writes | Prism uses fire-and-forget |
| ConversationViewModel | Deleted (Redux overhead) |
| God object patterns | Rewrite, don't reference |

---

## 5. Quick Lookup by Task

| When doing... | Look at... |
|---------------|------------|
| Adding a new coordinator | `TranscriptionCoordinator` pattern |
| Adding Hilt binding | `DomainModule.kt` |
| Implementing async polling | `TingwuPollingLoop.kt` |
| Adding debug instrumentation | `PipelineTracer.kt` |
| Processing transcripts | `TranscriptFormatter.kt` |
| Persisting job state | `TingwuJobStore.kt` |
| Memory/entity storage | Prism §5 (NOT old MetaHub) |
