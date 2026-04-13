# Wave 20 T4: Session Memory Extension Audit

Date: 2026-03-17
Wave: 20
Task: T4
Constitution: [`docs/specs/Architecture.md`](../specs/Architecture.md)
Flow Target: [`docs/core-flow/system-session-memory-flow.md`](../core-flow/system-session-memory-flow.md)

## Scope

Audit the delivered session-memory lane against the architecture constitution and the system session-memory core flow:

- Kernel ownership of admission
- bounded recent-turn retention
- carry-forward without full SSD reload
- stale-context replacement / eviction
- clarification / resume support
- telemetry / GPS coverage

This is an evidence-first audit. It records current code reality and drift. It does not assume the new north-star is already fully delivered.

## Files Inspected

- [`docs/core-flow/system-session-memory-flow.md`](../core-flow/system-session-memory-flow.md)
- [`docs/specs/Architecture.md`](../specs/Architecture.md)
- [`core/context/src/main/java/com/smartsales/core/context/ContextBuilder.kt`](../../core/context/src/main/java/com/smartsales/core/context/ContextBuilder.kt)
- [`core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt)
- [`core/context/src/main/java/com/smartsales/core/context/EnhancedContext.kt`](../../core/context/src/main/java/com/smartsales/core/context/EnhancedContext.kt)
- [`domain/session/src/main/java/com/smartsales/prism/domain/session/SessionWorkingSet.kt`](../../domain/session/src/main/java/com/smartsales/prism/domain/session/SessionWorkingSet.kt)
- [`app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt)
- [`docs/cerb/session-context/interface.md`](../cerb/session-context/interface.md)
- [`app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderTest.kt)
- [`app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderMemoryTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderMemoryTest.kt)
- [`app-core/src/test/java/com/smartsales/prism/domain/session/SessionAntiIllusionIntegrationTest.kt`](../../app-core/src/test/java/com/smartsales/prism/domain/session/SessionAntiIllusionIntegrationTest.kt)
- [`core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt`](../../core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt)

## Constitution Clauses Reused

- Kernel owns the Session Working Set lifecycle: [`Architecture.md:147`](../specs/Architecture.md#L147)
- applications work on RAM: [`Architecture.md:161`](../specs/Architecture.md#L161)
- Gateway Before Kernel: [`Architecture.md:162`](../specs/Architecture.md#L162)
- write-through persistence: [`Architecture.md:163`](../specs/Architecture.md#L163)
- clarification repair resumes the parent lane: [`Architecture.md:259`](../specs/Architecture.md#L259) through [`Architecture.md:284`](../specs/Architecture.md#L284)
- recent turns may extend RAM directly: [`Architecture.md:291`](../specs/Architecture.md#L291)

## Findings

### 1. The Kernel-owned session-memory structure is real, but the live app path largely bypasses its admission API

The Kernel side is present and coherent:

- `ContextBuilder` defines `recordUserMessage()` and `recordAssistantMessage()` in [`ContextBuilder.kt:31`](../../core/context/src/main/java/com/smartsales/core/context/ContextBuilder.kt#L31) through [`ContextBuilder.kt:39`](../../core/context/src/main/java/com/smartsales/core/context/ContextBuilder.kt#L39)
- `RealContextBuilder` implements both methods in [`RealContextBuilder.kt:201`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L201) through [`RealContextBuilder.kt:223`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L223)
- the session history is a bounded Kernel-owned RAM structure in [`RealContextBuilder.kt:55`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L55) through [`RealContextBuilder.kt:62`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L62)

But the live chat runtime does not actually use those admission methods. Repo-wide search shows the only concrete calls to `recordUserMessage()` and `recordAssistantMessage()` are in tests. In the real app, `AgentViewModel.send()` appends directly to UI history in [`AgentViewModel.kt:368`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L368) through [`AgentViewModel.kt:372`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L372), and `handlePipelineResult()` appends assistant messages directly to `_history` in multiple branches such as [`AgentViewModel.kt:406`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L406) through [`AgentViewModel.kt:413`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L413).

This matters because the session-memory north-star assumes the Kernel is the admission owner for recent-turn carry-forward. Right now the Kernel has the API, but the main runtime path is not consistently feeding it.

Assessment:

- constitution alignment: medium-low
- delivered behavior: partial
- required follow-up: wire the real chat/send and reply paths through Kernel session-admission APIs instead of only mutating UI history

### 2. Bounded retention is strongly delivered inside the Kernel

This part matches the core flow well.

`RealContextBuilder.pruneHistory()` strictly caps history to 20 messages in [`RealContextBuilder.kt:320`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L320) through [`RealContextBuilder.kt:324`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L324). `RealContextBuilderTest` proves this in [`RealContextBuilderTest.kt:220`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderTest.kt#L220) through [`RealContextBuilderTest.kt:234`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderTest.kt#L234).

This is a good match for:

- bounded carry-forward
- no unbounded transcript dump
- session memory as RAM rather than whole-chat persistence

Assessment:

- constitution alignment: high
- delivered behavior: strong

### 3. Carry-forward without full SSD reload is partially real, but implemented indirectly through history size heuristics rather than a first-class admission decision

The Kernel already reuses RAM and avoids full reload on later turns:

- `shouldSearchMemory(_sessionHistory)` gates heavier SSD/memory reload logic in [`RealContextBuilder.kt:105`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L105) and [`RealContextBuilder.kt:127`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L127)
- the heuristic is simply `sessionHistory.size < 2` in [`RealContextBuilder.kt:330`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L330) through [`RealContextBuilder.kt:331`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L330)
- `RealContextBuilderMemoryTest` shows subsequent turns reusing cached entity knowledge rather than reloading in [`RealContextBuilderMemoryTest.kt:94`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderMemoryTest.kt#L94) through [`RealContextBuilderMemoryTest.kt:122`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderMemoryTest.kt#L122)

So the spirit of bounded carry-forward is present. But the implementation is still fairly primitive:

- the “admission decision” is effectively a history-length heuristic
- it is not modeled as a first-class session-memory rule set
- it is not explicitly tied to clarification/resume threads

Assessment:

- constitution alignment: medium-high
- delivered behavior: present but under-modeled

### 4. Stale-context eviction exists for raw history size, but not as a clear semantic eviction model for abandoned or superseded threads

There are two kinds of cleanup in the new core flow:

- bounded raw retention
- semantic eviction of stale or superseded thread context

The delivered code clearly has the first one through `pruneHistory()`. But it does not yet have a first-class semantic eviction model for abandoned clarification threads or superseded local contexts. `resetSession()` and `loadSession()` fully replace the working set in [`RealContextBuilder.kt:281`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L281) through [`RealContextBuilder.kt:299`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L299), which is good for hard resets, but there is no dedicated lane concept for “this clarification thread was superseded, remove only that recent-turn carry-forward meaning.”

Assessment:

- constitution alignment: medium
- delivered behavior: coarse-grained only
- required follow-up: later define whether semantic eviction belongs in session memory proper or in feature-owned follow-up logic

### 5. Clarification-resume support is only weakly realized through session memory itself

The constitution and system core flow say clarification repair should resume the parent lane with bounded session context rather than restart from zero.

What exists:

- `EnhancedContext` carries `sessionHistory` in [`EnhancedContext.kt:19`](../../core/context/src/main/java/com/smartsales/core/context/EnhancedContext.kt#L19) through [`EnhancedContext.kt:20`](../../core/context/src/main/java/com/smartsales/core/context/EnhancedContext.kt#L20)
- query and RL consumers can read that history
- there are query/disambiguation tests proving some resume behavior elsewhere in the repo

What is missing at the session-memory lane level:

- no focused implementation that admits clarification repair into session memory as a first-class path
- no dedicated session-memory resume logic beyond “history exists if someone recorded it”
- no session-memory-specific clarification-resume tests

Given Finding 1, this is more serious than it looks: because the live runtime is not consistently calling the Kernel history admission API, the resume model is structurally weaker than the docs now claim.

Assessment:

- constitution alignment: low-to-medium
- delivered behavior: partial and indirect

### 6. Telemetry is materially behind the session-memory core flow

This is the clearest observability drift in the lane.

The session-memory core flow names:

- `INPUT_RECEIVED`
- `SESSION_MEMORY_UPDATED`
- `LIVING_RAM_ASSEMBLED`
- `UI_STATE_EMITTED`

But the delivered telemetry only has:

- `INPUT_RECEIVED`
- `LIVING_RAM_ASSEMBLED`
- `UI_STATE_EMITTED`

`SESSION_MEMORY_UPDATED` does not exist in the enum at all. `PipelineValve.Checkpoint` ends without it in [`PipelineValve.kt:11`](../../core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt#L11) through [`PipelineValve.kt:44`](../../core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt#L44), and repo-wide search found no implementation of it.

Assessment:

- constitution alignment: low
- required follow-up: add `SESSION_MEMORY_UPDATED` and decide where admission, refresh, and eviction should tag it

### 7. The anti-illusion session sync fix exists in tests and Kernel code, but the runtime wiring is not fully caught up

`SessionAntiIllusionIntegrationTest` proves that the Kernel can sync RAM and `HistoryRepository` without UI double-write in [`SessionAntiIllusionIntegrationTest.kt:72`](../../app-core/src/test/java/com/smartsales/prism/domain/session/SessionAntiIllusionIntegrationTest.kt#L72) through [`SessionAntiIllusionIntegrationTest.kt:127`](../../app-core/src/test/java/com/smartsales/prism/domain/session/SessionAntiIllusionIntegrationTest.kt#L127). And [session-context/interface.md](/home/cslh-frank/main_app/docs/cerb/session-context/interface.md#L95) still documents the consumer model where the UI layer should call the Kernel history admission methods.

But the live `AgentViewModel` currently does not. So the architecture fix exists as a capability and as a tested kernel path, but not as a cleanly realized runtime contract.

Assessment:

- architecture maturity: good
- runtime realization: behind

## Positive Alignments

1. Kernel ownership of the working set is real.
2. Session memory is bounded in RAM, not treated as unbounded SSD truth.
3. Carry-forward without full reload exists in practice.
4. The Kernel can sync RAM and history SSD correctly when its admission APIs are used.

## Evaluation

- Kernel ownership fidelity: high
- bounded retention fidelity: high
- carry-forward fidelity: medium-high
- clarification-resume fidelity: low-to-medium
- live runtime admission fidelity: low
- telemetry fidelity: low
- test confidence: medium-high for Kernel internals, low-to-medium for live runtime wiring

Overall evaluation:

The session-memory lane exists architecturally, but it is not yet fully realized as a live runtime lane.

The Kernel side is good:

- real Session Working Set
- real bounded history
- real reset/load semantics
- real RAM/SSD sync capability

The main drift is the live app wiring:

- the main UI send/reply path does not consistently admit turns through the Kernel session-memory API
- the promised `SESSION_MEMORY_UPDATED` valve does not exist
- clarification-resume is still mostly an indirect byproduct of other flows rather than a first-class session-memory behavior

## Derived Fix Tasks

1. Wire the real chat/send and reply runtime through `ContextBuilder.recordUserMessage()` and `recordAssistantMessage()` instead of only mutating UI history.
2. Add `SESSION_MEMORY_UPDATED` to `PipelineValve.Checkpoint` and instrument:
   - user-turn admission
   - assistant-turn admission
   - working-set reset/load
   - semantic session-memory replacement if later introduced
3. Add a focused session-memory clarification-resume test at the lane level.
4. Decide whether semantic stale-thread eviction belongs in the Kernel lane or remains feature-owned.

## Conclusion

The session-memory lane is real as a Kernel capability, but not yet fully real as a runtime operating lane. The architecture and tests are ahead of the app wiring here.

So the main T4 conclusion is:

- **Kernel model**: mostly healthy
- **runtime usage**: materially behind
- **telemetry**: clearly behind
