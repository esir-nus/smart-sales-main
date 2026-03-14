## 📋 Review Conference Report

**Subject**: Phase 1 — Domain Contract Purge Implementation Plan
**Panel**: 
1. `/01-senior-reviewr` (Chair) — Architecture sanity, patterns 
2. `/17-lattice-review` — Layer compliance, contract integrity

---

### Panel Input Summary

#### Architecture Reviewer — Lattice & Layer Compliance
- The `ClientProfileHub` sits at Layer 5 (Intelligence) as a "File Explorer" (SSD Reads). By having it return `TimelineItemModel.Task` (Layer 4) and `MemoryEntry` (Layer 2) directly, we are adhering to its defined role as an *aggregator*. 
- Returning a `Flow<ProfileActivityState>` is a strong pattern because Room DAOs natively support reactive streams. Ensure the implementation in Phase 2 actually returns a native Room `Flow` to avoid manual data-refresh TOCTOU bugs.

#### 💡 Senior Engineer's Synthesis
- Ripping out `UnifiedActivity` is a textbook **"Nuke and Pave" rewrite**. It was a DTO that forced us to map data just to throw away its inherent context (like urgency rings for Tasks, or `structuredJson` for Memories). 
- I love that **Step 1 of this plan is a pure mechanical deletion**. Deleting the `data class` and letting the Kotlin compiler light up the codebase with errors is the most foolproof way to find all coupling.
- **Assumption Audit**: The plan successfully identified every touchpoint (`FakeClientProfileHub`, `LlmTipGeneratorTest`, `ClientProfileHubTest`) via grep verification. 

---

### 🔴 Hard No
- Do not attempt to implement the real Room DAO queries (`combine` flow) in this PR. Keep Phase 1 strictly to the **Domain Contract Purge and Stubbing**.

### 🟡 Yellow Flags
- `LlmTipGenerator` currently relies on `getUnifiedTimeline()`. When you stub `observeProfileActivityState` to return `emptyFlow()`, the LLM will generate tips with zero timeline context. This is acceptable temporarily for Phase 1 compilation, but be prepared for tests to fail if they assert on tip context before Phase 2 is wired.

### 🟢 Good Calls
- Breaking this down into "Delete and Stub" (Phase 1) vs "Reactive Engine" (Phase 2). This isolates the blast radius of the refactor.
- Using `grep` and the Kotlin compiler as mechanical verification tools. That's the Anti-Laziness mandate in action. 

---

### 🔧 Decision
**Readiness Score**: 100% (Ready to execute)

The plan perfectly adheres to the Project Mono Data-Oriented OS contract. Go ahead and execute Phase 1 (The Purge).
