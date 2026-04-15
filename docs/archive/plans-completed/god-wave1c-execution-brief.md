# God Wave 1C Execution Brief

**Status:** L1 Accepted  
**Date:** 2026-03-24  
**Wave:** 1C  
**Mission:** `SimShell.kt` structural cleanup  
**Primary Tracker:** `docs/plans/god-tracker.md`  
**Structure Law:** `docs/specs/code-structure-contract.md`  
**Validation Report:** `docs/reports/tests/L1-20260324-god-wave1c-sim-shell.md`

---

## 1. Purpose

Wave 1C is the second production refactor wave in the god-file cleanup campaign.

It targets `SimShell.kt`, which had become a mixed shell host, reducer bucket, telemetry sink, projection helper, follow-up action owner, and large overlay composition container.

Wave 1C rewrites that trunk into stable SIM-owned role files without changing the public `SimShell(...)` call surface.

---

## 2. Wave 1C Law

Wave 1C may do:

- host/content extraction for `SimShell.kt`
- reducer, telemetry, projection, action, and follow-up-section extraction for the shell
- structure-test and guardrail updates needed for the new accepted shape
- tracker/doc sync for the accepted Wave 1C outcome

Wave 1C must **not** do:

- `SimAgentViewModel.kt` cleanup work that belongs to Wave 1D
- `SimSchedulerViewModel.kt` cleanup work that belongs to Wave 1E
- `SimAudioDrawer.kt` cleanup beyond import rewiring required by the shell split
- source-incompatible API changes to `SimShell(...)`

---

## 3. Delivered Structure

Wave 1C leaves `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt` as host-only.

Delivered extraction map:

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellReducer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellProjection.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellSections.kt`

Additional Wave 1C adjustments:

- `SimShell.kt` now measures `208 LOC`, below the Wave 1 shell-host budget
- `GodStructureGuardrailTest` now treats `SimShell.kt` as an `Accepted` pilot row
- `SimShellStructureTest` enforces the new split
- `SimRuntimeIsolationTest` now validates the split host/content ownership without reviving smart-root assumptions

---

## 4. Deferred Debt

Wave 1C keeps stale shell androidTest seams out of the acceptance bar.

These are not part of Wave 1C acceptance:

- `SimComposerInteractionTest`
- `SimDrawerGestureTest`
- `:app-core:compileDebugAndroidTestKotlin`

Reason:

- Wave 1C is the source-owned shell cleanup slice, not an androidTest restoration wave
- restoring stale gesture seams here would widen scope beyond the accepted SIM shell host split

---

## 5. Verification Status

Wave 1C acceptance uses focused JVM/L1 coverage plus module compile verification.

Executed commands:

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimConnectivityRoutingTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimRuntimeIsolationTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

---

## 6. Wave 1C Acceptance Bar

Wave 1C is complete only when:

- the host file is under the Wave 1 UI shell budget
- the tracker row for `SimShell.kt` moves to `Accepted`
- focused structure, handoff, connectivity, and runtime-isolation tests stay green
- the accepted shell split is documented in the tracker and Wave 1C brief
- `SimShell(...)` remains the SIM runtime host entrypoint

---

## 7. Related Documents

- `docs/plans/god-tracker.md`
- `docs/plans/tracker.md`
- `docs/plans/ui-tracker.md`
- `docs/specs/code-structure-contract.md`
- `docs/reports/tests/L1-20260324-god-wave1c-sim-shell.md`
