# Project: scheduler-major-update

## Objective

Bring global rescheduling to feature-completeness against the user's mental model: support time-anchor retitle alongside the existing event-anchor time-shift, then handle follow-on parity sprints for onboarding sandbox, Harmony, and prompt/resolver hardening as evidence demands.

## Status

open

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | time-anchor-retitle | done | Code, docs, focused tests, debug build, and on-device time-anchor retitle logcat evidence are complete. | [sprints/01-time-anchor-retitle.md](sprints/01-time-anchor-retitle.md) |

## Cross-Sprint Decisions

- Sprint 01 targets Android on `develop`; onboarding sandbox parity and Harmony delivery are deferred.
- Replacement semantics are preserved: retitle uses `rescheduleTask(oldTaskId, newTask)` with the same time and duration, not a new field-patch path.

## Lessons Pointer

`.agent/rules/lessons-learned.md` — no entries yet for this project.
