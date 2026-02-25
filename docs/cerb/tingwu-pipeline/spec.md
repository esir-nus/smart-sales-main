# Tingwu Pipeline Spec

> **OS Layer**: SSD Storage (Writes directly to MetaHub/SessionMetadata)

## Overview

The Tingwu Pipeline is an intelligent audio processing agent. It is fundamentally different from the `asr-service`. While `asr-service` provides simple, quick Speech-to-Text (e.g., for Scheduler Voice inputs), the `tingwu-pipeline` handles comprehensive, long-form audio processing.

It leverages Aliyun Tingwu's native capabilities to perform:
1. **Diarization**: Speaker separation (e.g., Sales vs. Client).
2. **Custom Prompts**: LLM extraction injected directly into the ASR phase.
3. **Auto-Chapters & Summarization**: Native structuring of long meetings.
4. **Text Polish**: Stutter removal and cleanup.

## Logical Flow

1. **Pre-requisite (External)**: Audio file is captured locally.
2. **OSS Upload (External)**: Audio is uploaded to OSS via the dedicated `oss` cerb feature. An `ossFileUrl` is obtained.
3. **Submission**: `TingwuPipeline.submit()` is called with the `ossFileUrl`. A task is created on Aliyun.
4. **Polling**: `TingwuPipeline.observeJob()` polls the API until completion.
5. **Artifact Fetching**: Upon completion, the pipeline downloads the rich JSON results (chapters, summaries, diarization).
6. **SSD Write**: The pipeline writes these intelligence artifacts directly into the database (MetaHub/SessionMetadata) so they are available for downstream tools (like Analyst Orchestrator).
7. **Emit Result**: The `TingwuJobState.Completed` is emitted to the UI via Flow.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Interface & Fakes** | ✅ SHIPPED | `TingwuPipeline` interface, Models, and `FakeTingwuPipeline` for UI testing. |
| **2** | **OSS Integration** | ✅ SHIPPED | Wire the pipeline to rely firmly on the new `oss` feature for upstream URLs. |
| **3** | **Legacy Porting** | ✅ SHIPPED | Port the `TingwuRunner`, `TingwuSubmissionService`, and `TingwuApi` from `data/ai-core` into the new Prism structure. |
| **4** | **Artifact Wiring** | ✅ SHIPPED | Ensure `TingwuArtifactBundle` is correctly written to `SessionContext` / `MemoryCenter` for downstream Analyst consumption. |

## Open Loop Considerations

- The pipeline must handle network interruptions gracefully during the long polling phase.
- Custom Prompts must be strictly defined to ensure we don't ask Tingwu to do something the Analyst LLM should be doing later. Tingwu should extract *structure*, while Analyst should perform *reasoning*.
