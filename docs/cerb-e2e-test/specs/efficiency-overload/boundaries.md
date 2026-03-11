# Efficiency Overload Boundaries

> **Target**: `L2EfficiencyOverloadTest.kt`
> **Level**: L2 Simulated Pipeline

## Dependency Management (Anti-Illusion)
- **Allowed Reals**: `RealUnifiedPipeline`
- **Required Fakes**: `FakeScheduledTaskRepository`, `FakeInputParserService`, `FakeEntityDisambiguationService`, `FakeScheduleBoard`, `FakeAlarmScheduler`
- **Forbidden Mocks**: Do not use `mockito`. The multi-task logic relies heavily on iterating over the parsed JSON arrays. We must verify the actual outputs by checking the state of the Fakes (DB row count, scheduled alarms count) and inspecting the emitted `SchedulerMultiTaskCreated` object.
