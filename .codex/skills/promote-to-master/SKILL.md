---
name: promote-to-master
description: Promote develop to master via PR. Use when the user says /merge, /promote, or asks to ship develop to master. This is the only sanctioned way to update master.
---

# Promote develop to master

This skill creates a promotion PR from `develop` to `master`. Master is protected and only receives merge commits via PR.

## Pre-flight checks

Before creating the PR, verify:

1. You are on the `develop` branch
2. `develop` is up to date with `origin/develop` (`git fetch origin && git rev-list HEAD..origin/develop --count` returns 0)
3. There are commits on `develop` that are not yet on `master` (`git rev-list master..develop --count` > 0)
4. The working tree is clean (`git status --porcelain` is empty)

If any check fails, report the issue and stop.

## Create the promotion PR

```bash
# Get the commit summary for the PR body
COMMITS=$(git log master..develop --oneline --no-merges | head -20)
MERGE_COUNT=$(git log master..develop --oneline --merges | wc -l)
COMMIT_COUNT=$(git log master..develop --oneline --no-merges | wc -l)

gh pr create --base master --head develop \
  --title "promote: develop → master" \
  --body "## Summary

Promoting develop to master.

**${COMMIT_COUNT} commits, ${MERGE_COUNT} merges** since last promotion.

### Notable commits
${COMMITS}

## Test plan
- [ ] CI passes (android-tests, platform-governance-check)
- [ ] No regressions in existing functionality
"
```

## After PR creation

1. Report the PR URL to the user
2. Wait for CI to pass
3. If the user says to merge, merge the PR:
   ```bash
   gh pr merge <PR_NUMBER> --merge --delete-branch=false
   ```
   Do NOT delete the develop branch after merge — it is the long-lived working branch.

## Sync platform branches (optional)

If the user asks, sync the promotion into platform branches:

```bash
git checkout platform/harmony
git merge develop --no-ff -m "merge: sync shared contracts from develop"
git push origin platform/harmony
git checkout develop
```

## Rules

- Never force-push to master
- Never merge directly without a PR
- Never delete the develop branch
- If CI fails, report the failure — do not bypass
