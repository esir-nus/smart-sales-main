# Wave 19 T0 Reverse Alignment Check

> **Purpose**: Reverse-check delivered code against the new T0 Cerb shard for the shared Path A scheduler spine.
> **Date**: 2026-03-17
> **Primary Contract**: `docs/cerb/scheduler-path-a-spine/spec.md`

## Result

T0 is broadly aligned.

The delivered code matches the new Cerb shard on the main architectural claims:

- one live Path A writer in `IntentOrchestrator`
- badge audio delegates instead of writing scheduler state directly
- `PathACommitted` is the early completion checkpoint
- Path B continues after the early Path A commit
- clarification state can attach onto the already-created Path A task

## Evidence

### Owner Chain

- Cerb shard defines `IntentOrchestrator` as the single live Path A writer:
  - `docs/cerb/scheduler-path-a-spine/spec.md:81`
- Delivered code writes the optimistic task from `IntentOrchestrator`:
  - `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt:191`

### Early Commit Checkpoint

- Cerb shard requires `PipelineResult.PathACommitted(task)` as the T0 checkpoint:
  - `docs/cerb/scheduler-path-a-spine/spec.md:127`
  - `docs/cerb/scheduler-path-a-spine/interface.md:26`
- Delivered code emits it immediately after persistence:
  - `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt:204`
  - `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt:213`

### Badge Audio Delegation

- Cerb shard says `RealBadgeAudioPipeline` is transport/lifecycle only:
  - `docs/cerb/scheduler-path-a-spine/spec.md:96`
- Delivered code delegates transcript scheduling to `IntentOrchestrator` and completes on first `PathACommitted`:
  - `app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt:122`
  - `app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt:126`
  - `app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt:169`

### Clarification Write-Back

- Cerb shard includes clarification-state write-back as in scope:
  - `docs/cerb/scheduler-path-a-spine/spec.md:40`
- Delivered code updates the already-created Path A task when Path B yields clarification/disambiguation:
  - `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt:252`
  - `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt:274`

## Findings

### Medium: stale lower doc still exists and can mislead feature work

`docs/specs/scheduler-path-a-execution-prd.md` still exists even though Wave 19 now points to the new Cerb shard as the implementation contract.

Risk:

- future work may accidentally plan or code from the old PRD instead of the new Cerb shard
- this is especially risky now that the semantic-parsing rule was corrected in core flow

Recommended next step:

- either explicitly mark the PRD as historical / stale
- or fold any still-valid content into Cerb shards and retire it from active use

### Low: `FastTrackParser` name is easy to over-trust

The T0 shard correctly defines `FastTrackParser` as a placeholder optimistic-task constructor, not a semantic parser:

- `docs/cerb/scheduler-path-a-spine/spec.md:110`

But the code name still sounds like a real semantic parser, which creates future risk:

- later developers may try to grow Kotlin heuristics inside it
- the user already flagged this exact concern correctly

Recommended next step:

- when T1 begins, replace or wrap this seam with an explicitly named lightweight semantic extractor contract
- avoid expanding `FastTrackParser` into a fake semantics engine

## Conclusion

The delivered T0 architecture is now documented correctly.

The main remaining risk is not T0 code behavior.
It is lower-layer documentation and naming drift that could push future work back toward heuristic parsing or stale implementation contracts.
