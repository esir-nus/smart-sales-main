# Scheduler Path A Delivered-Behavior Map

> Sprint: `docs/projects/bake-transformation/sprints/05-scheduler-path-a-dbm.md`
> Date: 2026-04-28
> Scope: current delivered behavior only. This map does not update target
> core-flow, demote Cerb authority, or write a BAKE contract.

## Evidence Commands

Commands used while building this map:

```text
sed -n '1,260p' docs/core-flow/scheduler-fast-track-flow.md
sed -n '1,300p' docs/core-flow/sim-scheduler-path-a-flow.md
sed -n '1,260p' docs/cerb/scheduler-path-a-spine/spec.md
sed -n '1,240p' docs/cerb/scheduler-path-a-spine/interface.md
sed -n '1,260p' docs/cerb/scheduler-path-a-uni-a/spec.md
sed -n '1,260p' docs/cerb/scheduler-path-a-uni-b/spec.md
sed -n '1,280p' docs/cerb/scheduler-path-a-uni-c/spec.md
sed -n '1,300p' docs/cerb/scheduler-path-a-uni-d/spec.md
sed -n '1,260p' docs/cerb/interface-map.md
sed -n '1,260p' core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt
sed -n '700,1045p' core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt
sed -n '1,320p' core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerIntelligenceRouter.kt
sed -n '1,620p' core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerPathACreateInterpreter.kt
sed -n '1,340p' domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngine.kt
sed -n '1,360p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerIngressCoordinator.kt
rg --files core/pipeline/src/test domain/scheduler/src/test app-core/src/test | rg "Scheduler|scheduler|PathA|Uni|FastTrack|Reschedule|Reminder|Alarm|ActiveTask"
rg -n "attemptSharedVoiceSchedulerRouting|executeSchedulerTaskCommand|scheduleCascade|cancelReminder|TaskReminderReceiver" core/pipeline/src/main/java domain/scheduler/src/main/java app-core/src/main/java
```

## Current Inputs

Delivered Scheduler Path A inputs include:

- top-level voice text passed to `IntentOrchestrator.processInput(input, isVoice = true, displayedDateIso?)` in `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`
- SIM scheduler drawer audio files transcribed by `AsrService`, then routed as text through `SimSchedulerIngressCoordinator.processTranscript(...)` in `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt` and `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerIngressCoordinator.kt`
- Path B text / compatibility scheduler proposals emitted by `RealUnifiedPipeline.buildSchedulerTaskCommand(...)` in `core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt`
- follow-up scheduler text hosted by `SimAgentFollowUpCoordinator` in `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt`
- manual scheduler UI actions such as `deleteItem`, `toggleDone`, and `onReschedule` in `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt`

Input contracts are modeled as `PipelineInput`, `SchedulerTaskCommand`, and
`PipelineResult` in `core/pipeline/src/main/java/com/smartsales/core/pipeline/PipelineModels.kt`;
Path A task/inspiration DTOs are `FastTrackResult`, `CreateTasksParams`,
`CreateVagueTaskParams`, `RescheduleTaskParams`, and `CreateInspirationParams`
in `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackDtos.kt`.

Delivered behavior: top-level voice and SIM drawer text have direct Path A
routing before or outside heavy Path B. Path B text can still emit scheduler
commands, but top-level voice suppresses later scheduler mutations once a Path A
terminal commit exists.

## Current Outputs

Delivered outputs include:

- `PipelineResult.PathACommitted(task)` for voice create/reschedule commits from `IntentOrchestrator`
- `PipelineResult.InspirationCommitted(id, content)` for top-level voice inspiration commit from `IntentOrchestrator`
- `PipelineResult.ConversationalReply(...)` for explicit rejects, safe failures, and compatibility command results
- persisted `ScheduledTask` rows through `ScheduledTaskRepository` in `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/ScheduledTaskRepository.kt`
- persisted inspiration rows through `InspirationRepository` via `FastTrackMutationEngine`
- SIM UI projection state updates from `SimSchedulerMutationCoordinator` and `SimSchedulerProjectionSupport`
- reminder alarms through `AlarmScheduler.scheduleCascade(...)` when SIM scheduler exact tasks are created/rescheduled, plus receiver notifications in `TaskReminderReceiver`

Delivered behavior: `IntentOrchestrator` emits `PathACommitted` after it can
retrieve the committed task by ID. `RoomScheduledTaskRepository` posts
`DB_WRITE_EXECUTED` telemetry asynchronously after insert/upsert/reschedule.
Unknown: top-level voice `IntentOrchestrator` commits do not directly schedule
Android alarms in the inspected path; reminder arming is clearly present in SIM
UI coordinators and restore/receiver paths, not in `commitVoiceSchedulerIntent`.

## Owner/Routing Chain

Delivered top-level voice route:

1. `IntentOrchestrator.processInput(...)` builds minimal context, evaluates
   `LightningRouter`, mints a `unifiedId`, and creates a `PipelineInput`.
2. For `isVoice = true`, `attemptSharedVoiceSchedulerRouting(...)` calls
   `SchedulerIntelligenceRouter.routeGeneral(...)` when shared router support
   and `ActiveTaskRetrievalIndex` are injected.
3. Create decisions call `commitVoiceSchedulerIntent(...)`, which executes
   `FastTrackMutationEngine` and emits `PathACommitted` for committed tasks.
4. Global reschedule decisions resolve the target through
   `ActiveTaskRetrievalIndex`, resolve time through
   `SchedulerRescheduleTimeInterpreter`, execute `FastTrackMutationEngine`, and
   emit `PathACommitted`.
5. If no terminal Path A commit exists, `RealUnifiedPipeline.processInput(...)`
   still runs. Later scheduler `TaskCommandProposal` / reschedule tool output is
   suppressed when a terminal Path A guard exists.

Fallback route: when the shared router or active-task index is unavailable,
`IntentOrchestrator` runs a legacy bounded `Uni-A -> Uni-B -> Uni-C` cascade
directly against `RealUniAExtractionService`, `RealUniBExtractionService`,
`RealUniCExtractionService`, and `FastTrackMutationEngine`.

SIM drawer route: `SimSchedulerViewModel.processAudio(...)` transcribes audio,
then `SimSchedulerIngressCoordinator.processTranscript(...)` uses
`SchedulerIntelligenceRouter.routeGeneral(...)`; creates are sent to
`SimSchedulerMutationCoordinator`, global reschedules to
`handleResolvedGlobalReschedule(...)`, and failures are normalized into
scheduler-scoped copy.

Target behavior from `docs/core-flow/scheduler-fast-track-flow.md` requires a
shared pre-fork envelope including GUID, urgency, and candidate entity. Delivered
behavior has unified IDs and urgency, but no complete reusable candidate-entity
pre-fork envelope was found in the inspected Path A route.

Interface-map ownership in `docs/cerb/interface-map.md` places
`IntentOrchestrator` in Core Pipeline as the high-level intent router and shared
Path A scheduler spine. The same map records `SchedulerIntelligenceRouter` as
the shared scheduler intent router across voice, Path B text, drawer, follow-up,
and onboarding surfaces. It also keeps scheduler mutation/storage ownership with
the scheduler domain and makes `ActiveTaskRetrievalIndex` scheduler-owned,
derived from canonical active tasks rather than chat or session memory.

## Path A Spine

The spine promised by `docs/cerb/scheduler-path-a-spine/spec.md` is delivered in
broader form than the original T0 placeholder:

- `IntentOrchestrator` remains a single top-level voice Path A writer.
- `PipelineResult.PathACommitted` exists and is emitted after persistence.
- `ScheduledTaskRepository` owns persistence.
- Later-lane scheduler writes are guarded by `SchedulerTerminalCommit`.

The interface guarantee in
`docs/cerb/scheduler-path-a-spine/interface.md` says voice callers pass raw
transcript text to `IntentOrchestrator.processInput(..., isVoice = true)`, and
that `PathACommitted` may emit before later downstream results finish. It also
defines `PathACommitted` as a persisted scheduler task using shared scheduler
thread identity, foreground completion for badge-audio consumers, and the guard
that suppresses downstream Path B scheduler writes for the same thread. It does
not guarantee exact semantic correctness, conflict-free final truth, or Path B
enrichment completion.

Delivered drift: the T0 doc says `FastTrackParser placeholder optimistic task`;
current delivered code has moved to `SchedulerPathACreateInterpreter`,
extractor services, deterministic create branches, and the shared
`SchedulerIntelligenceRouter`. The older spine doc is useful historical
foundation, but current behavior is not placeholder-only.

## Uni-A

Delivered `Uni-A` exact create behavior:

- `SchedulerPathACreateInterpreter` first attempts deterministic relative
  exact create and deterministic day-clock create for bounded cases, then
  attempts `Uni-M`, then `RealUniAExtractionService`.
- `RealUniAExtractionService` calls `ModelRegistry.SCHEDULER_EXTRACTOR`,
  compiles a Uni-A prompt, and lets `SchedulerLinter.parseUniAExtraction(...)`
  convert extractor JSON into `FastTrackResult.CreateTasks` or `NoMatch`.
- `FastTrackMutationEngine.handleCreateTasks(...)` persists exact tasks with
  `isVague = false`, normalized reminder metadata, and `id = unifiedId` for a
  single task when provided.
- `SchedulerIntelligenceRouterTest` covers a deterministic qualified-weekday
  explicit-clock route, while `UniAContractAlignmentTest` guards prompt/linter
  schema alignment.

Gap against target behavior: the core-flow says exact-create should be decided
by a lightweight semantic pass and not by hardcoded Kotlin heuristics. Delivered
behavior includes deterministic relative/day-clock shortcuts before extractor
fallback. Those shortcuts may be intentional SIM/product repairs, but they are a
delivered behavior to resolve in the target-flow sprint.

## Uni-B

Delivered `Uni-B` vague create behavior:

- `SchedulerPathACreateInterpreter` reaches `RealUniBExtractionService` only
  after deterministic/Uni-M/Uni-A exact routes do not commit.
- `RealUniBExtractionService` uses the scheduler extractor prompt and
  `SchedulerLinter.parseUniBExtraction(...)`.
- `FastTrackMutationEngine.handleCreateVagueTask(...)` persists a task with
  `isVague = true`, `timeDisplay = "待定"`, a day-bucket anchor at start of day,
  notes beginning with `时间待定`, `durationMinutes = 0`, `hasConflict = false`,
  and normalized reminder metadata that produces no alarm cascade for vague
  tasks.
- `FastTrackMutationEngineTest` covers unified ID preservation and conflict
  bypass for vague creation. `UniBContractAlignmentTest` guards prompt/linter
  schema and anti-fabrication wording.

Delivered behavior matches the current `docs/cerb/scheduler-path-a-uni-b/spec.md`
date-anchored slice. Fully undated vague input is not proven as delivered and
should remain a known gap rather than being treated as implemented.

## Uni-C

Delivered `Uni-C` inspiration behavior:

- In the legacy voice cascade, `IntentOrchestrator` attempts `Uni-C` after
  `Uni-A` and `Uni-B` decline.
- In shared voice routing, `Decision.NotMatched` triggers a bounded
  `UniCExtractionRequest`; accepted inspirations execute through
  `FastTrackMutationEngine`.
- `FastTrackMutationEngine.handleCreateInspiration(...)` trims content, refuses
  blank content with `MutationResult.NoMatch`, and writes only through
  `InspirationRepository.insert(...)`.
- `PipelineResult.InspirationCommitted` is emitted for top-level voice
  inspiration success, while SIM drawer routes accepted inspiration through
  `SimSchedulerMutationCoordinator.handleMutation(...)`.
- `UniCContractAlignmentTest` guards prompt/linter schema; `FastTrackMutationEngineTest`
  covers inspiration insertion and blank-content refusal.

Delivered behavior: inspiration is separated from scheduler task persistence in
the inspected mutation engine. Unknown: no runtime/UI-visible evidence was
captured in this sprint for the distinct inspiration render path.

## Uni-D

Delivered `Uni-D` conflict-visible create behavior:

- `Uni-D` is not a separate extraction service. Exact-create payloads from
  Uni-A/deterministic/Uni-M enter `FastTrackMutationEngine.handleCreateTasks(...)`.
- The engine computes conflict occupancy with
  `effectiveConflictOccupancyMinutes(...)`; `FIRE_OFF` bypasses collision logic
  via `bypassesConflictEvaluation(...)`.
- Non-`FIRE_OFF` exact tasks call `ScheduleBoard.checkConflict(...)`.
- On overlap, the persisted task keeps `isVague = false`, sets
  `hasConflict = true`, `conflictWithTaskId`, and a user-visible
  `conflictSummary` such as `与「牙医预约」时间冲突`.
- `FastTrackMutationEngineTest` covers conflict-visible persistence, smart
  conflict occupancy for zero-duration transport tasks, explicit duration
  override, and `FIRE_OFF` conflict bypass.

Delivered behavior matches the core rule that conflict does not reject creation.
Unknown: this sprint did not capture `adb logcat` or UI screenshots proving
runtime conflict-card rendering; the map only proves source/test behavior.

## Reschedule/Delete

Delivered reschedule behavior:

- Top-level voice and Path B text can enter global reschedule through
  `SchedulerIntelligenceRouter.mightExpressReschedule(...)` and
  `RealGlobalRescheduleExtractionService`.
- `ActiveTaskRetrievalIndex.buildShortlist(...)` derives candidates from
  `ScheduledTaskRepository.getActiveTasks()`, and `resolveTarget(...)` applies
  score and margin gates before returning `Resolved`, `Ambiguous`, or `NoMatch`.
- `SchedulerRescheduleTimeInterpreter.resolveNaturalInstruction(...)` supports
  signed deltas for exact tasks, rejects deltas for vague tasks, resolves
  explicit day-clock instructions, and can fall back to Uni-A exact extraction.
- `FastTrackMutationEngine.handleRescheduleTask(...)` resolves a task, computes
  conflict excluding the old task ID, copies the old task with a new start,
  duration, conflict state, and `isVague = false`, then calls
  `ScheduledTaskRepository.rescheduleTask(oldTaskId, newTask)`.
- `RoomScheduledTaskRepository.rescheduleTask(...)` enforces GUID inheritance by
  copying `newTask.id = oldTaskId` before DAO reschedule.
- SIM `SimSchedulerMutationCoordinator.executeResolvedReschedule(...)` also
  cancels and re-schedules reminders around a reschedule.

Delivered delete behavior:

- `SchedulerIntelligenceRouter.routeGeneral(...)` and `routeFollowUp(...)`
  reject deletion-shaped transcripts with scheduler-scoped messages.
- `RealUnifiedPipeline` can still build a `SchedulerTaskCommand.DeleteTask` for
  legacy `"deletion"` mutations; `IntentOrchestrator.executeSchedulerTaskCommand`
  deletes by `scheduleBoard.findLexicalMatch(...)`.
- SIM UI manual `deleteItem(id)` deletes by canonical ID and cancels reminders.

Gap/Ambiguity: the SIM core-flow includes delete as an included branch, but
voice deletion is explicitly unsupported in the shared router. Manual UI delete
is delivered; voice delete is not delivered as Path A mutation.

## Conflict Behavior

Conflict behavior is domain-owned in the inspected delivered path:

- exact creates and reschedules evaluate `ScheduleBoard.checkConflict(...)`;
  reschedule excludes the old task ID.
- `FIRE_OFF` bypasses conflict evaluation entirely.
- explicit duration wins for conflict occupancy; otherwise semantic/default
  conflict occupancy is used without overwriting persisted visible duration.
- vague tasks bypass conflict evaluation and persist `hasConflict = false`.
- conflict evidence persisted on tasks is `hasConflict`, `conflictWithTaskId`,
  and `conflictSummary`.

Target behavior in `docs/core-flow/scheduler-fast-track-flow.md` also requires
separate scheduler-card urgency/conflict/completion signals. Delivered domain
state carries separate fields, but this sprint did not inspect enough UI render
code or capture screenshots to prove every visual signal remains distinct.

## SIM Overlay

Delivered SIM behavior aligns with `docs/core-flow/sim-scheduler-path-a-flow.md`
in these ways:

- SIM scheduler drawer routes through Path A-backed scheduler collaborators, not
  Path B/CRM/plugin memory.
- The scheduler drawer handles exact create, vague create, conflict-visible
  create, inspiration capture, and global reschedule through
  `SimSchedulerIngressCoordinator`.
- SIM failures stay scheduler-scoped through
  `normalizeSimSchedulerDrawerFailureMessage(...)`.
- SIM manual delete and done actions mutate scheduler truth by ID and cancel
  reminders.
- SIM reschedule uses scheduler-owned target/time logic and re-arms exact
  reminders after replacement.

Gap/Ambiguity: SIM overlay says delete is an included branch, but voice delete
through the router is rejected with `SIM 当前不支持语音删除，请在面板手动操作`.
That is delivered behavior, not target-complete delete voice support. SIM also
contains local deterministic helper code in `SimSchedulerIngressCoordinator`,
while shared create routing now lives in `SchedulerPathACreateInterpreter` /
`SharedPathACreateInterpreter`; future cleanup should check for drift between
those paths.

## Telemetry

Delivered telemetry surfaces:

- `IntentOrchestrator` emits `PipelineValve` checkpoints such as
  `INPUT_RECEIVED`, `ROUTER_DECISION`, `PATH_A_PARSED`, `TASK_EXTRACTED`,
  `TASK_EXTRACTED_VAGUE`, `THOUGHT_EXTRACTED`, `CONFLICT_EVALUATED`,
  `PATH_A_DB_WRITTEN`, `TASK_COMMAND_ROUTED`, and mutation commit checkpoints.
- `RoomScheduledTaskRepository` posts `DB_WRITE_EXECUTED` through
  `SchedulerTelemetryDispatcher`.
- SIM scheduler ingress logs route preflight, route decision, create result,
  UI failure, and single-task extraction telemetry.
- `TaskReminderReceiver` logs reminder receipt and notification display, emits
  `SchedulerReminderSurfaceBus` for early reminders, and emits
  `SchedulerRefreshBus` for deadline reminders.

Gap: the canonical valves in core-flow docs do not exactly match the current
telemetry event names. This map found many useful joints, but no complete
one-to-one canonical valve implementation. No `adb logcat` evidence was
captured, so runtime telemetry delivery is unproven here.

## Tests

Relevant delivered test coverage found:

- `core/pipeline/src/test/java/com/smartsales/core/pipeline/SchedulerIntelligenceRouterTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/UniAContractAlignmentTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/UniBContractAlignmentTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/UniCContractAlignmentTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/GlobalRescheduleContractAlignmentTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/SchedulerRescheduleTimeInterpreterTest.kt`
- `domain/scheduler/src/test/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngineTest.kt`
- `domain/scheduler/src/test/java/com/smartsales/prism/domain/scheduler/AlarmSchedulerTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/scheduler/RealActiveTaskRetrievalIndexTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/scheduler/RoomScheduledTaskRepositoryTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSchedulerMutationCoordinatorTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSchedulerViewModelTest.kt`

Test reality: coverage is meaningful for DTO/prompt alignment, mutation engine
semantics, target resolution, reschedule time interpretation, repository
telemetry, alarm scheduling fakes, and SIM mutation coordination. Unknown:
this sprint did not run the test suite; it inventories source/test coverage and
records commands for future verification.

## Known Gaps

- Gap: no runtime/L3 evidence was captured. Android reminders, notifications,
  foreground banners, and UI card rendering still require `adb logcat` and/or
  screenshot evidence before making runtime claims.
- Gap: top-level voice `IntentOrchestrator` Path A commits do not visibly arm
  alarms in the inspected commit path; SIM UI coordinators do arm reminders for
  exact creates/reschedules.
- Gap: shared pre-fork candidate entity extraction from the core-flow was not
  found as a complete delivered envelope.
- Gap: deterministic exact-create shortcuts exist before lightweight extractor
  fallback, which may conflict with the core-flow's "semantic extraction as
  source of truth" wording.
- Gap: voice delete is explicitly rejected by `SchedulerIntelligenceRouter`,
  while SIM core-flow lists delete as included. Manual delete is delivered.
- Gap: fully undated vague create is not proven as delivered; current Uni-B is
  the date-anchored slice.
- Gap: canonical core-flow valve names and current telemetry checkpoint names
  are not fully aligned.
- Gap: the physical code contains shared and SIM-local create routing helpers;
  future target-flow work should decide whether this is acceptable reuse or
  drift.
