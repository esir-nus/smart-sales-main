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

Operator fills this section during Sprint 09 operation.

## 10. Closeout

Operator fills this section at Sprint 09 exit.
