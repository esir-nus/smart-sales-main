# 📋 Review Conference Report

**Subject**: Feature Dev Planner: Phase 3 (The Cross-Off Lifecycle) Implementation Plan
**Panel**: 
1. `/01-senior-reviewr` (Chair)
2. `/06-audit` (Evidence-based Audit)
3. `/17-lattice-review` (Architecture & Boundaries)

---

### Panel Input Summary

#### `/06-audit` — Evidence-based Audit (Docs-First Protocol)
- **Insight 1 (Literal Spec Violation)**: The implementation plan explicitly notes a contradiction: `docs/cerb/scheduler/spec.md` states "Completed tasks remain in storage", while the Tracker states "deletes the ScheduledTask". 
- **Insight 2 (Action)**: The plan currently chooses to follow the Tracker. The **Docs-First Protocol** strictly forbids this: *NO CODE WITHOUT DOCS.* The Tracker is just an index; the Cerb spec is the Source of Truth. If the architectural direction has shifted to an "Actionable vs Factual" divide, the `scheduler/spec.md` MUST be updated before writing the implementation plan.

#### `/17-lattice-review` — Architecture & Boundaries
- **Insight 1 (Module Coupling Risk)**: The plan proposes placing `TaskMemoryMapper.kt` in `domain/scheduler`. Converting a `Task` to a `MemoryEntry` requires `domain:scheduler` to depend on `domain:memory`. While feature modules can use multiple domains, putting cross-domain translation logic inside a single domain module violates strict boundary isolation.
- **Insight 2 (Action)**: Move `TaskMemoryMapper` to `app-core` (the integration layer) or `core-pipeline` rather than polluting `domain:scheduler` with `domain:memory` imports.

#### `/01-senior-reviewr` — Pragmatism & Vibe Coding (Chair)
- **Insight 1 (The Readiness Score)**: The plan fails the `[UI/UX] Ran Literal Spec Alignment Audit` gate because of the documented drift. Readiness is <70%. This is a **Hard No**. We cannot execute a plan that knowingly contradicts its own Owning Spec.
- **Insight 2 (Pragmatic Fix)**: First, align the docs. Second, move the mapper so we don't fight Gradle or Dagger later.

---

### 🔴 Hard No (Consensus)
- **Executing with Stale Specs**: You cannot implement a "Delete Task" feature when the owning spec literally says "Remain in storage". Update the spec first.

### 🟡 Yellow Flags
- **Cross-Domain Mapping Location**: Putting `Task.toMemoryEntry` inside `domain/scheduler` risks circular dependencies or breaking Gradle module isolation.

### 🟢 Good Calls
- **Extracting the Mapper**: Moving the JSON construction out of the ViewModel into a dedicated Mapper class/function is the right move for testability and pure Android-agnostic logic.
- **Test-Driven via L2**: Mandating writing `L2CrossOffLifecycleTest` first is excellent.

### 💡 Senior's Synthesis & Recommendation
The implementation plan is structurally sound for the *code* phase, but fundamentally illegal under the Project Mono Docs-First rules.

**What I'd Actually Do:**
1. **Halt Execution Phase**: Do not write the `ViewModel` code yet.
2. **Update the Spec**: Run `/04-doc-sync` or manually edit `docs/cerb/scheduler/spec.md` (specifically the "Task Completion Lifecycle" section) to replace the "remain in storage" logic with the new "Actionable to Factual Migration (MemoryEntry creation + Task deletion)" logic.
3. **Refine Plan**: Update the `implementation_plan.md` to shift the `TaskMemoryMapper.kt` location to `app-core/.../ui/drawers/scheduler/` or a dedicated `mapper` package at the app level, ensuring it has access to both domains without violating boundaries.

---

### 🔧 Prescribed Tools
1. Revise `docs/cerb/scheduler/spec.md` to resolve the drift.
2. Revise `implementation_plan.md` to point the mapper to the `app-core` integration layer.
3. Use `notify_user` to request approval for the revised plan.
