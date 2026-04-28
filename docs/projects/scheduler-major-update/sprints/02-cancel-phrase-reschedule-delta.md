# Sprint 02 - cancel-phrase-reschedule-delta

## Header

- Project: scheduler-major-update
- Sprint: 02
- Slug: cancel-phrase-reschedule-delta
- Date authored: 2026-04-28
- Author: Claude
- Operator: Codex (default)
- Evaluator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):**

> 晚上8点的开会取消了，得去机场接人。

**Interpretation:**

Cancel wording plus a replacement event at the same time anchor is a reschedule/replacement, not a pure delete. For the target phrase, the existing active task `开会 · 20:00` must be replaced with `去机场接人 · 20:00` through production scheduler routing. Pure cancellation without a replacement event, such as `取消晚上8点的开会`, remains unsupported/blocked.

## Scope

Files the operator may touch:

- `core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerIntelligenceRouter.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/PromptCompiler.kt` - only global reschedule extraction prompt/routing language
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterParsingSupport.kt`
- `domain/scheduler/src/test/java/com/smartsales/prism/domain/scheduler/SchedulerLinterTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/RealGlobalRescheduleExtractionServiceTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/GlobalRescheduleContractAlignmentTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/SchedulerIntelligenceRouterTest.kt`
- `core/pipeline/src/test/java/com/smartsales/core/pipeline/IntentOrchestratorTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSchedulerViewModelTest.kt`
- `docs/core-flow/scheduler-fast-track-flow.md` - only if the replacement-cancel rule is stale or missing
- `docs/specs/scheduler-path-a-execution-prd.md` - only if stale historical wording directly conflicts with the replacement-cancel rule; do not treat it as the active source of truth
- `docs/cerb/scheduler-path-a-spine/spec.md` - only if the active implementation contract needs sync for the replacement-cancel rule
- `docs/cerb/scheduler-path-a-uni-a/spec.md` - only if the active implementation contract needs sync for the replacement-cancel rule
- `docs/projects/scheduler-major-update/tracker.md`
- `docs/projects/scheduler-major-update/sprints/02-cancel-phrase-reschedule-delta.md`
- `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/**`

Debug control scope, only if needed to replay the exact phrase without bypassing production routing:

- `app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSchedulerViewModelTest.kt`

Out of scope:

- Manual scheduler card delete UX.
- `OnboardingQuickStartService` and onboarding sandbox parity.
- Harmony delivery.
- `CHANGELOG.md`; operator only proposes a closeout line for user gating.
- A working global voice delete feature. Pure delete remains unsupported/blocked.

## References

- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/specs/device-loop-protocol.md`
- `docs/sops/debugging.md` - Pre-Fix Report is mandatory after the failing baseline and before behavior edits
- `docs/core-flow/scheduler-fast-track-flow.md` - invariant 8, reschedule is replacement
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-uni-a/spec.md`
- `docs/cerb/interface-map.md` - scheduler write ownership
- `.agent/rules/lessons-learned.md` - prompt/linter and signature-change triggers
- `docs/reference/agent-lessons-details.md` - prompt/linter alignment details when touching extraction prompts
- `docs/projects/scheduler-major-update/sprints/01-time-anchor-retitle.md` - prior valid scheduler-debug-button evidence route

## Success Exit Criteria

1. Baseline L3 evidence is captured before behavior edits under `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/` and proves the failing boundary for the exact phrase `晚上8点的开会取消了，得去机场接人。`: early `looksLikeDeletionTranscript`, Lightning Router `classification=deletion`, global-reschedule extractor rejection, and command mapping to `SchedulerTaskCommand.DeleteTask`.
2. The Pre-Fix Report from `docs/sops/debugging.md` is appended to the Iteration Ledger before any behavior change. If baseline logs are insufficient, the operator first adds the smallest targeted diagnostic telemetry, reruns the same L3 scenario, and only then changes behavior.
3. Deterministic L3 setup proves the known start state contains exactly one active scheduler task: `开会 · 20:00`; the logcat window is cleared immediately before replaying the exact phrase through a faithful scheduler debug control or seeded input. Valid evidence must show the real route, such as `source=scheduler_debug_button`.
4. `./gradlew :core:pipeline:testDebugUnitTest --tests "*SchedulerIntelligenceRouterTest*"` passes with the target phrase routing as replacement/reschedule, not deletion, and pure delete wording such as `取消晚上8点的开会` still unsupported/blocked.
5. `./gradlew :core:pipeline:testDebugUnitTest --tests "*IntentOrchestratorTest*"` passes with one active `开会 · 20:00` task producing a replacement commit for `newTitle="去机场接人"` and clock cue `晚上8点`, while zero-match and ambiguous-match safety still reject.
6. `./gradlew :core:pipeline:testDebugUnitTest --tests "*RealGlobalRescheduleExtractionServiceTest*" --tests "*GlobalRescheduleContractAlignmentTest*"` passes with prompt/linter alignment proving cancel-wording plus replacement-event extraction.
7. `./gradlew :domain:scheduler:test --tests "*SchedulerLinterTest*"` passes with replacement-cancel extraction accepted and pure deletion still rejected.
8. `./gradlew :app-core:testDebugUnitTest --tests "*SimSchedulerViewModelTest*"` passes if the sprint touches scheduler debug replay, seeded input, or scheduler-viewmodel routing.
9. Existing Sprint 01 time-anchor retitle regression coverage still passes in the focused test set; the closeout names the exact passing test or command.
10. `./gradlew :app-core:assembleDebug` passes.
11. After-fix L3 positive evidence shows `source=scheduler_debug_button`, `SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY`, `Task Rescheduled (Room)`, and final UI XML containing `去机场接人` and `20:00`.
12. After-fix L3 negative evidence shows no unsupported-delete user message for the replacement phrase, no blunt `SchedulerTaskCommand.DeleteTask` route, no chat follow-up action panel, and no duplicate final `开会 · 20:00` task.
13. Docs sync is complete only if stale lower-layer docs are found: core-flow remains the behavioral north star, active Cerb scheduler specs reflect replacement-cancel semantics when they encode the affected branch, and the deprecated PRD is touched only to remove direct contradiction rather than to become a new active source of truth.
14. `docs/projects/scheduler-major-update/tracker.md` sprint 02 row is set to `done` only at successful close.

## Stop Exit Criteria

- A device or emulator is unavailable for the baseline or after-fix L3 loop; fill Closeout as `blocked` and do not claim runtime verification.
- The first L3 baseline cannot establish exactly one active `开会 · 20:00` task without out-of-scope app setup changes.
- The only available debug control bypasses scheduler routing or cannot produce route evidence such as `source=scheduler_debug_button`.
- Baseline logs remain insufficient to prove the failing boundary after one targeted diagnostic-telemetry iteration.
- Fixing the target phrase requires enabling general global delete or weakening the pure-delete unsupported/blocked rule.
- An out-of-scope regression appears in scheduler card delete UX, onboarding sandbox, Harmony, or unrelated badge/audio flows.
- Iteration bound hits without green L1/L2 tests and passing L3 evidence.

## Iteration Bound

3 iterations or 120 wall-clock minutes, whichever hits first.

Each iteration:

1. Run or rerun the exact L3 scenario with one hypothesis and save logcat/UI artifacts.
2. If the latest L3 output fails, diagnose only from measured evidence; add targeted telemetry before behavior edits when needed.
3. Apply the smallest prompt/router/linter/test/doc change that addresses the measured failure.
4. Run the focused L1/L2 command set for touched surfaces.
5. Rebuild, reinstall or cold launch as appropriate, and rerun the same L3 scenario until pass, blocked, or stopped.

## Required Evidence Format

Closeout must include:

- `git diff --stat` for the sprint changes.
- Baseline L3 artifact paths under `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/`, including `run-01-baseline-logcat.txt` and `run-01-baseline-ui.xml`.
- The Pre-Fix Report copied into the Iteration Ledger with literal logcat or grep evidence.
- `BUILD SUCCESSFUL` line and test count for every focused Gradle command in Success criteria 4-10 that was applicable.
- After-fix L3 artifact paths, including logcat and UI XML.
- Positive grep output for `source=scheduler_debug_button`, `SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY`, `Task Rescheduled (Room)`, `去机场接人`, and `20:00`.
- Negative grep output, or explicit no-match command results, for unsupported-delete message, `SchedulerTaskCommand.DeleteTask`, chat follow-up action panel text, and duplicate final `开会 · 20:00`.
- One-line confirmation that pure deletion remains unsupported/blocked, naming the passing test.
- One-line confirmation that Sprint 01 time-anchor retitle regression still passes, naming the passing test or command.

## Iteration Ledger

_Operator fills this section once per iteration._

### Iteration 1 - baseline and pre-fix report

- Added a scheduler drawer debug replay button for the exact Sprint 02 phrase and a setup button for `晚上8点我要开会`; the baseline known state was seeded directly to avoid unrelated create-time drift and proved exactly one active task `开会 · 20:00`.
- Baseline artifacts:
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-01b-known-state-ui.xml`
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-01b-baseline-logcat.txt`
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-01b-baseline-logcat-full.txt`
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-01b-baseline-ui.xml`
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-01b-baseline.png`
- Baseline result: failed as expected. The target phrase was rejected as unsupported voice deletion, and UI still showed `开会 · 20:00`.

### Pre-Fix Report

**1. Evidence-Based Root Cause:**

- `run-01b-baseline-logcat-full.txt` shows `transcript_ingress source=scheduler_debug_button length=18 text=晚上8点的开会取消了，得去机场接人。`
- `run-01b-baseline-logcat-full.txt` shows `route_preflight ... mightReschedule=false looksLikeDeletionTranscript=true shortlistSize=0`.
- `run-01b-baseline-logcat-full.txt` shows `route_decision intent=DELETE_UNSUPPORTED shape=UNSUPPORTED owner=REJECT terminal=false reason=delete unsupported`.
- `run-01b-baseline-ui.xml` shows the unsupported-delete message `SIM 当前不支持语音删除，请在面板手动操作` and the unchanged task `开会` at `20:00`.

**2. Spec Alignment Gate:**

- Core flow invariant 8 says reschedule is replacement, not surgical edit.
- Sprint 02 demand narrows this bug: cancel wording plus a replacement event at the same time anchor is replacement/reschedule; pure cancellation remains unsupported.
- Current code checks `looksLikeDeletionTranscript()` before global-reschedule extraction, so `取消` prevents the replacement phrase from reaching the extractor or time-anchor replacement path.
- This is a code/prompt-routing bug, not a core-flow gap.

**3. Proposed Fix:**

- Update `SchedulerIntelligenceRouter` so replacement-cancel phrases are considered reschedule candidates before the pure-deletion rejection branch.
- Update `PromptCompiler.compileGlobalRescheduleExtractionPrompt` with a strict cancel-plus-replacement example and pure-delete negative rule.
- Add/update focused linter, router, extraction contract, orchestrator, and SIM scheduler tests for the target phrase and pure-delete negative case.

**4. Proactive Risk Assessment & Readiness:**

- Readiness score: 95%. Verified assumptions: exact phrase reaches scheduler debug ingress, deletion preflight is the failing branch, known task exists at `20:00`, and pure delete must remain blocked. Remaining assumption: LLM prompt output must be shaped consistently, covered by contract-alignment tests.
- Potential gaps: ambiguous replacement phrasing with no clear new event, mixed new time plus new title, and old-title leakage into `targetQuery`.
- Blast radius: scheduler routing order, global reschedule extraction, and drawer debug replay only.
- L1/L2 testability: yes; router, linter, prompt contract, orchestrator, and SIM scheduler tests can prove the routing and negative cases.

## Closeout

Status: done

### Iteration 2 - fix and after-fix L3

- Implemented replacement-cancel routing by letting cancel wording with a same-time replacement event bypass the pure-delete rejection and enter global reschedule extraction.
- Synced the global reschedule prompt with one positive cancel-plus-replacement example and one pure-delete negative example.
- Added focused router, orchestrator, extraction-contract, linter, and SIM preflight tests for the target phrase and pure-delete guard.
- After-fix known-state artifact:
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-02-known-state-ui.xml`
- After-fix L3 artifacts:
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-02-after-fix-logcat-full.txt`
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-02-after-fix-logcat.txt`
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-02-after-fix-ui.xml`
  - `docs/projects/scheduler-major-update/evidence/02-cancel-phrase-reschedule-delta/run-02-after-fix.png`

### Verification

- `./gradlew :core:pipeline:testDebugUnitTest --rerun-tasks --tests "com.smartsales.core.pipeline.SchedulerIntelligenceRouterTest" --tests "com.smartsales.core.pipeline.IntentOrchestratorTest" --tests "com.smartsales.core.pipeline.RealGlobalRescheduleExtractionServiceTest" --tests "com.smartsales.core.pipeline.GlobalRescheduleContractAlignmentTest"`: `BUILD SUCCESSFUL`; `SchedulerIntelligenceRouterTest` 3 tests, `IntentOrchestratorTest` 25 tests, `RealGlobalRescheduleExtractionServiceTest` 3 tests, `GlobalRescheduleContractAlignmentTest` 1 test, all 0 failures/errors/skips.
- `./gradlew :domain:scheduler:test --tests "*SchedulerLinterTest*"`: `BUILD SUCCESSFUL`; `SchedulerLinterTest` 33 tests, 0 failures/errors/skips.
- `./gradlew :app-core:testDebugUnitTest --tests "*SimSchedulerViewModelTest*"`: `BUILD SUCCESSFUL`; `SimSchedulerViewModelTest` 57 tests, 0 failures/errors/skips.
- `./gradlew :app-core:assembleDebug`: `BUILD SUCCESSFUL`.
- `git diff --check`: clean.

### L3 Evidence Summary

- Positive log evidence from `run-02-after-fix-logcat-full.txt`:
  - `transcript_ingress source=scheduler_debug_button length=18 text=晚上8点的开会取消了，得去机场接人。`
  - `route_preflight ... mightReschedule=false looksLikeDeletionTranscript=true looksLikeReplacementCancelTranscript=true shortlistSize=1 transcript=晚上8点的开会取消了，得去机场接人。`
  - `SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY`
  - `Task Rescheduled (Room)`
- Positive UI evidence from `run-02-after-fix-ui.xml`: final scheduler UI contains `去机场接人` and `20:00`.
- Negative log evidence from `run-02-after-fix-logcat-full.txt`: no matches for `不支持语音删除`, `SchedulerTaskCommand.DeleteTask`, `DELETE_UNSUPPORTED`, `SimBadgeFollowUpChat`, `SIM badge scheduler follow-up action completed`, or `action=retitle`.
- Negative UI evidence from `run-02-after-fix-ui.xml`: no matches for `SIM 当前不支持语音删除`, `follow-up`, `SimBadgeFollowUpChat`, or `text="开会"`. The only remaining `开会` occurrence is the debug setup button label `S02建: 开会20`, not a final task card.
- Pure deletion remains unsupported/blocked, proven by `SchedulerIntelligenceRouterTest.routeGeneral still rejects pure cancel wording`, `IntentOrchestratorTest.shared scheduler router keeps pure cancel unsupported`, and `SchedulerLinterTest` pure-cancellation coverage.
- Sprint 01 time-anchor retitle regression still passes in `IntentOrchestratorTest.shared scheduler router resolves chinese correction time anchor retitle` and `RealGlobalRescheduleExtractionServiceTest` time-anchor retitle coverage.

### Diff Stat

```text
 .../smartsales/prism/ui/drawers/SchedulerDrawer.kt |  51 +++++++++
 .../prism/ui/sim/SimSchedulerIngressCoordinator.kt |  12 ++-
 .../prism/ui/sim/SimSchedulerViewModelTest.kt      |  17 +++
 .../smartsales/core/pipeline/IntentOrchestrator.kt |   9 +-
 .../com/smartsales/core/pipeline/PromptCompiler.kt |  13 ++-
 .../core/pipeline/SchedulerIntelligenceRouter.kt   |  20 +++-
 .../GlobalRescheduleContractAlignmentTest.kt       |   3 +
 .../core/pipeline/IntentOrchestratorTest.kt        | 117 +++++++++++++++++++++
 .../RealGlobalRescheduleExtractionServiceTest.kt   |  45 ++++++++
 .../pipeline/SchedulerIntelligenceRouterTest.kt    |  79 ++++++++++++++
 docs/core-flow/scheduler-fast-track-flow.md        |   2 +-
 docs/projects/scheduler-major-update/tracker.md    |   4 +-
 .../scheduler/SchedulerLinterParsingSupport.kt     |   2 +-
 .../prism/domain/scheduler/SchedulerLinterTest.kt  |  34 ++++++
 14 files changed, 398 insertions(+), 10 deletions(-)
```

Proposed changelog line for user gate: Scheduler now treats cancel wording plus a same-time replacement event as a reschedule, while pure voice cancellation remains blocked.
