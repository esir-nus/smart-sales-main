# Project: scheduler-major-update

## Objective

Bring global rescheduling to feature-completeness against the user's mental model: support time-anchor retitle alongside the existing event-anchor time-shift, then handle follow-on parity sprints for onboarding sandbox, Harmony, and prompt/resolver hardening as evidence demands.

## Status

open

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | time-anchor-retitle | done | Scheduler-drawer Chinese debug inputs now exercise create and time-anchor retitle without follow-up chat; focused tests and replacement L3 logcat are green. | [sprints/01-time-anchor-retitle.md](sprints/01-time-anchor-retitle.md) |
| 02 | cancel-phrase-reschedule-delta | done | Cancel wording plus a same-time replacement event now routes as replacement/reschedule; pure cancellation stays unsupported. | [sprints/02-cancel-phrase-reschedule-delta.md](sprints/02-cancel-phrase-reschedule-delta.md) |

## Cross-Sprint Decisions

- Sprint 01 targets Android on `develop`; onboarding sandbox parity and Harmony delivery are deferred.
- Replacement semantics are preserved: retitle uses `rescheduleTask(oldTaskId, newTask)` with the same time and duration, not a new field-patch path.
- Sprint 02 keeps pure cancellation unsupported while treating cancel wording plus a replacement event at the same time anchor as replacement/reschedule.

## Lessons Pointer

`.agent/rules/lessons-learned.md` — no entries yet for this project.
