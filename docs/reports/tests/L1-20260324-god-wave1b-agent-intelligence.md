# L1 God Wave 1B AgentIntelligence Validation

Date: 2026-03-24
Status: Accepted
Owner: Codex

## Scope

Validate the delivered Wave 1B structural cleanup slice:

1. `AgentIntelligenceScreen.kt` is reduced to a host-only file
2. extracted files own the content, timeline, section, preview, and SIM subtree roles
3. the god-file tracker and guardrail logic reflect `AgentIntelligenceScreen.kt` as `Accepted`
4. SIM composer and shared hero-shell contracts still point at the extracted ownership

## Source of Truth

- `docs/plans/god-tracker.md`
- `docs/plans/god-wave1b-execution-brief.md`
- `docs/specs/code-structure-contract.md`

## What Was Checked

### 1. Wave 1B extraction landed

Examined the delivered Wave 1B files:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceChatTimeline.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceHomeSections.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligencePreview.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt`

Observed outcome:

- the host file is reduced to the screen entrypoint and state collection
- previews are no longer in the host file
- the SIM subtree is no longer owned by the shared UI host file
- SIM composer test tags and rotating dynamic-island ownership now live with the SIM subtree

### 2. Focused JVM verification is green

Executed commands:

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentIntelligenceStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimComposerContractTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimHomeHeroExperimentContractTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

Observed result:

- the updated guardrail test accepts mixed pilot statuses and validates the accepted host row
- the new structure regression test passes
- the SIM composer contract test passes against the extracted SIM file
- the shared SIM home-hero experiment contract test passes against the extracted ownership
- `:app-core:compileDebugUnitTestKotlin` passes

### 3. Deferred scope stayed deferred

Wave 1B does not use stale androidTest surfaces as acceptance criteria.

Still deferred to Wave 1C / `SimShell`:

- `SimComposerInteractionTest`
- `SimDrawerGestureTest`
- `:app-core:compileDebugAndroidTestKotlin`

## Verification Run

### Focused acceptance pack

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentIntelligenceStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimComposerContractTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimHomeHeroExperimentContractTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

## Verdict

Accepted.

Wave 1B is structurally complete: `AgentIntelligenceScreen.kt` is no longer a god file, the new ownership seams are covered by focused L1 tests, and the remaining drawer-gesture cleanup debt is explicitly carried forward to Wave 1C.
