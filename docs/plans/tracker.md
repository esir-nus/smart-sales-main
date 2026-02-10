# Smart Sales Prism Tracker

> **SOT**: [`Prism-V1.md`](../specs/Prism-V1.md) · [`prism-ui-ux-contract.md`](../specs/prism-ui-ux-contract.md) (INDEX) · [`GLOSSARY.md`](../specs/GLOSSARY.md)  
> **Last Updated**: 2026-02-08

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
| **Context Builder** | ✅ | Session Context complete (Waves 1-3 shipped) |
| **Executor** | ✅ | `DashscopeExecutor` — Qwen LLM calls |
| **Publisher** | 🔲 | Chat/Analyst/Schedule publishers (inline in Orchestrator) |
| **Memory Writer** | ✅ | Delivered via `ContextBuilder.saveToMemory()` (retry deferred to Wave 4) |
| **Schema Linter** | ✅ | `SchedulerLinter` — structured JSON validation |
| **Memory Center Notifier** | 🔲 | Snackbar updates for memory changes |

---

## Mode Pipelines (§4)

| Mode | Status | Key Features |
|------|--------|--------------|
| **Coach** | 🚧 | Wave 1-2 shipped (`RealCoachPipeline`), Wave 3-4 pending |
| **Analyst** | 🚧 | `AnalystFlowControllerV2` wired, structured table output |
| **Scheduler** | ✅ | 8 waves (6 shipped, W8 in progress, W9 planned) |

---

## Memory System (§5)

> **Cerb Docs**: [`docs/cerb/memory-center/`](../cerb/memory-center/) — Self-contained spec + interface

| Layer | Status | Schema Reference |
|-------|--------|------------------|
| **Active Zone** | ✅ | `MemoryEntry` (isArchived=false) — `getActiveEntries()` shipped |
| **Archived Zone** | ✅ | `MemoryEntry` (isArchived=true) — `getArchivedEntries()` shipped |
| **Entity Registry** | ✅ | `EntityEntry` (§5.2) |
| **Session Cache** | ✅ | Path indexing + entity state tracking (§2.2 #1b) |
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
| **M1: Core Pipeline** | ✅ | Context → Execute → Publish working |
| **M2: Memory Integration** | ✅ | Room persistence, Entity Registry, RL Module |
| **M3: All Modes** | 🚧 | Coach ✅, Scheduler ✅, Analyst 🚧 |
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
| **3** | CRM Hierarchy → Client Profile Hub | ✅ SHIPPED |

**Wave 2 Shipped**: 2026-02-03
- `ParsedClues` carrier in `LintResult.Success`
- `RealContextBuilder.buildWithClues()` entity bridge
- LLM synthesizes entity resolution using conversation context

**Wave 2.5 Shipped**: 2026-02-04
- Renamed `RelevancyEntry` → `EntityEntry`
- Added CRM fields (accountId, contactId, dealStage)
- Added CRM types (ACCOUNT, CONTACT, DEAL)

**Wave 3 Shipped**: 2026-02-08 (→ Client Profile Hub Wave 1)
- `ClientProfileHub` interface (`getQuickContext`, `getFocusedContext`, `getUnifiedTimeline`)
- Domain models: `EntitySnapshot`, `FocusedContext`, `QuickContext`, `UnifiedActivity`
- `getByAccountId()` hierarchy query in `EntityRepository`
- `FakeClientProfileHub` + 4 passing tests

**Client Profile Hub Wave 2 Shipped**: 2026-02-08 (Timeline Aggregation)
- Entity-tagged `MemoryEntry.structuredJson` via `RealContextBuilder.saveToMemory()`
- `MemoryRepository.getByEntityId()` with quoted JSON matching
- `FakeClientProfileHub.getUnifiedTimeline()` with `MemoryEntry→UnifiedActivity` mapping
- `ContextBuilder.record*()` now suspend + persists to MemoryRepository
- 4 new tests (8 total for Client Profile Hub)

---

### Session Context (spec: `session-context/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Skeleton (Data classes + wiring) | ✅ SHIPPED |
| **2** | Path Indexing (Cache hit logic) | ✅ SHIPPED |
| **3** | Smart Triggers (State-driven loading) | ✅ SHIPPED |

**All Waves Shipped**: 2026-02-07
- `SessionContext`, `EntityTrace`, `EntityState` domain models
- O(1) alias resolution via path index cache (50-entry LRU)
- Session-scoped caching — no expiry timer, valid for entire session

---

### Client Profile Hub (spec: `client-profile-hub/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + Domain Models + Fake | ✅ SHIPPED |
| **2** | Timeline Aggregation | ✅ SHIPPED |
| **3** | CRM Export Integration | 🔲 |

**Wave 1 Shipped**: 2026-02-08
- `ClientProfileHub` interface, `EntitySnapshot`, `FocusedContext`, `QuickContext`, `UnifiedActivity`
- `getByAccountId()` hierarchy query, `FakeClientProfileHub` + 4 tests

**Wave 2 Shipped**: 2026-02-08
- Entity-tagged `MemoryEntry.structuredJson`, `MemoryRepository.getByEntityId()`
- `ContextBuilder.record*()` now suspend + persists to MemoryRepository

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
| **1.5** | Schema Migration (4-rule model) | ✅ SHIPPED |
| **2** | Orchestrator Integration (Parser) | ✅ SHIPPED |
| **3** | Context Builder Integration | ✅ SHIPPED |
| **4** | Time Decay + Deletion Cleanup | 🔲 |
| **5** | **OS Model Upgrade** (RAM Application) | 🔲 |

**Wave 1 Shipped**: 2026-02-04
- `RlModels` (Observation, Source)
- `ReinforcementLearner` interface + Fake facade
- `HabitContext` aggregation logic

**Wave 1.5 Shipped**: 2026-02-05
- `ObservationSource`: 3 values (INFERRED, USER_POSITIVE, USER_NEGATIVE)
- `UserHabit`: New 4-rule schema (inferredCount, explicitPositive, explicitNegative)
- `UserHabitRepository.observe(source)` + `delete()`
- `FakeUserHabitRepository`: Source-based routing
- Tests updated for new schema

**Wave 2 Shipped**: 2026-02-05
- `PrismOrchestrator.parseRlObservations()`: JSON parser for rl_observations
- Fire-and-forget wiring in `processSchedulerInput()`
- Source string → enum mapping

**Wave 3 Shipped**: 2026-02-05
- Added `habitContext` field to `EnhancedContext`
- Injected `ReinforcementLearner` into `RealContextBuilder`
- Wired `getHabitContext()` in both `build()` and `buildWithClues()`
- Created `RealContextBuilderTest` (4/4 tests passing)
- Learning feedback loop now complete: observations → storage → LLM prompts

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
| **1** | Interface + Fake | ✅ SHIPPED |
| **2** | Real Implementation (Backend) | ✅ SHIPPED |
| **2.5** | UI Wiring | ✅ SHIPPED |
| **3** | log# Recording Handler (BLE notification) | ✅ SHIPPED (2026-02-09) |
| **4** | Battery Level Reporting | 🔲 (Pending hardware) |

**Key Deliverables**:
- `ConnectivityBridge` interface (Prism domain)
- `RealConnectivityBridge` wrapping `DeviceConnectionManager`
- Preserved ESP32 rate limiting (2s query TTL, 300ms inter-command gap)

**Tech Debt**: 
- Battery level currently hardcoded placeholder (85%) in `ConnectivityViewModel`
- Real BLE battery characteristic not yet implemented by hardware team
- UI wired to observe placeholder, ready for real data when available
- `disconnect()` calls `forgetDevice()` (nuclear unpair) — no soft disconnect API in `DeviceConnectionManager`
- Modal stays open after disconnect — consider auto-dismiss with toast "已断开连接"
- Error states show generic "离线" — should show contextual hints ("检查蓝牙" vs "检查WiFi")

---

### Device Pairing (spec: `device-pairing/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Core Service (Wrap Legacy) | ✅ SHIPPED |
| **1.5** | Wiring (Onboarding UI) | ✅ SHIPPED |
| **2** | Robustness & Error Handling | 🔲 PLANNED |
| **3** | Polish (UX Refinement) | 🔲 PLANNED |

**Key Deliverables**:
- `PairingService` interface (Cerb compliant)
- `RealPairingService` wrapping `DeviceConnectionManager` + `BleScanner`
- Simplified 6-state model (vs legacy 9-state)

---

### ASR Service (spec: `asr-service/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + Fake | ✅ SHIPPED |
| **2** | FunASR Batch Implementation (OSS + Transcription API) | 🔧 IN PROGRESS |
| **3** | Error Handling + Retry | 🔲 |

**Key Deliverables**:
- `AsrService` interface
- `FunAsrService` refactoring to Batch API (`fun-asr` model via `Transcription.asyncCall`)
- Requires OSS upload (see OSS Service below)

**Wave 2 Note**: Original implementation used `fun-asr-realtime` (streaming) which returned empty results on local files. Switching to Batch API for file-based recognition.

---

### OSS Service (spec: `oss-service/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Core Upload (`OssUploader`) | 🔲 |
| **2** | Resilience (retry, multipart) | 🔲 |

**Key Deliverables**:
- `OssUploader` interface + `RealOssUploader` (Aliyun OSS SDK)
- Standalone `data:oss` module
- Public-read bucket URL generation

---

### Audio Management (spec: `audio-management/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + Fake | ✅ SHIPPED |
| **2** | Room Persistence | 🔲 |
| **3** | Pipeline Integration | 🔲 |
| **4** | UI (Audio Drawer) | 🔲 |

**Wave 1 Shipped**: `AudioRepository` interface, `FakeAudioRepository`, domain models (`AudioFile`, `AudioSource`, `TranscriptionStatus`)

---

### Badge Audio Pipeline (spec: `badge-audio-pipeline/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + State Machine | ✅ SHIPPED (2026-02-05) |
| **2** | Fake Pipeline | ✅ SHIPPED (2026-02-05) |
| **3** | Real Implementation | ✅ SHIPPED (2026-02-08) |
| **4** | Error Recovery | 🔲 |

**Key Deliverables**:
- `BadgeAudioPipeline` orchestrator
- `log#YYYYMMDD_HHMMSS` → Download → Transcribe → Schedule flow
- Integration with existing `PrismOrchestrator.processSchedulerAction()`

---

### Coach Mode (spec: `coach/`)

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + Fake | ✅ SHIPPED |
| **2** | Real LLM + Context | ✅ SHIPPED |
| **3** | Memory + Habit | ✅ SHIPPED (2026-02-08) |
| **4** | Analyst Suggestion | ✅ SHIPPED (2026-02-08) |

**Key Deliverables**:
- `CoachPipeline` interface + `RealCoachPipeline` (LLM via Executor)
- `FakeCoachPipeline` for testing
- Wired into `PrismOrchestrator` as default mode

**L2 Testing** (2026-02-08):
- ✅ 12/20 tests passed, all critical path verified
- Memory search: 3 seed entries hit on first turn
- Multi-turn context: 10+ turn conversation coherence validated
- Analyst suggestion: keyword heuristic + UI block + mode switch working
- Session isolation + history windowing (caps at 20) confirmed
- Memory persistence: append-only storage working correctly

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
| **7** | NL Deletion | ✅ SHIPPED |
| **8** | Pipeline Unification | 🔧 IN PROGRESS |
| **9** | Smart Tips (Memory → Card) | 🔲 PLANNED |

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
> **Current**: Phase 1 ✅ → Phase 2 🚧 → Phase 3 🚧 (Real implementations shipping ahead of UI)

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
| Scheduler Drawer (top gesture, carousels) | ✅ Logic + UI shipped (6 waves) |
| All 3 modes navigable with fake responses | 🔲 |
| UI matches prism-ui-ux-contract.md | 🔲 |

---

### Phase 3: Real Implementation (Swap Fakes) 🔲

**Goal**: Real LLM, real DB, real hardware integration

| Exit Criterion | Status |
|----------------|--------|
| DashScope API (Coach mode working) | ✅ `DashscopeExecutor` + `RealCoachPipeline` |
| Room persistence (Memory, Entity, UserHabit) | ✅ `RoomMemoryRepository`, `RoomEntityRepository`, `RoomUserHabitRepository` shipped |
| Tingwu integration (audio transcription) | ✅ `FunAsrService` shipped |
| ESP32 BLE (badge communication) | ✅ `RealConnectivityBridge` shipped |
| Memory Writer (fire-and-forget persistence) | ✅ `EntityWriter` shipped |
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

## OS Model Upgrade

> **North Star**: [`os-model-architecture.md`](../specs/os-model-architecture.md) — RAM/SSD mental model  
> **Strategy**: Spec-first, one wave per session, backward compatible  
> **Date**: 2026-02-10

| Wave | Focus | Spec | OS Layer | Status |
|------|-------|------|----------|--------|
| **0** | Create the RAM (SessionWorkingSet) | `session-context/spec.md` | Kernel | ✅ |
| **1** | Connect RL Module | `rl-module/spec.md` | RAM Application | ✅ |
| **2** | Connect EntityWriter | `entity-writer/spec.md` | RAM Application | 🔲 |
| **3** | Simplify Coach + Scheduler | `coach/spec.md` + `scheduler/spec.md` | — | 🔲 |
| **4** | CRM Hub → File Explorer | `client-profile-hub/spec.md` | File Explorer | 🔲 |

**Critical path**: W0 → W1 → W3 (fixes Coach Mode missing client habits bug)

**Supporting tasks** (do alongside Wave 0):
- [x] GLOSSARY.md: Add RAM, SSD, Kernel, Application, File Explorer, SessionWorkingSet
- [x] `memory-center/spec.md`: Add `> **OS Layer**: SSD Storage` header
- [x] `entity-registry/spec.md`: Add `> **OS Layer**: SSD Storage` header
- [x] `user-habit/spec.md`: Add `> **OS Layer**: SSD Storage` header

**Per-wave protocol**: Spec upgrade → `/cerb-check` → Review → Code (separate session) → `/acceptance-team`

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
| FTS4 Search | `MemoryDao.kt` | LIKE search for Chinese text; FTS4 needed for accuracy | Medium |
| Remaining Fakes | `FakeHistoryRepository`, `FakeAudioRepository` | Not yet backed by Room | Low (deferred) |
| TOCTOU in observe() | `RoomUserHabitRepository.kt` | Concurrent first-observation of same key may lose 1 count; add `@Transaction` if batching | Low |
| Room error handling | `RoomMemoryRepository`, `RoomEntityRepository`, `RoomUserHabitRepository` | No try-catch on write ops; uncaught SQLite exceptions possible | Low |
| ~~GIF Hardware Pipeline~~ | ~~`feature/media/Gif*.kt`~~ | ✅ Resolved (2026-02-08) — GIF files deleted, `sendGifCommand` removed from BleGateway, specs cleaned | ~~Medium~~ |
| Dead `httpChecker` field | `DefaultDeviceConnectionManager.kt` | Constructor param never used after HTTP gate removal. Remove field + DI provider | Low |

**Resolution**: Refactor to `FakeProgressService` / `FakeDelayService` post-beta.

---

> **Archived**: Phase 1/2 granular tracking moved to [`tracker-phase-1-granular.md`](../archived/tracker-phase-1-granular.md).
