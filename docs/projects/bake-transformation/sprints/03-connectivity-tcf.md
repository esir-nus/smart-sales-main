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

- Iteration 1 — Read the sprint contract, repo rules, BAKE protocol,
  sprint/project structure specs, lessons index/details for relevant
  connectivity triggers, Sprint 02 delivered-behavior map, both cluster-1
  core-flow docs, connectivity Cerb spec, and interface map. Added
  `scope: base-runtime-active` metadata to both core-flow docs, inserted
  delivered-vs-target gap alignment notes from Sprint 02, authored Sprint 04,
  and updated the project tracker. Verification passed; new sprint files are
  ignored by `.gitignore`, so Sprint 04 must be force-added on commit.

## 10. Closeout

- **Status**: success
- **Tracker summary**: Added base-runtime scope metadata and delivered-vs-target
  gap notes to connectivity/session core flows, then authored the first BAKE
  contract sprint.
- **Files changed**:
  - `docs/core-flow/badge-connectivity-lifecycle.md`
  - `docs/core-flow/badge-session-lifecycle.md`
  - `docs/projects/bake-transformation/tracker.md`
  - `docs/projects/bake-transformation/sprints/03-connectivity-tcf.md`
  - `docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md`
- **Evidence**:

```text
$ git diff --stat -- docs/core-flow/badge-connectivity-lifecycle.md docs/core-flow/badge-session-lifecycle.md docs/projects/bake-transformation/
 docs/core-flow/badge-connectivity-lifecycle.md     | 27 ++++++++++
 docs/core-flow/badge-session-lifecycle.md          | 37 ++++++++++----
 .../sprints/03-connectivity-tcf.md                 | 57 ++++++++++++++++++++++
 docs/projects/bake-transformation/tracker.md       |  3 +-
 4 files changed, 114 insertions(+), 10 deletions(-)

$ rg -n "scope: base-runtime-active|Delivered behavior|Target behavior|Gap" docs/core-flow/badge-connectivity-lifecycle.md docs/core-flow/badge-session-lifecycle.md
docs/core-flow/badge-session-lifecycle.md:2:scope: base-runtime-active
docs/core-flow/badge-session-lifecycle.md:135:- Delivered behavior: the registry remains audio-agnostic, and audio observes `DeviceRegistryManager.activeDevice` to cancel queued and active manual badge downloads when the active MAC changes.
docs/core-flow/badge-session-lifecycle.md:136:- Gap: `rec#` auto-download is fenced after download success, but its own launched job is not explicitly cancelled by the manual badge-download queue cancellation path.
docs/core-flow/badge-session-lifecycle.md:157:Delivered behavior:
docs/core-flow/badge-session-lifecycle.md:167:Target behavior:
docs/core-flow/badge-session-lifecycle.md:174:Gap:
docs/core-flow/badge-session-lifecycle.md:216:Delivered behavior: Sprint 02 implementation now satisfies the minimal manual-sync cancellation/fencing rule for queued and active badge downloads. Target behavior keeps the stronger per-item MAC ownership rule open for the BAKE contract and follow-up implementation.
docs/core-flow/badge-connectivity-lifecycle.md:2:scope: base-runtime-active
docs/core-flow/badge-connectivity-lifecycle.md:54:Delivered behavior:
docs/core-flow/badge-connectivity-lifecycle.md:61:Target behavior:
docs/core-flow/badge-connectivity-lifecycle.md:68:Gap:

$ wc -l docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md
108 docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md

$ rg -n "\| 03 \| connectivity-tcf \| (done|blocked)|\| 04 \| connectivity-bake-contract \| authored" docs/projects/bake-transformation/tracker.md
27:| 03 | connectivity-tcf | done | Added base-runtime scope metadata and delivered-vs-target gap notes to connectivity/session core flows, then authored the first BAKE contract sprint. | [sprints/03-connectivity-tcf.md](sprints/03-connectivity-tcf.md) |
28:| 04 | connectivity-bake-contract | authored | Write the cluster-1 BAKE implementation contract and sync supporting connectivity discovery docs. | [sprints/04-connectivity-bake-contract.md](sprints/04-connectivity-bake-contract.md) |
```

- **Notes**: `docs/projects/**/sprints/*.md` is ignored by `.gitignore`, so
  `docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md`
  requires `git add -f` during the success commit.
- **Hardware evidence**: not applicable; this sprint was docs-only and made no
  runtime claims.
- **Lesson proposals**: none.
- **CHANGELOG line**: none; internal BAKE transformation docs only.
