# Project: workflow-renovation

## Objective

Replace per-task feature-branch / worktree isolation with a sprint-contract workflow. Every task runs inside one self-contained contract; projects own holistic scope (code + docs + UI together); context resets at sprint/project boundaries.

Authoritative plan: `/home/cslh-frank/.claude/plans/i-suddenly-realized-that-serene-lark.md`
Baseline: tag `pre-renovation-baseline` at `origin/develop` SHA `c9d21be1b`.

## Status

closed - sprint 06 executed 2026-04-24

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 05 | cleanup | done | Removed the task-route-gate worktree/branch and deleted `docs/plans/active-lanes.md`; widened the residue sweep cleaned the remaining doc references and opened the sprint PR | [05-cleanup.md](sprints/05-cleanup.md) |
| 06 | branch-graveyard-sweep | done | Harvested the Harmony-native app commits onto `platform/harmony` via PR #30, archive-tagged the graveyard, and deleted every bucket A/B branch while leaving sprint 04/05 deferred refs untouched | [06-branch-graveyard-sweep.md](sprints/06-branch-graveyard-sweep.md) |

## Cross-Sprint Decisions

- Sprint 05 widened with explicit user approval after fresh `origin/develop` still exposed pre-existing `active-lanes` references outside the original narrow sweep.
- Sprint 06 rebased on top of the merged sprint 05 state before execution.
- Archive tags are the retained reference surface for deleted branches; `parking/develop-diverged-audio-drawer` remains out of scope.
- `.codex/skills/ship/SKILL.md` remains excluded from sprint 05; sprint 03 still owns those references.
- Sprint 04/05-owned branches stay deferred until their owning sprint closes.
- Mixed Android/docs deltas found inside Harmony source branches were excluded from the Harmony harvest; only `platforms/harmony/smartsales-app/**` changes landed on `platform/harmony`.

## Lessons Pointer

- Relevant governance trigger during sprint 05: *Rescue Branch Lane Split and Post-Merge Cleanup* in `docs/reference/agent-lessons-details.md`.
- Relevant governance triggers for sprint 06: *Harmony Sprint Lane Misrouting* and *Lane Contamination Across Android and Harmony* in `docs/reference/agent-lessons-details.md`.
- No new lesson proposal added at project close; the existing lane-contamination and Harmony-misrouting lessons were sufficient for this run.
