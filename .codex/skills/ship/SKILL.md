---
name: ship
description: Peer-review a sprint-close diff and execute git operations. Never touches master -- only merge and promote-to-master touch master through PRs. Use when Claude hands off work via /ship or when the user asks to ship a closeout-ready sprint.
---

# Ship

Ship a closeout-ready sprint contract. Codex is the git operator.

## Inputs

Read:
- the active sprint contract path
- `docs/specs/sprint-contract.md`
- `docs/specs/ship-time-checks.md`

If the contract path is missing, ask once and stop.

## Step 1: Confirm sprint-close state

The contract is the source of truth.

- Scope comes from the contract `Scope` block.
- `/ship` runs only after every Success Exit Criterion is green.
- If the operator is mid-iteration and Closeout is not filled, halt.
- One commit per sprint close. No mid-sprint commits. See `docs/specs/sprint-contract.md` §Commit Discipline.

Gather branch context:
- `git rev-parse --abbrev-ref HEAD`
- `git status --porcelain`

Stop on:
- missing contract path
- missing closeout-ready state
- `master`
- no in-scope changes

## Step 2: Light review

Review only files matching the contract Scope.

Hard blockers:
1. Secrets, credentials, or `local.properties` content in scope.
2. Broken imports or missing-file references in scope.
3. Reverse-dependency hits from out-of-scope dirt into scoped files.
4. Lane-scope incoherence under `docs/specs/ship-time-checks.md`.

Out-of-scope dirt:
- leave it unstaged
- do not widen Scope to include it
- still check whether it depends on the files being shipped

## Step 3: Decide action by branch

Branch decides the post-commit action:

| Branch | Action |
|---|---|
| `develop` | stage + commit + push |
| `feature/*` | stage + commit + push + PR to `develop` |
| `platform/harmony` | stage + commit + push |
| `master` | STOP |
| other | ask once |

Never touches `master`.

## Step 4: Stage and commit

Stage only files matching the contract Scope block.

Rules:
- Treat Scope entries as explicit paths, globs, or module slices per `docs/specs/sprint-contract.md` §3.
- Never use `git add -A` or `git add .`.
- Never widen beyond Scope even if other dirty files look related.
- Commit message should reference the contract file path.

## Step 5: Push and PR

- Push the current branch.
- On a feature branch, create the PR to `develop`.
- On `develop` or `platform/harmony`, push directly.

## Report

Print:
- review verdict
- contract path
- staged scope
- commit hash and first line
- push target
- PR URL when created

## Rules

- Never touch `master`.
- Never run mid-sprint.
- Never commit files outside the contract Scope.
- Never bypass blockers from `docs/specs/ship-time-checks.md`.
