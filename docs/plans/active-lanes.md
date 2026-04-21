# Active Lanes Registry

In-flight task registry for the declaration-first shipping contract. See `docs/specs/declaration-first-shipping.md`.

Each entry is appended when a task passes the pre-flight scope conflict check, and removed on successful `/ship` or via `/abandon <title>`.

## Format

```
- date: YYYY-MM-DD
  lane: android | harmony | docs
  title: <short title>
  scope:
    - <file or module glob>
    - <file or module glob>
  force_parallel_reason: <only if --force-parallel was used>
```

## Active Entries

- date: 2026-04-21
  lane: android
  title: DTQ-03 scheduler hardening
  scope:
    - docs/plans/active-lanes.md
    - app-core/src/main/java/com/smartsales/prism/data/memory/RealScheduleBoard.kt
    - app-core/src/main/java/com/smartsales/prism/data/scheduler/RealActiveTaskRetrievalIndex.kt
    - domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/ExactTimeCueResolver.kt
    - domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/GlobalRescheduleExtractionContract.kt
    - domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterParsingSupport.kt
    - core/pipeline/src/main/java/com/smartsales/core/pipeline/PromptCompiler.kt
    - core/pipeline/src/main/java/com/smartsales/core/pipeline/RealGlobalRescheduleExtractionService.kt
    - app-core/src/test/java/com/smartsales/prism/data/scheduler/RealActiveTaskRetrievalIndexTest.kt
    - app-core/src/test/java/com/smartsales/prism/domain/memory/ScheduleBoardTest.kt
    - domain/scheduler/src/test/java/com/smartsales/prism/domain/scheduler/ExactTimeCueResolverTest.kt
    - domain/scheduler/src/test/java/com/smartsales/prism/domain/scheduler/SchedulerLinterTest.kt
    - core/pipeline/src/test/java/com/smartsales/core/pipeline/FollowUpRescheduleContractAlignmentTest.kt
    - core/pipeline/src/test/java/com/smartsales/core/pipeline/RealGlobalRescheduleExtractionServiceTest.kt
- date: 2026-04-21
  lane: docs
  title: DTQ-03 scheduler hardening docs sync
  scope:
    - docs/plans/active-lanes.md
    - docs/cerb/sim-scheduler/spec.md
    - docs/cerb/sim-scheduler/interface.md
    - docs/archive/reports/tests/L1-20260421-dtq-03-scheduler-hardening.md
