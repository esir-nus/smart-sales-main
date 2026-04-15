## 📋 Review Conference Report

**Subject**: Architecture Nuke & Pave: Elevating Lightning Router to `PrismOrchestrator` (Dual-Engine Routing Refactor)
**Panel**: `/01-senior-reviewr` (Chair), `/17-lattice-review` (Architecture), `/08-ux-specialist` (Interaction Flows)

---

### Panel Input Summary

#### `/17-lattice-review` — Architecture & Contracts
- **Interface Alignment**: The current `AnalystPipeline.handleInput()` contract still expects to return `AnalystResponse.Chat` for Phase 1 clarification. This is a severe contract violation if Lightning Router (Phase 0) is now handling NOISE and SIMPLE_QA at the Orchestrator level.
- **Dependency Inversion**: `RealAnalystPipeline` currently instantiates or injects `ConsultantService`. This must be inverted. The Orchestrator must own the Router, and the Analyst Pipeline should only receive pre-vetted `DEEP_ANALYSIS` or `CRM_TASK` intents.
- **State Machine Rot**: `AnalystState.CONSULTING` is now dead state.

#### `/08-ux-specialist` — Interaction & Dual-Engine UX
- **Mascot Ephemerality**: If `PrismOrchestrator` routes `NOISE/GREETINGS` to System I (Mascot), what does it return to the main chat UI? It *must* return `UiState.Idle` so no chat bubble is drawn in the permanent history. The Mascot draws its own overlay.
- **Fast-Track UI**: When Lightning Router handles `SIMPLE_QA`, it still needs to appear in the chat history (as System II output, even though it bypassed the heavy planner). We must ensure `RouterResult.response` maps to a standard `UiState.Response` so the user sees the answer.

---

### 🔴 Hard No (Consensus)
- We absolutely cannot leave `ConsultantService` inside `AnalystPipeline`. It violates the Dual-Engine spec where the Mascot needs to intercept NOISE *before* Analyst mode is even considered.

### 🟡 Yellow Flags
- **`UiState` Mapping**: `RealAnalystPipeline` returns `AnalystResponse`, but `PrismOrchestrator` must yield `UiState` to the ViewModel. The mapping layer is currently messy. We need to cleanly map `AnalystResponse.Plan` to `UiState.MarkdownStrategyState`.

### 🟢 Good Calls
- The doc Nuke & Pave was successful. `lightning-router` is now correctly documented as Phase 0.

---

### 💡 Senior's Synthesis & Implementation Plan

The architecture drift is clear. The code trails the documentation. We must execute the "Nuke & Pave" in code.

**The Action Plan (Feature Dev Planner aligned):**

1. **Extract**: Rename `RealConsultantService` to `RealLightningRouter` and move its DI binding from Analyst to Prism layer.
2. **Purge Analyst**: 
   - Remove `AnalystState.CONSULTING`.
   - Rip out the 3-loop `info_sufficient` check native to `RealAnalystPipeline`.
   - Update `AnalystPipeline` interface to no longer return `AnalystResponse.Chat` for disambiguation (if Phase 0 handles it), OR clarify that Phase 2 handles entity disambiguation.
3. **Elevate to Orchestrator**: 
   - `PrismOrchestrator` injects `LightningRouter`.
   - Phase 0 routing: `evaluateIntent(MINIMAL_CONTEXT)`.
   - Path A (System I): `NOISE`/`GREETINGS` -> Call `MascotService.interact()`, return `UiState.Idle` (do not record in chat history).
   - Path B (Fast Track): `SIMPLE_QA` -> return `UiState.Response(result.response)`.
   - Path C (System II): `TASK`/`ANALYSIS` -> Call `AnalystPipeline.handleInput()`.

---

### 🔧 Prescribed Tools

Based on this review, we are ready to transition from PLANNING to EXECUTION.

1. `/feature-dev-planner` — We have completed Phase 0, 1, 2, and 3 (Plan). The plan is solid and compliant with the OS Model.
2. Proceed to execute the `RealAnalystPipeline` purge and `PrismOrchestrator` wiring.
