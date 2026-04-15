---
description: Safely commit and push the current lane through the repo-owned lane harness
---

# Lane Ship Workflow

Use this workflow when the operator wants to commit, push, or ship the current lane without getting lost across parallel branches and worktrees.

## Required Control Plane

Read these first:

- `ops/lane-registry.json`
- `docs/sops/lane-worktree-governance.md`
- `handoffs/README.md` when lane pause/resume state matters

## Execution Rule

Do not improvise raw `git commit` / `git push` logic when the lane harness can answer the same need.

Prefer these repo commands:

- `scripts/lane status`
- `scripts/lane commit`
- `scripts/lane push`
- `scripts/lane ship`

## Workflow

1. Run `scripts/lane status`.
2. Confirm the reported worktree, branch, lane id, staged set, and generated message match the operator intent.
3. If the request is commit-only, run `scripts/lane commit`.
4. If the request is push-only, run `scripts/lane push`.
5. If the request is full publication, run `scripts/lane ship`.

## Fail-Closed Rule

If the lane harness fails, stop and report:

- current branch
- current lane
- blocking path or registry mismatch
- next safe repair action

Do not bypass the harness unless the operator explicitly asks to repair the harness itself.
