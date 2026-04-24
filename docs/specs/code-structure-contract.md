# Code Structure Contract

> **Status:** Active Structural Policy
> **Last Updated:** 2026-03-24
> **Companion Tracker:** [`../plans/god-tracker.md`](../plans/god-tracker.md)
> **Related UI Policy:** [`prism-ui-ux-contract.md`](./prism-ui-ux-contract.md)

---

## Working Role

This document is the repo-level policy for **code shape**.

It does not replace:

- feature behavior specs in `docs/cerb/**` or `docs/cerb-ui/**`
- UI workflow docs in `docs/sops/ui-dev-mode.md` and `docs/sops/ui-building.md`
- architecture/module ownership in `docs/cerb/interface-map.md`

Instead, it answers a narrower question:

- where should responsibility live inside code files?
- what file shapes are allowed?
- what responsibility mixes are forbidden?
- when is a large file a tracked exception instead of accepted structure?

Simple law:

- Cerb owns feature truth
- UI SOP owns UI workflow
- this document owns code-shape legality

---

## Why This Contract Exists

The repo now has a battle-tested UI workflow and successful prototype-to-Kotlin transplant history.

The next bottleneck is structural:

- some shared UI trunk files are too large and too mixed
- some ViewModels and service classes own too many behavior lanes at once
- transplant targets are becoming hard for humans and agents to understand quickly
- code comprehension cost is now slowing safe delivery

This contract exists to stop “god files” from becoming the default landing zone for future work.

---

## Scope Boundaries

This contract covers:

- Kotlin file role clarity
- canonical decomposition shapes
- responsibility budgets
- forbidden responsibility mixes
- exception policy
- structural enforcement expectations

This contract does **not** cover:

- feature behavior
- UI copy/literals
- product decisions
- architecture-layer ownership already defined elsewhere

Rule:

- do not duplicate full feature behavior here
- link to Cerb or core-flow docs when behavior matters

---

## Governance Split

Use the three layers together:

1. **UI SOP stack**
   - prototype-first UI workflow
   - screenshot critique
   - approval and transplant method

2. **Cerb / cerb-ui**
   - per-feature UI truth
   - literals, invariants, and surface ownership

3. **Code Structure Contract**
   - file role legality
   - decomposition law
   - anti-god-file guardrails

Rule:

- do not remove UI from Cerb entirely
- do not use Cerb as the main file-shape enforcement layer
- do not use this contract as a feature-spec replacement

---

## Primary Role Rule

Every non-trivial Kotlin file should have **one primary role**.

Allowed primary roles include:

- screen host
- content/composition file
- section file
- component file
- preview file
- projection/mapper file
- ViewModel entrypoint
- coordinator
- reducer
- effects/support file
- repository/service
- parser/linter
- state machine / transport gateway

Rule:

- one file may assist one neighboring role
- one file must not act as the default container for every nearby responsibility

---

## Canonical Shapes

### UI Surface Shape

For major UI surfaces, prefer:

- `XxxScreen.kt`
  - host only
  - state collection
  - top-level side effects
  - callback wiring
- `XxxContent.kt`
  - main layout composition
- `XxxSections.kt`
  - larger local sections
- `XxxComponents.kt`
  - reusable local render pieces
- `XxxPreview.kt`
  - previews only
- `XxxProjection.kt`
  - presentation helpers when needed

### Complex ViewModel Shape

For large ViewModels, prefer:

- `XxxViewModel.kt`
  - public seam only
  - exposed state
  - event entrypoints
- `XxxCoordinator.kt`
  - orchestration across sub-lanes
- `XxxReducer.kt`
  - pure state transitions where useful
- `XxxProjection.kt`
  - derived UI-facing projections
- `XxxEffects.kt` or support files
  - heavy side-effect or lane-specific logic

### Service / Parser / Gateway Shape

For large logic-heavy non-UI files, prefer separating:

- transport/state machine
- parser/normalizer
- policy/validation
- telemetry/reporting
- repository persistence or storage concerns

Rule:

- if the split creates files with no stable role, the split is wrong
- optimize for discoverability, not arbitrary fragmentation

---

## Transitional Wave 1 Budgets

These budgets are the current transitional caps for the active cleanup wave.

They are intentionally not final forever numbers.

| Role | Budget |
|------|--------|
| UI host / shell | 550 LOC |
| UI sections / components | 350 LOC |
| ViewModel / coordinator | 650 LOC |
| service / manager / linter / gateway | 650 LOC |

Rule:

- budget alone does not define legality
- a file can be under budget and still be structurally wrong if it mixes forbidden roles

---

## Forbidden Mixes

The following mixes are explicitly disallowed unless tracked as a temporary exception:

1. **host screen + component library + previews**
2. **shell host + reducers + telemetry + projections**
3. **ViewModel + multiple heavy feature lanes in one file**
4. **service + parser + state machine + retry/telemetry policy**

Interpretation:

- the file does not need to literally contain every possible bad element to be problematic
- if the file is clearly functioning as a kitchen sink for one of these clusters, it should be treated as a violation

---

## Anti-Overfragmentation Rule

Do not split a file only to satisfy a number.

Create a new file only when it owns:

- a stable role, or
- a proven responsibility cluster

Bad split examples:

- tiny helper files with no durable ownership
- moving a few functions out while keeping the same god-file mental model
- renaming a god file into multiple vague files that still require reading all of them to find anything

Good split outcome:

- a new engineer or agent can quickly tell where to land a change
- the host file becomes readable top-down
- future prototype transplant targets become obvious

---

## Exception Policy

If a file is above budget or violates a forbidden mix, it must appear in:

- `docs/projects/god-file-cleanup/tracker.md`

Required exception fields:

- path
- primary role
- current size
- reason
- owner
- target decomposition
- sunset wave/date
- required tests
- status

Rules:

- no sunset = invalid exception
- no required tests = invalid exception
- no owner = invalid exception
- exceptions are temporary, not an alternate steady state

---

## Structural Enforcement

Structural checks should fail when:

- a guarded file exceeds its budget and has no valid tracker exception
- an exception has passed its sunset
- a guarded file violates a named forbidden mix
- a guarded UI host still contains previews after the migration rule applies

Initial enforcement should stay shallow and named:

- named-file budget checks
- preview separation checks
- named forbidden-mix checks
- tracker exception checks

Rule:

- do not start with an overly smart universal analyzer if a small named-file checker will catch the real regressions

---

## Refactor Timing Rule

When an active cleanup wave targets shared trunk files:

- major new trunk transplants should pause for those files
- unrelated leaf-level UI polish may continue only if it avoids the targeted files

This rule exists because structural cleanup is meant to improve future delivery speed, not compete with the same files at the same time.

---

## Current Active Pilot

The active pilot is the SIM / agent shared trunk cleanup tracked in:

- `docs/projects/god-file-cleanup/tracker.md`

Immediate pilot files:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt`

Future budgets and role definitions may be tightened after that pilot proves the pattern.

---

## Related Documents

- `docs/projects/god-file-cleanup/tracker.md`
- `docs/plans/tracker.md`
- `docs/projects/ui-campaign/tracker.md`
- `docs/sops/ui-dev-mode.md`
- `docs/sops/ui-building.md`
- `docs/specs/prism-ui-ux-contract.md`
- `docs/cerb/interface-map.md`
