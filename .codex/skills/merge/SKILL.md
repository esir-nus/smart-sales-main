---
name: merge
description: End-of-day Android sweep. Use when the user says /merge. Finds every Android feature branch ahead of develop, opens a PR if one doesn't exist, waits for CI, merges into develop, and deletes the merged branch. Excludes platform lanes (platform/harmony, ios/*). For develop -> master, use promote-to-master.
---

# merge — End-of-day Android sweep

Sweep all Android feature branches into `develop` in one pass. One confirmation up front, then unattended per-branch PR + CI + merge + delete.

This skill is **Android-only**. Harmony and iOS work commits directly to their respective lane branches (`platform/harmony`, `ios/*`) and is **never** swept by this skill.

For `develop -> master` promotion (publish), use the `promote-to-master` skill instead.

## Step 1: Preflight

1. Working tree must be clean:
   ```bash
   git status --porcelain
   ```
   If dirty: STOP. List the dirty files. Tell the user to `/cnp` (which will land them on a feature branch) before re-running `/merge`.

2. Fetch:
   ```bash
   git fetch --all --prune
   ```

3. Current branch:
   - On `master`: STOP. "Cannot run from master. Checkout develop first."
   - On `platform/harmony` or `ios/*`: STOP. "merge is Android-only. Platform lanes don't sweep — they commit directly."
   - On `develop`: continue.
   - On a feature branch: warn that the sweep operates from `develop`; offer to checkout develop and continue.

## Step 2: Build the sweep queue

Find every branch (local OR remote) that is ahead of `origin/develop` and qualifies as an Android feature branch.

```bash
# Candidate branches: ahead of origin/develop
# (use both local refs and origin/* refs, then dedupe by short name)
```

Inclusion rule: ahead of `origin/develop` AND **not** in any of:
- `master`
- `develop`
- `platform/harmony`
- anything matching `harmony/*`
- anything matching `ios/*`
- anything matching `release/*`

Practical recipe:

```bash
git for-each-ref --format='%(refname:short)' refs/heads refs/remotes/origin \
  | sed 's#^origin/##' \
  | sort -u \
  | grep -Ev '^(master|develop|platform/harmony|HEAD)$' \
  | grep -Ev '^(harmony/|ios/|release/)' \
  | while read b; do
      ahead=$(git rev-list --count origin/develop..origin/"$b" 2>/dev/null \
              || git rev-list --count origin/develop.."$b" 2>/dev/null)
      [ "${ahead:-0}" -gt 0 ] && echo "$b $ahead"
    done
```

For each candidate, gather:
- branch name
- commits ahead of develop
- existing PR (if any) targeting `develop`:
  ```bash
  gh pr list --head "$b" --base develop --state open --json number,url,isDraft,mergeable
  ```

If queue is empty: report "Nothing to sweep" and STOP.

## Step 3: Confirm the queue

Show the user one summary:

```
Sweep queue (Android → develop):
  1. feat/foo            3 commits   PR #123 (open)
  2. feat/bar            1 commit    no PR yet (will create)
  3. fix/baz             5 commits   PR #119 (draft — SKIPPED)
Proceed with 1 and 2? [Y/n]
```

- Drafts are skipped (reported but not merged).
- Single Y/N for the whole queue. No per-branch prompts after this.
- If user declines: STOP.

## Step 4: Per-branch sweep loop

For each accepted branch in queue order:

1. Checkout and fast-forward:
   ```bash
   git checkout "$b"
   git pull --ff-only origin "$b" || { echo "non-FF on $b"; STOP; }
   ```

2. Ensure pushed:
   ```bash
   git push -u origin "$b"
   ```

3. Open PR if missing:
   ```bash
   COMMITS=$(git log develop.."$b" --oneline --no-merges)
   COUNT=$(git log develop.."$b" --oneline --no-merges | wc -l)
   FILES=$(git diff develop.."$b" --name-only)
   gh pr create --base develop --head "$b" \
     --title "<title from branch / commits, ≤70 chars>" \
     --body "$(cat <<EOF
## Summary
<2-3 bullets from commit summary>

**$COUNT commits** on this branch.

### Changed files
$FILES

## Test plan
- [ ] CI passes (android-tests, platform-governance-check)
- [ ] No regressions in existing functionality
EOF
)"
   ```

4. Wait for CI:
   ```bash
   gh pr checks "$PR" --watch
   ```
   - Pass → continue.
   - Fail → STOP the sweep. Report the failing branch + check + URL. Do NOT bypass. Do NOT continue to the next branch without user confirmation.
   - Pending after a reasonable wait → STOP. Report status.

5. Merge:
   ```bash
   gh pr merge "$PR" --merge --delete-branch
   ```

6. Verify:
   ```bash
   gh pr view "$PR" --json state   # must be MERGED
   ```

7. Cleanup local ref:
   ```bash
   git checkout develop
   git pull --ff-only origin develop
   git branch -D "$b" 2>/dev/null || true
   ```

## Step 5: Final report

After the loop:

```
Swept N branches into develop:
  ✓ feat/foo  → PR #123 merged, branch deleted
  ✓ feat/bar  → PR #128 created, merged, branch deleted
Skipped (drafts): fix/baz (#119)
Stopped at: <none | branch + reason>

develop is at <SHA>.
Next: /promote-to-master to publish, or /sync-harmony to flow shared contracts into platform/harmony.
```

## Rules

- Android-only. Never touches `platform/harmony`, `harmony/*`, `ios/*`, `release/*`, or `master`.
- `develop -> master` is publishing, not sweeping. Use `promote-to-master`.
- Never force-push.
- Never merge a PR with failing CI. Stop and report.
- Never bypass hooks or `gh pr merge` admin overrides.
- Always `--delete-branch` on merge — feature branches are short-lived.
- Working tree must be clean before sweep starts; refuse otherwise.
- Drafts are skipped, not merged.
- One confirmation up front; then unattended unless something fails.
