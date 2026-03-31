# SIM Wave 4 Execution Brief

**Status:** In Progress
**Date:** 2026-03-20
**Wave:** 4
**Mission:** SIM standalone prototype
**Behavioral Authority:** `docs/core-flow/sim-scheduler-path-a-flow.md`
**Companion Core Flow:** `docs/core-flow/scheduler-fast-track-flow.md`
**Current Reading Priority:** Historical reference only; not current source of truth.
**Historical Owning Spec At The Time:** `docs/cerb/sim-scheduler/spec.md`
**Historical Boundary Brief:** `docs/plans/sim_implementation_brief.md`
**Current Active Truth:** `docs/plans/tracker.md`, `docs/core-flow/sim-scheduler-path-a-flow.md`, `docs/core-flow/scheduler-fast-track-flow.md`, `docs/cerb/scheduler-path-a-spine/spec.md`, `docs/cerb/scheduler-path-a-spine/interface.md`, `docs/cerb-ui/scheduler/contract.md`, `docs/cerb/interface-map.md`

---

## 1. Purpose

This brief compresses Wave 4 into one practical handoff artifact.

Use it when implementing the SIM scheduler slice, reviewing the SIM scheduler runtime, and validating that SIM keeps Path A truth without dragging the smart scheduler runtime back into the standalone app.

This is not the full scheduler product spec.
It is the execution brief for the Wave 4 SIM scheduler lane.

---

## 2. Required Read Order

Before coding Wave 4, read in this order:

1. `docs/plans/tracker.md`
2. `docs/core-flow/sim-scheduler-path-a-flow.md`
3. `docs/core-flow/scheduler-fast-track-flow.md`
4. `docs/cerb/scheduler-path-a-spine/spec.md`
5. `docs/cerb/scheduler-path-a-spine/interface.md`
6. `docs/cerb-ui/scheduler/contract.md`
7. `docs/cerb/interface-map.md`
8. this file as historical execution context

If code reality forces a boundary or ownership change, update the main tracker and the current shared scheduler docs in the same session.

---

## 3. Wave Objective

Wave 4 exists to make the SIM scheduler real as a standalone Path A lane.

Wave 4 must deliver:

- repository-backed SIM scheduler timeline
- real Path A task creation without reusing the smart `SchedulerViewModel`
- delete and reschedule handling inside the SIM scheduler seam
- conflict-visible scheduler results
- explicit safe-fail feedback for bad target branches
- a dev/test mic entry that routes into the same scheduler execution seam as the real badge-origin path

Wave 4 is successful only if the SIM scheduler feels like Prism while staying Path A-only and avoiding Path B, CRM, plugin, and smart-memory contamination.

---

## 4. Product Decisions Locked For This Slice

- keep the inspiration shelf visible in SIM
- keep simple shelf-card inspiration `Ask AI` behavior in this slice as a normal chat-session launcher
- define that launcher as equivalent to opening a new chat session manually, seeding the first user turn with the inspiration text, and auto-submitting it
- treat only deeper inspiration-shelf work as explicit tech debt, not the base shelf-card `Ask AI` launcher behavior
- keep physical badge input as the true product entry for global scheduler follow-up behavior
- keep the scheduler-drawer mic only as a dev/test substitute when the physical badge path is unavailable
- do not add a clarification loop
- if delete/reschedule target resolution is still ambiguous or missing, safe-fail with explicit feedback and no mutation

---

## 5. Allowed Touch Map

### New Or Expanded SIM Artifacts Expected

- `SimSchedulerViewModel` real runtime implementation
- Wave 4 scheduler execution brief
- SIM scheduler tests
- tracker notes for deferred scheduler debt

### Existing Files Safe To Reuse Through Controlled Seams

- `app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt`
- scheduler-owned domain contracts such as `ScheduledTaskRepository`, `ScheduleBoard`, and `FastTrackMutationEngine`
- Uni-A / Uni-B / Uni-C scheduler extraction services when used only as narrow scheduler extraction seams

### Forbidden Runtime Assumptions

- using the current `SchedulerViewModel` directly in SIM
- requiring Path B or CRM enrichment for normal SIM scheduler operation
- treating the scheduler-drawer mic as the product-default scheduler input route
- introducing a clarification loop for ambiguous reschedule/delete in this wave

---

## 6. Execution Sequence

### Step 1: Replace The Shell Stub

- replace the fake local `SimSchedulerViewModel` timeline with repository-backed timeline data
- keep `SchedulerDrawer` mounted in `SimShell`
- avoid widening the change into SIM audio/chat ownership

### Step 2: Wire Narrow Path A Execution

- use ASR plus the Uni-A / Uni-B / Uni-C scheduler extraction services for transcript interpretation
- execute resulting scheduler DTOs through `FastTrackMutationEngine`
- keep the runtime narrow to scheduler-specific collaborators rather than the smart scheduler runtime root

### Step 3: Support Direct Drawer Actions

- delete must mutate the real scheduler repository
- direct reschedule from an existing task card must support explicit time updates through the SIM scheduler seam
- if the requested target or new time cannot be resolved safely, return explicit feedback and do not mutate

### Step 4: Keep Inspiration Shelf Visible And Preserve Simple `Ask AI`

- keep the shelf rendered in SIM
- preserve the simple shelf-card `Ask AI` action as a chat-session launcher
- keep that launcher behavior equivalent to manually opening a new chat session, filling the first user turn with the inspiration text, and sending it immediately
- allow existing inspiration display/delete behavior that does not force smart-runtime contamination
- defer timeline/multi-select inspiration AI and deeper inspiration-specific upgrades as tech debt

### Step 5: Record The Known Gaps

- log the global badge-thread follow-up context owner as a remaining gap if this slice does not yet finish cross-interface continuity
- log only advanced inspiration shelf follow-on work as deferred work
- do not blur those gaps into vague “future polish”

---

## 7. Validation Checklist

Wave 4 verification must prove:

- SIM scheduler timeline is repository-backed rather than shell-local fake data
- Path A task creation works in SIM without the smart `SchedulerViewModel`
- conflict-visible creates stay visible
- delete mutates the intended real task
- direct reschedule mutates the intended real task only when safe
- no-match or ambiguous scheduler actions safe-fail without mutation
- inspiration shelf remains visible
- shelf-card inspiration `Ask AI` launches a new chat session with the card text as the first auto-submitted user turn
- deferred inspiration debt is limited to advanced follow-on behavior rather than the base launcher contract
- scheduler-drawer mic works as a dev/test substitute without redefining the real badge-origin product story

---

## 8. Known Gaps To Carry Explicitly

- true cross-interface badge-thread follow-up context ownership may remain incomplete in this slice
- advanced inspiration shelf follow-on behavior beyond the base shelf-card `Ask AI` chat launcher is deferred
- smart tips, Path B enrichment, and smart-memory merge remain out of Wave 4 scope

If any of these still exist after implementation, they must be recorded in `docs/plans/tracker.md` or the current cleanup backlog rather than silently ignored.

---

## 9. Doc Sync Targets

If Wave 4 changes scheduler ownership, runtime seams, or deferred debt, sync these docs in the same session:

- `docs/plans/tracker.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-spine/interface.md`
- `docs/cerb-ui/scheduler/contract.md`

---

## 10. Done-When Summary

Wave 4 is done when:

- scheduler drawer works in the standalone app with real scheduler data
- task creation is Path A-backed through the SIM scheduler seam
- delete and direct reschedule are real rather than placeholder actions
- explicit safe-fail feedback exists for bad target branches
- inspiration shelf remains visible
- shelf-card inspiration `Ask AI` behaves like a plain seeded new-chat launcher
- deferred scheduler gaps are logged explicitly instead of being hidden
