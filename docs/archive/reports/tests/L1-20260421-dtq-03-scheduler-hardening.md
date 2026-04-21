# L1 Validation Report — DTQ-03 Scheduler Hardening

Status: Accepted
Date: 2026-04-21
Scope: DTQ-03 scheduler hardening
Evidence class: L1

## Scope

Validate the DTQ-03 Android scheduler hardening slice for:

- B1 injected local-day clock seam in `RealScheduleBoard`
- B2 relative-day exact-time guard coverage in `ExactTimeCueResolverTest`
- B3 stale delta-only test drift in `SchedulerLinterTest`
- M4 bounded LLM relative-day trust (`anchor + 365d`)
- M5 bounded `deltaFromTargetMinutes` (`+/-20160`, non-zero)
- M6 task-title redaction in `RealScheduleBoard` debug logs
- M7 shortlist ownership filtering for global reschedule suggestions

This L1 pass is limited to deterministic scheduler/backend logic and its local test seams.

## Executed Commands

1. `./gradlew :domain:scheduler:test --tests com.smartsales.prism.domain.scheduler.ExactTimeCueResolverTest --tests com.smartsales.prism.domain.scheduler.SchedulerLinterTest`
2. `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.FollowUpRescheduleContractAlignmentTest --tests com.smartsales.core.pipeline.RealGlobalRescheduleExtractionServiceTest`
3. `./gradlew :app-core:testFullDebugUnitTest --tests com.smartsales.prism.domain.memory.ScheduleBoardTest --tests com.smartsales.prism.domain.scheduler.SchedulerCoordinatorTest --tests com.smartsales.prism.domain.scheduler.SchedulerBreakItTest --tests com.smartsales.prism.ui.drawers.scheduler.SchedulerViewModelAudioStatusTest --tests com.smartsales.prism.data.real.L2CrossOffLifecycleTest`
4. `./gradlew :app-core:test :domain:scheduler:test`
5. `./gradlew :app:assembleDebug`
6. `rg -n "LocalDate\\.now|Instant\\.now" app-core/src/main/java/com/smartsales/prism/domain/scheduler app-core/src/main/java/com/smartsales/prism/data/memory domain/scheduler/src/main/java`
7. `grep -Rn "it.title" app-core/src/main/java/com/smartsales/prism/data/memory/RealScheduleBoard.kt`
8. `rg -n "LocalDate\\.now|Instant\\.now" app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt app-core/src/main/java/com/smartsales/prism/data/scheduler/RealInspirationRepository.kt`
9. `./gradlew :domain:scheduler:tasks --all`

## Result

Accepted for the DTQ-03 contracted scheduler/backend slice.

Focused DTQ-03 tests passed, `:app:assembleDebug` passed, the contracted backend scheduler paths have no remaining direct `LocalDate.now()` / `Instant.now()` calls, and `RealScheduleBoard` no longer logs raw task titles.

The broad `./gradlew :app-core:test :domain:scheduler:test` command still fails in `:app-core:testHarmonyDebugUnitTest`, but the failures are outside DTQ-03 and match pre-existing unrelated suite debt:

- `RealBadgeAudioPipelineIngressTest > bridge notification triggers processFile through init collector`
- `L2DualEngineBridgeTest > Scenario 4 - Dual Path Town and Highway Concurrency`
- `OnboardingFlowTransitionTest > sim host now follows the same production path through quick start`
- `OnboardingFlowTransitionTest > full app host starts at welcome and walks the full prototype path`

Report path for that unrelated failure set:

- `app-core/build/reports/tests/testHarmonyDebugUnitTest/index.html`

## Mini-Lab Outcomes

### Scenario A — timezone seam

Covered by `ScheduleBoardTest` via `real schedule board queries date range from injected local day`.

- Injected `TimeProvider.now = 2026-04-21T23:58:00Z`
- Injected `TimeProvider.today = 2026-04-22` in `Asia/Shanghai`
- Observed repository query start = `2026-04-22`
- Result: reminder-day bucket follows injected local day, not timezone-naive system day

### Scenario B — delta round-trip

Covered by `SchedulerLinterTest` via `follow-up reschedule V2 delta extraction returns supported operand`.

- Input payload: `{"decision":"RESCHEDULE_EXACT","timeKind":"DELTA_FROM_TARGET","deltaFromTargetMinutes":60}`
- Observed result: `Supported(DeltaFromTarget(60))`
- Negative bound also covered by `follow-up reschedule V2 delta extraction rejects out of bounds offset`

### Scenario C — negative year-9999 / far-future bound

Covered by:

- `ExactTimeCueResolverTest` via `rejectRelativeDayStartTime rejects llm date beyond one year from lawful anchor`
- `SchedulerLinterTest` via `Uni-A exact create rejects relative anchor date beyond one year`

Observed behavior:

- model-led relative-day date beyond lawful anchor + 365d is rejected
- parse path returns no mutation
- raw-title grep in `RealScheduleBoard` found no `it.title` logging

### Scenario D — negative ownership filtering

Covered by `RealGlobalRescheduleExtractionServiceTest` via `extract filters suggestion and preferred ids to owned shortlist`.

- Input suggested id outside shortlist: `task-9`
- Input preferred ids: `task-9`, `task-2`, `task-1`
- Owned shortlist: `task-1`, `task-2`
- Observed result: `suggestedTaskId = task-2`, `preferredTaskIds = [task-2, task-1]`
- Result: non-owned ids are stripped before downstream resolution

## Notes

- The contracted backend scheduler paths now return zero hits for direct `LocalDate.now()` / `Instant.now()` usage outside the `TimeProvider` seam.
- Broader UI/preview paths still contain direct time reads outside this DTQ-03 contract:
  - `app-core/src/main/java/com/smartsales/prism/data/scheduler/RealInspirationRepository.kt`
  - `app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt`
  - `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerCalendar.kt`
  - `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/FakeSchedulerViewModel.kt`
- `normalizeRelativeDayStartTime` received focused branch-oriented tests, but a numeric branch coverage percentage was not recorded because `:domain:scheduler` exposes no coverage task in the current Gradle configuration.
