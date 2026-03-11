# Adaptive Habit Loop Spec

> **OS Layer**: L2 Testing Environment (RAM + App integration)
> **State**: SHIPPED

## Execution Scenarios

### 1. The Reinforcement Amplification
- **Inject**: A specific `RlObservation` into `ReinforcementLearner`.
- **State**: The learner processes the observation by delegating to `FakeUserHabitRepository`.
- **Expect**: The habit is recorded and its confidence is increased correctly when observed multiple times.

### 2. The Garbage Collection
- **Inject**: A habit with a low confidence score (below `DELETION_THRESHOLD`), simulating an old or unreinforced habit.
- **State**: The `RealContextBuilder` triggers `loadUserHabits()`.
- **Expect**: The `RealReinforcementLearner` partitions the habits, drops the dead ones, and explicitly issues a `delete` to the Repository.

### 3. The Context Injection
- **Inject**: A highly confident habit exists in the Repository.
- **State**: The pipeline executes an ETL phase.
- **Expect**: The habit successfully propagates through `loadUserHabits()` into the `PipelineContext` seamlessly, ensuring the LLM would receive it.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Mock Eviction Structure | ✅ SHIPPED | Setup tear-down, fakes injected |
| **2** | Scenario Writing | ✅ SHIPPED | All RL Branches written |
