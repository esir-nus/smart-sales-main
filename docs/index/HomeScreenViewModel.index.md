# HomeScreenViewModel Index

Path: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`

Purpose: Central chat + media/transcription coordinator for the Home screen. This file is large; use tags to jump to V1-critical seams.

How to use:
- Search for `[HSVM:` tags in the Kotlin file.
- Use the Hotspot Index below to pick the right anchor before reading a large block.

## Hotspot Index (tags + purpose)

1) HSVM:STREAMING_PIPELINE — `startStreamingResponse(...)`
   - Purpose: start chat streaming, wire coordinator callbacks, update assistant placeholder.
   - Keywords: ChatStreamCoordinator, onDelta, onCompleted, onError.
   - Notes: V1 streaming path must remain deterministic.

2) HSVM:STREAMING_PIPELINE — `ChatStreamCoordinator.start(...)` call site
   - Purpose: centralized stream execution with retry hooks and requestProvider.
   - Keywords: completionEvaluator, requestProvider, onRetryStart, onTerminal.

3) HSVM:RETRY_LOOP — V1 retry wiring inside `startStreamingResponse(...)`
   - Purpose: V1 General retry loop + strict formatting repair instruction.
   - Keywords: V1GeneralRetryPolicy, V1GeneralCompletionEvaluator.
   - Notes: Do not add heuristics.

4) HSVM:CHAT_PUBLISH — `handleStreamCompleted(...)`
   - Purpose: finalize assistant output, apply V1 publisher, metadata gating.
   - Keywords: GeneralChatV1Finalizer, sanitizeAssistantOutput, handleGeneralChatMetadata.
   - Notes: Publisher owns truth; do not reintroduce heuristics in V1.

5) HSVM:CHAT_PUBLISH — V1 publish path inside `handleStreamCompleted(...)`
   - Purpose: visible2user-only rendering and artifact gating.
   - Keywords: v1Finalizer, artifactStatus, displayMarkdown.

6) HSVM:TINGWU_BATCH_RELEASE — `handleTranscriptionBatchRelease(...)`
   - Purpose: ingestion of Tingwu batch events and HUD tracking.
   - Keywords: AudioTranscriptionBatchEvent, tingwuTraceStore.

7) HSVM:PREFIX_GATE — `transcriptionBatchGate.offer(...)`
   - Purpose: continuous-prefix-only publish; buffers out-of-order batches.
   - Keywords: V1BatchIndexPrefixGate.
   - Notes: ordering primitive is numeric batchIndex.

8) HSVM:MACRO_WINDOW_FILTER — V1 macro-window filtering branch
   - Purpose: range filtering [absStartMs, absEndMs) using timedSegments.
   - Keywords: V1TingwuWindowedChunkBuilder, v1Window, timedSegments.
   - Notes: no text similarity dedupe; no transcript polishing.

9) HSVM:DEBUG_SNAPSHOT — `refreshDebugSnapshot()`
   - Purpose: HUD debug snapshot update, fail-soft behavior.
   - Keywords: debugOrchestrator, DebugSnapshot.

10) HSVM:SESSION_BOOTSTRAP — `init { ... }`
    - Purpose: initial observers and session bootstrap.
    - Keywords: observeDeviceConnection, observeMediaSync, observeSessions.

11) HSVM:SESSION_BOOTSTRAP — `prepareInitialSession(...)`
    - Purpose: first session creation + history bootstrap.
    - Keywords: sessionId, initialSessionPrepared.

12) HSVM:SESSION_BOOTSTRAP — profile + persona loading
    - Purpose: user profile and persona state initialization.
    - Keywords: loadUserProfile, salesPersona.

## Section Map (coarse; use tags for anchors)
- Initialization & session bootstrap: HSVM:SESSION_BOOTSTRAP
- Chat send/streaming pipeline: HSVM:STREAMING_PIPELINE, HSVM:RETRY_LOOP
- Final publish + metadata: HSVM:CHAT_PUBLISH
- Tingwu batch processing: HSVM:TINGWU_BATCH_RELEASE, HSVM:PREFIX_GATE, HSVM:MACRO_WINDOW_FILTER
- Debug/HUD snapshot: HSVM:DEBUG_SNAPSHOT

## Do-not-touch notes (V1-critical)
- HSVM:CHAT_PUBLISH — V1 publisher path and metadata gating are contract-critical.
- HSVM:TINGWU_BATCH_RELEASE + HSVM:PREFIX_GATE + HSVM:MACRO_WINDOW_FILTER — V1 Tingwu truth/ordering/dedupe.
- HSVM:RETRY_LOOP — must remain deterministic; no heuristic JSON scraping.
