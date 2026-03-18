# Wave 20 T1: Query Lane Audit

> **Purpose**: Audit the current sync/query lane against the architecture constitution and [`docs/core-flow/system-query-assembly-flow.md`](../core-flow/system-query-assembly-flow.md).
> **Parent Tracker**: [`docs/plans/tracker.md`](../plans/tracker.md)
> **Evidence Map**: [`docs/reports/20260317-wave20-t0-architecture-evidence-map.md`](20260317-wave20-t0-architecture-evidence-map.md)
> **Date**: 2026-03-17

---

## Scope

This audit compares the current query-lane code reality against:

- [`docs/specs/Architecture.md`](../specs/Architecture.md)
- [`docs/core-flow/system-query-assembly-flow.md`](../core-flow/system-query-assembly-flow.md)

### Code Seams Inspected

- [`IntentOrchestrator.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)
- [`RealLightningRouter.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealLightningRouter.kt)
- [`RealUnifiedPipeline.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt)
- [`RealEntityDisambiguationService.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealEntityDisambiguationService.kt)
- [`RealInputParserService.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealInputParserService.kt)
- [`RealContextBuilder.kt`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt)
- [`PipelineModels.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/PipelineModels.kt)
- [`PipelineValve.kt`](../../core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt)

### Tests Inspected

- [`IntentOrchestratorTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/IntentOrchestratorTest.kt)
- [`RealIntentOrchestratorTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/RealIntentOrchestratorTest.kt)
- [`RealLightningRouterTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/RealLightningRouterTest.kt)
- [`RealUnifiedPipelineTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealUnifiedPipelineTest.kt)
- [`RealEntityDisambiguationServiceTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/RealEntityDisambiguationServiceTest.kt)

---

## Findings

### 1. Alias-cache ambiguity fast-fails without a resumable parent-lane state

**Severity**: High

The query core flow says ambiguity is a minor loop that should suspend unsafe progress and later resume the same parent lane after repair. Current code only partially does that.

In the phase-0 gateway, the alias-cache ambiguity branch emits `DisambiguationIntercepted` and returns immediately in [`IntentOrchestrator.kt:150`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L150). That branch does **not** call the stateful disambiguation service, does **not** store a resumable query thread, and does **not** route the repair turn through session-memory ownership.

But the only delivered stateful resume mechanism lives in [`RealEntityDisambiguationService.kt:25`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealEntityDisambiguationService.kt#L25) and only works after [`RealEntityDisambiguationService.kt:60`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealEntityDisambiguationService.kt#L60) has been called to create pending intent state.

So today there are effectively two ambiguity paths:

- **orchestrator alias-cache ambiguity**: intercepts but does not create a resumable state
- **pipeline disambiguation service ambiguity**: can resume, but only for its own path

That is a direct drift against the query-lane minor-loop resume law.

### 2. The parser path bypasses the alias-first architecture by loading a full contact sheet before verified IDs

**Severity**: High

The constitution and query core flow say candidate names should prefer fast alias resolution before heavy world fetch. Current parser behavior does something heavier and more duplicative.

[`RealInputParserService.kt:65`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealInputParserService.kt#L65) loads all `PERSON`, `ACCOUNT`, and `CONTACT` entities from the repository, builds a contact sheet in [`RealInputParserService.kt:72`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealInputParserService.kt#L72), and sends that sheet to the extractor in [`RealInputParserService.kt:95`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealInputParserService.kt#L95).

That service is then used inside the query lane in [`RealUnifiedPipeline.kt:98`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L98).

The result is:

- alias grounding exists at the gateway in [`IntentOrchestrator.kt:147`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L147)
- but a second, heavier contact-sheet resolution path still runs deeper in the pipeline

This makes the current query lane architecturally split:

- one path tries alias/L1 fast grounding
- another path rebuilds a broader world slice for semantic resolution

That is exactly the kind of duplicate grounding logic the architecture was supposed to eliminate.

### 3. The delivered query lane has no real minimal/partial RAM branch for ungrounded deep queries

**Severity**: Medium

The query core flow explicitly allows ungrounded deep queries to proceed with minimal or partial RAM instead of forcing false clarification. Current code does not expose a delivered branch for that.

`PipelineInput` defaults `requestedDepth` to `FULL` in [`PipelineModels.kt:10`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/PipelineModels.kt#L10). The orchestrator creates `PipelineInput` without overriding that in [`IntentOrchestrator.kt:180`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L180), and the unified pipeline forwards that depth directly to the kernel in [`RealUnifiedPipeline.kt:173`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L173).

So although the phase-0 build is minimal in [`IntentOrchestrator.kt:91`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L91), the main query lane itself does not currently branch into a delivered “minimal / partial RAM” path for ungrounded deep queries.

That means `Uni-Q5` is still mostly north-star intent, not actual current behavior.

### 4. The test surface proves interception but not orchestrator-level ambiguity repair-and-resume

**Severity**: Medium

The test suite does prove that ambiguity can be intercepted. For example, [`IntentOrchestratorTest.kt:164`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/IntentOrchestratorTest.kt#L164) verifies alias-cache ambiguity short-circuits the unified pipeline, and [`RealEntityDisambiguationServiceTest.kt:48`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/RealEntityDisambiguationServiceTest.kt#L48) proves the stateful disambiguation service can resume after an explicit cure.

But those two proofs do not cover the same branch.

There is no inspected test proving this end-to-end behavior:

1. alias-cache ambiguity intercepted at the orchestrator
2. user repairs the ambiguity
3. the same parent query lane resumes through a preserved thread

So the current test surface validates the two halves separately, but not the actual architectural behavior the core flow requires.

---

## Confirmed Alignments

These parts of the query lane do align well enough with the current north star.

- **Phase-0 gateway is real**
  - [`IntentOrchestrator.kt:90`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L90)
- **Noise/greeting short-circuit really bypasses System II**
  - [`IntentOrchestrator.kt:122`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L122)
- **Core query telemetry is materially present**
  - `INPUT_RECEIVED`, `ROUTER_DECISION` in [`IntentOrchestrator.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)
  - `ALIAS_RESOLUTION`, `LIVING_RAM_ASSEMBLED`, `LLM_BRAIN_EMISSION` in [`RealUnifiedPipeline.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt)
  - `SSD_GRAPH_FETCHED` in [`RealContextBuilder.kt:111`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L111)
- **Kernel still owns final RAM assembly**
  - [`RealUnifiedPipeline.kt:173`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L173)
  - [`RealContextBuilder.kt:73`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L73)

---

## Assessment

The query lane is structurally real and partially compliant.

Its strongest delivered properties are:

- a real phase-0 gateway
- observable major checkpoints
- actual kernel-owned RAM assembly
- clear short-circuit protection for low-value input

Its biggest drift is architectural duplication around grounding and ambiguity handling:

- alias-first is not the sole grounding path
- ambiguity interception exists, but one major branch does not resume
- minimal/partial RAM for ungrounded deep queries is not truly delivered yet

### Evaluation

- **Architecture fidelity**: medium
- **Phase-0 quality**: high
- **Grounding purity**: medium-low
- **Minor-loop fidelity**: medium-low
- **Telemetry coverage**: medium-high
- **Test proof quality**: medium

So the query lane is not broken in the simple sense.
It is better described as **operationally functioning but architecturally split**.

---

## Derived Fix Tasks

These should become lower-layer spec or code tasks after the rest of Wave 20 confirms neighboring lane realities.

1. **Unify ambiguity handling**
   - make the alias-cache ambiguity branch create a resumable minor-loop state instead of one-shot intercept only

2. **Collapse duplicate grounding**
   - decide whether the parser should consume gateway-grounded candidates, or whether the gateway should become the sole alias-first owner

3. **Deliver a real minimal/partial RAM path**
   - introduce an explicit query-depth branch for ungrounded deep queries instead of relying on `FULL` depth everywhere

4. **Add one end-to-end resume test**
   - prove alias ambiguity -> user repair -> same parent query lane resumes
