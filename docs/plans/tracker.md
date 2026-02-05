# Smart Sales Prism Tracker

> **SOT**: [`Prism-V1.md`](../specs/Prism-V1.md) · [`prism-ui-ux-contract.md`](../specs/prism-ui-ux-contract.md) (INDEX) · [`GLOSSARY.md`](../specs/GLOSSARY.md)  
> **Last Updated**: 2026-02-05

---

## Current Phase

| Phase | Status | Notes |
|-------|--------|-------|
| **Planning** | ✅ | Prism-V1.md + UI/UX Contract finalized |
| **Execution** | 🏗️ | Phase 2 In Progress (Chunk G Complete) |
| **Verification** | 🔲 | — |

---

## Core Components (§2.2)

| Component | Status | Notes |
|-----------|--------|-------|
| **Context Builder** | 🔲 | Multimodal normalization (Tingwu, Qwen-VL) |
| **Executor** | 🔲 | Strategy-based LLM selection per mode |
| **Publisher** | 🔲 | Chat/Analyst/Schedule publishers |
| **Memory Writer** | 🔲 | Fire-and-forget with retry |
| **Schema Linter** | 🔲 | Entity/Plan/Scheduler/Relevancy linters |
| **Memory Center Notifier** | 🔲 | Snackbar updates for memory changes |

---

## Mode Pipelines (§4)

| Mode | Status | Key Features |
|------|--------|--------------|
| **Coach** | 🔲 | Lightweight chat, optional memory search |
| **Analyst** | 🔲 | Planner LLM, visible Plan Card, Chart tools |
| **Scheduler** | 🔲 | Global Top Drawer, structured JSON output |

---

## Memory System (§5)

> **Cerb Docs**: [`docs/cerb/memory-center/`](../cerb/memory-center/) — Self-contained spec + interface

| Layer | Status | Schema Reference |
|-------|--------|------------------|
| **Active Zone** | 🔲 | `MemoryEntry` (isArchived=false) (§5.7) |
| **Archived Zone** | 🔲 | `MemoryEntry` (isArchived=true) (§5.1) |
| **Entity Registry** | 🔲 | `EntityEntry` (§5.2) |
| **Session Cache** | 🔲 | In-task fast access (§2.2 #1b) |
| **ScheduleBoard** | ✅ | Conflict index ([spec](../cerb/memory-center/spec.md#scheduleboard-conflict-index)) |

---

## Testability Contract (§7)

| Criterion | Target | Current |
|-----------|--------|---------|
| **Interface + Impl** | 100% modules | — |
| **Fake Available** | 100% interfaces | — |
| **Zero Android in Domain** | 0 imports | — |

---

## Product Milestones

| Milestone | Status | Description |
|-----------|--------|-------------|
| **M0: Prism Spec** | ✅ | Architecture documented |
| **M1: Core Pipeline** | 🔲 | Context → Execute → Publish working |
| **M2: Memory Integration** | 🔲 | Hot/Cement + Relevancy Library |
| **M3: All Modes** | 🔲 | Coach, Analyst, Scheduler functional |
| **M4: Polish** | 🔲 | UX, performance, edge cases |

---

## Quick Links

- [Prism-V1.md](../specs/Prism-V1.md) — Architecture SOT
- [prism-ui-ux-contract.md](../specs/prism-ui-ux-contract.md) — UX SOT
- [Memory Center Cerb](../cerb/memory-center/) — Spec + Interface (self-contained)
- [legacy-to-prism-dictionary.md](../reference/legacy-to-prism-dictionary.md) — Legacy mapping
- [tracker-lattice-era.md](../archived/tracker-lattice-era.md) — Archived Lattice tracker

---

## Memory Center Waves

> **Cerb Docs**: 
> - [`memory-center/spec.md`](../cerb/memory-center/spec.md) — Core storage, ScheduleBoard
> - [`relevancy-library/spec.md`](../cerb/relevancy-library/spec.md) — Entity lookup, disambiguation
> - [`user-habit/spec.md`](../cerb/user-habit/spec.md) — Behavioral learning
>
> **Strategy**: MVP-first, each wave ships independently

---

### Memory Center Core (spec: `memory-center/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | ScheduleBoard + Two-Phase Pipeline | ✅ SHIPPED |
| **2** | Active/Archived Lazy Compaction + Subscription Config | ✅ SHIPPED |

**Wave 1 Shipped**: 2026-02-03
- ScheduleBoard interface + RealScheduleBoard implementation
- Conflict detection with `excludeId` self-exclusion
- Duration inference from task types

**Wave 2 Shipped**: 2026-02-04
- SubscriptionConfig with FREE/PRO/ENTERPRISE tiers (7/14/30 day windows)
- Tier-aware `getActiveEntries()` and `getArchivedEntries()` methods
- Lazy compaction via query-time filtering (no background jobs)
- 7/7 unit tests passed in `FakeMemoryRepositoryTest`

---

### Entity Registry (spec: `entity-registry/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Core Model + Repository | ✅ (inherited) |
| **2** | LLM Disambiguation Flow | ✅ SHIPPED |
| **2.5** | CRM Schema + Rename (Relevancy → Entity) | ✅ SHIPPED |
| **3** | CRM Hierarchy → [Client Profile Hub](../client-profile-hub/spec.md) | 🔲 (Planning) |

**Wave 2 Shipped**: 2026-02-03
- `ParsedClues` carrier in `LintResult.Success`
- `RealContextBuilder.buildWithClues()` entity bridge
- LLM synthesizes entity resolution using conversation context

**Wave 2.5 Shipped**: 2026-02-04
- Renamed `RelevancyEntry` → `EntityEntry`
- Added CRM fields (accountId, contactId, dealStage)
- Added CRM types (ACCOUNT, CONTACT, DEAL)

---

### User Habit (spec: `user-habit/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Schema + Repository (Storage) | ✅ SHIPPED |
| **2-4** | Moved to RL Module | — |

**Wave 1 Shipped**: 2026-02-04
- `UserHabit` model + `UserHabitRepository` interface
- Fake implementation passing logic tests
- Confidence score calculation (obs/total)

---

### RL Module (spec: `rl-module/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + Observation Schema | ✅ SHIPPED |
| **2** | Orchestrator Integration (Parser) | 🔲 (Planning) |
| **3** | Context Builder Integration | 🔲 |

**Wave 1 Shipped**: 2026-02-04
- `RlModels` (Observation, Source)
- `ReinforcementLearner` interface + Fake facade
- `HabitContext` aggregation logic

---

## Badge Audio Integration

> **Cerb Docs**:
> - [`connectivity-bridge/`](../cerb/connectivity-bridge/) — Prism wrapper for legacy BLE/HTTP
> - [`asr-service/`](../cerb/asr-service/) — FunASR cloud transcription
> - [`badge-audio-pipeline/`](../cerb/badge-audio-pipeline/) — End-to-end orchestration
>
> **Strategy**: Replace simulated transcription with real hardware audio

---

### Connectivity Bridge (spec: `connectivity-bridge/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + Fake | 🔲 (Planning) |
| **2** | Real Implementation (Legacy Wrapper) | 🔲 |
| **3** | record#end Handler | 🔲 |

**Key Deliverables**:
- `ConnectivityBridge` interface (Prism domain)
- `RealConnectivityBridge` wrapping `DeviceConnectionManager`
- Preserved ESP32 rate limiting (2s query TTL, 300ms inter-command gap)

---

### ASR Service (spec: `asr-service/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + Fake | ✅ SHIPPED |
| **2** | FunASR Implementation | 🔲 |
| **3** | Error Handling + Retry | 🔲 |

**Key Deliverables**:
- `AsrService` interface
- `FunAsrService` using DashScope SDK (`fun-asr-realtime` model)
- 16kHz WAV support, Chinese/English language hints

---

### Badge Audio Pipeline (spec: `badge-audio-pipeline/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + State Machine | 🔲 (Planning) |
| **2** | Fake Pipeline | 🔲 |
| **3** | Real Implementation | 🔲 |
| **4** | Error Recovery | 🔲 |

**Key Deliverables**:
- `BadgeAudioPipeline` orchestrator
- `record#end` → Download → Transcribe → Schedule flow
- Integration with existing `PrismOrchestrator.processSchedulerAction()`

---

## Scheduler Waves

> **Cerb Docs**: [`spec.md`](../cerb/scheduler/spec.md) · [`interface.md`](../cerb/scheduler/interface.md)  
> **Strategy**: UX-first, each wave ships independently

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Repository + Linter (Core CRUD) | ✅ Complete |
| **1.5** | ViewModel Wiring | ✅ Complete |
| **2** | Alarm Cascade | ✅ SHIPPED |
| **3** | Smart Reminder Inference | ✅ SHIPPED |
| **4** | Input Classification + Multi-Task + Reschedule | ✅ SHIPPED |
| **5** | Inspiration Storage | ✅ SHIPPED |
| **6** | Conflict Resolution | ✅ SHIPPED |

### Wave 1 & 1.5: Core + Wiring ✅

**Shipped**: 2026-02-02

**Deliverables**: `ScheduledTaskRepository`, `SchedulerLinter`, `SchedulerViewModel`, UI integration with conflict warning

### Wave 2: Alarm Cascade ✅ SHIPPED

**Shipped**: 2026-02-03

**Deliverables**: `RealAlarmScheduler.kt` (rewritten), `TaskReminderReceiver.kt` (rewritten), `FakeAlarmScheduler.kt`, `AlarmSchedulerTest.kt`, notification channel + permission

### Wave 6: Conflict Resolution ✅ SHIPPED

**Shipped**: 2026-02-05

**Deliverables**: `ConflictAction.kt`, `RealConflictResolver.kt`, `ConflictCard.kt`, ViewModel wiring (`handleConflictResolution`, `toggleConflictExpansion`)

**Cerb Docs**: [`conflict-resolver/spec.md`](../cerb/conflict-resolver/spec.md) · [`conflict-resolver/interface.md`](../cerb/conflict-resolver/interface.md)

---

## Federated Spec System ✅

> **Status**: Production Ready (90%) — Shipped 2026-02-01

| Category | Files | Status |
|----------|-------|--------|
| **INDEX** | `prism-ui-ux-contract.md` | ✅ |
| **Terminology** | `GLOSSARY.md` | ✅ |
| **Modules** | `HomeScreen.md`, `SchedulerDrawer.md`, `AudioDrawer.md`, `ConnectivityModal.md`, `AnalystMode.md` | ✅ |
| **Components** | `ThinkingBox.md` | ✅ |
| **Flows** | `OnboardingFlow.md` | ✅ |

**Tech Debt (Logged)**:
- [ ] Add JSON contracts to AudioDrawer, SchedulerDrawer, HomeScreen, ConnectivityModal
- [ ] Monitor OnboardingFlow.md (232 lines) — split if >300

---

## Prism Clean Room Roadmap (5-Phase Model)

> **Strategy**: Contract-First Architecture Reset  
> **Mandate**: NO old code extraction — fresh rewrite only (learn WHAT from legacy, write HOW fresh)  
> **Current**: Phase 1 ✅ → Phase 2 🔲

---

### Phase 0: Infrastructure Audit ✅

**Goal**: Identify reusable shared infrastructure (NOT legacy Prism code)

| Exit Criterion | Status |
|----------------|--------|
| Audit `:data:ai-core` for DashScope/Tingwu clients | ✅ |
| Audit `:core:util` for clean utilities | ✅ |
| Confirm NO legacy Prism imports needed | ✅ |
| Lock dependency list for `app-prism` | ✅ |

**Spike Artifact**: `RealOrchestrator.kt` (Phase 3 prototype, may revise later)

---

### Phase 1: Skeleton + Contracts ✅

**Goal**: Interfaces + Fakes that prove Prism-V1 architecture compiles

| Exit Criterion | Status |
|----------------|--------|
| Core pipeline interfaces (ContextBuilder, Executor, Publisher) | ✅ |
| Memory interfaces (MemoryWriter, MemoryRepository, RelevancyRepository) | ✅ |
| UI state models (base: 5 states, more in Phase 2) | ✅ |
| All Fakes return valid mock data | ✅ |
| Build passes: `./gradlew :app-prism:compileDebugKotlin` | ✅ |
| Zero Android imports in domain layer | ✅ |

**Artifacts**: 14 new files in `domain/core/` and `domain/memory/`

---

### Phase 2: UX Layer + UI Skeletons 🔲

**Goal**: Android prototype runs end-to-end with Fake I/O

| Exit Criterion | Status |
|----------------|--------|
| Home Screen (Session List, Knot FAB) | 🔲 |
| Chat Interface (Coach/Analyst mode toggle) | 🔲 |
| Audio Drawer (bottom gesture, card states) | 🔲 |
| Scheduler Drawer (top gesture, carousels) | 🚧 Logic wired |
| All 3 modes navigable with fake responses | 🔲 |
| UI matches prism-ui-ux-contract.md | 🔲 |

---

### Phase 3: Real Implementation (Swap Fakes) 🔲

**Goal**: Real LLM, real DB, real hardware integration

| Exit Criterion | Status |
|----------------|--------|
| DashScope API (Coach mode working) | ⚡ Prototype exists |
| Room persistence (RelevancyEntry, MemoryEntry) | 🔲 |
| Tingwu integration (audio transcription) | 🔲 |
| ESP32 BLE (badge communication) | 🔲 |
| Memory Writer (fire-and-forget persistence) | 🔲 |
| All integration tests pass | 🔲 |

---

### Phase 4: Beta Shipping 🔲

**Goal**: APK out, bug triage, edge case handling

| Exit Criterion | Status |
|----------------|--------|
| APK size < 50MB | 🔲 |
| Crash-free rate > 99% (Firebase) | 🔲 |
| Cold start < 3s | 🔲 |
| All critical user journeys tested | 🔲 |
| Beta distribution via internal channel | 🔲 |

---

### Phase 1: Skeleton (Interfaces + Fakes) ✅

> **Archived Granular Progress**: See [`tracker-phase-1-granular.md`](../archived/tracker-phase-1-granular.md) for detailed interface/fake tracking.

**Goal**: Production-ready interfaces that evolve with implementation. Fakes prove wiring.

> **Mapping Table**: [`prism-interface-mapping.md`](../reference/prism-interface-mapping.md) — Bidirectional sync with spec

### Phase 2: UX Layer (Page-by-Page + Fake Chaining) 🏗️

**Goal**: Visual & interaction completeness. No real backend.

| Area | Status |
|------|--------|
| **Home Screen** | 🔲 |
| **Audio Drawer** | 🔲 |
| **Chat Interface** | 🔲 |
| **Scheduler Drawer** | 🚧 Logic wired |

> **Archived Detail**: See `tracker-phase-1-granular.md` since Phase 2 granularity is tracked in `docs/CN_Dev/UX合约/` files.

### Phase 3: Core Layer (Real Implementation)
**Goal**: Swap Fakes for Real logic. Data flows to DB/API.

- [ ] **Data Layer**: Room Databases (Sessions, Relevancy, Memory)
- [ ] **External Services**: Tingwu (Aliyun), DashScope (Qwen), ESP32 (BLE)
- [ ] **Pipeline Integration**: Context → LLM → Publisher → Writer
- [ ] **Verification**: Integration tests with Real implementations

---

### Tech Debt (Deferred for Beta)

> **Logged**: 2026-01-30 — Does NOT block beta small circle test

| Item | Location | Issue | Priority |
|------|----------|-------|----------|
| `delay()` in UI | `ResponseBubble.kt:129` | Simulated typing should use Fake | Low |
| `delay()` in UI | `ConnectivityModal.kt:240` | Progress sim should use Fake | Low |
| `delay()` in UI | `OnboardingScreen.kt:226,229` | Firmware progress sim should use Fake | Low |
| `delay()` in UI | `SchedulerDrawer.kt` | ✅ Resolved (Moved to Fake) | Low |
| Fake Tests | `FakeAudioRepository` etc. | No unit tests for Fakes | Low (not needed for beta) |
| Activity Trace Timing | `PrismOrchestrator.kt` | First trace may be missed due to phase transition race | Low |

**Resolution**: Refactor to `FakeProgressService` / `FakeDelayService` post-beta.

---

### Phase 4: Ship
- [ ] Beta APK Distribution
- [ ] Bug Triage & Edge Case Handling
- [ ] Store Submission
