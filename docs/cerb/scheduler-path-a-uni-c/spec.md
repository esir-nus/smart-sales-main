# Scheduler Path A Uni-C Inspiration (Wave 19 T3)

> **OS Layer**: System II / Scheduler fast-track inspiration universe
> **Scope**: `Uni-C` timeless intent routing that must never write into the scheduler task table
> **Status**: SHIPPED
> **Behavioral Authority Above This Doc**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Foundation Contract Below This Universe**: `docs/cerb/scheduler-path-a-spine/spec.md`

## Overview

`Uni-C` is the timeless-intent universe.

It exists for voice input where:

- the user is expressing a thought, aspiration, or note
- the utterance should be preserved
- but the utterance is not yet a schedulable commitment

`Uni-C` must persist that content only to inspiration storage.
It must not degrade into a vague scheduler task, and it must not silently bleed into the task table.

## T3 Goal

Wave 19 T3 is complete only when timeless intent can produce one real inspiration artifact through the shared Path A spine without pretending to be a schedule item.

The required execution law is:

1. bounded semantic extraction decides whether the utterance belongs to `Uni-C`
2. the core idea is extracted into an inspiration payload
3. deterministic mutation persists only to inspiration storage
4. the early Path A checkpoint returns a real inspiration result, not a task-shaped placeholder

## In Scope

- one timeless voice utterance
- inspiration-only semantic extraction
- inspiration storage persistence
- non-calendar render path
- strict separation from scheduler task mutation

## Explicitly Out of Scope

This shard does **not** define:

- exact schedulable create (`Uni-A`)
- vague / needs-time create (`Uni-B`)
- conflict-visible create (`Uni-D`)
- reschedule branches
- clarification loop behavior
- Path B enrichment
- CRM/entity mutation from inspiration capture

If semantic extraction does not yield timeless intent, the system must exit `Uni-C`.
It must not save inspirational language as a scheduler card just because the user said it through the same voice entry.

## Human Reality Constraint

Users often speak aspirations and reminders in a loose, emotional, or reflective way:

- `I should really learn to play guitar someday`
- `以后想练口语`
- `记一下这个想法`

Those are valid capture events, but they are not schedulable commitments.

The dangerous failure mode is turning reflective intent into a task-shaped object just because the system wants every voice turn to look “productive”.

## Execution Flow

```text
[Voice Transcript]
        |
        v
[IntentOrchestrator shared Path A spine]
        |
        v
[Bounded Uni-C runtime attempt]
        |
        v
[Lightweight semantic inspiration extraction]
        |
        +--> [NotInspiration / NotUniC] ---> exit Uni-C branch
        |                                   and continue to later-universe handling without inspiration success
        |
        v
[Inspiration DTO validation / normalization]
        |
        v
[Deterministic inspiration persistence]
        |
        v
[InspirationRepository write only]
        |
        v
[PathA inspiration commit result]
        |
        v
[Distinct inspiration render outside scheduler timeline]
```

## Runtime Entry Law

`Uni-C` must not run in parallel with `Uni-A` or `Uni-B`.

The runtime contract is:

- exact schedulable interpretation gets first refusal through `Uni-A`
- vague schedulable interpretation gets second refusal through `Uni-B`
- only after schedulable task universes decline may the bounded `Uni-C` attempt run
- if `Uni-C` accepts, Path A commits inspiration-only persistence
- if `Uni-C` declines, the spine continues to later-lane handling without claiming inspiration success

`Uni-C` must not become a convenient bucket for “scheduler parsing failed, save something anyway”.

## Non-Schedulable Mutation Law

Core Flow is strict here:

- timeless intent is not a task
- `Uni-C` must halt schedulable mutation flow
- no scheduler task row may be written
- no conflict check may run
- no Path B enrichment may be triggered from this branch

Inspiration landing in the task table is a blocking regression.

## Success Contract

For `Uni-C`, success means:

- one inspiration artifact is persisted
- no scheduler task is written
- no conflict-visible state is created
- UI renders the result outside the normal scheduler timeline lane

The delivered foreground success result for this universe is:

```kotlin
PipelineResult.InspirationCommitted(
    id = itemId,
    content = extractedContent
)
```

It must not masquerade as a scheduler task commit.

## Ownership Rules

### IntentOrchestrator

Owns:

- sequential `Uni-A` -> `Uni-B` -> `Uni-C` runtime branching
- preserving `unifiedId`
- handing inspiration payloads into the inspiration owner
- blocking any scheduler-task mutation once `Uni-C` is accepted

### Uni-C Semantic Extraction

Owns:

- recognizing timeless intent
- refusing schedulable intent
- extracting the core idea only
- declining when the utterance is better handled by later-lane conversational logic
- requiring non-empty inspiration content in the prompt contract, with a worked example for `以后想学吉他`
- emitting extractor-side evidence for raw Uni-C JSON and extracted content length

### SchedulerLinter

Owns:

- strict decode of the serializer-backed `Uni-C` contract
- validating that inspiration payloads are structurally sound
- falling back from `idea.content` to `idea.title` to the raw transcript so legacy/partial extractor output does not persist blank inspiration text
- refusing malformed or task-shaped payloads

### Inspiration Repository / Owner

Owns:

- the only real write path for `Uni-C`
- persistence of the extracted idea
- safe recovery to empty state when persistence JSON is corrupt
- storage semantics that remain separate from scheduler tasks

## Persistence Contract

`Uni-C` persistence must:

- write only to inspiration storage
- preserve `unifiedId` or equivalent correlation token if needed for tracing
- avoid any task-table mirror row

Lower layers may choose different tables or domain types.
The behavior must remain:

- inspiration stored
- scheduler task table untouched

## UI Contract

`Uni-C` must render outside the scheduler timeline lane.

Acceptable render shapes include:

- an inspiration card
- a note-style capture confirmation
- a distinct non-calendar drawer item

Unacceptable render shapes include:

- a normal scheduler card
- a vague/awaiting-time scheduler card
- a conflict-visible scheduler card

## Telemetry Contract

`Uni-C` should emit distinct runtime evidence:

- `THOUGHT_EXTRACTED`
- extractor raw JSON plus extracted inspiration content length at the extractor -> linter joint
- `PATH_A_DB_WRITTEN` for inspiration storage
- `UI_STATE_EMITTED` for the inspiration lane / confirmation state

It must not reuse scheduler create summaries that make inspiration look like `Uni-A` or `Uni-B`.

## Verification Target

T3 verification should prove:

- timeless input becomes one inspiration artifact
- legacy/partial extractor output falls back to the raw transcript instead of persisting blank inspiration text
- no scheduler task row is created
- no conflict check is run
- no Path B enrichment branch is activated
- the visible result renders outside the scheduler timeline
