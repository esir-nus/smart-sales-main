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
- connectivity state or badge management behavior

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
- keep inspiration shelf visible in the reused scheduler surface
- treat shelf-card `Ask AI` as a plain chat-session launcher that seeds the first user turn with the inspiration text and auto-submits it
- create Path A tasks
- reschedule and delete in the delivered Path A-compatible way
- surface conflict-visible scheduler results

### Shipping Hardening Carry Lane

The baseline Path A slice is in place, but SIM scheduler is not yet shipping-complete.

The remaining scheduler hardening requirements are:

- **Date attention signaling**
  - when create or reschedule lands on a date different from the currently viewed page, the target date must enter an attention state in the calendar
  - conflict-visible create must use a warning/error attention treatment distinct from normal create
  - first user tap on that date acknowledges the attention state and stops the blink/glow
- **Reschedule motion semantics**
  - reschedule must show that the source card moved rather than simply vanished
  - source-card exit direction should reflect whether the task moved toward a future or past date
  - destination date must also receive attention signaling
- **Android-native banner and reminder integration**
  - scheduler create / conflict feedback should deepen beyond short toast-level status
  - task-time reminders should integrate with the existing Android notification and alarm stack rather than inventing a SIM-only duplicate
- **Urgency-driven reminder cascade**
  - reminder cadence must stay domain-owned by `UrgencyLevel.buildCascade(...)`
  - delivery tier must stay split by `CascadeTier`: EARLY banner vs DEADLINE full-screen alarm

Current repo evidence already covers part of this lane:

- calendar attention rendering exists in shared UI via `unacknowledgedDates` / `rescheduledDates`
- reschedule-ready card state already exists in shared UI models via `TimelineItem.Task.isExiting` and `exitDirection`
- legacy reminder infrastructure already exists in `RealAlarmScheduler`, `TaskReminderReceiver`, `AlarmActivity`, and `RealNotificationService`

Current SIM-specific gaps:

- create and vague-create now mark off-page target dates into the reused calendar attention state
- conflict-visible create now reuses the amber warning-priority calendar channel while normal create remains blue
- multi-task create now aggregates attention per target date, so mixed batches may mark multiple dates independently
- reschedule path marks the destination date but does not yet drive the source-card motion contract
- SIM has not yet deliberately adopted the legacy reminder/notification stack as part of its shipped scheduler contract

### Deferred

- inspiration timeline/multi-select AI behavior is deprecated in SIM V1; only the shelf-card `Ask AI` launcher remains valid
- deeper inspiration shelf upgrades beyond the base shelf-card `Ask AI` chat-launch equivalence
- tips generation
- smart voice-to-scheduler badge flow as a required V1 dependency
- completed-memory merge model if it forces SIM to inherit extra runtime data dependencies
- deeper badge-origin scheduler follow-up intelligence beyond the shell continuity binding
- physical-badge-specific L3 proof for the continuity lane while hardware remains unavailable

### SIM Shell Continuity Boundary

If SIM needs cross-surface continuity for a badge-origin scheduler follow-up thread, that continuity is owned by `SIM Shell`, not by `SIM Scheduler`.

In the current implementation this continuity is metadata-only:

- badge-origin thread id
- bound SIM chat session id
- last active shell surface

It is not:

- scheduler-owned session memory
- proof that SIM chat already carries real scheduler follow-up semantics
- a reason to widen `ISchedulerViewModel`

The current SIM implementation starts or replaces this shell-owned binding from `BadgeAudioPipeline.events` only when `PipelineEvent.Complete` yields `TaskCreated` or non-empty `MultiTaskCreated`.

### Date Attention Contract

SIM reuses the shared scheduler calendar attention channels rather than widening the interface.

- off-page exact create and off-page vague create add their target dates to `unacknowledgedDates`
- off-page conflict-create adds its target date to both `unacknowledgedDates` and `rescheduledDates`, so the shared calendar renders the warning-priority amber treatment
- same-page create emits no calendar attention because the created task is already visible in the active timeline
- multi-task create aggregates attention per target date; any conflicting task on a date upgrades that date to the amber warning-priority channel
- first user tap on the affected date must acknowledge and clear both attention sets for that date

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

Connectivity is also out of scheduler scope:

- scheduler must not import `ConnectivityBridge`, `ConnectivityService`, or badge connection state as part of its baseline runtime
- scheduler create/delete/reschedule behavior must remain meaningful when connectivity is absent or offline

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
- create/reschedule attention states make the affected date obvious and user-acknowledgeable
- reschedule motion makes the move visible rather than implicit
- reminder and notification behavior is explicitly aligned with the existing alarm/cascade model
- the smart app scheduler runtime has not been modified in place beyond safe shared UI reuse
