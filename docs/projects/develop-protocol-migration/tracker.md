# Project: develop-protocol-migration

## Objective

Deliver the end state: clean remote repo (no dead branches, no orphan PRs), clean local repo (no stale worktrees/branches/tags), `develop` contains all latest useful updates, and full compliance with the new development model (sprint-contract workflow, two-trunk split, index+details docs, doc-production discipline). Replace per-task feature-branch / worktree isolation with a sprint-contract workflow. Every task runs inside one self-contained contract; projects own holistic scope (code + docs + UI together); context resets at sprint/project boundaries.

Project was renamed from `workflow-renovation` on 2026-04-23 after the user clarified that "renovation" under-described the scope — the real goal is a fully clean + model-compliant repo, not just governance updates.

Authoritative plan: `/home/cslh-frank/.claude/plans/i-suddenly-realized-that-serene-lark.md`
Baseline: tag `pre-renovation-baseline` at `origin/develop` SHA `c9d21be1b`.

## Status

open — authored 2026-04-23 (renamed from workflow-renovation same day)

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | specs-bootstrap | done | Wrote sprint-contract.md (58 lines) + project-structure.md (81 lines); both under the 200-line ceiling; schema self-applies | [01-specs-bootstrap.md](sprints/01-specs-bootstrap.md) |
| 02 | governance-shrink | done | Replaced the old shipping spec with ship-time-checks.md, rewrote CLAUDE.md and AGENTS.md governance rules, and landed the project-folder rename | [02-governance-shrink.md](sprints/02-governance-shrink.md) |
| 03 | skills-align | planned | Update `/sprint`, `/ship`, `/merge`, `/changelog` skills to contract model; retire `/merge-harmony`; `/ship` runs against contract scope | — |
| 04 | trackers-migrate | planned | Migrate `god-tracker`, `harmony-tracker`, `ui-tracker`, `bug-tracker`, wave briefs into `docs/projects/<slug>/`; migrate DTQ-03 carry-over; shrink `docs/plans/tracker.md` to project index; archive `docs/plans/changelog.md` | — |
| 05 | cleanup | planned | Delete `.worktrees/task-route-gate/`, `feat/task-route-gate` branch, the retired lane registry file; grep-sweep residue | — |
| 06 | branch-graveyard-sweep | authored | Triage all non-trunk branches: delete already-landed, merge usable work via one PR per lane (`cleanup/android-consolidation`, `cleanup/harmony-consolidation`), archive-as-tag the rest, delete branches, prune worktrees + remote refs | [06-branch-graveyard-sweep.md](sprints/06-branch-graveyard-sweep.md) |

Decomposition guideline: projects that don't close in ~6 sprints should decompose or justify staying open. This project is budgeted at 6.

## Cross-Sprint Decisions

- **Bootstrap scope:** sprint 01 produced the two spec files and nothing else. The scaffold (this tracker + sprint 01 contract file) was Claude's authoring output; operator shipped everything in one commit on sprint close.
- **Operator default:** Codex. User may override per sprint at handoff. **Sprint 01 override: Claude** (docs work, author-executes-own-work while context was loaded).
- **Ship cadence:** one PR per sprint while `/merge` and `/ship` still run under old rules (sprints 01 and 02). Direct-to-develop becomes the default only after sprint 03 rewires the skills.
- **Project rename:** `workflow-renovation` → `develop-protocol-migration` on 2026-04-23 (git mv, applied during sprint 02). Objective widened to full clean + compliant repo; sprint 06 (branch-graveyard-sweep) is the final delivery sprint — project closes when it does.
- **Merge mechanics during sprint 06:** usable branch work merges to `develop` via `cleanup/android-consolidation` PR and to `platform/harmony` via `cleanup/harmony-consolidation` PR. Direct-merge is not used for sprint 06 ops.
- **Per-branch review (sprint 06):** agent decides using commit-evidence (diff + PR state + commit messages + parking date). No per-branch user ping unless genuine ambiguity surfaces (dirty WIP, conflicting merges, cross-lane mixing).
- **Sprint sequencing:** strictly sequential 01 → 02 → 03 → 04 → 05 → 06. No parallelism. Each sprint builds on the previous.
- **Deferred:** DTQ-03 carry-over migration lives in sprint 04. Soft gates (CHANGELOG rescue catch-up, dirty main-tree classification) stay deferred; main worktree is off-limits to all sprints per user policy.

## Inputs Pending for Later Sprints

- **Sprint 06 (branch-graveyard-sweep) — main worktree policy:** main worktree stays on `parking/develop-diverged-audio-drawer` with its dirty tree untouched; it is explicitly out of sprint 06 scope. End state: main worktree unchanged, all other worktrees either clean or removed.

## Lessons Pointer

- Recent additions to `.agent/rules/lessons-learned.md` from the pre-renovation closure: *Hermetic ASR Unit Tests*, *Rescue Branch Lane Split and Post-Merge Cleanup*.
- At project close, propose two amendments to `docs/specs/sprint-contract.md`:
  1. "Authoring produces uncommitted files; operator commits at close" (role-boundary rule that surfaced in sprint 01/02)
  2. "The contract model is for multi-step work with iteration value; one-shot ops run ad-hoc" (contract-scope rule that surfaced during APK-packaging review)
- This project's own lessons will be proposed at project close, after sprint 06.
