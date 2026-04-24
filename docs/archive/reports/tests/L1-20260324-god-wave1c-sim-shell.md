# L1 God Wave 1C SimShell Validation

Date: 2026-03-24
Status: Accepted
Owner: Codex

## Scope

Validate the delivered Wave 1C structural cleanup slice:

1. `SimShell.kt` is reduced to a host-only file under the shell budget
2. extracted files own the shell content, reducer, telemetry, projection, action, and follow-up section roles
3. the god-file tracker and guardrail logic reflect `SimShell.kt` as `Accepted`
4. SIM shell handoff, connectivity routing, and runtime-isolation contracts remain green after the split

## Source of Truth

- `docs/projects/god-file-cleanup/tracker.md`
- `docs/plans/god-wave1c-execution-brief.md`
- `docs/specs/code-structure-contract.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`

## What Was Checked

### 1. Wave 1C extraction landed

Examined the delivered Wave 1C files:

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellReducer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellProjection.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellSections.kt`

Observed outcome:

- the host file now keeps SIM runtime ownership, state collection, effects, and top-level callback wiring only
- the large shell composition tree no longer lives in `SimShell.kt`
- reducer, telemetry, projection, and action helpers no longer live in the host file
- `SimShell.kt` now measures `208 LOC`, below the Wave 1 shell-host budget of `550 LOC`

### 2. Focused JVM verification is green

Executed commands:

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimConnectivityRoutingTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimRuntimeIsolationTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

Observed result:

- the updated guardrail test now requires `SimShell.kt` to remain an accepted pilot row under budget
- the new structure regression test passes against the extracted shell ownership
- shell handoff and connectivity-routing tests still pass against the moved helpers
- runtime isolation still proves SIM owns its own runtime host instead of regressing to the smart shell root
- `:app-core:compileDebugUnitTestKotlin` passes

### 3. Deferred scope stayed deferred

Wave 1C does not use stale androidTest shell seams as acceptance criteria.

Still deferred outside Wave 1C acceptance:

- `SimComposerInteractionTest`
- `SimDrawerGestureTest`
- `:app-core:compileDebugAndroidTestKotlin`

## Verification Run

### Focused acceptance pack

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimConnectivityRoutingTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimRuntimeIsolationTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

## Verdict

Accepted.

Wave 1C is structurally complete: `SimShell.kt` is no longer a god file, the accepted host split is covered by focused L1 tests, and the remaining Wave 1 pilot debt now carries forward to `SimAgentViewModel.kt` and `SimSchedulerViewModel.kt`.
