## DocSync Proposal

### Context
Completed Project Mono Wave 2 (EntityWriter Linter Upgrade). Ripped out `JSONObject` from `SchedulerLinter` in favor of strict `kotlinx.serialization` against the `UnifiedMutation` data class. Upgraded the `L2` Integration test payloads to match the new strict contract. 

### Cross-Reference Audit
- [x] No deprecated `docs/guides/` refs
- [x] No `RealizeTheArchi` refs (now `tracker.md`)
- [x] No `ux-tracker.md` refs (now `ux-experience.md`)
- [x] All file links point to existing files
- [x] `docs/plans/tracker.md` reflects current spec wave status accurately
- [x] `docs/cerb/interface-map.md` includes any newly added interfaces or cross-module edges

### Docs to Update

#### 1. docs/cerb/core-contracts/spec.md
**Why:** Needs to reflect the completion of Wave 3 (`Linter Decoupling`), which was essentially the work done in this session mapping to Mono Wave 2.
**Changes:**
- Update Wave 3 (Linter Decoupling) status to `✅ SHIPPED`.
- Update doc State to reflect real-world progress.

#### 2. docs/cerb/scheduler/spec.md
**Why:** The `SchedulerLinter` section needs an update to note the architectural change to `UnifiedMutation` and strict serialization, replacing the old essay-grading logic.
**Changes:**
- Update the Architecture/Process descriptions of `SchedulerLinter` to emphasize `kotlinx.serialization` instead of regex/JSONObject.

#### 3. docs/cerb/unified-pipeline/spec.md
**Why:** The Linter integration is a key step in the Pipeline.
**Changes:**
- State in the Linter step that Evaluators (like `SchedulerLinter`) now use strict `UnifiedMutation` contract enforcement.

### Docs NOT Updated (and why)
- `docs/plans/tracker.md`: Already updated to Wave 2 `SHIPPED` during Phase 4.5 Ship Gate.
- `docs/cerb/entity-writer/spec.md`: Already updated during Phase 4.5.
- `docs/cerb/interface-map.md`: No new cross-module dependencies were introduced; `SchedulerLinter` mapping to `UnifiedMutation` was effectively established during Wave 1's creation of `core-contracts`.
