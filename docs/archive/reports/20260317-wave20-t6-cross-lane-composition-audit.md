# Wave 20 T6: Cross-Lane Composition Audit

Date: 2026-03-17
Wave: 20
Task: T6
Constitution: [`docs/specs/Architecture.md`](../specs/Architecture.md)

## Scope

Audit the interchange points between the five system lanes rather than each lane in isolation:

- `query -> mutation`
- `query -> session memory`
- `RL -> session memory`
- `plugin -> mutation`

This is the final composition audit for Wave 20. It uses the lane audits as evidence, but focuses on ownership transfer and payload continuity between lanes.

## Files Inspected

- [`docs/specs/Architecture.md`](../specs/Architecture.md)
- [`docs/core-flow/system-query-assembly-flow.md`](../core-flow/system-query-assembly-flow.md)
- [`docs/core-flow/system-typed-mutation-flow.md`](../core-flow/system-typed-mutation-flow.md)
- [`docs/core-flow/system-reinforcement-write-through-flow.md`](../core-flow/system-reinforcement-write-through-flow.md)
- [`docs/core-flow/system-session-memory-flow.md`](../core-flow/system-session-memory-flow.md)
- [`docs/core-flow/system-plugin-gateway-flow.md`](../core-flow/system-plugin-gateway-flow.md)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt)
- [`core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt)
- [`app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt)

## Constitution Clauses Reused

- minor loops suspend and resume the parent lane: [`Architecture.md:252`](../specs/Architecture.md#L252) through [`Architecture.md:284`](../specs/Architecture.md#L284)
- decoupled RL as async write-through: [`Architecture.md:290`](../specs/Architecture.md#L290)
- session memory as direct RAM extension: [`Architecture.md:291`](../specs/Architecture.md#L291)
- plugin workflows honoring typed boundaries and OS ownership: [`Architecture.md:292`](../specs/Architecture.md#L292)
- plugin writes must hand off into typed mutation / central writer path: [`Architecture.md:115`](../specs/Architecture.md#L115)
- typed mutation boundary: [`Architecture.md:354`](../specs/Architecture.md#L354)

## Findings

### 1. `query -> mutation` exists, but only partially as a clean lane handoff

This is the strongest delivered cross-lane interchange, but it is still mixed.

The real handoff exists inside `RealUnifiedPipeline`:

- a single `UnifiedMutation` decode can emit conversational reply and structured mutation results in [`RealUnifiedPipeline.kt:231`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L231) through [`RealUnifiedPipeline.kt:265`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L265)
- profile mutations become `PipelineResult.MutationProposal` in [`RealUnifiedPipeline.kt:256`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L256) through [`RealUnifiedPipeline.kt:261`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L261)
- `IntentOrchestrator` then routes those proposals toward `EntityWriter` on confirmation or voice auto-commit in [`IntentOrchestrator.kt:247`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L247) through [`IntentOrchestrator.kt:259`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L259)

But the same query lane also exits into non-typed scheduler/task dispatch via `ToolDispatch` in [`RealUnifiedPipeline.kt:267`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L267) through [`RealUnifiedPipeline.kt:293`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L293). So the handoff is split:

- CRM/profile mutation: typed proposal -> writer
- scheduler/plugin mutation: generic dispatch -> plugin/registry path

That means the architecture has a partially clean `query -> mutation` seam, not a unified one.

Assessment:

- composition fidelity: medium
- strongest branch: profile mutation
- weakest branch: task/plugin branch escaping the typed lane

### 2. `query -> session memory` is the weakest major handoff in live runtime

The constitution and session-memory flow expect repair input and fresh turns to pass through Kernel admission before resuming downstream work.

What the code does:

- the query lane reads session history from `ContextBuilder.build(...)` in [`IntentOrchestrator.kt:95`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L95) through [`IntentOrchestrator.kt:123`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L123) and later in `RealUnifiedPipeline`
- `RealContextBuilder` has the right Kernel admission APIs in [`RealContextBuilder.kt:201`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L201) through [`RealContextBuilder.kt:223`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L223)

What the live runtime does not do:

- `AgentViewModel.send()` appends user turns directly to UI history in [`AgentViewModel.kt:368`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L368) through [`AgentViewModel.kt:372`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L372)
- `handlePipelineResult()` appends assistant turns directly to UI history in branches like [`AgentViewModel.kt:406`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L406) through [`AgentViewModel.kt:413`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L413)
- there is no delivered `SESSION_MEMORY_UPDATED` valve or explicit Kernel admission trace

So the query lane consumes session memory when it exists, but the live runtime does not reliably feed that lane through the Kernel. That weakens:

- clarification repair resume
- bounded carry-forward
- future multi-turn feature flows

Assessment:

- composition fidelity: low
- main issue: admission bypass, not Kernel design

### 3. `RL -> session memory` is structurally the healthiest handoff, but it depends on the weak upstream query/session seam

This handoff does exist clearly:

- `RealUnifiedPipeline` passes the latest input and assembled `EnhancedContext` into RL in [`RealUnifiedPipeline.kt:208`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L208) through [`RealUnifiedPipeline.kt:211`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L211)
- `RealHabitListener` extracts observations from that packet and then calls `contextBuilder.applyHabitUpdates(...)` in [`RealHabitListener.kt:127`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L127) through [`RealHabitListener.kt:130`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt#L130)
- `RealContextBuilder` refreshes Section 2 / Section 3 in [`RealContextBuilder.kt:259`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L259) through [`RealContextBuilder.kt:275`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L275)

So the downstream handoff is real and honors ownership:

- RL does not own admission rules
- Kernel owns RAM refresh

But the quality of the RL learning packet still depends on the weaker `query -> session memory` seam. Since live user/assistant turns are not consistently admitted through the Kernel, RL’s view of “recent session context” is only as strong as whatever `EnhancedContext.sessionHistory` happens to contain.

Assessment:

- composition fidelity: medium-high
- direct seam: good
- upstream dependency quality: limited by session-admission drift

### 4. `plugin -> mutation` is effectively missing

This is the clearest absent interchange in the whole architecture.

The constitution says plugin-caused writes must re-enter the typed mutation / central writer lane. The current code does not deliver that:

- plugins return `Flow<UiState>` directly from [`ToolRegistry.kt:74`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L74) through [`ToolRegistry.kt:75`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L75)
- `RealToolRegistry` simply dispatches and returns plugin UI flow in [`RealToolRegistry.kt:18`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt#L18) through [`RealToolRegistry.kt:31`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt#L31)
- current real plugins only emit `UiState`, not typed mutation commands

So there is no delivered handoff like:

- plugin result -> typed mutation command
- plugin write request -> central writer
- plugin update -> post-write RAM refresh through the mutation lane

Assessment:

- composition fidelity: low
- current reality: absent

### 5. The architecture has lanes, but most interchange points are not first-class telemetry seams yet

The composition problem is not just behavioral. It is also observability.

Current telemetry can show:

- query input and routing
- brain emission and typed decode
- DB writes in some lanes
- plugin dispatch received

But the lane transfers themselves are not first-class trace points:

- no explicit `query -> mutation` handoff valve
- no explicit proposal accepted / commit requested valve
- no `SESSION_MEMORY_UPDATED`
- no RL-specific extraction / decode handoff valves
- no `plugin -> mutation` handoff valve because no such handoff exists yet
- no `PLUGIN_CAPABILITY_CALL` or `PLUGIN_EXTERNAL_CALL` in real code

So if payload loss or ownership bypass happens between lanes, the current GPS model often cannot prove where the transfer broke.

Assessment:

- composition observability: low-to-medium

### 6. The main architectural risk is no longer missing lanes; it is bypassed ownership at transfer points

This is the final Wave 20 pattern:

- query lane exists
- mutation lane exists
- RL lane exists
- session-memory lane exists as Kernel structure
- plugin lane exists as registry

The risk is that cross-lane movement often bypasses the next lane’s ownership rule:

- query bypasses session-memory admission in live runtime
- query partially bypasses typed mutation for scheduler/plugin branches
- plugin has no mutation re-entry at all
- telemetry rarely marks the transfer itself

This is a composition problem, not a lane-existence problem.

## Positive Alignments

1. `query -> mutation` is partially real through the `UnifiedMutation` decode path.
2. `RL -> session memory` is a real ownership-preserving handoff.
3. The architecture already has enough real lane structure to fix composition without redesigning from zero.

## Evaluation

- lane existence fidelity: high
- `query -> mutation`: medium
- `query -> session memory`: low
- `RL -> session memory`: medium-high
- `plugin -> mutation`: low
- cross-lane observability: low-to-medium
- overall composition fidelity: medium-low

Overall evaluation:

The five-lane architecture is real, but the interchanges are still immature. The repo’s next architectural work should not be “invent more lanes.” It should be:

- formalize the handoff rules
- wire runtime through the owning lane APIs
- add telemetry at the transfer points

## Derived Fix Tasks

1. Normalize `query -> mutation` so task/plugin branches do not escape typed lane ownership through generic `ToolDispatch` where mutation semantics are intended.
2. Wire live chat runtime through Kernel session admission so `query -> session memory` becomes real, not only latent.
3. Add `SESSION_MEMORY_UPDATED` and related transfer valves for:
   - user-turn admission
   - assistant-turn admission
   - clarification repair admission
4. Keep `RL -> session memory` as the model seam, but improve its upstream packet quality by fixing the query/session admission path.
5. Define and implement a real `plugin -> mutation` re-entry path for plugin-caused writes.
6. Add composition-level tests for:
   - clarification repair using actual session-memory admission
   - proposal confirm -> writer commit
   - plugin result -> typed mutation handoff
7. Add explicit handoff telemetry where payload responsibility changes between lanes.

## Conclusion

Wave 20’s final conclusion is:

- the architecture constitution is viable
- the five system flows are justified
- the main debt is now at the handoff seams

So the next round should be a composition-fix wave, not another conceptual architecture wave.
