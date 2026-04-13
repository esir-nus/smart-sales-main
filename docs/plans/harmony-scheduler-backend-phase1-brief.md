# Harmony Scheduler Backend Phase 1 Brief

> **Purpose**: Bounded execution brief for the first Harmony scheduler backend/dataflow verification slice.
> **Status**: Active
> **Date**: 2026-04-08
> **Primary Tracker**: `docs/plans/harmony-tracker.md`
> **Behavioral Authority**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Implementation Contract**: `docs/platforms/harmony/scheduler-backend-first.md`

---

## Goal

Build one Harmony-owned backend verification sandbox that proves the scheduler dataflow can be observed and reasoned about before broad UI translation begins.

## In Scope

- Harmony-local scheduler models for verification
- operator-seeded scenario execution
- critical-joint telemetry
- local task/trace persistence
- minimal operator-facing debug surface

## Out of Scope

- production scheduler UI parity
- reminder/alarm adapter work
- onboarding scheduler integration
- badge-driven scheduler ingress
- broad natural-language completeness claims

## Current Mini-Lab Matrix

| Scenario | Expected Outcome |
|---|---|
| `EXACT_CREATE` | one active task snapshot, committed trace visible, ingress owner and owner chain visible |
| `VAGUE_CREATE` | one needs-time task snapshot, no fabricated due time, Path A commit visible |

## Minimum Evidence

- telemetry for ingress, classification, commit, and persistence
- persisted task snapshot that survives page reload
- latest Path A commit record with root and lineage identity
- visible ingress owner and owner chain
- explicit note that reminder/alarm behavior remains deferred

## Deferred Internal Scaffold

The current phase may keep these paths in code without exposing them in the operator UI:

- `CONFLICT_CREATE`
- `RESCHEDULE`
- `NULL_FAIL`

They remain deferred scaffold only until the proof scope is intentionally widened in docs and the operator surface.
