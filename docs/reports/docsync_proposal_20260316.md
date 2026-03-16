## DocSync Proposal

### Context
Completed Wave 17 T1 execution (Path A Data Contracts) which involved rewriting `SchedulerLinter.kt` and `SchedulerLinterTest.kt` to depend entirely on deterministic DTOs (`FastTrackDtos.kt`). In addition, earlier work extracted core pipeline modules (`RealUnifiedPipeline`, `IntentOrchestrator`) and relocated telemetry tracking points (`PipelineValves`).

### Cross-Reference & Telemetry Audit
- [x] No deprecated `docs/guides/` refs
- [x] No `RealizeTheArchi` refs (now `tracker.md`)
- [x] No `ux-tracker.md` refs (now `ux-experience.md`)
- [x] All file links point to existing files
- [x] `docs/plans/tracker.md` reflects current spec wave status accurately
- [x] `docs/cerb/interface-map.md` includes the newly rewritten `SchedulerLinter` interface
- [x] `docs/plans/telemetry/pipeline-valves.md` was appropriately adjusted during the pipeline telemetry extraction

### Docs Updated / Committed
The following documents and code modifications are ready to be fully synced and committed:
**1. `docs/plans/tracker.md`** & **`docs/cerb/interface-map.md`**
**Why:** Track Wave 17 T1 completion and `SchedulerLinter` layer interface definition. Already updated in the previous task.

**2. `docs/cerb/scheduler-linter/spec.md`** & **`interface.md`**
**Why:** New atomic Cerb shards mapping the Path A FastTrack functionality. Already created and mapped.

**3. `docs/reports/20260316-wave17-t1-acceptance-report.md`**
**Why:** Mechanical verification outcomes.

**Code to Commit:**
- Refactored `SchedulerLinter` and Path A DTOs.
- Centralized `RealEntityWriter` and domain GPS extractions.
- Extracted telemetry models and logic (`PipelineValve` relocated to `:core:telemetry`).

Shall I proceed with `git add` and `git commit` to finalize this sync?
