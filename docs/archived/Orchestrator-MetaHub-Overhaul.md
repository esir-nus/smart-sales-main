** this doc is outdated and archived, for history reference only.

Here’s a markdown overhaul plan you can drop straight into the repo (e.g. `docs/plans/T-orch-07-Orchestrator-MetaHub-Overhaul.md`).

---

# T-orch-07 – Orchestrator + MetaHub Overhaul Plan (V2 Alignment)

> **Goal:** Bring `HomeOrchestrator`, `TranscriptOrchestrator`, `ExportOrchestrator`, `MetaHub`, and their consumers back into a clean, predictable state that **matches** `Orchestrator-MetadataHub-Mvp-V3.md`, fixes current regressions, and avoids introducing new ones.

---

## 0. Ground Rules & Constraints

### 0.1 Design & Spec Sources

Implementation in this plan **must** adhere to:

* `docs/plans/Orchestrator-MetadataHub-Mvp-V3.md`
* `docs/api-contracts.md`
* `docs/tingwu-doc.md`
* `docs/AGENTS.md`
* `docs/role-contract.md`
* `docs/style-guide.md`

If code disagrees with these docs, code is wrong (unless doc is explicitly marked *outdated*).

### 0.2 Layering & Ownership

* **Only orchestrators talk to LLMs:**

  * `HomeOrchestratorImpl` (GENERAL_CHAT / SMART_ANALYSIS)
  * `RealTranscriptOrchestrator` (TRANSCRIPT_METADATA)
* **MetaHub writes** are only allowed in:

  * `HomeOrchestratorImpl` (SessionMetadata)
  * `RealTranscriptOrchestrator` (TranscriptMetadata + SessionMetadata merge)
  * `RealExportOrchestrator` (ExportMetadata)
  * Existing low-level metadata helpers (if any)
* **Export path must be LLM-free:**

  * `ExportOrchestrator` & friends never call AiChatService.
  * CSV is generated solely from `SessionMetadata.crmRows`.

### 0.3 No Parallel V2 Types

Do **not** introduce v2-dupes like:

* `NewSessionMetadata`, `SessionMetadataV2`
* `V2HomeOrchestrator`, `NewTranscriptOrchestrator`

Instead, **extend existing types**:

* `SessionMetadata`, `TranscriptMetadata`, `ExportMetadata`
* `MetaHub` + `InMemoryMetaHub`
* `HomeOrchestrator` / `HomeOrchestratorImpl`
* `TranscriptOrchestrator` / `RealTranscriptOrchestrator`
* `ExportOrchestrator` / `RealExportOrchestrator`
* `SessionTitleResolver`

### 0.4 Fail Soft, Never Crash

On any metadata / JSON / MetaHub failure:

* Chat still shows assistant reply.
* Tingwu transcript still renders (with fallback speaker labels).
* Export either succeeds or fails with a clear, non-crashy error.
* No raw JSON or metadata keys should leak into user-facing UI.

---

## 1. Phase 0 – Codex Codebase Recon (No Writes)

> **Codex-only phase.** Just reading & grepping; no edits. Purpose is to sync mental model with actual repo.

### 1.1 Files to Scan

Codex should open and **fully read**:

* **MetaHub & models**

  * `core/util/src/main/java/.../metahub/SessionMetadata.kt`
  * `core/util/src/main/java/.../metahub/TranscriptMetadata.kt`
  * `core/util/src/main/java/.../metahub/ExportMetadata.kt`
  * `core/util/src/main/java/.../metahub/MetaHub.kt`
  * `core/util/src/main/java/.../metahub/InMemoryMetaHub.kt`

* **Home / Chat**

  * `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
  * `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenBindings.kt`
  * `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestrator.kt`
  * `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt`
  * `feature/chat/src/main/java/com/smartsales/feature/chat/title/SessionTitleResolver.kt` (or similar)

* **Tingwu / Transcript**

  * `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt`
  * `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTranscriptOrchestrator.kt`
  * `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`

* **Export**

  * `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt`
  * `data/ai-core/src/main/java/com/smartsales/data/aicore/RealExportOrchestrator.kt`
  * Any ExportManager / encoder used within.

* **Audio / Session binding**

  * `feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesViewModel.kt`
  * `feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesScreen.kt`
  * `feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesModels.kt`
  * `app/src/main/java/com/smartsales/aitest/audio/DefaultAudioTranscriptionCoordinator.kt`
  * Any Home → Audio upload entrypoints.

* **Tests (just to understand expectations)**

  * `feature/chat/src/test/.../HomeExportActionsTest.kt`
  * `feature/chat/src/test/.../HomeOrchestratorImplTest.kt`
  * `feature/chat/src/test/.../SessionTitle*Test.kt`
  * `data/ai-core/src/test/.../RealTranscriptOrchestratorTest.kt`
  * `feature/media/src/test/.../AudioFilesViewModelTest.kt`
  * Instrumentation: `AiFeatureTestActivityTest`, `NavigationSmokeTest`, `AudioTranscriptToChatTest`, etc.

### 1.2 Recon Checklist (Codex internal)

Codex should build an internal diff between **v2 spec vs actual code** for:

* MetaHub schema & merge semantics.
* HomeOrchestratorImpl JSON contract & fields handled.
* HomeScreenViewModel streaming flow & SMART_ANALYSIS pipeline.
* Transcript orchestration & Tingwu integration (speakerMap, confidence).
* Export orchestration (PDF/CSV, filenames).
* Audio → Tingwu → Chat sessionId wiring.
* History titles & transcription flags (title-driven vs metadata-driven).

No edits yet; just a mental map.

---

## 2. Phase 1 – MetaHub Model & Merge Semantics

> **Status:** Partially done (per recent progress). This phase finishes and hardens it.

### 2.1 SessionMetadata Extensions

* Extend existing `SessionMetadata` with v2 fields (all **nullable**):

  * `mainPerson`, `shortSummary`, `summaryTitle6Chars`, `location`
  * `stage: SessionStage?` (enum)
  * `riskLevel: RiskLevel?` (enum)
  * `tags: Set<String>`
  * `crmRows: List<CrmRow>`
  * `latestMajorAnalysisMessageId: String?`
  * `latestMajorAnalysisAt: Long?`
  * `latestMajorAnalysisSource: AnalysisSource?`
  * `lastUpdatedAt: Long?`

* Add `CrmRow` data class and `AnalysisSource` enum per v2 doc.

### 2.2 TranscriptMetadata / ExportMetadata Extensions

* `TranscriptMetadata`:

  * Add optional fields for:

    * `speakerMap: Map<String, SpeakerMeta>`
    * `sessionId`, `diarizedSegmentCount`
    * `stage`, `riskLevel`, `extra/notes`
  * Implement `mergeWith(other: TranscriptMetadata)` for non-destructive merge.

* `ExportMetadata`:

  * Ensure it tracks:

    * `sessionId`
    * `format` (PDF/CSV)
    * `fileName`
    * `exportedAt`

### 2.3 InMemoryMetaHub Merge Helpers

* Update `InMemoryMetaHub` to **non-null merge**:

  * For sessions:

    * `new = old.copy(...)` with:

      * `field = newField ?: oldField`
      * `tags = old.tags ∪ new.tags`
      * `crmRows = old.crmRows + new.crmRows` (with dedupe if needed)
  * For transcripts:

    * merge speakerMap and extra fields similarly.

* Keep threading model (Mutex, etc.) unchanged.

* Add focused unit tests for:

  * `SessionMetadata.mergeWith` semantics.
  * `InMemoryMetaHub` upsert does not lose existing data.

---

## 3. Phase 2 – HomeOrchestratorImpl & HomeScreenViewModel

> Orchestrator writes metadata, VM owns UI behavior. No JSON leaks.

### 3.1 HomeOrchestratorImpl (Completed events → MetaHub)

* On `ChatStreamEvent.Completed` for **GENERAL_CHAT** & **SMART_ANALYSIS**:

  1. Extract JSON from `fullText`:

     * Prefer fenced ```json blocks.
     * Fallback to brace scanning where needed.
  2. Parse JSON into:

     * session-level fields:

       * `main_person`, `short_summary`, `summary_title_6chars`, `location`
       * `stage`, `risk_level`
       * `highlights[]`, `actionable_tips[]` → tags
       * `crm_rows[]` → `List<CrmRow>`
     * latest major analysis fields:

       * message id (from context), timestamp, `AnalysisSource`:

         * `GENERAL_FIRST_REPLY`
         * `SMART_ANALYSIS_USER`
         * `SMART_ANALYSIS_AUTO`
  3. Use MetaHub merge helper to update `SessionMetadata`.
  4. Always **re-emit original** Completed event to VM (no mutation of text).

* Do **not**:

  * change `fullText`,
  * strip JSON inside orchestrator,
  * format UI strings.

### 3.2 HomeScreenViewModel Streaming Fixes

* **Streaming pipeline**:

  * Tokens:

    * Update an in-flight assistant bubble’s text only.
    * Do not append new messages.
  * Completed:

    * Take the final accumulated text.
    * Run `sanitizeAssistantOutput` (or successor) exactly once.
    * Update the existing assistant message with final text (no extra bubble).
    * Clear any streaming temp state.

* Ensure no path appends duplicated Completed content to the chat history.

### 3.3 Title Refresh & History Sync

* After the **first GENERAL_CHAT assistant reply**:

  1. Read `SessionMetadata` via MetaHub.
  2. Use `SessionTitleResolver` to compute title:

     * Prefer MetaHub (mainPerson + summaryTitle6Chars).
     * Fallback to previous heuristics when necessary.
  3. Update:

     * session repository title,
     * history list UI (e.g. via existing repo / state flows).

* History titles should thus reflect MetaHub-driven state.

### 3.4 SMART_ANALYSIS Gating & Metadata Consumption

* Before sending SMART_ANALYSIS:

  * Use `findLatestLongContent` (or equivalent) + threshold:

    * If no sufficient content → show “内容太少，无法做有价值的智能分析”，**do not** send analysis request.
    * Else:

      * Build SMART_ANALYSIS request (content + optional goal).
      * Use `HomeOrchestrator` with SMART_ANALYSIS mode.

* On SMART_ANALYSIS Completed:

  * Store final markdown as `latestAnalysisMarkdown`.
  * Do **not** parse JSON again (orchestrator already wrote metadata).
  * Optionally append a short “分析完成，可导出 PDF/CSV” helper message (plain text only).

---

## 4. Phase 3 – ExportOrchestrator (LLM-free) & Export Macro

### 4.1 RealExportOrchestrator Implementation

* Keep existing **interface**; adjust internals:

  * `exportPdf(sessionId, markdown)`:

    * Build filename from MetaHub SessionMetadata:

      * `yyyyMMdd_HHmm_<mainPersonOr未知客户>_<summaryTitleOr销售咨询>`
    * Sanitize filename (no invalid FS chars), truncate if long.
    * Use `ExportManager` to generate PDF from markdown.
    * Write `ExportMetadata` via MetaHub.

  * `exportCsv(sessionId)`:

    * Read `SessionMetadata.crmRows`.

    * CSV schema e.g.:

      ```text
      client,region,stage,progress,next_step,owner
      ```

    * If `crmRows` is empty:

      * Output header-only CSV (safe fallback).

    * Write `ExportMetadata`.

* **No calls** to AiChatService or any LLM client in this class.

### 4.2 HomeScreenViewModel Export Macro

* When user taps export PDF/CSV:

  1. If `latestAnalysisMarkdown` exists and SessionMetadata has a recent `latestMajorAnalysis*`:

     * PDF:

       * call `ExportOrchestrator.exportPdf(sessionId, latestAnalysisMarkdown)`.
     * CSV:

       * call `ExportOrchestrator.exportCsv(sessionId)`.

  2. Else (no major analysis yet):

     * Auto trigger SMART_ANALYSIS once (source = `SMART_ANALYSIS_AUTO`).
     * On successful analysis, proceed as above.
     * If analysis cannot run (content too short), surface the existing “内容太少” message and **skip** export.

* VM does not write ExportMetadata directly and does not use LLM.

---

## 5. Phase 4 – TranscriptOrchestrator & RealTingwuCoordinator

### 5.1 RealTranscriptOrchestrator

* Input: `TranscriptMetadataRequest` (existing type), including:

  * `transcriptId`
  * `sessionId?`
  * `segments` (diarized with speakerId/text/job info)
  * optional `fileName` / main person hints
  * `force: Boolean` (for retry vs cache)

* Behavior:

  1. **Cache check** (force=false):

     * If MetaHub has `TranscriptMetadata` with non-default speakerMap:

       * Return cached metadata.
       * Stamp note like `speakerInferCacheHit`.

  2. **Prompt build**:

     * Sample segments via helper (limit segments + chars, cover speakers).
     * Build a prompt that:

       * Explains the sales conversation context.
       * Requests `speaker_map`, `main_person`, `short_summary`, `summary_title_6chars`, `location`, `stage`, `risk_level`.

  3. **LLM call**:

     * Single non-streaming chat via AiChatService.

  4. **JSON parse**:

     * Extract JSON block (fenced → bare JSON → brace scanning).
     * Parse:

       * `speaker_map` → `Map<String, SpeakerMeta>`
       * `confidence` clamped to [0f,1f].
       * session-level fields as above.
     * If JSON parse fails → return null, no MetaHub writes.

  5. **MetaHub writes**:

     * Merge `TranscriptMetadata` via `mergeWith`.
     * Merge `SessionMetadata` via non-null merge helper.

* `force=true`:

  * Always call LLM, even if cache exists.
  * Overwrite speakerMap with new metadata (still non-null merge semantics).

### 5.2 RealTingwuCoordinator Integration

* Auto path (job completion):

  * After Tingwu job succeeds:

    * Keep current polling & artifact fetch.
    * Build diarized segments.
    * Call `TranscriptOrchestrator.inferTranscriptMetadata(force=false)`.
    * Merge orchestrator speakerMap into Tingwu labels:

      * If `meta.displayName != null && confidence >= threshold` → override.
      * Else keep original label.
    * Call `buildMarkdown(...)` using **merged labels only**, no JSON.

* Retry path (“智能识别角色”):

  * Separate entrypoint:

    * Reuse stored diarized segments and job context.
    * Call `TranscriptOrchestrator.inferTranscriptMetadata(force=true)`.
    * Rebuild markdown with merged labels.
    * Update UI state.

* Markdown must remain clean:

  * Format:

    ```text
    ## 逐字稿
    - [00:01 - 00:05] 罗总：...
    ```

  * No debug/JSON/notes printed.

---

## 6. Phase 5 – Audio Upload Session Binding

### 6.1 Correct SessionId Semantics

* **Home in-session upload**:

  * If user is currently in a chat session (has `sessionId`):

    * Audio upload from Home must:

      * Create Tingwu job with that `sessionId`.
      * Route transcript back into the same session (append markdown/messages).
    * Must **not** create a new session.

* **Audio Sync page “转写/新会话”**:

  * Only here is it allowed to:

    * Create a new `sessionId` for an uploaded audio.
    * Start Tingwu with that new `sessionId`.
    * On completion, open a new chat session bound to it.

### 6.2 Navigation & Tests

* Adjust navigation so:

  * Home → Audio upload → Chat:

    * Either stays on current chat, or pushes transcript into it.
  * Audio Sync → “转写并查看聊天”:

    * Opens **new** chat session.

* Update tests:

  * `AudioTranscriptToChatTest` should assert:

    * Home upload keeps same `sessionId`.
  * Audio-related integration tests should validate:

    * Only Audio Sync path produces new sessions.

---

## 7. Phase 6 – Tests & Validation

### 7.1 Unit Tests to Update/Add

* `core/util`:

  * `InMemoryMetaHubTest`:

    * Verify non-null merge semantics (session & transcript).
    * Verify tags/crmRows union behavior.

* `data/ai-core`:

  * `RealTranscriptOrchestratorTest`:

    * Cache hit vs force=true.
    * JSON parse success (speaker_map + session fields).
    * JSON parse failure returns null, no MetaHub write.
    * confidence clamping & threshold behavior.

  * `RealTingwuCoordinatorTest`:

    * Correct label merge based on confidence.
    * Markdown uses merged labels, no JSON.

  * `RealExportOrchestratorTest`:

    * PDF filename uses MetaHub metadata.
    * CSV generated from crmRows, header-only fallback.

* `feature/chat`:

  * `HomeOrchestratorImplTest`:

    * Completed events unchanged.
    * Correct metadata written into MetaHub (mainPerson, summaryTitle6Chars, crmRows, latestMajorAnalysis*).

  * `HomeExportActionsTest`:

    * SMART_ANALYSIS used as source for export.
    * PDF uses analysis markdown.
    * CSV uses crmRows, no LLM.

  * `SessionTitle*Test`:

    * Titles derived from MetaHub when available.
    * Fallback heuristics when metadata missing.

* `feature/media`:

  * `AudioFilesViewModelTest`:

    * Retry speaker inference calls orchestrator with force=true.
    * Busy state prevents concurrent retries.
    * On error, busy state resets.

### 7.2 Command Checklist (when possible)

In a real environment (outside sandbox), run:

```bash
./gradlew :core:util:test
./gradlew :data:ai-core:test
./gradlew :feature:chat:test
./gradlew :feature:media:test

./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

If some commands can’t run due to permissions, at least ensure all referenced tests compile and logically match v2 behavior.

---

## 8. Success Criteria

This overhaul is considered **done** when:

* ✅ **Session titles & export filenames**:

  * Driven by MetaHub (mainPerson + summaryTitle6Chars) via `SessionTitleResolver`.
* ✅ **Smart analysis**:

  * No duplicate streaming output.
  * Only runs when there is enough content.
  * Metadata (latestMajorAnalysis*, crmRows) is correctly populated.
* ✅ **CSV export**:

  * 100% generated from `crmRows`.
  * No LLM involved.
* ✅ **Tingwu transcripts**:

  * Speaker labels refined via TranscriptOrchestrator.
  * Confidence threshold respected.
  * Markdown clean, readable, JSON-free.
* ✅ **Audio upload**:

  * Home upload stays in current session.
  * Audio Sync can create new sessions; tests reflect this.
* ✅ **All updated tests**:

  * Encode the v2 contracts and pass on CI.

Once these are true and the test suite is green, T-orch-07 can be considered complete.
