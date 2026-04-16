---
name: merge
description: Execute PR creation and merge for feature branches into develop. Use when the user says /merge for a feature branch. For develop -> master promotion, use the promote-to-master skill instead.
---

# Merge feature branch to develop

Create a PR from the current feature branch to develop, wait for CI, and merge.

For develop -> master promotion, use the `promote-to-master` skill instead.

## Step 1: Preflight

1. Confirm branch is NOT `develop`, `master`, or `platform/harmony`:
   ```bash
   git rev-parse --abbrev-ref HEAD
   ```
   - On `develop`: STOP. "Use promote-to-master skill for develop -> master."
   - On `master`: STOP. "Cannot merge from master."
   - On `platform/harmony`: STOP. "Platform branches do not merge back."

2. Working tree is clean:
   ```bash
   git status --porcelain
   ```
   If dirty: STOP. "Uncommitted changes. Commit or stash first."

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
- Never force-push to develop or master.
- Never merge if CI fails — report and stop.
- Never delete the develop branch.
- `--delete-branch` on merge is correct: feature branches are short-lived and should be cleaned up.
