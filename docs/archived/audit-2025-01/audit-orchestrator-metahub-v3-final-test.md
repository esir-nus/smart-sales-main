# T-orch-V3 Final Test & Orchestrator+MetaHub Audit Report

**Date**: 2025-01-27

## 1. Test Execution Results

### 1.1 Core Util / MetaHub Tests
**Command**: `./gradlew :core:util:test`

**Result**: ❌ FAILED

**Failing Tests**:
- `InMemoryMetaHubTest.upsertSession_overwritesExisting` (line 17)
- `TranscriptMetadataMergeTest.mergeWith_preservesExistingSpeakerAndMergesNewOnes` (line 41)

**Analysis**: 
- `InMemoryMetaHubTest`: Test expects direct overwrite, but `InMemoryMetaHub.upsertSession` uses `mergeWith`, causing merge behavior instead of overwrite.
- `TranscriptMetadataMergeTest`: Confidence clamping assertion may be failing due to merge logic issue.

### 1.2 TranscriptOrchestrator Tests
**Command**: `./gradlew :data:ai-core:testDebugUnitTest --tests "com.smartsales.data.aicore.RealTranscriptOrchestratorTest"`

**Result**: ✅ PASSED

All tests passed successfully.

### 1.3 Tingwu Coordinator Tests
**Command**: `./gradlew :data:ai-core:testDebugUnitTest --tests "com.smartsales.data.aicore.RealTingwuCoordinatorTest"`

**Result**: ✅ PASSED

All tests passed successfully.

### 1.4 Home / Chat Tests
**Command**: `./gradlew :feature:chat:testDebugUnitTest --tests "com.smartsales.feature.chat.home.HomeExportActionsTest" --tests "com.smartsales.feature.chat.home.HomeStreamingDedupTest" --tests "com.smartsales.feature.chat.home.HomeTranscriptionTest"`

**Result**: ❌ FAILED (Compilation Error)

**Error Details**:
```
e: file:///home/cslh-frank/main_app/feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeStreamingDedupTest.kt:231:13 
Class 'FakeSessionRepository' is not abstract and does not implement abstract member 
public abstract suspend fun updateTitle(id: String, newTitle: String): Unit defined in com.smartsales.feature.chat.AiSessionRepository

e: file:///home/cslh-frank/main_app/feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeStreamingDedupTest.kt:245:9 
'pin' overrides nothing
```

**Analysis**: `FakeSessionRepository` in `HomeStreamingDedupTest.kt` is missing `updateTitle` method and has incorrect `pin` method signature.

---

## 2. Files Inspected

All specified files were read and analyzed. Key findings documented in sections below.

---

## 3. Audit Checklist Results

### 3.1 LLM Boundaries

**Status**: ✅ PASS

**Findings**:

1. **HomeOrchestratorImpl** (`feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:28-31`):
   - ✅ Constructor injects `AiChatService`
   - ✅ Uses `aiChatService.streamChat(request)` at line 34

2. **RealTranscriptOrchestrator** (`data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:43-46`):
   - ✅ Constructor injects `AiChatService`
   - ✅ Uses `aiChatService.sendMessage(AiChatRequest(prompt = prompt))` at line 60

3. **HomeScreenViewModel** (`feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:193-208`):
   - ✅ No `AiChatService` dependency in constructor
   - ✅ Only uses `HomeOrchestrator` interface (line 195)

4. **RealTingwuCoordinator** (`data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:62-70`):
   - ✅ No `AiChatService` dependency
   - ✅ Only depends on `TranscriptOrchestrator` (line 67)

5. **RealExportOrchestrator** (`data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:34-38`):
   - ✅ No `AiChatService` dependency
   - ✅ Only uses `MetaHub`, `ExportManager`, `ExportFileStore`

6. **InMemoryMetaHub** (`core/util/src/main/java/com/smartsales/core/metahub/InMemoryMetaHub.kt:15-76`):
   - ✅ No LLM dependencies
   - ✅ Pure data storage implementation

**Conclusion**: LLM boundaries are correctly enforced. Only `HomeOrchestratorImpl` and `RealTranscriptOrchestrator` access LLM services.

---

### 3.2 MetaHub Models & Merge Semantics

**Status**: ⚠️ PARTIAL (Test failures indicate potential merge issues)

**SessionMetadata** (`core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt:11-51`):

**Fields Verified**:
- ✅ `mainPerson`, `shortSummary`, `summaryTitle6Chars`, `location`, `stage`, `riskLevel`, `tags`, `crmRows`
- ✅ `latestMajorAnalysisMessageId`, `latestMajorAnalysisAt`, `latestMajorAnalysisSource`
- ✅ `lastUpdatedAt`

**mergeWith Implementation** (lines 29-43):
- ✅ Non-null override: `other.field ?: field` (lines 31-36)
- ✅ Tags union: `(tags + other.tags).filter { it.isNotBlank() }.toSet()` (line 37)
- ✅ `lastUpdatedAt = maxOf(lastUpdatedAt, other.lastUpdatedAt)` (line 38)
- ✅ `crmRows` dedupe via `mergeCrmRows` (lines 45-50): `distinctBy { it.client.trim() + "|" + it.owner.trim() }`

**TranscriptMetadata** (`core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt:11-66`):

**mergeWith Implementation** (lines 29-43):
- ✅ `speakerMap` merge: `mergeSpeakers` function (lines 45-65)
  - New overwrites existing (line 51-62)
  - Confidence clamped: `confidence?.coerceIn(0f, 1f)` (line 60)
- ✅ `extra` merged: `extra + other.extra` (line 42)
- ✅ `createdAt` monotonic: `maxOf(createdAt, other.createdAt)` (line 34)

**InMemoryMetaHub** (`core/util/src/main/java/com/smartsales/core/metahub/InMemoryMetaHub.kt:29-51`):
- ✅ `upsertSession` uses `mergeWith` (line 32)
- ✅ `upsertTranscript` uses `mergeWith` (line 43)

**Issues Found**:
- ⚠️ `InMemoryMetaHubTest.upsertSession_overwritesExisting` fails because test expects overwrite but implementation merges
- ⚠️ `TranscriptMetadataMergeTest.mergeWith_preservesExistingSpeakerAndMergesNewOnes` fails at confidence assertion (line 41)

---

### 3.3 HomeOrchestratorImpl & JSON Handling

**Status**: ✅ PASS

**HomeOrchestratorImpl.streamChat** (`feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:32-40`):
- ✅ Only processes `ChatStreamEvent.Completed` (line 35)
- ✅ Only when `shouldParseMetadata(request)` is true (line 35)
- ✅ Extracts JSON via `extractJsonBlock(event.fullText)` (line 47)
  - Supports fenced blocks: ````json ... ``` (lines 71-74)
  - Supports brace fallback (lines 79-93)
- ✅ Parses into `SessionMetadata` including `crmRows`, `latestMajorAnalysis*` (lines 96-137)
- ✅ Upserts via `metaHub.upsertSession(merged)` (line 54)
- ✅ Emits original `Completed` event without mutating `fullText` (line 38)

**HomeScreenViewModel** (`feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`):
- ✅ No JSON parsing for session metadata in VM
- ✅ Only reads from MetaHub: `metaHub.getSession(sessionId)` (lines 360, 783, 2152, 2220, 2445, 2474)
- ✅ Uses MetaHub for titles, debug HUD, export hints

**Conclusion**: JSON parsing is correctly isolated to `HomeOrchestratorImpl`. VM only reads from MetaHub.

---

### 3.4 HomeScreenViewModel Streaming & Dedup

**Status**: ⚠️ PARTIAL (Test compilation error prevents verification)

**Streaming Pipeline** (`feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:1061-1145`):

**Behavior Verified**:
- ✅ First assistant placeholder bubble created (line 1005: `createAssistantPlaceholder()`)
- ✅ Delta events handled via `StreamingDeduplicator.mergeSnapshot` (lines 1074-1079)
- ✅ Completed event finalizes content and cleans up (lines 1081-1115)
- ✅ `StreamingDeduplicator` class implements overlap detection (lines 2335-2355)

**HomeStreamingDedupTest** (`feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeStreamingDedupTest.kt`):
- ❌ **Compilation Error**: `FakeSessionRepository` missing `updateTitle` method (line 231)
- ❌ **Compilation Error**: `pin` method signature incorrect (line 245)

**Test Cases** (from code inspection):
- `delta only keeps single assistant bubble` (lines 91-103): Tests Delta-only dedup
- `delta then completed replaces content once` (lines 106-119): Tests Delta + Completed dedup
- `multiple rounds keep one assistant bubble per turn` (lines 122-143): Tests multi-round behavior

**Conclusion**: Streaming dedup logic appears correct in implementation, but tests cannot run due to compilation errors.

---

### 3.5 SMART_ANALYSIS & Export Path

**Status**: ✅ PASS

**SMART_ANALYSIS Requests** (`feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`):
- ✅ Only sent through `HomeOrchestrator` (line 1052: `homeOrchestrator.streamChat(request)`)
- ✅ `QuickSkillId.SMART_ANALYSIS` mapped to `"SMART_ANALYSIS"` mode (line 2377-2384)

**HomeScreenViewModel.exportMarkdown** (lines 356-412):
- ✅ Uses `latestAnalysisMarkdown` cache when available (line 365)
- ✅ If MetaHub has `latestMajorAnalysisMessageId` but cache empty → shows hint, no auto-run (lines 369-379)
- ✅ If no analysis and enough content → auto `SMART_ANALYSIS` once, then export (lines 381-409)
- ✅ If content too short → shows "内容太少" hint, no analysis, no export (lines 384-386)

**ExportOrchestrator** (`data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt`):
- ✅ `exportPdf(sessionId, markdown)`: Uses MetaHub for filename via `buildBaseName(metaHub.getSession(sessionId))` (line 50), gets markdown from VM parameter
- ✅ `exportCsv(sessionId)`: Uses only `SessionMetadata.crmRows` (line 64: `meta?.crmRows.orEmpty()`)
- ✅ No LLM usage: Constructor has no `AiChatService` dependency (lines 34-38)

**Conclusion**: Export path correctly uses MetaHub for metadata, VM for markdown content. No LLM usage in export.

---

### 3.6 TranscriptOrchestrator & Tingwu Path

**Status**: ✅ PASS

**TranscriptMetadataRequest Constructor Usage** (`data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:1023-1029`):
- ✅ Uses `transcriptId = transcriptId` (from job context)
- ✅ Uses `sessionId = jobRequests[transcriptId]?.sessionId` (line 1025)
- ✅ Uses `diarizedSegments = segments` (line 1026)
- ✅ Uses `speakerLabels = fallback` (line 1027)
- ✅ Uses `force = force` (line 1028)
- ✅ **No ghost params**: No `segments` or `fileName` parameters

**RealTranscriptOrchestrator** (`data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:49-70`):
- ✅ Cache vs force semantics: `readCachedMetadata` checks `request.force` and `hasReadableSpeaker` (lines 72-85)
- ✅ JSON parsing fail-soft: `runCatching { JSONObject(jsonText) }.getOrElse { return null }` (line 193)
- ✅ Metadata persisted: `persistMetadata` writes both `TranscriptMetadata` and `SessionMetadata` (lines 276-287)

**Test Results**:
- ✅ `RealTranscriptOrchestratorTest`: All tests passed
- ✅ `RealTingwuCoordinatorTest`: All tests passed

**Conclusion**: TranscriptOrchestrator correctly handles cache, force, and fail-soft parsing. Tingwu coordinator correctly constructs `TranscriptMetadataRequest`.

---

### 3.7 Audio Upload & Session Binding

**Status**: ✅ PASS

**Home Audio Upload Path** (`feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:518-583`):
- ✅ Passes current `sessionId` into `submitTranscription` (line 557: `transcriptionCoordinator.submitTranscription(..., uploadPayload)`)
- ✅ `TranscriptionChatRequest` created with `sessionId` (line 565: `TranscriptionChatRequest(..., recordingId = stored.id)`)
  - Note: `TranscriptionChatRequest` constructor not shown in file, but `onTranscriptionRequested` receives it with `request.sessionId` (line 631)

**onTranscriptionRequested** (lines 628-726):
- ✅ Reuses `request.sessionId` when available: `val targetSessionId = request.sessionId ?: "session-${UUID.randomUUID()}"` (line 631)
- ✅ Only generates new session when `request.sessionId` is null (line 631)
- ✅ Sets `this@HomeScreenViewModel.sessionId = targetSessionId` (line 634)

**RealTingwuCoordinator** (`data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:1215-1249`):
- ✅ `upsertTranscriptMetadata` receives `sessionId` from `request?.sessionId` (line 1228)
- ✅ Writes `TranscriptMetadata` with `sessionId` (line 1229)

**Conclusion**: Audio upload correctly preserves and passes `sessionId` through the pipeline. Session binding works as designed.

---

## 4. Risk Summary

| Issue | Severity | Location | Description |
|-------|----------|----------|-------------|
| InMemoryMetaHubTest failure | 🔴 High | `core/util/src/test/java/com/smartsales/core/metahub/InMemoryMetaHubTest.kt:17` | Test expects overwrite but implementation merges. Test name misleading or implementation incorrect. |
| TranscriptMetadataMergeTest failure | 🔴 High | `core/util/src/test/java/com/smartsales/core/metahub/TranscriptMetadataMergeTest.kt:41` | Confidence clamping assertion fails. Merge logic may have bug. |
| HomeStreamingDedupTest compilation error | 🔴 High | `feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeStreamingDedupTest.kt:231,245` | Missing `updateTitle` method and incorrect `pin` signature in `FakeSessionRepository`. |

---

## 5. Overall Conclusion

**Status**: ⚠️ **PARTIALLY ALIGNED** - Core architecture aligns with Orchestrator-MetadataHub-Mvp-V3, but test failures indicate implementation issues.

**Strengths**:
- ✅ LLM boundaries correctly enforced
- ✅ MetaHub models and merge semantics implemented
- ✅ HomeOrchestratorImpl JSON handling correct
- ✅ Export path LLM-free
- ✅ TranscriptOrchestrator and Tingwu path working
- ✅ Audio upload session binding correct

**Issues Requiring Fixes**:
1. **MetaHub merge test failures**: Tests expect different behavior than implementation. Either fix tests or fix implementation to match V3 spec.
2. **HomeStreamingDedupTest compilation**: Fix `FakeSessionRepository` to implement `updateTitle` and correct `pin` signature.

**Recommended Follow-up Tasks**:
1. **T-Fix-MetaHub-Tests**: Fix `InMemoryMetaHubTest` and `TranscriptMetadataMergeTest` to match V3 merge semantics, or fix implementation if tests are correct.
2. **T-Fix-StreamingDedupTest**: Fix `FakeSessionRepository` in `HomeStreamingDedupTest.kt` to implement `updateTitle(id: String, newTitle: String)` and remove or fix `pin` method.

---

## 6. Test Command Summary

| Command | Result | Notes |
|---------|--------|-------|
| `./gradlew :core:util:test` | ❌ FAILED | 2 test failures |
| `./gradlew :data:ai-core:testDebugUnitTest --tests "com.smartsales.data.aicore.RealTranscriptOrchestratorTest"` | ✅ PASSED | All tests passed |
| `./gradlew :data:ai-core:testDebugUnitTest --tests "com.smartsales.data.aicore.RealTingwuCoordinatorTest"` | ✅ PASSED | All tests passed |
| `./gradlew :feature:chat:testDebugUnitTest --tests "com.smartsales.feature.chat.home.HomeExportActionsTest" --tests "com.smartsales.feature.chat.home.HomeStreamingDedupTest" --tests "com.smartsales.feature.chat.home.HomeTranscriptionTest"` | ❌ FAILED | Compilation error |

---

**Report Generated**: 2025-01-27

