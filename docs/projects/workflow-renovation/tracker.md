# Project: workflow-renovation

## Objective

Replace per-task feature-branch / worktree isolation with a sprint-contract workflow. Every task runs inside one self-contained contract; projects own holistic scope (code + docs + UI together); context resets at sprint/project boundaries.

Authoritative plan: `/home/cslh-frank/.claude/plans/i-suddenly-realized-that-serene-lark.md`
Baseline: tag `pre-renovation-baseline` at `origin/develop` SHA `c9d21be1b`.

## Status

open — authored 2026-04-23

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | specs-bootstrap | done | Wrote sprint-contract.md (58 lines) + project-structure.md (81 lines); both under the 200-line ceiling; schema self-applies | [01-specs-bootstrap.md](sprints/01-specs-bootstrap.md) |
| 02 | governance-shrink | planned | Shrink `declaration-first-shipping.md` → `ship-time-checks.md`; rewrite CLAUDE.md + AGENTS.md branch/declaration sections | — |
| 03 | skills-align | planned | Update `/sprint`, `/ship`, `/merge`, `/changelog` skills to contract model | — |
| 04 | trackers-migrate | planned | Move `god-tracker`, `harmony-tracker`, `ui-tracker`, `bug-tracker`, wave briefs into `docs/projects/<slug>/`; migrate DTQ-03 carry-over; shrink `docs/plans/tracker.md` to project index | — |
| 05 | cleanup | planned | Delete `.worktrees/task-route-gate/`, `feat/task-route-gate` branch, `docs/plans/active-lanes.md`; grep-sweep residue | — |

Decomposition guideline: projects that don't close in ~6 sprints should decompose or justify staying open. This project is budgeted at 5.

## Cross-Sprint Decisions

- **Bootstrap scope:** sprint 01 produces the two spec files and nothing else. The scaffold (this tracker + the sprint 01 contract file) is Claude's authoring output; operator ships everything in one commit on sprint close.
- **Operator default:** Codex. User may override per sprint at handoff. **Sprint 01 override: Claude** (docs work, author-executes-own-work while context is loaded).
- **Ship cadence during renovation:** one PR per sprint while `/merge` and `/ship` still run under old rules (sprints 01 and 02). Direct-to-develop becomes the default only after sprint 03 rewires the skills.
- **Deferred:** DTQ-03 carry-over migration lives in sprint 04, not sprint 01. Soft gates (CHANGELOG rescue catch-up, dirty main-tree classification) stay deferred.

## Inputs Pending for Later Sprints

- **Sprint 02 (governance-shrink) — branch-model rule:** `develop` is the canonical source for adb-packaged Android installation. On-device builds and APK artifacts come from `develop`, not from feature/parking branches. Land in CLAUDE.md's Branch Model section. Parallel to resolve at authoring time: is the Harmony equivalent "hdc installation built from `platform/harmony`"? If yes, state both rules symmetrically.
- **Sprint 02 (governance-shrink) — doc-production rule:** Agents must not auto-proliferate doc artifacts. Ask before producing a non-code artifact; prefer commit messages over new files when the information fits. Land in CLAUDE.md + AGENTS.md.

## Lessons Pointer

- Recent additions to `.agent/rules/lessons-learned.md` from the pre-renovation closure: *Hermetic ASR Unit Tests*, *Rescue Branch Lane Split and Post-Merge Cleanup*.
- This project's own lessons (if any) will be proposed at project close, after sprint 05.
