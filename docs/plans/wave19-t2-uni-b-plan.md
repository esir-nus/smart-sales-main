# Wave 19 T2 Plan: Uni-B Vague Creation

> **Wave**: 19
> **Universe**: `Uni-B`
> **Behavioral Authority**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Foundation Below**: `docs/cerb/scheduler-path-a-spine/spec.md`
> **Dependency Above**: shipped `Uni-A` exact-create foundation

## Goal

Deliver the next Path A universe after `Uni-A`:

- the user clearly intends to schedule something
- the utterance is schedulable
- but the time is not exact enough for normal create

`Uni-B` must persist that intent without fabricating exact time.

## Scope

`Uni-B` covers inputs like:

- date-only intent
- part-of-day intent
- unresolved-time intent
- relative-day intent that is schedulable but still not exact enough to land in `Uni-A`

Examples:

- `三天以后提醒我开会`
- `明天下午提醒我打电话`
- `周五找 Frank 聊报价`

## Non-Goals

This plan does not include:

- exact-create semantics already owned by `Uni-A`
- conflict-visible exact creation owned by `Uni-D`
- inspiration-only timeless capture owned by `Uni-C`
- reschedule branches
- CRM candidate enrichment loops

## Delivery Order

1. Flow
- Lock the exact `Uni-B` branch in Core Flow terms:
  schedulable intent exists, but exact time does not.
- Preserve the law that `Uni-B` is a valid scheduler outcome, not a failed `Uni-A`.

2. Spec
- Create a dedicated Cerb shard at `docs/cerb/scheduler-path-a-uni-b/`.
- Define:
  - vague-task input envelope
  - persistence contract
  - UI render contract
  - telemetry checkpoints
  - explicit boundaries against `Uni-A` and `Uni-D`

3. Code
- Introduce a narrow semantic extraction contract for vague-create, separate from `Uni-A`.
- Persist a real scheduler task with unresolved-time semantics instead of faking an exact ISO datetime.
- Keep `PathACommitted` and scheduler success semantics aligned with the delivered vague-task contract.

4. PU Test
- Add one-universe tests proving:
  - schedulable but not-exact input becomes a persisted vague task
  - no fabricated exact time is written
  - the task renders with red-flag / awaiting-time treatment
  - no exact-create telemetry is emitted for `Uni-B`

5. Fix Loop
- Repair any drift between Core Flow, the `Uni-B` shard, and the delivered UI treatment before starting `Uni-C`.

## Main Design Law

`Uni-B` must treat vague scheduling as first-class persisted intent.
It must not:

- silently discard schedulable vague input
- silently invent exact time
- disguise `Uni-B` as `Uni-A`

## Entry Risk

The main trap is accidentally widening `Uni-A` so date-only or part-of-day input gets forced into fake exactness.

`Uni-B` should be built as its own universe, not as a permissive exception list inside `Uni-A`.
