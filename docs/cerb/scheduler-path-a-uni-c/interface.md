# Scheduler Path A Uni-C Interface

> **Blackbox contract** — For the `Uni-C` inspiration branch built on top of the shared Path A spine.

---

## Entry Surface

`Uni-C` does not introduce a new public app entrypoint.

It extends the existing Path A spine entry:

```kotlin
suspend fun processInput(
    input: String,
    isVoice: Boolean = false,
    displayedDateIso: String? = null
): Flow<PipelineResult>
```

For `Uni-C`:

- voice input enters the shared Path A spine
- `Uni-A` exact-create is attempted first
- `Uni-B` vague-create is attempted second when appropriate
- only after scheduler universes decline may `Uni-C` be attempted
- `displayedDateIso` is not a semantic requirement for `Uni-C`; timeless intent should not depend on scheduler-page anchoring

---

## Internal Inspiration Extraction Seam

The implementation must expose an internal seam behaviorally equivalent to:

```kotlin
data class UniCExtractionRequest(
    val transcript: String,
    val nowIso: String,
    val timezone: String,
    val unifiedId: String
)

sealed class UniCExtractionResult {
    data class InspirationCreate(
        val content: String,
        val title: String?
    ) : UniCExtractionResult()

    data class NotInspiration(
        val reason: String
    ) : UniCExtractionResult()
}
```

### Contract Rules

- `InspirationCreate` means the utterance is timeless intent and should not enter scheduler mutation
- `NotInspiration` means `Uni-C` must stop and yield to later-lane handling
- the machine-routing schema must come from a real `@Serializable` Kotlin contract
- the linter must decode that same contract directly
- the seam must reject task-shaped outputs that try to sneak schedule fields into inspiration capture
- Kotlin heuristics must not be treated as the semantic truth source for deciding timeless inspiration vs schedulable intent

---

## DTO Handoff

Successful `InspirationCreate` must normalize into an inspiration-only lane behaviorally equivalent to:

```kotlin
FastTrackResult.CreateInspiration(
    params = CreateInspirationParams(
        unifiedId = unifiedId,
        content = content
    )
)
```

Rules:

- exactly one inspiration payload only
- `unifiedId` must be preserved
- the payload must not carry scheduler time fields
- no scheduler task DTO may be emitted for this universe

---

## Early Commit Result

`Uni-C` must not reuse scheduler success semantics.

The delivered foreground success result is:

```kotlin
sealed class PipelineResult {
    data class InspirationCommitted(
        val id: String,
        val content: String
    ) : PipelineResult()
}
```

- the committed result is not a scheduler task
- the scheduler drawer must not treat it as `PathACommitted`
- success copy must not imply calendar creation

`ConversationalReply` is not equivalent to inspiration persistence proof.

---

## Adjacent Module Expectations

### IntentOrchestrator

`IntentOrchestrator` owns the sequencing boundary:

- no `Uni-C` before `Uni-A` / `Uni-B` have declined
- no later-lane scheduler task command may revive scheduler mutation after `Uni-C` accepts

### Scheduler UI

Scheduler timeline surfaces must not render `Uni-C` as:

- exact card
- vague/awaiting-time card
- conflict card

### Inspiration Surface

An inspiration/note surface must be able to render the persisted payload without requiring a fake calendar representation.
