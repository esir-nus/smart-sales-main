# SIM Scheduler Spec

> **Scope**: Scheduler slice for the SIM standalone prototype
> **Status**: SPEC_ONLY
> **Behavioral Authority Above This Doc**:
> - `docs/core-flow/sim-scheduler-path-a-flow.md`
> - `docs/core-flow/scheduler-fast-track-flow.md`
> **Related Product Doc**: `docs/to-cerb/sim-standalone-prototype/concept.md`
> **Audit Evidence**: `docs/reports/20260319-sim-standalone-code-audit.md`

---

## 1. Purpose

`SIM Scheduler` is the scheduler implementation contract for the standalone simplified app.

Its job is to preserve the delivered Prism scheduler feel while narrowing execution to the requested minimum:

- Path A scheduling only
- no Path B memory highway dependency
- no smart-agent shell dependency

---

## 2. Behavioral Source

The SIM scheduler inherits behavior from the existing scheduler north star, but only within a reduced scope.

Inherited behavioral authority:

- create real scheduler items through Path A
- respect no-fabricated-time rules
- allow conflict-visible creation
- keep scheduler UI signals intact

Explicitly excluded from SIM V1:

- Path B memory enrichment
- CRM/entity enrichment
- generic plugin-driven writes
- smart shell-specific session behavior

---

## 3. Ownership

`SIM Scheduler` owns:

- scheduler drawer behavior in the standalone app
- Path A task creation/update/delete/reschedule UI flow
- prototype-local scheduler viewmodel boundary

`SIM Scheduler` reads from:

- `ScheduledTaskRepository`
- existing scheduler-owned data surfaces needed for Path A render

`SIM Scheduler` does not own:

- CRM memory
- Path B enrichment
- smart-agent session memory

---

## 4. UI Contract Direction

SIM should reuse the existing scheduler drawer UI and its child components whenever possible.

Allowed direct UI reuse target:

- `SchedulerDrawer`

Forbidden direct runtime reuse:

- current `SchedulerViewModel` as-is, because it still includes memory merging, inspiration shelf behavior, tips, and voice/orchestrator hooks beyond the SIM minimum

Expected replacement:

- `SimSchedulerViewModel : ISchedulerViewModel`

---

## 5. Scope for SIM V1

### Included

- open scheduler drawer
- view timeline
- create Path A tasks
- reschedule and delete in the delivered Path A-compatible way
- surface conflict-visible scheduler results

### Deferred

- inspiration shelf if it increases surface area without helping the standalone goal
- tips generation
- smart voice-to-scheduler badge flow as a required V1 dependency
- completed-memory merge model if it forces SIM to inherit extra runtime data dependencies

---

## 6. Data Shape Rules

SIM should prefer existing scheduler domain models where doing so does not reintroduce smart runtime coupling.

Minimum required projection:

- timeline items suitable for `SchedulerDrawer`
- conflict state
- active day offset
- exact alarm permission prompts only if reminders remain in SIM V1 scope

If a current scheduler field only exists for smart-app integration, SIM should not depend on it by default.

---

## 7. Human Reality

### Organic UX

The scheduler must still feel like Prism's scheduler.

That means:

- same drawer behavior
- same card family
- same conflict visibility principles

It does not require:

- the full scheduler feature backlog

### Data Reality

If keeping the current completed-memory merge forces SIM to depend on unrelated memory services, that is overbuild for this mission.

### Failure Gravity

The main failure is accidentally importing smart-only scheduler collaborators because the UI appears reusable.

The UI is reusable.
The runtime must still be narrowed.

---

## 8. Wave Plan

| Wave | Focus | Status | Deliverable |
|------|-------|--------|-------------|
| 1 | Path A-only scheduler contract | PLANNED | SIM scheduler spec/interface |
| 2 | Prototype scheduler VM | PLANNED | `SimSchedulerViewModel` |
| 3 | Drawer integration | PLANNED | existing `SchedulerDrawer` mounted in `SimShell` |
| 4 | Verification | PLANNED | Path A-only test slice and isolation proof |

---

## 9. Done-When Definition

SIM scheduler is ready only when:

- scheduler drawer runs in the standalone app
- task creation is Path A-backed
- no Path B or smart-memory dependency is required for normal operation
- conflict-visible behavior remains intact
- the smart app scheduler runtime has not been modified in place beyond safe shared UI reuse
