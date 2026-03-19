# Scheduler Path A Uni-D Interface

> **Blackbox contract** — For the `Uni-D` conflict-visible exact-create branch built on top of the shared Path A spine.

---

## Entry Surface

`Uni-D` does not introduce a new public app entrypoint.

It extends the existing Path A spine entry:

```kotlin
suspend fun processInput(
    input: String,
    isVoice: Boolean = false,
    displayedDateIso: String? = null
): Flow<PipelineResult>
```

For `Uni-D`:

- voice input enters the shared Path A spine
- exact semantic extraction is attempted through the same bounded exact lane used by `Uni-A`
- deterministic conflict evaluation decides whether the exact result is clean (`Uni-A`) or overlapping (`Uni-D`)
- `displayedDateIso` remains page-anchor context only; it does not change the conflict-visible contract itself

---

## Internal Exact Extraction Seam

`Uni-D` reuses the exact semantic extraction seam behaviorally equivalent to:

```kotlin
data class UniAExtractionRequest(
    val transcript: String,
    val nowIso: String,
    val timezone: String,
    val unifiedId: String,
    val displayedDateIso: String? = null
)

sealed class UniAExtractionResult {
    data class ExactCreate(
        val title: String,
        val startTimeIso: String,
        val durationMinutes: Int,
        val urgency: String
    ) : UniAExtractionResult()

    data class NotExact(
        val reason: String
    ) : UniAExtractionResult()
}
```

### Contract Rules

- `Uni-D` does **not** require a separate semantic payload schema from `Uni-A`
- the small fast model decides exactness only
- deterministic conflict evaluation decides clean exact vs conflict-visible exact
- `NotExact` means `Uni-D` must not run
- Kotlin heuristics must not be treated as the semantic truth source

---

## DTO Handoff

Successful `ExactCreate` must normalize into the existing exact-task lane:

```kotlin
FastTrackResult.CreateTasks(
    params = CreateTasksParams(
        unifiedId = unifiedId,
        tasks = listOf(
            TaskDefinition(
                title = title,
                startTimeIso = startTimeIso,
                durationMinutes = durationMinutes,
                urgency = urgency
            )
        )
    )
)
```

Rules:

- exactly one task only
- `unifiedId` must be preserved
- conflict-visible state is not decided here
- overlap handling happens in deterministic mutation

---

## Early Commit Result

`Uni-D` still completes through:

```kotlin
sealed class PipelineResult {
    data class PathACommitted(
        val task: ScheduledTask
    ) : PipelineResult()
}
```

For `Uni-D`, `PathACommitted` implies:

- the committed task is a real persisted scheduler task
- `task.id == unifiedId`
- `task.isVague == false`
- `task.hasConflict == true`
- `task.conflictWithTaskId` carries one persisted overlap identifier
- `task.conflictSummary` carries one user-visible caution string
- UI must not treat it as a clean `Uni-A` card

`ConversationalReply` is not equivalent to conflict-visible success.
Clean success copy without visible caution treatment is not equivalent to `Uni-D` success either.

---

## Deterministic Conflict Boundary

The ownership split is:

- semantic extraction decides exactness
- deterministic board evaluation decides overlap
- persistence writes the final exact conflict state

So the implementation must behave as if it exposes:

```kotlin
sealed class ExactMutationOutcome {
    data class ClearExact(val task: ScheduledTask) : ExactMutationOutcome()
    data class ConflictExact(val task: ScheduledTask) : ExactMutationOutcome()
    data class Rejected(val reason: String) : ExactMutationOutcome()
}
```

Rules:

- overlap must produce `ConflictExact`, not `Rejected`
- `Rejected` is reserved for malformed or non-creatable cases, not valid overlap
- exact tasks with `durationMinutes == 0` still enter this boundary as point-in-time occupancy checks
- this boundary must not fabricate a fake duration merely to trigger overlap math

---

## Adjacent Module Expectations

### Scheduler UI

Scheduler timeline surfaces must render `Uni-D` as a conflict / caution state.

Minimum expectation:

- the card visibly differs from clean `Uni-A`
- the user can tell why attention is required without opening logs or developer tooling
- the foreground scheduler status is caution-state, not clean success

In the shipped T4 slice, this means:

- scheduler foreground status uses `⚠️ 已创建，发现冲突`
- timeline/task-card render consumes `conflictSummary`
- task details surface repeats the conflict summary in caution styling

They must not render it as:

- a normal clean exact card
- a vague / awaiting-time card
- a non-created outcome

### BadgeAudioPipeline

Badge audio may still use the first `PathACommitted` as foreground completion, but the downstream scheduler surfaces must preserve the conflict-visible treatment rather than flattening it into clean success.
