# Scheduler Intelligence & Routing Audit

> **Target**: Scheduler Intelligence Routing (`IntentOrchestrator`, `UnifiedPipeline`, SIM follow-up ingress, extraction services)
> **Observation Date**: 2026-04-03
> **Auditor**: Codex / System
> **Status**: Overhaul implemented in the shared core routing path

## 1. Outcome

The architecture split that triggered this audit is now materially reduced.

Delivered result:

- scheduler intent routing is no longer trapped inside SIM-only ingress paths
- `IntentOrchestrator` voice routing now uses the shared core scheduler router
- `RealUnifiedPipeline` text routing now tries the same shared scheduler router before the legacy JSON scheduler fallback
- later-lane suppression is implemented through `SchedulerTerminalCommit`, so a Path A terminal scheduler commit blocks later scheduler mutations for the same `unifiedId`
- multi-task follow-up reschedule no longer fails at selection-first gating; it now attempts global target routing first and safely rejects unresolved targets inside scheduler-owned copy

This is a real architectural shift, not a surface patch.

## 2. Shipped Core Routing Shape

### Shared scheduler brain

The shared routing brain now lives in `core/pipeline`:

- `SchedulerIntelligenceRouter`
- `SchedulerPathACreateInterpreter`
- shared global / follow-up reschedule extractors

The create cascade now runs in one core location:

1. deterministic relative-time create
2. deterministic chained/day-clock create
3. deterministic direct-failure fast-fail
4. `Uni-M` multi-task create
5. `Uni-A`
6. `Uni-B`

Reschedule handling now also routes through the same core layer instead of SIM-only coordinator logic.

### Voice / Path A

`IntentOrchestrator` now delegates voice scheduler routing into the shared router when the shared dependencies are present.

Delivered gains:

- batch create (`Uni-M`) is now available through the shared voice entry path
- vague create is now a first-class typed scheduler command
- global reschedule now routes through the shared active-task shortlist + final target gate
- later-lane suppression is explicit through `SchedulerTerminalCommit`

### Text / Path B

`RealUnifiedPipeline` now routes `PATH_B_TEXT` scheduler traffic through the shared scheduler router first.

Delivered gains:

- Path B text can now emit typed scheduler proposals for exact create, vague create, mixed batch create, and global reschedule
- the old `UnifiedMutation` JSON scheduler parse is now a compatibility fallback only for shared-router `NotMatchedOrUnavailable`
- Path A and Path B now share the same scheduler extraction semantics before falling back to legacy behavior

### SIM follow-up lane

`SimAgentFollowUpCoordinator` now delegates routing into the shared router instead of owning a separate global-reschedule branch.

Delivered gains:

- follow-up sessions can route through global target resolution before task selection is required
- multi-task follow-up without selection now safely asks for a clearer target instead of prematurely blocking on UI selection
- once one task is resolved, the existing V1 write path still owns execution and the V2 extractor remains shadow-only telemetry

## 3. Findings That Were Closed

### Closed: dual-brain scheduler routing

Previously:

- SIM owned the advanced create/reschedule routing
- core voice and text paths lagged behind

Now:

- the routing brain is shared in `core/pipeline`
- SIM consumes that shared brain instead of being the only place where the richer routing existed

### Closed: Path A / Path B scheduler drift

Previously:

- Path A used the newer extraction contracts
- Path B text still relied directly on legacy JSON mutation parsing

Now:

- Path B text tries the shared scheduler router first
- legacy JSON parsing is demoted to compatibility fallback

### Closed: missing later-lane suppression

The explicit question about later-lane suppression is now answered in code:

- yes, it is implemented
- the guard is `SchedulerTerminalCommit` inside `IntentOrchestrator`
- later scheduler `TaskCommandProposal` and later scheduler reschedule tool dispatch for the same thread are suppressed once Path A already committed

### Closed: multi-task follow-up selection-first failure

Previously:

- a multi-task follow-up reschedule without a selected task failed before it reached global target clarification

Now:

- the follow-up lane attempts global target routing first
- if target extraction stays unsupported or invalid, it returns scheduler-owned clarification copy
- if a global target resolves, the mutation proceeds without requiring prior manual selection

## 4. Verification Evidence

Focused verification passed after the overhaul:

- `./gradlew :core:pipeline:testDebugUnitTest --tests 'com.smartsales.core.pipeline.IntentOrchestratorTest'`
- `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.ui.sim.SimAgentViewModelTest'`
- `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.real.RealUnifiedPipelineTest' --tests 'com.smartsales.prism.ui.onboarding.OnboardingQuickStartServiceTest' --tests 'com.smartsales.prism.ui.sim.SimSchedulerViewModelTest' --tests 'com.smartsales.prism.ui.sim.SimAgentViewModelTest'`

New or important regression coverage now includes:

- Path B mixed batch create through shared scheduler routing
- Path B global reschedule through shared scheduler routing
- scheduler-drawer create phrases no longer misroute into reschedule on bare relative offsets
- multi-task follow-up without selection now safely rejects ambiguous target extraction
- multi-task follow-up without selection can reschedule an explicit globally resolved target

## 5. Remaining Risks

The overhaul is substantial, but it is not the final cleanup pass.

Open follow-up debt:

- `RealUnifiedPipeline` still retains the legacy JSON scheduler fallback for shared-router `NotMatchedOrUnavailable`; that path should eventually be retired after more production evidence
- follow-up V2 remains a shadow-only time-semantics experiment and does not yet replace the V1 execution path
- retrieval-hint-heavy people/location-led target phrasing still needs stronger on-device proof beyond unit coverage

## 6. Final Assessment

The system is no longer in the old “two disconnected brains” state.

The shared scheduler intelligence foundation now exists in the core routing layer, and the two highest-value gaps were closed:

- voice and Path B text now share the same scheduler routing stack
- later-lane double-mutation suppression is implemented instead of remaining a design note

The remaining work is cleanup and proof hardening, not foundational architecture rescue.
