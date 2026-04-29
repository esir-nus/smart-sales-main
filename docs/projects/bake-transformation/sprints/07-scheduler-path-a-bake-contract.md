# Sprint Contract: 07-scheduler-path-a-bake-contract

## 1. Header

- **Slug**: scheduler-path-a-bake-contract
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Claude-authored contract lineage; Codex authored from Sprint 06 plan
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Author Sprint 07 as the follow-on Scheduler Path A BAKE contract
sprint. It should require writing `docs/bake-contracts/scheduler-path-a.md`,
syncing interface-map authority, demoting relevant Cerb scheduler docs to
supporting reference, and using centralized Debug HUD / Debug Lab scenarios for
future L3 evidence instead of scattered feature-local debug buttons.

**Codex interpretation**: This sprint writes the Scheduler Path A BAKE
implementation contract from Sprint 05 delivered behavior and Sprint 06 target
core-flow alignment. It updates authority docs so the BAKE contract becomes the
implementation record under the scheduler core-flow docs, while Cerb scheduler
Path A docs become supporting/reference discovery docs.

## 3. Scope

Docs-only. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md`
- `docs/bake-contracts/scheduler-path-a.md`
- `docs/cerb/interface-map.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-spine/interface.md`
- `docs/cerb/scheduler-path-a-uni-a/spec.md`
- `docs/cerb/scheduler-path-a-uni-a/interface.md`
- `docs/cerb/scheduler-path-a-uni-b/spec.md`
- `docs/cerb/scheduler-path-a-uni-b/interface.md`
- `docs/cerb/scheduler-path-a-uni-c/spec.md`
- `docs/cerb/scheduler-path-a-uni-c/interface.md`
- `docs/cerb/scheduler-path-a-uni-d/spec.md`
- `docs/cerb/scheduler-path-a-uni-d/interface.md`

No files outside this list may be written.

Out of scope:

- code edits
- physical Cerb archival moves
- modifying `.agent/**`
- adding feature-local debug buttons to scheduler, connectivity, audio, shell,
  telemetry, or other product UI
- runtime success claims without fresh device-loop evidence

## 4. References

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/specs/device-loop-protocol.md`
- `docs/projects/bake-transformation/sprints/05-scheduler-path-a-dbm.md`
- `docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`
- `docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md`
- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/interface-map.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-spine/interface.md`
- `docs/cerb/scheduler-path-a-uni-a/spec.md`
- `docs/cerb/scheduler-path-a-uni-b/spec.md`
- `docs/cerb/scheduler-path-a-uni-c/spec.md`
- `docs/cerb/scheduler-path-a-uni-d/spec.md`
- `app-core/src/main/java/com/smartsales/prism/ui/debug/L2DebugHud.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerIntelligenceRouter.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerPathACreateInterpreter.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngine.kt`
- `app-core/src/main/java/com/smartsales/prism/data/scheduler/RoomScheduledTaskRepository.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerIngressCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerMutationCoordinator.kt`

## 5. Success Exit Criteria

1. BAKE contract exists at `docs/bake-contracts/scheduler-path-a.md` and
   includes protocol frontmatter plus required BAKE sections.
   Verify:
   `test -f docs/bake-contracts/scheduler-path-a.md`
   `rg -n "^protocol: BAKE$|^version: 1.0$|^domain: scheduler-path-a$|^runtime: base-runtime-active$|^last-verified:" docs/bake-contracts/scheduler-path-a.md`
   `rg -n "^## Pipeline Contract$|^## Telemetry Joints$|^## UI Docking Surface$|^## Core-Flow Gap$|^## Test Contract$|^## Cross-Platform Notes$" docs/bake-contracts/scheduler-path-a.md`

2. The BAKE contract records delivered behavior and core-flow gaps from Sprint
   05/06 instead of silently treating current code as complete.
   Verify:
   `rg -n "alarm arming|pre-fork candidate entity|deterministic exact-create|voice delete|undated vague|canonical valve|SIM-local create routing" docs/bake-contracts/scheduler-path-a.md`

3. The BAKE contract declares Scheduler Path A inputs, outputs/guarantees,
   invariants, error paths, telemetry joints, UI docking surface, test contract,
   and cross-platform notes.
   Verify:
   `rg -n "### Inputs|### Outputs / Guarantees|### Invariants|### Error Paths|## Telemetry Joints|## UI Docking Surface|## Test Contract|## Cross-Platform Notes" docs/bake-contracts/scheduler-path-a.md`

4. `docs/cerb/interface-map.md` cites `docs/bake-contracts/scheduler-path-a.md`
   as the implementation record for Scheduler Path A, while preserving
   core-flow docs as behavioral north stars.
   Verify:
   `rg -n "scheduler-path-a\\.md|BAKE|Scheduler Path A" docs/cerb/interface-map.md`

5. The relevant Cerb scheduler Path A docs mark themselves as
   supporting/reference docs beneath the BAKE contract without deleting
   historical content.
   Verify:
   `rg -n "BAKE|supporting|reference" docs/cerb/scheduler-path-a-spine/spec.md docs/cerb/scheduler-path-a-spine/interface.md docs/cerb/scheduler-path-a-uni-a/spec.md docs/cerb/scheduler-path-a-uni-a/interface.md docs/cerb/scheduler-path-a-uni-b/spec.md docs/cerb/scheduler-path-a-uni-b/interface.md docs/cerb/scheduler-path-a-uni-c/spec.md docs/cerb/scheduler-path-a-uni-c/interface.md docs/cerb/scheduler-path-a-uni-d/spec.md docs/cerb/scheduler-path-a-uni-d/interface.md`

6. Runtime/L3 evidence policy in the BAKE contract allows centralized Debug
   HUD / Debug Lab scenarios and explicitly prohibits scattered feature-local
   debug buttons unless an exception is scoped. The policy must group future
   scenario triggers under one central surface for Scheduler Path A,
   Connectivity / Badge Session, Audio Pipeline, Shell Routing, and Pipeline
   Telemetry; debug triggers are valid only as controlled entrypoints into real
   pipeline paths or realistic upstream module I/O simulation, and evidence
   still comes from real execution.
   Verify:
   `rg -n "Debug HUD|Debug Lab|L2DebugHud|feature-local debug buttons|Scheduler Path A|Connectivity / Badge Session|Audio Pipeline|Shell Routing|Pipeline Telemetry|real pipeline paths|real execution" docs/bake-contracts/scheduler-path-a.md`

7. Tracker row for Sprint 07 is updated to `done`, `stopped`, or `blocked` with
   a factual one-line summary.
   Verify:
   `rg -n "\\| 07 \\| scheduler-path-a-bake-contract \\| (done|stopped|blocked)" docs/projects/bake-transformation/tracker.md`

## 6. Stop Exit Criteria

- **Missing target-flow input**: Sprint 06 did not close successfully or either
  scheduler core-flow doc lacks the Sprint 05 delivered/target/gap alignment
  notes.
- **Contract ambiguity blocker**: The operator cannot decide whether a branch is
  delivered behavior, target behavior, or a core-flow gap from Sprint 05/06 docs
  without a product decision.
- **Central debug reachability blocker**: Future L3 evidence requires a runtime
  trigger that cannot be reached from the existing central Debug HUD / Debug Lab
  direction. Stop and surface the required central debug extension instead of
  adding a feature-local debug button.
- **Runtime-evidence blocker**: Any claim about reminders, notifications, UI
  rendering, device integration, or telemetry delivery lacks fresh `adb logcat`
  and device-loop evidence. Record the gap; do not claim success.
- **Scope creep**: Any pull toward code edits, physical Cerb archival moves, or
  scattered feature-local debug controls.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `wc -l docs/bake-contracts/scheduler-path-a.md`
2. `rg -n "^protocol: BAKE$|^## Pipeline Contract$|^## Core-Flow Gap$|^## Test Contract$|^## Cross-Platform Notes$" docs/bake-contracts/scheduler-path-a.md`
3. `rg -n "scheduler-path-a\\.md|BAKE|Scheduler Path A" docs/cerb/interface-map.md docs/cerb/scheduler-path-a-spine/spec.md docs/cerb/scheduler-path-a-spine/interface.md docs/cerb/scheduler-path-a-uni-a/spec.md docs/cerb/scheduler-path-a-uni-b/spec.md docs/cerb/scheduler-path-a-uni-c/spec.md docs/cerb/scheduler-path-a-uni-d/spec.md`
4. `rg -n "Debug HUD|Debug Lab|L2DebugHud|feature-local debug buttons|Scheduler Path A|Connectivity / Badge Session|Audio Pipeline|Shell Routing|Pipeline Telemetry|real pipeline paths|real execution" docs/bake-contracts/scheduler-path-a.md`
5. `git diff --stat -- docs/bake-contracts/scheduler-path-a.md docs/cerb/interface-map.md docs/cerb/scheduler-path-a-spine/spec.md docs/cerb/scheduler-path-a-spine/interface.md docs/cerb/scheduler-path-a-uni-a/spec.md docs/cerb/scheduler-path-a-uni-a/interface.md docs/cerb/scheduler-path-a-uni-b/spec.md docs/cerb/scheduler-path-a-uni-b/interface.md docs/cerb/scheduler-path-a-uni-c/spec.md docs/cerb/scheduler-path-a-uni-c/interface.md docs/cerb/scheduler-path-a-uni-d/spec.md docs/cerb/scheduler-path-a-uni-d/interface.md docs/projects/bake-transformation/`

If runtime/L3 validation is attempted or required by a future scope decision,
closeout must additionally include filtered `adb logcat` output and device-loop
artifacts from centralized Debug HUD / Debug Lab scenarios. Central debug
triggers should group Scheduler Path A, Connectivity / Badge Session, Audio
Pipeline, Shell Routing, and Pipeline Telemetry under one operator surface.
They are valid only when they enter real pipeline paths or realistic upstream
module I/O simulation; evidence still comes from real execution: telemetry
joints, persisted state, filtered logcat, and, where relevant, screenshots or
human-observed UI state.

## 9. Iteration Ledger

Operator fills this section during execution.

## 10. Closeout

Operator fills this section at exit.
