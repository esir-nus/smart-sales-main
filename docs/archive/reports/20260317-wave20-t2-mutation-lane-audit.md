# Wave 20 T2: Typed Mutation Lane Audit

Date: 2026-03-17
Wave: 20
Task: T2
Constitution: [`docs/specs/Architecture.md`](../specs/Architecture.md)
Flow Target: [`docs/core-flow/system-typed-mutation-flow.md`](../core-flow/system-typed-mutation-flow.md)

## Scope

Audit the delivered typed mutation lane against the architecture constitution and the system mutation core flow:

- structured brain emission
- strict typed decode
- proposal vs commit handling
- central writer routing
- RAM write-through
- telemetry / GPS checkpoints

This is an evidence-first audit. It records current code reality and drift. It does not treat lower-layer implementation drift as a design failure by default.

## Files Inspected

- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)
- [`data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt)
- [`domain/core/src/main/java/com/smartsales/prism/domain/core/UnifiedMutation.kt`](../../domain/core/src/main/java/com/smartsales/prism/domain/core/UnifiedMutation.kt)
- [`core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt`](../../core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt)
- [`app-core/src/test/java/com/smartsales/prism/data/real/RealUnifiedPipelineTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealUnifiedPipelineTest.kt)
- [`core/pipeline/src/test/java/com/smartsales/core/pipeline/IntentOrchestratorTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/IntentOrchestratorTest.kt)
- [`core/pipeline/src/test/java/com/smartsales/core/pipeline/RealIntentOrchestratorTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/RealIntentOrchestratorTest.kt)
- [`app-core/src/test/java/com/smartsales/prism/data/real/L2UserFlowTests.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/L2UserFlowTests.kt)
- [`data/crm/src/test/java/com/smartsales/data/crm/writer/RealEntityWriterTest.kt`](../../data/crm/src/test/java/com/smartsales/data/crm/writer/RealEntityWriterTest.kt)
- [`data/crm/src/test/java/com/smartsales/data/crm/writer/GpsValveVerificationTest.kt`](../../data/crm/src/test/java/com/smartsales/data/crm/writer/GpsValveVerificationTest.kt)

## Constitution Clauses Reused

- typed mutation boundary: [`Architecture.md:354`](../specs/Architecture.md#L354)
- JSON coercion resilience: [`Architecture.md:356`](../specs/Architecture.md#L356)
- domain vs UI state decoupling: [`Architecture.md:359`](../specs/Architecture.md#L359)
- central writer rule: [`Architecture.md:360`](../specs/Architecture.md#L360)
- plugin writes must re-enter typed mutation / central writer path: [`Architecture.md:115`](../specs/Architecture.md#L115)

## Findings

### 1. Scheduler and task mutations still leave the typed lane as shapeless `ToolDispatch` payloads instead of terminating in a true typed owning writer path

The strongest typed mutation path is the profile-mutation path. `RealUnifiedPipeline` strictly decodes LLM output into `UnifiedMutation` via `kotlinx.serialization` in [`RealUnifiedPipeline.kt:231`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L231) through [`RealUnifiedPipeline.kt:240`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L240). But the scheduler side does not stay in that typed lane afterward. It switches on free-form `classification` strings and then emits generic `ToolDispatch` packets with `Map<String, String>` params in [`RealUnifiedPipeline.kt:267`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L267) through [`RealUnifiedPipeline.kt:293`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L293).

That is explicit transitional behavior, even called out in code as “until T3 wires up the real plugin” at [`RealUnifiedPipeline.kt:267`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L267). It means the lane only partially satisfies the constitution. The LLM output itself is typed, but scheduler-owned writes still cross into a shapeless dispatch surface instead of a typed owning writer/repository path.

Assessment:

- constitution alignment: medium
- delivered behavior: transitional
- required follow-up: define a typed task-mutation handoff path rather than `ToolDispatch(Map<String, String>)`

### 2. Proposal/commit exists for profile mutations, but the commit seam is only partially observable and only partially centralized

The proposal seam is real. `RealUnifiedPipeline` emits `PipelineResult.MutationProposal` from typed `profileMutations` in [`RealUnifiedPipeline.kt:256`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L256) through [`RealUnifiedPipeline.kt:261`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L261). `IntentOrchestrator` caches that proposal for non-voice input in [`IntentOrchestrator.kt:219`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L219) through [`IntentOrchestrator.kt:232`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L232).

But the commit seam is weaker than the north-star flow:

- confirmation is keyed to a literal `"确认执行"` string in [`IntentOrchestrator.kt:58`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L58)
- confirmed commits fan out as direct `entityWriter.updateAttribute(...)` calls in [`IntentOrchestrator.kt:77`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L77) through [`IntentOrchestrator.kt:83`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L83)
- voice mode silently auto-commits the same profile mutations in background in [`IntentOrchestrator.kt:221`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L221) through [`IntentOrchestrator.kt:229`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L229)

This still uses the central writer, which is good, but the approval/commit phase is not itself a first-class typed command. It is an orchestrator-held open-loop cache plus imperative writer calls.

Assessment:

- constitution alignment: medium-high
- delivered behavior: workable but operationally ad hoc
- required follow-up: define a typed commit command or explicit proposal execution contract rather than relying on cached `PipelineResult` objects and literal confirmation strings

### 3. The central writer and RAM write-through spine are real and materially aligned

`RealEntityWriter` is a true centralized mutation owner for entity/profile writes. It:

- persists through `EntityRepository` in [`RealEntityWriter.kt:61`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L61) through [`RealEntityWriter.kt:64`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L64), [`RealEntityWriter.kt:127`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L127) through [`RealEntityWriter.kt:130`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L130), and [`RealEntityWriter.kt:242`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L242) through [`RealEntityWriter.kt:245`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L245)
- updates active RAM through `KernelWriteBack` in [`RealEntityWriter.kt:270`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L270) through [`RealEntityWriter.kt:276`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L276)
- removes deleted entities from session RAM in [`RealEntityWriter.kt:261`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L261) through [`RealEntityWriter.kt:263`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L263)

This is one of the stronger architecture matches in the codebase. The mutation lane’s SSD-to-RAM coherence rule is genuinely delivered here.

Assessment:

- constitution alignment: high
- delivered behavior: strong
- required follow-up: preserve this spine when normalizing other mutation subpaths

### 4. Telemetry is strong for brain, decode, and DB-write checkpoints, but weak at proposal-execution and mutation-specific UI visibility

The main lane checkpoints are materially present:

- `LLM_BRAIN_EMISSION` in [`RealUnifiedPipeline.kt:223`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L223) through [`RealUnifiedPipeline.kt:229`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L229)
- `LINTER_DECODED` in [`RealUnifiedPipeline.kt:248`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L248) through [`RealUnifiedPipeline.kt:254`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L254)
- `DB_WRITE_EXECUTED` in multiple writer paths, with dedicated GPS tests in [`GpsValveVerificationTest.kt:57`](../../data/crm/src/test/java/com/smartsales/data/crm/writer/GpsValveVerificationTest.kt#L57) through [`GpsValveVerificationTest.kt:196`](../../data/crm/src/test/java/com/smartsales/data/crm/writer/GpsValveVerificationTest.kt#L196)
- `UI_STATE_EMITTED` exists as a checkpoint enum in [`PipelineValve.kt:42`](../../core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt#L42) through [`PipelineValve.kt:43`](../../core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt#L43) and is emitted by UI/ViewModel layers elsewhere in the app

But the mutation-specific proposal/commit seam is not fully traceable:

- no dedicated valve for proposal cached
- no dedicated valve for confirmed commit requested
- no dedicated valve proving voice auto-commit was accepted and handed into the writer path

Assessment:

- constitution alignment: medium-high
- delivered behavior: observable at coarse checkpoints, but not fully at mutation-approval checkpoints
- required follow-up: add proposal/commit sub-valves if this open-loop path remains

### 5. Test coverage validates typed decode output and writer telemetry, but does not yet prove the full proposal-to-confirm-to-write path

What is covered:

- `RealUnifiedPipelineTest` proves typed decode can emit scheduler `ToolDispatch` from LLM output in [`RealUnifiedPipelineTest.kt:93`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealUnifiedPipelineTest.kt#L93) through [`RealUnifiedPipelineTest.kt:136`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealUnifiedPipelineTest.kt#L136)
- `RealEntityWriterTest` proves writer behavior and write-through mechanics
- `GpsValveVerificationTest` proves every current `DB_WRITE_EXECUTED` path in the writer
- `L2UserFlowTests` proves proposals are cached rather than silently committed in a high-stakes flow in [`L2UserFlowTests.kt:240`](../../app-core/src/test/java/com/smartsales/prism/data/real/L2UserFlowTests.kt#L240) through [`L2UserFlowTests.kt:290`](../../app-core/src/test/java/com/smartsales/prism/data/real/L2UserFlowTests.kt#L290)

What is missing:

- no test for `"确认执行"` actually flushing a cached mutation proposal end-to-end
- no test for voice auto-commit of profile mutations end-to-end
- no test proving plugin-caused writes re-enter the typed mutation / writer lane
- no test proving mutation-specific UI visibility is downstream of committed truth rather than only conversational reply

Assessment:

- constitutional confidence from tests: medium
- required follow-up: add end-to-end proposal execution tests before changing this seam further

## Positive Alignments

1. The typed mutation boundary is real at the LLM decode edge.
   - `UnifiedMutation` is a pure domain data class in [`UnifiedMutation.kt:11`](../../domain/core/src/main/java/com/smartsales/prism/domain/core/UnifiedMutation.kt#L11) through [`UnifiedMutation.kt:52`](../../domain/core/src/main/java/com/smartsales/prism/domain/core/UnifiedMutation.kt#L52).
   - Decode uses `kotlinx.serialization` with `coerceInputValues = true` in [`RealUnifiedPipeline.kt:233`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L233) through [`RealUnifiedPipeline.kt:236`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L236), which matches the architecture’s resilience rule.

2. The entity/profile mutation path really honors the central writer rule.
   - All delivered profile commits route through `EntityWriter` and then `EntityRepository` plus `KernelWriteBack`.

3. The writer telemetry is stronger than expected.
   - The repo already has path-by-path GPS tests for `DB_WRITE_EXECUTED`, which is unusually concrete and useful for the later fix loop.

## Evaluation

- typed boundary fidelity: high
- central writer fidelity for CRM/entity writes: high
- proposal/commit architecture fidelity: medium
- scheduler/task mutation fidelity inside this lane: low-to-medium
- telemetry fidelity: medium-high
- test confidence: medium

Overall evaluation:

The mutation lane is more constitution-aligned than the query lane, but it is still split into a strong typed CRM/profile branch and a transitional scheduler/plugin dispatch branch. The strongest delivered part is the `UnifiedMutation -> EntityWriter -> KernelWriteBack` path. The weakest part is everything that leaves the typed lane as generic `ToolDispatch` or cached open-loop execution logic.

## Derived Fix Tasks

1. Replace generic scheduler `ToolDispatch(Map<String, String>)` with a typed task-mutation execution contract or owning typed writer path.
2. Define a typed proposal-execution contract for confirmation and approved auto-commit instead of relying on cached `PipelineResult` plus literal `"确认执行"` handling.
3. Add proposal/commit telemetry checkpoints if the open-loop confirmation model stays.
4. Add end-to-end tests for:
   - cached proposal -> confirm -> writer commit
   - voice auto-commit -> writer commit
   - plugin-caused write -> typed mutation / central writer re-entry

## Conclusion

The current mutation lane is not broken. It already has a real typed decode boundary and a real central writer/write-through spine. But it is not yet a fully unified typed mutation architecture across all mutation classes. The repo still carries a transitional split:

- CRM/entity mutation: mostly architecture-compliant
- scheduler/plugin mutation: still routed through generic dispatch seams

That split should be treated as the main mutation-lane drift for the Wave 20 fix loop.
