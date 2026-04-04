# Scheduler Path A Spine Interface

> **Blackbox contract** — For callers and adjacent modules that depend on the shared Path A scheduler spine.

---

## Entry Surface

### IntentOrchestrator

```kotlin
suspend fun processInput(
    input: String,
    isVoice: Boolean = false
): Flow<PipelineResult>
```

For T0 scheduler voice traffic:

- callers send raw transcript text
- `isVoice = true` enables the shared Path A scheduler spine
- the flow may emit an early `PathACommitted` before later downstream results finish

---

## Early Commit Result

```kotlin
sealed class PipelineResult {
    data class PathACommitted(
        val task: ScheduledTask
    ) : PipelineResult()
}
```

### Meaning

`PathACommitted` guarantees:

- the scheduler task was already persisted through `ScheduledTaskRepository`
- the persisted task uses the shared scheduler thread identity (`unifiedId`)
- this is the foreground completion checkpoint for badge-audio consumers
- this early commit also arms the later-lane scheduler suppression guard for the same scheduler thread, so downstream Path B scheduler writes do not re-mutate the same `unifiedId`

`PathACommitted` does **not** guarantee:

- exact semantic parse correctness
- conflict-free final business truth for later universes
- completed Path B enrichment

---

## Badge Audio Consumption Rule

`RealBadgeAudioPipeline` consumes the first `PathACommitted` as its foreground completion signal.

It should:

- map the committed `ScheduledTask` into `SchedulerResult.TaskCreated`
- emit `PipelineEvent.Complete`
- continue to tolerate later background Path B work without blocking the drawer close

It should not:

- mint its own scheduler IDs
- write `ScheduledTaskRepository` directly
- claim semantic scheduling ownership

---

## T0 Limits

This interface is only the T0 contract.

It deliberately leaves later universe semantics open:

- `Uni-A` exact semantic extraction
- `Uni-B` vague creation
- conflict universe rules
- reschedule universe rules

Those are governed by the later scheduler Cerb shards and core-flow docs rather than by this minimal interface contract.

`Uni-A` exact-create is now defined in `docs/cerb/scheduler-path-a-uni-a/spec.md`.
