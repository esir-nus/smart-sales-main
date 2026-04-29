# Sprint Contract: 09-shell-routing-tcf

## 1. Header

- **Slug**: shell-routing-tcf
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Codex-authored from user-approved Sprint 09 plan
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Author the next BAKE transformation sprint after Sprint 08, using
the approved plan for a shell-routing TCF sprint.

**Codex interpretation**: This sprint converts Sprint 08's shell-routing
delivered-behavior findings into target core-flow alignment. It is a docs-only
TCF sprint: update the current shell-routing target docs, make delivered-vs-
target gaps checkable for the later BAKE implementation contract, and author the
follow-on shell-routing BAKE contract sprint only if the target-flow alignment is
stable enough by close.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/09-shell-routing-tcf.md`
- `docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `docs/cerb-ui/dynamic-island/spec.md`

Out of scope:

- code edits
- BAKE implementation contract verification or authority sync beyond authoring
  the next sprint contract
- Cerb demotion, archive moves, or interface-map BAKE authority changes
- device/runtime claims without fresh `adb logcat`
- Harmony-native or platform overlay changes
- broad UX rewrites unrelated to Sprint 08 shell-routing gaps

## 4. References

Governance and project protocol:

- `AGENTS.md`
- `docs/plans/tracker.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/specs/bake-protocol.md`
- `.agent/rules/lessons-learned.md`

Prior sprint evidence:

- `docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md`
- `docs/projects/bake-transformation/evidence/08-shell-routing-dbm/delivered-behavior-map.md`

Target docs and ownership references:

- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `docs/specs/base-runtime-unification.md`
- `docs/cerb-ui/home-shell/spec.md`
- `docs/cerb-ui/dynamic-island/spec.md`
- `docs/cerb/interface-map.md`

Delivered behavior evidence paths to keep traceable:

- `app-core/src/main/java/com/smartsales/prism/MainActivity.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinatorTest.kt`

## 5. Success Exit Criteria

1. Sprint 08 DBM gaps are reflected as explicit target-flow notes in the owning
   docs, covering first-launch handoff, dynamic-island Wi-Fi authority,
   canonical telemetry valves, smart-only blocking, drawer conflict runtime
   proof, and new-session test coverage.
   Verify:
   `rg -n "Sprint 08|first-launch|PARTIAL_WIFI_DOWN|Wi-Fi|SIM_ENTRY_STARTED|SMART_SURFACE_BLOCKED|Drawer Conflict|new-session" docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md`

2. The target-flow docs distinguish target behavior from delivered gaps and do
   not weaken core-flow authority to match current code by default.
   Verify:
   `rg -n "Target behavior|Delivered gap|BAKE gap|must not|remains the target" docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md`

3. If dynamic-island Wi-Fi takeover is intentionally allowed, the docs state
   the exact authority boundary and update the exclusion language consistently.
   If it remains disallowed, the delivered `PARTIAL_WIFI_DOWN` behavior is
   recorded as a BAKE gap.
   Verify:
   `rg -n "PARTIAL_WIFI_DOWN|Wi-Fi mismatch|manager-only refinement|BAKE gap" docs/core-flow/sim-shell-routing-flow.md docs/cerb-ui/dynamic-island/spec.md`

4. If target-flow alignment is stable enough, Sprint 10 exists as
   `docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md`
   and follows the ten-section sprint schema. If not, Sprint 09 closeout records
   why Sprint 10 was not authored.
   Verify:
   `test -f docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md || rg -n "Sprint 10 was not authored|not stable enough" docs/projects/bake-transformation/sprints/09-shell-routing-tcf.md`

5. Tracker row for Sprint 09 is updated to `done`, `stopped`, or `blocked` at
   close, and the `shell-routing` domain status advances only on successful
   target-flow sync.
   Verify:
   `rg -n "\| 09 \| shell-routing-tcf \| (done|stopped|blocked)|shell-routing \| base-runtime-active .* (tcf-written|dbm-written)" docs/projects/bake-transformation/tracker.md`

6. Static docs checks pass.
   Verify:
   `git diff --check -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/09-shell-routing-tcf.md docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md`

## 6. Stop Exit Criteria

- **Target authority blocker**: Sprint 08 gaps expose a product decision that
  cannot be resolved from existing core-flow, UX governance, or DBM evidence.
- **Scope blocker**: Correct target-flow alignment requires editing files outside
  the declared write scope.
- **Runtime evidence blocker**: A proposed target note depends on current device
  behavior that is not proven by fresh `adb logcat`; record it as an unknown or
  BAKE gap instead of claiming it.
- **BAKE contract blocker**: Sprint 10 cannot be authored because target-flow
  alignment remains internally inconsistent at the end of Sprint 09.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `rg -n "Sprint 08|first-launch|PARTIAL_WIFI_DOWN|SIM_ENTRY_STARTED|SMART_SURFACE_BLOCKED|Drawer Conflict|new-session" docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md`
2. `rg -n "Target behavior|Delivered gap|BAKE gap|must not|remains the target" docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md`
3. If Sprint 10 is authored:
   `rg -n "^## 1\. Header|^## 2\. Demand|^## 3\. Scope|^## 4\. References|^## 5\. Success Exit Criteria|^## 6\. Stop Exit Criteria|^## 7\. Iteration Bound|^## 8\. Required Evidence Format|^## 9\. Iteration Ledger|^## 10\. Closeout" docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md`
4. `rg -n "\| 09 \| shell-routing-tcf \| (done|stopped|blocked)|shell-routing \| base-runtime-active .* (tcf-written|dbm-written)" docs/projects/bake-transformation/tracker.md`
5. `git diff --check -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/09-shell-routing-tcf.md docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md`
6. `git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/09-shell-routing-tcf.md docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md`

## 9. Iteration Ledger

- **Iteration 1 — 2026-04-29**
  - Read the Sprint 09 contract, project tracker, sprint-contract schema,
    project-structure spec, BAKE protocol, lessons index, Sprint 08 DBM
    contract and delivered-behavior map, shell-routing core flow, UX governance
    flow, base-runtime unification spec, dynamic-island shard, and interface
    map.
  - Updated shell-routing target docs to resolve Sprint 08 gaps without
    accepting delivered drift as target behavior: first-launch now returns to
    normal shell use after forced onboarding, `PARTIAL_WIFI_DOWN` remains
    excluded from island takeover, canonical valve gaps remain explicit, smart
    surface blocking now requires `SMART_SURFACE_BLOCKED`, and Drawer Conflict
    plus new-session follow-up-clear coverage are called out for BAKE.
  - Authored
    `docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md`
    because target-flow alignment was internally consistent.
  - Updated the project tracker: Sprint 09 is `done`, Sprint 10 is `authored`,
    and the `shell-routing` domain status is `tcf-written`.
  - Evaluator result: static target-term, target-vs-gap, Sprint 10 schema,
    tracker, and `git diff --check` verification passed. No runtime/L3 claims
    were made.

## 10. Closeout

**Status**: success

**Tracker summary**: Aligned shell-routing target core flows from Sprint 08 DBM
gaps and authored the shell-routing BAKE contract sprint.

**Changed files**:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/09-shell-routing-tcf.md`
- `docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `docs/cerb-ui/dynamic-island/spec.md`

**Evidence command outputs**:

```text
$ rg -n "Sprint 08|first-launch|PARTIAL_WIFI_DOWN|SIM_ENTRY_STARTED|SMART_SURFACE_BLOCKED|Drawer Conflict|new-session" docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md
docs/cerb-ui/dynamic-island/spec.md:60:Sprint 08 alignment note:
docs/cerb-ui/dynamic-island/spec.md:63:- `PARTIAL_WIFI_DOWN`, Wi-Fi mismatch, update, and manager-only diagnostic states remain excluded from target island takeover.
docs/cerb-ui/dynamic-island/spec.md:64:- Delivered gap: Sprint 08 found current delivered code renders `PARTIAL_WIFI_DOWN` as a persistent takeover. That is a BAKE gap for the shell-routing implementation contract, not a new target rule.
docs/core-flow/sim-shell-routing-flow.md:28:Sprint 08 alignment note:
docs/core-flow/sim-shell-routing-flow.md:31:- Delivered gap: Sprint 08 found first-launch code returns to home after forced onboarding, with a shell-owned scheduler handoff gate possibly auto-opening the real scheduler drawer; this matches `docs/core-flow/base-runtime-ux-surface-governance-flow.md`.
docs/core-flow/sim-shell-routing-flow.md:32:- BAKE gap: Sprint 08 found delivered `PARTIAL_WIFI_DOWN` persistent dynamic-island takeover, but Wi-Fi mismatch and manager-only diagnostic states remain excluded from the target island takeover rules.
docs/core-flow/sim-shell-routing-flow.md:33:- Delivered gap: Sprint 08 found current telemetry does not prove the canonical valves `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, drawer-open/close valves, or `SMART_SURFACE_BLOCKED`.
docs/core-flow/sim-shell-routing-flow.md:34:- Test target: the BAKE contract must require direct Drawer Conflict proof and new-session follow-up-clear coverage; static source checks alone are not sufficient runtime proof.
docs/core-flow/sim-shell-routing-flow.md:117:Target behavior: the island takeover list above remains the target. Delivered gap: Sprint 08 found `PARTIAL_WIFI_DOWN` currently renders as a persistent connectivity takeover in delivered code. That is a BAKE gap to repair or justify in the implementation sprint, not permission to widen the target island authority.
docs/core-flow/sim-shell-routing-flow.md:125:Delivered gap from Sprint 08: current static evidence did not prove these canonical valves by name for `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, drawer open/close coverage, or `SMART_SURFACE_BLOCKED`; convergence remains the target.
docs/core-flow/sim-shell-routing-flow.md:215:- the block must emit or record the `SMART_SURFACE_BLOCKED` checkpoint so the attempted route is auditable
docs/core-flow/sim-shell-routing-flow.md:217:### Branch-S2: Drawer Conflict
docs/core-flow/sim-shell-routing-flow.md:223:- the BAKE contract must require direct Drawer Conflict proof rather than relying only on reducer source shape
docs/core-flow/sim-shell-routing-flow.md:267:- new-session behavior clears active badge scheduler follow-up prompts when appropriate and remains covered by executable tests
docs/core-flow/base-runtime-ux-surface-governance-flow.md:96:Sprint 08 shell-routing target note:
docs/core-flow/base-runtime-ux-surface-governance-flow.md:99:- Dynamic-island connectivity takeover remains limited to approved transport-truth shell states; `PARTIAL_WIFI_DOWN`, Wi-Fi mismatch, update, and manager-only diagnostic refinements must not take over the island unless this UX authority is deliberately revised.
docs/core-flow/base-runtime-ux-surface-governance-flow.md:100:- Drawer exclusivity remains a one-active-surface UX invariant. Delivered gaps from Sprint 08, including incomplete Drawer Conflict runtime proof, are BAKE gaps rather than weakened UX rules.
docs/core-flow/base-runtime-ux-surface-governance-flow.md:101:- The smart-only route block remains the target: attempted smart-only surfaces must leave the user on a valid shell surface and record `SMART_SURFACE_BLOCKED`.
```

```text
$ rg -n "Target behavior|Delivered gap|BAKE gap|must not|remains the target" docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md
docs/cerb-ui/dynamic-island/spec.md:62:- Target behavior remains the transport-truth connectivity lane defined here.
docs/cerb-ui/dynamic-island/spec.md:64:- Delivered gap: Sprint 08 found current delivered code renders `PARTIAL_WIFI_DOWN` as a persistent takeover. That is a BAKE gap for the shell-routing implementation contract, not a new target rule.
docs/core-flow/sim-shell-routing-flow.md:30:- Target behavior remains governed by this flow and the UX governance flow, not by delivered code drift.
docs/core-flow/sim-shell-routing-flow.md:31:- Delivered gap: Sprint 08 found first-launch code returns to home after forced onboarding, with a shell-owned scheduler handoff gate possibly auto-opening the real scheduler drawer; this matches `docs/core-flow/base-runtime-ux-surface-governance-flow.md`.
docs/core-flow/sim-shell-routing-flow.md:32:- BAKE gap: Sprint 08 found delivered `PARTIAL_WIFI_DOWN` persistent dynamic-island takeover, but Wi-Fi mismatch and manager-only diagnostic states remain excluded from the target island takeover rules.
docs/core-flow/sim-shell-routing-flow.md:33:- Delivered gap: Sprint 08 found current telemetry does not prove the canonical valves `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, drawer-open/close valves, or `SMART_SURFACE_BLOCKED`.
docs/core-flow/sim-shell-routing-flow.md:117:Target behavior: the island takeover list above remains the target. Delivered gap: Sprint 08 found `PARTIAL_WIFI_DOWN` currently renders as a persistent connectivity takeover in delivered code. That is a BAKE gap to repair or justify in the implementation sprint, not permission to widen the target island authority.
docs/core-flow/sim-shell-routing-flow.md:125:Delivered gap from Sprint 08: current static evidence did not prove these canonical valves by name for `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, drawer open/close coverage, or `SMART_SURFACE_BLOCKED`; convergence remains the target.
docs/core-flow/base-runtime-ux-surface-governance-flow.md:98:- Target behavior: onboarding completion returns to normal shell use, not a mandatory connectivity-manager stop before the user can reach home or discussion.
docs/core-flow/base-runtime-ux-surface-governance-flow.md:100:- Drawer exclusivity remains a one-active-surface UX invariant. Delivered gaps from Sprint 08, including incomplete Drawer Conflict runtime proof, are BAKE gaps rather than weakened UX rules.
```

```text
$ rg -n "^## 1\. Header|^## 2\. Demand|^## 3\. Scope|^## 4\. References|^## 5\. Success Exit Criteria|^## 6\. Stop Exit Criteria|^## 7\. Iteration Bound|^## 8\. Required Evidence Format|^## 9\. Iteration Ledger|^## 10\. Closeout" docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md
3:## 1. Header
14:## 2. Demand
25:## 3. Scope
48:## 4. References
78:## 5. Success Exit Criteria
132:## 6. Stop Exit Criteria
147:## 7. Iteration Bound
151:## 8. Required Evidence Format
166:## 9. Iteration Ledger
170:## 10. Closeout
```

```text
$ rg -n "\| 09 \| shell-routing-tcf \| (done|stopped|blocked)|shell-routing \| base-runtime-active .* (tcf-written|dbm-written)" docs/projects/bake-transformation/tracker.md
33:| 09 | shell-routing-tcf | done | Aligned shell-routing target core flows from Sprint 08 DBM gaps and authored the shell-routing BAKE contract sprint. | [sprints/09-shell-routing-tcf.md](sprints/09-shell-routing-tcf.md) |
43:| shell-routing | base-runtime-active | `docs/core-flow/sim-shell-routing-flow.md` | high | tier-1 | tcf-written |
```

```text
$ git diff --check -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/09-shell-routing-tcf.md docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md
<no output>
```

```text
$ git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/09-shell-routing-tcf.md docs/projects/bake-transformation/sprints/10-shell-routing-bake-contract.md docs/core-flow/sim-shell-routing-flow.md docs/core-flow/base-runtime-ux-surface-governance-flow.md docs/cerb-ui/dynamic-island/spec.md
 docs/cerb-ui/dynamic-island/spec.md                |   8 +-
 .../base-runtime-ux-surface-governance-flow.md     |   7 +
 docs/core-flow/sim-shell-routing-flow.md           |  21 ++-
 .../sprints/09-shell-routing-tcf.md                | 113 +++++++++++++-
 .../sprints/10-shell-routing-bake-contract.md      | 172 +++++++++++++++++++++
 docs/projects/bake-transformation/tracker.md       |   5 +-
 6 files changed, 318 insertions(+), 8 deletions(-)
```

**Lesson proposals**: none.

**CHANGELOG line**: none.
