# Sprint Contract: 03-connectivity-tcf

## 1. Header

- **Slug**: connectivity-tcf
- **Project**: bake-transformation
- **Date authored**: 2026-04-28
- **Author**: Claude-authored contract lineage; Codex authored from sprint 01 triage close
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Convert the cluster-1 delivered-behavior map into the optimized
target core-flow direction and author the first BAKE contract sprint.

**Claude/Codex interpretation**: This sprint consumes sprint 02's delivered map,
then updates only the touched cluster-1 core-flow docs so the target behavior is
explicit before a BAKE implementation contract is written. It does not edit code
and does not write the BAKE contract itself.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/03-connectivity-tcf.md`
- `docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/core-flow/badge-session-lifecycle.md`

No files outside this list may be written.

## 4. References

- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/core-flow/badge-session-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/interface-map.md`

## 5. Success Exit Criteria

1. Both touched core-flow docs include `scope: base-runtime-active`
   frontmatter or an equivalent existing metadata field.
   Verify: `rg -n "scope: base-runtime-active" docs/core-flow/badge-connectivity-lifecycle.md docs/core-flow/badge-session-lifecycle.md`

2. Target-flow edits explicitly distinguish current delivered behavior from
   target behavior where sprint 02 found gaps.
   Verify: `rg -n "Delivered behavior|Target behavior|Gap" docs/core-flow/badge-connectivity-lifecycle.md docs/core-flow/badge-session-lifecycle.md`

3. Sprint 04 contract exists and passes the 10-section harness schema check
   with sections 1-8 filled and sections 9-10 left for the operator.
   Verify: `wc -l docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md`

4. Tracker row for sprint 03 is updated to `done` or `blocked`, and sprint 04
   is added as `authored` if created.
   Verify: `rg -n "\| 03 \| connectivity-tcf \| (done|blocked)|\| 04 \| connectivity-bake-contract \| authored" docs/projects/bake-transformation/tracker.md`

## 6. Stop Exit Criteria

- **Missing input**: Sprint 02 did not close successfully or the delivered map
  is absent.
- **Product decision blocker**: The delivered map exposes a target behavior
  choice that cannot be resolved from existing core-flow docs.
- **Scope creep**: Any pull toward code edits, Cerb archival, interface-map
  updates, or final BAKE contract writing. Those belong to sprint 04 or later.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `git diff --stat -- docs/core-flow/badge-connectivity-lifecycle.md docs/core-flow/badge-session-lifecycle.md docs/projects/bake-transformation/`
2. `rg -n "scope: base-runtime-active|Delivered behavior|Target behavior|Gap" docs/core-flow/badge-connectivity-lifecycle.md docs/core-flow/badge-session-lifecycle.md`
3. `wc -l docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md`

## 9. Iteration Ledger

## 10. Closeout

