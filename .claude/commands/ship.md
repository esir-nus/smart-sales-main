Ship current work from an active sprint contract. Never touches `master`; only `/merge` and `promote-to-master` reach `master` through PRs.

Primary path: produce a structured handoff block for Codex.
Backup path: if Codex is unavailable, Claude executes the same git steps directly.

## Pipeline

### Step 1: Preflight

1. Read the active sprint contract file path first. If it is missing, stop.
2. Read `docs/specs/sprint-contract.md` and `docs/specs/ship-time-checks.md`.
3. Run:
   - `git rev-parse --abbrev-ref HEAD`
   - `git status --porcelain`
   - `git diff --name-only`
   - `git diff --cached --name-only`
4. If the contract Closeout is not closeout-ready or any Success Exit Criterion is still open, stop: `/ship` does not run mid-sprint.
5. If no in-scope changes exist, stop: "Nothing to ship."

### Step 2: Derive ship scope from the contract

Use the contract's `Scope` block as the authoritative path set for this ship.

- Treat Scope entries as explicit paths, globs, or module slices per `docs/specs/sprint-contract.md`.
- Stage only files matching that Scope set.
- Never use `git add -A` or `git add .`.
- Dirty files outside Scope remain unstaged out-of-scope dirt.
- Out-of-scope dirt still participates in reverse-dependency review.

### Step 3: Validate

Run the narrow ship-time review from `docs/specs/ship-time-checks.md`:

1. Secrets or credentials in staged scope: blocker.
2. Broken imports or missing-file references inside staged scope: blocker.
3. Reverse-dependency check from out-of-scope dirt into staged scope: blocker.
4. Lane-scope coherence against the current branch and contract scope: blocker.

Branch action:
- `develop`: commit + push
- `feature/*`: commit + push + PR to `develop`
- `platform/harmony`: commit + push
- `master`: STOP
- other: warn and ask the user to confirm

### Step 4: Produce handoff block

Output:

```markdown
## Codex Ship Handoff

### Contract
- Path: {active contract path}
- Scope: {contract Scope summary}
- Closeout ready: yes

### Branch
- Current: {branch name}
- Action: {commit + push | commit + push + PR to develop | commit + push}

### Changed Files
{only files matching contract Scope}

### Review
- Ship-time checks: {PASS | BLOCKED}
- Out-of-scope dirt: {none | summary}

### Commit
- Message: {references the contract path}

### Instructions for Codex
1. Stage only the Scope files.
2. Commit once for sprint close.
3. Push.
4. If on a feature branch, open the PR to `develop`.
```

## Rules

- The active contract path is the primary input.
- `/ship` runs only for a closeout-ready sprint.
- Stage only files matching the contract Scope.
- Never widen the commit beyond Scope.
- Never touch `master`.
