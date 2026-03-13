## 📋 Review Conference Report

**Date**: 2026-03-13
**Subject**: Wave 3 (Scheduler Migration) - The PendingProposalStore & Open-Loop Architecture
**Panel**: `/01-senior-reviewr` (Chair), `/06-audit` (Evidence & Alignment)

---

### Phase 1: Triage
- **Review Subject**: Proposed `PendingProposalStore` architecture for fulfilling the Open-Loop DB writes under the Mono pipeline.
- **Review Type Classification**: Architecture (structure, contracts, layers) & Concept Validation.
- **Complexity Level**: Medium (Requires Senior & Audit validation against the PRD).

---

### Panel Input Summary

#### 🛡️ `/06-audit` (Evidence-Based Code Audit)
- **Scope**: `SmartSales_PRD.md §2`, `docs/cerb/unified-pipeline/spec.md`, `AgentViewModel.kt`
- **Fact 1 (The PRD Rule)**: L9 explicitly demands an *"Open-Loop Lifecycle (Chat → Proposal → Confirm → Result). This guarantees the 'human-in-the-loop' before any destructive actions..."*
- **Fact 2 (Current Code State)**: `AgentViewModel.kt` L430 currently intercepts `PipelineResult.MutationProposal` and merely emits a chat bubble saying "T3 Open-Loop Defense: Render a proposal card instead of mutating". It does *not* persist anything. 
- **Fact 3 (The Unified Pipeline Contract)**: `UnifiedPipeline` is a stateless flow. If it fires the `.insert()` internally during execution, it violates the "Confirm" step of the open-loop. If it drops the data, the data is lost forever.
- **Alignment Verdict**: The proposed `PendingProposalStore` mathematically satisfies the PRD's Open-Loop requirement by bridging the stateless pipeline and the stateful user confirmation.

#### 🧠 `/01-senior-reviewr` (Senior Synthesis)
- **The Good**: Recognizing that `RealUnifiedPipeline` cannot both natively write to the database (Closed Loop) AND obey the PRD (Open Loop) simultaneously is a massive win. You didn't just hack the `.insert()` into the pipeline.
- **The Yellow Flag (State Management)**: Adding a `PendingProposalStore` (a Singleton holding mutable state) introduces a memory leak risk and a race condition if the user starts a new conversation before confirming the old one. 
- **The Pragmatic Choice**: You need a place to hold the parsed `UnifiedMutation` between the LLM finishing and the user clicking "Confirm". A `PendingStateStore` scoped to the `AgentViewModel` (or session) is the correct architectural pattern here (similar to a Redux pending action). 

---

### 🔴 Hard No
- Do NOT insert into `ScheduledTaskRepository` directly from `RealUnifiedPipeline` before the user confirms. That violates the fundamental safety mechanism of the app.

### 🟡 Yellow Flags
- Be careful with the `PendingProposalStore` lifecycle. If the user types a new prompt instead of clicking "Confirm", you must clear the store to avoid committing stale LLM hallucinations.

### 🟢 Good Calls
- Relying on `SchedulerLinter` for strict Kotlin parsing *before* storing it in the pending state. The data waiting for confirmation is already strongly typed and validated.

### 💡 Senior's Synthesis & Final Verdict
**APPROVED WITH ONE CAVEAT.**
The `PendingProposalStore` is the correct solution to bridge the stateless Unified Pipeline with the Open-Loop UI requirement. 

**Implementation Tweak**: 
Instead of a separate global Singleton, place the pending state management *inside* the `IntentOrchestrator` or `AgentViewModel` tied to the current interaction cycle. When `confirmAnalystPlan()` is called, it should pluck the pending `MutationProposal`, execute the `.insert()` and `.updateProfile()` calls via the repositories, and then clear the pending state.

---

### 🔧 Prescribed Tools
1. `/feature-dev-planner` (Phase 4): Proceed to Execution. Implement the Pending State pattern in `AgentViewModel` / `IntentOrchestrator` and rewrite the `scheduler/spec.md` to match.
