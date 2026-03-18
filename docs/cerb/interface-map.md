# Interface Map

> **System**: Smart Sales is an AI-powered sales assistant. A BLE badge records conversations, the app transcribes them, creates scheduled tasks, and provides proactive insights — all coordinated through an LLM pipeline.
>
> **Purpose**: Module ownership + data flow. Read this BEFORE any cross-module change.
> **Rule**: If data belongs to Module B, query B's interface at runtime. Don't store B's data on A's model.
> **Last Updated**: 2026-03-18 (Test infrastructure sync)
>
> **Status Legend**: ✅ = Shipped (Real impl) · 📐 = Interface only (Fake impl) · 🔲 = Not yet coded

---

## Layer 1: Infrastructure

Leaf services with no upstream dependencies. They don't call other modules.
**Architectural Rule**: Layer 1 modules are fully self-contained Android libraries. They define their own interfaces and domain models within their respective modules (e.g., `data/oss/`). They do **NOT** place definitions in the central `app-core/domain/` layer.

| Module | Track | Owns (Writes) | Reads From | Key Interface | OS Layer | Status |
|--------|-------|--------------|------------|---------------|----------|--------|
| **[ConnectivityBridge](./connectivity-bridge/spec.md)** | Hardware & Audio | BLE + HTTP device state | — | `connectionState: StateFlow`, `isReady()` | — | ✅ |
| **[NotificationService](./notifications/spec.md)** | System I & Ambient | System notification display | — | `show(id, title, body, channel...) -> Unit` | — | ✅ |
| **[OSS](./oss-service/spec.md)** | Hardware & Audio | File upload/download | — | `upload(File, objectKey) -> OssUploadResult` | — | ✅ |
| **[ASR](./asr-service/spec.md)** | Hardware & Audio | Transcription results | OSS (downloads audio files to transcribe) | `transcribe(File) -> AsrResult` | — | ✅ |
| **[TingwuPipeline](./tingwu-pipeline/spec.md)** | Hardware & Audio | Transcription & Audio Intelligence | OSS (reads `fileUrl`) | `submit(TingwuRequest) -> Result<String>` | OS: SSD | ✅ |
| **[PipelineTelemetry](./pipeline-telemetry/spec.md)** | System II & Routing | Pipeline logs (to Logcat) | — | `recordEvent(PipelinePhase, String) -> Unit` | OS: RAM | ✅ |
| **[TestFakesDomain](./test-infrastructure/spec.md)** | Testing | Pure JVM Domain Fakes | All Pure Domain Interfaces | `FakeMemoryRepository`, `FakeEntityRepository` | OS: JVM | ✅ |
| **[TestFakesPlatform](./test-infrastructure/spec.md)** | Testing | Android/Platform State | test-fakes-domain, App-Core | `FakeExecutor`, `FakeContextBuilder`, `FakeToolRegistry` | OS: Android | ✅ |

---

## Layer 2: Data Services

Store and query domain data. Other modules use their interfaces but never each other's storage.
**Architectural Rule**: The domain contracts for these services are physically extracted into isolated Gradle library modules (`:domain:crm`, `:domain:memory`, `:domain:habit`, `:domain:session`). The Gradle graph strictly enforces that LTM/SSD modules (`crm`, `memory`, `habit`) do **NOT** depend on STM/RAM (`session`). Additionally, their physical Room storage implementations are isolated into `:data:*` feature modules backed by a strictly decoupled `:core:database` persistence engine.

| Module | Track | Owns (Writes) | Reads From | Key Interface | OS Layer | Status |
|--------|-------|--------------|------------|---------------|----------|--------|
| **[CoreContracts](./core-contracts/spec.md)** | Foundation | Strict JSON-to-Kotlin Models (`UnifiedMutation`) | — | `UnifiedMutation` (The "One Currency" contract driving Prompt Schema & Linter Deserialization) | OS: App | ✅ |
| **[EntityWriter](./entity-writer/spec.md)** (LTM) | Entity Resolution | Entity mutations (create/update/merge aliases) | SessionContext (write-through to RAM S1) | `upsertFromClue(String, ...) -> UpsertResult` | OS: App | ✅ |
| **[EntityRegistry](./entity-registry/spec.md)** (LTM) | Entity Resolution | Entity queries (read-only view of entities) | — | `findByAlias(String) -> List<EntityEntry>` | OS: SSD | ✅ |
| **[MemoryCenter](./memory-center/spec.md)** (LTM) | Memory & OS | Conversation memory entries | — | `suspend search(String) -> List<MemoryEntry>` | OS: SSD | ✅ |
| **[UserHabit](./user-habit/spec.md)** (RL) | Memory & OS | Behavioral pattern observations | — | `RlPayload` (Secondary Currency for HabitListener) | OS: SSD | ✅ |
| **[SessionHistory](./session-history/spec.md)** (STM) | Memory & OS | Session navigation metadata (list, pin, rename) | — | `getGroupedSessionsFlow() -> Flow<Map>` | OS: SSD | ✅ |
| **[SessionContext](./session-context/spec.md)** (STM) | Memory & OS | Per-session workspace (3 sections), sticky notes, scheduler pattern signal transport | EntityWriter (S1 via write-through), RLModule (S2/S3) | *(Merged into ContextBuilder)* | OS: Kernel | ✅ |
| **AliasCache** (L1 Cache) | Entity Resolution | Fast-lookup mapping for EntityCandidates | EntityRegistry (Hydration) | `suspend match(List<String>) -> CacheResult` | OS: RAM | ✅ |
| **[Mutation Module](./scheduler-domain/spec.md)** | Intelligent Scheduler | Atomic Operations, Lexical Conflict Checks, Delete->Insert Reschedule | ScheduleBoard | `suspend rescheduleTask(...)`, `insertTask(...)` | OS: RAM | ✅ |
| **[SchedulerDomain](./scheduler-domain/spec.md)** (LTM) | Intelligent Scheduler | ScheduledTask, InspirationEntry | — | `ScheduledTaskRepository` | OS: SSD | ✅ |

> **EntityWriter vs EntityRegistry**: Writer handles mutations (dedup, merge, alias registration) AND write-through to RAM S1. Registry handles queries. Callers MUST use Writer for writes, Registry for reads. Never call `EntityRepository.save()` directly.
>
> **EntityWriter → SessionContext (CQRS write-through)**: EntityWriter synchronously updates RAM Section 1 via `RealContextBuilder.updateEntityInSession()` to keep memory instantly consistent. It defers SSD persistence to an asynchronous `AppScope`. Callers experience zero SSD transit latency.

---

## Layer 3: Core Pipeline

Orchestrates LLM-powered processing. Reads from Layer 2 data services.

| Module | Track | Owns (Writes) | Reads From | Key Interface | OS Layer | Status |
|--------|-------|--------------|------------|---------------|----------|--------|
| **ContextBuilder** | System II & Routing | `EnhancedContext` (assembled prompt context incl. `scheduleContext` and `schedulerPatternContext`) | EntityRegistry, MemoryCenter, ScheduledTaskRepository, HistoryRepository | `suspend build(String, Mode, ...) -> EnhancedContext` | OS: Kernel | ✅ |
| **[InputParser](./input-parser/spec.md)** | System II & Routing | Semantic intent and EntityID resolution | AliasIndex (internal) | `suspend parseIntent(String) -> ParseResult` | OS: App | ✅ |
| **[EntityDisambiguator](./entity-disambiguation/spec.md)** | Entity Resolution | `PendingIntent` interruption state | InputParser | `suspend process(String) -> DisambiguationResult` | OS: App | ✅ |
| **[LightningRouter](./lightning-router/spec.md)** | System II & Routing | Intent evaluation & Fast-fail alias check (Phase 0) | ContextBuilder, AliasCache | `suspend evaluateIntent(EnhancedContext) -> RouterResult?` | OS: App | ✅ |
| **EntityResolverService** | Entity Resolution | Entity disambiguation matching | EntityRegistry | `suspend resolve(String, List<EntityEntry>) -> EntityEntry?` | OS: App | ✅ |
| **ModelRegistry** | System II & Routing | Static LLM Profiles (models, temps, skills) | — | `ModelRegistry` | OS: App | ✅ |
| **[Executor](./model-routing/spec.md)** | System II & Routing | Raw LLM output (stateless — no storage) | ModelRouter | `suspend execute(LlmProfile, String) -> ExecutorResult` | — | ✅ |
| **[PluginRegistry](./plugin-registry/spec.md)** | System II & Routing | Executable pure-Kotlin workflows (Tools), runtime capability-gateway dispatch | SessionContext / Kernel (read-only via PluginGateway), future bounded OS capabilities | `executeTool(ToolId, PluginRequest, PluginGateway) -> Flow<UiState>` | OS: App | ✅ |
| **[SchedulerLinter](./scheduler-linter/spec.md)** | Intelligent Scheduler | Intent parsing to DTOs | — | `suspend parseFastTrackIntent(String) -> FastTrackResult` | OS: App | ✅ |
| **[UnifiedPipeline](./unified-pipeline/spec.md)** | System II & Routing | System II context ETL, typed profile proposals, typed scheduler task-command proposals | ContextBuilder, InputParser, EntityDisambiguator | `suspend processInput(PipelineInput) -> Flow<PipelineResult>` | OS: App | ✅ |
| **[IntentOrchestrator](./scheduler-path-a-spine/spec.md)** | System II & Routing | High-level intent routing (Phase 0) and shared Path A scheduler spine | AgentViewModel, LightningRouter, UnifiedPipeline, PluginRegistry | `suspend processInput(String, isVoice) -> Flow<PipelineResult>` | OS: App | ✅ |

> **UnifiedPipeline emits typed proposals; IntentOrchestrator owns commit handoff.** Profile/entity proposals commit through `EntityWriter`. Scheduler create/delete/reschedule proposals commit through scheduler-owned paths (`FastTrackMutationEngine`, `ScheduledTaskRepository`, `ScheduleBoard`). Feature modules still receive results; they do not own these writes.
>
> **ContextBuilder reads EntityRegistry for Entity Knowledge Context.** `ContextBuilder.buildEntityKnowledge()` calls `EntityRepository.getAll()` at session start to load the structured entity graph into the LLM prompt (RAM Section 1). This is a Kernel → SSD read.
>
> **PluginRegistry runtime boundary (Wave 21 / T3)**: runtime plugins may read bounded session history and emit bounded progress only through `PluginGateway`. Plugin-caused writes remain deferred to the later typed mutation re-entry contract.

---

## Layer 4: Features

User-facing features. Each receives processed results from Orchestrator (Layer 3) and reads from Data Services (Layer 2).

| Module | Track | Owns (Writes) | Reads From (directly) | Receives From (via Orchestrator) | OS Layer | Status |
|--------|-------|--------------|----------------------|----------------------------------|----------|--------|
| **[Mascot (System I)](./mascot-service/spec.md)** | System I & Ambient | Ephemeral interactions, greetings | EventBus (Idle, Error) | `StateFlow<MascotState>` | OS: App | ✅ |
| **[SchedulerDrawer](./scheduler/spec.md)** | Intelligent Scheduler | Visual UI states | Scheduler, ScheduleBoard | `ISchedulerViewModel` | OS: App | ✅ |
| **[ScheduleBoard](./scheduler/spec.md)** | Intelligent Scheduler | Conflict index (in-memory cache) | ScheduledTaskRepository (populates index) | — | OS: SSD | ✅ |
| **[BadgeAudioPipeline](./badge-audio-pipeline/spec.md)** | Hardware & Audio | Audio recording lifecycle | ASR, OSS, ConnectivityBridge | Delegates transcript to `IntentOrchestrator`; consumes early `PathACommitted` completion while Path B continues in background | — | ✅ |
| **[AudioManagement](./audio-management/spec.md)** | Hardware & Audio | Manual sync/transcribe states | ConnectivityBridge, TingwuPipeline | *Observes DB State* | OS: App | 🚧 |
| **[ConflictResolver](./conflict-resolver/spec.md)** | Intelligent Scheduler | Conflict resolution actions | ScheduleBoard | `resolve(...) -> ConflictResolution` | OS: App | ✅ |
| **[AgentIntelligenceUI](../cerb-ui/agent-intelligence/spec.md)** | System II & Routing | Wait-state UI components | — | `StateFlow<UiState>` | OS: App | 📐 |
| **[DevicePairing](./device-pairing/spec.md)** | Hardware & Audio | BLE pairing session states | Legacy BLE stack | `StateFlow<PairingState>` | OS: App | ✅ |

> **"Reads From" vs "Receives From"**: "Reads From" = the feature calls the interface directly. "Receives From" = UnifiedPipeline pushes results into the feature's ViewModel. This distinction prevents confusion about who initiates the call.

> **Domain vs UI Decoupling Rule (Wave 13)**: Features in Layer 4 (e.g., Scheduler, CRM) MUST define their own internal UI State projections (e.g., `SchedulerUiState`). They MUST NOT leak `app-core` ViewModels or UI State flags directly into Layer 2 Domain contracts. The Domain contract (`ScheduledTask`) is the SSD truth; the UI translates it.

---

## Delivery Workflow Registry (The TaskBoard Vault)

The backend maintains a list of pure-Kotlin actionable workflows (The "Hands"). The LLM never executes these—it only recommends them by returning their `workflowId`.

Current recognized Vault IDs:
- `GENERATE_PDF`
- `EXPORT_CSV`
- `DRAFT_EMAIL`
- `TALK_SIMULATOR` (Plugin Workflow)

---

## Layer 5: Intelligence

Cross-cutting services that aggregate data from multiple Layer 2 sources.

| Module | Track | Owns (Writes) | Reads From | Key Interface | OS Layer | Status |
|--------|-------|--------------|------------|---------------|----------|--------|
| **[ClientProfileHub](./client-profile-hub/spec.md)** | Memory & OS | Aggregated client context for tips | EntityRegistry, MemoryCenter, UserHabit | `observeProfileActivityState(String) -> Flow<ProfileActivityState>` | OS: App | 📐 |
| **[RLModule](./rl-module/spec.md)** | Memory & OS | Habit context for prompts (S2/S3 population) | UserHabit | `loadUserHabits() -> HabitContext` | OS: App | ✅ |

---

## Data Flow: Voice → Task

```mermaid
graph TD
    A["Badge Mic"] --> B["ConnectivityBridge"]
    B --> C["BadgeAudioPipeline"]
    C --> D["ASR (Tingwu)"]
    D -->|Submits to| E["IntentOrchestrator (Phase 0)"]
    E -->|1. Gateway| E1["LightningRouter"]
    E1 -->|GREETING/NOISE| F1["MascotService"]
    E1 -->|Entity Candidate| E1a["AliasCache (L1 Lookup)"]
    E1a -->|Fast-Fail >1| H2["IntentOrchestrator holds Pending State"]
    E1a -->|1 Exact Match| E2["UnifiedPipeline (with injected EntityID)"]
    E1 -->|No Candidate / CRM_TASK| E2
    E2 --> F0["EntityDisambiguator (Heavy Gateway)"]
    F0 --> F2["ContextBuilder (Kernel ETL fetching SSD Graph)"]
    F2 --> F["Executor (LLM)"]
    F --> G["UnifiedMutation JSON Contract (One Currency)"]
    G --> H1["UnifiedPipeline emits MutationProposal / TaskCommandProposal"]
    H1 --> H2
    H2 -->|User Confirms| H3["IntentOrchestrator routes to owning executor"]
    H3 --> I["EntityWriter / Scheduler Owner"]
```

---

## Ownership Rules

| Rule | Rationale |
|------|-----------|
| **Entity resolution** belongs to EntityRegistry (`findByAlias`). Consumers store display names, not IDs. | IDs can change when entities merge. Display names are the stable key for consumers. |
| **Entity mutations** go through EntityWriter only. Never call `EntityRepository.save()`. | EntityWriter handles dedup, alias registration, and merge policies. Bypassing it creates orphaned entities. |
| **Memory queries** go through MemoryRepository. Never cache memory entries long-term. | Memory entries are hot storage — they can be updated or deleted by any pipeline run. Caching creates stale reads. |
| **Conflict detection** belongs to ScheduleBoard. ViewModel observes results, doesn't compute. | ScheduleBoard maintains a time-indexed cache. Recomputing in ViewModel would miss concurrent inserts. |
| **LLM calls** go through Executor. No module calls Dashscope directly. | Executor handles retry, timeout, and model selection policies. Direct calls bypass rate limiting. |
| **STM vs LTM constraint** | LTM (Memory/CRM) must not depend on STM (Session). STM is ephemeral and session-scoped. LTM is persistent and cross-session. Inverse dependency creates memory leaks and architectural breaks. |

---

## Anti-Patterns This Map Prevents

| ❌ Wrong | ✅ Right | Why |
|----------|---------|-----|
| Store `entityId` on Task model | Query `EntityRepository.findByAlias()` at use time | EntityRegistry owns resolution; stored IDs go stale on merge |
| Call `EntityRepository.save()` | Call `EntityWriter.upsertFromClue()` | EntityWriter owns dedup/merge/alias logic |
| Import ASR types in Scheduler | Go through Orchestrator | Layer 1 → Layer 4 skip violates dependency direction |
| Cache MemoryEntry on ViewModel | Query MemoryRepository per request | Memory entries are mutable hot storage |
| Feature module calls EntityWriter (production) | UnifiedPipeline calls EntityWriter | Only Layer 3 writes entities; Layer 4 receives results |
| Bypass ContextBuilder for RAM writes | EntityWriter calls `RealContextBuilder.updateEntityInSession()` | Write-through keeps SSD and RAM in sync automatically |
