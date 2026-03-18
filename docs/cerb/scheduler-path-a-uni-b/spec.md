# Scheduler Path A Uni-B Vague Creation (Wave 19 T2)

> **OS Layer**: System II / Scheduler fast-track vague-create universe
> **Scope**: `Uni-B` schedulable creation when date anchor exists but exact time does not
> **Status**: IN_PROGRESS (date-anchored slice delivered)
> **Behavioral Authority Above This Doc**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Foundation Contract Below This Universe**: `docs/cerb/scheduler-path-a-spine/spec.md`

## Overview

`Uni-B` is the next real Path A universe after `Uni-A`.

It exists for schedulable input where:

- the user clearly intends to create a schedule item
- the utterance provides enough semantic structure to create a real scheduler card
- but the time is still unresolved, incomplete, or only partially constrained

`Uni-B` must persist that intent without fabricating exact time.

## Current T2 Slice

The delivered T2 slice is intentionally bounded to **date-anchored vague create**.

That means the current implementation may commit `Uni-B` when:

- the utterance yields a real date anchor
- but not a safely exact time

Examples:

- `三天以后提醒我开会`
- `明天下午提醒我打电话`
- `后一天找 Frank 聊报价`

This slice does **not** yet commit fully undated vague requests such as `schedule team standup`, because current scheduler persistence still requires a day anchor and Core Flow forbids silently inventing one.

## Date-Only Law

Date-only requests belong to `Uni-B`, not `Uni-A`.

Examples:

- `明天提醒我打电话`
- `tomorrow remind me to go to the airport`
- `后一天提醒我吃饭`

For these inputs:

- preserve the date anchor
- do not fabricate exact clock time
- persist a vague task with `isVague = true`

## Execution Law

The required execution law is:

1. `Uni-A` gets first right of refusal for exact-create
2. if `Uni-A` exits without exact commit, a bounded `Uni-B` semantic pass may run
3. `Uni-B` may commit only when:
   - schedulable intent is real
   - a real day anchor exists
   - exact time is still unresolved
4. conflict check must be bypassed
5. the task must persist in explicit vague state
6. UI must render an awaiting-time scheduler card, not an exact-slot card

## Human Reality Constraint

Users often speak in a way that fixes the day before fixing the time.

Examples:

- `三天以后提醒我开会`
- `周五找法务聊一下`
- `明天下午给客户回电话`

That is not a failed schedule intent. It is a valid schedulable intent with unresolved time.

The dangerous failure mode is turning that into a fake exact timestamp.

## Runtime Entry Law

`Uni-B` is entered only after `Uni-A` declines exact-create.

The runtime contract is:

- `Uni-A` exact-create attempt
- if `Uni-A` returns `NotExact`, try bounded `Uni-B`
- if `Uni-B` returns vague-create, commit Path A with explicit vague semantics
- if `Uni-B` does not return vague-create, fall through to later-lane handling

`Uni-B` must not be entered in parallel with `Uni-A`, and it must not widen `Uni-A` by stealth.

## Success Contract

For `Uni-B`, success means:

- one vague scheduler task is persisted
- `task.id == unifiedId`
- `task.isVague == true`
- `task.hasConflict == false`
- exact overlap math was not run as if the task had an exact time
- the task renders as an awaiting-time scheduler card

`PipelineResult.PathACommitted` remains the early success result for this universe, but the committed task is explicitly vague rather than exact.

## Conflict Bypass Law

Core Flow is strict here:

- unresolved-time tasks must not be checked against the board as if exact
- `Uni-B` therefore bypasses conflict evaluation entirely
- no exact-slot conflict warning should be generated from vague-create itself

This is not a best-effort choice. It is a behavioral law.

## Persistence Contract

The current implementation encodes vague state in the existing task table by persisting:

- a real scheduler task row
- `isVague = true`
- a date anchor used for day-bucket query/render
- no fabricated exact user-facing time

The day anchor is a storage/query anchor, not an exact schedule truth.
UI must not display it as if it were a resolved clock time.

## Telemetry Contract

`Uni-B` must emit distinct runtime evidence:

- `TASK_EXTRACTED_VAGUE` when the vague task is semantically accepted
- `DB_WRITE_EXECUTED` when the vague task is persisted
- `PATH_A_DB_WRITTEN` when Path A write proof exists
- `UI_STATE_EMITTED` when the awaiting-time card appears in scheduler UI

It must not reuse exact-create summaries that make a vague task look like `Uni-A`.

## Ownership Rules

### IntentOrchestrator

Owns:

- sequential `Uni-A` -> `Uni-B` runtime branching
- preserving `unifiedId`
- emitting `PathACommitted` for vague success
- falling through to later-lane handling when vague-create is not committed

### Uni-B Semantic Extraction

Owns:

- recognizing schedulable-but-not-exact input
- requiring a real day anchor
- refusing commit when no day anchor exists
- extracting title + day anchor + optional time hint

### SchedulerLinter

Owns:

- strict decode of the `Uni-B` serializer-backed contract
- validating title is non-blank
- validating anchor date is parseable
- refusing malformed vague payloads

### FastTrackMutationEngine

Owns:

- bypassing conflict check for `Uni-B`
- persisting one task in explicit vague state
- preserving `id = unifiedId`

## Verification Target

T2 verification should prove:

- date-anchored schedulable vague input becomes a persisted vague task
- no fabricated exact ISO time is claimed as user-facing truth
- conflict check is bypassed
- the task appears as a red-flagged / awaiting-time card
- `Uni-A` exact-create telemetry is not reused as if vague were exact
