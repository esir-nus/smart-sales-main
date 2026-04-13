## DocSync Proposal

### Context
Extracted `IAgentViewModel` to establish a Parallel UI Skin Contract, decoupling UI (Skin) from Backend Pipeline (Brain). 
Gutted the deprecated `prism-ui-ux-contract.md` and transitioned the single source of truth for UI states to `docs/cerb-ui/agent-intelligence/interface.md`.
Implemented `UiSpecAlignmentTest` to mechanically enforce the Docs-First protocol between `interface.md` and `UiState` via Reflection.

### Cross-Reference Audit
- [x] No deprecated `docs/guides/` refs found
- [x] No `RealizeTheArchi` refs found in active docs
- [x] No `ux-tracker.md` refs found in active docs
- [x] All file links point to existing files
- [x] `docs/plans/tracker.md` reflects current spec wave status accurately (Wave 6 T7 and T8 marked complete)
- [x] `docs/cerb/interface-map.md` is intact and up to date

### Docs to Update / Updated

#### 1. `docs/plans/tracker.md`
**Why**: Status tracking.
**Changes**:
- Marked Wave 6 T7 and T8 as Shipped.
- Logged the changelog for 2026-03-14 for the Parallel UI Contract and Docs-First UI Verification.
*(Already completed during the task)*

#### 2. `docs/cerb-ui/agent-intelligence/interface.md`
**Why**: Single Source of Truth for the UI Contract.
**Changes**:
- Updated Section 3 (Data Contract) to exactly match the 13 `UiState` subclasses at runtime.
*(Already completed during the task)*

#### 3. `docs/specs/prism-ui-ux-contract.md`
**Why**: Deprecated in favor of the new Parallel UI Contract.
**Changes**:
- Gutted legacy content. Replaced with the "Brain, Body, and Skin" metaphor explaining the new decoupled `IAgentViewModel` architecture.
*(Already completed during the task)*

#### 4. `.agent/workflows/acceptance-team-[tool].md`
**Why**: The Acceptance Team needed to be upgraded to use the new mechanical scripts.
**Changes**:
- Updated the Contract Examiner to explicitly require running `./gradlew :app-core:testDebugUnitTest --tests "*PromptCompilerBadgeTest*"` and `./gradlew :app-core:testDebugUnitTest --tests "*UiSpecAlignmentTest*"`.
*(Already completed during the task)*

### Docs NOT Updated (and why)
- `docs/cerb/interface-map.md`: No new cross-module boundaries were drawn, only extracted an interface within the same `app-core` layer boundaries. 

---
**Ready to commit all these changes?**
