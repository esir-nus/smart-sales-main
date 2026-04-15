# 🌟 North Star: Path A Surgical Migration (ID Conservation)

> **Context**: Migrating the legacy Path A logic (mid-February, synchronous ID conservation) into the System III Dual-Engine Architecture.
> **Scope**: Restoring the "Unified ID Pass-Through" to fix the orphaned optimistic task regression without breaking pipeline decoupling.

## 1. Core Philosophy (Elevating Legacy Mode)
The legacy Scheduler was robust because it executed sequentially, allowing LLM updates to overwrite original tasks deterministically using the same Primary Key. The new architecture decoupled Path A (Optimistic UI) and Path B (LLM Backend) but lost the connection between them.

This migration **elevates the legacy logic** (ID Conservation & Atomicity) into the new System III rules:
- **Path A** dictates the `unifiedId` and persists the 0-latency placeholder.
- **Path B** strictly honors and inherits that `unifiedId` when writing its final DTO via the `CREATE_TASK` or `RESCHEDULE_TASK` plugins.
- **Result**: The timeline visually updates seamlessly without flashing or ghosting. 

## 2. In-Scope vs. Out-of-Scope

### 🟢 IN-SCOPE (The Surgical Fix)
1. **Pipeline Data Pass-Through**: `IntentOrchestrator` generated `unifiedId` must survive the trip through `RealUnifiedPipeline`.
2. **LLM Payload Augmentation**: `RealUnifiedPipeline` must explicitly attach the `unifiedId` into the `PipelineResult.ToolDispatch` parameters.
3. **Plugin Execution**: A new `SchedulerPlugin` (handling `CREATE_TASK` and `RESCHEDULE_TASK`) must catch the dispatch, extract the `unifiedId`, and execute an **Upsert** against `ScheduledTaskRepository`. 

### 🔴 OUT-OF-SCOPE (Deferred for Later)
1. **Unfolded Card Content**: Path B will eventually populate "highlights" and deep notes on the expanded card. That UI rendering is deferred. We only care about the top-level card updating.
2. **CRM Entity Linkage**: Resolving entities (e.g., "Li Zong") to CRM profiles, while technically facilitated by this `unifiedId`, is deferred. The cross-module `EntityWriter` updates are not blocked, but the linking logic is out-of-scope for this specific bug fix.


---


# 🛡️ /06-audit: Evidence-Based Code Audit

## 1. Context & Scope
- **Target**: `IntentOrchestrator.kt`, `RealUnifiedPipeline.kt`, `ToolRegistry` (Plugin system). 
- **Goal**: Surgical migration of legacy ID conservation for Path A.

## 2. Existence Verification
- [x] `IntentOrchestrator.kt`: Found in `:core:pipeline`. Confirmed it generates `unifiedId = java.util.UUID.randomUUID().toString()`.
- [x] `RealUnifiedPipeline.kt`: Found in `:core:pipeline`. Confirmed it extracts JSON but drops `unifiedId`.
- [x] `FastTrackParser.kt`: Found in `:domain:scheduler`. Confirmed it receives `unifiedId` and creates optimistic task.
- [x] `SchedulerPlugin.kt`: ❌ NOT FOUND. The destination plugin to execute `CREATE_TASK` does not currently exist.

## 3. Logic Analysis
- **Implementation**: `IntentOrchestrator` passes `pipelineInput(unifiedId=...)` to `unifiedPipeline.processInput`. Inside `RealUnifiedPipeline.kt` (lines 267-285), when `mutation.classification == "schedulable"`, it emits `ToolDispatch("CREATE_TASK", mapOf("tasks" to tasksJson))`. The `unifiedId` is totally ignored. 
- **Dependencies**: The fix requires changing the Map payload in `RealUnifiedPipeline.kt` to include `"unifiedId" to input.unifiedId`.
- **Gaps/Risks**: The `SchedulerPlugin` must be built. It must map the JSON back to a `TaskMutation` and insert it, defaulting to the passed `unifiedId`.

## 4. Conclusion
- **Ready to Proceed?**: **YES**. The gap is crystal clear and mathematically verifiable.


---


# 📋 /00-review-conference: Plan & Execution

### Triage
- **Subject**: Path A Surgical Migration North Star
- **Type**: Architecture, Core Pipeline flow
- **Complexity**: Medium (Pipeline -> Plugin data flow)

### Panel Selection
1. **`/01-senior-reviewr` (Chair)** — Pipeline resilience, Vibe Coding heuristics.
2. **`/17-lattice-review`** — Layer separation, plugin boundary compliance.

### Conference Execution

#### `/17-lattice-review` — Architecture & Boundaries
- "Injecting the `unifiedId` into the generic `ToolDispatch` `mapOf` parameters in `RealUnifiedPipeline` is perfectly compliant with the Plugin System. It prevents us from bleeding Scheduler-specific data classes into the generic `core:pipeline` layer. The generic stringly-typed Plugin interface boundary is maintained."
- "Creating `SchedulerPlugin` in `:domain:scheduler` correctly isolates the domain logic away from the orchestration layer. This is a massive improvement over the legacy `PrismOrchestrator` God Class."

#### `/01-senior-reviewr` — Pragmatism & Anti-Drift
- "This North Star perfectly adheres to the 'Rewrite Over Extract' and 'Deferred Scope' principles. You identified the exact mechanical breakpoint (the dropped ID) and refused to expand the scope into CRM updates or UI styling."
- "The decision to use an `Upsert` in the Plugin instead of a complex diffing engine is the right call for optimistic UI. Just blast the DB with the new truth using the same ID, and Room/Compose will natively handle the rest."

### 💡 Senior's Synthesis (Final Verdict)
**🟢 APPROVED.** 

This migration marries the deterministic reliability of the legacy code with the isolated, decoupled layout of the new System III architecture. By deferring the CRM linking and the Unfolded Card rendering, we've boxed the risk down to a single parameter pass-through and a basic plugin implementation.

### 🔴 Hard No
- Do NOT try to modify `UnifiedMutation.kt` to carry the `unifiedId`. The LLM has no business knowing about database Primary Keys. The `unifiedId` should be side-loaded locally inside `RealUnifiedPipeline` right before emitting the `ToolDispatch`.

### 🔧 Prescribed Next Steps
1. **Implement Pipeline Fix**: Modify `RealUnifiedPipeline.kt` to inject `input.unifiedId` into the `CREATE_TASK` and `RESCHEDULE_TASK` tool dispatch parameters.
2. **Implement Plugin**: Create `SchedulerPlugin.kt` in `:domain:scheduler` implementing the `Tool` interface from the gateway, executing the `ScheduledTaskRepository.upsertTask()`.
