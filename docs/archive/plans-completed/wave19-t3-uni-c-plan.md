# Wave 19 T3 Plan: Uni-C Inspiration

> **Behavioral Source**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Implementation Contract**:
> - `docs/cerb/scheduler-path-a-uni-c/spec.md`
> - `docs/cerb/scheduler-path-a-uni-c/interface.md`

## Goal

Deliver the `Uni-C` timeless-intent universe as a first-class Path A branch:

- timeless voice intent is captured as inspiration
- no scheduler task row is written
- no conflict logic runs
- the visible result lands outside the scheduler timeline lane

## T3 Scope

In scope:

- bounded `Uni-C` semantic extraction after `Uni-A` and `Uni-B` decline
- inspiration-only DTO contract and linter decode
- deterministic inspiration persistence
- explicit non-scheduler success result
- non-calendar render path

Out of scope:

- any scheduler task creation fallback
- vague-task persistence
- conflict-visible scheduling
- reschedule behavior
- Path B enrichment
- CRM/entity mutation from inspiration capture

## Execution Plan

### 1. Flow

Lock the runtime order:

1. `Uni-A` exact-create attempt
2. `Uni-B` vague-create attempt
3. `Uni-C` inspiration attempt
4. later-lane handling only if all three decline

Key law:

- `Uni-C` is not a scheduler recovery bucket
- it is only for timeless intent

### 2. Spec

Use the new owning shard as the only live T3 contract:

- `docs/cerb/scheduler-path-a-uni-c/spec.md`
- `docs/cerb/scheduler-path-a-uni-c/interface.md`

Before code, tighten any missing details only inside that shard:

- exact foreground success result name
- inspiration surface owner
- telemetry checkpoint names if new ones are required

### 3. Code

Implement in this order:

1. narrow `:domain` `Uni-C` extraction contract
2. prompt/linter mechanical alignment for timeless inspiration routing
3. `IntentOrchestrator` sequential branch wiring after `Uni-B`
4. deterministic inspiration write path through the inspiration owner
5. explicit suppression of scheduler mutation once `Uni-C` accepts
6. UI mapping to a non-calendar inspiration result

### 4. PU Test

Add one-universe validation proving:

- timeless input becomes one inspiration artifact
- no scheduler task row is created
- no conflict check is run
- scheduler UI does not render it as exact, vague, or conflict-visible task state
- success is backed by inspiration persistence proof, not generic conversational reply

### 5. Fix Loop

Repair any drift where:

- timeless input still leaks into task-table mutation
- scheduler UI treats inspiration as task success
- later-lane scheduler commands revive scheduling after `Uni-C` has already accepted

## Candidate Touch Points

Likely code seams:

- `core/pipeline/.../IntentOrchestrator.kt`
- `core/pipeline/.../PromptCompiler.kt`
- `core/pipeline/.../RealUniCExtractionService.kt`
- `domain/scheduler/.../SchedulerLinter.kt`
- inspiration owner / repository seam
- UI result mapping layer for inspiration confirmation

## Exit Criteria

T3 is ready to close when:

- one timeless voice utterance produces one inspiration artifact
- zero scheduler tasks are created for that utterance
- the result renders outside the scheduler timeline lane
- targeted tests pass
- docs remain synced to delivered behavior

## Main Risk

The main T3 trap is false usefulness:

- if the system cannot confidently schedule it, it may try to “save something” as a vague task
- that would corrupt the `Uni-B` / `Uni-C` boundary and reopen task-table bleed

So the decisive rule is:

- timeless intent must remain inspiration-only, even when the system is tempted to turn it into a scheduler object
