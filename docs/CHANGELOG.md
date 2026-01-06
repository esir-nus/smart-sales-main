# Documentation Update Changelog

---

## V1.2.0 - 2026-01-05

### Summary
Implemented missing V1 sections that were planned but not yet present in spec.

### Changes to docs/Orchestrator-V1.md
- **Version**: Updated from 1.1.0 → 1.2.0
- **§4.1 Versioning Glossary**: Added definitions for `schemaVersion` vs `version`, migration policy
- **§8.1 Tingwu Runner Retry Policy**: Added retry rules (maxRetries=3, backoff, error categories, terminal behavior, partial failures)
- **§13/Appendix D State Recovery Rules**: Added recovery rules for PublisherState, TingwuJobState, DisectorPlan, M2B
- **§14 Future Extensions**: Renumbered from §13

### Cross-Reference Fix
- `api-contracts.md` §5.1 reference to "Section 8.1" now resolves

---

## 2025-01-27

### Summary
Updated documentation system to ensure V1 is CURRENT and consistent, V7 is ARCHIVED, and all contracts/examples are unambiguous and validate.

## Files Modified

### 1. docs/orchestrator-v1.schema.json
- **M2B Schema Unification**: Replaced generic `items[]` and `sourcePointers[]` with canonical `chapters[]` and `keyPoints[]` structure
- **New Definitions**: Added `M2BChapter` and `M2BKeyPoint` schema definitions with required source pointers
- **Schema Header Fix**: Updated `$comment` to use integer `schemaVersion: 1` instead of `SchemaVersion: 1.1.0` and changed idempotency key to use `schemaVersion` instead of `version`
- **M2B Structure**: `M2BTranscriptionState` now requires `chapters[]` and `keyPoints[]` arrays, each entry must include `audioAssetId` + `recordingSessionId` + anchor (chapterId and/or timeRange)

### 2. docs/orchestrator-v1.examples.json
- **M2B Example Update**: Rewrote `M2BTranscriptionState` example to match new schema structure
- **Structure Changes**: 
  - Chapters now have `audioAssetId` and `recordingSessionId` at top level (not nested in `source`)
  - KeyPoints use `timeRange` object instead of nested `source` object
  - Removed deprecated `source` nesting pattern

### 3. docs/Orchestrator-V1.md
- **Identifier Glossary** (§6.0): Added comprehensive table with all identifiers (chatSessionId, turnId, audioAssetId, recordingSessionId, disectorPlanId, batchAssetId, batchIndex, publishedPrefixBatchIndex, jobId)
- **M2B Section Update** (§6.2): Updated to describe canonical `chapters[]` + `keyPoints[]` structure with hard rules about source pointers
- **Tingwu Retry Policy** (§8.1): Added dedicated subsection specifying:
  - maxRetries = 3 (default)
  - Retryable errors (429, 5xx, timeouts) referencing api-contracts.md
  - Terminal behavior after retries exhausted
  - Partial failure handling (prefix stops at gap, later batches can run but not publish)
- **State Recovery Rules** (§12.1): Added section explaining recovery of PublisherState, TingwuJobState, SessionMemory, DisectorPlan, and partial failure impact
- **Versioning Glossary** (§4.1): Added section explaining `schemaVersion` (JSON schema versioning) vs artifact-specific versioning, migration strategy

### 4. docs/style-guide.md
- **V4 Reference Fix**: Updated line 428 to reference `docs/Orchestrator-V1.md` (CURRENT) instead of `docs/archived/Orchestrator-MetadataHub-V4.md`

## Archive Consistency Verification

### V7 Files Status
- ✅ `docs/archived/Orchestrator-MetadataHub-V7.md` - Already archived
- ✅ `docs/archived/metahub-schema-v7.json` - Already archived
- ✅ All references point to `docs/archived/` paths

### References Updated
- `docs/AGENTS.md`: Already references `docs/archived/Orchestrator-MetadataHub-V7.md` ✓
- `docs/T-Task.md`: Already references `docs/archived/Orchestrator-MetadataHub-V7.md` ✓
- `docs/style-guide.md`: Updated to remove V4 reference ✓

## Validation Results

### JSON Validation
- ✅ `docs/orchestrator-v1.schema.json` - Valid JSON
- ✅ `docs/orchestrator-v1.examples.json` - Valid JSON

### Schema-Examples Alignment
- ✅ M2B example structure matches new schema definition
- ✅ All required fields present in examples
- ✅ Source pointers enforced in schema and examples

## Key Changes Summary

1. **M2B Canonical Structure**: Adopted Option B (chapters[] + keyPoints[]) as ONLY canonical structure
2. **Tingwu Retry**: Added explicit terminal behavior specification
3. **Identifier Glossary**: Centralized all identifier definitions with lifecycle rules
4. **State Recovery**: Documented recovery rules for all stateful components
5. **Versioning Clarity**: Unified on `schemaVersion` (integer) with clear policy
6. **Archive Consistency**: Verified all V7 references point to archived/ paths

## Files NOT Modified (as requested)
- `docs/role-contract.md` - Unchanged
- `docs/orchestrator-sample-response.md` - Unchanged

---

## Architecture Refactoring Log

> Detailed history of architecture refactoring waves. North Star is in [ArchitectureRefactoring.md](./ArchitectureRefactoring.md).

### Summary Metrics

| Metric | Original | Current | Change |
|--------|----------|---------|--------|
| HomeScreenViewModel | 3668 lines | 2505 lines | **-31.7%** |
| HomeOrchestratorImpl | 531 lines | 75 lines | **-85.9%** |
| HomeScreen.kt | 2547 lines | **1490 lines** | **-41.5%** |
| Domain classes | 4 | 14 | +10 |
| Unit tests | 0 | 198+ | New (+20) |
| V1 Modules | 0/8 | 8/8 | **100%** |

---

### Wave 18: UI Component Extraction ✅ (2026-01-06)

**HomeScreen.kt Reduction (Target: <1500 lines)**
- Extract Debug HUD → `debug/DebugHud.kt` (399 lines): 2221 → 1873 (-348)
- Extract HistoryDrawer → `history/HistoryDrawer.kt` (175 lines): 1874 → 1746 (-128)
- Extract HomeInputArea → `input/HomeInputArea.kt` (326 lines): 1746 → 1490 (-256)
- **Total reduction**: 2221 → 1490 lines (**-731 lines, -32.9%**)
- **Target achieved**: <1500 lines ✅

---

### Wave 17 / M3: Full V1 Alignment ✅ (2026-01-06)

**Verification milestone (no code changes)**
- All 8 V1 modules verified as implemented
- TranscriptPublisher: `ChatMessageBuilder` (domain) + `TranscriptPublisherUseCase` (data)
- PlanReview workflow created for evidence-based planning

---

### Wave 16 / M2: ViewModel Purity ✅ (2026-01-06)

**M2.1: UI Models Extraction**
- Created `HomeUiModels.kt` (59 lines): ChatMessageUi, QuickSkillUi, DeviceSnapshotUi, AudioSummaryUi
- HomeScreenViewModel: 2553 → 2505 lines (-1.9%)

**M2.2: Delegation Verification**
- Session history → `SessionsManager` (verified complete)
- Transcription → `TranscriptionCoordinator` (verified complete)
- Streaming → `ChatStreamCoordinator` (verified complete)

---

### Wave 15 / M1: Domain Completeness ✅ (2026-01-06)

**M1.1: SmartAnalysisParser Extraction**
- Created `SmartAnalysisParser.kt` (548 lines) in domain layer
- Extracted from `HomeOrchestratorImpl`: parsing, markdown generation, post-processing
- Orchestrator reduced from 531 → 75 lines (**-85.9%**)

**M1.2: Domain Test Coverage**
- Created `SmartAnalysisParserTest.kt` (16 tests): JSON parsing, POV normalization, post-processing
- Created `DisectorUseCaseTest.kt` (10 tests): V1 Appendix A batch splitting, 10s overlap

### Phase 1: RealTingwuCoordinator Split ✅
- Extracted `TingwuRunnerRepository`, `TranscriptPublisherUseCase`

### Hilt DI Fix ✅
- Converted 4 ViewModels to domain coordinators

### Wave 1: ChatMessageBuilder ✅
- Extracted 3 pure helper functions (~65 lines)

### Wave 2: InputClassifier ✅
- Extracted 5 classification functions (~145 lines)

### Wave 3: ChatPublisher ✅
- Created V1-aligned `ChatPublisher.kt` (~140 lines)
- Removed 700 lines of legacy heuristic sanitizer

### Wave 4: MetadataParser ✅
- Extracted pure JSON parsing (~100 lines)

### Wave 5: Typed Error Model ✅
- Created `ChatError.kt` sealed class (~60 lines)

### Wave 6: HUD Delegation Cleanup ✅
- Removed wrapper function (~14 lines)

### Wave 8A: Extraction Consolidation ✅
- Consolidated `extractMetadataJson` and `extractChannels` into `ChatPublisher`

### Wave 9: Display Resolution Extraction ✅
- Added `resolveDisplayText` to `ChatPublisher`

### Wave 10: Voiceprint Removal ✅
- Removed 4 deprecated voiceprint functions (~138 lines)
- Deleted `VoiceprintLabPanel` component (~327 lines)

### Wave 11: Dead Code Removal ✅
- Removed 3 unused functions (-56 lines)

### Wave 12: Stale Comment Cleanup ✅
- Removed 24 "moved to" comments

### Wave 13: Blank Line Compression ✅
- Removed 27 consecutive blank lines

### Wave 14: Duplicate Header Cleanup ✅
- Removed duplicate file header and stale comments (-8 lines)

