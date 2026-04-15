# Wave 20 T3: Reinforcement Write-Through Audit

Date: 2026-03-17
Wave: 20
Task: T3
Constitution: [`docs/specs/Architecture.md`](../specs/Architecture.md)
Flow Target: [`docs/core-flow/system-reinforcement-write-through-flow.md`](../core-flow/system-reinforcement-write-through-flow.md)

## Scope

Audit the delivered reinforcement lane against the architecture constitution and the system RL core flow:

- latest-input trigger behavior
- contextual learning material
- typed observation extraction
- asynchronous execution
- SSD write-through
- RAM refresh
- telemetry / GPS coverage

This is an evidence-first audit. It records current code reality and drift. It does not assume the lane is wrong simply because the new core flow is ahead of implementation detail.

## Files Inspected

- [`docs/core-flow/system-reinforcement-write-through-flow.md`](../core-flow/system-reinforcement-write-through-flow.md)
- [`docs/specs/Architecture.md`](../specs/Architecture.md)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/HabitListener.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/HabitListener.kt)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt)
- [`app-core/src/main/java/com/smartsales/prism/data/rl/RealReinforcementLearner.kt`](../../app-core/src/main/java/com/smartsales/prism/data/rl/RealReinforcementLearner.kt)
- [`core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt)
- [`core/context/src/main/java/com/smartsales/core/context/EnhancedContext.kt`](../../core/context/src/main/java/com/smartsales/core/context/EnhancedContext.kt)
- [`core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RealHabitListenerTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RealHabitListenerTest.kt)
- [`core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RlPayloadSchemaTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RlPayloadSchemaTest.kt)
- [`app-core/src/test/java/com/smartsales/prism/data/rl/RealReinforcementLearnerTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/rl/RealReinforcementLearnerTest.kt)
- [`app-core/src/test/java/com/smartsales/prism/data/real/L2WriteBackConcurrencyTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/L2WriteBackConcurrencyTest.kt)

## Constitution Clauses Reused

- background reinforcement listeners as async write-through workers: [`Architecture.md:92`](../specs/Architecture.md#L92)
- Section 2 and Section 3 habit split: [`Architecture.md:156`](../specs/Architecture.md#L156) through [`Architecture.md:157`](../specs/Architecture.md#L157)
- write-through persistence: [`Architecture.md:163`](../specs/Architecture.md#L163)
- async writes outside the critical UX path: [`Architecture.md:287`](../specs/Architecture.md#L287)
- session memory as direct RAM extension: [`Architecture.md:291`](../specs/Architecture.md#L291)

## Findings

### 1. The RL lane is genuinely asynchronous and latest-input triggered

This part aligns well with the constitution.

`RealUnifiedPipeline` invokes the habit listener with the latest raw input and the already assembled `EnhancedContext` in [`RealUnifiedPipeline.kt:208`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L208) through [`RealUnifiedPipeline.kt:211`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L211). The `HabitListener` contract explicitly states this is a background daemon that should not block the main pipeline in [`HabitListener.kt:6`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/HabitListener.kt#L6) through [`HabitListener.kt:21`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/HabitListener.kt#L21). `RealHabitListener` then launches onto the provided app scope in [`RealHabitListener.kt:58`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L58).

That means the trigger model is correct:

- trigger = new latest user input
- lane = background
- ownership = query lane provides the context packet, RL does not own session admission

Assessment:

- constitution alignment: high
- delivered behavior: strong

### 2. The learning packet is contextual, but narrower than the new north-star doc

The listener does not learn from raw text alone. It builds a prompt from:

- active entity context in [`RealHabitListener.kt:63`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L63) through [`RealHabitListener.kt:69`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L69)
- bounded recent session history via `takeLast(4)` in [`RealHabitListener.kt:71`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L71) through [`RealHabitListener.kt:76`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L76)
- the latest raw input in [`RealHabitListener.kt:79`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L79)

So the code already follows the spirit of “latest input + bounded recent history + active context.”

But the packet is still narrower than the core-flow doc now describes. `EnhancedContext` can carry `habitContext`, schedule context, tool artifacts, and more in [`EnhancedContext.kt:11`](../../core/context/src/main/java/com/smartsales/core/context/EnhancedContext.kt#L11) through [`EnhancedContext.kt:35`](../../core/context/src/main/java/com/smartsales/core/context/EnhancedContext.kt#L35), yet `RealHabitListener` only serializes active entities and the last four chat turns. It does not incorporate existing habit context, schedule context, or other active RAM sections into the RL extraction prompt.

That is not a fatal mismatch, but it is real drift against the broader “active RAM context” phrasing in the new core flow.

Assessment:

- constitution alignment: medium-high
- delivered behavior: context-aware but partial
- required follow-up: decide whether “active RAM context” should remain broad in the doc or be narrowed to “active entities + bounded recent turns” for the current RL lane

### 3. Typed extraction is real, but the RL lane uses its own payload contract rather than a shared system mutation contract

`RealHabitListener` uses `kotlinx.serialization` with `coerceInputValues = true` in [`RealHabitListener.kt:92`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L92) through [`RealHabitListener.kt:95`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L95), then parses into `RlPayload` or directly into `List<RlObservation>` in [`RealHabitListener.kt:97`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L97) through [`RealHabitListener.kt:103`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L103). Invalid JSON is safely ignored in [`RealHabitListener.kt:104`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L104) through [`RealHabitListener.kt:107`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L107). `RlPayloadSchemaTest` confirms this extraction contract mechanically in [`RlPayloadSchemaTest.kt:11`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RlPayloadSchemaTest.kt#L11) through [`RlPayloadSchemaTest.kt:56`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RlPayloadSchemaTest.kt#L56).

This is a good typed boundary. But it is a parallel typed boundary. `UnifiedMutation` also defines `rlObservations` in [`UnifiedMutation.kt:48`](../../domain/core/src/main/java/com/smartsales/prism/domain/core/UnifiedMutation.kt#L48) through [`UnifiedMutation.kt:51`](../../domain/core/src/main/java/com/smartsales/prism/domain/core/UnifiedMutation.kt#L51), while the delivered RL lane uses `RlPayload` instead.

Assessment:

- constitution alignment: high on typed extraction
- design coherence: medium
- required follow-up: decide whether RL should keep a dedicated typed payload or converge with the broader system mutation family later

### 4. The write-through path and the Section 2 / Section 3 split are strongly delivered

This is the strongest part of the lane after async triggering.

`RealHabitListener` hands parsed observations to `ReinforcementLearner` in [`RealHabitListener.kt:127`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L127) through [`RealHabitListener.kt:129`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L129), then immediately requests RAM refresh through `contextBuilder.applyHabitUpdates(...)` in [`RealHabitListener.kt:130`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L130).

`RealReinforcementLearner` delegates writes to `UserHabitRepository.observe(...)` in [`RealReinforcementLearner.kt:29`](../../app-core/src/main/java/com/smartsales/prism/data/rl/RealReinforcementLearner.kt#L29) through [`RealReinforcementLearner.kt:38`](../../app-core/src/main/java/com/smartsales/prism/data/rl/RealReinforcementLearner.kt#L38). `RealContextBuilder.applyHabitUpdates(...)` then refreshes:

- Section 2 for global habits in [`RealContextBuilder.kt:261`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L261) through [`RealContextBuilder.kt:267`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L267)
- Section 3 for entity-contextual habits in [`RealContextBuilder.kt:268`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L268) through [`RealContextBuilder.kt:274`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L274)

`RealReinforcementLearnerTest` proves the global-vs-client split in [`RealReinforcementLearnerTest.kt:101`](../../app-core/src/test/java/com/smartsales/prism/data/rl/RealReinforcementLearnerTest.kt#L101) through [`RealReinforcementLearnerTest.kt:174`](../../app-core/src/test/java/com/smartsales/prism/data/rl/RealReinforcementLearnerTest.kt#L174).

Assessment:

- constitution alignment: high
- delivered behavior: strong

### 5. No-op behavior is correctly quiet

The RL lane does not force writes when there is no learnable signal.

`RealHabitListener` returns early on:

- LLM failure in [`RealHabitListener.kt:83`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L83) through [`RealHabitListener.kt:88`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L88)
- parse failure in [`RealHabitListener.kt:104`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L104) through [`RealHabitListener.kt:107`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L107)
- empty observation sets in [`RealHabitListener.kt:109`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L109) through [`RealHabitListener.kt:112`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L112)

`RealHabitListenerTest` covers empty and invalid JSON no-op behavior in [`RealHabitListenerTest.kt:82`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RealHabitListenerTest.kt#L82) through [`RealHabitListenerTest.kt:116`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RealHabitListenerTest.kt#L116).

Assessment:

- constitution alignment: high
- delivered behavior: strong

### 6. Telemetry is the main weak point: the RL lane is barely visible in the valve protocol

The RL core-flow doc currently names `LIVING_RAM_ASSEMBLED`, `LLM_BRAIN_EMISSION`, `LINTER_DECODED`, and `DB_WRITE_EXECUTED` as canonical valves. But the delivered RL lane does not tag any RL-specific valve checkpoints in `RealHabitListener` or `RealReinforcementLearner`.

What exists:

- `LIVING_RAM_ASSEMBLED` is emitted by the query lane before the listener is called, not by RL itself
- `DB_WRITE_EXECUTED` may occur indirectly if the habit repository implementation emits it, but the listener itself provides no RL-specific valve trace

What does not exist in the delivered RL lane:

- no RL-specific `LLM_BRAIN_EMISSION`
- no RL-specific `LINTER_DECODED`
- no explicit valve for “background listener triggered”
- no explicit valve for “habit write-through refresh applied”

So the lane exists, but its GPS visibility is weak relative to the constitution’s observability standard.

Assessment:

- constitution alignment: low-to-medium
- required follow-up: add RL-specific valve checkpoints if the lane is meant to be auditable at architecture level

### 7. Test coverage is decent for the parts, but still thin on end-to-end asynchronous behavior

What is covered:

- listener parses valid observations and calls the learner in [`RealHabitListenerTest.kt:44`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RealHabitListenerTest.kt#L44) through [`RealHabitListenerTest.kt:80`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RealHabitListenerTest.kt#L80)
- listener quietly no-ops on empty/invalid outputs
- learner writes observations into repository and preserves global/client split
- `L2WriteBackConcurrencyTest` exercises a concurrent-style writeback scenario and validates that habit SSD state and final context coexist correctly in [`L2WriteBackConcurrencyTest.kt:172`](../../app-core/src/test/java/com/smartsales/prism/data/real/L2WriteBackConcurrencyTest.kt#L172) through [`L2WriteBackConcurrencyTest.kt:225`](../../app-core/src/test/java/com/smartsales/prism/data/real/L2WriteBackConcurrencyTest.kt#L225)

What is missing:

- no direct valve/GPS tests for the RL lane
- no explicit test proving the RL lane never delays the user-visible reply
- no focused test for fragmented-turn learning material using real session history plus active entity context

Assessment:

- constitutional confidence from tests: medium
- required follow-up: add one or two lane-level tests for non-blocking behavior and fragmented-turn context

## Positive Alignments

1. The RL lane is genuinely background and latest-input triggered.
2. The RL lane is typed and safely no-ops on invalid or empty extraction.
3. The Section 2 / Section 3 split is strongly preserved in both learner and RAM refresh.
4. RAM write-through is real, not just aspirational.

## Evaluation

- asynchronous background fidelity: high
- typed observation extraction: high
- write-through and RAM convergence: high
- Section 2 / Section 3 ownership fidelity: high
- contextual learning packet fidelity: medium-high
- telemetry fidelity: low-to-medium
- test confidence: medium

Overall evaluation:

The reinforcement lane is materially real and closer to the constitution than the query lane. Its core behavior is sound:

- latest input triggers it
- it runs in the background
- it writes typed observations
- it refreshes RAM correctly
- it preserves global vs contextual habits

The main architectural weakness is observability, not existence. The second weakness is that the learning packet is only partially using active RAM: the code currently serializes entities plus recent turns, not the broader active context the north-star doc now implies.

## Derived Fix Tasks

1. Add RL-specific valve checkpoints for:
   - listener triggered
   - RL extraction emitted
   - RL payload decoded
   - habit write-through refresh applied
2. Add one end-to-end test proving the main reply is not blocked by RL activity.
3. Add one focused test for fragmented-turn learning using real recent history plus active entity context.
4. Decide whether the RL learning packet should remain “active entities + last four turns” or expand to a broader active-RAM contract.
5. Decide whether `RlPayload` should remain a dedicated RL contract or eventually converge with the broader system mutation family.

## Conclusion

The RL lane is delivered in substance. It is asynchronous, typed, quiet on no-op, and correctly write-through to Sections 2 and 3. The architecture is already alive here.

The main drift is that the lane is still under-instrumented and slightly under-specified in how much of active RAM it truly consumes. That makes `T3` more of an observability-and-contract-precision audit than a structural failure audit.
