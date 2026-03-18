# Scheduler Path A Uni-A Exact Create (Wave 19 T1)

> **OS Layer**: System II / Scheduler fast-track exact-create universe
> **Scope**: `Uni-A` exact schedulable creation with no conflict, built on top of the shipped Path A spine
> **Status**: SHIPPED
> **Behavioral Authority Above This Doc**: `docs/core-flow/scheduler-fast-track-flow.md`
> **Foundation Contract Below This Universe**: `docs/cerb/scheduler-path-a-spine/spec.md`

## Overview

This shard defines the first real Path A universe after T0.

`Uni-A` is the exact-create happy path:

- the user speaks one clear schedulable intent
- the time is semantically resolvable as exact
- the system creates one real scheduled task
- the task lands with normal timeline treatment
- no conflict, vague, clarification, or reschedule semantics are involved

This shard exists because the T0 spine was intentionally architectural only.
T1 must now replace placeholder optimism with true semantic exact creation for this one universe.

## T1 Goal

Wave 19 T1 is complete only when an exact voice scheduling utterance can produce one real scheduled task through the shared Path A spine without relying on Kotlin time heuristics.

The required execution law is:

1. lightweight semantic extraction decides whether the utterance is an exact create candidate
2. exact scheduler DTO output is validated and normalized
3. deterministic mutation persists the real task with `id = unifiedId`
4. the early Path A checkpoint returns the real exact task, not a fake placeholder

## Delivered T1 Shape

The shipped T1 path uses:

- a narrow `Uni-A` extraction contract in `:domain`
- prompt generation from that contract's serializer metadata
- strict `SchedulerLinter` deserialization of the same contract
- `IntentOrchestrator` branch routing for `ExactCreate` vs `NotExact`
- deterministic persistence with `id = unifiedId`

This replaces the old `FastTrackParser` heuristic path for `Uni-A` exact-create.

## In Scope

- one exact schedulable voice utterance
- lightweight-model semantic extraction
- exact-task DTO validation and normalization
- deterministic single-task creation
- no-conflict persistence
- normal timeline render through the existing scheduler UI path

## Explicitly Out of Scope

This shard does **not** define:

- vague / missing-time handling (`Uni-B`)
- inspiration handling (`Uni-C`)
- conflict-visible creation (`Uni-D`)
- reschedule branches
- clarification loop behavior
- candidate enrichment from CRM memory
- habit-based suggestions or future personal-agent optimizations

If semantic extraction does not yield an exact create result, the system must exit `Uni-A`.
It must not invent exact time via local string heuristics.

## Human Reality Constraint

Exact scheduling is still semantic understanding work even when the utterance sounds simple.

Examples:

- "book me with Frank tomorrow afternoon at three"
- "set a flight reminder for Friday 6am"
- "next Wednesday 2:30 review the Q2 deck"

These are not safe to solve with regex, substring math, or hand-authored Kotlin date rules as the source of truth.
The small fast model should do the semantic interpretation, while the code should validate and persist the result deterministically.

## Execution Flow

```text
[Voice Transcript]
        |
        v
[IntentOrchestrator shared Path A spine]
        |
        v
[Bounded Uni-A runtime attempt for voice input]
        |
        v
[Lightweight semantic extraction pass]
        |
        +--> [NotExact / NotUniA] ---> exit Uni-A branch without fabricating exactness
        |                               and continue to later-lane handling without claiming schedule success
        |
        v
[Scheduler DTO validation / normalization]
        |
        v
[Deterministic exact create mutation]
        |
        v
[ScheduledTaskRepository upsert with id = unifiedId]
        |
        v
[PipelineResult.PathACommitted(real exact task)]
        |
        v
[Normal timeline render]
```

## Runtime Entry Law

Real runtime entry into `Uni-A` must not depend solely on the upstream router classifying the utterance as `CRM_TASK`.

The repair law is:

- after the existing short-circuit cases (`NOISE`, `GREETING`, and non-voice hardware delegation) are handled,
- bounded `Uni-A` extraction may be attempted for surviving voice input,
- the lightweight semantic extractor decides `ExactCreate` vs `NotExact`,
- and only then does the system either commit Path A or fall through to later-lane handling.

This is required because real exact scheduling utterances can still be labeled `DEEP_ANALYSIS` or `SIMPLE_QA` by the current router in live runtime.

So the runtime contract is:

- router enum is advisory for the shared pipeline
- `Uni-A` entry is governed by bounded schedulable voice semantics
- `NotExact` is a clean exit, not a failure and not a fabricated exact create

## Success Proof Law

User-visible scheduler success must be backed by persistence proof.

For `Uni-A`, acceptable proof is:

- `PipelineResult.PathACommitted`
- and corresponding Path A persistence telemetry such as `PATH_A_DB_WRITTEN`

If `Uni-A` exits `NotExact`, is rejected by conflict, or falls through to later-lane handling without a real scheduler write, the UI must not emit success copy such as `搞定`.

Conversational reply is not scheduler success proof.
Generic completion without a written schedule card is not scheduler success proof.

## Ownership Rules

### IntentOrchestrator

`IntentOrchestrator` remains the single live Path A owner.

For `Uni-A`, it owns:

- bounded runtime entry into the exact-create universe for surviving voice input
- passing bounded semantic context into the lightweight extractor
- branching on `ExactCreate` vs `NotExact`
- preserving the T0 `unifiedId`
- handing validated DTO output into deterministic mutation
- emitting the final `PathACommitted` result
- explicitly falling through to later-lane handling when no Path A commit exists

It must not bypass the shared spine or let badge-audio regain direct task write ownership.

### Lightweight Semantic Extraction Pass

`Uni-A` requires a small fast model pass as the semantic decision-maker.

Its job is narrowly bounded:

- determine whether the utterance is an exact create candidate
- produce one exact task payload when confidence is sufficient
- decline with `NotExact` when time/title semantics are not exact enough for `Uni-A`

Its input context should stay intentionally small:

- raw transcript
- current local time / timezone anchor
- optional displayed scheduler-page date anchor when the user is speaking relative to the opened calendar page
- optional bounded hints already available at Path A entry

It should not depend on full Path B memory assembly just to satisfy `Uni-A`.

### SchedulerLinter

`SchedulerLinter` owns validation and normalization after the semantic pass.

For `Uni-A`, it should validate that:

- exactly one task is present
- `title` is non-blank
- `startTimeIso` is explicit and parseable
- `durationMinutes` is valid for deterministic persistence
- urgency normalizes into the current scheduler enum space

`SchedulerLinter` is not the semantic source of truth for exactness.
It validates model output; it does not replace the model with local time heuristics.

### FastTrackMutationEngine

`FastTrackMutationEngine` owns deterministic exact-task creation after validation.

For `Uni-A`, it must:

- persist one task only
- preserve `id = unifiedId`
- set `isVague = false`
- set `hasConflict = false` when no overlap exists
- avoid placeholder-only task creation

### ScheduledTaskRepository

`ScheduledTaskRepository` remains the only persistence owner.

For `Uni-A`, the repository write is successful only when the stored task is already the real exact task for the utterance.

## Semantic Extraction Contract

The lightweight model pass should behave as if it exposes a contract equivalent to:

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

The actual implementation class name may change.
What matters is the behavioral contract:

- exact result when semantic confidence is sufficient
- `NotExact` when the utterance is vague, mixed, ambiguous, or outside this universe

`Uni-A` must also obey two semantic time laws:

- real-day language such as `明天` / `tomorrow` anchors to the real current date from `nowIso`
- UI-relative language such as `下一天` / `后一天` anchors to `displayedDateIso` when that UI context exists; if it does not exist, the extractor must return `NotExact` rather than silently guessing

And one locale-default law:

- bare Chinese hour expressions such as `一点` / `1点` default to daytime `13:00`
 - explicit early-morning forms such as `凌晨一点` default to `01:00`

And one anti-fabrication law:

- date-only inputs such as `明天提醒我打电话` or `tomorrow remind me to go to the airport` are not exact enough for `Uni-A`
- `Uni-A` must not invent `00:00`, current-clock time, lunch time, or any other guessed exact time for those inputs
- those inputs must exit to `Uni-B` if they still contain a valid date anchor
- explicit early-morning wording such as `凌晨一点` may resolve to `01:00`
- this default belongs in semantic interpretation, not Kotlin heuristic fallback

## Prompt-Linter Protocol Compliance

`Uni-A` must obey the repo's prompt-linter protocol.

That means:

- the lightweight model output must be backed by a real `@Serializable` Kotlin contract in the `:domain` layer
- the prompt must derive its machine-routing schema from that Kotlin contract, not from handwritten Markdown or hardcoded JSON examples
- the linter must deserialize the exact same contract via `kotlinx.serialization`
- semantic routing fields and DTO fields must not be maintained as independent prompt-only strings

For T1, the chosen implementation shape is:

1. introduce a narrower `Uni-A` extraction contract in `:domain`
2. generate the lightweight-model prompt from that contract
3. decode that exact contract on the linter side
4. map the validated extraction result into the existing scheduler DTO lane for deterministic mutation

The prompt text must explicitly carry the anchor/default laws above so the model and linter contract remain aligned on:

- `明天` vs `下一天` / `后一天`
- real current date vs displayed calendar page
- colloquial Chinese daytime default for bare `一点`

This shard does **not** permit:

- markdown schemas as the machine-routing contract
- prompt-only key lists with no backing Kotlin model
- regex or manual JSON traversal inside the linter
- a lightweight-model prompt shape that can drift away from the linter's expected fields

T1 must also introduce the same mechanical guardrail pattern already used by the existing brain-body alignment tests:

- schema generated from serializer metadata
- prompt contains the required machine-routing keys
- linter deserializes that same contract without manual traversal

## DTO / Mutation Contract

`Uni-A` success must be expressible through the existing scheduler DTO path:

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

T1 must not create multiple tasks under the banner of `Uni-A`.
If the model extracts multiple tasks or an incomplete time, that is outside this shard.

## Success Conditions

`Uni-A` is successful only when all of the following are true:

- one exact task is produced semantically
- the persisted task uses the shared `unifiedId`
- `startTime` is concrete and parseable
- `isVague = false`
- `clarificationState = null`
- `hasConflict = false`
- the task appears as a normal scheduler timeline item

## Failure / Exit Conditions

The `Uni-A` branch must exit without claiming success when:

- the model returns `NotExact`
- the extracted time is vague or missing
- the payload fails DTO validation
- more than one task is returned
- a conflict branch is required instead of normal create

When `Uni-A` exits, the implementation must not fabricate exactness locally.
Control should fall back to the non-`Uni-A` Path A routing surface for later-universe handling.

## Intricacies

### Organic UX

The user should feel like the system understood a simple exact scheduling request immediately.
That only works if the committed task is already semantically real, not a temporary title-only placeholder disguised as success.

### Data Reality

`Uni-A` does not require CRM/entity enrichment to succeed.
An impersonal but exact task such as a flight reminder is still valid `Uni-A`.
Candidate absence is not a blocker for this shard.

### Anchor Reality

Scheduler speech can refer to two different date anchors in the same UI:

- real-life anchor: what day it actually is now
- page anchor: what day the scheduler is currently displaying

This shard requires the system to keep them distinct.

- `明天` / `tomorrow` must stay attached to real current date semantics
- `下一天` / `后一天` may attach to the displayed scheduler page date
- if the UI does not supply page-anchor context, page-relative phrasing must exit `Uni-A` as `NotExact`

### Locale Time Reality

Chinese colloquial time defaults are asymmetric.

- bare `一点` should normally mean `13:00`
- `凌晨一点` should mean `01:00`

This is part of semantic understanding, not a post-hoc date parser patch.

### Failure Gravity

The dangerous failure is false exactness:

- code locally guesses a date/time
- the task is persisted as if exact understanding happened
- later universes inherit a fabricated scheduler truth

This shard forbids that failure mode.

## Verification Target

T1 verification should prove:

- semantic exact extraction comes from the lightweight model path
- one real exact task is persisted
- the task uses `id = unifiedId`
- `isVague = false`
- `hasConflict = false`
- the task renders normally on the timeline
- non-exact model output does not masquerade as `Uni-A`
