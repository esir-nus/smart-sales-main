---
name: ship
description: Peer-review a diff and execute git operations (stage, commit, push, PR to develop). Never touches master -- only merge and promote-to-master touch master through PRs. Use when Claude hands off work via /codex-handoff or /ship, or when the user says /ship.
---

# Ship

Peer-review the current diff, then land work on `develop` or a feature branch. Never touches `master` -- only `/merge` and `promote-to-master` touch master through PRs.

Codex is the git operator. Claude (or the user) prepares the changes; Codex reviews and ships.

This skill follows the declaration-first shipping contract. See `docs/specs/declaration-first-shipping.md` for the governing philosophy: friction belongs upfront (at declaration and task start), not at ship time. `/ship` enforces only what the earlier gates cannot.

## Step 1: Understand the handoff

Every `/ship` invocation must carry a declaration. Expected fields:

- **Lane**: `android`, `harmony`, or `docs`
- **Ship Scope**: explicit file list or module slice
- **Out-of-Scope Files**: optional
- **Verification Scope**: tests/build/device evidence (for `docs` lane: doc-render check or "n/a")

If a Claude handoff block is present, read these from it. If missing, ask the operator before proceeding — do not infer from `git status`. The declaration is the source of truth; the dirty tree is context.

Gather branch context for reporting:

1. `git rev-parse --abbrev-ref HEAD`
2. `git status --porcelain`
3. `git diff --name-only` and `git diff --cached --name-only`

## Step 2: Peer review the declared scope

Review ONLY files in the declared Ship Scope. Run `git diff -- <scope files>` and check:

1. **Correctness**: Does the code match the declared behavior?
2. **Regressions**: Broken imports, missing files, obvious deletions of needed code.
3. **Boundary violations**: `domain/` importing `android.*`? Harmony artifacts under `app/`, `app-core/`, `core/`, `data/`, or `domain/`?
4. **Secrets**: API keys, tokens, `local.properties` content, hardcoded credentials.
5. **Lane-scope coherence**:
   - `lane: android` → no files under `platforms/harmony/`, no files under `docs/` or repo-root markdown
   - `lane: harmony` → only `platforms/harmony/` or shared contract paths from `docs/cerb/interface-map.md`; no `docs/` or repo-root markdown
   - `lane: docs` → only `docs/`, `CLAUDE.md`, `AGENTS.md`, `SmartSales_PRD.md`, `CHANGELOG.md`, other repo-root markdown; no code; must ship to `develop`
   - all declared files exist; Ship Scope non-empty
6. **Reverse-dependency check**: for each dirty out-of-scope file, grep for imports of shipped files. A hit means shipping the declared scope would leave broken references in the worktree.

Blockers (halt ship):
- any of the above inside declared scope
- reverse-dependency hits from out-of-scope dirt

Non-blockers (report in final output, do not halt):
- branch name mismatch with declared lane (e.g., shipping `android` from `platform/harmony`)
- unrelated dirty files outside scope with no reverse-dependency on shipped files
- cosmetic/style issues

Do NOT widen review to the full dirty tree. Out-of-scope dirt is inspected only for reverse-dependency and reported as context.

## Step 3: Determine the action

Branch is advisory under the declaration-first contract — the declared lane decides what ships, not the branch name. Branch still determines the post-commit action:

| Branch | Action |
|---|---|
| `develop` | stage + commit + push |
| `feature/*` | stage + commit + push + create PR to develop |
| `platform/harmony` | stage + commit + push |
| `master` | STOP. Never commit directly to master. |
| other | ask the user to confirm |

Lane/branch mismatch (e.g., `lane: android` on `platform/harmony`) is a non-blocker — report it, do not halt.

## Step 4: Stage and commit

1. Stage ONLY files in the declared Ship Scope. Never `git add -A` or `git add .`, and never widen to other dirty files even if they look related.
   ```bash
   git add <declared-scope-files>
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

## Step 7: Update active-lanes registry and trace log

After a successful push (and PR creation if applicable):

1. Remove the shipped task's entry from `docs/plans/active-lanes.md`.
2. Append a trace entry to `docs/plans/changelog.md` with: date, lane, title, behavior summary, verification summary, branch, ignored-dirt summary, any `--force-parallel` reasons from the originating task. This is automated — do not ask the operator to fill it in.

## Step 8: Report

Print:
- Review verdict: PASS or blockers found
- Declared lane and Ship Scope
- Commit: `{hash} {first line}`
- Push: `origin/{branch}`
- PR URL (if created)
- Non-blockers surfaced: branch mismatch, unrelated dirt summary

## Rules

- `/ship` never touches master. Only `/merge` and `promote-to-master` touch master through PRs.
- Never commit or push to master directly.
- Never force-push to master or develop.
- Never use `git add -A` or `git add .` — stage only declared Ship Scope files.
- Never commit files that contain secrets (`.env`, `local.properties`, credentials).
- Never widen the commit beyond the declared Ship Scope, even if other dirty files look related.
- If a blocker is found inside declared scope or via reverse-dependency, stop and report — do not ship.
- Non-blockers (branch mismatch, unrelated dirt) are reported, not gated.
- If no handoff declaration is present, ask the operator — do not infer lane/scope from `git status`.
- If no changes to commit in declared scope, say so and stop.
