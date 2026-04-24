# Sprint 06 — branch-graveyard-sweep

## Header

- Project: develop-protocol-migration
- Sprint: 06
- Slug: branch-graveyard-sweep
- Date authored: 2026-04-23
- Author: Claude
- Operator: **Codex** (default; user may override at handoff)
- Lane: repo-wide (spans `develop` + `platform/harmony`; also creates archive tags)
- Branch: work happens on two dedicated cleanup PR branches (`cleanup/android-consolidation` from `origin/develop`, `cleanup/harmony-consolidation` from `origin/platform/harmony`); archive tags + branch deletions happen outside any PR
- Worktree: operator may spawn a temporary cleanup worktree (`.worktrees/cleanup-sweep`) or run in `develop-protocol` if clean at sprint start
- Ship path: two PRs (Android cleanup → `develop`, Harmony cleanup → `platform/harmony`); direct operations for branch deletions, tag creation, and remote prune

## Demand

**User ask (verbatim):** "ok now we i launch a former project that honors our current develop protocal that sweep and cleanup for full merged and clean local and remote repo... simply put my expected reults are clean remote repo(git), clean local repo, and develope branch contains alllatest updates"

**Interpretation:** Triage every non-trunk branch (local + remote). Land genuinely useful unmerged work to the correct trunk via one consolidation PR per lane. Archive-as-tag the work that has value but isn't ready to land. Delete everything else. Prune stale worktree registrations and remote refs. Close the project. Agent decides per-branch with commit-evidence; no user ping unless ambiguity surfaces.

## Scope

**Files to edit:**
- `docs/projects/develop-protocol-migration/tracker.md` — flip sprint 06 row to `done`; flip project Status to `closed`
- `docs/projects/develop-protocol-migration/sprints/06-branch-graveyard-sweep.md` — this file; operator appends iteration ledger + closeout
- `CHANGELOG.md` — one line summarizing the clean-repo outcome (internal governance; "Internal: completed develop-protocol migration, single-trunk sprint-contract workflow live")

**Branches to triage (local + origin where present):**

Trunks — never touched:
- `master`, `develop`, `platform/harmony`

Explicitly delete (already resolved by earlier sprints or never-needed):
- `feat/task-route-gate` (deleted in sprint 05 — verify absence, not re-delete)
- `docs/sprint-01-specs-bootstrap` (merged via PR #26)
- `docs/sprint-02-governance-shrink` (merged via sprint 02 PR)
- `docs/sprint-03-skills-align` (merged via sprint 03 PR)
- `docs/sprint-04-trackers-migrate` (merged via sprint 04 PR)
- `docs/sprint-05-cleanup` (merged via sprint 05 PR)
- `docs/pre-renovation-gate-reconcile` (merged via PR #25)
- `fix/asr-hermetic-tests` (merged via PR #24)

All others — triage per decision rule below:
- `codex/safety-before-governance-cleanup`, `codex/safety-before-harmony-undo`
- `docs/workflow-philosophy-sync`
- `feat/app-core-onboarding-pipeline`, `feat/inspiration-pipeline-guardrails`
- `feature/agent-coalition-governance-sync`, `feature/hs010-tingwu-hardening-l3`
- `feature/root-android-single-artifact`, `feature/root-governance-cleanup`, `feature/root-harmony-scheduler-parking`, `feature/root-residue-parking`
- `fix/scheduler-polish`
- `governance/harmony-push-preflight`
- `harmony/scheduler-phase-2b`, `harmony/scheduler-phase-2b-clean`
- `parking/develop-diverged-audio-drawer` (main worktree's branch — see special handling below)
- `parking/docs-workflow-philosophy-sync-20260423`, `parking/feature-root-android-single-artifact-20260423`, `parking/feature-root-governance-cleanup-20260423`, `parking/feature-root-harmony-scheduler-parking-20260423`, `parking/feature-root-residue-parking-20260423`, `parking/fix-scheduler-polish-20260423`, `parking/harmony-scheduler-phase-2b-20260423`

**Out of scope:**
- Main worktree (`/home/cslh-frank/main_app`) itself — must remain untouched. If `parking/develop-diverged-audio-drawer` has usable work, the operator creates an archive tag from it without checking it out in the main worktree (use a temp worktree).
- Any source code edits
- Any Claude memory files
- Any files under `.claude/`, `.codex/`, `.agent/`, `.gemini/`, `.kiro/`, `.roo/`, `.windsurf/` (multi-agent coexistence rule)

## References

1. **Authoritative plan:** `/home/cslh-frank/.claude/plans/i-suddenly-realized-that-serene-lark.md` — project renaming + sprint 06 context
2. **Sprint 01 specs:** `docs/specs/sprint-contract.md`, `docs/specs/project-structure.md`
3. **Tracker:** `docs/projects/develop-protocol-migration/tracker.md` — Cross-Sprint Decisions § *Per-branch review* and § *Merge mechanics* encode the agent-decides rule
4. **Lesson — post-merge cleanup:** `.agent/rules/lessons-learned.md` entry *Rescue Branch Lane Split and Post-Merge Cleanup* — squash-merged branches look "ahead" by commit count; cleanup safety comes from PR merge state, not from branch-ahead count
5. **Shell inventory commands:** `git branch -a`, `gh pr list --state all --head <branch>`, `git log --oneline origin/<trunk>..<branch>`, `git worktree list`

## Success Exit Criteria

**Remote cleanliness:**
- `git ls-remote --heads origin | wc -l` returns 3 (master, develop, platform/harmony)
- `gh pr list --state open --json number --jq length` returns 0 (no orphan open PRs besides the two cleanup PRs after they merge)

**Local cleanliness:**
- `git branch | grep -v -E '^\*?\s*(master|develop|platform/harmony)$' | wc -l` returns 0 (no non-trunk local branches)
- `git worktree list | wc -l` equals the count of actively-needed worktrees (minimum = main + any used by in-flight work; all `.worktrees/*` worktrees for retired branches removed)
- `git worktree prune --dry-run` shows no entries to prune
- `git remote prune origin --dry-run` shows no entries to prune

**Develop has latest useful work:**
- `cleanup/android-consolidation` PR is MERGED into `develop` (contains all Android-lane work salvaged from triage)
- `cleanup/harmony-consolidation` PR is MERGED into `platform/harmony` if any Harmony work was salvaged (otherwise marked as "no-op, closed without merge" in evidence)
- `git log origin/develop..<any-deleted-branch>` returns empty for every deleted branch (verification via archive tag if archived)

**Archive preservation:**
- Every branch that was archive-tagged has a tag `archive/<branch-slug>-<YYYYMMDD>` at origin
- `git tag -l 'archive/*' | wc -l` > 0 if any branches were archived; 0 is also valid if none had preserve-worthy content
- Running `git show archive/<slug>-<date>` on any archive tag produces the branch's final commit

**Project closure:**
- `docs/projects/develop-protocol-migration/tracker.md` sprint 06 row status is `done`
- Project Status is `closed`
- `CHANGELOG.md` has a one-line internal-governance entry referencing the migration close

**Main worktree untouched:**
- `git -C /home/cslh-frank/main_app status --porcelain` output at sprint close is IDENTICAL to output at sprint start (captured in iteration ledger)

**Residual reference hygiene (final-delivery check):**

This is the project-level straggler sweep, not a per-sprint-scoped grep. It catches references to retired mechanisms that earlier sprints' narrower grep checks intentionally excluded (other-sprint territory). If earlier sprints completed cleanly, this should return empty. If it returns hits, those are real gaps — either fold into sprint 06's scope as small corrections or halt and surface to user.

- `git grep -l -E "declaration-first-shipping\.md|retired lane registry|task-route-gate|pre-flight scope conflict|--force-parallel|/merge-harmony" -- ':!docs/archive/**' ':!docs/projects/develop-protocol-migration/sprints/**'` returns **0 hits**
- Allowed exceptions (exclude paths if present): `docs/archive/**` (historical), `docs/projects/develop-protocol-migration/sprints/**` (contracts' own historical evidence), and any file the user explicitly retains with an archival note
- If any hit falls under `docs/specs/**`, `docs/sops/**`, `docs/cerb/**`, or `docs/core-flow/**` — operator edits the reference in-sprint (rule: swap to `ship-time-checks.md` / remove the retired lane-registry reference / rewrite sentence to match new model). Change remains within sprint 06 Scope because the end state ("clean repo per new protocol") logically covers it.
- If a hit falls outside `docs/**` and isn't in an obvious archival path — halt and surface; don't guess at how to rewrite code-adjacent references

## Stop Exit Criteria

- Per-branch triage surfaces genuine ambiguity: dirty WIP state, conflicting merges, cross-lane mixing that can't be cleanly split, or work whose intent cannot be inferred from commit evidence → halt and surface to user; do not guess
- Consolidation PR has merge conflicts that can't be resolved via standard three-way merge → halt; do not force-resolve
- Main worktree `git status` changes during sprint (means operator touched it) → halt immediately; status must be byte-identical throughout
- Iteration bound reached without all Success Exit Criteria green → halt; do not partial-clean

On stop: do NOT commit. Do NOT delete branches. Do NOT push cleanup PRs. Leave work-in-progress in place; fill Closeout with `status: stopped` + failing criterion + per-branch decision log so far.

## Iteration Bound

- Max 6 iterations OR 4 hours wall-clock, whichever hits first
- Larger bound than other sprints because branch-by-branch triage across ~25 branches + two PR creations + archive tag pushes is genuinely longer work

## Per-Branch Decision Rule

For every branch in Scope's triage list, operator collects this evidence:

1. `gh pr list --state all --head <branch> --json number,state --jq '.[0].state'` — PR state (MERGED / CLOSED / OPEN / missing)
2. `git log --oneline origin/<trunk>..<branch>` — commits ahead of its natural trunk (develop for Android/docs/codex/feat/feature/fix/parking-android; platform/harmony for harmony/*)
3. `git diff origin/<trunk>...<branch> --stat` — file-level diff summary
4. `git log -5 --format="%h %s" <branch>` — recent commit messages
5. Branch age: `git log -1 --format='%cr' <branch>`

Operator decides per this table:

| Evidence pattern | Outcome |
|---|---|
| PR state MERGED | Delete (local + remote), no tag |
| PR state CLOSED without merge, and diff empty vs trunk | Delete, no tag |
| PR state CLOSED without merge, diff non-empty, commits look like abandoned WIP (TODO/WIP markers, half-done commits) | Archive-as-tag, then delete |
| PR state CLOSED without merge, diff non-empty, commits look clean + finished but explicitly superseded (parking/* twin exists with later date) | Archive-as-tag from the later branch, delete both |
| No PR exists, diff empty vs trunk | Delete, no tag |
| No PR exists, diff non-empty, work is Android/shared scope, commits look clean + finished | Add to `cleanup/android-consolidation` cherry-pick list |
| No PR exists, diff non-empty, work is Harmony scope, commits look clean + finished | Add to `cleanup/harmony-consolidation` cherry-pick list |
| No PR exists, diff non-empty, commits look like abandoned WIP | Archive-as-tag, then delete |
| Branch has genuine ambiguity (dirty WIP, cross-lane mixing, can't classify) | Halt per Stop criterion; surface to user |

Special cases:
- `parking/develop-diverged-audio-drawer` — tied to main worktree; archive-as-tag from a temp worktree (never checkout in main), then `git worktree remove` doesn't apply because main is not a `.worktrees/*` entry. Delete the branch name only if the main worktree is graduated off it; otherwise leave the branch in place with an archive tag twin for safety. Main worktree's checkout stays on the (still-existing) branch.
- `codex/safety-before-*` — per commit-evidence, these are safety checkpoints taken before pre-renovation operations that succeeded. Delete without tag (the operations they protected against are already complete).
- `docs/workflow-philosophy-sync` paired with `parking/docs-workflow-philosophy-sync-20260423` — the parking twin is the authoritative version per parking-convention; treat the non-parking branch as superseded.
- Parking twins (`parking/<original>-20260423`) generally supersede their non-parking originals; cross-reference and archive whichever has content the other doesn't.

## Required Evidence Format

At close, operator appends to Closeout:

1. **Pre-sweep inventory** — full output of `git branch -a` and `git worktree list` taken at iteration 1 start
2. **Per-branch decision log** — one row per triaged branch with: branch name, PR state, diff line count, decision (delete / archive+delete / android-cherry-pick / harmony-cherry-pick / halt), reasoning (one line)
3. **Consolidation PR links** — `cleanup/android-consolidation` PR number + merged SHA; same for Harmony if created
4. **Archive tags** — output of `git tag -l 'archive/*'` after tag creation + `git push origin --tags` confirmation
5. **Post-sweep verification** — full output of:
   - `git ls-remote --heads origin`
   - `git branch`
   - `git worktree list`
   - `git worktree prune --dry-run`
   - `git remote prune origin --dry-run`
6. **Main worktree invariance proof** — `git -C /home/cslh-frank/main_app status --porcelain` output at iteration 1 start AND at sprint close; diff between them must be empty
7. **Residual reference hygiene sweep** — output of `git grep -l -E "declaration-first-shipping\.md|retired lane registry|task-route-gate|pre-flight scope conflict|--force-parallel|/merge-harmony" -- ':!docs/archive/**' ':!docs/projects/develop-protocol-migration/sprints/**'` (expected: empty). If non-empty at first run, operator either fixed in-sprint or halted and surfaced; evidence shows both the initial run and the post-fix run.
8. **Project-close markers** — `docs/projects/develop-protocol-migration/tracker.md` showing sprint 06 `done` + project `closed`; `CHANGELOG.md` new entry

## Iteration Ledger

*(Operator appends one entry per iteration. Not committed mid-sprint.)*

- *(empty — awaiting operator pickup)*

## Closeout

*(Operator fills at exit.)*

- **Status:** *(success | stopped | blocked)*
- **Summary for project tracker** *(one line):*
- **Evidence artifacts:**
  - Pre-sweep inventory:
  - Per-branch decision log:
  - Consolidation PR links:
  - Archive tags:
  - Post-sweep verification:
  - Main worktree invariance proof:
  - Residual reference hygiene sweep:
  - Project-close markers:
- **Lesson proposals** *(0-N; human-gated. Propose here: role-boundary spec amendment, contract-scope spec amendment, anything surfaced during branch triage that would help future agents):*
- **CHANGELOG line** *(internal-governance; something like "Internal: completed develop-protocol migration; single-trunk sprint-contract workflow is now the operating model"):*
