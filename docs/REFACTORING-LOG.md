# Architecture Refactoring Log

> Detailed history of architecture refactoring waves. North Star is in [ArchitectureRefactoring.md](./ArchitectureRefactoring.md).

---

## Summary Metrics

| Metric | Original | Current | Change |
|--------|----------|---------|--------|
| HomeScreenViewModel | 3668 lines | 2505 lines | **-31.7%** |
| HomeOrchestratorImpl | 531 lines | 75 lines | **-85.9%** |
| HomeScreen.kt | 2547 lines | **1399 lines** | **-45.1%** |
| Domain classes | 4 | 14 | +10 |
| Unit tests | 0 | 218+ | New (+40) |
| V1 Modules | 0/8 | 8/8 | **100%** |

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
