# God Wave 1D Execution Brief

**Status:** L1 Accepted  
**Date:** 2026-03-24  
**Wave:** 1D  
**Mission:** `SimAgentViewModel.kt` structural cleanup  
**Primary Tracker:** `docs/plans/god-tracker.md`  
**Structure Law:** `docs/specs/code-structure-contract.md`  
**Validation Report:** `docs/reports/tests/L1-20260324-god-wave1d-sim-agent-vm.md`

---

## 1. Purpose

Wave 1D is the third production refactor wave in the god-file cleanup campaign.

It targets `SimAgentViewModel.kt`, which had become a mixed public ViewModel seam, session persistence owner, audio-grounded chat owner, pending-audio completion bridge, and badge follow-up mutation lane.

Wave 1D rewrites that trunk into stable SIM-owned support files without changing the shell-facing `SimAgentViewModel` API.

---

## 2. Wave 1D Law

Wave 1D may do:

- public-seam reduction for `SimAgentViewModel.kt`
- session/persistence extraction for SIM chat history and audio binding normalization
- chat/audio lane extraction for general chat, audio-grounded chat, and pending-audio completion
- follow-up lane extraction for badge follow-up creation, quick actions, reschedule flow, and V2 shadow telemetry
- structure-test and guardrail updates needed for the new accepted shape
- tracker/doc sync for the accepted Wave 1D outcome

Wave 1D must **not** do:

- `SimSchedulerViewModel.kt` cleanup work that belongs to Wave 1E
- new public `IAgentViewModel` surface changes
- new DI seams or repo-wide abstractions just to satisfy file split goals
- behavior changes that widen scheduler authority beyond the current SIM follow-up contract

---

## 3. Delivered Structure

Wave 1D leaves `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt` as a public seam file.

Delivered extraction map:

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt`

Additional Wave 1D adjustments:

- `SimAgentViewModel.kt` now measures `422 LOC`, below the Wave 1 ViewModel budget
- `GodStructureGuardrailTest` now treats `SimAgentViewModel.kt` as an `Accepted` pilot row
- `SimAgentViewModelStructureTest` enforces the new split
- shell callers remain source-compatible across `SimShell`, `SimShellContent`, `SimShellActions`, and `AgentIntelligenceScreen`

---

## 4. Deferred Debt

Wave 1D does not attempt the remaining heavy SIM scheduler trunk cleanup.

These are not part of Wave 1D acceptance:

- `SimSchedulerViewModel.kt` decomposition
- scheduler ingress/projection extraction beyond what already exists
- any new scheduler drawer behavior changes

Reason:

- Wave 1D is the SIM chat/follow-up ViewModel cleanup slice
- widening into scheduler trunk cleanup would blur the accepted Wave 1E ownership boundary

---

## 5. Verification Status

Wave 1D acceptance uses focused JVM/L1 coverage plus module compile verification.

Executed commands:

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimAgentViewModelStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimAgentViewModelTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

---

## 6. Wave 1D Acceptance Bar

Wave 1D is complete only when:

- the host file is under the Wave 1 ViewModel budget
- the tracker row for `SimAgentViewModel.kt` moves to `Accepted`
- focused structure and SIM behavior tests stay green
- the accepted split keeps shell/UI call sites source-compatible
- docs and trackers reference the delivered Wave 1D split

---

## 7. Related Documents

- `docs/plans/god-tracker.md`
- `docs/plans/tracker.md`
- `docs/specs/code-structure-contract.md`
- `docs/reports/tests/L1-20260324-god-wave1d-sim-agent-vm.md`
