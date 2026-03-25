# Tingwu Pipeline Spec

> **OS Layer**: SSD Storage (Writes directly to MetaHub/SessionMetadata)

## Overview

The Tingwu Pipeline is an intelligent audio processing agent. It is fundamentally different from the `asr-service`. While `asr-service` provides simple, quick Speech-to-Text (e.g., for Scheduler Voice inputs), the `tingwu-pipeline` handles comprehensive, long-form audio processing.

It leverages Aliyun Tingwu's native capabilities to perform:
1. **Diarization**: Speaker separation (e.g., Sales vs. Client).
2. **Identity Recognition**: Optional provider-side speaker-role recognition. The client may send user-metadata-derived `SceneIntroduction` and `IdentityContents` hints, but Tingwu still performs the final role judgment from the audio/result model rather than from a local hard-coded label map.
3. **Meeting Assistance / Key Information**: Optional provider-returned assistance payloads such as `Actions` and `KeyInformation`, which can be normalized into downstream keyword displays.
4. **Custom Prompts**: LLM extraction injected directly into the ASR phase.
5. **Auto-Chapters & Summarization**: Native structuring of long meetings.
6. **Text Polish**: Stutter removal and cleanup.

## CreateTask Request Contract

For the current SIM/live path, the Tingwu create-task request may include these capability switches:

- `Parameters.Transcription.DiarizationEnabled`
- `Parameters.Transcription.Diarization`
- `Parameters.IdentityRecognitionEnabled`
- `Parameters.IdentityRecognition.SceneIntroduction`
- `Parameters.IdentityRecognition.IdentityContents[]`
- `Parameters.MeetingAssistanceEnabled`
- `Parameters.MeetingAssistance.Types = ["Actions", "KeyInformation"]`

Contract notes:

- diarization and identity recognition are complementary rather than mutually exclusive
- user metadata may shape `SceneIntroduction` and `IdentityContents`, but this is advisory request context only
- provider-returned identity labels are the authoritative upstream speaker-role output when present
- missing optional identity/meeting-assistance payloads must degrade back to plain diarization/transcript behavior instead of failing the whole job

## Logical Flow

1. **Pre-requisite (External)**: Audio file is captured locally.
2. **OSS Upload (External)**: Audio is uploaded to OSS via the dedicated `oss` cerb feature. An `ossFileUrl` is obtained.
3. **Submission**: `TingwuPipeline.submit()` is called with the `ossFileUrl`. A task is created on Aliyun. The request may enable diarization, meeting assistance, and provider identity recognition, including metadata-derived scene/role hints for the current user context.
4. **Polling**: `TingwuPipeline.observeJob()` polls the API until completion.
5. **Artifact Fetching (Synchronous Batch)**: Upon completion, the pipeline downloads the available result JSONs/assets **concurrently but waits for all to finish** (`awaitAll`). Depending on the enabled request fields and returned links, this may include Summarization, AutoChapters, TextRewrite, Spectrum, MeetingAssistance, and IdentityRecognition.
6. **SSD Write**: The pipeline writes these intelligence artifacts directly into the database (MetaHub/SessionMetadata) so they are available for downstream tools (like Analyst Orchestrator). Normalized speaker labels and provider keywords may also be persisted alongside the source-led raw payloads.
7. **Emit Result**: A single, complete `TingwuJobState.Completed` is emitted to the UI via Flow. **(Dumb Data, Smart UI: The UI will handle "fake streaming" cosmetics, the pipeline provides the complete data instantly).**

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
- **Graceful Degradation**: If one optional artifact download fails (e.g., Aliyun networking blip), it must be caught and logged, allowing the rest of the artifacts (and the main transcript) to still be emitted instead of crashing the flow.
- **Identity Fallback**: If provider identity-recognition output is absent or incomplete, the pipeline should keep diarization/raw speaker ids usable instead of inventing local speaker roles.
