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

- **Iteration 1 — 2026-04-29**
  - Read the sprint contract, BAKE tracker, top-level tracker, sprint-contract
    schema, BAKE protocol, project-structure spec, lessons index, shell-routing
    core flow, UX governance flow, base-runtime unification spec, home-shell and
    dynamic-island shards, interface map, historical L3 reports, and the
    execution-critical Kotlin/test paths.
  - Wrote
    `docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`
    with delivered-vs-target/gap/historical/unknown sections and evidence
    commands.
  - Updated the Sprint 08 tracker row to `done` and the `shell-routing` domain
    status to `dbm-written`.
  - Evaluator result: static acceptance checks passed. No runtime/L3 claims were
    made without fresh `adb logcat`; missing runtime proof is recorded as Gap or
    Unknown in the DBM.

## 10. Closeout

**Status**: success

**Tracker summary**: Delivered behavior map created for shell routing, covering
launch, first-launch setup, RuntimeShell ownership, drawer routes,
dynamic-island/connectivity arbitration, smart-only blocking, telemetry, tests,
and gaps.

**Changed files**:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md`
- `docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`

**Evidence command outputs**:

```text
$ test -f docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md && echo exists
exists
```

```text
$ rg -n "^## [0-9]+\. Header|^## [0-9]+\. Demand|^## [0-9]+\. Scope|^## [0-9]+\. References|^## [0-9]+\. Success Exit Criteria|^## [0-9]+\. Stop Exit Criteria|^## [0-9]+\. Iteration Bound|^## [0-9]+\. Required Evidence Format|^## [0-9]+\. Iteration Ledger|^## [0-9]+\. Closeout" docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md
3:## 1. Header
14:## 2. Demand
26:## 3. Scope
46:## 4. References
96:## 5. Success Exit Criteria
145:## 6. Stop Exit Criteria
159:## 7. Iteration Bound
163:## 8. Required Evidence Format
175:## 9. Iteration Ledger
189:## 10. Closeout
```

```text
$ wc -l docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md
249 docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md
```

```text
$ rg -n "Launch|RuntimeShell|RuntimeShellContent|RuntimeShellState|Scheduler Drawer|Audio Drawer|Connectivity|New Session" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md
55:Delivered behavior: production launch mounts `MainActivity`, which injects `BadgeAudioPipeline`, onboarding/discoverability gates, theme state, and then renders `RuntimeShell(...)` inside `PrismTheme` and `PrismSystemBarsEffect`.
65:## First-Launch And Onboarding
77:## RuntimeShell Ownership
97:## Scheduler Drawer
107:## Audio Drawer
133:## Connectivity
143:## New Session
```

```text
$ rg -n "Drawer Exclusivity|Audio Reselect|Ask AI|Badge Scheduler Follow-Up|Dynamic Island Arbitration|Connectivity Takeover|Smart-Only Surface Blocking|Known Gaps" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md
151:## Drawer Exclusivity
161:## Audio Reselect
171:## Ask AI
179:## Badge Scheduler Follow-Up
189:## Dynamic Island Arbitration
199:## Connectivity Takeover
209:## Smart-Only Surface Blocking
237:## Known Gaps
```

```text
$ rg -n "Delivered behavior|Target behavior|Gap|Historical reference|Unknown|adb logcat" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md
5:Evidence class: static DBM from docs, Kotlin source, and JVM tests. No fresh `adb logcat` was captured in this sprint, so runtime/L3 behavior is marked as Historical reference, Gap, or Unknown unless it is proven by current source/test evidence.
55:Delivered behavior: production launch mounts `MainActivity`...
57:Target behavior: `docs/core-flow/sim-shell-routing-flow.md` requires standalone base-runtime shell launch...
59:Gap: none for the static root ownership claim...
61:Historical reference: `docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md` recorded an L3 launch...
63:Unknown: current installed-device launch state on 2026-04-29 because no fresh `adb logcat` or `dumpsys` was collected.
```

```text
$ rg -n "RuntimeShell.kt|RuntimeShellContent.kt|RuntimeShellState.kt|RuntimeShellReducer.kt|BaseRuntimeShellCore.kt|SimShellDynamicIslandCoordinator.kt|SimShellHandoffTest|SimConnectivityRoutingTest" docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md
23:- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`
24:- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
25:- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellState.kt`
26:- `sed -n '1,280p' app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`
27:- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/ui/shell/BaseRuntimeShellCore.kt`
28:- `sed -n '1,640p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt`
33:- `sed -n '1,260p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimConnectivityRoutingTest.kt`
38:- `sed -n '1,860p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`
```

```text
$ rg -o '`(docs|app-core)/[^`]+`' docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md | sort -u | wc -l
31

$ rg -o '`docs/[^`]+`' docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md | sort -u | wc -l
11

$ rg -o '`app-core/[^`]+\.kt`' docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md | sort -u | wc -l
20
```

```text
$ rg -n "\| 08 \| shell-routing-dbm \| (done|stopped|blocked)|shell-routing \| base-runtime-active .* (dbm-written|triaged)" docs/projects/bake-transformation/tracker.md
32:| 08 | shell-routing-dbm | done | Delivered behavior map created for shell routing, covering launch, first-launch setup, RuntimeShell ownership, drawer routes, dynamic-island/connectivity arbitration, smart-only blocking, telemetry, tests, and gaps. | [sprints/08-shell-routing-dbm.md](sprints/08-shell-routing-dbm.md) |
41:| shell-routing | base-runtime-active | `docs/core-flow/sim-shell-routing-flow.md` | high | tier-1 | dbm-written |
```

```text
$ git diff --check -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md docs/projects/bake-transformation/evidence/08-shell-routing-dbm/
<no output>
```

```text
$ git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md docs/projects/bake-transformation/evidence/08-shell-routing-dbm/
 .../sprints/08-shell-routing-dbm.md                | 143 ++++++++++++++++++++-
 docs/projects/bake-transformation/tracker.md       |   4 +-
 2 files changed, 143 insertions(+), 4 deletions(-)

$ git status --short -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md docs/projects/bake-transformation/evidence/08-shell-routing-dbm/
 M docs/projects/bake-transformation/tracker.md
 M docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md
?? docs/projects/bake-transformation/evidence/08-shell-routing-dbm/
```

Note: `git diff --stat` does not list the new evidence file while it is
untracked. `git status --short` is included to show the new artifact in the
dirty tree.

**Lesson proposals**: none.

**CHANGELOG line**: none. This was docs-only DBM work with no user-visible
product behavior change.
