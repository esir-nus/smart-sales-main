# Wave 19 T4 Plan: Uni-D Conflict-Visible Create

> **Behavioral Source**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Implementation Contract**:
> - `docs/cerb/scheduler-path-a-uni-d/spec.md`
> - `docs/cerb/scheduler-path-a-uni-d/interface.md`

## Goal

Deliver the `Uni-D` exact conflict universe as a first-class Path A branch:

- exact schedulable voice intent still commits when overlap exists
- the task persists with explicit conflict-visible state
- the scheduler UI shows caution treatment rather than clean exact-success treatment
- valid overlap never degrades into rejection

## T4 Scope

In scope:

- reuse of the existing exact semantic extraction lane
- deterministic conflict evaluation as the `Uni-A` / `Uni-D` split point
- conflict-visible exact-task persistence
- visible caution-state scheduler render
- suppression of later-lane scheduler mutation after a conflict-visible Path A commit

Out of scope:

- widening exact semantic extraction rules
- vague-task persistence
- inspiration capture
- automatic conflict resolution
- negotiation / suggestion UX
- reschedule behavior

## Execution Plan

### 1. Flow

Lock the runtime order:

1. bounded exact semantic extraction
2. deterministic conflict evaluation
3. clear exact -> `Uni-A`
4. overlapping exact -> `Uni-D`
5. later-lane handling only if no Path A commit exists

Key law:

- `Uni-D` is valid create-with-warning, not a failed create

### 2. Spec

Use the new owning shard as the only live T4 contract:

- `docs/cerb/scheduler-path-a-uni-d/spec.md`
- `docs/cerb/scheduler-path-a-uni-d/interface.md`

Before code, keep these details explicit inside the shard:

- minimal persisted conflict evidence beyond a bare boolean
- exact telemetry checkpoint names
- caution-over-clean-success UI rule

### 3. Code

Implement in this order:

1. keep the existing exact semantic extraction contract unchanged
2. change deterministic mutation so overlap persists instead of rejecting
3. preserve exact time while setting `hasConflict = true`
4. attach minimal conflict evidence needed for visible caution treatment
5. emit `PathACommitted` with the conflict-visible task
6. ensure scheduler UI renders caution state instead of clean exact success
7. keep later-lane scheduler mutation suppressed once conflict-visible Path A commit exists

### 4. PU Test

Add one-universe validation proving:

- exact conflicting input still creates one real task
- `task.id == unifiedId`
- `task.isVague == false`
- `task.hasConflict == true`
- exact time is preserved
- scheduler UI does not render the card as clean `Uni-A`
- no `NoMatch` / silent drop occurs for valid overlap

### 5. Fix Loop

Repair any drift where:

- overlap still returns rejection
- conflict-visible tasks do not carry enough evidence for UI caution treatment
- UI flattens `Uni-D` into clean success copy
- later-lane scheduler commands try to replace the already committed conflict-visible task

## Candidate Touch Points

Likely code seams:

- `core/pipeline/.../IntentOrchestrator.kt`
- `domain/scheduler/.../FastTrackMutationEngine.kt`
- scheduler task model / persistence seam if conflict evidence needs a new field
- scheduler UI render layer for caution treatment
- exact-path tests already covering `Uni-A` conflict rejection

## Exit Criteria

T4 is ready to close when:

- one exact conflicting utterance produces one persisted task
- the task carries conflict-visible state
- the scheduler surface shows caution treatment
- no valid overlapping intent is rejected outright
- targeted tests pass
- docs remain synced to delivered behavior

## Main Risk

The main T4 trap is legacy reject-on-conflict behavior surviving inside deterministic mutation.

The second trap is softer but still serious:

- the task persists with `hasConflict = true`
- but the UI still looks like clean `Uni-A`

So the decisive rule is:

- overlap must persist, and the warning must remain visible to the user
