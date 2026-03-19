# Scheduler Path A Uni-D Conflict-Visible Create (Wave 19 T4)

> **OS Layer**: System II / Scheduler fast-track conflict-visible exact-create universe
> **Scope**: `Uni-D` exact schedulable creation when overlap exists and the task must still persist with caution-state treatment
> **Status**: SHIPPED
> **Behavioral Authority Above This Doc**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Foundation Contract Below This Universe**: `docs/cerb/scheduler-path-a-spine/spec.md`

## Overview

`Uni-D` is the exact-but-conflicting universe.

It exists for voice input where:

- the user clearly intends to create a schedule item
- the utterance is semantically exact enough to resolve title and time
- deterministic board evaluation finds an overlap with an existing exact task
- the user's intent must still be preserved as a real task

`Uni-D` must not reject the task outright just because conflict exists.

## T4 Goal

Wave 19 T4 is complete only when an exact conflicting utterance can produce one real persisted task through the shared Path A spine with explicit conflict-visible state.

The required execution law is:

1. lightweight semantic extraction yields an exact-create payload
2. deterministic conflict evaluation checks overlap against the board
3. overlap moves the write into `Uni-D`, not into rejection
4. persistence stores a real task with conflict-visible state
5. the early Path A checkpoint returns a committed task that UI can render with caution treatment

## In Scope

- one exact schedulable voice utterance
- deterministic conflict evaluation
- conflict-visible persistence
- caution-state scheduler render
- strict separation from vague and inspiration semantics

## Explicitly Out of Scope

This shard does **not** define:

- clean exact create with no conflict (`Uni-A`)
- vague / missing-time create (`Uni-B`)
- inspiration capture (`Uni-C`)
- reschedule branches
- clarification loop behavior
- automatic conflict resolution
- suggestion or negotiation UX beyond visible caution treatment

If semantic extraction does not yield an exact create result, the system must exit `Uni-D`.
If no overlap exists, the task belongs to `Uni-A`, not `Uni-D`.

## Human Reality Constraint

Conflict does not erase intent.

Examples:

- `Meet with Zhang at 3pm` while another exact task already occupies 3pm
- `明天下午三点开会` while the slot is already booked

Users still asked to create something real.
The dangerous failure mode is silently rejecting the request or pretending it was never valid.

## Execution Law

`Uni-D` is not a separate semantic parse universe from `Uni-A`.

The delivered runtime should behave as:

1. exact semantic extraction runs once for exact-create candidacy
2. deterministic board evaluation decides clear vs overlap
3. clear exact create remains `Uni-A`
4. overlap exact create becomes `Uni-D`
5. the task still persists through the shared Path A spine

So the split between `Uni-A` and `Uni-D` happens at deterministic mutation time, not at the LLM contract boundary.

## Runtime Entry Law

`Uni-D` must not widen the meaning of exactness.

The runtime contract is:

- `Uni-A` exact-create extraction gets first right of refusal
- conflict evaluation always runs for exact-create candidates
- if overlap is absent, the task commits as `Uni-A`
- if overlap is present, the task commits as `Uni-D`
- overlap must not downgrade the result into `NoMatch`

`Uni-D` is therefore a persisted caution-state exact task, not a failed create.

## Conflict Persistence Law

For `Uni-D`, persistence must:

- write one real task row
- preserve `id = unifiedId`
- set `isVague = false`
- set `hasConflict = true`
- preserve the exact resolved time
- preserve enough overlap evidence for caution-state render, at minimum:
  - `conflictWithTaskId`
  - `conflictSummary`

`hasConflict = true` alone is not a sufficient long-term contract if the UI cannot explain why the card requires attention.

This is a create-with-warning universe, not a no-op universe.

## Success Contract

For `Uni-D`, success means:

- one exact scheduler task is persisted
- `task.id == unifiedId`
- `task.isVague == false`
- `task.hasConflict == true`
- UI renders the task with caution / conflict-visible treatment

`PipelineResult.PathACommitted` remains the early success result for this universe, but the committed task must already carry conflict-visible state.

`ConversationalReply` is not equivalent to conflict-visible success.
Generic clean-success copy is not sufficient for `Uni-D` if the consuming scheduler surface does not also show visible caution treatment.

## Ownership Rules

### IntentOrchestrator

Owns:

- bounded runtime entry into exact-create handling
- preserving `unifiedId`
- emitting `PathACommitted` for conflict-visible success
- preventing later-lane scheduler mutation from replacing the already committed exact conflict result

### Exact Semantic Extraction

Owns:

- recognizing exact schedulable intent
- producing one exact task payload
- declining non-exact input before conflict logic is considered

It does **not** decide `Uni-A` vs `Uni-D`.
That split belongs to deterministic conflict evaluation.

### ScheduleBoard / Deterministic Mutation

Own:

- overlap detection
- converting exact-create into clear exact or conflict-visible exact
- constructing the persisted overlap evidence from the detected overlap set
- persisting the final task state

Conflict check is mandatory here.
Reject-on-conflict is a behavioral violation for `Uni-D`.

Exact tasks with `durationMinutes == 0` are still conflict-participating.
They must be evaluated as point-in-time occupancy against exclusive slots rather than as silently non-conflicting empty intervals.
This shard does not permit inventing a fake default duration just to make overlap math work.

In the shipped T4 slice, deterministic mutation derives the caution payload from the first overlapping board item:

- `conflictWithTaskId = overlap.entryId`
- `conflictSummary = 与「<overlap.title>」时间冲突`

## Telemetry Contract

`Uni-D` must emit runtime evidence that distinguishes it from `Uni-A`:

- `TASK_EXTRACTED` for accepted exact extraction
- `CONFLICT_EVALUATED` with overlap result
- `DB_WRITE_EXECUTED` for persisted conflict-visible task state
- `PATH_A_DB_WRITTEN` for Path A write proof when used by the shared spine
- `UI_STATE_EMITTED` for foreground caution-state scheduler status
- `UI_RENDERED` for the conflict-visible scheduler card render

It must not reuse clean exact-create summaries that make a conflicted task look conflict-free.

## Verification Target

T4 verification should prove:

- exact conflicting input still creates one real task
- `hasConflict = true`
- the task is not downgraded to vague or inspiration state
- no `NoMatch` / silent drop occurs for valid conflicting intent
- the visible result is caution-state, not normal clean success
