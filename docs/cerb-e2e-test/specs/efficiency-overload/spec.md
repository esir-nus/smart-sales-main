# Efficiency Overload Spec

> **OS Layer**: L2 Testing Environment (RAM + App integration)
> **State**: SHIPPED

## Execution Scenarios

### 1. The Multi-Task Burst
- **Inject**: Upstream LLM JSON parse array returns multiple valid tasks.
- **State**: `FakeScheduledTaskRepository` starts empty.
- **Expect**: `RealUnifiedPipeline` routes to `LintResult.MultiTask`, parses all items, inserts them sequentially into the `scheduledTaskRepository`, and returns a single `PipelineResult.SchedulerMultiTaskCreated` emission containing details for all generated tasks.

### 2. The Conflict Cascade
- **Inject**: A bulk array of tasks where two tasks share the exact same start time/duration, causing a schedule board overlap.
- **State**: The `FakeScheduleBoard` evaluates checkConflict logic during the array loop.
- **Expect**: The pipeline inserts both tasks, but flags `hasConflict = true` on the final `SchedulerMultiTaskCreated` return object so the UI can warn the user.

### 3. The Alarm Array
- **Inject**: A multi-task payload.
- **State**: The `FakeAlarmScheduler` starts empty.
- **Expect**: The pipeline iterates over the array and fires `alarmScheduler.scheduleCascade()` cleanly for every single task id inserted, proving alarm logic scales safely across a bulk execution.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Mock Eviction Structure | ✅ SHIPPED | Setup tear-down, fakes injected |
| **2** | Scenario Writing | ✅ SHIPPED | All Multi-Task Branches written |
