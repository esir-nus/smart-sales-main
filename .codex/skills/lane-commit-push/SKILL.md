---
name: lane-commit-push
description: Use when the user wants to commit, push, or ship work safely from the current lane while parallel agents or worktrees are active. This skill makes Codex use the repo-owned lane harness commands (`scripts/lane status|commit|push|ship`) instead of improvising raw git commit/push logic, and it relies on `ops/lane-registry.json`, `docs/sops/lane-worktree-governance.md`, and `handoffs/README.md` as the shared control plane.
---

# Lane Commit Push

Use this skill when the user asks to commit, push, ship, or safely publish the current lane.

## Core Rule

Do not invent ad-hoc git commit/push behavior.

Always prefer the repo-owned lane harness commands:

- `scripts/lane status`
- `scripts/lane commit`
- `scripts/lane push`
- `scripts/lane ship`

## Read Before Acting

Read only what you need:

1. `ops/lane-registry.json`
2. `docs/sops/lane-worktree-governance.md`
3. `handoffs/README.md` when paused/resumable state matters

## Workflow

1. Run `scripts/lane status` first to confirm current worktree, branch, attached lane, staged files, and generated commit message.
2. If the command fails, fix the harness violation instead of bypassing it with raw git.
3. Use `scripts/lane commit` for commit-only requests.
4. Use `scripts/lane push` for push-only requests.
5. Use `scripts/lane ship` when the user wants the full commit + push path.

## Safety Rules

- Never run lane-local commit/push from the integration tree.
- Never override the detected lane/branch unless the user explicitly asks to repair the registry or lease.
- Prefer `--message` only when the user provides an exact commit message or wants a specific override.
- If the harness blocks the action, report the exact block reason and the next safe repair step.
