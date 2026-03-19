# Scheduler Path A Uni-A Interface

> **Blackbox contract** — For the `Uni-A` exact-create branch built on top of the shared Path A spine.

---

## Entry Surface

`Uni-A` does not introduce a new public app entrypoint.

It extends the existing Path A spine entry:

```kotlin
suspend fun processInput(
    input: String,
    isVoice: Boolean = false,
    displayedDateIso: String? = null
): Flow<PipelineResult>
```

For T1:

- `isVoice = true` routes voice traffic into the shared Path A spine
- `displayedDateIso` is optional UI context used only for page-relative scheduling language
- after short-circuit exclusions are handled, the spine may perform a bounded `Uni-A` attempt for voice input even if the router labeled the utterance `DEEP_ANALYSIS` or `SIMPLE_QA`
- the `Uni-A` branch is entered only when lightweight semantic extraction returns an exact-create result
- `NotExact` means the spine must continue without claiming scheduler success

---

## Internal Exact Extraction Seam

The implementation must expose an internal seam behaviorally equivalent to:

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

- `ExactCreate` means the small fast model judged the utterance exact enough for normal create
- `NotExact` means `Uni-A` must stop and yield to later-universe routing
- router enum alone must not be treated as the sole runtime gate for attempting this seam
- Kotlin regex or handcrafted date math must not be treated as the semantic truth source for this seam
- the machine-routing schema must come from a real `@Serializable` Kotlin contract, not a handwritten prompt schema
- the linter must decode the exact same contract the prompt advertises
- `明天` / `tomorrow` / `后天` must anchor to `nowIso`, not to `displayedDateIso`
- `下一天` / `后一天` may anchor to `displayedDateIso`; if that field is absent, the seam should return `NotExact`
- bare Chinese `一点` / `1点` defaults to `13:00`; explicit `凌晨一点` may resolve to `01:00`
- closed-set relative-day family is not left to model discretion once the transcript is known; the linter must deterministically normalize or reject illegal anchor dates before persistence
- lawful day-anchor plus explicit clock cue remains an exact-create case even if a fallback vague payload surfaces it first; downstream normalization must yield an exact task DTO, not a vague commit

### Protocol Note

This seam must follow the repo's prompt-linter protocol:

- `Uni-A` uses a narrower extraction model than `UnifiedMutation`
- the new contract must live in `:domain`
- the prompt plus linter must both bind to it mechanically
- the validated extraction result is then mapped into the existing scheduler DTO lane

---

## DTO Handoff

Successful `ExactCreate` must normalize into the existing scheduler DTO lane:

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
- blank title or vague time is invalid for `Uni-A`
- date/time semantics remain model-driven, but closed-set anchor-family law is enforced deterministically at the linter boundary

---

## Early Commit Result

`Uni-A` still completes through the existing checkpoint:

```kotlin
sealed class PipelineResult {
    data class PathACommitted(
        val task: ScheduledTask
    ) : PipelineResult()
}
```

For `Uni-A`, `PathACommitted` additionally implies:

- the committed task is already the real exact task
- `task.id == unifiedId`
- `task.isVague == false`
- `task.clarificationState == null`
- `task.hasConflict == false`

It does not imply support for later universes such as vague create, conflict-visible create, or reschedule.

`ConversationalReply` is not equivalent to `PathACommitted`.
It must not be used as scheduler success proof for the scheduler UI.

---

## Adjacent Module Expectations

### BadgeAudioPipeline

`RealBadgeAudioPipeline` continues to consume the first `PathACommitted` as its foreground completion signal.

For `Uni-A`, the consumed task should already be suitable for normal scheduler render without waiting for Path B clarification.

### Scheduler UI

The scheduler UI should receive a normal timeline item.

It should not need:

- vague-task treatment
- red-flag missing-time treatment
- clarification-card treatment
- conflict caution treatment

Those belong to later universes.
