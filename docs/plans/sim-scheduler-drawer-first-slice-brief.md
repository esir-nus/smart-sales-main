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

Any simultaneous scheduler create/reschedule hardening belongs to the SIM scheduler behavior lane (`docs/cerb/sim-scheduler/spec.md` plus `docs/plans/sim-tracker.md`), not to this UI transplant brief.

---

## 2. Locked decisions

- keep the shared scheduler runtime and reuse `SchedulerDrawer`
- add a SIM-only presentation seam rather than forking a second drawer
- preserve current scheduler IA, mic lane, reminder prompting, date attention, and inspiration shelf behavior
- when the scheduler drawer is open, hide the SIM header hamburger / new-session actions but keep the center dynamic island mounted
- use a bottom-edge SIM dismiss handle with tap plus downward drag semantics; scrim tap and system back stay unchanged
- month chevrons may page visible month inside the calendar UI, but attention acknowledgement must still wait for explicit day tap
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
- move the SIM grip treatment to the bottom edge with tap plus downward drag dismissal
- restyle the calendar strip, timeline rails, task-card surfaces, and inspiration shelf into the SIM dark/frosted family
- keep the SIM timeline presentation start-time-first: active task rail/header should show only the start time, and SIM should not surface dangling end-time placeholders such as `HH:mm - ...`
- keep month paging UI-local so chevrons change visible month without calling scheduler date acknowledgement
- tighten SIM timeline gutter/card rhythm so rail times stay one-line and the terminal rail does not overrun below the last card
- add at least one SIM preview for faster visual review
- keep drawer acceptance anchored to the real `visualMode = SIM` call path rather than the shared default visual mode
- cover the active SIM review states in the drawer acceptance harness: collapsed done/conflict plus expanded details/note/tips/conflict

---

## 4. Verification

- `./gradlew :app-core:compileDebugKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.UiSpecAlignmentTest --tests com.smartsales.prism.ui.InsetOwnershipContractTest`
- `./gradlew :app-core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerSimModeTest,com.smartsales.prism.ui.drawers.scheduler.SchedulerCalendarTest,com.smartsales.prism.ui.sim.SimDrawerGestureTest`

Remaining acceptance work after this brief:

- device screenshots for collapsed normal, off-page attention, calendar expanded, and wider prototype-vs-device fidelity review after the focused SIM-mode drawer harness is green
- visual QA against the approved prototype
