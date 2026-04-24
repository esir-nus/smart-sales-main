# Project: develop-protocol-migration

## Objective

Deliver the end state: clean remote repo (no dead branches, no orphan PRs), clean local repo (no stale worktrees/branches/tags), `develop` contains all latest useful updates, and full compliance with the new development model (sprint-contract workflow, two-trunk split, index+details docs, doc-production discipline). Replace per-task feature-branch / worktree isolation with a sprint-contract workflow. Every task runs inside one self-contained contract; projects own holistic scope (code + docs + UI together); context resets at sprint/project boundaries.

Project was renamed from `workflow-renovation` on 2026-04-23 after the user clarified that "renovation" under-described the scope — the real goal is a fully clean + model-compliant repo, not just governance updates.

Authoritative plan: `/home/cslh-frank/.claude/plans/i-suddenly-realized-that-serene-lark.md`
Baseline: tag `pre-renovation-baseline` at `origin/develop` SHA `c9d21be1b`.

## Status

open — authored 2026-04-23 (renamed from workflow-renovation same day); rebudgeted to 8 sprints on 2026-04-24 after sprint 04 split

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | specs-bootstrap | done | Wrote sprint-contract.md (58 lines) + project-structure.md (81 lines); both under the 200-line ceiling; schema self-applies | [01-specs-bootstrap.md](sprints/01-specs-bootstrap.md) |
| 02 | governance-shrink | done | Replaced the old shipping spec with ship-time-checks.md, rewrote CLAUDE.md and AGENTS.md governance rules, and landed the project-folder rename | [02-governance-shrink.md](sprints/02-governance-shrink.md) |
| 03 | skills-align | planned | Update `/sprint`, `/ship`, `/merge`, `/changelog` skills to contract model; retire `/merge-harmony`; `/ship` runs against contract scope | — |
| 04 | trackers-migrate | done | Moved six trackers/briefs out of `docs/plans/` into per-project `docs/projects/<slug>/` folders via `git mv`; widened sweep updated the one Kotlin test + one harness settings hook and the guardrail test stayed green against the new tracker path | [04-trackers-migrate.md](sprints/04-trackers-migrate.md) |
| 05 | cleanup | done | Executed under `docs/projects/workflow-renovation/` folder (rename-drift artifact): removed the task-route-gate worktree/branch and deleted `docs/plans/active-lanes.md`; widened residue sweep cleaned remaining doc references | [workflow-renovation 05-cleanup.md](../workflow-renovation/sprints/05-cleanup.md) |
| 06 | branch-graveyard-sweep | done | Executed under `docs/projects/workflow-renovation/` folder: harvested Harmony-native app commits onto `platform/harmony` via PR #30, archive-tagged the graveyard, deleted every bucket A/B branch while leaving sprint 04/05 deferred refs untouched | [workflow-renovation 06-branch-graveyard-sweep.md](../workflow-renovation/sprints/06-branch-graveyard-sweep.md) |
| 07 | dtq-03-carryover | done | Classified `8883cf5a8`, user initially chose harvest, then pivoted to archive-only after the checkpoint cherry-pick correctly stopped on multi-hunk conflicts; iteration 4 archive-tagged both source tips, deleted `fix/scheduler-polish`, `parking/fix-scheduler-polish-20260423`, and the empty harvest helper, and removed both attached worktrees | [07-dtq-03-carryover.md](sprints/07-dtq-03-carryover.md) |
| 08 | main-tracker-shrink | planned | Rewrite `docs/plans/tracker.md` (~1154 lines) to a thin project index per `docs/specs/project-structure.md`; archive active-epic narrative into the per-project trackers produced by sprint 04; handle `docs/plans/changelog.md` deprecation | — |

Decomposition guideline: projects that don't close in ~6 sprints should decompose or justify staying open. See Ongoing Justification below for why this project now runs eight.

## Cross-Sprint Decisions

- **Bootstrap scope:** sprint 01 produced the two spec files and nothing else. The scaffold (this tracker + sprint 01 contract file) was Claude's authoring output; operator shipped everything in one commit on sprint close.
- **Operator default:** Codex. User may override per sprint at handoff. **Sprint 01 override: Claude** (docs work, author-executes-own-work while context was loaded).
- **Ship cadence:** one PR per sprint while `/merge` and `/ship` still run under old rules (sprints 01 and 02). Direct-to-develop becomes the default only after sprint 03 rewires the skills.
- **Project rename:** `workflow-renovation` → `develop-protocol-migration` on 2026-04-23 (git mv, applied during sprint 02). Objective widened to full clean + compliant repo; sprint 06 (branch-graveyard-sweep) is the final delivery sprint — project closes when it does.
- **Merge mechanics during sprint 06:** usable branch work merges to `develop` via `cleanup/android-consolidation` PR and to `platform/harmony` via `cleanup/harmony-consolidation` PR. Direct-merge is not used for sprint 06 ops.
- **Per-branch review (sprint 06):** agent decides using commit-evidence (diff + PR state + commit messages + parking date). No per-branch user ping unless genuine ambiguity surfaces (dirty WIP, conflicting merges, cross-lane mixing).
- **Sprint sequencing:** strictly sequential 01 → 02 → 03 → 04 → 05 → 06. No parallelism. Each sprint builds on the previous.
- **Deferred:** Soft gates (CHANGELOG rescue catch-up, dirty main-tree classification) stay deferred; main worktree is off-limits to all sprints per user policy.
- **Sprint 04 split (2026-04-24):** the original sprint 04 bundled three independent concerns (trackers-migrate, DTQ-03 carry-over, main-tracker-shrink). Split into three narrow sprints — 04 (trackers-migrate), 07 (dtq-03-carryover), 08 (main-tracker-shrink) — so that stop criteria, evidence shape, and operator iteration bounds stay clean per concern. Numbers 07 and 08 preserve monotonic ordering even though 05 and 06 already closed.
- **Sprint 04 widening (2026-04-24):** first-pass execution correctly stopped when operator surfaced `GodStructureGuardrailTest.kt` as a runtime consumer of `docs/projects/god-file-cleanup/tracker.md` — out of the original sweep scope. Contract revised to widen scope to the specific non-docs consumers (one Kotlin test + one harness settings hook), with a hard gate on running the guardrail test post-move. The general rule "if an unknown consumer surfaces, stop" remains. Blocked worktree at `.worktrees/docs-sprint-04-trackers-migrate/` is reusable; operator resumes from there with the revised contract.
- **Sprint 04 close mechanics (2026-04-24):** the widened scope was sufficient; repo-wide residue grep after the move returned only the contract file plus Claude's auto-memory file, and `GodStructureGuardrailTest` passed against `docs/projects/god-file-cleanup/tracker.md`. No further non-docs consumers were discovered.
- **Sprint 07 authoring-time surprise (2026-04-24):** pre-authoring cherry-check against `develop` found all five named commits on `fix/scheduler-polish` already on `develop`. The real carry-over is the single `chore: checkpoint` commit on `parking/fix-scheduler-polish-20260423` — 952 insertions of scheduler dev-tooling. Sprint 07 contract therefore inserts a classify-and-route gate before any ref-plane op; user decides harvest vs. archive-only at iteration 1 close.
- **Sprint 07 pivot (2026-04-24):** harvest path stopped at iteration 2 per the contract's multi-hunk rule (`app-core/build.gradle.kts` had two conflict hunks against develop's newer android-app-versioning machinery). Rather than widen the contract to allow manual resolution, user + Claude reviewed integration risk — the dev-tooling is WIP, scheduler file under active god-file decomposition, versioning infrastructure changed — and pivoted to archive-only. Reimplementing against current develop is expected cheaper than resolving + integration-testing against evolved scheduler shape.
- **Rename drift bookkeeping:** sprints 05 and 06 closed under the pre-rename `docs/projects/workflow-renovation/` folder rather than `docs/projects/develop-protocol-migration/` because the rename landed in sprint 02 but sprint 05/06 authoring referenced the older slug. Evidence-of-record for both sprints lives in the workflow-renovation folder; the canonical project of record remains develop-protocol-migration. Disposition of the duplicate workflow-renovation folder is explicitly out-of-scope for this project and will be revisited (if needed) after sprint 08.

## Ongoing Justification (past sprint 6)

Per `docs/specs/project-structure.md` size discipline, projects running past sprint 6 must declare why. This project runs to sprint 8 because the planned sprint 04 split into three narrow sprints (04 / 07 / 08) after authoring review. Each of the three split sprints has distinct evidence requirements (file moves vs. branch disposition vs. tracker rewrite) and an independent stop condition, so bundling them would violate the iteration-bound discipline in `docs/specs/sprint-contract.md`. The project still closes on a single end state — the sprint 08 close triggers project closure.

## Inputs Pending for Later Sprints

- **Sprint 06 (branch-graveyard-sweep) — main worktree policy:** main worktree stays on `parking/develop-diverged-audio-drawer` with its dirty tree untouched; it is explicitly out of sprint 06 scope. End state: main worktree unchanged, all other worktrees either clean or removed.
- **Sprint 08 (main-tracker-shrink) input from sprint 04:** the post-migration `docs/projects/<slug>/` folder list produced by sprint 04 is the input to the shrunk `docs/plans/tracker.md` project index. Sprint 08 cannot start before sprint 04 closes.
- **Sprint 08 (main-tracker-shrink) — `docs/plans/changelog.md` disposition:** deprecated per CLAUDE.md "Single Changelog" rule. Decide at sprint 08 authoring time whether this file archives under a project folder or gets deleted outright with history preserved via git.
- **Sprint 07 (dtq-03-carryover) — pre-run inventory:** `fix/scheduler-polish` tip is `1d164a51f` with five commits (changelog updates, badge-wav audio fix, declaration-first-shipping docs, coalition-integration governance protocol). Each commit must be cherry-checked against `develop` at sprint authoring time because some may already have landed via other paths.

## Lessons Pointer

- Recent additions to `.agent/rules/lessons-learned.md` from the pre-renovation closure: *Hermetic ASR Unit Tests*, *Rescue Branch Lane Split and Post-Merge Cleanup*.
- At project close, propose two amendments to `docs/specs/sprint-contract.md`:
  1. "Authoring produces uncommitted files; operator commits at close" (role-boundary rule that surfaced in sprint 01/02)
  2. "The contract model is for multi-step work with iteration value; one-shot ops run ad-hoc" (contract-scope rule that surfaced during APK-packaging review)
- This project's own lessons will be proposed at project close, after sprint 06.
