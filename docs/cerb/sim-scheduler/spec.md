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
- the conflict-prioritized reminder ordering and date-target metadata used by the SIM shell dynamic island

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
- allow scheduler-drawer mic voice reschedule inside the scheduler-owned Path A lane
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
  - SIM now adopts the existing Android reminder/alarm stack only for persisted exact tasks
  - scheduler create / conflict / completion still stay on in-drawer status text in T4.8; no immediate native banner is emitted at mutation time
  - task-time reminders must reuse the shared alarm/notification stack rather than inventing a SIM-only duplicate
- **Urgency-driven reminder cascade**
  - reminder cadence must stay domain-owned by `UrgencyLevel.buildCascade(...)`
  - delivery tier must stay split by `CascadeTier`: EARLY banner vs DEADLINE full-screen alarm

Current repo evidence already covers part of this lane:

- calendar attention rendering exists in shared UI via `unacknowledgedDates` / `rescheduledDates`
- reschedule-ready card state already exists in shared UI models via `TimelineItem.Task.isExiting` and `exitDirection`
- legacy reminder infrastructure already exists in `RealAlarmScheduler`, `TaskReminderReceiver`, `AlarmActivity`, and `RealNotificationService`

Current SIM-specific state and remaining gaps:

- create and vague-create now mark off-page target dates into the reused calendar attention state
- conflict-visible create now reuses the amber warning-priority calendar channel while normal create remains blue
- multi-task create now aggregates attention per target date, so mixed batches may mark multiple dates independently
- single-task explicit relative-duration create now takes a deterministic scheduler-owned exact-create branch before model-led extraction when the transcript contains one lawful relative-time phrase and one remaining task body
- if that explicit relative-time phrase resolves to an exact time but stripping the phrase leaves no task body, SIM must safe-fail with scheduler-owned copy rather than falling through into Uni-C-style inspiration failure text
- for the remaining model-led create path, SIM now fronts create with `Uni-M` ordered multi-task decomposition before the single-task Uni-A / Uni-B / Uni-C chain
- `Uni-M` resolves fragments left-to-right; standalone explicit relative-duration fragments such as `N hours/minutes later`, `N小时后`, `N小时以后`, and `N小时之后` may anchor to `nowIso`, standalone `明天/后天 + clock` fragments may anchor to `nowIso` via day offsets, exact clock-relative fragments otherwise require a prior exact anchor, and clock-relative fragments after a vague date-only predecessor downgrade to vague when the day anchor remains lawful
- exact non-`FIRE_OFF` tasks without explicit duration now use domain-owned conflict occupancy windows for collision evaluation only; semantic transport/travel tasks may conflict even when persisted `durationMinutes = 0`
- `FIRE_OFF` reminders still bypass collision logic and therefore do not block or get blocked by other scheduled items
- reschedule path now drives the shared source-card exit-motion contract together with destination attention
- scheduler-drawer voice reschedule is an approved SIM scheduler lane when the transcript clearly implies a reschedule target plus a new exact time
- after one target is resolved, explicit day+clock phrases such as `明天早上8点` must resolve through scheduler-owned deterministic parsing before falling back to model-led exact-time extraction; this same exact-time rule also applies to the task-scoped follow-up reschedule lane
- SIM now reuses the shared reminder stack for persisted exact tasks only: create and conflict-create arm reminders, vague tasks do not, delete and mark-done cancel, reschedule cancels then rearms, and restore-from-done does not rearm in T4.8
- reminder-reliability prompting stays on the viewmodel/UI boundary through a process-lifetime gate so one create batch does not spam repeated settings prompts; the same seam may carry exact-alarm and OEM-specific notification hardening guidance
- SIM still defers immediate create/conflict/completion native notifications and still lacks device-level acceptance proof for full banner/deadline delivery

### Dynamic Island Reminder Projection Contract

The SIM scheduler remains the owner of dynamic-island reminder truth even though the shell owns the header presentation.

- reminder ordering must prefer conflict-visible tasks before normal reminders
- within the same priority tier, preserve scheduler ordering rather than letting the shell reshuffle tasks independently
- completed tasks must not appear
- each projected reminder must remain targetable to its corresponding scheduler date page
- the SIM shell may present at most the first 3 projected entries and rotate them vertically one line at a time
- the shared island renderer may still collapse one visible item at a time; the top-3 rotation is shell presentation, not scheduler-owned chrome

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

Wave 8 narrows this continuity into a task-scoped follow-up lane.

The delivered V1 follow-up context is:

- badge-origin thread id
- bound SIM chat session id
- bound task id list plus task summary snapshot
- last active shell surface through the shell-owned active binding

It is still not:

- scheduler-owned session memory
- open-ended scheduler assistant behavior
- a reason to widen `ISchedulerViewModel`

The current SIM implementation still starts or replaces this lane from `BadgeAudioPipeline.events` only when `PipelineEvent.Complete` yields `TaskCreated` or non-empty `MultiTaskCreated`.

Follow-up mutation ownership remains narrow:

- shell/chat may host prompting, task selection, and follow-up input
- scheduler-owned mutation truth still belongs to the scheduler task repository, conflict check, and reminder stack
- task-scoped follow-up reschedule may interpret explicit delta phrasing such as `推迟1小时` or `提前半小时`, but the offset must anchor to the selected task's current persisted start time rather than `nowIso`
- general SIM chat, audio drawer, and unrelated sessions do not inherit scheduler-drawer mutation rights from the scheduler mic lane

### Date Attention Contract

SIM reuses the shared scheduler calendar attention channels rather than widening the interface.

- off-page exact create and off-page vague create add their target dates to `unacknowledgedDates`
- off-page conflict-create adds its target date to both `unacknowledgedDates` and `rescheduledDates`, so the shared calendar renders the warning-priority amber treatment
- same-page create emits no calendar attention because the created task is already visible in the active timeline
- multi-task create aggregates attention per target date; any conflicting task on a date upgrades that date to the amber warning-priority channel
- first user tap on the affected date must acknowledge and clear both attention sets for that date

### Multi-Task Create Contract

SIM treats one multi-task utterance as an ordered batch of independent create tasks.

- `Uni-M` runs first and only for create decomposition; it must not widen delete/reschedule scope
- each persisted task still gets its own `unifiedId`
- one utterance-level batch id may be used for telemetry and residue reporting only
- fragments resolve left-to-right rather than by naive comma-splitting
- standalone explicit relative-duration phrases such as `N hours/minutes later`, `N小时后`, `N小时以后`, and `N小时之后` are lawful exact create by anchoring to `nowIso`
- standalone `明天/后天/tomorrow + clock` inside a multi-task batch should prefer deterministic `nowIso` day-offset anchoring rather than model-computed absolute dates
- exact clock-relative fragments are lawful only when a prior exact fragment exists in the same chain
- if a clock-relative fragment follows a vague date-only fragment, SIM must downgrade it to a vague task when the same lawful day anchor still exists rather than fabricating a clock time or silently dropping it
- partial success is valid: lawful fragments create tasks, unlawful fragments remain explicit residue in the aggregate batch status

### Native Reminder / Alarm Contract

SIM adopts the shared scheduler reminder infrastructure with a narrowed boundary.

- only persisted exact tasks schedule reminders
- vague tasks never schedule reminders
- conflict-persisted exact tasks still schedule reminders
- reminder cadence remains domain-owned by `UrgencyLevel.buildCascade(...)`
- scheduler create/conflict/completion do not emit immediate native notifications in T4.8; only task-time reminders use the native stack
- delete cancels the task reminder
- mark-done cancels the task reminder
- restore-from-done does not rearm the reminder in T4.8
- reschedule cancels the prior reminder and then arms the new exact-time reminder
- reminder-reliability prompting stays at the `ISchedulerViewModel` boundary and is gated to one prompt emission per process lifetime
- the delivered prompt must adapt to current OEM risk when possible: exact alarm, battery optimization, and OEM-specific lock-screen / floating / background-notification guidance should not be hardcoded as Xiaomi-only copy
- reminder scheduling failure must not roll back task persistence or batch success

### Scheduler-Drawer Voice Reschedule Contract

Scheduler-drawer voice reschedule is supported only within approved SIM scheduler scope.

- the owning entry surface is the scheduler drawer mic (`按住录音` in `SchedulerDrawer`)
- the runtime may resolve the target task through scheduler-owned confidence-gated matching rather than exact-title equality
- matching may use normalized title plus participant/location clues and scheduler-local context such as the current visible date page
- low-confidence or near-tie candidate resolution must safe-fail with explicit feedback and no mutation
- after one task is resolved, absolute exact-time phrasing should keep the delivered exact-time reschedule rules
- explicit day+clock phrasing such as `明天早上8点` must remain valid even when the new-time tail does not restate the task title
- explicit delta phrasing such as `推迟1小时` / `提前半小时` must anchor to the resolved task's current persisted start time rather than `nowIso`
- audio drawer, general SIM chat, and unrelated sessions must not reuse this lane as implicit scheduler authority

---

## 6. Data Shape Rules

SIM should prefer existing scheduler domain models where doing so does not reintroduce smart runtime coupling.

Minimum required projection:

- timeline items suitable for `SchedulerDrawer`
- conflict state
- active day offset
- ordered active reminder projection for shell dynamic island summary selection
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
