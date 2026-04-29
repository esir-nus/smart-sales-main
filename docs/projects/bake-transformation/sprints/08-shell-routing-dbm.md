# Sprint Contract: 08-shell-routing-dbm

## 1. Header

- **Slug**: shell-routing-dbm
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Claude-authored contract lineage; Codex authored from the previous-agent plan
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Author Sprint 08 as the next BAKE transformation sprint: a
docs-only delivered-behavior-map sprint for the `shell-routing` domain, following
the Sprint 05 DBM pattern.

**Codex interpretation**: This sprint maps delivered shell-routing behavior from
current docs, Kotlin sources, and tests. It writes the DBM artifact and updates
the project tracker close state. It does not update target core-flow docs, write
a BAKE implementation contract, demote Cerb authority, edit code, or make
runtime/L3 claims without fresh device evidence.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md`
- `docs/projects/bake-transformation/evidence/08-shell-routing-dbm/`

No files outside this list may be written.

Out of scope:

- code edits
- target core-flow updates
- Cerb demotion, archive moves, or authority changes
- BAKE implementation contract writing
- Harmony-native or platform overlay changes
- runtime, device, adb, or L3 claims without explicit evidence captured during
  this sprint

## 4. References

Governance and project protocol:

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `.agent/rules/lessons-learned.md`

Shell-routing north-star and UX governance:

- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `docs/specs/base-runtime-unification.md`
- `docs/specs/prism-ui-ux-contract.md`
- `docs/cerb/interface-map.md`

Shell, home, and island reference docs:

- `docs/cerb-ui/home-shell/spec.md`
- `docs/cerb-ui/dynamic-island/spec.md`
- `docs/cerb-ui/dynamic-island/interface.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md`
- `docs/archive/reports/tests/L3-20260319-sim-wave1-shell-acceptance.md`

Code and test inventory:

- `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellState.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/shell/BaseRuntimeShellCore.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/DynamicIsland.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/BaseRuntimeShellCoreStructureTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/HistoryDrawerStructureTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellStructureTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimConnectivityRoutingTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSettingsRoutingTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimRuntimeIsolationTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinatorTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/components/DynamicIslandStateMapperTest.kt`

## 5. Success Exit Criteria

1. Delivered-behavior map exists at
   `docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`.
   Verify:
   `test -f docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`

2. The map covers launch and first-launch/onboarding routing,
   RuntimeShell/RuntimeShellContent/RuntimeShellState ownership, chat,
   scheduler drawer, audio drawer, history, settings, connectivity, and
   new-session routes.
   Verify:
   `rg -n "Launch|First-Launch|Onboarding|RuntimeShell|RuntimeShellContent|RuntimeShellState|Chat|Scheduler Drawer|Audio Drawer|History|Settings|Connectivity|New Session" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`

3. The map covers drawer exclusivity, audio reselect, Ask AI, badge scheduler
   follow-up prompt, dynamic island arbitration, connectivity takeover,
   smart-only surface blocking, telemetry/log seams, tests, and known gaps.
   Verify:
   `rg -n "Drawer Exclusivity|Audio Reselect|Ask AI|Badge Scheduler Follow-Up|Dynamic Island Arbitration|Connectivity Takeover|Smart-Only Surface Blocking|Telemetry|Tests|Known Gaps" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`

4. The map cites concrete source docs and code rather than relying on agent
   narration. It must cite at least 16 distinct repository paths, including at
   least 6 docs and at least 6 Kotlin source or test paths.
   Verify:
   `rg -o '`(docs|app-core)/[^`]+`' docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md | sort -u | wc -l`

5. The map distinguishes delivered behavior from target behavior, gaps,
   historical reference, and unknowns where core-flow, Cerb, archived reports,
   code, or tests differ.
   Verify:
   `rg -n "Delivered behavior|Target behavior|Gap|Historical reference|Unknown" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`

6. The map records evidence commands used to inspect shell-routing sources and
   tests, including repository search and source reads.
   Verify:
   `rg -n "Evidence Commands|rg |sed -n|find " docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`

7. The map does not claim runtime/L3 proof without fresh device evidence. If
   runtime behavior is discussed, missing `adb logcat` evidence is recorded as a
   gap or unknown.
   Verify:
   `rg -n "adb logcat|runtime evidence|L3|Unknown|Gap" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`

8. Tracker row for sprint 08 is updated to `done`, `stopped`, or `blocked`
   with a factual one-line summary, and the `shell-routing` domain status is
   updated only if the sprint actually closes successfully.
   Verify:
   `rg -n "\| 08 \| shell-routing-dbm \| (done|stopped|blocked)|shell-routing \| base-runtime-active .* (dbm-written|triaged)" docs/projects/bake-transformation/tracker.md`

## 6. Stop Exit Criteria

- **Scope blocker**: The delivered behavior cannot be mapped without writing
  outside the sprint scope.
- **Source ambiguity blocker**: Current core-flow, Cerb/reference docs, archived
  reports, code, and tests conflict so strongly that the operator cannot write a
  factual delivered-behavior map without a product decision.
- **Runtime-evidence blocker**: UI, lifecycle, onboarding handoff, connectivity,
  notification, or device behavior requires fresh adb/logcat evidence. Record
  the missing proof as a gap; do not claim L3 behavior without it.
- **Authority creep**: Any pull toward target core-flow edits, Cerb demotion,
  BAKE contract writing, platform overlay changes, or code changes.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `wc -l docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`
2. `rg -n "Launch|RuntimeShell|RuntimeShellContent|RuntimeShellState|Scheduler Drawer|Audio Drawer|Connectivity|New Session" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`
3. `rg -n "Drawer Exclusivity|Audio Reselect|Ask AI|Badge Scheduler Follow-Up|Dynamic Island Arbitration|Connectivity Takeover|Smart-Only Surface Blocking|Known Gaps" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`
4. `rg -n "Delivered behavior|Target behavior|Gap|Historical reference|Unknown|adb logcat" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`
5. `rg -n "RuntimeShell.kt|RuntimeShellContent.kt|RuntimeShellState.kt|RuntimeShellReducer.kt|BaseRuntimeShellCore.kt|SimShellDynamicIslandCoordinator.kt|SimShellHandoffTest|SimConnectivityRoutingTest" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`
6. `rg -o '`(docs|app-core)/[^`]+`' docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md | sort -u | wc -l`
7. `git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md docs/projects/bake-transformation/evidence/08-shell-routing-dbm/`

## 9. Iteration Ledger

Operator fills this section during Sprint 08 operation.

## 10. Closeout

Operator fills this section at Sprint 08 exit.
