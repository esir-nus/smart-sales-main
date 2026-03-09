# Assessment Report: Anti-Illusion Test Overhaul Wave 2 (Pre-Execution Audit)
**Date**: 2026-03-09
**Protocol on Trial**: `docs/cerb-e2e-test/testing-protocol.md`
**Target Implementation**: `RealLightningRouterTest.kt`, `IntentOrchestratorTest.kt`, `EchoPluginTest.kt`

## 1. Contextual Anchor (The "State of the World")
The system is currently preparing for Wave 2 of the Anti-Illusion Test Overhaul. The goal is to completely evict Mockito from L2 E2E Simulated Pipeline tests to enforce the Blackbox E2E principle. The `:core:test-fakes` module has been built and is available, but the `core:pipeline` tests still rely heavily on `mockito-kotlin` to stub execution results, creating a "Testing Illusion." This assessment audits the current state before executing the eviction plan.

## 2. Executive Summary
The Pre-Execution Audit confirms that `RealLightningRouterTest` and `IntentOrchestratorTest` violate the Blackbox E2E Principle by mocking intermediate steps (like LLM Execution and Context Building) rather than testing the real pipeline logic with Fakes. However, `EchoPluginTest` is already compliant with the protocol. The proposed implementation plan correctly targets the eviction of Mockito in favor of the new Fakes module.

## 3. Drifts & Architectural Discoveries
- **Assumption vs. Code Reality**: We assumed these tests were validating pipeline routing and E2E mechanics. In reality, they are merely L1 logic verifications proving that Mockito can return strings and static enums.
- **Verdict**: The current tests are illusions. The `testing-protocol.md` spec is the source of truth, and the core routing tests must be strictly rewritten to match it.

## 4. Friction & Fixes
| Constraint/Error | Root Cause | Fix Applied (Planned) |
|------------------|------------|-----------------------|
| "Testing Illusion" | Tests mock the LLM Executor and Context Builder rather than running Fake implementations. | The implementation plan will replace `org.mockito.kotlin.whenever` with `FakeContextBuilder` and `FakeExecutor`. |
| Cross-Module Fake Limits | The `core:pipeline` module requires fake implementations of `LightningRouter` and `MascotService` to test the Orchestrator, which might not be available yet. | Must verify `:core:test-fakes` contains all required test doubles before removing mocks. |

## 5. Identified Gaps & Weaknesses
- `IntentOrchestratorTest` uses 13 `@Mock` and `when()` mocking statements for routing, completely bypassing real execution.
- `RealLightningRouterTest` mocks `ExecutorResult.Success` manually without verifying if the outputs are tied to realistically formulated query data.
- **Strength Identified**: `EchoPluginTest` cleanly uses `FakePluginGateway` without any external mocking dependencies.

## 6. Advice to the Consul (Strategic Next Steps)
1. **Proceed immediately** with the implementation plan to replace Mockito with Fakes in `:core:pipeline`.
2. **Leave `EchoPluginTest.kt` alone**: It is already written natively with `FakePluginGateway` and serves as the golden standard for isolation testing.
3. Ensure `:core:test-fakes` provides sufficient deterministic data to cover the core E2E pillar flows when testing the `IntentOrchestrator`.
