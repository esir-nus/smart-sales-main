PR creation and merge orchestration. This is the only path that touches `master` -- through PRs, never direct commits.

Primary path: produce a Codex handoff for `/merge` (feature -> develop) or `promote-to-master` (develop -> master).
Backup path: if Codex is unavailable, Claude executes the PR creation and merge directly.

For `harmony/*` feature branches landing into `platform/harmony`, use `/merge-harmony` instead.

Use this when the user asks to merge, integrate, promote, or land a branch. Two modes: feature branch -> develop, or develop -> master (promotion).

## Pipeline

Execute these steps in order. Stop immediately if any gating step fails.

### Step 1: Detect Merge Direction

1. Run `git rev-parse --abbrev-ref HEAD`.
2. Determine mode:
   - **feature/\* or any non-develop/non-master/non-platform branch**: feature -> develop
   - **develop**: develop -> master (promotion)
   - **platform/harmony**: STOP. "Platform branches do not merge back. Use `/ship` to push directly."
   - **master**: STOP. "Cannot merge from master. Switch to develop or a feature branch."

If the user passed an argument (e.g., `/merge develop`, `/merge feature/foo`), use that to override detection.

### Step 2: Preflight Checks

#### Common checks (both modes)

1. Working tree is clean: `git status --porcelain` must be empty.
   - If dirty: STOP. "Uncommitted changes. Use `/ship` first."
2. Local branch is up to date with remote: `git fetch origin && git rev-list HEAD..origin/{branch} --count` == 0.
   - If behind: warn, suggest `git pull`.

#### Feature -> develop mode

3. Feature branch has commits ahead of develop: `git rev-list develop..HEAD --count` > 0.
   - If none: STOP. "No commits to merge."
4. Check if PR already exists: `gh pr list --head {branch} --base develop --json number,url`.
   - If exists: report PR URL, check CI status with `gh pr checks {number}`.

#### Develop -> master (promotion) mode

3. Develop has commits ahead of master: `git rev-list master..develop --count` > 0.
   - If none: STOP. "develop is already up to date with master."
4. Check if promotion PR already exists: `gh pr list --head develop --base master --json number,url`.
   - If exists: report PR URL, check CI status.

### Step 3: Gather Context

#### Feature -> develop

1. Commit summary: `git log develop..HEAD --oneline --no-merges`
2. Files changed: `git diff develop..HEAD --name-only`
3. Module impact: classify changed files by module (`app-core/`, `data/*`, `domain/*`, `core/*`, `docs/`, etc.)
4. CI expectations: which workflows will trigger based on changed paths

#### Develop -> master (promotion)

1. Commit summary: `git log master..develop --oneline --no-merges | head -20`
2. Merge count: `git log master..develop --oneline --merges | wc -l`
3. Commit count: `git log master..develop --oneline --no-merges | wc -l`

### Step 4: Produce Handoff

#### Feature -> develop mode

Output:

```
## Codex Merge Handoff: Feature -> Develop

### Branch
- Source: {feature branch name}
- Target: develop
- Commits ahead: {N}

### Changed Files
{grouped by module}

### PR
- Title: {from branch name or commit summary, under 70 chars}
- Base: develop
- Body:
  ## Summary
  {2-3 bullets}

  ## Test plan
  - [ ] CI passes (android-tests, platform-governance-check)
  - [ ] {feature-specific checks}

### CI Impact
- android-tests.yml: {will trigger | will not trigger}
- platform-governance-check.yml: {will trigger | will not trigger}

### Instructions for Codex
1. Push branch: git push -u origin {branch}
2. Create PR: gh pr create --base develop --head {branch} --title "{title}" --body "{body}"
3. After CI passes: gh pr merge {number} --merge --delete-branch
```

#### Develop -> master (promotion) mode

Output:

```
## Codex Merge Handoff: Promotion

### Summary
- Commits to promote: {N commits, M merges}
- Notable commits:
  {top 10 oneline}

### Instructions
Use Codex's `promote-to-master` skill, which handles:
1. Preflight verification
2. PR creation (develop -> master)
3. CI wait
4. Merge (--delete-branch=false to preserve develop)

Manual fallback if promote-to-master is unavailable:
1. gh pr create --base master --head develop --title "promote: develop -> master" --body "{body}"
2. Wait for CI to pass
3. gh pr merge {number} --merge --delete-branch=false
```

#### Post-Merge: Harmony Sync (optional, either mode)

If the user requests, append:

```
### Harmony Sync
git checkout platform/harmony
git merge develop --no-ff -m "merge: sync shared contracts from develop"
git push origin platform/harmony
git checkout develop
```

### Step 5: Report

Print:
- Merge direction (feature -> develop, or develop -> master)
- PR URL if one already exists, with CI status
- Next action for the user

## Hard Rules

- Never merge directly via `git merge` on master -- always through GitHub PR.
- Never force-push to master.
- Never delete the `develop` branch.
- Never merge `platform/harmony` back into develop.
- For `harmony/*` feature branches, use `/merge-harmony` (targets `platform/harmony`), not this command.
- If working tree is dirty, refuse and redirect to `/ship`.
- If CI fails, report the failure -- do not bypass.
- This skill is read-only for git write operations. Codex executes the handoff.
