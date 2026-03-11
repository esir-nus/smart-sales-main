# WorldStateSeeder Boundaries

> **Target**: `WorldStateSeeder.kt` and `L2WorldStateSeederTest.kt`
> **Level**: L2 Simulated Pipeline / Infrastructure

## Dependency Management (Anti-Illusion)
- **Allowed Reals**: `RealEntityWriter`, `SessionWorkingSet`, `RealContextBuilder`, `EntityDisambiguator`
- **Required Fakes**: `FakeEntityRepository`, `FakeUserHabitRepository`, `FakeMemoryRepository`
- **Forbidden Mocks**: Do not use `mockito` for data storage or resolution checks. The injection must write authentic Chinese B2B aliases and overlapping data directly into the Fakes to prove the resolution logic handles organic chaos.
