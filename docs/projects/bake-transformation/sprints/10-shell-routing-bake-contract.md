# Sprint Contract: 10-shell-routing-bake-contract

## 1. Header

- **Slug**: shell-routing-bake-contract
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Codex-authored from Sprint 09 target-flow alignment
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Author the follow-on shell-routing BAKE implementation contract
sprint after Sprint 09 target core-flow alignment.

**Codex interpretation**: This sprint writes the shell-routing BAKE
implementation contract from Sprint 08 delivered behavior and Sprint 09 target
alignment. It must record the delivered implementation, name core-flow gaps
instead of accepting drift, sync authority docs, and require runtime evidence
with `adb logcat` anywhere runtime behavior is claimed.

## 3. Scope

Docs-only. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md`
- `docs/bake-contracts/shell-routing.md`
- `docs/cerb/interface-map.md`
- `docs/cerb-ui/home-shell/spec.md`
- `docs/cerb-ui/dynamic-island/spec.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`

No files outside this list may be written.

Out of scope:

- code edits
- physical Cerb archival moves
- changing core-flow target behavior
- Harmony-native or platform overlay changes
- runtime success claims without fresh `adb logcat`

## 4. References

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md`
- `docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`
- `docs/projects/bake-transformation/sprints/09-shell-routing-tcf.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `docs/specs/base-runtime-unification.md`
- `docs/cerb/interface-map.md`
- `docs/cerb-ui/home-shell/spec.md`
- `docs/cerb-ui/dynamic-island/spec.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `app-core/src/main/java/com/smartsales/prism/MainActivity.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellState.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimConnectivityRoutingTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSettingsRoutingTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimRuntimeIsolationTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinatorTest.kt`

## 5. Success Exit Criteria

1. BAKE contract exists at `docs/bake-contracts/shell-routing.md` and includes
   protocol frontmatter plus required BAKE sections.
   Verify:
   `test -f docs/bake-contracts/shell-routing.md`
   `rg -n "^protocol: BAKE$|^version: 1.0$|^domain: shell-routing$|^runtime: base-runtime-active$|^last-verified:" docs/bake-contracts/shell-routing.md`
   `rg -n "^## Pipeline Contract$|^## Telemetry Joints$|^## UI Docking Surface$|^## Core-Flow Gap$|^## Test Contract$|^## Cross-Platform Notes$" docs/bake-contracts/shell-routing.md`

2. The contract records shell-routing pipeline inputs, outputs/guarantees,
   invariants, and error paths for launch, first-launch onboarding handoff,
   drawer routes, connectivity entry, dynamic-island arbitration, smart-only
   blocking, history/settings/new-session, and badge scheduler follow-up.
   Verify:
   `rg -n "### Inputs|### Outputs / Guarantees|### Invariants|### Error Paths|first-launch|drawer|connectivity|dynamic-island|SMART_SURFACE_BLOCKED|new-session|follow-up" docs/bake-contracts/shell-routing.md`

3. The contract names telemetry joints and gaps, including canonical target
   valves and delivered gaps for missing `SIM_ENTRY_STARTED`,
   `SIM_SHELL_MOUNTED`, drawer-open/close valves, and
   `SMART_SURFACE_BLOCKED`.
   Verify:
   `rg -n "SIM_ENTRY_STARTED|SIM_SHELL_MOUNTED|SCHEDULER_DRAWER_OPENED|AUDIO_DRAWER_OPENED|SCHEDULER_DRAWER_CLOSED|AUDIO_DRAWER_CLOSED|SMART_SURFACE_BLOCKED|Delivered gap" docs/bake-contracts/shell-routing.md`

4. The contract records Sprint 09 core-flow gaps without weakening target
   behavior, including `PARTIAL_WIFI_DOWN` dynamic-island takeover, first-launch
   handoff wording, Drawer Conflict proof, and new-session follow-up-clear test
   coverage.
   Verify:
   `rg -n "PARTIAL_WIFI_DOWN|first-launch|Drawer Conflict|new-session|Core-Flow Gap|must not|target" docs/bake-contracts/shell-routing.md`

5. Runtime claims require runtime evidence. If the operator claims installed-app
   launch, onboarding, drawer conflict, dynamic-island, connectivity, or follow-up
   behavior, closeout includes filtered `adb logcat` evidence; otherwise the
   contract records those branches as static-only or unproven runtime gaps.
   Verify:
   `rg -n "adb logcat|runtime evidence|static-only|unproven runtime gap" docs/bake-contracts/shell-routing.md docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md`

6. `docs/cerb/interface-map.md` cites `docs/bake-contracts/shell-routing.md` as
   the implementation record for shell routing, while preserving core-flow docs
   as behavioral north stars.
   Verify:
   `rg -n "shell-routing\\.md|BAKE|Shell Routing|shell routing" docs/cerb/interface-map.md`

7. The scoped Cerb shell/dynamic-island/home docs mark themselves as
   supporting/reference docs beneath the shell-routing BAKE contract without
   deleting historical content.
   Verify:
   `rg -n "BAKE|supporting|reference" docs/cerb-ui/home-shell/spec.md docs/cerb-ui/dynamic-island/spec.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md`

8. Tracker row for Sprint 10 is updated to `done`, `stopped`, or `blocked` with
   a factual one-line summary.
   Verify:
   `rg -n "\\| 10 \\| shell-routing-bake-contract \\| (done|stopped|blocked)" docs/projects/bake-transformation/tracker.md`

## 6. Stop Exit Criteria

- **Missing target-flow input**: Sprint 09 did not close successfully or the
  shell-routing target-flow docs lack Sprint 08 delivered/target/gap alignment.
- **Contract ambiguity blocker**: The operator cannot decide whether a branch is
  delivered behavior, target behavior, or a core-flow gap from Sprint 08/09 docs
  without a product decision.
- **Runtime-evidence blocker**: Any claim about installed-app launch, onboarding
  handoff, drawer conflict behavior, connectivity takeover, follow-up behavior,
  or telemetry delivery lacks fresh `adb logcat`. Record the gap; do not claim
  success.
- **Scope creep**: Any pull toward code edits, core-flow target changes, physical
  Cerb archival moves, or platform overlay changes.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `wc -l docs/bake-contracts/shell-routing.md`
2. `rg -n "^protocol: BAKE$|^## Pipeline Contract$|^## Telemetry Joints$|^## UI Docking Surface$|^## Core-Flow Gap$|^## Test Contract$|^## Cross-Platform Notes$" docs/bake-contracts/shell-routing.md`
3. `rg -n "first-launch|PARTIAL_WIFI_DOWN|SIM_ENTRY_STARTED|SIM_SHELL_MOUNTED|SMART_SURFACE_BLOCKED|Drawer Conflict|new-session|follow-up" docs/bake-contracts/shell-routing.md`
4. `rg -n "shell-routing\\.md|BAKE|Shell Routing|supporting|reference" docs/cerb/interface-map.md docs/cerb-ui/home-shell/spec.md docs/cerb-ui/dynamic-island/spec.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md`
5. `git diff --stat -- docs/bake-contracts/shell-routing.md docs/cerb/interface-map.md docs/cerb-ui/home-shell/spec.md docs/cerb-ui/dynamic-island/spec.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md docs/projects/bake-transformation/`

If runtime/L3 validation is attempted or required by a future scope decision,
closeout must additionally include filtered `adb logcat` output proving the
specific runtime claim. Screenshots and static tests may support that evidence,
but they do not replace logcat.

## 9. Iteration Ledger

- **Iteration 1 — 2026-04-29**
  - Read the Sprint 10 contract, Sprint 08 DBM evidence, Sprint 09 TCF
    closeout, BAKE protocol, sprint-contract schema, project tracker, current
    shell-routing core flows, interface map, and scoped shell Cerb docs.
  - Wrote `docs/bake-contracts/shell-routing.md` from delivered Sprint 08
    behavior and Sprint 09 target-flow alignment.
  - Synced the interface-map BAKE overlay and added concise BAKE authority
    notices to the scoped home-shell, dynamic-island, and historical sim-shell
    docs.
  - Updated the project tracker: Sprint 10 is `done` and `shell-routing` is
    `bake-contract-written`.
  - Evaluator result: static BAKE frontmatter, section, required-term,
    authority-sync, tracker, and whitespace checks passed. No runtime/L3 claims
    were made.

## 10. Closeout

**Status**: success

**Tracker summary**: Wrote the shell-routing BAKE contract, synced interface-map
authority, and demoted shell Cerb docs to supporting reference.

**Changed files**:

- `docs/bake-contracts/shell-routing.md`
- `docs/cerb/interface-map.md`
- `docs/cerb-ui/home-shell/spec.md`
- `docs/cerb-ui/dynamic-island/spec.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md`

**Evidence command outputs**:

```text
$ test -f docs/bake-contracts/shell-routing.md && wc -l docs/bake-contracts/shell-routing.md
181 docs/bake-contracts/shell-routing.md
```

```text
$ rg -n "^protocol: BAKE$|^## Pipeline Contract$|^## Telemetry Joints$|^## UI Docking Surface$|^## Core-Flow Gap$|^## Test Contract$|^## Cross-Platform Notes$" docs/bake-contracts/shell-routing.md
2:protocol: BAKE
20:## Pipeline Contract
98:## Telemetry Joints
123:## UI Docking Surface
131:## Core-Flow Gap
153:## Test Contract
171:## Cross-Platform Notes
```

```text
$ rg -n "first-launch|PARTIAL_WIFI_DOWN|SIM_ENTRY_STARTED|SIM_SHELL_MOUNTED|SMART_SURFACE_BLOCKED|Drawer Conflict|new-session|follow-up" docs/bake-contracts/shell-routing.md
25:- Onboarding completion state and the forced first-launch setup flag.
27:  handles, history, settings, new-session, connectivity, scheduler, and audio
33:  follow-up prompts.
48:  badge scheduler follow-up state when appropriate.
56:  follow-up session and show a prompt/chip, but it must not force-switch chat.
73:- MUST record smart-only route blocks through `SMART_SURFACE_BLOCKED` or an
100:- [INPUT_RECEIVED]: app launch, first-launch setup gate, header action,
102:  settings/history entry, new-session request, and badge scheduler completion.
117:canonical valves for `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, `CHAT_VISIBLE`,
120:`SMART_SURFACE_BLOCKED`. Future work must converge delivered logs with these
137:- Gap: delivered dynamic-island code treats `PARTIAL_WIFI_DOWN` as a persistent
141:  including `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, drawer open/close valves,
142:  and `SMART_SURFACE_BLOCKED`.
143:- Gap: Drawer Conflict proof remains static/source-backed and historical L3 was
146:- Gap: new-session follow-up-clear behavior exists in source/test helpers, but
150:  file-picker escape, current telemetry delivery, and badge-origin follow-up
167:  claimed branch, including launch/mount, first-launch onboarding, Drawer
169:  new-session follow-up clear, and badge follow-up prompt where applicable.
```

```text
$ rg -n "shell-routing\.md|BAKE|Shell Routing|supporting|reference" docs/cerb/interface-map.md docs/cerb-ui/home-shell/spec.md docs/cerb-ui/dynamic-island/spec.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md
docs/cerb/interface-map.md:7:> **Last Updated**: 2026-04-29 (Shell Routing BAKE contract is now the implementation record; shell Cerb docs remain supporting/reference docs beneath it)
docs/cerb/interface-map.md:56:- `docs/bake-contracts/shell-routing.md` is the verified BAKE implementation
docs/cerb/interface-map.md:57:  contract for Shell Routing.
docs/cerb/interface-map.md:60:  behavioral north-star docs above the Shell Routing BAKE contract.
docs/cerb/interface-map.md:65:  the Shell Routing BAKE contract until a later archival sprint moves
docs/cerb-ui/home-shell/spec.md:6:> **BAKE Authority Notice**: Supporting/reference UI shard beneath
docs/cerb-ui/dynamic-island/spec.md:7:> **BAKE Authority Notice**: Supporting/reference UI shard beneath
docs/cerb/sim-shell/spec.md:8:> **BAKE Authority Notice**: Supporting/reference historical shard beneath
docs/cerb/sim-shell/interface.md:6:> **BAKE Authority Notice**: Supporting/reference historical interface shard
```

```text
$ rg -n "\| 10 \| shell-routing-bake-contract \| (done|stopped|blocked)|shell-routing \| base-runtime-active .* bake-contract-written" docs/projects/bake-transformation/tracker.md
34:| 10 | shell-routing-bake-contract | done | Wrote the shell-routing BAKE contract, synced interface-map authority, and demoted shell Cerb docs to supporting reference. | [sprints/10-shell-routing-bake-contract.md](sprints/10-shell-routing-bake-contract.md) |
43:| shell-routing | base-runtime-active | `docs/core-flow/sim-shell-routing-flow.md` | high | tier-1 | bake-contract-written |
```

```text
$ git diff --check -- docs/bake-contracts/shell-routing.md docs/cerb/interface-map.md docs/cerb-ui/home-shell/spec.md docs/cerb-ui/dynamic-island/spec.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md
<no output>
```

```text
$ git diff --stat --cached -- docs/bake-contracts/shell-routing.md docs/cerb/interface-map.md docs/cerb-ui/home-shell/spec.md docs/cerb-ui/dynamic-island/spec.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md docs/projects/bake-transformation/
 docs/bake-contracts/shell-routing.md               | 181 +++++++++++++++++++++
 docs/cerb-ui/dynamic-island/spec.md                |   3 +
 docs/cerb-ui/home-shell/spec.md                    |   3 +
 docs/cerb/interface-map.md                         |  13 +-
 docs/cerb/sim-shell/interface.md                   |   4 +
 docs/cerb/sim-shell/spec.md                        |   4 +
 .../sprints/10-shell-routing-bake-contract.md      | 115 ++++++++++++-
 docs/projects/bake-transformation/tracker.md       |   4 +-
 8 files changed, 322 insertions(+), 5 deletions(-)
```

Runtime/L3 validation was not attempted. No installed-device behavior is claimed
without fresh filtered `adb logcat`.

**Lesson proposals**: none.

**CHANGELOG line**: none.
