---
name: cnp
description: Commit and Push the current working tree to the current branch. Use when the user says /cnp. Atomic save-point — stages all changes, commits with a generated message, and pushes to origin. Does NOT switch branches, merge, or create PRs.
---

# cnp — Commit and Push

A dumb, predictable save button. Stages everything in the working tree, commits on the current branch, pushes to origin. That's it.

Use this so the working tree is clean before invoking `merge`, `ship`, or `promote-to-master` (which all refuse on dirty trees).

## Step 1: Preflight

1. Confirm we're in a git repo:
   ```bash
   git rev-parse --is-inside-work-tree
   ```

2. Read current branch:
   ```bash
   BRANCH=$(git rev-parse --abbrev-ref HEAD)
   ```

3. Refuse on protected branches unless user explicitly confirms:
   - `master` — STOP. "Cannot cnp directly to master. Use a feature branch."
   - `develop` — WARN. Ask user to confirm; develop should normally receive changes via `merge`.
   - `platform/harmony` — WARN. Same reasoning.

4. Check there is something to commit:
   ```bash
   git status --porcelain
   ```
   If empty AND no unpushed commits exist (`git rev-list @{u}..HEAD --count` returns 0): STOP. "Nothing to commit or push."

## Step 2: Stage

Stage all changes including untracked files:
```bash
git add -A
```

Re-check what is staged:
```bash
git diff --cached --stat
git diff --cached --name-only
```

Refuse to commit files that look sensitive: `local.properties`, `*.jks`, `*.keystore`, `google-services.json`, anything matching `*.local.*`. If found in staged set: unstage just those, warn user, continue.

## Step 3: Commit

Generate a concise commit message from the staged diff:
- Subject line under 70 chars, imperative mood ("add X", "fix Y", "update Z").
- If changes span >3 files or >2 logical areas, add a short body with bullets.
- Do NOT include co-author trailers, do NOT mention the agent.

```bash
git commit -m "<generated subject>" [-m "<optional body>"]
```

If pre-commit hook fails: report the failure verbatim and STOP. Do not retry with `--no-verify`. Do not amend.

## Step 4: Push

```bash
git push -u origin "$BRANCH"
```

If the branch has no upstream, `-u` sets it. If push is rejected (non-fast-forward): STOP. "Remote has diverged. Pull or rebase first — do not force-push from cnp."

## Step 5: Report

Print:
- Branch
- Commit SHA + subject
- Push result (new branch / fast-forward / up-to-date)

## Rules

- This skill never switches branches.
- This skill never merges, rebases, or creates PRs.
- This skill never force-pushes.
- This skill never bypasses hooks (`--no-verify`, `--no-gpg-sign`).
- Sensitive-file refusal is non-negotiable; unstage and warn, never commit them.
- On `master` / `develop` / `platform/harmony`, require explicit user confirmation before committing — these branches normally receive changes via PR/merge skills, not direct `cnp`.
