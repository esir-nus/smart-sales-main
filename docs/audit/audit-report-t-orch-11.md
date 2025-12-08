# T-orch-11 – HomeScreenViewModel / Orchestrator-MetaHub Cleanup Audit

**Date**: 2025-12-06  
**Scope**: HomeScreenViewModel cleanup verification after T-orch-11

---

## 1. No ghost parameters on ChatRequest

**Status**: ✅ **OK**

**Evidence**:
- `ChatRequest` definition (`feature/chat/src/main/java/com/smartsales/feature/chat/core/AiChatService.kt:28-35`):
  ```kotlin
  data class ChatRequest(
      val sessionId: String,
      val userMessage: String,
      val quickSkillId: String? = null,
      val audioContextSummary: AudioContextSummary? = null,
      val history: List<ChatHistoryItem> = emptyList(),
      val isFirstAssistantReply: Boolean = false
  )
  ```
  - No `isAutoAnalysis` or `analysisMessageId` fields present.

- `buildChatRequest` (`HomeScreenViewModel.kt:1183-1203`):
  ```kotlin
  private fun buildChatRequest(
      userMessage: String,
      skillId: QuickSkillId?,
      historySource: List<ChatMessageUi>,
      audioContext: AudioContextSummary?,
      isAutoAnalysis: Boolean
  ): ChatRequest {
      // ... builds ChatRequest without isAutoAnalysis or analysisMessageId
      return ChatRequest(
          sessionId = sessionId,
          userMessage = userMessage,
          quickSkillId = mapSkillToMode(skillId),
          audioContextSummary = audioContext,
          history = history
      )
  }
  ```
  - Parameter `isAutoAnalysis` is accepted but NOT passed into `ChatRequest`.

- `sendMessageInternal` (`HomeScreenViewModel.kt:1049`):
  ```kotlin
  val request = buildChatRequest(content, quickSkillId, newState.chatMessages, audioContext, isAutoAnalysis)
  ```
  - Passes `isAutoAnalysis` to `buildChatRequest`, but it's only used for logging (line 1016), not in `ChatRequest`.

**Conclusion**: ✅ No ghost parameters passed to `ChatRequest`. `isAutoAnalysis` is VM-only and used for logging.

---

## 2. isAutoAnalysis is VM-only

**Status**: ✅ **OK**

**Evidence**:
- Usage in `HomeScreenViewModel.kt`:
  - Line 407: `isAutoAnalysis = true` (passed to `sendMessageInternal`)
  - Line 970: `isAutoAnalysis: Boolean = false` (parameter in `sendMessageInternal`)
  - Line 1016: `"isAutoAnalysis" to isAutoAnalysis` (logging only)
  - Line 1049: Passed to `buildChatRequest` but not used in `ChatRequest` construction

- No usage found in:
  - `data/ai-core` module
  - `core/util` module
  - `HomeOrchestratorImpl`
  - `ExportOrchestrator`

- Not written to MetaHub models (checked `SessionMetadata.kt` - no such field).

**Conclusion**: ✅ `isAutoAnalysis` is VM-only, used for logging and gating behavior, does not cross module boundaries.

---

## 3. No references to removed metadata types

**Status**: ✅ **OK**

**Evidence**:
- Grep search for `GeneralChatMetadata` and `SmartAnalysisMetadata`:
  - No matches found in workspace (excluding reference-source).

- `HomeScreenViewModel.kt` methods:
  - `parseGeneralChatMetadata` (line 2191): Returns `SessionMetadata?`, not `GeneralChatMetadata`
  - `parseSmartAnalysisMetadata` (line 2255): Returns `SessionMetadata?`, not `SmartAnalysisMetadata`
  - Both methods parse JSON and construct `SessionMetadata` directly.

- No `.toSessionMetadata()` conversions found based on removed types.

**Conclusion**: ✅ No references to `GeneralChatMetadata` or `SmartAnalysisMetadata`. All parsing returns `SessionMetadata`.

---

## 4. Metadata flow is orchestrator → MetaHub → VM

**Status**: ⚠️ **Partial** (duplicate parsing exists)

**Evidence**:
- `HomeOrchestratorImpl.kt` (lines 35-36, 43-55):
  ```kotlin
  if (event is ChatStreamEvent.Completed && shouldParseMetadata(request)) {
      runCatching { maybeUpsertSessionMetadata(request, event.fullText) }
  }
  ```
  - Parses assistant JSON and writes `SessionMetadata` to MetaHub ✅

- `HomeScreenViewModel.kt`:
  - Lines 1087, 1090: Also parses assistant JSON:
    ```kotlin
    if (shouldParseSessionMetadata) {
        handleGeneralChatMetadata(rawFullText)
    }
    if (isSmartAnalysis) {
        handleSmartAnalysisMetadata(rawFullText)
    }
    ```
  - Lines 2134-2252: `handleGeneralChatMetadata` and `handleSmartAnalysisMetadata` parse JSON and write to MetaHub.
  - **Issue**: This is duplicate parsing - both orchestrator and VM parse the same JSON.

- VM reads from MetaHub:
  - Line 360: `metaHub.getSession(sessionId)` (in `exportMarkdown`)
  - Line 783: `metaHub.getSession(sessionId)` (in `refreshDebugSessionMetadata`)
  - Uses `updateTitleFromMetadata` (line 2174, 2241) and `updateDebugSessionMetadata` (line 2187, 2251) ✅

- No duplicate/custom metadata model in VM ✅

**Conclusion**: ⚠️ Metadata flow is correct (orchestrator → MetaHub → VM), but there is duplicate parsing in both orchestrator and VM. The VM should rely on orchestrator's parsing and only read from MetaHub.

---

## 5. Export uses only exportPdf/exportCsv

**Status**: ✅ **OK**

**Evidence**:
- `ExportOrchestrator.kt` interface (lines 27-31):
  ```kotlin
  interface ExportOrchestrator {
      suspend fun exportPdf(sessionId: String, markdown: String): Result<ExportResult>
      suspend fun exportCsv(sessionId: String): Result<ExportResult>
  }
  ```
  - No `exportMarkdown` method ✅

- `HomeScreenViewModel.kt`:
  - `performExport` (lines 414-446):
    ```kotlin
    val result = when (format) {
        ExportFormat.PDF -> exportOrchestrator.exportPdf(sessionId, markdown)
        ExportFormat.CSV -> exportOrchestrator.exportCsv(sessionId)
    }
    ```
    - Correctly calls `exportPdf` or `exportCsv` ✅

  - `exportMarkdown` (line 356): Private method that routes to `performExport` ✅
  - `onExportPdfClicked` (line 312): Calls `exportMarkdown(ExportFormat.PDF)` ✅
  - `onExportCsvClicked` (line 316): Calls `exportMarkdown(ExportFormat.CSV)` ✅

- No direct `AiChatService` calls in export path ✅

**Conclusion**: ✅ Export correctly uses only `exportPdf`/`exportCsv`. No `exportMarkdown` calls on orchestrator.

---

## 6. Smart-analysis & export behavior

**Status**: ✅ **OK**

**Evidence**:
- `exportMarkdown` (`HomeScreenViewModel.kt:356-412`):
  - Line 362: First tries `latestAnalysisMarkdown` from VM state ✅
  - Line 361: Checks MetaHub's `latestMajorAnalysisMessageId` ✅
  - Lines 383-409: If no cached analysis, finds latest long content and triggers SMART_ANALYSIS through existing chat pipeline ✅
  - Line 407: `isAutoAnalysis = true` passed to `sendMessageInternal` ✅
  - No new LLM calls except through existing chat pipeline ✅

- No JSON parsing or metadata fabrication in VM for export ✅

**Conclusion**: ✅ Export behavior is correct: uses cached analysis, consults MetaHub, triggers SMART_ANALYSIS through chat pipeline if needed.

---

## 7. No new LLM usage added by this change

**Status**: ✅ **OK**

**Evidence**:
- `HomeScreenViewModel.kt`:
  - No `AiChatService` injection or usage found ✅
  - Uses `homeOrchestrator: HomeOrchestrator` (line 195) ✅
  - All LLM calls go through `homeOrchestrator.streamChat(request)` (line 1072) ✅

- `ExportOrchestrator.kt`:
  - No `AiChatService` dependency ✅
  - Only uses `MetaHub`, `ExportManager`, `ExportFileStore` ✅

- `HomeOrchestratorImpl.kt`:
  - Uses `AiChatService` (line 29) ✅
  - This is expected - orchestrators are allowed to call `AiChatService` ✅

**Conclusion**: ✅ No new LLM usage. Only orchestrators call `AiChatService`. VM and `ExportOrchestrator` do not use `AiChatService`.

---

## Summary Table

| Checklist Item | Status | Notes |
|---------------|--------|-------|
| 1. No ghost parameters on ChatRequest | ✅ OK | `isAutoAnalysis` is VM-only, not passed to `ChatRequest` |
| 2. isAutoAnalysis is VM-only | ✅ OK | Only used in VM for logging/gating |
| 3. No references to removed metadata types | ✅ OK | No `GeneralChatMetadata` or `SmartAnalysisMetadata` found |
| 4. Metadata flow is orchestrator → MetaHub → VM | ⚠️ Partial | Duplicate parsing in orchestrator and VM |
| 5. Export uses only exportPdf/exportCsv | ✅ OK | Correct usage, no `exportMarkdown` on orchestrator |
| 6. Smart-analysis & export behavior | ✅ OK | Uses cache, consults MetaHub, triggers through pipeline |
| 7. No new LLM usage added | ✅ OK | Only orchestrators call `AiChatService` |

---

## Overall Status

**⚠️ Yellow** - One partial issue found

**Issues**:
1. **Duplicate metadata parsing**: Both `HomeOrchestratorImpl` and `HomeScreenViewModel` parse assistant JSON and write to MetaHub. The VM should rely on orchestrator's parsing and only read from MetaHub.

**Recommendations**:
- Remove metadata parsing from `HomeScreenViewModel` (`handleGeneralChatMetadata` and `handleSmartAnalysisMetadata` methods).
- VM should only read from MetaHub after orchestrator has written metadata.
- This aligns with the Overhaul + V2 spec: orchestrators parse and write, VM reads.

**All other items pass** ✅

