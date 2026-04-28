# Sprint Contract: 05-scheduler-path-a-dbm

## 1. Header

- **Slug**: scheduler-path-a-dbm
- **Project**: bake-transformation
- **Date authored**: 2026-04-28
- **Author**: Claude-authored contract lineage; Codex refined from Sprint 04 close
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Produce the delivered-behavior map for Scheduler Path A as the
next tier-1 BAKE domain after the closed connectivity BAKE contract.

**Codex interpretation**: This sprint is a read-heavy docs sprint. The operator
records what current committed docs and code actually deliver for Scheduler Path
A creation, routing, Uni-A/B/C/D extraction, reschedule/delete handling,
conflicts, SIM overlay constraints, telemetry, and tests. It does not optimize
target behavior, update core-flow docs, demote Cerb authority, or write a BAKE
implementation contract.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/05-scheduler-path-a-dbm.md`
- `docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/`

No files outside this list may be written.

Out of scope:

- code edits
- target core-flow updates
- Cerb demotion, archive moves, or authority changes
- BAKE implementation contract writing
- runtime, device, adb, or L3 claims without explicit evidence captured during
  this sprint

## 4. References

Governance and project protocol:

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `.agent/rules/lessons-learned.md`
- `docs/reference/agent-lessons-details.md`

Scheduler contracts and discovery docs:

- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-spine/interface.md`
- `docs/cerb/scheduler-path-a-uni-a/spec.md`
- `docs/cerb/scheduler-path-a-uni-b/spec.md`
- `docs/cerb/scheduler-path-a-uni-c/spec.md`
- `docs/cerb/scheduler-path-a-uni-d/spec.md`
- `docs/cerb/interface-map.md`

Code and test inventory:

- `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerIntelligenceRouter.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerPathACreateInterpreter.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerRescheduleTimeInterpreter.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/`
- `app-core/src/main/java/com/smartsales/prism/data/scheduler/`
- scheduler-related tests under `core/pipeline/src/test`,
  `domain/scheduler/src/test`, and `app-core/src/test`

## 5. Success Exit Criteria

1. Delivered-behavior map exists at
   `docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`.
   Verify: `test -f docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`

2. The map covers current inputs and outputs, owner/routing chain, Path A
   spine, Uni-A, Uni-B, Uni-C, Uni-D delivered behavior, reschedule/delete
   behavior, conflict behavior, SIM overlay constraints, telemetry and tests,
   and known gaps.
   Verify:
   `rg -n "## (Current Inputs|Current Outputs|Owner/Routing Chain|Path A Spine|Uni-A|Uni-B|Uni-C|Uni-D|Reschedule/Delete|Conflict Behavior|SIM Overlay|Telemetry|Tests|Known Gaps)" docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`

3. The map cites concrete source docs and code rather than relying on agent
   narration. It must cite at least 12 distinct repository paths, including at
   least 4 scheduler docs and at least 4 Kotlin source or test paths.
   Verify:
   `rg -o '`(docs|core|domain|app-core)/[^`]+`' docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md | sort -u | wc -l`

4. The map distinguishes delivered behavior from core-flow or Cerb target
   behavior where those layers are ahead of code, stale, or ambiguous.
   Verify:
   `rg -n "Delivered behavior|Target behavior|Gap|Ambiguity|Stale|Unknown" docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`

5. The map records evidence commands used to inspect Scheduler Path A sources
   and tests, including repository search and source reads.
   Verify:
   `rg -n "Evidence Commands|rg |sed -n|find " docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`

6. Tracker row for sprint 05 is updated to `done`, `stopped`, or `blocked`
   with a factual one-line summary.
   Verify:
   `rg -n "\| 05 \| scheduler-path-a-dbm \| (done|stopped|blocked)" docs/projects/bake-transformation/tracker.md`

## 6. Stop Exit Criteria

- **Scope blocker**: The delivered behavior cannot be mapped without writing
  outside the sprint scope.
- **Source ambiguity blocker**: Current code and docs conflict so strongly that
  the operator cannot write a factual delivered-behavior map without a product
  decision.
- **Runtime-evidence blocker**: Runtime, notification, alarm, background, or
  device behavior requires fresh adb/logcat evidence. Record the missing proof
  as a gap; do not claim L3 behavior without it.
- **Authority creep**: Any pull toward target core-flow edits, Cerb demotion,
  BAKE contract writing, or code changes.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `wc -l docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`
2. `rg -n "Current Inputs|Owner/Routing Chain|Path A Spine|Uni-A|Uni-B|Uni-C|Uni-D|Known Gaps" docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`
3. `rg -n "IntentOrchestrator|SchedulerIntelligenceRouter|RealUnifiedPipeline|FastTrackMutationEngine|RoomScheduledTaskRepository|RealAlarmScheduler|TaskReminderReceiver" docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`
4. `rg -n "Delivered behavior|Target behavior|Gap|Unknown|adb logcat" docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`
5. `rg -o '`(docs|core|domain|app-core)/[^`]+`' docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md | sort -u | wc -l`
6. `git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/05-scheduler-path-a-dbm.md docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/`

## 9. Iteration Ledger

- Iteration 1 — Read the sprint contract, repo rules, BAKE protocol,
  sprint/project structure specs, lessons index/details for architecture and
  cross-platform triggers, scheduler core-flow docs, scheduler Cerb shards,
  interface map, current routing/mutation/repository/reminder code, SIM ingress
  code, and scheduler-related tests. Wrote the delivered-behavior map at
  `docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`,
  updated the project tracker, and ran the required evidence commands. The map
  records runtime/logcat evidence as a gap rather than making L3 claims.

## 10. Closeout

- **Status**: success
- **Tracker summary**: Delivered behavior map created for Scheduler Path A,
  covering shared routing, Uni-A/B/C/D, reschedule/delete, SIM overlay,
  telemetry, tests, and runtime-evidence gaps.
- **Files changed**:
  - `docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`
  - `docs/projects/bake-transformation/sprints/05-scheduler-path-a-dbm.md`
  - `docs/projects/bake-transformation/tracker.md`
- **Evidence**:

```text
$ wc -l docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md
372 docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md

$ rg -n "Current Inputs|Owner/Routing Chain|Path A Spine|Uni-A|Uni-B|Uni-C|Uni-D|Known Gaps" docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md
32:## Current Inputs
72:## Owner/Routing Chain
92:`IntentOrchestrator` runs a legacy bounded `Uni-A -> Uni-B -> Uni-C` cascade
116:## Path A Spine
142:## Uni-A
144:Delivered `Uni-A` exact create behavior:
150:  compiles a Uni-A prompt, and lets `SchedulerLinter.parseUniAExtraction(...)`
165:## Uni-B
167:Delivered `Uni-B` vague create behavior:
170:  after deterministic/Uni-M/Uni-A exact routes do not commit.
186:## Uni-C
188:Delivered `Uni-C` inspiration behavior:
190:- In the legacy voice cascade, `IntentOrchestrator` attempts `Uni-C` after
191:  `Uni-A` and `Uni-B` decline.
208:## Uni-D
210:Delivered `Uni-D` conflict-visible create behavior:
212:- `Uni-D` is not a separate extraction service. Exact-create payloads from
213:  Uni-A/deterministic/Uni-M enter `FastTrackMutationEngine.handleCreateTasks(...)`.
241:  explicit day-clock instructions, and can fall back to Uni-A exact extraction.
351:## Known Gaps
366:- Gap: fully undated vague create is not proven as delivered; current Uni-B is

$ rg -n "IntentOrchestrator|SchedulerIntelligenceRouter|RealUnifiedPipeline|FastTrackMutationEngine|RoomScheduledTaskRepository|RealAlarmScheduler|TaskReminderReceiver" docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md
22:sed -n '1,260p' core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt
23:sed -n '700,1045p' core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt
24:sed -n '1,320p' core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerIntelligenceRouter.kt
26:sed -n '1,340p' domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngine.kt
29:rg -n "attemptSharedVoiceSchedulerRouting|executeSchedulerTaskCommand|scheduleCascade|cancelReminder|TaskReminderReceiver" core/pipeline/src/main/java domain/scheduler/src/main/java app-core/src/main/java
36:- top-level voice text passed to `IntentOrchestrator.processInput(input, isVoice = true, displayedDateIso?)` in `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`
38:- Path B text / compatibility scheduler proposals emitted by `RealUnifiedPipeline.buildSchedulerTaskCommand(...)` in `core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt`
57:- `PipelineResult.PathACommitted(task)` for voice create/reschedule commits from `IntentOrchestrator`
58:- `PipelineResult.InspirationCommitted(id, content)` for top-level voice inspiration commit from `IntentOrchestrator`
61:- persisted inspiration rows through `InspirationRepository` via `FastTrackMutationEngine`
63:- reminder alarms through `AlarmScheduler.scheduleCascade(...)` when SIM scheduler exact tasks are created/rescheduled, plus receiver notifications in `TaskReminderReceiver`
65:Delivered behavior: `IntentOrchestrator` emits `PathACommitted` after it can
66:retrieve the committed task by ID. `RoomScheduledTaskRepository` posts
68:Unknown: top-level voice `IntentOrchestrator` commits do not directly schedule
76:1. `IntentOrchestrator.processInput(...)` builds minimal context, evaluates
79:   `SchedulerIntelligenceRouter.routeGeneral(...)` when shared router support
82:   `FastTrackMutationEngine` and emits `PathACommitted` for committed tasks.
85:   `SchedulerRescheduleTimeInterpreter`, execute `FastTrackMutationEngine`, and
87:5. If no terminal Path A commit exists, `RealUnifiedPipeline.processInput(...)`
92:`IntentOrchestrator` runs a legacy bounded `Uni-A -> Uni-B -> Uni-C` cascade
94:`RealUniCExtractionService`, and `FastTrackMutationEngine`.
98:`SchedulerIntelligenceRouter.routeGeneral(...)`; creates are sent to
109:`IntentOrchestrator` in Core Pipeline as the high-level intent router and shared
110:Path A scheduler spine. The same map records `SchedulerIntelligenceRouter` as
121:- `IntentOrchestrator` remains a single top-level voice Path A writer.
128:transcript text to `IntentOrchestrator.processInput(..., isVoice = true)`, and
139:`SchedulerIntelligenceRouter`. The older spine doc is useful historical
152:- `FastTrackMutationEngine.handleCreateTasks(...)` persists exact tasks with
155:- `SchedulerIntelligenceRouterTest` covers a deterministic qualified-weekday
173:- `FastTrackMutationEngine.handleCreateVagueTask(...)` persists a task with
178:- `FastTrackMutationEngineTest` covers unified ID preservation and conflict
190:- In the legacy voice cascade, `IntentOrchestrator` attempts `Uni-C` after
194:  `FastTrackMutationEngine`.
195:- `FastTrackMutationEngine.handleCreateInspiration(...)` trims content, refuses
201:- `UniCContractAlignmentTest` guards prompt/linter schema; `FastTrackMutationEngineTest`
213:  Uni-A/deterministic/Uni-M enter `FastTrackMutationEngine.handleCreateTasks(...)`.
221:- `FastTrackMutationEngineTest` covers conflict-visible persistence, smart
234:  `SchedulerIntelligenceRouter.mightExpressReschedule(...)` and
242:- `FastTrackMutationEngine.handleRescheduleTask(...)` resolves a task, computes
246:- `RoomScheduledTaskRepository.rescheduleTask(...)` enforces GUID inheritance by
253:- `SchedulerIntelligenceRouter.routeGeneral(...)` and `routeFollowUp(...)`
255:- `RealUnifiedPipeline` can still build a `SchedulerTaskCommand.DeleteTask` for
256:  legacy `"deletion"` mutations; `IntentOrchestrator.executeSchedulerTaskCommand`
311:- `IntentOrchestrator` emits `PipelineValve` checkpoints such as
315:- `RoomScheduledTaskRepository` posts `DB_WRITE_EXECUTED` through
319:- `TaskReminderReceiver` logs reminder receipt and notification display, emits
332:- `core/pipeline/src/test/java/com/smartsales/core/pipeline/SchedulerIntelligenceRouterTest.kt`
338:- `domain/scheduler/src/test/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngineTest.kt`
341:- `app-core/src/test/java/com/smartsales/prism/data/scheduler/RoomScheduledTaskRepositoryTest.kt`
356:- Gap: top-level voice `IntentOrchestrator` Path A commits do not visibly arm
364:- Gap: voice delete is explicitly rejected by `SchedulerIntelligenceRouter`,

$ rg -n "Delivered behavior|Target behavior|Gap|Unknown|adb logcat" docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md
48:Delivered behavior: top-level voice and SIM drawer text have direct Path A
65:Delivered behavior: `IntentOrchestrator` emits `PathACommitted` after it can
68:Unknown: top-level voice `IntentOrchestrator` commits do not directly schedule
103:Target behavior from `docs/core-flow/scheduler-fast-track-flow.md` requires a
159:Gap against target behavior: the core-flow says exact-create should be decided
182:Delivered behavior matches the current `docs/cerb/scheduler-path-a-uni-b/spec.md`
204:Delivered behavior: inspiration is separated from scheduler task persistence in
205:the inspected mutation engine. Unknown: no runtime/UI-visible evidence was
225:Delivered behavior matches the core rule that conflict does not reject creation.
226:Unknown: this sprint did not capture `adb logcat` or UI screenshots proving
260:Gap/Ambiguity: the SIM core-flow includes delete as an included branch, but
277:Target behavior in `docs/core-flow/scheduler-fast-track-flow.md` also requires
299:Gap/Ambiguity: SIM overlay says delete is an included branch, but voice delete
323:Gap: the canonical valves in core-flow docs do not exactly match the current
325:one-to-one canonical valve implementation. No `adb logcat` evidence was
347:telemetry, alarm scheduling fakes, and SIM mutation coordination. Unknown:
351:## Known Gaps
353:- Gap: no runtime/L3 evidence was captured. Android reminders, notifications,
354:  foreground banners, and UI card rendering still require `adb logcat` and/or
356:- Gap: top-level voice `IntentOrchestrator` Path A commits do not visibly arm
359:- Gap: shared pre-fork candidate entity extraction from the core-flow was not
361:- Gap: deterministic exact-create shortcuts exist before lightweight extractor
364:- Gap: voice delete is explicitly rejected by `SchedulerIntelligenceRouter`,
366:- Gap: fully undated vague create is not proven as delivered; current Uni-B is
368:- Gap: canonical core-flow valve names and current telemetry checkpoint names
370:- Gap: the physical code contains shared and SIM-local create routing helpers;

$ rg -o '`(docs|core|domain|app-core)/[^`]+`' docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md | sort -u | wc -l
27

$ git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/05-scheduler-path-a-dbm.md docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/
 .../delivered-behavior-map.md                      | 372 +++++++++++++++++++++
 .../sprints/05-scheduler-path-a-dbm.md             | 288 ++++++++++++++++
 docs/projects/bake-transformation/tracker.md       |   3 +-
 3 files changed, 662 insertions(+), 1 deletion(-)
```

- **Runtime evidence**: not claimed. Scheduler reminders, notification
  delivery, foreground banners, and UI card rendering still require fresh
  `adb logcat` and/or screenshot evidence in a future runtime sprint.
- **Lesson proposals**: none.
- **CHANGELOG line**: none; internal BAKE transformation docs only.
