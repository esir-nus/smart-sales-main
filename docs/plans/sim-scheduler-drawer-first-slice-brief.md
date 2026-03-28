# SIM Scheduler Drawer First Slice Brief

**Status:** In Progress  
**Date:** 2026-03-24  
**Owner:** UI transplant slice  
**Primary Tracker:** `docs/plans/ui-tracker.md`  
**Prototype Source:** `prototypes/sim-shell-family/sim_scheduler_drawer_shell.html`

---

## 1. Purpose

Deliver the first approved SIM scheduler drawer transplant as a **SIM-only visual/layout pass** on top of the shared `SchedulerDrawer`.

This slice does not change scheduler ownership or scheduler behavior.

---

## 2. Locked decisions

- keep the shared scheduler runtime and reuse `SchedulerDrawer`
- add a SIM-only presentation seam rather than forking a second drawer
- preserve current scheduler IA, mic lane, reminder prompting, date attention, and inspiration shelf behavior
- keep SIM inspiration multi-select disabled
- treat the debug-only mic panel as non-fidelity tooling; it may remain in debug builds but is not the prototype acceptance target

Deferred from this first slice:

- persistent peek / half-open production interaction
- new scheduler intelligence or smarter tips
- audio/chat scope widening
- non-SIM scheduler redesign

---

## 3. Implementation scope

- add `SchedulerDrawerVisualMode` so `AgentShell` stays standard while `SimShellContent` opts into SIM presentation
- restyle the SIM drawer container into a dark top-anchored slab with:
  - top monolith alignment
  - bottom gap above the SIM composer
  - rounded bottom corners only
  - quiet border and heavier shadow
- move the SIM grip treatment to the top edge while preserving current dismiss behavior
- restyle the calendar strip, timeline rails, task-card surfaces, and inspiration shelf into the SIM dark/frosted family
- keep the SIM timeline presentation start-time-first: active task rail/header should show only the start time, and SIM should not surface dangling end-time placeholders such as `HH:mm - ...`
- add at least one SIM preview for faster visual review

---

## 4. Verification

- `./gradlew :app-core:compileDebugKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimSchedulerViewModelTest --tests com.smartsales.prism.ui.UiSpecAlignmentTest`

Remaining acceptance work after this brief:

- device screenshots for collapsed normal, conflict, off-page attention, calendar expanded, and expanded-details states
- visual QA against the approved prototype
