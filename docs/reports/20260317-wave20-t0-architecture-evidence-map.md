# Wave 20 T0: Architecture Constitution Evidence Map

> **Purpose**: Lock the constitutional clauses, active code seams, telemetry anchors, and likely test seams that will be used to audit the five system core flows against current code reality.
> **Parent Tracker**: [`docs/plans/tracker.md`](../plans/tracker.md)
> **Constitution**: [`docs/specs/Architecture.md`](../specs/Architecture.md)
> **System Core Flows**:
> - [`docs/core-flow/system-query-assembly-flow.md`](../core-flow/system-query-assembly-flow.md)
> - [`docs/core-flow/system-typed-mutation-flow.md`](../core-flow/system-typed-mutation-flow.md)
> - [`docs/core-flow/system-reinforcement-write-through-flow.md`](../core-flow/system-reinforcement-write-through-flow.md)
> - [`docs/core-flow/system-session-memory-flow.md`](../core-flow/system-session-memory-flow.md)
> - [`docs/core-flow/system-plugin-gateway-flow.md`](../core-flow/system-plugin-gateway-flow.md)
> **Date**: 2026-03-17

---

## 1. Locked Constitutional Clauses

These are the architecture clauses the lane audits must use as the primary review law.

### Core Hierarchy

- [`docs/specs/Architecture.md`](../specs/Architecture.md): repo constitution and top architectural SOT
- [`docs/core-flow/**`](../core-flow): behavior inside the architecture
- lower specs and code must be judged underneath those two layers

### Architectural Laws To Reuse In Every Audit

- **One Currency Rule**
  - Query lane currency = verified IDs plus RAM context
  - Mutation lane currency = strictly typed Kotlin payloads
- **Active Runtime Layers**
  - Presentation
  - Phase-0 Gateway
  - Kernel
  - System II Pipeline
  - System III Plugins
  - Writers / Repositories
- **Strict Interaction Rules**
  - applications work on RAM
  - gateway before kernel
  - write-through persistence
  - kernel owns the RAM
- **Sync Loop**
  - phase-0 gateway
  - lightning router
  - alias lib
  - disambiguation minor loop
  - SSD graph fetch -> RAM
  - Brain acts
- **Minor Loop Rule**
  - clarification is anti-guessing
  - disambiguation is anti-guessing
  - suspend -> yield -> receive -> resume
- **Async Loops**
  - decoupled entity writing and merging
  - decoupled RL
  - session memory
  - plugin / System III workflows
- **Valve Protocol**
  - mandatory global checkpoints
  - path-specific checkpoints
- **Validation Gates**
  - typed mutation boundary
  - domain vs UI state decoupling
  - central writer rule

### Constitutional Focus By Audit Task

| Audit | Primary Clauses |
|------|------------------|
| `T1 Query Lane` | Sync Loop, Minor Loop Rule, Gateway Before Kernel, Applications Work on RAM |
| `T2 Typed Mutation` | One Currency, Typed Mutation Boundary, Central Writer Rule, Write-Through Persistence |
| `T3 Reinforcement` | Decoupled RL, Session Memory, Applications Work on RAM, Domain vs UI Decoupling |
| `T4 Session Memory` | Session Memory, Gateway Before Kernel, Kernel Owns the RAM |
| `T5 Plugin Gateway` | System III Plugins, Plugin SDK / Capability Gateway, typed boundaries, valve protocol |
| `T6 Cross-Lane Composition` | lane handoffs between Sync, Mutation, RL, Session Memory, and Plugin lanes |

---

## 2. Telemetry / GPS Evidence Anchors

### Living Tracker

- [`docs/plans/telemetry/pipeline-valves.md`](../plans/telemetry/pipeline-valves.md)

### Implementation Anchor

- [`core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt`](../../core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt)

### Checkpoint Inventory Locked For Wave 20

**Mandatory global checkpoints from the constitution**

- `INPUT_RECEIVED`
- `ROUTER_DECISION`
- `ALIAS_RESOLUTION`
- `SSD_GRAPH_FETCHED`
- `LIVING_RAM_ASSEMBLED`
- `LLM_BRAIN_EMISSION`
- `LINTER_DECODED`

**Path-specific / optional checkpoints currently relevant**

- `PATH_A_PARSED`
- `PATH_A_DB_WRITTEN`
- `PLUGIN_DISPATCH_RECEIVED`
- `PLUGIN_INTERNAL_ROUTING`
- `PLUGIN_EXTERNAL_CALL`
- `PLUGIN_YIELDED_TO_OS`
- `DB_WRITE_EXECUTED`
- `UI_STATE_EMITTED`

### Telemetry Reality Note

The telemetry tracker already marks several plugin-phase valves as pending in [`docs/plans/telemetry/pipeline-valves.md`](../plans/telemetry/pipeline-valves.md), even though the enum exists centrally in [`PipelineValve.kt`](../../core/telemetry/src/main/java/com/smartsales/core/telemetry/PipelineValve.kt). Plugin telemetry completeness is therefore an expected audit hotspot, not a surprise.

---

## 3. Active Code Owners

These are the current code seams that appear to own the architecture path today.

| Role | Current Owner |
|------|---------------|
| Phase-0 gateway | [`IntentOrchestrator`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt) |
| Router / gatekeeper | [`RealLightningRouter`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealLightningRouter.kt) |
| Kernel / RAM owner | [`RealContextBuilder`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt) |
| Session RAM data structure | [`SessionWorkingSet`](../../domain/session/src/main/java/com/smartsales/prism/domain/session/SessionWorkingSet.kt) |
| System II reasoning / decode | [`RealUnifiedPipeline`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt) |
| RL async listener | [`RealHabitListener`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt) |
| Central writer | [`RealEntityWriter`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt) |
| Plugin contract surface | [`ToolRegistry.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt) |
| Plugin registry | [`RealToolRegistry`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt) |
| Current first-party plugin examples | [`app-core/src/main/java/com/smartsales/prism/data/real/plugins`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins) |

---

## 4. System Lane Evidence Map

### 4.1 Query Lane

- **North Star**: [`docs/core-flow/system-query-assembly-flow.md`](../core-flow/system-query-assembly-flow.md)
- **Primary code seams**:
  - [`IntentOrchestrator.processInput`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)
  - [`RealLightningRouter.evaluateIntent`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealLightningRouter.kt)
  - [`RealUnifiedPipeline.processInput`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt)
  - [`RealContextBuilder.build`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt)
- **Likely test seams**:
  - [`core/pipeline/src/test/java/com/smartsales/core/pipeline/IntentOrchestratorTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/IntentOrchestratorTest.kt)
  - [`core/pipeline/src/test/java/com/smartsales/core/pipeline/RealIntentOrchestratorTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/RealIntentOrchestratorTest.kt)
  - [`core/pipeline/src/test/java/com/smartsales/core/pipeline/RealLightningRouterTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/RealLightningRouterTest.kt)
  - [`app-core/src/test/java/com/smartsales/prism/data/real/RealUnifiedPipelineTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealUnifiedPipelineTest.kt)
- **Telemetry anchors**:
  - `INPUT_RECEIVED`
  - `ROUTER_DECISION`
  - `ALIAS_RESOLUTION`
  - `SSD_GRAPH_FETCHED`
  - `LIVING_RAM_ASSEMBLED`
  - `LLM_BRAIN_EMISSION`
- **Audit hotspots**:
  - anti-guessing clarification / disambiguation behavior
  - query -> mutation branch
  - query -> session-memory resume branch
  - router JSON parsing vs architecture typed ideals

### 4.2 Typed Mutation Lane

- **North Star**: [`docs/core-flow/system-typed-mutation-flow.md`](../core-flow/system-typed-mutation-flow.md)
- **Primary code seams**:
  - [`RealUnifiedPipeline.processInput`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt)
  - [`RealEntityWriter`](../../data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt)
  - voice auto-commit branches in [`IntentOrchestrator`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)
- **Likely test seams**:
  - [`app-core/src/test/java/com/smartsales/prism/data/real/RealUnifiedPipelineTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealUnifiedPipelineTest.kt)
  - [`data/crm/src/test/java/com/smartsales/data/crm/writer/RealEntityWriterTest.kt`](../../data/crm/src/test/java/com/smartsales/data/crm/writer/RealEntityWriterTest.kt)
- **Telemetry anchors**:
  - `LLM_BRAIN_EMISSION`
  - `LINTER_DECODED`
  - `DB_WRITE_EXECUTED`
  - `UI_STATE_EMITTED`
- **Audit hotspots**:
  - strict typed decode vs fallback parsing
  - central writer ownership
  - proposal-vs-commit behavior
  - plugin-caused writes re-entering the mutation lane

### 4.3 Reinforcement Write-Through Lane

- **North Star**: [`docs/core-flow/system-reinforcement-write-through-flow.md`](../core-flow/system-reinforcement-write-through-flow.md)
- **Primary code seams**:
  - [`RealUnifiedPipeline.processInput`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt) for trigger point
  - [`RealHabitListener.analyzeAsync`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt)
  - [`RealContextBuilder.applyHabitUpdates`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt)
- **Likely test seams**:
  - [`core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RealHabitListenerTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/habit/RealHabitListenerTest.kt)
  - [`app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderTest.kt)
  - [`app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderMemoryTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderMemoryTest.kt)
- **Telemetry anchors**:
  - `LIVING_RAM_ASSEMBLED`
  - `LLM_BRAIN_EMISSION`
  - `LINTER_DECODED`
  - `DB_WRITE_EXECUTED`
- **Audit hotspots**:
  - latest-input trigger correctness
  - bounded history and active RAM usage
  - truly asynchronous no-block behavior
  - no-op quiet path

### 4.4 Session Memory Extension Lane

- **North Star**: [`docs/core-flow/system-session-memory-flow.md`](../core-flow/system-session-memory-flow.md)
- **Primary code seams**:
  - [`RealContextBuilder._sessionHistory` and history lifecycle](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt)
  - [`RealContextBuilder.recordUserMessage`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt)
  - [`RealContextBuilder.recordAssistantMessage`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt)
  - [`SessionWorkingSet`](../../domain/session/src/main/java/com/smartsales/prism/domain/session/SessionWorkingSet.kt)
- **Likely test seams**:
  - [`app-core/src/test/java/com/smartsales/prism/domain/session/SessionWorkingSetTest.kt`](../../app-core/src/test/java/com/smartsales/prism/domain/session/SessionWorkingSetTest.kt)
  - [`app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderMemoryTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderMemoryTest.kt)
  - [`app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/real/RealContextBuilderTest.kt)
- **Telemetry anchors**:
  - no dedicated `SESSION_MEMORY_UPDATED` implementation seam found yet
  - nearest live anchors today appear to be `INPUT_RECEIVED`, `LIVING_RAM_ASSEMBLED`, and `UI_STATE_EMITTED`
- **Audit hotspots**:
  - gateway-before-kernel filtering
  - kernel ownership of session-memory admission
  - bounded carry-forward vs transcript dumping
  - stale-context eviction / replacement

### 4.5 Plugin Gateway / Capability SDK Lane

- **North Star**: [`docs/core-flow/system-plugin-gateway-flow.md`](../core-flow/system-plugin-gateway-flow.md)
- **Primary code seams**:
  - [`ToolRegistry.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt)
  - [`RealToolRegistry.executeTool`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt)
  - plugin auto-commit in [`IntentOrchestrator`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)
  - first-party plugins in [`app-core/src/main/java/com/smartsales/prism/data/real/plugins`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins)
- **Likely test seams**:
  - [`core/pipeline/src/test/java/com/smartsales/core/pipeline/EchoPluginTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/EchoPluginTest.kt)
  - [`app-core/src/test/java/com/smartsales/prism/data/fakes/plugins/FakeConfigurablePluginBreakItTest.kt`](../../app-core/src/test/java/com/smartsales/prism/data/fakes/plugins/FakeConfigurablePluginBreakItTest.kt)
  - [`app-core/src/test/java/com/smartsales/prism/architecture/BrainBodyAlignmentTest.kt`](../../app-core/src/test/java/com/smartsales/prism/architecture/BrainBodyAlignmentTest.kt)
- **Telemetry anchors**:
  - `PLUGIN_DISPATCH_RECEIVED`
  - `PLUGIN_INTERNAL_ROUTING`
  - `PLUGIN_EXTERNAL_CALL`
  - `PLUGIN_YIELDED_TO_OS`
- **Audit hotspots**:
  - real capability SDK vs thin permission gateway
  - plugin-internal routing visibility
  - plugin-caused writes re-entering typed mutation
  - plugin telemetry gaps already marked pending in telemetry tracker

---

## 5. Cross-Lane Composition Checkpoints

These interchanges should be treated as explicit audit targets in `T6`.

| Handoff | Current Evidence Seams | Why It Matters |
|--------|-------------------------|----------------|
| `query -> mutation` | [`RealUnifiedPipeline`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt), [`IntentOrchestrator`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt) | Proves that response turns and SSD-changing turns share one constitutional path instead of ad-hoc writes |
| `query -> session memory` | [`IntentOrchestrator`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt), [`RealContextBuilder`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt) | Needed for clarification and follow-up resume behavior |
| `RL -> session memory` | [`RealHabitListener`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealHabitListener.kt), [`RealContextBuilder`](../../core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt) | Ensures RL consumes session context without owning it |
| `plugin -> mutation` | [`ToolRegistry.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt), [`RealToolRegistry`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt), plugin implementations | Proves plugin work cannot bypass typed mutation / central writer rules |

---

## 6. Immediate Audit Risks Already Visible From T0

These are not the full lane-audit findings. They are simply the obvious pressure points exposed while building the evidence map.

- **Router seam still uses ad-hoc JSON parsing**
  - current owner: [`RealLightningRouter`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealLightningRouter.kt)
- **Session memory lacks a dedicated valve implementation anchor**
  - `SESSION_MEMORY_UPDATED` exists only in the new core-flow doc, not yet in the telemetry enum
- **Plugin capability SDK is still more architectural intent than delivered code**
  - current concrete gateway is permission-oriented in [`ToolRegistry.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt)
- **Voice auto-commit branches are important cross-lane shortcuts**
  - current owner: [`IntentOrchestrator`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)

These should be treated as likely drift hotspots when `T1-T6` begin.

---

## 7. Recommended Audit Sequence

1. Use this evidence map as the shared anchor for Wave 20.
2. Start `T1` at the phase-0 gateway and query lane because it touches the most downstream lanes.
3. Run `T2` next so query-to-mutation drift is judged against a locked mutation law.
4. Audit `T4` before finalizing `T3`, because RL ownership depends on session-memory ownership.
5. Run `T5` after `T2`, because plugin-caused writes must be judged against the mutation lane.
6. Finish with `T6` to check the interchange points once the single-lane evidence is already locked.
