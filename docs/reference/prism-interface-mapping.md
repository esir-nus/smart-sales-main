# Prism Spec-to-Interface Mapping

> **Purpose**: Bidirectional sync table between Prism-V1.md and codebase.
> **Usage**: When spec changes → update interfaces. When interfaces change → update spec.
> **Last Synced**: 2026-01-26

---

## Legend

| Symbol | Meaning |
|--------|---------|
| 🔲 | Not started |
| 🏗️ | Interface defined (Fake exists) |
| ✅ | Real implementation complete |
| 📍 | Spec section reference |

---

## Core Pipeline (§2.2)

| Spec Section | Interface | Data Class | Repository | Fake | Real | Status |
|--------------|-----------|------------|------------|------|------|--------|
| §2.2 #1 | `ContextBuilder` | `EnhancedContext` | — | `FakeContextBuilder` | `RealContextBuilder` | 🏗️ |
| §2.2 #1b | `SessionCache` | `SessionCacheSnapshot` | — | `FakeSessionCache` | `RoomSessionCache` | 🏗️ |
| §2.2 #2 | `Orchestrator` | `Mode` | — | `FakeOrchestrator` | `PrismOrchestrator` | 🏗️ |
| §2.2 #3 | `Executor` | `ExecutorResult` | — | `FakeExecutor` | `DashscopeExecutor` | 🏗️ |
| §2.2 #4 | `ModePublisher` | `UiState` | — | `FakeModePublisher` | `ChatPublisher`, `AnalystPublisher`, `SchedulePublisher` | 🏗️ |
| §2.2 #5 | `MemoryWriter` | — | `MemoryEntryRepository` | `FakeMemoryWriter` | `RoomMemoryWriter` | 🏗️ |
| §2.2 #6 | `Linter` (sealed) | `LintResult`, `LintError` | — | `EntityLinter`, `PlanLinter`, `SchedulerLinter`, `RelevancyLinter` | (Real = same classes) | 🏗️ |
| §2.2 #7 | `MemoryCenterNotifier` | `MemoryNotification` | — | `FakeMemoryCenterNotifier` | `SnackbarMemoryCenterNotifier` | 🏗️ |

---

## Input Tools (§2.2 #1)

| Spec Section | Interface | Data Class | Repository | Fake | Real | Status |
|--------------|-----------|------------|------------|------|------|--------|
| §2.2 #1 | `TingwuRunner` | `TranscriptBlock` | — | `FakeTingwuRunner` | `AliyunTingwuRunner` | 🏗️ |
| §2.2 #1 | `VisionAnalyzer` | `VisionResult` | — | `FakeVisionAnalyzer` | `QwenVLAnalyzer` | 🏗️ |
| §2.2 #1 | `UrlFetcher` | `UrlContent` | — | `FakeUrlFetcher` | `OkHttpUrlFetcher` | 🏗️ |
| ESP32 Protocol | `BleConnector` | `BleFileInfo` | — | `FakeBleConnector` | `Esp32BleConnector` | 🏗️ |

---

## Mode Pipelines (§4)

| Spec Section | Interface | Data Class | Repository | Fake | Real | Status |
|--------------|-----------|------------|------------|------|------|--------|
| §4.1 Coach | — (uses `Executor`) | `ChatMessage` | — | — | — | 🏗️ |
| §4.5 Analyst | `Planner` | `ExecutionPlan` | — | `FakePlanner` | `DashscopePlanner` | 🏗️ |
| §4.6 Plan Card | — | `PlanCardState` | — | — | — | 🔲 |
| §4.3 Scheduler | — | `SchedulerCommand` | `ScheduledTaskRepository`, `InspirationRepository` | `FakeScheduledTaskRepository`, `FakeInspirationRepository` | `RoomScheduledTaskRepository`, `RoomInspirationRepository` | 🏗️ |
| §4.7 Conflict | — | `ConflictCard` | — | — | — | 🏗️ |

---

## Memory System (§5)

| Spec Section | Interface | Data Class | Repository | Fake | Real | Status |
|--------------|-----------|------------|------------|------|------|--------|
| §5.1 Hot Zone | — | `MemoryEntryEntity` | `MemoryEntryRepository` | `FakeMemoryEntryRepository` | `RoomMemoryEntryRepository` | 🏗️ |
| §5.2 Relevancy | — | `RelevancyEntry` | `RelevancyRepository` | `FakeRelevancyRepository` | `RoomRelevancyRepository` | 🏗️ |
| §5.4 Alias | — | `AliasMapping` | — | — | — | 🏗️ |
| §5.5 Supporting | — | `EntryRef`, `DecisionRecord` | — | — | — | 🔲 |
| §5.6 Structured | — | `ExtractedEntity` | — | — | — | 🏗️ |
| §5.7 MemoryEntry | — | `MemoryEntryEntity` | `MemoryEntryRepository` | — | — | 🏗️ |
| §5.7 Artifacts | — | `ArtifactMeta` | `ArtifactRepository` | `FakeArtifactRepository` | `FileSystemArtifactRepository` | 🏗️ |
| §5.8 UserProfile | — | `UserProfile` | `UserProfileRepository` | `FakeUserProfileRepository` | `RoomUserProfileRepository` | 🏗️ |
| §5.9 UserHabit | — | `UserHabit` | `UserHabitRepository` | `FakeUserHabitRepository` | `RoomUserHabitRepository` | 🏗️ |

---

## Workflow Payloads (§5.7)

| Workflow | Data Class | Spec Reference | Status |
|----------|------------|----------------|--------|
| **Coach** | `ChatMessage`, `CoachPayload` | §5.7 payloadJson | 🏗️ |
| **Analyst** | `AnalysisChapter`, `AnalystPayload` | §5.7 payloadJson | 🏗️ |
| **Scheduler** | `ScheduledTask`, `SchedulerPayload` | §5.7 payloadJson | 🏗️ |

---

## UI States (prism-ui-ux-contract.md)

| UX Component | UI State | Data Class | Spec Reference | Status |
|--------------|----------|------------|----------------|--------|
| **Home** | Empty, Loaded, Error | `SessionListState` | §1.1 | 🔲 |
| **Knot FAB** | Idle, Tapped, Thinking, Tip | `KnotFabState` | §1.2 | 🔲 |
| **Audio Drawer** | Syncing, Cards, Error | `AudioDrawerState` | §1.3 | 🔲 |
| **Audio Card** | NotTranscribed, Transcribing, Transcribed, Error | `AudioCardState` | §1.4 | 🔲 |
| **Chat** | Coach, Analyst, Switching | `ChatState` | §2.1 | 🔲 |
| **Thinking Box** | Hidden, Folded, Unfolded, Complete | `ThinkingBoxState` | §2.2 | 🔲 |
| **Plan Card** | Visible, Executing, Complete | `PlanCardState` | §2.3 | 🔲 |
| **Disambiguation** | AutoResolved, Picker | `DisambiguationState` | §3.16 | 🔲 |
| **Scheduler Drawer** | Syncing, Cards, Error | `SchedulerDrawerState` | §1.5 | 🔲 |
| **Task Card** | Pending, Active, Alarm, Done, Conflict | `TaskCardState` | §3.5 | 🔲 |
| **Inspiration Card** | Normal, Selected, MultiSelect | `InspirationCardState` | §3.2 | 🔲 |
| **Conflict Card** | Visible, Selected, Confirming | `ConflictCardState` | §3.4 | 🔲 |

---

## Sync Protocol

### Spec → Code (When spec changes)

1. Update this mapping table first
2. Add/modify interface in `domain/`
3. Add/modify Fake implementation
4. Update tracker.md checklist
5. Run contamination check: `grep -rn "android\." domain/`

### Code → Spec (When implementation reveals gaps)

1. Log the gap in this table (add row with ⚠️ marker)
2. Open PR/issue for spec update
3. After spec update, change ⚠️ → 🔲

---

## Counts (Auto-Update)

| Category | Interfaces | Data Classes | Repositories | Total Items |
|----------|------------|--------------|--------------|-------------|
| Core Pipeline | 8 | 5 | 1 | 14 |
| Input Tools | 4 | 4 | 0 | 8 |
| Mode Pipelines | 1 | 4 | 2 | 7 |
| Memory System | 0 | 8 | 5 | 13 |
| Workflow Payloads | 0 | 6 | 0 | 6 |
| UI States | 0 | 12 | 0 | 12 |
| **TOTAL** | **13** | **39** | **8** | **60** |
