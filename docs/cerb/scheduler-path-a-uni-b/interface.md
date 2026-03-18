# Scheduler Path A Uni-B Interface

> **Blackbox contract** — For the `Uni-B` vague-create branch built on top of the shared Path A spine.

---

## Entry Surface

`Uni-B` does not introduce a new public app entrypoint.

It extends the existing Path A spine entry:

```kotlin
suspend fun processInput(
    input: String,
    isVoice: Boolean = false,
    displayedDateIso: String? = null
): Flow<PipelineResult>
```

For `Uni-B`:

- voice input enters the shared Path A spine
- `Uni-A` exact-create is attempted first
- only after `Uni-A` exits `NotExact` may `Uni-B` be attempted
- `displayedDateIso` may help resolve page-relative day anchors such as `下一天` / `后一天`

---

## Internal Vague Extraction Seam

The implementation must expose an internal seam behaviorally equivalent to:

```kotlin
data class UniBExtractionRequest(
    val transcript: String,
    val nowIso: String,
    val timezone: String,
    val unifiedId: String,
    val displayedDateIso: String? = null
)

sealed class UniBExtractionResult {
    data class VagueCreate(
        val title: String,
        val anchorDateIso: String,
        val timeHint: String?,
        val urgency: String
    ) : UniBExtractionResult()

    data class NotVague(
        val reason: String
    ) : UniBExtractionResult()
}
```

### Contract Rules

- `VagueCreate` means the input is schedulable, date-anchored, and still not exact enough for `Uni-A`
- `NotVague` means `Uni-B` must stop and yield to later-universe routing
- `anchorDateIso` must be a real date anchor, not a guessed exact datetime
- conflict check must not run for `VagueCreate`
- bare vague inputs with no day anchor must not fabricate a date just to satisfy this seam
- the machine-routing schema must come from a real `@Serializable` Kotlin contract
- the linter must decode that same contract directly

---

## DTO Handoff

Successful `VagueCreate` must normalize into a dedicated vague-task lane behaviorally equivalent to:

```kotlin
FastTrackResult.CreateVagueTask(
    params = CreateVagueTaskParams(
        unifiedId = unifiedId,
        title = title,
        anchorDateIso = anchorDateIso,
        timeHint = timeHint,
        urgency = urgency
    )
)
```

Rules:

- exactly one task only
- `unifiedId` must be preserved
- title and anchor date are required
- no exact `startTimeIso` is emitted for this universe

---

## Early Commit Result

`Uni-B` still completes through:

```kotlin
sealed class PipelineResult {
    data class PathACommitted(
        val task: ScheduledTask
    ) : PipelineResult()
}
```

For `Uni-B`, `PathACommitted` implies:

- the committed task is a real persisted scheduler item
- `task.id == unifiedId`
- `task.isVague == true`
- `task.hasConflict == false`
- UI must not treat it as an exact-slot task

`ConversationalReply` is not equivalent to vague-create success.

---

## Telemetry Expectation

`Uni-B` should produce runtime evidence that distinguishes it from `Uni-A`:

- vague extraction marker
- persistence proof
- Path A write proof
- UI render proof

The summaries should identify vague-create explicitly rather than reusing exact-create wording.
