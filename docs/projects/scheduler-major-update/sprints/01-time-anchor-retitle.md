# Sprint 01 - time-anchor-retitle

## Header

- Project: scheduler-major-update
- Sprint: 01
- Slug: time-anchor-retitle
- Date authored: 2026-04-28
- Author: Claude
- Operator: Codex (default)
- Evaluator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):**

> i herby announce this atsks under ui-ux-poilsh project for : a small update for the global reshceduling ... i have a task for 9 am remind me to get up, and when i say no actually i will get at 10am, and the rechduling will work ... however, if i say, actaully no, 9 am i should be catching a flight, this will fail as a reshceduling as it's time driven ... study and author a sprint fro this update nice and clean

**Clarifications:**

> it's either event-anchor or time-anchor, don't ocmplicate thing, if tiem and event are all cahnged, it a new schedule tasks

> annoce a project for handling this update: project scheduler-major-update

**Interpretation:**

Add time-anchor retitle as a second resolution mode for global reschedule. When an utterance carries an exact clock cue, optional date qualifier, and a new event title for that slot, the system deterministically resolves the cue against active tasks. Exactly one match is retitled through the existing replacement path with the same `startTime` and `durationMinutes`; zero or multiple matches reject with explicit feedback and must not fall through to create. Combined retitle + time-shift is out of scope and remains a new schedule task per user clarification.

## Scope

Files the operator may touch:

- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/GlobalRescheduleExtractionContract.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterParsingSupport.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/ActiveTaskRetrievalIndex.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/ExactTimeCueResolver.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackDtos.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngine.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/PromptCompiler.kt` — only `compileGlobalRescheduleExtractionPrompt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt` — only the global-reschedule branch
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt` — only the global-reschedule branch, if pass-through is required
- `app-core/src/main/java/com/smartsales/prism/data/scheduler/RealActiveTaskRetrievalIndex.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerIngressCoordinator.kt` — only `handleResolvedGlobalReschedule`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerMutationCoordinator.kt` — only `executeResolvedReschedule`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt` — only the global-reschedule branch
- `core/test-fakes-domain/src/main/java/com/smartsales/core/test/fakes/FakeActiveTaskRetrievalIndex.kt`
- `domain/scheduler/src/test/java/com/smartsales/prism/domain/scheduler/SchedulerLinterTest.kt`
- `domain/scheduler/src/test/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngineTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/scheduler/RealActiveTaskRetrievalIndexTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/RealGlobalRescheduleExtractionServiceTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/GlobalRescheduleContractAlignmentTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/IntentOrchestratorTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSchedulerMutationCoordinatorTest.kt`
- `app-core/src/main/java/com/smartsales/prism/MainActivity.kt` — debug-only evidence launch extra
- `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt` — debug-only evidence loop wiring
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt` — debug-only evidence seed
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt` — debug seed guard
- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/specs/scheduler-path-a-execution-prd.md`
- `docs/projects/scheduler-major-update/tracker.md`
- `docs/projects/scheduler-major-update/sprints/01-time-anchor-retitle.md`

Out of scope:

- `OnboardingQuickStartService` and `OnboardingQuickStartSandboxResolver`; sandbox parity uses a different item type and resolver.
- `RealFollowUpRescheduleExtractionService`; only global reschedule extraction changes.
- `mightExpressReschedule` keyword changes.
- Combined retitle + time-shift behavior.
- `CHANGELOG.md`; operator only proposes a closeout line for user gating.
- Harmony.

## References

- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/core-flow/scheduler-fast-track-flow.md` — invariant 8, reschedule remains replacement-oriented
- `docs/specs/scheduler-path-a-execution-prd.md` — "Lexical Fuzzy Match Protocol"
- `docs/cerb/interface-map.md` — scheduler write ownership
- `.agent/rules/lessons-learned.md` — prompt and signature-change triggers
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/ExactTimeCueResolver.kt` — existing exact-time resolver requires relative-day context; sprint exposes or adds a clock-cue helper
- `app-core/src/main/java/com/smartsales/prism/data/scheduler/RealActiveTaskRetrievalIndex.kt` — active-task deterministic target gate
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngine.kt` — replacement mutation engine

## Success Exit Criteria

1. `GlobalRescheduleExtractionPayload` and `GlobalRescheduleExtractionResult.Supported` carry optional `newTitle: String?`; `GlobalRescheduleContractAlignmentTest` proves the prompt schema and a worked example include `newTitle`.
2. `./gradlew :domain:scheduler:test --tests "*SchedulerLinterTest*"` passes with cases for: blank old-target clues + non-blank `newTitle` accepted; both shapes blank rejected; `targetQuery` carrying only a time-anchor phrase plus `newTitle` accepted; `targetQuery` carrying old-event text plus `newTitle` rejected.
3. `./gradlew :app-core:testDebugUnitTest --tests "*RealActiveTaskRetrievalIndexTest*"` passes for `resolveTargetByClockAnchor(clockCue, nowIso, timezone, displayedDateIso)`: plain clock uses today in the provided timezone when `displayedDateIso == null`; scheduler/follow-up callers pass displayed date when available; date-qualified cues use the qualified date; one match resolves, two matches are ambiguous, zero matches are no-match, and comparison is exact to the minute.
4. `./gradlew :domain:scheduler:test --tests "*FastTrackMutationEngineTest*"` passes with `newTitle` proving `taskRepository.rescheduleTask(oldId, newTask)` is called, `newTask.title == newTitle`, `startTime` and `durationMinutes` are preserved, and conflict re-evaluation is skipped or clear.
5. `./gradlew :app-core:testDebugUnitTest --tests "*SimSchedulerMutationCoordinatorTest*"` passes with `newTitle` proving title-only replacement, reminder cancel, and reminder re-schedule.
6. `./gradlew :core:pipeline:testDebugUnitTest --tests "*IntentOrchestratorTest*"` passes with cases for: "改成9点赶飞机" plus one matching active task emits `PathACommitted` with unchanged start time; two matching tasks rejects as ambiguous; zero matching tasks rejects and does not fall through to create; "把拿合同推迟1个小时" still follows the existing event-anchor time-shift path.
7. `./gradlew :core:pipeline:testDebugUnitTest --tests "*RealGlobalRescheduleExtractionServiceTest*"` and `./gradlew :core:pipeline:testDebugUnitTest --tests "*GlobalRescheduleContractAlignmentTest*"` pass.
8. `./gradlew :app:assembleDebug` passes.
9. On-device evidence: `adb logcat` from a device with one matching active task shows the time-anchor branch tag and `rescheduleTask` invocation for a real "改成9点赶飞机" input; save at `docs/projects/scheduler-major-update/evidence/01-time-anchor-retitle/logcat-time-anchor.txt`.
10. Docs sync: `docs/core-flow/scheduler-fast-track-flow.md` clarifies that time-anchor retitle still uses replacement semantics, and `docs/specs/scheduler-path-a-execution-prd.md` gains a concise time-anchor resolution subsection.
11. `docs/projects/scheduler-major-update/tracker.md` sprint 01 row is set to `done` at successful close.

## Stop Exit Criteria

- The LLM still emits old-event `targetQuery` plus `newTitle` for clean time-anchor inputs after one prompt iteration; surface and stop instead of coercing.
- A required surface cannot supply `nowIso`, `timezone`, or its available displayed-date context to `resolveTargetByClockAnchor`; surface and stop rather than guessing.
- Reminder text reads from a stale cached snapshot instead of the replacement task title; surface and stop for a reminder-cache audit sprint.
- Iteration bound hits without green focused tests.

## Iteration Bound

3 iterations or 90 wall-clock minutes, whichever hits first.

Each iteration:

1. Implement or refine one slice: contract/prompt/linter, resolver, mutation, consumers, then docs.
2. Run the focused Gradle command for the slice.
3. If green, advance; if red, fix within the same slice unless a stop criterion is met.

## Required Evidence Format

Closeout must include:

- `git diff --stat` for the close commit.
- `BUILD SUCCESSFUL` line and test count for every Gradle test command in Success criteria 2-7.
- `BUILD SUCCESSFUL` line for `./gradlew :app:assembleDebug`.
- `adb logcat` excerpt of no more than 30 lines saved to the evidence path in Success criterion 9.
- One-line confirmation that the event-anchor regression test still passes, with the test name.

## Iteration Ledger

_Operator fills this section once per iteration._

### Iteration 1 - implementation and focused verification

- Contract/prompt/linter: added optional `newTitle`, prompt examples, time-anchor query validation, and ownership filtering behavior for title mode.
- Resolver/mutation/consumers: added clock-anchor resolution, title-only replacement mutation, SIM drawer/follow-up routing, Path B pass-through command support, and top-level voice routing with no create fallback on no-match/ambiguous time anchors.
- Docs/tests: synced core-flow and PRD notes, updated project tracker, and added focused linter, resolver, mutation, extraction, and orchestrator tests.
- Evaluator result: focused tests and debug assemble are green. Device replay is blocked because `adb devices` returned no attached devices.

### Iteration 2 - device evidence loop

- Invalidated on 2026-04-28: the evidence loop used `sim_debug_time_anchor_retitle` to seed and drive a scheduler follow-up chat session. That path is not valid evidence for the required top-level voice/SIM rec-button behavior.
- Invalidated evidence file: `docs/projects/scheduler-major-update/evidence/01-time-anchor-retitle/logcat-time-anchor.txt` currently shows `SIM badge scheduler follow-up action completed` / `action=retitle`, so it must not be accepted as closeout proof.

### Iteration 3 - top-level voice correction

- Removed the Sprint-01 time-anchor retitle debug/follow-up launch path from `MainActivity`, `RuntimeShell`, `SimShellActions`, and its shell handoff test.
- Added scheduler commit kind metadata so create commits and reschedule commits are distinguishable; physical badge reschedules now map to `SchedulerResult.TaskRescheduled` and do not start a badge scheduler follow-up session.
- Removed the time-anchor retitle mutation branch from `SimAgentFollowUpCoordinator`; unrelated badge-created follow-up session behavior remains in place.
- Routed top-level SIM general sends that look scheduler-shaped through `IntentOrchestrator.processInput(..., isVoice = true)` before ordinary chat fallback. `PathACommitted` is silent in chat so Room/Scheduler UI owns the visible card update; explicit scheduler rejection appends short failure feedback.
- Evaluator result: focused tests, `:app:assembleDebug`, and `:app-core:assembleDebug` are green. Device `fc8ede3e` was visible, but `adb install -r app-core-debug.apk` timed out after 60 seconds and a second `adb install --no-streaming` attempt was manually interrupted, so replacement L3 logcat evidence remains blocked.

### Iteration 4 - scheduler drawer debug automation correction

- Moved the quick automation surface into the scheduler drawer SIM debug panel instead of the chat session interface.
- Added Chinese scheduler-drawer inputs for the intended human loop: `明天早上八点我要赶飞机`, `明早八点应该是要去开会`, and `不对，明早八点应该是去机场接人`.
- Routed those buttons through scheduler transcript ingress (`source=scheduler_debug_button`) so they exercise scheduler create/reschedule behavior directly rather than follow-up chat actions.
- Evaluator result: focused tests and `:app-core:assembleDebug` are green. Device `fc8ede3e` replay succeeded; the final visible scheduler state was `最近：去机场接人 · 08:00 - ...`, and the replacement logcat evidence contains no `SimBadgeFollowUpChat`, `SIM badge scheduler follow-up action completed`, or `action=retitle` lines.

### Post-close verification - reusable device-loop rerun

- 2026-04-28 rerun on device `fc8ede3e`: rebuilt and installed `app-core-debug.apk`, cleared app data, completed the known-entry onboarding path, opened the scheduler drawer, cleared logcat, and replayed the three scheduler debug buttons.
- Observation: the first create action opened the exact-alarm permission guidance dialog, which covered the debug buttons and caused the first retitle tap attempt to miss. The loop treated this as a failed observation, dismissed `稍后`, and continued with the same log window.
- Evidence result: `run-05-device-loop-logcat.txt` contains the create transcript plus two `SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY` / `Task Rescheduled (Room)` retitles; `run-05-device-loop-ui.xml` shows `最近：去机场接人 · 08:00 - ...`; negative grep found no `SimBadgeFollowUpChat`, `SIM badge scheduler follow-up action completed`, `action=retitle`, or chat follow-up action text.

## Closeout

_Operator fills at exit._

- **Status:** `done`
- **One-liner for tracker:** Scheduler-drawer Chinese debug inputs now exercise create and time-anchor retitle without follow-up chat; focused tests and replacement L3 logcat are green.
- **Evidence artifacts:**
  - Correction focused tests:
    - `./gradlew :core:pipeline:testDebugUnitTest --tests "*IntentOrchestratorTest*"`: BUILD SUCCESSFUL; `IntentOrchestratorTest` tests=22, failures=0, errors=0.
    - `./gradlew :app-core:testDebugUnitTest --tests "*SimAgentViewModelTest*" --tests "*SimShellHandoffTest*" --tests "*RealBadgeAudioPipelineIngressTest*" --tests "*SchedulerPipelineNotificationsTest*" --tests "*PipelineEventTest*"`: BUILD SUCCESSFUL; `SimAgentViewModelTest` tests=53, `SimShellHandoffTest` tests=33, `RealBadgeAudioPipelineIngressTest` tests=5, `SchedulerPipelineNotificationsTest` tests=3, `PipelineEventTest` tests=3; failures=0, errors=0.
    - `./gradlew :app:assembleDebug`: BUILD SUCCESSFUL.
    - `./gradlew :app-core:assembleDebug`: BUILD SUCCESSFUL.
  - Scoped `git diff --stat`: 24 files changed, 728 insertions(+), 12 deletions(-) across scheduler time-anchor files.
  - `./gradlew :domain:scheduler:test --tests "*SchedulerLinterTest*"`: BUILD SUCCESSFUL; `SchedulerLinterTest` tests=31, failures=0, errors=0.
  - `./gradlew :domain:scheduler:test --tests "*FastTrackMutationEngineTest*"`: BUILD SUCCESSFUL; `FastTrackMutationEngineTest` tests=12, failures=0, errors=0.
  - `./gradlew :app-core:testDebugUnitTest --tests "*RealActiveTaskRetrievalIndexTest*"`: BUILD SUCCESSFUL; `RealActiveTaskRetrievalIndexTest` tests=10, failures=0, errors=0.
  - `./gradlew :app-core:testDebugUnitTest --tests "*SimSchedulerMutationCoordinatorTest*"`: BUILD SUCCESSFUL; `SimSchedulerMutationCoordinatorTest` tests=3, failures=0, errors=0.
  - `./gradlew :core:pipeline:testDebugUnitTest --tests "*IntentOrchestratorTest*"`: BUILD SUCCESSFUL; `IntentOrchestratorTest` tests=22, failures=0, errors=0.
  - `./gradlew :core:pipeline:testDebugUnitTest --tests "*RealGlobalRescheduleExtractionServiceTest*"`: BUILD SUCCESSFUL; `RealGlobalRescheduleExtractionServiceTest` tests=2, failures=0, errors=0.
  - `./gradlew :core:pipeline:testDebugUnitTest --tests "*GlobalRescheduleContractAlignmentTest*"`: BUILD SUCCESSFUL; `GlobalRescheduleContractAlignmentTest` tests=1, failures=0, errors=0.
  - `./gradlew :app:assembleDebug`: BUILD SUCCESSFUL.
  - `./gradlew :app-core:testDebugUnitTest --tests "*SimShellHandoffTest*"`: BUILD SUCCESSFUL; debug evidence scenario seed test passed.
  - `./gradlew :app-core:assembleDebug`: BUILD SUCCESSFUL for the scheduler-drawer replay APK; 441 actionable tasks, 39 executed, 402 up-to-date.
  - Device evidence: replaced at `docs/projects/scheduler-major-update/evidence/01-time-anchor-retitle/logcat-time-anchor.txt`. Device `fc8ede3e` replayed scheduler-drawer buttons for `明天早上八点我要赶飞机`, `明早八点应该是要去开会`, and `不对，明早八点应该是去机场接人`; logcat shows `source=scheduler_debug_button`, `SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY`, and `Task Rescheduled (Room)` for the two retitles. Negative grep found no `SimBadgeFollowUpChat`, `SIM badge scheduler follow-up action completed`, or `action=retitle` lines in the replay log.
  - UI evidence: `uiautomator` dump after replay showed final visible scheduler text `最近：去机场接人 · 08:00 - ...`; no chat follow-up action panel was visible.
  - Event-anchor regression: `shared scheduler router resolves top level voice global reschedule before path b` remains covered in `IntentOrchestratorTest` and passed in the focused command above.
- **Lesson proposals:** none.
- **CHANGELOG line (proposed; user gates landing):**
  `- **[改进] 全局改期支持时间锚点改名** — "改成9点赶飞机" 这类输入现可命中已有 9 点任务并就地替换标题；时间和事件同时变化仍按新建处理。`
