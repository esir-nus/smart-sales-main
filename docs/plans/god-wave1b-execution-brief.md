# God Wave 1B Execution Brief

**Status:** L1 Accepted  
**Date:** 2026-03-24  
**Wave:** 1B  
**Mission:** `AgentIntelligenceScreen.kt` structural cleanup  
**Primary Tracker:** `docs/plans/god-tracker.md`  
**Structure Law:** `docs/specs/code-structure-contract.md`  
**Validation Report:** `docs/reports/tests/L1-20260324-god-wave1b-agent-intelligence.md`

---

## 1. Purpose

Wave 1B is the first production refactor wave in the god-file cleanup campaign.

It targets `AgentIntelligenceScreen.kt`, which had become a mixed host, layout, SIM subtree, non-SIM subtree, section library, and preview container.

Wave 1B rewrites that trunk into stable ownership files without changing the public `AgentIntelligenceScreen(...)` call surface.

---

## 2. Wave 1B Law

Wave 1B may do:

- host/content/sections/preview extraction for `AgentIntelligenceScreen.kt`
- SIM subtree extraction into a SIM-owned UI file
- structure-test and guardrail updates needed for the new accepted shape
- tracker/doc sync for the accepted Wave 1B outcome

Wave 1B must **not** do:

- `SimShell.kt` cleanup work that belongs to Wave 1C
- `SimAgentViewModel.kt` or `SimSchedulerViewModel.kt` cleanup
- stale drawer-gesture androidTest seam restoration just to satisfy outdated ownership assumptions
- source-incompatible API changes to `AgentIntelligenceScreen(...)`

---

## 3. Delivered Structure

Wave 1B leaves `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt` as host-only.

Delivered extraction map:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceChatTimeline.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceHomeSections.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligencePreview.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt`

Additional Wave 1B adjustments:

- SIM composer test tags moved with the SIM subtree
- `resolveSimDynamicIslandIndex` moved with SIM ownership
- `GodStructureGuardrailTest` now supports mixed pilot statuses
- `AgentIntelligenceStructureTest` enforces the new split

---

## 4. Deferred Debt

Wave 1B explicitly defers stale SIM gesture androidTest seams to Wave 1C / `SimShell`.

These are not part of Wave 1B acceptance:

- `SimComposerInteractionTest`
- `SimDrawerGestureTest`
- `:app-core:compileDebugAndroidTestKotlin`

Reason:

- those tests still expect shell-owned gesture seams that no longer belong to `AgentIntelligenceScreen.kt`
- restoring them here would re-couple Wave 1B to Wave 1C ownership

---

## 5. Verification Status

Wave 1B acceptance uses focused JVM/L1 coverage plus module compile verification.

Executed commands:

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentIntelligenceStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimComposerContractTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimHomeHeroExperimentContractTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

---

## 6. Wave 1B Acceptance Bar

Wave 1B is complete only when:

- the host file is under the Wave 1 UI host budget
- the tracker row for `AgentIntelligenceScreen.kt` moves to `Accepted`
- focused structure and SIM contract tests stay green
- the stale SIM gesture androidTest drift is documented as deferred to Wave 1C
- docs and trackers reference the delivered Wave 1B split

---

## 7. Related Documents

- `docs/plans/god-tracker.md`
- `docs/plans/tracker.md`
- `docs/plans/ui-tracker.md`
- `docs/specs/code-structure-contract.md`
- `docs/reports/tests/L1-20260324-god-wave1b-agent-intelligence.md`
