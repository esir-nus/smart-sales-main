# Assessment Report: Wave 1 Lightning Router E2E Trial
**Date**: 2026-03-09
**Protocol on Trial**: `docs/cerb-e2e-test/testing-protocol.md`
**Target Implementation**: `RealLightningRouterTest.kt` & `AgentViewModelTest.kt`

## 1. Contextual Anchor (The "State of the World")
This assessment was generated immediately following the completion of the inaugural "Wave 1: Lightning Fast-Track" E2E tests. 
- **Architectural State**: The project had recently completed "Stage 2 Modularization", effectively isolating `:data` and `:domain` layers, but was suffering from un-updated test templates.
- **Testing Goal**: We attempted to prove that the `LightningRouter` natively short-circuits the LLM `Executor` for `GREETING` intents by bypassing the `UnifiedPipeline`. 
- **Environment Constraints**: We ran purely in the JVM unit testing environment (L1/L2) without Robolectric, hitting friction against Android SDK boundaries.

---

## 2. Executive Summary
The inaugural trial of the unified testing protocol—specifically enforcing the L1/L2/L3 Standard and Evidence-Based Documentation—was a resounding success in surfacing architectural truths over documented assumptions. 

The `/feature-dev-planner` workflow prevented the agent from blindly injecting tests into the `UnifiedPipeline` by forcing a code-read step. This revealed that the actual Phase 0 orchestration happens natively in `AgentViewModel`.

However, the trial exposed significant friction in JVM testing environments (Android SDK stubs), severe Kotlin type-erasure ambiguities (Mockito), and a critical gap in test-double infrastructure (the absence of a unified Fakes module).

---

## 3. Drifts & Architectural Discoveries

### 3.1 The Orchestrator Drift (Minor)
- **Assumption vs. Code Reality**: Early assumptions implied the `UnifiedPipeline` was responsible for intercepting `NOISE` and `GREETINGS`. In reality, the code implements this in `AgentViewModel.send()`. The ViewModel directly calls `LightningRouter.evaluateIntent()` and short-circuits to `MascotService` before the `UnifiedPipeline` is ever invoked.
- **Verdict**: Valid implementation, but tight coupling of routing logic to the Presentation layer restricts scalability. We tested the reality, not the illusion.

### 3.2 The "Stage 2 Modularization" Toll
- **Assumption vs. Code Reality**: Tests failed because test templates still looked for `UserProfile` in `:domain:model` instead of the newly extracted `:domain:memory`.
- **Verdict**: Cross-module component testing is brittle without unified interfaces.

---

## 4. Friction & Fixes

| Constraint / Error | Root Cause | Fix Applied |
|--------------------|------------|-------------|
| **Mockito `any()` Ambiguity** | Kotlin compiler couldn't resolve overloaded `any()` for `ModelRegistry` vs `LlmProfile` interfaces. | Replaced generic `any()` with explicitly typed matches: `any<LlmProfile>()`. |
| **`Log.e not mocked`** | `RealLightningRouter` calls `android.util.Log`, throwing exceptions in pure JVM tests. | Injected `testOptions { unitTests.isReturnDefaultValues = true }`. |
| **`AssertionError` on Returns** | Enabling default returns caused the Android SDK stub for `JSONObject` to silently return `null`. | Injected dependency `"org.json:json:20231013"` to run real JSON parsing on JVM. |
| **`NullPointerException` in `init`** | `AgentViewModel` triggers Coroutine Flows on initialization. | Added `flowOf(emptyList())` and `runBlocking` scoped mocks to the Setup block. |

---

## 5. Identified Gaps & Weaknesses

### 5.1 The "Mocking Boilerplate" Gap
In `AgentViewModelTest.kt`, over 30 lines of code were dedicated purely to setting up `Mockito.mock()` for unrelated repositories (`ScheduledTaskRepository`, `HistoryRepository`, etc.). This makes L2 Simulated tests brittle. Modifying a signature in an unrelated repository will break the UI test compilation.

### 5.2 The "Suspend Function" Mocking Trap
Mocking suspend functions using Mockito requires a `runTest` or `runBlocking` scope. The agent repeatedly stumbled over this when setting up initialization mocks in `@Before`.

---

## 6. Advice to the Consul (Strategic Next Steps)

1. **Promote the Creation of `:core:test-fakes`**:
   Before embarking on Wave 2 (The Dual-Engine Bridge), we must pause to build a dedicated test-doubles module. We should assemble a `PrismTestRig` class that automatically injects `FakeHistoryRepository`, etc., so feature tests can be 10 lines of assertions instead of 40 lines of setup.
   
2. **AcceptanceFixtureBuilder Prioritization**:
   Testing stateful flows (like Wave 2 CRM modifications) requires standardized JSON seed data. This utility is mandatory before proceeding to Wave 2.

3. **ViewModel Refactoring Warning**:
   `AgentViewModel` is a mega-orchestrator. We highly recommend extracting an **`IntentOrchestrator`** use-case down into the `:domain` layer to handle the `LightningRouter -> Pipeline` handoff natively, stripping the ViewModel of its heavy routing responsibilities.
