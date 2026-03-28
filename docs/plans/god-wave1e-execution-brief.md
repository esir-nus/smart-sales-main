# God Wave 1E Execution Brief

**Status:** L1 Accepted  
**Date:** 2026-03-24  
**Wave:** 1E  
**Mission:** `SimSchedulerViewModel.kt` structural cleanup  
**Primary Tracker:** `docs/plans/god-tracker.md`  
**Structure Law:** `docs/specs/code-structure-contract.md`  
**Validation Report:** `docs/reports/tests/L1-20260324-god-wave1e-sim-scheduler-vm.md`

---

## 1. Purpose

Wave 1E is the fourth production refactor wave in the god-file cleanup campaign.

It targets `SimSchedulerViewModel.kt`, which had become a mixed public ViewModel seam, transcript ingress lane, mutation executor, reminder owner, and projection/warning owner.

Wave 1E rewrites that trunk into stable SIM-owned support files without changing the shell-facing `ISchedulerViewModel` contract.

---

## 2. Wave 1E Law

Wave 1E may do:

- public-seam reduction for `SimSchedulerViewModel.kt`
- ingress extraction for audio transcript routing, deterministic relative parsing, Uni-M fragment resolution, and scheduler drawer voice-reschedule target handling
- mutation extraction for fast-track execution and resolved reschedule writes
- reminder extraction for exact reminder schedule/cancel and exact-alarm prompting
- projection extraction for top-urgent/timeline flow construction, conflict/date attention, exit motion, and status emission
- structure-test and guardrail updates needed for the new accepted shape
- tracker/doc sync for the accepted Wave 1E outcome

Wave 1E must **not** do:

- `ISchedulerViewModel` surface changes
- new DI seams or repo-wide abstractions just to satisfy file split goals
- Path B or SIM chat authority expansion into scheduler mutation
- behavior changes that widen accepted SIM scheduler scope beyond the current Path A contract

---

## 3. Delivered Structure

Wave 1E leaves `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt` as a public seam file.

Delivered extraction map:

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerIngressCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerMutationCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerReminderSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerProjectionSupport.kt`

Additional Wave 1E adjustments:

- `SimSchedulerViewModel.kt` now measures `251 LOC`, below the Wave 1 ViewModel budget
- `GodStructureGuardrailTest` now treats `SimSchedulerViewModel.kt` as an `Accepted` pilot row
- `SimSchedulerViewModelStructureTest` enforces the new split
- scheduler callers remain source-compatible through `ISchedulerViewModel`

---

## 4. Deferred Debt

Wave 1E does not attempt secondary scheduler/domain cleanup outside the SIM scheduler trunk.

These are not part of Wave 1E acceptance:

- `SchedulerLinter.kt` decomposition
- `SimRescheduleTimeInterpreter.kt` refactoring
- new scheduler behavior or interface changes

Reason:

- Wave 1E is the scheduler ViewModel cleanup slice
- widening beyond the public seam split would blur the accepted Wave 2 debt boundary

---

## 5. Verification Status

Wave 1E acceptance uses focused JVM/L1 coverage plus module compile verification.

Executed commands:

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimSchedulerViewModelStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimSchedulerViewModelTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

---

## 6. Wave 1E Acceptance Bar

Wave 1E is complete only when:

- the host file is under the Wave 1 ViewModel budget
- the tracker row for `SimSchedulerViewModel.kt` moves to `Accepted`
- focused structure and scheduler behavior tests stay green
- the accepted split keeps `ISchedulerViewModel` callers source-compatible
- docs and trackers reference the delivered Wave 1E split

---

## 7. Related Documents

- `docs/plans/god-tracker.md`
- `docs/plans/tracker.md`
- `docs/specs/code-structure-contract.md`
- `docs/reports/tests/L1-20260324-god-wave1e-sim-scheduler-vm.md`
