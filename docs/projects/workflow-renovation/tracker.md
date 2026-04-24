# Project: workflow-renovation

## Objective

Replace per-task feature-branch / worktree isolation with a sprint-contract workflow. Every task runs inside one self-contained contract; projects own holistic scope (code + docs + UI together); context resets at sprint/project boundaries.

Authoritative plan: `/home/cslh-frank/.claude/plans/i-suddenly-realized-that-serene-lark.md`
Baseline: tag `pre-renovation-baseline` at `origin/develop` SHA `c9d21be1b`.

## Status

open - sprint 05 executed

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 05 | cleanup | done | Removed the task-route-gate worktree/branch and deleted `docs/plans/active-lanes.md`; widened the residue sweep cleaned the remaining doc references and opened the sprint PR | [05-cleanup.md](sprints/05-cleanup.md) |
| 06 | branch-graveyard-sweep | planned | Harvest Harmony commits onto `platform/harmony`, archive-tag + delete the remaining branch graveyard, then close the project | - |

## Cross-Sprint Decisions

- Sprint 05 widened with explicit user approval after fresh `origin/develop` still exposed pre-existing `active-lanes` references outside the original narrow sweep.
- Sprint 06 stays separate: its contract and PR will be cut from fresh `origin/develop`, not from the blocked sprint 05 branch.
- `.codex/skills/ship/SKILL.md` remains excluded from sprint 05; sprint 03 still owns those references.

## Lessons Pointer

- Relevant governance trigger during sprint 05: *Rescue Branch Lane Split and Post-Merge Cleanup* in `docs/reference/agent-lessons-details.md`.
- Any new lesson proposal waits until sprint 06 closes the project.
