---
name: merge
description: Execute PR creation and merge for feature branches into develop. The worktree must be clean; otherwise stop and finish the active sprint through /ship first. Use when the user says /merge for a feature branch. For develop -> master promotion, use the promote-to-master skill instead.
---

# Merge feature branch to develop

Create a PR from the current feature branch to `develop`, wait for CI, and merge.

For `develop` -> `master` promotion, use `promote-to-master`.

## Step 1: Preflight

1. Confirm branch is not `develop`, `master`, or `platform/harmony`.
2. Run `git status --porcelain`.
3. If the worktree is dirty, STOP:
   "Worktree is dirty. Finish the active sprint contract via `/ship`, or abandon with `git restore`. See `docs/specs/sprint-contract.md` §Commit Discipline."
4. Run `git fetch origin`.
5. Verify the branch is ahead of `develop`.

Stop rules:
- `develop`: use `promote-to-master`
- `master`: stop
- `platform/harmony`: stop

## Step 2: Check for existing PR

Use `gh pr list --head <branch> --base develop --json number,url,state`.

- If an open PR exists, report it and move to CI status.
- Otherwise continue.

## Step 3: Push and create PR

- Push the branch if needed.
- Gather commit summary from `develop..HEAD`.
- Create the PR to `develop`.

## Step 4: Wait for CI

- If checks fail, report and stop.
- If checks are pending, report and wait.
- If checks pass, continue.

## Step 5: Merge and sync local

- `gh pr merge <number> --merge --delete-branch`
- switch to `develop`
- pull `origin develop`

## Optional Harmony sync

`/merge-harmony` is retired — `develop` -> `platform/harmony` sync is a manual `git merge develop --no-ff` on `platform/harmony`, documented here only as an optional post-merge step.

## Rules

- Worktree must be clean before merge.
- Never merge `platform/harmony` back into `develop`.
- Never bypass failed CI.
- Never delete `develop`.
