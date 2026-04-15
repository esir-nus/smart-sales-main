# Dual-Engine Bridge Boundaries

> **Target**: `L2-DualEngineBridgeTest.kt`
> **Level**: L2 Simulated

## Dependency Management (Anti-Illusion)
- **Allowed Reals**: `RealUnifiedPipeline`, `RealContextBuilder`, `RealEntityWriter`, `SessionWorkingSet`
- **Required Fakes**: `FakeEntityRepository`, `FakeMemoryRepository`, `FakeUserHabitRepository`, `FakeToolRegistry`, `FakeExecutor`
- **Forbidden Mocks**: Do not use `mockito` for asserting database writes, context reads, or orchestrator execution. Use native fake inspection.
