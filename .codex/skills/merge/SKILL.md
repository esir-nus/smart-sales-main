---
name: merge
description: Execute PR creation and merge for feature branches into develop. If the worktree is dirty, classify whether it is coherent or lane-mixed, print a recovery plan, and stop. Use when the user says /merge for a feature branch. For develop -> master promotion, use the promote-to-master skill instead.
---

# Merge feature branch to develop

Create a PR from the current feature branch to develop, wait for CI, and merge.

If the worktree is dirty, do not blindly stop with a generic warning. Run the dirty-tree preflight below, classify the tree, print recovery guidance, and stop without mutating git state.

For develop -> master promotion, use the `promote-to-master` skill instead.

## Step 1: Preflight

1. Confirm branch is NOT `develop`, `master`, or `platform/harmony`:
   ```bash
   git rev-parse --abbrev-ref HEAD
   ```
   - On `develop`: STOP. "Use promote-to-master skill for develop -> master."
   - On `master`: STOP. "Cannot merge from master."
   - On `platform/harmony`: STOP. "Platform branches do not merge back."

2. Inspect working tree state before deciding whether merge is possible:
   ```bash
   git status --porcelain
   git diff --name-only
   git diff --cached --name-only
   git ls-files --others --exclude-standard
   ```

   Preflight outcomes:
   - `clean-and-mergeable`: no blocking dirty files; continue to Step 3.
   - `dirty-but-coherent`: worktree is dirty, but the files belong to one coherent lane and could be shipped first; STOP with recovery guidance.
   - `dirty-and-mixed`: worktree is dirty and spans multiple incompatible lanes; STOP with recovery guidance.

   Dirty-tree classification rules:

   ### 2.1 Path buckets

   Classify tracked and untracked files into these buckets:

   - **Android/shared implementation**:
     - `app/**`
     - `app-core/**`
     - `core/**`
     - `data/**`
     - `domain/**`
   - **Harmony-native implementation**:
     - `platforms/harmony/**`
   - **Shared docs/contracts** (develop-owned by repo law):
     - `docs/core-flow/**`
     - `docs/cerb/**`
     - `docs/cerb-ui/**`
     - `docs/specs/**`
     - `docs/cerb/interface-map.md`
   - **Harmony overlay / Harmony evidence docs**:
     - `docs/platforms/harmony/**`
     - Harmony-specific reports under `docs/reports/**`
   - **Governance / tracker docs**:
     - `docs/plans/**`
   - **Local tooling / non-product**:
     - `.codex/**`
     - comparable repo-local helper assets that are not product or governance artifacts

   ### 2.2 Outcome rules

   - `dirty-but-coherent`:
     - dirty files all belong to one mergeable lane for the current branch, such as:
       - Android/shared implementation plus matching shared docs on a `feature/*` branch
       - shared docs/contracts only on a `feature/*` or `shared/*` branch
     - no Harmony-native implementation is mixed in
   - `dirty-and-mixed`:
     - Harmony-native implementation appears alongside Android/shared implementation
     - Harmony-native implementation appears alongside develop-owned shared contracts/docs in the same dirty tree
     - multiple buckets imply different branch targets under `docs/specs/platform-governance.md`

   ### 2.3 Shared-doc routing rule

   Shared docs/contracts are owned by `develop` by default.

   - If `docs/cerb/**`, `docs/cerb-ui/**`, `docs/specs/**`, `docs/core-flow/**`, or `docs/cerb/interface-map.md` are dirty alongside Harmony-native implementation, classify them as a separate `shared-contract` bucket.
   - Do **not** treat those shared docs as Harmony-owned just because Harmony code is also dirty.
   - Recovery guidance must route the shared-contract bucket to a `develop`-based branch first.
   - Harmony overlay docs under `docs/platforms/harmony/**` stay with the Harmony bucket.
   - `docs/plans/**` route by ownership:
     - branch/governance/shared tracker state -> develop-based branch
     - Harmony sprint/evidence state -> Harmony branch
   - If shared-contract docs are dirty alongside both Android and Harmony implementation, still classify the tree as `dirty-and-mixed` and recommend:
     1. extract shared-contract docs to a develop-based branch
     2. land that branch first
     3. sync `develop -> platform/harmony`

   ### 2.4 Untracked-file policy

   - Untracked `.codex/**` or similar local tooling paths are advisory only. Report them, but they do not block merge preflight by themselves.
   - Untracked product files, tests, reports, build files, or docs under product/governance paths do count as dirty work and participate in classification.
   - Untracked files under `platforms/harmony/**` count as Harmony-native dirty work.
   - Untracked files under `docs/**` count as dirty docs/work and participate in routing.

   ### 2.5 Output when blocked

   If outcome is `dirty-but-coherent` or `dirty-and-mixed`, STOP and print:
   - outcome name
   - why merge is blocked
   - bucketed file groups
   - recommended destination branch types for each bucket
   - whether `/ship` is appropriate now

   `/ship` relationship:
   - `dirty-but-coherent`: recommend `/ship` after the user is ready to stage and commit the coherent scope.
   - `dirty-and-mixed`: do not redirect straight to `/ship`; first print the split/recovery guidance and say `/ship` becomes appropriate only after the tree is separated into coherent branches.

3. Branch has commits ahead of develop:
   ```bash
   git fetch origin
   git rev-list develop..HEAD --count
   ```
   If 0: STOP. "No commits to merge."

4. Branch is up to date with remote (if tracking):
   ```bash
   git rev-list HEAD..origin/$(git rev-parse --abbrev-ref HEAD) --count 2>/dev/null
   ```
   If behind: warn, suggest `git pull`.

## Step 2: Check for existing PR

```bash
BRANCH=$(git rev-parse --abbrev-ref HEAD)
gh pr list --head "$BRANCH" --base develop --json number,url,state
```

- If open PR exists: report URL, skip to Step 4 (CI check).
- If no PR: continue to Step 3.

## Step 3: Push and create PR

```bash
# Push branch
git push -u origin "$BRANCH"

# Gather context
COMMITS=$(git log develop..HEAD --oneline --no-merges)
COMMIT_COUNT=$(git log develop..HEAD --oneline --no-merges | wc -l)
FILES=$(git diff develop..HEAD --name-only)
```

Create the PR:

```bash
gh pr create --base develop --head "$BRANCH" \
  --title "{title derived from branch name or commits, under 70 chars}" \
  --body "$(cat <<'EOF'
## Summary
{2-3 bullets from commit summary}

**{COMMIT_COUNT} commits** on this branch.

### Changed files
{FILES list}

## Test plan
- [ ] CI passes (android-tests, platform-governance-check)
- [ ] No regressions in existing functionality
EOF
)"
```

## Step 4: Wait for CI

```bash
gh pr checks {PR_NUMBER}
```

- If checks pass: proceed to merge.
- If checks fail: report failures. Do NOT merge. Do NOT bypass.
- If checks pending: report status and wait for user instruction.

## Step 5: Merge

```bash
gh pr merge {PR_NUMBER} --merge --delete-branch
```

Verify:
```bash
gh pr view {PR_NUMBER} --json state
```
Must return `MERGED`.

## Step 6: Sync local

```bash
git checkout develop
git pull origin develop
```

## Step 7: Report

Print:
- PR URL
- Merge status
- Commit landed on develop

## Optional: Harmony sync

If the user requests, sync into platform/harmony:

```bash
git checkout platform/harmony
git merge develop --no-ff -m "merge: sync shared contracts from develop"
git push origin platform/harmony
git checkout develop
```

## Rules

- This skill is for feature -> develop only. Use `promote-to-master` for develop -> master.
- The dirty-tree planner is diagnostic only. Do not auto-stash, auto-commit, auto-branch, auto-split, or auto-open multiple PRs.
- Shared docs/contracts are develop-owned and flow `develop -> platform/harmony`, never the reverse.
- Do not treat Harmony-native implementation mixed with shared-contract docs as a mergeable feature branch.
- Never force-push to develop or master.
- Never merge if CI fails — report and stop.
- Never delete the develop branch.
- `--delete-branch` on merge is correct: feature branches are short-lived and should be cleaned up.
