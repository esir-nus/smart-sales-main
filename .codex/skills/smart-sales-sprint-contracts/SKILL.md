---
name: smart-sales-sprint-contracts
description: Author, plan, review, or repair Smart Sales sprint contracts under `docs/projects/**/sprints/`, including Sprint 06, BAKE DBM/TCF/BAKE-contract sequencing, tracker row updates, scope/evidence quality, and closeout evidence expectations.
---

# Smart Sales Sprint Contracts

Use this skill when creating, reviewing, or repairing sprint contracts in `docs/projects/**/sprints/`. It is narrower than feature delivery or Cerb scaffolding: the output is a bounded sprint contract and matching tracker state, not implementation.

## Load Order

Read the minimum authoritative context in this order:

1. `AGENTS.md`
2. `docs/plans/tracker.md`
3. The owning project tracker at `docs/projects/<slug>/tracker.md`
4. The prior sprint contract in the same project, if one exists
5. `docs/specs/sprint-contract.md`
6. `docs/specs/project-structure.md`
7. Project northstar docs such as `docs/specs/harness-manifesto.md` and `docs/specs/bake-protocol.md`
8. Relevant domain docs: `docs/core-flow/**`, `docs/cerb/**`, `docs/specs/**`, `docs/sops/**`, and `docs/cerb/interface-map.md` as needed

For Sprint 06 or later in a project, verify whether the project tracker needs the six-sprint ongoing-justification from `docs/specs/project-structure.md`.

## Authoring Rules

- Fill sections 1-8 at authoring time.
- Leave sections 9-10 for the operator. Do not pre-fill the Iteration Ledger or Closeout except with explicit placeholders that say the operator fills them.
- Keep each contract self-contained. References must be explicit repo paths, not prior conversation.
- Keep the scope small enough for a single sprint. A contract over about 200 lines is a split signal.
- Match the declared lane: `develop` for Android/shared governance, `platform/harmony` for Harmony integration.
- Update the owning project tracker row when a sprint is added, planned, authored, stopped, blocked, or done.
- Do not create extra docs when the information belongs in the contract, tracker row, or eventual commit message.

## Contract Quality Checklist

Every sprint contract must have the ten sections from `docs/specs/sprint-contract.md` in order:

1. Header
2. Demand
3. Scope
4. References
5. Success Exit Criteria
6. Stop Exit Criteria
7. Iteration Bound
8. Required Evidence Format
9. Iteration Ledger
10. Closeout

Sections 1-8 must be checkable before handoff. Success criteria must name commands, paths, grep patterns, tests, evidence artifacts, or visible outputs. Stop criteria must include the shape of blockers and the iteration bound.

## BAKE Sequencing

For BAKE transformation projects, preserve this usual sequence unless the user explicitly chooses a different path:

- After a domain DBM closes, the next sprint is usually a target core-flow or TCF authoring sprint.
- After TCF closes, the next sprint is usually the BAKE implementation contract for that domain.
- The BAKE implementation contract must name the pipeline contract, telemetry joints, error paths, test contract, cross-platform notes, and any Core-Flow gap.
- Treat `docs/specs/bake-protocol.md` as the project northstar for what a verified backend contract should contain, while `docs/specs/harness-manifesto.md` governs planning, evidence, and closeout.

## Verification Policy

Choose verification by sprint impact, not by habit.

### Docs-only authoring sprints

Use static verification. This applies to contracts like a Sprint 06 TCF authoring sprint when the sprint only creates or edits governance/spec text and does not change runtime behavior.

Require evidence such as:

- Schema check against sections 1-10
- `rg` checks for required terms, paths, section headers, tracker rows, and stale placeholders
- Line count or size check for contract discipline
- `git diff --check`
- Tracker row check in `docs/projects/<slug>/tracker.md` and, when needed, `docs/plans/tracker.md`

Do not require `adb` or device access for docs-only authoring contracts unless the contract itself depends on runtime facts.

### Runtime, UI, or device-affecting sprints

The sprint contract must require an on-device iteration loop. Unit tests and screenshots can support the result, but they are not sufficient runtime proof.

Required contract content:

- Expected telemetry joints, including `VALVE_PROTOCOL` checkpoints when the path uses pipeline telemetry
- Exact Android `Log.d` tags when known; otherwise a criterion to discover or add targeted tags before claiming root cause
- At least one negative case such as missing input, empty data, invalid state, offline path, duplicate trigger, cache miss, or permission denial
- A bounded rerun/fix loop tied to section 7's iteration bound
- Android runtime evidence using `adb logcat`; screenshots and user reports are supporting evidence only
- If working on Harmony, use the lane's `hdc`/hilog equivalent and keep Android `adb` requirements out of Harmony-only contracts

Good runtime evidence names the command and expected proof shape, for example:

```bash
adb logcat -c
adb logcat -s VALVE_PROTOCOL:I BadgeSession:D SchedulerPathA:D | head -200
```

The contract should require the operator to paste or attach filtered log excerpts proving each declared joint and negative case.

## Review Prompts

When reviewing a proposed sprint contract, check:

- Does the task belong in one sprint, and is the scope explicit?
- Are sections 1-8 authored and sections 9-10 left to the operator?
- Does the tracker row match the sprint number, slug, status, summary, and contract path?
- Does the evidence class match the work: static for docs-only, on-device loop for runtime/device work?
- For BAKE work, does the sprint follow DBM -> TCF -> BAKE-contract sequencing unless an exception is declared?
- Are stop criteria concrete enough to prevent grinding past missing docs, unavailable devices, out-of-scope regressions, or iteration-bound failure?

## Anti-Patterns

- Pre-filling the operator's Iteration Ledger or Closeout during authoring
- Treating screenshots or user recollection as root-cause proof for Android runtime behavior
- Requiring `adb logcat` for a docs-only Sprint 06 authoring task with no runtime dependency
- Writing a BAKE implementation contract immediately after DBM when the target core-flow/TCF layer is still missing
- Creating a new protocol doc instead of keeping sprint-specific planning inside the contract

## Source References

Read `references/source-map.md` when you need the exact repo files this skill adapts.
