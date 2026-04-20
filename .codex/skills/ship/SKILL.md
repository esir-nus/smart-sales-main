---
name: ship
description: Peer-review a diff and execute git operations (stage, commit, push, PR to develop). Never touches master -- only merge and promote-to-master touch master through PRs. Use when Claude hands off work via /codex-handoff or /ship, or when the user says /ship.
---

# Ship

Peer-review the current diff, then land work on `develop` or a feature branch. Never touches `master` -- only `/merge` and `promote-to-master` touch master through PRs.

Codex is the git operator. Claude (or the user) prepares the changes; Codex reviews and ships.

## Step 1: Understand the handoff

If a Claude handoff block is present in the conversation, use it as context (branch, changed files, suggested commit message, PR details).

If no handoff block exists, gather context yourself:

1. `git rev-parse --abbrev-ref HEAD`
2. `git status --porcelain`
3. `git diff --name-only` (unstaged) and `git diff --cached --name-only` (staged)

## Step 2: Peer review the diff

Review the changes from a fresh perspective. Run `git diff` (or `git diff --cached` for staged files) and check:

1. **Correctness**: Does the code do what the commit message / handoff scope says it does?
2. **Regressions**: Are there obvious deletions of needed code, broken imports, or missing files?
3. **Boundary violations**: Does `domain/` import `android.*`? Do Harmony artifacts appear in `app/`, `app-core/`, `core/`, `data/`, or `domain/`?
4. **Secrets**: Are there API keys, tokens, or credentials in the diff? Check for `local.properties` content, hardcoded keys, `.env` values.
5. **Scope creep**: Are there changes that don't belong to the stated scope?

If issues are found:
- **Blocking** (secrets, broken imports, boundary violations): STOP. Report the issue. Do not ship.
- **Advisory** (style nits, minor scope creep): Note them but proceed if the user confirms.

## Step 3: Determine the action

Based on the current branch:

| Branch | Action |
|---|---|
| `develop` | stage + commit + push |
| `feature/*` or `harmony/*` | stage + commit + push + create PR to develop |
| `platform/*` | STOP. "Platform branches are shipping snapshots. Switch to develop or create a feature branch from develop." |
| `master` | STOP. Never commit directly to master. |
| other | ask the user to confirm |

## Step 4: Stage and commit

1. Stage specific files only. Never use `git add -A` or `git add .`.
   ```bash
   git add path/to/file1 path/to/file2
   ```

2. Use the commit message from the handoff block if provided. Otherwise, draft a conventional commit message:
   - Prefix: `feat` | `fix` | `refactor` | `docs` | `chore` | `test`
   - Scope: from file paths (e.g., `connectivity`, `scheduler`, `shell`)
   - First line under 72 characters

3. Commit with co-author line:
   ```bash
   git commit -m "$(cat <<'EOF'
   {prefix}({scope}): {description}

   Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
   EOF
   )"
   ```

## Step 5: Push

```bash
# If branch has no upstream yet:
git push -u origin {branch}

# If branch already tracks remote:
git push
```

## Step 6: Create PR (feature branches only)

If on a feature branch, create a PR to develop:

```bash
gh pr create --base develop --head {branch} \
  --title "{title}" \
  --body "$(cat <<'EOF'
## Summary
{2-3 bullets}

## Test plan
- [ ] CI passes (android-tests, platform-governance-check)
- [ ] {feature-specific checks}
EOF
)"
```

## Step 7: Report

Print:
- Review verdict: PASS or issues found
- Commit: `{hash} {first line}`
- Push: `origin/{branch}`
- PR URL (if created)

## Rules

- `/ship` never touches master. Only `/merge` and `promote-to-master` touch master through PRs.
- Never commit or push to master directly.
- Never force-push to master or develop.
- Never use `git add -A` or `git add .` — stage specific files.
- Never commit files that contain secrets (`.env`, `local.properties`, credentials).
- If the review finds blocking issues, stop and report — do not ship.
- If no changes to commit, say so and stop.
