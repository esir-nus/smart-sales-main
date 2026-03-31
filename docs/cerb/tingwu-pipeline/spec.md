# Tingwu Pipeline Spec

> **Role**: Long-form audio artifact processing
> **Status**: Active supporting spec
> **Last Updated**: 2026-03-31
> **Purpose**: Define the source-led Tingwu processing lane used by audio management and optional audio-grounded chat context.
> **Base-Runtime Rule**: Tingwu belongs to the shared non-Mono audio lane. Deeper memory/entity/plugin augmentation remains Mono-only and must not redefine the source-led Tingwu contract.

## Overview

The Tingwu Pipeline is the long-form audio artifact engine.
It is separate from `asr-service`.

- `asr-service` is the short, fast speech-to-text lane used by flows such as scheduler voice ingress
- `tingwu-pipeline` is the long-form artifact lane used for transcript, chapters, summaries, speaker separation, keywords, and adjacent provider-returned structure

Current product posture:

- Tingwu is the source of truth for long-form audio intelligence
- local/UI layers may reorganize or readability-polish the returned artifacts
- local/UI layers must not invent unsupported facts or sections
- Tingwu output may later enrich chat context, but the pipeline itself is not a smart-agent orchestration lane

## Current Capability Contract

The current live request path may enable provider features such as:

- diarization
- provider identity recognition hints
- meeting-assistance / key-information style payloads
- custom prompt support when the request explicitly needs provider-side shaping
- chapters, summaries, and other native structuring features

Contract notes:

- metadata-derived scene/identity hints are advisory request context only
- provider-returned identity labels remain the upstream truth when present
- missing optional payloads must degrade back to transcript/diarization behavior instead of failing the whole job

## Logical Flow

1. Audio is captured or selected in a consumer flow outside Tingwu.
2. The consumer uploads the audio to OSS or otherwise obtains the provider-facing file URL.
3. `TingwuPipeline.submit()` creates the provider job.
4. `TingwuPipeline.observeJob()` tracks job progress until completion or failure.
5. When the provider job completes, the implementation fetches the available result artifacts.
6. The implementation persists source-led artifacts into the appropriate audio storage path for later drawer/chat use.
7. The consumer receives one completed artifact bundle and may render it directly, readability-polish it, or bind it into chat context.

## Persistence Rule

Tingwu persistence should be described in terms of audio artifact storage and consumer-visible reuse.

Current expectations:

- completed artifacts are stored so already-processed audio can be reopened without rerunning Tingwu by default
- shared audio-management consumers may reuse the stored artifacts later
- deeper memory/entity/plugin consumers are optional later augmentation, not the default explanation of this lane

## Current Consumer Boundary

Current active consumers for this lane are:

- audio drawer artifact rendering
- SIM namespaced audio repositories and persisted artifact reopen
- optional chat-context attachment that reuses the same stored Tingwu artifacts

This spec does **not** require:

- MemoryCenter-style persistence as the default explanation
- entity/plugin orchestration as the default consumer model
- smart-agent runtime ownership of Tingwu jobs
- Mono-only augmentation to explain normal non-Mono audio behavior

Mono may later augment this lane, but the augmentation must remain downstream of the source-led Tingwu contract defined here.

## Wave Summary

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Interface and fakes | ✅ Shipped | `TingwuPipeline` interface, models, and fake pipeline |
| **2** | OSS-backed submission | ✅ Shipped | Provider jobs rely on upstream OSS/public URL context |
| **3** | Provider integration port | ✅ Shipped | Current implementation ported from older ai-core assets into the Prism structure |
| **4** | Artifact persistence and reuse | ✅ Shipped | Source-led artifacts persist for later audio drawer and optional chat-context reuse |

## Open Loop Considerations

- handle polling/network interruptions gracefully during long-running jobs
- optional artifact download failures should be caught so transcript and other available artifacts still survive
- if identity output is absent or partial, keep diarization/raw speaker ids usable instead of inventing speaker roles
- custom prompt usage must stay narrow and must not smuggle smart-agent reasoning requirements into the provider request
- readability polishing belongs to downstream consumers and must preserve source-led truth
