---
name: ship
description: Peer-review a diff and execute git operations (stage, commit, push, PR to develop). Never touches master -- only merge and promote-to-master touch master through PRs. Use when Claude hands off work via /codex-handoff or /ship, or when the user says /ship.
---

# Ship

Peer-review the current diff, then land work on `develop` or a feature branch. Never touches `master` -- only `/merge` and `promote-to-master` touch master through PRs.

Codex is the git operator. Claude (or the user) prepares the changes; Codex reviews and ships.

This skill follows the declaration-first shipping contract. See `docs/specs/declaration-first-shipping.md` for the governing philosophy: friction belongs upfront (at declaration and task start), not at ship time. `/ship` enforces only what the earlier gates cannot.

## Step 1: Read the handoff summary

The operator's ship summary is trusted. Read lane + scope + test results from it and proceed — do not re-ask or ask the operator to re-confirm. Expected fields:

- **Lane**: `android`, `harmony`, or `docs`
- **Intent**: `dataflow`, `cosmetic`, or `hybrid` (from the originating sprint contract; see `.claude/commands/sprint.md` Step 2)
- **Ship Scope**: explicit file list or module slice
- **Verification Scope**: tests/build/device evidence the operator already ran
- **Device-loop evidence**: required when the originating sprint contract declared `Device loop: required` (L3 on `lane: android` or `lane: harmony`). See `docs/specs/device-loop-protocol.md` §6 for the schema.
- **Out-of-Scope Files**: optional

If the summary is genuinely missing lane, intent, or scope, ask once. Otherwise trust it.

### Auto-split for docs files

Scan the dirty tree against the declared Ship Scope:

- Any file under `docs/`, `CLAUDE.md`, `AGENTS.md`, `SmartSales_PRD.md`, `CHANGELOG.md`, or other repo-root markdown is **auto-routed to a `docs` lane split**.
- If the operator declared `lane: android` or `lane: harmony` and the Ship Scope includes doc files, silently peel them out into a second `docs` lane ship targeting `develop`. Do not ask — this is automatic.
- If the dirty tree contains out-of-scope doc files the operator did not declare, include them in the auto-split `docs` ship only if the operator's summary implies they belong (e.g., they are explicitly listed or referenced). Otherwise leave them as unrelated dirt.

Result: up to two ships per invocation — one platform lane (if declared) and one `docs` lane (if docs touched).

Gather branch context for reporting:

1. `git rev-parse --abbrev-ref HEAD`
2. `git status --porcelain`

## Step 2: Light review + verification

Trust the operator's review and test evidence. Do a light sanity pass on declared scope files only:

1. **Secrets**: API keys, tokens, `local.properties` content, hardcoded credentials. (Hard blocker.)
2. **Lane-scope coherence**:
   - `lane: android` → no `platforms/harmony/` files (docs auto-split already handled)
   - `lane: harmony` → only `platforms/harmony/` or shared contract paths from `docs/cerb/interface-map.md`
   - `lane: docs` → only docs + repo-root markdown; no code
3. **Broken imports / missing files** (grep-level check, not full review).
4. **Reverse-dependency check**: for each dirty out-of-scope file, grep for imports of shipped files. A hit is a blocker.
5. **Intent-vs-diff coherence** (see `.claude/commands/sprint.md` Step 2 for the intent axis):
   - `intent: cosmetic` → diff must NOT touch any file matching `**/*ViewModel.kt`, `**/*Repository.kt`, `**/repository/**`, `**/data/**`, `**/domain/**`, `**/flow/**`, `**/*Mapper.kt`, `**/*UseCase.kt`, or equivalent ArkTS state/service files. Any hit is a blocker — re-declare as `dataflow`.
   - `intent: hybrid` → declared file count must be ≤3. Diff file count must not exceed the declared list. Either violation is a blocker — split into `dataflow` + `cosmetic`.
   - `intent: cosmetic | hybrid` declared for net-new feature work → blocker. New features must enter as `dataflow`. Signal: handoff scope summary uses words like "add", "new", "introduce" combined with a non-existing-before surface. When ambiguous, ask once.
   - `intent: cosmetic` → diff must NOT add new UI element registrations to `docs/specs/ui_element_registry.md`. Restyling registered elements is allowed; adding new ones requires a `dataflow` sprint.

Re-run the declared tests from Verification Scope to confirm they still pass after any scope-splitting. For `lane: docs` with no tests declared, skip.

Blockers (halt ship):
- secrets
- lane-scope incoherence
- broken imports inside declared scope
- reverse-dependency hits from out-of-scope dirt
- declared tests fail on re-run
- **Device-loop evidence missing on L3 sprint**: if the sprint contract declared `Device loop: required` and the handoff lacks the schema fields from `docs/specs/device-loop-protocol.md` §6 (device id, signed artifact, per-joint log excerpts for both first pass and cold relaunch, UI states verified, deferred list), halt ship and ask the operator to run the device loop and attach evidence. Compile success is not a substitute.
- **Intent-vs-diff violation** (cosmetic touches dataflow files; hybrid exceeds 3-file cap; cosmetic/hybrid declared for net-new feature; cosmetic registers new UI elements). See Step 2 rule 5 above.

Non-blockers (report in final output, do not halt):
- branch name mismatch with declared lane
- unrelated dirty files outside scope with no reverse-dep
- cosmetic/style issues

Do NOT re-do the operator's full review. The handoff summary is the primary evidence; this step is just verification the ship won't break the tree.

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
2. Append a trace entry to `docs/plans/changelog.md` with: date, lane, **intent** (`dataflow | cosmetic | hybrid`), title, behavior summary, verification summary, branch, ignored-dirt summary, any `--force-parallel` reasons from the originating task. This is automated — do not ask the operator to fill it in. The `intent` field feeds the hybrid-frequency drift signal in `/sprint` Step 2.

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
- L3 sprints on `lane: android` or `lane: harmony` ship only with device-loop evidence per `docs/specs/device-loop-protocol.md`. No exceptions; missing evidence is a hard blocker, not a non-blocker.
- `intent` is mandatory in every handoff (`dataflow | cosmetic | hybrid`). Intent-vs-diff violations (cosmetic touching dataflow files, hybrid exceeding 3-file cap, cosmetic/hybrid declared for net-new work, cosmetic adding new registry entries) are hard blockers per `.claude/commands/sprint.md` Step 2.
- If no changes to commit in declared scope, say so and stop.
