# 🛡️ Code Audit: The Anti-Illusion Test Overhaul

## 1. Context & Scope
- **Target**: The Layer 3 Core Pipeline Test Suite (`app-core` and `:core:pipeline`).
- **Goal**: Rigorously verify the exact state of the Anti-Illusion testing protocol. Identify exact files that are "hallucinating" (violating protocol via Mockito) and explicitly map out the required work.

## 2. Existence Verification & Logic Tracing
- [x] Identified `testing-protocol.md` defining the 3-Level Testing Standard (L1: Logic, L2: Sim, L3: Chaotic Device).
- [x] Identified `docs/cerb-e2e-test/tasklist_log.md` indicating L2/L3 E2E testing is only 1/6th complete.
- [x] Swept the codebase for protocol violations (`import org.mockito`) across all test suites. 

### Critical Violators (The Hallucinators) ❌
The following files are actively violating the **Anti-Hallucination** and **Fake Over Mock** protocols by bypassing information gates:

1. **`RealUnifiedPipelineTest.kt`**: 
   - **Violation**: Uses `mockito.mock` for `ContextBuilder`, `EntityWriter`, `ScheduledTaskRepository`, etc.
   - **Logic Tracing**: The test literally skips the pipeline's intelligence by mocking `ContextBuilder.build()` to blindly return an `EnhancedContext`. It has ZERO branch coverage for partial/missing intents.

2. **`RealEntityWriterTest.kt`**:
   - **Violation**: Uses `mockito.mock` for data layer repositories instead of the `FakeEntityRepository`.
   
3. **`RealInputParserServiceTest.kt`**:
   - **Violation**: Uses legacy mocks.

4. **`RealAudioRepositoryBreakItTest.kt`**:
   - **Violation**: Uses legacy mocks.

## 3. Test Coverage
- **Coverage**: The current coverage is a **Testing Illusion**. The tests look green, but they are evaluating mocked behaviors, not the actual application routing logic.

## 4. Conclusion & Directive
- **Ready to Proceed?**: **CAUTION**. The test suite is providing false security.
- **Concrete Next Step**: Execute the **Tech Debt: Anti-Illusion Overhaul** from `tracker.md`. 
  - Stop making "guessy" E2E tests based on illusions.
  - Delete the mocks in `RealUnifiedPipelineTest.kt`.
  - Inject the `FakeMemoryRepository` and `FakeEntityRepository` created in the previous Wave 1 test infrastructure task.
  - Write explicit tests for the 3 context states (Empty/Noise, Partial/Clarification, Sufficient/Execution) defined in `.agent/rules/anti-illusion-protocol.md`.
