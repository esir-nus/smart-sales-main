# Smart Sales Prism Tracker

> **SOT**: [`Prism-V1.md`](../specs/Prism-V1.md) · [`prism-ui-ux-contract.md`](../specs/prism-ui-ux-contract.md) (INDEX) · [`GLOSSARY.md`](../specs/GLOSSARY.md)  
> **Last Updated**: 2026-02-01

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
| **Hot Zone** | 🔲 | `MemoryEntryEntity` (§5.7) |
| **Cement Zone** | 🔲 | Archived entries (§5.1) |
| **Relevancy Library** | 🔲 | `RelevancyEntry` (§5.2) |
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

> **Cerb Docs**: [`spec.md`](../cerb/memory-center/spec.md) · [`interface.md`](../cerb/memory-center/interface.md)  
> **Strategy**: MVP-first, each wave ships independently

| Wave | Focus | Status |
|------|-------|--------|
| **1** | ScheduleBoard + Two-Phase Pipeline | ✅ Complete |
| **2** | Entity Resolution ("张总" → picker) | 🔲 |
| **3** | Location Conflict (room double-booking) | 🔲 |
| **4** | Reinforcement Learning (picker improves) | 🔲 |
| **5** | Hot/Cement Zones (30-day simulation) | 🔲 |
| **6** | User Habit Learning (behavioral nudges) | 🔲 |

### Wave 1: ScheduleBoard ✅

**Shipped**: 2026-02-03

| Deliverable | File | Status |
|-------------|------|--------|
| Interface | `domain/memory/ScheduleBoard.kt` | ✅ |
| Model | `domain/memory/ScheduleItem.kt` | ✅ |
| Implementation | `data/memory/RealScheduleBoard.kt` | ✅ |
| Tests | `ScheduleBoardTest.kt` (6 tests) | ✅ |
| Cerb Interface | `interface.md` updated | ✅ |
| ViewModel Wiring | `SchedulerViewModel.kt` → conflict check | ✅ |
| UI Feedback | `SchedulerDrawer.kt` → warning banner | ✅ |

**Test Cases Covered**:
- [x] No conflict: Empty schedule
- [x] No conflict: Adjacent (2-3pm, 3-4pm)
- [x] Conflict: Overlap (2-4pm vs 3-5pm)
- [x] Conflict: Contained (2-5pm contains 3-4pm)
- [x] COEXISTING tasks don't conflict
- [x] Duration inferred from task type

### Wave 2: Entity Resolution 🔲

**Next Step**: Implement EntityResolver interface for "张总" → contact picker

| Deliverable | File | Status |
|-------------|------|--------|
| Interface | `EntityResolver.kt` | 🔲 |
| Implementation | `RealEntityResolver.kt` | 🔲 |
| Tests | `EntityResolverTest.kt` | 🔲 |

**Test Cases**:
- [ ] One match → Auto-resolve
- [ ] Multiple → Picker
- [ ] Zero → NotFound

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

### Phase 1: Skeleton (Interfaces + Fakes)
**Goal**: Production-ready interfaces that evolve with implementation. Fakes prove wiring.

> **Mapping Table**: [`prism-interface-mapping.md`](../reference/prism-interface-mapping.md) — Bidirectional sync with spec

---

#### 1.1 Core Pipeline Interfaces (§2.2)

| Interface | Data Class | Fake | Spec |
|-----------|------------|------|------|
| `Orchestrator` | `Mode`, `OrchestratorResult` | `FakeOrchestrator` | §2.2 #2 |
| `ContextBuilder` | `EnhancedContext` | `FakeContextBuilder` | §2.2 #1 |
| `Executor` | `ExecutorResult` | `FakeExecutor` | §2.2 #3 |
| `ModePublisher` | `UiState` | `FakeChatPublisher`, `FakeAnalystPublisher`, `FakeSchedulePublisher` | §2.2 #4 |
| `MemoryWriter` | — | `FakeMemoryWriter` | §2.2 #5 |
| `SessionCache` | `SessionCacheSnapshot` | `FakeSessionCache` | §2.2 #1b |
| `MemoryCenterNotifier` | `MemoryNotification` | `FakeMemoryCenterNotifier` | §2.2 #7 |
| `Planner` | `ExecutionPlan` | `FakePlanner` | §4.5-4.6 |

- [x] All 8 interfaces defined
- [x] All 8 Fakes implemented
- [ ] All data classes implemented

---

#### 1.2 Input Tool Interfaces (§2.2 #1)

| Interface | Data Class | Fake | Real (Phase 3) |
|-----------|------------|------|----------------|
| `TingwuRunner` | `TranscriptBlock` | `FakeTingwuRunner` | `AliyunTingwuRunner` |
| `VisionAnalyzer` | `VisionResult` | `FakeVisionAnalyzer` | `QwenVLAnalyzer` |
| `UrlFetcher` | `UrlContent` | `FakeUrlFetcher` | `OkHttpUrlFetcher` |
| `BleConnector` | `BleFileInfo`, `BleEvent` | `FakeBleConnector` | `Esp32BleConnector` |

- [x] All 4 interfaces defined
- [x] All 4 Fakes implemented
- [x] All data classes implemented

---

#### 1.3 Linter Interfaces (§2.2 #6)

| Linter | Validates | Data Class |
|--------|-----------|------------|
| `EntityLinter` | Structured entity JSON | `ExtractedEntity` |
| `PlanLinter` | Execution plan | `ExecutionPlan` |
| `SchedulerLinter` | Scheduler commands | `SchedulerCommand` |
| `RelevancyLinter` | Relevancy updates | `RelevancyEntry` |

- [x] `Linter` sealed interface
- [ ] `LintResult`, `LintError` data classes
- [x] All 4 linter implementations (return Pass in Phase 1)

---

#### 1.4 Repository Interfaces

| Repository | Entity | Fake | Real (Phase 3) |
|------------|--------|------|----------------|
| `RelevancyRepository` | `RelevancyEntry` | `FakeRelevancyRepository` | `RoomRelevancyRepository` |
| `MemoryEntryRepository` | `MemoryEntryEntity` | `FakeMemoryEntryRepository` | `RoomMemoryEntryRepository` |
| `UserProfileRepository` | `UserProfile` | `FakeUserProfileRepository` | `RoomUserProfileRepository` |
| `UserHabitRepository` | `UserHabit` | `FakeUserHabitRepository` | `RoomUserHabitRepository` |
| `ArtifactRepository` | `ArtifactMeta` | `FakeArtifactRepository` | `FileSystemArtifactRepository` |
| `SessionsRepository` | `Session` | `FakeSessionsRepository` | `RoomSessionsRepository` |
| `ScheduledTaskRepository` | `ScheduledTask` | `FakeScheduledTaskRepository` | `RoomScheduledTaskRepository` |
| `InspirationRepository` | `Inspiration` | `FakeInspirationRepository` | `RoomInspirationRepository` |

- [x] All 8 repository interfaces defined
- [x] All 8 Fakes implemented
- [x] All 8 Real Room implementations created (Chunk B)
- [x] Build fixed — entities aligned with domain contracts
- [x] Core Pipeline implementations (Chunk C): ContextBuilder, Executor, Orchestrator, MemoryWriter, SessionCache
- [x] Input Tools implementations (Chunk D): OkHttpUrlFetcher, QwenVLAnalyzer, DashscopeTingwuRunner
- [x] Mode Publishers (Chunk E): ChatPublisher, AnalystPublisher, SchedulePublisher
- [x] Linters + Notifier (Chunk F): EntityLinter, PlanLinter, SchedulerLinter, RelevancyLinter, MemoryCenterNotifier
- [x] UI Layer (Chunk G): PrismViewModel, ThinkingBox, PlanCard, ConflictResolver, ResponseBubble, MinimalChatScreen

---

#### 1.5 Domain Models (Data Classes)

**Core Pipeline:**
- [x] `EnhancedContext` — Full context payload with userProfile, habits, sessionCache
- [x] `ExecutionPlan` — Planner output with retrievalScope, tools, deliverables
- [x] `ExecutorResult` — LLM response with displayContent, structuredJson
- [x] `UiState` — Sealed class (Idle, Loading, Thinking, Streaming, Response, PlanCard, Error)
- [x] `Mode` — Enum (COACH, ANALYST, SCHEDULER)

**Memory System (§5):**
- [x] `RelevancyEntry` — Entity context with aliases, demeanor, metrics
- [x] `AliasMapping` — Disambiguation with confirmationCount, recency
- [ ] `EntryRef` — Memory pointer
- [x] `DecisionRecord` — Rethink history
- [x] `ExtractedEntity` — LLM structured output
- [x] `MemoryEntryEntity` — Room entity with payloadJson
- [x] `UserProfile` — Room entity (§5.8)
- [x] `UserHabit` — Room entity (§5.9)
- [x] `ArtifactMeta` — File reference with type, path, mimeType

**Workflow Payloads (§5.7):**
- [x] `ChatMessage` — Coach bubble model
- [x] `CoachPayload` — messages, topic
- [x] `AnalysisChapter` — Analyst chapter model
- [x] `AnalystPayload` — chapters, keyInsights
- [x] `ScheduledTask` — Task with alarm, priority, status
- [x] `SchedulerPayload` — scheduledAt, priority, status
- [x] `Inspiration` — Idea card model
- [x] `ConflictCard` — Rethink UI model

**Input Types:**
- [x] `TranscriptBlock` — Tingwu output
- [x] `VisionResult` — Qwen-VL output
- [x] `UrlContent` — Fetched web content
- [x] `BleFileInfo`, `BleEvent` — ESP32 models

---

#### 1.6 UI State Models (prism-ui-ux-contract.md)

| Component | State Class | States |
|-----------|-------------|--------|
| Home | `SessionListState` | Empty, Loaded, Error |
| Knot FAB | `KnotFabState` | Idle, Tapped, Thinking, Tip |
| Audio Drawer | `AudioDrawerState` | Syncing, Cards, Error |
| Audio Card | `AudioCardState` | NotTranscribed, Transcribing, Transcribed, Error |
| Chat | `ChatState` | Coach, Analyst, Switching |
| Thinking Box | `ThinkingBoxState` | Hidden, Folded, Unfolded, Complete |
| Plan Card | `PlanCardState` | Visible, Executing, Complete |
| Disambiguation | `DisambiguationState` | AutoResolved, Picker |
| Scheduler Drawer | `SchedulerDrawerState` | Syncing, Cards, Error |
| Task Card | `TaskCardState` | Pending, Active, Alarm, Done, Conflict |
| Inspiration Card | `InspirationCardState` | Normal, Selected, MultiSelect |
| Conflict Card | `ConflictCardState` | Visible, Selected, Confirming |

- [ ] All 12 UI state classes defined

---

#### 1.7 DI Wiring

- [ ] `FakeCoreModule` — Hilt @Binds for all Fakes
- [ ] Build variant toggle (Fake vs Real via flavors)
- [ ] `RealCoreModule` (placeholder, swapped in Phase 3)
- [x] `:data:prism-lib` module created (Phase 2)

---

#### 1.8 Verification

- [x] Integration test: ViewModel → FakeOrchestrator → UiState (5 tests passed)
- [x] Contamination check: `grep -rn "android\." domain/prism-core/` returns empty
- [x] All interfaces compile: `./gradlew :domain:prism-core:compileDebugKotlin`
- [x] App assembles: `./gradlew :app:assembleDebug` ✅

---

#### Phase 1 Counts

| Category | Interfaces | Data Classes | Fakes | Total |
|----------|------------|--------------|-------|-------|
| Core Pipeline | 8 | 5 | 8 | 21 |
| Input Tools | 4 | 5 | 4 | 13 |
| Linters | 5 | 2 | 4 | 11 |
| Repositories | 8 | 8 | 8 | 24 |
| Workflow Payloads | 0 | 8 | 0 | 8 |
| UI States | 0 | 12 | 0 | 12 |
| **TOTAL** | **25** | **40** | **24** | **89** |

### Phase 2: UX Layer (Page-by-Page + Fake Chaining)
**Goal**: Visual & interaction completeness. No real backend.

#### P1: Home Screen
- [ ] Session List UI (Group by date)
- [ ] Knot FAB (Idle/Thinking/Tip states)
- [ ] Empty State Hero

#### P2: Audio Drawer
- [ ] Bottom-up gesture & card layout
- [ ] Transcription states (Shimmer → Progress → Text)
- [ ] Sync flow (Pull-to-refresh simulation)

#### P3: Chat Interface
- [ ] Bubbles & Mode Toggle (Coach/Analyst)
- [ ] Thinking Box (Fold/Unfold)
- [ ] **FIX**: Plan Card execution + `complete` state (Strikethrough)
- [ ] **FIX**: Entity Disambiguation Picker (State A / State B)

#### P4: Scheduler Drawer
- [ ] Top-down gesture & Carousels
- [ ] Task/Inspiration/Conflict Cards
- [ ] **FIX**: Smart Alarm Badge (`[⏰ 智能提醒]` vs manual)
- [ ] **FIX**: Fake MODIFY intent flow (Voice correction scenario)

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
