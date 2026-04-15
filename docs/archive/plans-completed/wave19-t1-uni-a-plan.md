# Wave 19 T1 Plan: Uni-A Exact Create

> **Wave**: 19
> **Task**: T1
> **Status**: Implemented
> **Behavioral Authority**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Owning Spec**: `docs/cerb/scheduler-path-a-uni-a/spec.md`
> **Foundation Contract**: `docs/cerb/scheduler-path-a-spine/spec.md`

## Goal

Ship `Uni-A` as the first real Path A universe:

- exact voice schedulable input
- lightweight-model semantic extraction
- no Kotlin heuristic parsing as the semantic source of truth
- one deterministic exact task persisted with `id = unifiedId`
- normal timeline render

## Current Reality

The delivered T0 spine is architecturally correct but semantically incomplete:

- `IntentOrchestrator` still calls `FastTrackParser.parseToOptimisticTask()` for all voice scheduler traffic
- `FastTrackParser` still fabricates optimistic time/title using local heuristics
- `PromptCompiler` only advertises the broad `UnifiedMutation` contract today
- `SchedulerLinter` only decodes `UnifiedMutation`
- `FastTrackMutationEngine` and `RoomScheduledTaskRepository.batchInsertTasks()` still generate fresh UUIDs, so they cannot yet preserve `unifiedId`

## Chosen T1 Shape

T1 will use a **narrower `:domain` extraction contract** for `Uni-A`.

That contract will:

- represent only the exact-create decision surface needed by `Uni-A`
- be the single machine-routing truth for the lightweight model prompt
- be the exact object deserialized by the linter
- then map into the existing scheduler DTO lane for deterministic mutation

This is intentionally narrower than `UnifiedMutation`.
`UnifiedMutation` remains the larger System II currency for the heavy pipeline; `Uni-A` gets its own smaller exact-extraction contract.

## Ownership Map

### Domain

Own the new narrow extraction contract and linter decode seam.

Files:

- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/*`

Responsibilities:

- `@Serializable` exact extraction contract
- KDoc field constraints for prompt generation
- strict decode to the same contract
- mapping from validated extraction to `FastTrackResult.CreateTasks`

### Pipeline

Own the lightweight model prompt and branch routing.

Files:

- `core/pipeline/src/main/java/com/smartsales/core/pipeline/*`

Responsibilities:

- generate prompt/schema from serializer metadata
- call `ModelRegistry.EXTRACTOR`
- branch `ExactCreate` vs `NotExact`
- preserve `unifiedId` when handing into mutation

### Scheduler Mutation / Persistence

Own deterministic exact-task persistence.

Files:

- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngine.kt`
- `app-core/src/main/java/com/smartsales/prism/data/scheduler/RoomScheduledTaskRepository.kt`

Responsibilities:

- persist one exact task only
- preserve `id = unifiedId`
- avoid generating replacement UUIDs on the `Uni-A` exact path

## Implementation Order

1. **Contract**
   - Add the narrow `Uni-A` extraction data class in `:domain`
   - Add field KDoc for exactness rules, nullability, and time format

2. **Prompt / Linter Alignment**
   - Add a prompt builder that derives JSON shape from that contract
   - Add a `SchedulerLinter` decode method for the same contract
   - Keep prompt/linter mechanically aligned through serializer metadata

3. **Orchestrator Rewrite**
   - Replace the `FastTrackParser` exact-create path in `IntentOrchestrator`
   - Call the lightweight extractor for voice scheduler traffic
   - On exact success: validate -> mutate -> emit `PathACommitted(real task)`
   - On not-exact: do not fabricate exactness locally

4. **Mutation / Repository Fix**
   - Ensure the exact-create path preserves `unifiedId`
   - Remove fresh UUID generation from the exact-create path

5. **Tests**
   - prompt/linter alignment for the new narrow contract
   - linter decode tests for exact vs not-exact
   - orchestrator exact-create path test proving `PathACommitted` carries the real exact task
   - non-exact voice scheduler test proving no fake exact commit occurs

## Expected Code Touches

Likely files:

- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/UniAExtractionContract.kt` (new)
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngine.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/PromptCompiler.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/UniAExtractionService.kt` (new or equivalent)
- `app-core/src/main/java/com/smartsales/prism/data/scheduler/RoomScheduledTaskRepository.kt`
- targeted test files in `domain/scheduler`, `core/pipeline`, and `app-core`

## Risks

### Prompt-Linter Drift

If the prompt shape is handwritten while the linter expects a different Kotlin model, T1 will regress into schema drift immediately.

### False Exactness

If not-exact output falls back to local time guessing, T1 fails the core-flow contract.

### GUID Drift

If mutation or repository layers still mint their own IDs, `PathACommitted` will not represent the stable Path A thread identity.

### Partial Rewrite Trap

If `FastTrackParser` remains the live semantic path alongside the new extractor, T1 will keep two competing exactness sources and remain unstable.

## Exit Criteria

T1 is done only when:

- exact voice input is semantically parsed by the lightweight model
- prompt and linter bind to the same narrow contract
- one exact task is persisted with `id = unifiedId`
- `PathACommitted` returns the real exact task
- non-exact input does not masquerade as `Uni-A`
- targeted PU tests pass
