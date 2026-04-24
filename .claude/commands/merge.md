PR creation and merge orchestration. This is the only path that reaches `master`, and only through PRs.

Primary path: produce a Codex handoff for feature -> `develop` or `develop` -> `master` promotion.
Backup path: if Codex is unavailable, Claude executes the same steps directly.

## Pipeline

### Step 1: Detect merge direction

1. Run `git rev-parse --abbrev-ref HEAD`.
2. Determine mode:
   - feature or other non-`develop`/`master`/`platform/harmony` branch -> feature -> `develop`
   - `develop` -> promotion to `master`
   - `platform/harmony` -> STOP
   - `master` -> STOP

### Step 2: Clean-worktree gate

Run:
- `git status --porcelain`
- `git fetch origin`

If `git status --porcelain` is not empty, STOP:
"Worktree is dirty. Finish the active sprint contract via `/ship`, or abandon with `git restore`. See `docs/specs/sprint-contract.md` §Commit Discipline."

### Step 3: Preflight

Feature -> `develop`:
- verify commits exist ahead of `develop`
- check whether a PR already exists
- collect commit summary and changed files for the handoff

`develop` -> `master` promotion:
- verify `develop` is ahead of `master`
- check whether a promotion PR already exists
- route to `promote-to-master`

### Step 4: Produce handoff

Feature -> `develop` handoff includes:
- source branch
- target `develop`
- commit summary
- changed files
- PR title/body
- CI wait instruction
- merge instruction with branch deletion

Promotion handoff includes:
- `develop` -> `master`
- notable commits
- instruction to use `promote-to-master`

### Optional Harmony sync

`/merge-harmony` is retired — `develop` -> `platform/harmony` sync is a manual `git merge develop --no-ff` on `platform/harmony`, documented here only as an optional post-merge step.

## Rules

- Keep the worktree clean before merge.
- Never merge `platform/harmony` back into `develop`.
- Never merge directly on `master`.
- Never bypass failed CI.
