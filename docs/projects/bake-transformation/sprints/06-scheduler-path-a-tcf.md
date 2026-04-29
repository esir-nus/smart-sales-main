# Sprint Contract: 06-scheduler-path-a-tcf

## 1. Header

- **Slug**: scheduler-path-a-tcf
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Claude-authored contract lineage; Codex authored from the revised plan
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Author `06-scheduler-path-a-tcf` as the next BAKE transformation
sprint. It remains docs-only: update Scheduler Path A core-flow docs with
Sprint 05 delivered-vs-target gaps, then author Sprint 07 as the BAKE contract
sprint. Add the newly approved testing policy that future L3/runtime BAKE
evidence should use a centralized Debug HUD/Debug Lab, not scattered
feature-local debug buttons.

**Codex interpretation**: This sprint consumes Sprint 05's Scheduler Path A
delivered-behavior map, adds base-runtime metadata and delivered-vs-target gap
notes to the scheduler core-flow docs, authors the follow-on BAKE contract
sprint, and updates the project tracker. It does not edit code, write the BAKE
contract itself, or make runtime claims.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md`
- `docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md`
- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`

No files outside this list may be written.

Out of scope:

- code edits
- BAKE contract content at `docs/bake-contracts/scheduler-path-a.md`
- interface-map authority changes
- Cerb demotion or archival
- runtime, device, adb, or L3 claims

## 4. References

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/05-scheduler-path-a-dbm.md`
- `docs/projects/bake-transformation/evidence/05-scheduler-path-a-dbm/delivered-behavior-map.md`
- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/interface-map.md`
- `app-core/src/main/java/com/smartsales/prism/ui/debug/L2DebugHud.kt`

## 5. Success Exit Criteria

1. Both Scheduler Path A core-flow docs include `scope: base-runtime-active`.
   Verify:
   `rg -n "scope: base-runtime-active" docs/core-flow/scheduler-fast-track-flow.md docs/core-flow/sim-scheduler-path-a-flow.md`

2. Both core-flow docs explicitly distinguish delivered behavior, target
   behavior, and gaps from Sprint 05.
   Verify:
   `rg -n "Delivered behavior|Target behavior|Gap" docs/core-flow/scheduler-fast-track-flow.md docs/core-flow/sim-scheduler-path-a-flow.md`

3. The core-flow gap notes include the Sprint 05 gaps for top-level voice alarm
   arming, pre-fork candidate entity envelope, deterministic exact-create
   shortcuts, voice delete, fully undated vague create, canonical valve names,
   and shared/SIM-local create-routing drift.
   Verify:
   `rg -n "alarm arming|pre-fork candidate entity|Deterministic exact-create|Voice delete|Fully undated vague|Canonical valve|SIM-local create routing" docs/core-flow/scheduler-fast-track-flow.md docs/core-flow/sim-scheduler-path-a-flow.md`

4. Sprint 07 contract exists and passes the ten-section sprint-contract schema
   check with sections 1-8 filled and sections 9-10 left for the operator.
   Verify:
   `rg -n "## 1\\. Header|## 2\\. Demand|## 3\\. Scope|## 4\\. References|## 5\\. Success Exit Criteria|## 6\\. Stop Exit Criteria|## 7\\. Iteration Bound|## 8\\. Required Evidence Format|## 9\\. Iteration Ledger|## 10\\. Closeout" docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md`

5. Sprint 07 contains the centralized Debug HUD / Debug Lab policy anchored by
   `L2DebugHud`, allows centralized runtime scenarios, and prohibits scattered
   feature-local debug buttons.
   Verify:
   `rg -n "Debug HUD|Debug Lab|L2DebugHud|feature-local debug buttons" docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md`

6. The project tracker records Sprint 06 as `done`, records Sprint 07 as
   `authored`, moves Scheduler Path A to `tcf-written`, and includes an
   ongoing-justification entry because Sprint 07 is authored.
   Verify:
   `rg -n "\\| 06 \\| scheduler-path-a-tcf \\| done|\\| 07 \\| scheduler-path-a-bake-contract \\| authored|scheduler-path-a \\| base-runtime-active .* tcf-written|Ongoing" docs/projects/bake-transformation/tracker.md`

## 6. Stop Exit Criteria

- **Missing input**: Sprint 05 did not close successfully or the delivered map
  is absent.
- **Product decision blocker**: The delivered map exposes a target behavior
  choice that cannot be stated as a gap without deciding product behavior.
- **Scope creep**: Any pull toward code edits, interface-map updates, Cerb
  demotion, physical archival moves, or writing the Scheduler Path A BAKE
  contract itself.
- **Runtime-evidence blocker**: Any request to prove runtime behavior in this
  docs-only sprint. Record the future evidence requirement; do not claim L3.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `wc -l docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md`
2. `rg -n "## 1\\. Header|## 2\\. Demand|## 3\\. Scope|## 4\\. References|## 5\\. Success Exit Criteria|## 6\\. Stop Exit Criteria|## 7\\. Iteration Bound|## 8\\. Required Evidence Format|## 9\\. Iteration Ledger|## 10\\. Closeout" docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md`
3. `rg -n "scope: base-runtime-active|Delivered behavior|Target behavior|Gap|Debug HUD|L2DebugHud" docs/core-flow/scheduler-fast-track-flow.md docs/core-flow/sim-scheduler-path-a-flow.md docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md`
4. `rg -n "\\| 06 \\| scheduler-path-a-tcf \\| done|\\| 07 \\| scheduler-path-a-bake-contract \\| authored|Ongoing" docs/projects/bake-transformation/tracker.md`
5. `git diff --check -- docs/projects/bake-transformation docs/core-flow/scheduler-fast-track-flow.md docs/core-flow/sim-scheduler-path-a-flow.md`

## 9. Iteration Ledger

- Iteration 1 — Reviewed the revised plan against the sprint-contract skill,
  project tracker, Sprint 05 delivered-behavior map, scheduler core-flow docs,
  BAKE protocol, and existing `L2DebugHud` debug surface. Found that the plan
  was sound for authoring, but execution closeout required the tracker to move
  Sprint 06 from authored to done and the Scheduler Path A domain from
  `dbm-written` to `tcf-written`. Added base-runtime metadata and
  delivered/target/gap notes to both scheduler core-flow docs, authored Sprint
  07 with the centralized Debug HUD / Debug Lab policy, updated the tracker,
  and ran the required static verification commands.

## 10. Closeout

- **Status**: success
- **Tracker summary**: Added base-runtime scope metadata and Sprint 05
  delivered-vs-target gap notes to Scheduler Path A core flows, then authored
  the Scheduler Path A BAKE contract sprint.
- **Findings**:
  - The revised plan was coherent for a docs-only TCF sprint: no runtime or adb
    evidence was required because the work only authored/synchronized docs.
  - Execution required closing Sprint 06 as `done` after the plan was applied;
    leaving it `authored` would have represented authoring state, not operated
    sprint state.
  - Sprint 07 correctly needs to keep future L3/runtime scenario controls
    centralized under Debug HUD / Debug Lab direction instead of scattered
    feature-local debug buttons.
- **Files changed**:
  - `docs/core-flow/scheduler-fast-track-flow.md`
  - `docs/core-flow/sim-scheduler-path-a-flow.md`
  - `docs/projects/bake-transformation/tracker.md`
  - `docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md`
  - `docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md`
- **Evidence**:

```text
$ wc -l docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md
  216 docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md
  183 docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md
  399 total

$ rg -n "## 1\\. Header|## 2\\. Demand|## 3\\. Scope|## 4\\. References|## 5\\. Success Exit Criteria|## 6\\. Stop Exit Criteria|## 7\\. Iteration Bound|## 8\\. Required Evidence Format|## 9\\. Iteration Ledger|## 10\\. Closeout" docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:3:## 1. Header
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:14:## 2. Demand
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:28:## 3. Scope
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:58:## 4. References
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:86:## 5. Success Exit Criteria
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:134:## 6. Stop Exit Criteria
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:153:## 7. Iteration Bound
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:157:## 8. Required Evidence Format
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:177:## 9. Iteration Ledger
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:181:## 10. Closeout
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:3:## 1. Header
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:14:## 2. Demand
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:29:## 3. Scope
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:49:## 4. References
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:63:## 5. Success Exit Criteria
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:98:## 6. Stop Exit Criteria
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:111:## 7. Iteration Bound
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:115:## 8. Required Evidence Format
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:125:## 9. Iteration Ledger
docs/projects/bake-transformation/sprints/06-scheduler-path-a-tcf.md:137:## 10. Closeout

$ rg -n "scope: base-runtime-active|Delivered behavior|Target behavior|Gap|Debug HUD|L2DebugHud" docs/core-flow/scheduler-fast-track-flow.md docs/core-flow/sim-scheduler-path-a-flow.md docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:19:supporting reference, and using centralized Debug HUD / Debug Lab scenarios for
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:77:- `app-core/src/main/java/com/smartsales/prism/ui/debug/L2DebugHud.kt`
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:127:   `rg -n "Debug HUD|Debug Lab|L2DebugHud|feature-local debug buttons|Scheduler Path A|Connectivity / Badge Session|Audio Pipeline|Shell Routing|Pipeline Telemetry|real pipeline paths|real execution" docs/bake-contracts/scheduler-path-a.md`
docs/projects/bake-transformation/sprints/07-scheduler-path-a-bake-contract.md:143:  trigger that cannot be reached from the existing central Debug HUD / Debug Lab
docs/core-flow/sim-scheduler-path-a-flow.md:2:scope: base-runtime-active
docs/core-flow/sim-scheduler-path-a-flow.md:46:Delivered behavior:
docs/core-flow/sim-scheduler-path-a-flow.md:53:Target behavior:
docs/core-flow/sim-scheduler-path-a-flow.md:60:Gap:
docs/core-flow/scheduler-fast-track-flow.md:2:scope: base-runtime-active
docs/core-flow/scheduler-fast-track-flow.md:43:Delivered behavior:
docs/core-flow/scheduler-fast-track-flow.md:50:Target behavior:
docs/core-flow/scheduler-fast-track-flow.md:58:Gap:

$ rg -n "\\| 06 \\| scheduler-path-a-tcf \\| done|\\| 07 \\| scheduler-path-a-bake-contract \\| authored|Ongoing|scheduler-path-a \\| base-runtime-active .* tcf-written" docs/projects/bake-transformation/tracker.md
30:| 06 | scheduler-path-a-tcf | done | Added base-runtime scope metadata and Sprint 05 delivered-vs-target gap notes to Scheduler Path A core flows, then authored the Scheduler Path A BAKE contract sprint. | [sprints/06-scheduler-path-a-tcf.md](sprints/06-scheduler-path-a-tcf.md) |
31:| 07 | scheduler-path-a-bake-contract | authored | Write the Scheduler Path A BAKE contract, sync interface-map authority, and demote Scheduler Path A Cerb docs to supporting reference. | [sprints/07-scheduler-path-a-bake-contract.md](sprints/07-scheduler-path-a-bake-contract.md) |
39:| scheduler-path-a | base-runtime-active | `docs/core-flow/scheduler-fast-track-flow.md` | high | tier-1 | tcf-written |
77:Project expected to exceed six sprints. Ongoing-justification entry required
80:## Ongoing Justification

$ git diff --check -- docs/projects/bake-transformation docs/core-flow/scheduler-fast-track-flow.md docs/core-flow/sim-scheduler-path-a-flow.md
# no output
```

- **Hardware evidence**: not applicable; this sprint was docs-only and made no
  runtime claims.
- **Lesson proposals**: none.
- **CHANGELOG line**: none; internal BAKE transformation docs only.
