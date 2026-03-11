# Adaptive Habit Loop Boundaries

> **Target**: `L2AdaptiveHabitLoopTest.kt`
> **Level**: L2 Simulated Pipeline

## Dependency Management (Anti-Illusion)
- **Allowed Reals**: `RealUnifiedPipeline`, `RealContextBuilder`, `RealReinforcementLearner`
- **Required Fakes**: `FakeUserHabitRepository`, `FakeInputParserService`, `FakeEntityDisambiguationService`, `FakeTimeProvider`
- **Forbidden Mocks**: Do not use `mockito`. We must strictly use direct instance initialization and `FakeUserHabitRepository` assertions to prove RL functionality.
