# 📋 Review Conference Report

**Subject**: Architecture Refactor: `SchedulerViewModel`
**Panel**: 
1. `/00-review-conference` (Planner & Triage)
2. `/01-senior-reviewr` (Chair & Pragmatism)
3. `/17-lattice-review` (Architecture & Boundaries)
4. `/06-audit` (Evidence-based Audit)

---

### Triage (Phase 1)
- **Subject**: `SchedulerViewModel` refactoring / rewriting.
- **Type**: Architecture (Contracts, Layers, Coupling) + Code Quality.
- **Complexity**: Complex (Deeply coupled "God Object", touched by multiple pipelines).

---

### Panel Input Summary

#### `/06-audit` — Evidence-based Audit
- **Insight 1 (Size & Scope)**: `SchedulerViewModel.kt` is 860 lines long. It imports `UnifiedPipeline`, `BadgeAudioPipeline`, `AsrService`, `TipGenerator`, `AlarmScheduler`, `InspirationRepository`, etc. It has roughly 50 imports.
- **Insight 2 (Responsibility Bleed)**: The ViewModel currently contains raw audio transcription logic (`simulateFromMic` directly calling `asrService`), conflict resolution algorithms (`resolveConflictGroup`), and cross-module pipeline orchestration.

#### `/17-lattice-review` — Architecture & Boundaries
- **Insight 1 (Layer Violation)**: The UI layer (`ViewModel`) is acting as a Layer 3 (Pipeline) orchestrator. `docs/cerb/interface-map.md` states: "Feature modules (Scheduler) receive results from UnifiedPipeline; they don't call EntityWriter themselves." The ViewModel is directly simulating transcripts, bypassing the `IntentOrchestrator`.
- **Insight 2 (OS Model)**: The ViewModel should be a pure consumer of RAM (`SessionWorkingSet`) and `StateFlow<UiState>`. It should not be actively executing `PipelineInput` and manually parsing `PipelineResult.MutationProposal`.

#### `/01-senior-reviewr` — Pragmatism & Vibe Coding (Chair)
- **Insight 1 (Refactoring Axis - Coupling)**: Looking at the Axis 2 Coupling test: is it easy to decouple? No. The ViewModel holds shared mutable state (`_conflictedTaskIds`, `_rescheduledDates`) tightly interwoven with UI triggers and pipeline results.
- **Insight 2 (The Vibe Check)**: This is a classic "God Object." It started simple, but every new Wave (Multi-Task, Habit Tips, Cross-Off, Reschedule) dumped more business logic into `SchedulerViewModel` because it was the easiest place to reach the UI. It violates "Context Clarity" and "Debuggability."

---

### 🔴 Hard No (Consensus)
- **Do not simply "Extract"**: Surgical extraction will fail because the state is intertwined. You cannot just move `resolveConflictGroup` without moving the 5 `MutableStateFlow` variables it touches.
- **Do not leave this as-is**: The upcoming Parallel UI Development protocol requires a pure `IAgentViewModel` / `FakeAgentViewModel` contract. This file is currently untestable without heavy Mocking, which violates the Anti-Illusion protocol.

### 🟡 Yellow Flags
- **Rewrite Risk**: A full "Nuke and Pave" rewrite of `SchedulerViewModel` will temporarily break the UI. We must ensure we have pure Domain-layer orchestrators ready to catch the logic we rip out.

### 🟢 Good Calls
- **Catching it now**: Identifying this before implementing the `TaskMemoryMapper` Reactive Unification is the exact right operational tempo. If we added the Flow.combine logic to this 860-line file, it would become completely unmaintainable.

### 💡 Senior's Synthesis & Recommendation
This is a **REWRITE**, not an extract. The code is misaligned with the target architecture (UI acting as Layer 3 Orchestrator) and is incredibly tightly coupled.

**What I'd Actually Do (The Nuke and Pave Strategy):**
1. **Push Logic Down**: Create a `SchedulerCoordinator` (or enhance `IntentOrchestrator`) in the Domain/Pipeline layer to handle all AST simulation, conflict resolution math, and pipeline feedback.
2. **Purify the ViewModel**: `SchedulerViewModel` must be stripped down to exactly what its name implies: mapping Domain state to UI state. It should only expose `StateFlow`s for the UI and accept simple `onEvent` intents (e.g., `onToggleDone`, `onDelete`).
3. **The Parallel Spec**: Use this rewrite to formalize the `docs/cerb-ui/scheduler/interface.md` contract. Define exactly what the UI needs, and build a `FakeSchedulerViewModel` first to prove the UI works decoupled from the monolith.

---

### 🔧 Prescribed Tools
1. `/feature-dev-planner` — Initiate a formal plan for "Epic: SchedulerViewModel UI Skin Modernization" (Phase 1: Component Rewrite).
2. Align with the existing tracker item: `🚧 ACTIVE EPIC -> 🎨 Epic: UI Skin Modernization & Protocol Validation -> Phase 2: The Parallel Proving Ground`. This refactor directly fulfills that roadmap goal.
