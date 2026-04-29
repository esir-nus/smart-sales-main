---
protocol: BAKE
version: 1.0
domain: scheduler-path-a
layer: L2-L4 scheduler corridor
runtime: base-runtime-active
owner: core/pipeline, domain/scheduler, app-core SIM scheduler
core-flow-doc:
  - docs/core-flow/scheduler-fast-track-flow.md
  - docs/core-flow/sim-scheduler-path-a-flow.md
last-verified: 2026-04-29
---

# Scheduler Path A BAKE Contract

This contract records the delivered implementation for Scheduler Path A. The
core-flow docs remain the behavioral north stars; this BAKE contract is the
implementation record and tracks gaps explicitly.

## Pipeline Contract

### Inputs

- Top-level voice text entering `IntentOrchestrator.processInput(input,
  isVoice = true, displayedDateIso?)`.
- SIM scheduler drawer transcripts routed through
  `SimSchedulerIngressCoordinator.processTranscript(...)`.
- Path B text and compatibility scheduler proposals emitted by
  `RealUnifiedPipeline.buildSchedulerTaskCommand(...)`.
- Follow-up scheduler text hosted by `SimAgentFollowUpCoordinator`.
- Manual scheduler UI actions such as delete, done toggle, and reschedule.

### Outputs / Guarantees

- `PipelineResult.PathACommitted(task)` is emitted after a top-level voice
  scheduler task is persisted and the committed task can be retrieved.
- `PipelineResult.InspirationCommitted(id, content)` is emitted for top-level
  voice inspiration commits.
- Scheduler-scoped conversational replies are emitted for explicit rejects,
  safe failures, and compatibility command results.
- Scheduler truth is persisted through `ScheduledTaskRepository`; inspiration
  truth is persisted through `InspirationRepository`.
- SIM projection state updates through scheduler-owned mutation and projection
  coordinators, while exact SIM creates and reschedules arm reminder cascades.
- Later-lane scheduler writes are suppressed for the same scheduler thread after
  an early terminal Path A commit.

### Invariants

- MUST keep scheduler mutation truth owned by scheduler collaborators, not by
  chat, selected UI state, or Path B enrichment.
- MUST preserve the shared scheduler thread identity when Path A commits.
- MUST refuse null, blank, or meaningless input without writing scheduler or
  inspiration state.
- MUST not fabricate exact time; vague tasks stay vague unless a lawful exact
  time is present.
- MUST persist exact conflicts visibly instead of rejecting the user's intent.
- MUST keep inspiration storage separate from the scheduler task table.
- MUST fail safely for no-match, ambiguous, or unsupported reschedule/delete
  target resolution.
- MUST keep `FIRE_OFF` collision bypass separate from ordinary urgency display.

### Error Paths

- Empty, garbled, or non-schedulable input returns explicit failure or falls
  through without mutation.
- Ambiguous or no-match reschedule targets return scheduler-scoped safe failure.
- Vague-task signed-delta reschedule fails because there is no lawful stored
  exact base time to shift.
- Voice delete is rejected by shared routing today; manual delete by canonical
  task ID is delivered.
- Fully undated vague create is not proven delivered and must not be treated as
  implemented.
- Runtime reminder, notification, card-render, and telemetry-delivery claims
  require fresh device-loop evidence; source and JVM tests are not L3 proof.

## Telemetry Joints

- [INPUT_RECEIVED]: top-level voice input, SIM drawer transcript, Path B text
  scheduler proposal, follow-up text, manual scheduler UI action.
- [ROUTER_DECISION]: `LightningRouter`, shared
  `SchedulerIntelligenceRouter`, deterministic create helpers, Uni-A/B/C
  extraction, global reschedule extraction, and compatibility command routing.
- [TASK_EXTRACTED]: delivered `PipelineValve` and route logs for exact-create
  extraction, including deterministic exact-create branches and Uni-A fallback.
- [TASK_EXTRACTED_VAGUE]: delivered route and linter surfaces for date-anchored
  Uni-B vague create.
- [THOUGHT_EXTRACTED]: delivered Uni-C inspiration extraction and mutation
  branch.
- [TARGET_RESOLVED]: delivered active-task index and reschedule-time interpreter
  evidence for global reschedule.
- [CONFLICT_EVALUATED]: delivered mutation-engine conflict evaluation and
  `FIRE_OFF` bypass handling.
- [DB_WRITE_EXECUTED]: delivered repository telemetry from
  `RoomScheduledTaskRepository` after insert, upsert, or reschedule.
- [SIM_SCHEDULER_ENTERED]: delivered SIM ingress route preflight, route
  decision, create result, UI failure, and single-task extraction logs.
- [REMINDER_SURFACE]: delivered reminder receipt and notification-display logs
  from `TaskReminderReceiver`, plus SIM exact create/reschedule reminder
  scheduling paths.

Current gaps: canonical valve names in the core-flow docs do not fully match
delivered telemetry checkpoint names, and no fresh `adb logcat` evidence was
captured for L3 runtime delivery. Future evidence must converge canonical valve
names and delivered names rather than translating them ad hoc.

## UI Docking Surface

Scheduler UI attaches to repository/projection state produced by scheduler
owners. SIM scheduler surfaces can present exact, vague, conflict, inspiration,
reschedule, manual delete, done, and failure states, but mutation authority
stays in scheduler-owned routing, mutation, repository, reminder, and projection
collaborators.

Future L3/runtime scenario controls must be centralized under Debug HUD /
Debug Lab direction, with `L2DebugHud` as the current anchor. Do not add
scattered feature-local debug buttons for Scheduler Path A, Connectivity /
Badge Session, Audio Pipeline, Shell Routing, Pipeline Telemetry, or adjacent
product UI unless a sprint scopes an explicit exception. Debug triggers are
valid only as controlled entrypoints into real pipeline paths or realistic
upstream module I/O simulation; evidence still comes from real execution.

## Core-Flow Gap

- Gap: alarm arming is proven for SIM exact creates and reschedules through SIM
  UI coordinators, but top-level voice alarm arming is not proven in the
  inspected `IntentOrchestrator` commit path.
- Gap: the shared pre-fork candidate entity envelope is incomplete; delivered
  behavior has unified IDs and urgency, but not the full reusable candidate
  entity envelope required by the scheduler core flow.
- Gap: deterministic exact-create shortcuts run before semantic extraction
  fallback. They may be legitimate scheduler-owned safety branches, but the
  target-flow decision is not closed.
- Gap: voice delete is rejected while SIM manual delete by canonical ID is
  delivered.
- Gap: fully undated vague create is not proven delivered; current Uni-B
  coverage is the date-anchored slice.
- Gap: canonical valve names are not fully aligned with delivered
  `PipelineValve`, `DB_WRITE_EXECUTED`, SIM ingress, and reminder log names.
- Gap: shared and SIM-local create-routing helpers may represent drift if they
  diverge; SIM-local create routing must either remain explicitly justified or
  fold into shared scheduler-owned routing.
- Gap: runtime reminders, notifications, foreground banners, UI card rendering,
  and telemetry delivery do not have fresh device-loop evidence in Sprint 05/07.

## Test Contract

- Evidence class: contract-test for this docs-only BAKE write; platform-runtime
  for any future Android reminder, notification, UI-render, or telemetry claim.
- Existing source/test coverage includes
  `SchedulerIntelligenceRouterTest`, `UniAContractAlignmentTest`,
  `UniBContractAlignmentTest`, `UniCContractAlignmentTest`,
  `GlobalRescheduleContractAlignmentTest`,
  `SchedulerRescheduleTimeInterpreterTest`,
  `FastTrackMutationEngineTest`, `AlarmSchedulerTest`,
  `RealActiveTaskRetrievalIndexTest`, `RoomScheduledTaskRepositoryTest`,
  `SimSchedulerMutationCoordinatorTest`, and `SimSchedulerViewModelTest`.
- Minimum verification for this contract: prove BAKE frontmatter and sections
  exist, prove the Sprint 05/06 gaps are recorded, prove the interface map and
  Scheduler Path A Cerb docs cite this contract as authority/reference, and run
  `git diff --check` for scoped docs.
- Minimum future runtime closure: centralized Debug HUD / Debug Lab scenario
  entry, `adb logcat` evidence for declared telemetry joints, persisted state
  proof, and screenshots or observed UI state only as supporting artifacts.

## Cross-Platform Notes

- This contract is platform-neutral product truth for Scheduler Path A. Android
  code is implementation evidence, not a HarmonyOS or iOS delivery
  prescription.
- Native implementations must preserve scheduler-owned mutation truth, exact
  versus vague semantics, conflict-visible persistence, safe reschedule/delete
  failure, inspiration separation, reminder cascade semantics, and canonical
  telemetry joints.
- Reminder, notification, ASR, foreground service, alarm, logging, and UI
  mechanics are platform-owned adapters. Do not claim parity on another
  platform without platform-native runtime evidence.
