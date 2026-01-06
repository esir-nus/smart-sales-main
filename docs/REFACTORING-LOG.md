# Architecture Refactoring Log

> Detailed history of architecture refactoring waves. North Star is in [ArchitectureRefactoring.md](./ArchitectureRefactoring.md).

---

## Summary Metrics

| Metric | Original | Current | Change |
|--------|----------|---------|--------|
| HomeScreenViewModel | 3668 lines | 2524 lines | **-31.2%** |
| HomeOrchestratorImpl | 531 lines | 75 lines | **-85.9%** |
| HomeScreen.kt | 2547 lines | **1318 lines** | **-48.3%** |
| Domain classes | 4 | 14 | +10 |
| Unit tests | 0 | 223+ | New (+45) |
| V1 Modules | 0/8 | 8/8 | **100%** |

---

## Wave 24 / P3.6: History Package Consolidation ✅ (2026-01-06)

**Package Reorganization for History Components**

**Changes:**
- Moved `HistoryDrawer.kt` from `home/history/` → `history/` package
- Updated package declarations and imports
- **Note:** SessionsManager already exists as Coordinator pattern (M4 verified)

**Rationale:**
- Research revealed most extraction work already complete
- HistoryDrawer: 176 lines, already extracted
- SessionsManager: Domain Coordinator with 10+ unit tests
- Creating HistoryReducer would be over-engineering for CRUD operations

**Impact:**
- Cleaner package structure
- History components properly isolated
- **No line count change** (reorganization only)
- Coordinator pattern validated as portable alternative to Reducer

---

## Wave 23 / P3.5: ConversationScreen Extraction ✅ (2026-01-06)

**Completed SendMessage Wiring + UI Extraction**

**Phase 1: SendMessage→Reducer Wiring**
- Added `onMessagesChanged` callback to `ConversationViewModel` for state sync
- Updated `HomeScreenViewModel.onSendMessage` to dispatch through `ConversationReducer`
- Preserved SmartAnalysis, export skills, and low-info guards in ViewModel (deferred to P3.8)
- Normal chat messages now flow through portable Reducer pattern

**Phase 2: UI Extraction**
- Created `ConversationScreen.kt` (~120 lines) - message list + typing indicator
- Extracted `AssistantTypingBubble.kt` to messages/ package (~50 lines)
- Updated `HomeScreen.kt` to delegate conversation rendering (-80 lines)

**Impact:**
- HomeScreenViewModel: 2505 → 2524 lines (+19, wiring code)
- HomeScreen.kt: 1399 → 1318 lines (-81)
- **First isolated screen component** in M5 Navigation & Isolation phase
- Normal chat flow now portable (SmartAnalysis deferred)

---

## Wave 22 / P3.7: HomeScreen Consolidation ✅ (2026-01-06)

**Eliminated Dual HomeScreen Implementations**

**Problem:**
- Two parallel HomeScreen implementations with divergent features
- `MainScreen.kt` (bottom nav) → `app/.../HomeScreen.kt` (585 lines, simpler UI)
- `AiFeatureTestActivity.kt` (overlay UX) → `feature/chat/.../HomeScreenRoute()` (1400 lines, full-featured)

**Solution:**
- Deleted `app/.../screens/home/HomeScreen.kt` (585 lines)
- Updated `MainScreen.kt` to use feature module's `HomeScreenRoute`
- Fixed navigation callbacks: `onNavigateToHistory` → `onNavigateToChatHistory`
- Removed `deviceManagerViewModel` parameter (feature module manages internally)

**Impact:**
- **Single source of truth** for home screen behavior
- Elimina future drift between implementations
- **-585 lines** of duplicated code

---

---

## Wave 21 / M4 Complete: Portable Reducers ✅ (2026-01-06)

**M4 Milestone Complete** - Business logic is platform-agnostic

**Pattern Validation:**
- ConversationReducer: Pure `reduce()` pattern + streaming (P3.1.B1 + B2 partial)
- SessionsManager: Coordinator pattern with StateFlow (169 lines, pre-existing)
- TranscriptionCoordinator: Coordinator pattern (domain/transcription, pre-existing)

**Key Recognition:**
- P3.2 HistoryReducer was already complete via `SessionsManager`
- P3.3 TranscriptionReducer was already complete via `TranscriptionCoordinator`
- Mixed patterns (Reducer + Coordinator) both achieve portability goal

**Architecture Decision:**
- Don't rewrite SessionsManager to pure Reducer - working code,  tested, portable
- ConversationReducer establishes best practice for **new** features
- Existing Coordinators are acceptable debt

---

---

## Wave 20 / P3.1.B1: ConversationViewModel Wiring ✅ (2026-01-06)

**Portable Reducer Pattern Established**
- Created `ConversationViewModel` (regular class, injects HomeOrchestrator)
- Wired into `HomeScreenViewModel` as state delegate
- `onInputChanged` → dispatches to `ConversationReducer`
- State sync: `ConversationViewModel.state` → `HomeUiState.inputText`

**Test Coverage**
- `ConversationReducerTest`: 14 passing tests (Phase A)
- `ConversationViewModelTest`: 5 passing tests (Phase B1)
- **Total**: 19 tests, 100% pass rate ✅

**Architectural Notes**
- ConversationViewModel is NOT `@HiltViewModel` (Hilt prohibits ViewModel-to-ViewModel injection)
- Streaming side effects deferred to P3.1.B2
- First reducer pattern in codebase - blueprint for HistoryReducer, TranscriptionReducer

---

## Wave 19: UI Component Extraction & Test Fixes ✅ (2026-01-06)

**MessageBubble Extraction**
- Extract MessageBubble → `messages/MessageBubble.kt` (132 lines): 1490 → 1399 (-91)
- Isolated bubble rendering for future UX iteration
- **Cumulative HomeScreen.kt reduction**: 2221 → 1399 lines (**-822 lines, -37%**)

**Test Fixes**
- Fixed `SessionsManagerTest`: FakeMetaHub now stores sessions (extension function works)
- Fixed `TranscriptionCoordinatorTest`: UnconfinedTestDispatcher, null jobId, async/launch patterns
- **All tests passing**: 20/20 ✅ (was 14/20)

---

## Wave 18: UI Component Extraction ✅ (2026-01-06)

**HomeScreen.kt Reduction (Target: <1500 lines)**
- Extract Debug HUD → `debug/DebugHud.kt` (399 lines): 2221 → 1873 (-348)
- Extract HistoryDrawer → `history/HistoryDrawer.kt` (175 lines): 1874 → 1746 (-128)
- Extract HomeInputArea → `input/HomeInputArea.kt` (326 lines): 1746 → 1490 (-256)
- **Total reduction**: 2221 → 1490 lines (**-731 lines, -32.9%**)
- **Target achieved**: <1500 lines ✅

---

## Wave 17 / M3: Full V1 Alignment ✅ (2026-01-06)

**Verification milestone (no code changes)**
- All 8 V1 modules verified as implemented
- TranscriptPublisher: `ChatMessageBuilder` (domain) + `TranscriptPublisherUseCase` (data)
- PlanReview workflow created for evidence-based planning

---

## Wave 16 / M2: ViewModel Purity ✅ (2026-01-06)

**M2.1: UI Models Extraction**
- Created `HomeUiModels.kt` (59 lines): ChatMessageUi, QuickSkillUi, DeviceSnapshotUi, AudioSummaryUi
- HomeScreenViewModel: 2553 → 2505 lines (-1.9%)

**M2.2: Delegation Verification**
- Session history → `SessionsManager` (verified complete)
- Transcription → `TranscriptionCoordinator` (verified complete)
- Streaming → `ChatStreamCoordinator` (verified complete)

---

## Wave 15 / M1: Domain Completeness ✅ (2026-01-06)

**M1.1: SmartAnalysisParser Extraction**
- Created `SmartAnalysisParser.kt` (548 lines) in domain layer
- Extracted from `HomeOrchestratorImpl`: parsing, markdown generation, post-processing
- Orchestrator reduced from 531 → 75 lines (**-85.9%**)

**M1.2: Domain Test Coverage**
- Created `SmartAnalysisParserTest.kt` (16 tests): JSON parsing, POV normalization, post-processing
- Created `DisectorUseCaseTest.kt` (10 tests): V1 Appendix A batch splitting, 10s overlap

---

## Phase 1: RealTingwuCoordinator Split ✅
- Extracted `TingwuRunnerRepository`, `TranscriptPublisherUseCase`

## Hilt DI Fix ✅
- Converted 4 ViewModels to domain coordinators

## Wave 1: ChatMessageBuilder ✅
- Extracted 3 pure helper functions (~65 lines)

## Wave 2: InputClassifier ✅
- Extracted 5 classification functions (~145 lines)

## Wave 3: ChatPublisher ✅
- Created V1-aligned `ChatPublisher.kt` (~140 lines)
- Removed 700 lines of legacy heuristic sanitizer

## Wave 4: MetadataParser ✅
- Extracted pure JSON parsing (~100 lines)

## Wave 5: Typed Error Model ✅
- Created `ChatError.kt` sealed class (~60 lines)

## Wave 6: HUD Delegation Cleanup ✅
- Removed wrapper function (~14 lines)

## Wave 8A: Extraction Consolidation ✅
- Consolidated `extractMetadataJson` and `extractChannels` into `ChatPublisher`

## Wave 9: Display Resolution Extraction ✅
- Added `resolveDisplayText` to `ChatPublisher`

## Wave 10: Voiceprint Removal ✅
- Removed 4 deprecated voiceprint functions (~138 lines)
- Deleted `VoiceprintLabPanel` component (~327 lines)

## Wave 11: Dead Code Removal ✅
- Removed 3 unused functions (-56 lines)

## Wave 12: Stale Comment Cleanup ✅
- Removed 24 "moved to" comments

## Wave 13: Blank Line Compression ✅
- Removed 27 consecutive blank lines

## Wave 14: Duplicate Header Cleanup ✅
- Removed duplicate file header and stale comments (-8 lines)
