# Sprint 05 - cleanup

## Header

- Project: workflow-renovation
- Sprint: 05
- Slug: cleanup
- Date authored: 2026-04-24
- Author: Claude
- Operator: **Codex** (default operator)
- Lane: `develop`
- Branch: `docs/sprint-05-cleanup` (cut fresh from `origin/develop`)
- Worktree: `.worktrees/develop-merge`
- Ship path: one PR to `develop` for the docs/tracker/contract changes plus the ref-plane deletions recorded in evidence

## Demand

**User ask (verbatim from project tracker sprint index):** "Delete `.worktrees/task-route-gate/`, `feat/task-route-gate` branch, `docs/plans/active-lanes.md`; grep-sweep residue."

**Interpretation:** The first pass removed the local worktree, local branch, and registry file as planned. Fresh `origin/develop` still exposed pre-existing `active-lanes` references in archive/reference/project-history docs outside the original narrow scope, so the user approved widening sprint 05 to clean that doc residue too while keeping `.codex/skills/ship/SKILL.md` out of scope.

## Scope

- `.worktrees/task-route-gate/` - remove via `git worktree remove`
- local branch `feat/task-route-gate` and remote `origin/feat/task-route-gate` - delete
- `docs/plans/active-lanes.md` - delete
- `docs/reference/agent-lessons-details.md` - remove the retired registry path reference
- `docs/archive/declaration-first-shipping-archive-20260423.md` - replace the retired registry path with generic historical wording
- `docs/projects/develop-protocol-migration/tracker.md` - clean the retired registry path from the sprint 05 summary row
- `docs/projects/develop-protocol-migration/sprints/01-specs-bootstrap.md` - clean the retired registry path from out-of-scope notes
- `docs/projects/develop-protocol-migration/sprints/02-governance-shrink.md` - clean the retired registry path from historical grep/evidence text
- `docs/projects/develop-protocol-migration/sprints/06-branch-graveyard-sweep.md` - clean the retired registry path from residual-reference wording
- `docs/projects/workflow-renovation/tracker.md` - sprint 05 row update
- `docs/projects/workflow-renovation/sprints/05-cleanup.md` - this contract

**Out of scope** (do not touch):

- `.codex/skills/ship/SKILL.md`, `.codex/skills/merge/SKILL.md`, `.codex/skills/changelog/SKILL.md` (sprint 03)
- `docs/plans/changelog.md` - historical mention intentionally retained
- Any branch other than `feat/task-route-gate` (sprint 06 owns those)

## References

1. `docs/specs/sprint-contract.md`
2. `docs/specs/project-structure.md`
3. `docs/projects/workflow-renovation/tracker.md`
4. `docs/projects/develop-protocol-migration/tracker.md`

## Success Exit Criteria

Literally checkable:

- `git worktree list` does not contain `.worktrees/task-route-gate`
- `test ! -d .worktrees/task-route-gate`
- `git show-ref --verify --quiet refs/heads/feat/task-route-gate` exits non-zero
- `git ls-remote origin refs/heads/feat/task-route-gate` emits no matching ref
- `test ! -f docs/plans/active-lanes.md`
- `grep -rln "active-lanes" --include="*.md" docs/ AGENTS.md CLAUDE.md 2>/dev/null | grep -v '^docs/plans/changelog.md$' | grep -v '^docs/projects/workflow-renovation/'` returns empty
- `docs/projects/workflow-renovation/tracker.md` sprint 05 row shows status `done`
- One PR to `develop` covers the file deletion + residue cleanup + tracker update + contract closeout

## Stop Exit Criteria

Halt and surface:

- `.worktrees/task-route-gate/` has uncommitted work before removal
- `feat/task-route-gate` tip is not `c055d7564a75eb6e6cc34846bbbc1a0dcbc0c790` at execution time
- Residue grep still finds hits outside the exclusion list after the widened cleanup
- Any required git command fails, including remote branch deletion verification or PR creation/auth

## Iteration Bound

- Max 3 iterations OR 60 minutes wall-clock, whichever hits first
- One iteration = delete/ref cleanup attempt + residue verification + tracker/contract update

## Required Evidence Format

At close, operator appends to Closeout:

1. Preconditions evidence:
   - `git rev-parse feat/task-route-gate`
   - `git -C .worktrees/task-route-gate status --short`
   - `test -f docs/plans/active-lanes.md && echo registry-present`
2. Execution evidence:
   - `git worktree remove .worktrees/task-route-gate`
   - `git branch -D feat/task-route-gate`
   - `git push origin :refs/heads/feat/task-route-gate`
   - `rm docs/plans/active-lanes.md`
   - widened residue-edit evidence
3. Verification evidence:
   - `git worktree list`
   - `git show-ref --verify --quiet refs/heads/feat/task-route-gate; echo local-exit=$?`
   - `git ls-remote origin refs/heads/feat/task-route-gate`
   - `test -f docs/plans/active-lanes.md; echo file-exit=$?`
   - residue grep command output

Agent narration without these artifacts is not acceptable evidence.

## Iteration Ledger

- **Iteration 1 (2026-04-24, operator Claude, authoring):** verified preconditions - `.worktrees/task-route-gate` attached to `feat/task-route-gate` at clean tip `c055d7564`, `docs/plans/active-lanes.md` present on develop, sprint 02 already cleaned CLAUDE.md + AGENTS.md + the live shipping spec of `active-lanes` references. Wrote the narrow cleanup contract.
- **Iteration 2 (2026-04-24, operator Codex, execution):** removed `.worktrees/task-route-gate`, deleted local branch `feat/task-route-gate`, deleted `docs/plans/active-lanes.md`, then ran the post-delete residue grep on fresh `origin/develop`. Evaluator saw unexpected surviving references in `docs/reference/agent-lessons-details.md`, `docs/archive/declaration-first-shipping-archive-20260423.md`, and `docs/projects/develop-protocol-migration/**`, so the original contract hit its stop condition before commit.
- **Iteration 3 (2026-04-24, operator Codex, user-approved widening):** user approved widening sprint 05. Cleaned the newly-found archive/reference/project-history residue so the scoped `active-lanes` grep now returns only the intentionally-excluded `docs/plans/changelog.md`. Evaluator saw the local cleanup was coherent, but remote deletion/PR work was blocked because both `git push origin :refs/heads/feat/task-route-gate` and `gh auth status` failed on invalid GitHub auth.
- **Iteration 4 (2026-04-24, operator Codex, auth repaired):** user repaired GitHub auth. Verified `gh auth status` is healthy, configured SSH-over-443 fallback, confirmed the remote `feat/task-route-gate` ref is absent, and prepared the branch for commit/push/PR. Next action: commit, push, and open the sprint 05 PR.
- **Iteration 5 (2026-04-24, operator Codex, ship branch opened):** committed the widened cleanup, amended the contract evidence, pushed final commit `9e08d1a40` to `docs/sprint-05-cleanup`, and opened PR #28 to `develop`. Evaluator saw the contract evidence now has the required commit/PR artifacts. Next action: leave sprint 05 on review path and cut sprint 06 from fresh `origin/develop`.

## Closeout

- **Status:** success
- **Summary for project tracker:** Removed the task-route-gate worktree/branch and deleted `docs/plans/active-lanes.md`; widened the residue sweep cleaned the remaining doc references and opened the sprint PR.
- **Evidence artifacts:**
  - Preconditions evidence:
    ```text
    git rev-parse feat/task-route-gate
    c055d7564a75eb6e6cc34846bbbc1a0dcbc0c790

    git -C .worktrees/task-route-gate status --short
    [no output]

    test -f docs/plans/active-lanes.md && echo registry-present
    registry-present
    ```
  - Execution evidence:
    ```text
    git worktree remove .worktrees/task-route-gate
    [no output]

    git branch -D feat/task-route-gate
    Deleted branch feat/task-route-gate (was c055d7564).

    git push origin :refs/heads/feat/task-route-gate
    Connection closed by 198.18.0.19 port 22
    fatal: Could not read from remote repository.

    Please make sure you have the correct access rights
    and the repository exists.

    rm docs/plans/active-lanes.md
    [no output]

    gh auth status
    github.com
      X Failed to log in to github.com account esir-nus (keyring)
      - Active account: true
      - The token in keyring is invalid.
      - To re-authenticate, run: gh auth login -h github.com
      - To forget about this account, run: gh auth logout -h github.com -u esir-nus

    commit SHA
    9e08d1a40

    PR URL
    https://github.com/esir-nus/smart-sales-main/pull/28
    ```
  - Verification evidence:
    ```text
    gh auth status
    github.com
      ✓ Logged in to github.com account esir-nus (keyring)
      - Active account: true
      - Git operations protocol: ssh
      - Token scopes: 'admin:public_key', 'gist', 'read:org', 'repo'

    git worktree list
    /home/cslh-frank/main_app                                              71f3019b6 [parking/develop-diverged-audio-drawer]
    /home/cslh-frank/main_app/.worktrees/develop-merge                     e98b3f549 [docs/sprint-05-cleanup]
    /home/cslh-frank/main_app/.worktrees/develop-protocol                  7b2743b56 [docs/sprint-02-governance-shrink]
    /home/cslh-frank/main_app/.worktrees/fix-scheduler-debug-optimization  8883cf5a8 [parking/fix-scheduler-polish-20260423]
    /home/cslh-frank/main_app/.worktrees/harmony-governance-preflight      6da696cab [governance/harmony-push-preflight]
    /home/cslh-frank/main_app/.worktrees/harmony-scheduler-phase-2b        588edf54b [parking/harmony-scheduler-phase-2b-20260423]
    /home/cslh-frank/main_app/.worktrees/harmony-scheduler-phase-2b-clean  d0a6299f9 [harmony/scheduler-phase-2b-clean]
    /home/cslh-frank/main_app/.worktrees/root-android-single-artifact      620e8bc0d [parking/feature-root-android-single-artifact-20260423]
    /home/cslh-frank/main_app/.worktrees/root-governance-cleanup           a9eaad947 [parking/feature-root-governance-cleanup-20260423]
    /home/cslh-frank/main_app/.worktrees/root-harmony-scheduler-parking    a6ba036aa [parking/feature-root-harmony-scheduler-parking-20260423]
    /home/cslh-frank/main_app/.worktrees/root-residue-parking              0b15486e3 [parking/feature-root-residue-parking-20260423]
    /home/cslh-frank/main_app/.worktrees/workflow-philosophy-sync          182c08c5c [parking/docs-workflow-philosophy-sync-20260423]

    test ! -d .worktrees/task-route-gate; echo dir-exit=$?
    dir-exit=0

    git show-ref --verify --quiet refs/heads/feat/task-route-gate; echo local-exit=$?
    local-exit=1

    git ls-remote origin refs/heads/feat/task-route-gate
    [no output]

    test -f docs/plans/active-lanes.md; echo file-exit=$?
    file-exit=1

    grep -rln "active-lanes" --include="*.md" docs/ AGENTS.md CLAUDE.md 2>/dev/null | grep -v '^docs/plans/changelog.md$' | grep -v '^docs/projects/workflow-renovation/'
    [no output]
    ```
- **Lesson proposals:** none
- **CHANGELOG line:** none. Internal-governance documentation only; not user-visible.
