# Scheduler Path A Spine (Wave 19 T0)

> **OS Layer**: System II / Scheduler hand-off spine
> **Scope**: Shared Path A entry routing and early optimistic commit for scheduler voice input
> **Status**: SHIPPED for T0
> **Behavioral Authority Above This Doc**: `docs/core-flow/scheduler-fast-track-flow.md`

## Overview

This shard defines the delivered T0 scheduler spine that all later Path A universes must pass through.

Its job is architectural, not semantic-completion work:

- unify entry ownership
- allocate stable scheduler thread identity
- perform one early Path A commit through one owner chain
- let Path B continue independently afterwards

This shard is intentionally narrower than later universe specs.
It does **not** define `Uni-A` exact semantic understanding.

## T0 Goal

Wave 19 T0 is complete only when scheduler voice input no longer has two live write paths.

The delivered owner chain is:

1. `RealBadgeAudioPipeline` owns recording, download, ASR, cleanup, and pipeline events
2. `IntentOrchestrator` owns the shared Path A scheduler spine
3. `ScheduledTaskRepository` owns the actual task persistence
4. `PipelineResult.PathACommitted` is the early checkpoint emitted after the Path A write lands

## In Scope

- badge-audio transcript delegation into the shared spine
- `unifiedId` allocation in `IntentOrchestrator`
- one early optimistic scheduler write for voice schedulable traffic
- `PathACommitted` emission for the drawer/UI completion checkpoint
- continued background Path B collection after the early commit
- clarification-state write-back onto the already-created Path A task

## Explicitly Out of Scope

T0 does **not** claim:

- exact semantic title/time extraction
- conflict-complete universe handling
- vague-task universe correctness
- reschedule universe correctness
- lightweight-model semantic parsing

Those belong to later universe work.

## Delivered Execution Flow

```text
[Badge Audio / Voice Transcript]
        |
        v
[IntentOrchestrator.processInput(input, isVoice = true)]
        |
        v
[unifiedId allocated]
        |
        v
[FastTrackParser placeholder optimistic task]
        |
        v
[ScheduledTaskRepository.upsertTask]
        |
        v
[PipelineResult.PathACommitted(task)]
        |
        +--> [BadgeAudioPipeline completes drawer-facing path]
        |
        +--> [UnifiedPipeline / Path B continues in background]
```

## Ownership Rules

### IntentOrchestrator

`IntentOrchestrator` is the single live Path A writer for T0 scheduler traffic.

It owns:

- route classification at the phase-0 gateway
- `unifiedId` minting
- optimistic task creation trigger
- persistence hand-off into `ScheduledTaskRepository`
- `PathACommitted` emission
- clarification-state updates when downstream Path B yields disambiguation or clarification results

It must not share live Path A write ownership with `RealBadgeAudioPipeline`.

### RealBadgeAudioPipeline

`RealBadgeAudioPipeline` is a transport/lifecycle component, not a scheduler writer.

It owns:

- badge recording detection
- WAV download
- ASR transcription
- completion/error events
- badge/local cleanup

It delegates scheduler creation to `IntentOrchestrator` and completes its foreground path on the first `PathACommitted`.

### FastTrackParser

In T0, `FastTrackParser` is only a placeholder optimistic-task constructor.

It is **not** the semantic source of truth for exact time understanding.
Do not read this shard as approval for Kotlin heuristics to implement later universes.

### ScheduledTaskRepository

`ScheduledTaskRepository` is the T0 persistence owner for the early Path A task.

The T0 guarantee is:

- one task upsert through the shared spine
- stable `id = unifiedId`
- early persistence before Path B completes

## Required T0 Result

The shared spine must emit:

```kotlin
PipelineResult.PathACommitted(task: ScheduledTask)
```

Meaning:

- the task has already been persisted through the shared Path A spine
- the result is an early commit checkpoint, not a promise of final semantic enrichment
- downstream Path B may still add clarification state or other follow-up effects later

## Non-Goals and Warnings

- Do not implement exact semantic understanding with hardcoded Kotlin regex or time heuristics under the banner of T0.
- Do not let badge audio regain direct scheduler write ownership.
- Do not treat `PathACommitted` as proof that `Uni-A` exact creation is solved.

## Verification Target

T0 verification should prove:

- one shared owner chain exists
- badge audio no longer writes scheduler tasks directly
- `PathACommitted` is emitted after persistence
- downstream Path B still continues independently
- clarification state can still attach to the existing Path A task
