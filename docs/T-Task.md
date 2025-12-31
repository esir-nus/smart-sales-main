# T-Task: Engineering Trace (Orchestrator)

Current spec: `docs/Orchestrator-V1.md` (CURRENT)  
Purpose: track active workstreams and preserve archived traces.  
Note: This file is CURRENT. Historical traces (e.g., V7) are preserved under Archived Traces and frozen.

---

## NON-NORMATIVE TASK LOG NOTE

This file is a **non-normative** task log / dev trace. It does **not** define behavior or contracts.
**SoT**: `docs/Orchestrator-V1.md` (CURRENT) + V1 schema/examples.
Agents MUST stop reading after **“V1 Workstreams (CURRENT)”** unless the operator explicitly requests archived history.
Anything under **ARCHIVED** is historical only and MUST NOT be used as a target spec.

---

## 1) V1 Workstreams (CURRENT)

### WS-V1-1 — Disector plan + batch policy
- 10min target + 10s pre-roll overlap
- Absolute timeline grounding and deterministic plan persistence

### WS-V1-2 — Tingwu batch orchestration
- maxInflight=10 queue + retry scaffolding
- Batch completion can be out-of-order; publish is prefix-only

### WS-V1-3 — MemoryCenter interface + M2BTranscriptionState schema
- LLM outputs are optional; schema must be stable
- Speaker mapping evolves by batchIndex

### WS-V1-4 — Publisher deterministic rendering
- Absolute time anchoring + overlap range filtering
- Transcript + analysis views, no suspicious-gap polishing

---

## 2) Task List (V1)

Legend: TODO / DOING / DONE / BLOCKED

### T1-001 Orchestrator-V1 spec + doc sync
- Status: DONE
- Evidence:
  - `docs/Orchestrator-V1.md`

### T18f Tingwu macro-window filter kill-switch (default ON)
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/AiCoreConfig.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
- Behavior:
  - `enableV1TingwuMacroWindowFilter` default `true`
  - When ON: apply V1 macro-window filtering when `v1Window + timedSegments` present; emit `v1_tingwu_window_filter_applied` (counts only)
  - When OFF: always publish legacy `markdownChunk`; do not emit `v1_tingwu_window_filter_applied`

---

## Archived Traces

### V7 Dev Trace (Orchestrator–MetaHub) [ARCHIVED]

# T-Task: V7 Dev Trace (Orchestrator–MetaHub) [ARCHIVED]

Doc version: 1.0  
Historical reference (V7 era): `docs/archived/Orchestrator-MetadataHub-V7.md` (ARCHIVED)  
Replacement: `docs/Orchestrator-V1.md`  
Purpose: Track V7 engineering workstreams, decisions, and verification evidence.

> Note: V7 is deprecated and archived. This file is frozen for historical reference only; new work must follow V1.

---

## 0) Scope and Outcomes (Expected Results Under Control)

V7 targets:

- Tingwu + OSS as default transcription lane; XFyun lane present but disabled by default.
- M1/M2/M3/M4 model documented and enforceable:
  - M1: UserMetadata (static JSON from onboarding/user-center)
  - M2: ConversationDerivedState (dynamic session state; provenance + uiSignals reserved)
  - M3: SessionState + RenamingMetadata (accepted vs candidate; export gating)
  - M4: External Knowledge & Style placeholder (API + pack versioning + HUD trace)
- HUD has 3 copy/paste sections:
  1) Effective run snapshot
  2) Raw transcription output
  3) Preprocessed snapshot

---

## 1) Workstreams

### WS1 — Docs and Contracts
- V7 spec doc (Orchestrator–MetaHub)
- V7 schema (`docs/archived/metahub-schema-v7.json`)
- api-contracts + ux-contract alignment
- Historical note (V7 era): AGENTS.md was updated during the transition; CURRENT spec is V1 and V7 is ARCHIVED.
- Archived banners added to V6/V5/V4

### WS2 — Provider Lanes (Integration)
- Tingwu + OSS lane (default) verification
- XFyun lane (disabled by default) wiring audit + guardrails
- Third-party integration registry (docs/source-repo.json + schema) kept 3rd-party only

### WS3 — Multi-agent Pipeline (Design -> Implementation)
- Agent1 (Memory Keeper + Smart Analysis) patch model for M2
- Agent2 (Polisher) bounded edits model
- Pseudo-streaming batch-by-batch contract

### WS4 — Debug/HUD Observability
- Snapshot composition rules (3 sections)
- Redaction rules: no secrets, no raw HTTP bodies, copy-only JSON blocks

### WS5 — Export (Gated by Smart Analysis)
- Export gating model
- Export naming via RenamingMetadata

### WS6 — External Knowledge & Style (M4 placeholder)
- Pack versioning fields, retrieval trace, consent/feature flags (design only)

---

## 2) Task List (Status Tracking)

Legend: TODO / DOING / DONE / BLOCKED

### T7-001 Doc sync to V7 (spec + contracts + schema)
- Status: DONE
- Evidence:
  - `docs/archived/Orchestrator-MetadataHub-V7.md`
  - `docs/archived/metahub-schema-v7.json`
  - `docs/api-contracts.md`
  - `docs/ux-contract.md`
  - `docs/AGENTS.md`

### T7-002 Archive banners (V6/V5/V4)
- Status: DONE
- Evidence:
  - banner inserted at top of archived docs

### T7-003 Third-party registry refactor (OSS/Tingwu/XFyun/Dashscope only)
- Status: DONE
- Evidence:
  - `docs/source-repo.json`
  - `docs/source-repo.schema.json`

### T7-004 Implementation: MetaHub storage for M2/M3 (patch-based)
- Status: DONE
- Definition of done:
  - M2 writes are patch-based with provenance
  - M3 renaming accepted vs candidate works and is stable after user override
 - Evidence:
  - `core/util/src/main/java/com/smartsales/core/metahub/ConversationDerivedState.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/M2PatchRecord.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/MetaHubM2Helper.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/MetaHub.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/InMemoryMetaHub.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/FileBackedMetaHub.kt`
  - `core/util/src/test/java/com/smartsales/core/metahub/M2PatchMergeTest.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/metahub/FileBackedMetaHubTest.kt`
  - Tests:
    - `./gradlew :core:util:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 7s)
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 14s)
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 22s)
 - Semantics summary:
  - M2 patch records are internal derived structures (schema does not define patch type), used only for storage and deterministic merge.
  - Merge applies in append order; later writes override (only fields explicitly in the patch).
  - effective.updatedAt uses the last patch createdAt; version equals patch count; reads do not generate new timestamps.
  - PatchHistory and effectiveM2 persistence is handled by FileBackedMetaHub (atomic writes).
 - On-device sanity checklist (manual):
  - [ ] Trigger at least 1 M2 patch write (use existing debug entry/flow)
  - [ ] Force-stop app and restart
  - [ ] Verify same session m2PatchHistory/effectiveM2 still loads (HUD/file path check)

### T7-004A Implementation: MetaHub M3 naming stability (candidate/accepted/effective)
- Status: DONE
- Evidence:
  - `core/util/src/main/java/com/smartsales/core/metahub/RenamingMetadata.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/MetaHubRenamingHelper.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/ExportNameResolver.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryViewModel.kt`
  - `core/util/src/test/java/com/smartsales/core/metahub/RenamingMetadataTest.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/ExportNameResolverTest.kt`
  - `feature/chat/src/test/java/com/smartsales/feature/chat/history/ChatHistoryViewModelTest.kt`
  - Tests:
    - `./gradlew :core:util:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 8s)
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 18s)
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 12s)
- Semantics summary:
  - M3 naming writes go directly to `SessionMetadata.renaming`, avoid parallel SessionState.
  - accepted overrides candidate; candidate updates do not overwrite accepted (merge/helper follow this).
  - effective name resolves to accepted > candidate > fallback.
  - ExportNameResolver prioritizes M3 naming: accepted first, then candidate.
  - User renames write M3 accepted + timestamp (userRenamedAt).

### T7-005 Implementation: provider lane selection (default Tingwu+OSS; XFyun disabled)
- Status: DONE
- Definition of done:
  - Effective lane selection visible in HUD Section 1
  - XFyun cannot be used unless explicitly enabled and preflight validated
  - Evidence:
    - `data/ai-core/src/main/java/com/smartsales/data/aicore/params/AiParaSettings.kt` (default Tingwu; added `xfyunEnabled=false` gate)
    - `data/ai-core/src/main/java/com/smartsales/data/aicore/params/TranscriptionLaneSelector.kt` (centralized lane decision + disabled reason)
    - `app/src/main/java/com/smartsales/aitest/audio/SwitchableAudioTranscriptionCoordinator.kt` (fallback to Tingwu when XFyun not enabled)
    - `app/src/main/java/com/smartsales/aitest/ui/screens/audio/AiParaSettingsViewModel.kt` (enable gate when user explicitly selects XFyun)
    - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt` + `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt` (HUD Section 1 shows lane + disabled reason; Tingwu HUD Raw/Transcript export buttons)
    - `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt` + `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/TingwuTraceStore.kt` (Tingwu raw transcripts persisted; HUD export entry)
    - Tests:
      - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 8s)
      - `./gradlew :feature:chat:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 8s)

### T7-006 Implementation: HUD 3-block copy snapshot
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugSnapshot.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugSnapshotRedactor.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 17s)
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 25s)

### T7-007 Implementation: pseudo-streaming transcript batches
- Status: DONE
- Evidence:
  - `feature/media/src/main/java/com/smartsales/feature/media/audiofiles/AudioTranscriptionCoordinator.kt` (added batch event stream interface)
  - `feature/media/src/main/java/com/smartsales/feature/media/audiofiles/TranscriptionBatchPlanner.kt` (fixed-line batch split)
  - `app/src/main/java/com/smartsales/aitest/audio/DefaultAudioTranscriptionCoordinator.kt` (Tingwu batch event output)
  - `app/src/main/java/com/smartsales/aitest/audio/XfyunAudioTranscriptionCoordinator.kt` (XFyun batch event output)
  - `app/src/main/java/com/smartsales/aitest/audio/SwitchableAudioTranscriptionCoordinator.kt` (batch stream delegation)
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt` (batch merge + completion freeze)
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/TingwuTraceStore.kt` (batch plan recording)
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt` (Section 3 batch plan display)
  - `feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeTranscriptionTest.kt` (batch stream unit test)
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 8s)
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 11s)
  - On-device sanity checklist (manual):
    - Use medium/long audio to start transcription; confirm transcript bubble updates at least twice.
    - Processing shows "transcription in progress"; after completion, streaming indicator stops.
    - After completion, speaker labels no longer change (frozen).
    - HUD Section 3 shows batch plan summary and preview (placeholder if missing).

### T7-008 Implementation: Smart Analysis gating for export
- Status: DONE
- Evidence:
  - `core/util/src/main/java/com/smartsales/core/metahub/ExportNameResolver.kt` (accepted/candidate/fallback name resolution)
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt` (export gate decision + early return)
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt` (export button disable + reason)
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt` (export guardrail + naming persistence)
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt` (HUD Section 1 shows exportGate + resolved name)
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 13s)
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 33s)
  - On-device sanity checklist (manual):
    - [ ] When Smart Analysis is not ready, export button is disabled and shows a reason
    - [ ] When Smart Analysis completes, export button becomes enabled
    - [ ] Export file name follows accepted > candidate > fallback
    - [ ] HUD Section 1 shows exportGate status + resolved name

### T7-008.1 Implementation: export chips immediate + auto-analysis auto-export
- Status: DONE
- Evidence:
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt` (export quick skills act immediately; no select+send)
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt` (auto-analysis + auto-export state machine; last click wins)
  - `feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeExportActionsTest.kt` (auto-analysis+export when not ready)
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 8s)
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 28s)
  - On-device sanity checklist (manual):
    - [ ] When Smart Analysis not ready, tapping "Export PDF/CSV" auto-triggers analysis
    - [ ] After analysis, export sharing opens automatically (no second tap)
    - [ ] When Smart Analysis already ready, tapping export works immediately
    - [ ] Smart Analysis chip still requires send to trigger (mode unchanged)
    - [ ] Multiple taps: last tap wins

### T7-008.2B Implementation: interrupted audio recovery banner (Home)
- Status: DONE
- Evidence:
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt` (Home top banner + two actions)
  - Tests:
    - `./gradlew :core:util:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 7s)
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 8s)
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 15s)
  - On-device sanity checklist (manual):
    - [ ] Start audio transcription and force-stop app; after restart, Home top banner appears
    - [ ] Tap "Got it" to hide; restart again and the same startedAt no longer appears
    - [ ] Tap "Re-upload" to open audio selection flow; after file selection, transcription starts normally without sending a message

### T7-009 Placeholder: External Knowledge & Style (M4) API interface
- Status: TODO
- Definition of done:
  - feature flag + consent gates defined
  - HUD Section 1 logs pack versions + traceId (when enabled)

### T7-009 Implementation: Persistent MetaHub (file-backed)
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/FileBackedMetaHub.kt`
  - `app/src/main/java/com/smartsales/aitest/MetaHubModule.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/metahub/FileBackedMetaHubTest.kt`
  - Tests:
    - `./gradlew :core:util:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 14s)
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 15s)
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 34s)
- Notes:
  - Only SessionMetadata is persisted (including M3 renaming); M2 patch is still not persisted (V7 mismatch; needs a separate task later).
  - Errata: M2 patch persistence statements conflict with T7-004; treat T7-004 evidence/tests as authoritative; this V7 trace is preserved as-is.
- On-device sanity checklist (manual):
  - [ ] Enter a session and run Smart Analysis to generate latestMajorAnalysis*
  - [ ] Manually rename session (writes M3 accepted)
  - [ ] Force-stop app and restart
  - [ ] Verify export gate remains ready
  - [ ] Verify HUD Section 1 shows same sessionId + M3 accepted name
  - [ ] Verify renamed session persists

### T7-010B1 Implementation: Tingwu preprocess -> MetaHub M2 patch
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt` (append Tingwu preprocess patch on completion)
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/TingwuPreprocessPatchBuilder.kt` (deterministic preview/batch build)
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/TingwuPreprocessPatchBuilderTest.kt` (preprocess snapshot after patch)
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 29s)
- On-device sanity checklist (manual):
  - [ ] After Tingwu transcription completes, HUD Section 3 shows MetaHub preprocess preview + batches
  - [ ] Force-stop and restart; same session still shows preprocess snapshot

### T7-010B2 Implementation: HUD Section 3 MetaHub preprocess provenance line
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/debug/RealDebugOrchestratorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 25s)
- On-device sanity checklist (manual):
  - [ ] After Tingwu transcription completes, HUD Section 3 shows preprocess.source line

### T7-010B3A Tingwu preprocess trace capture (A1/A2)
- A1 Status: DONE (completed TingwuTraceStore fields and ordering; not yet wired to actual output)
- A2 Status: DONE (after Tingwu completion, trace batches/edges are preferred)
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/TingwuTraceStore.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/TingwuPreprocessPatchBuilder.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/RealTingwuCoordinatorTest.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/TingwuPreprocessPatchBuilderTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 1m 43s)
- On-device sanity checklist (manual):
  - [ ] After Tingwu transcription completes, MetaHub M2 preprocess uses trace batches and suspicious boundaries

### T7-010B3B1 Tingwu suspicious-boundary detector (pure helper)
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuSuspiciousBoundaryDetector.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/tingwu/TingwuSuspiciousBoundaryDetectorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 20s)
- On-device sanity checklist (manual):
  - [ ] Feed transcription text with timestamps and speaker labels; detector returns non-empty suspicious boundaries

### T7-010B3B2 Tingwu suspicious-boundary wiring (trace -> M2)
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/RealTingwuCoordinatorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 27s)
- On-device sanity checklist (manual):
  - [ ] After Tingwu transcription completes, trace includes non-empty suspiciousBoundaries and M2 preprocess uses them

### T7-010B3C Debug HUD — Tingwu suspiciousBoundaries inspector
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/debug/RealDebugOrchestratorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 20s)
- On-device sanity checklist (manual):
  - [ ] HUD Section3B shows Tingwu suspiciousBoundaries (count/indices/details)

### T7-010B3D Debug HUD hygiene — suppress legacy xfyun.suspiciousBoundaries
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/debug/RealDebugOrchestratorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon` (BUILD SUCCESSFUL in 20s)
- On-device sanity checklist (manual):
  - [ ] During Tingwu preprocess, Section3 shows a "suppressed" note for xfyun.suspiciousBoundaries

---

## 3) Decision Log

- D7-001: Third-party integration registry is limited to OSS/Tingwu/XFyun/Dashscope + placeholders; it must not contain product specs or M1/M2/M3 schema.
- D7-002: XFyun REST parameter tables remain only in `docs/xfyun-asr-rest-api.md` (no duplication).
- D7-003: UI is allowed to read M2 for future playful UX; stabilized uiSignals reserved as future upgrade.

---

## 4) Open Questions / Risks

- R7-001: Tingwu+OSS lane specifics (OSS vendor/API shape) not documented here; must be discovered from code + tests.
- R7-002: M2 stabilization (hysteresis) is not implemented in V7 docs; reserved for future upgrade.
- R7-003: External memory / M4 governance: pack update process and permissions are TBD.

---

## 5) Verification Checklist (when implementation starts)

- [x] Restored default transcription provider to Tingwu+OSS; XFyun disabled by default (evidence: `app/src/main/java/com/smartsales/aitest/ui/screens/audio/AudioFilesShell.kt`; test: `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`)
- [ ] HUD 3 sections present and copyable
- [ ] No raw JSON visible in normal UI bubbles
- [ ] Export gated by Smart Analysis
- [ ] M4 placeholder does not change behavior unless enabled
