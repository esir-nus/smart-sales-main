---
name: merge
description: Autonomous end-of-day ANDROID-ONLY sweep. Use when the user says /merge. Finds every Android feature branch ahead of develop, opens a PR if missing, waits for CI, merges into develop, deletes the branch. Self-heals common refusals (non-FF, lockfile conflicts, flaky CI, mergeable=UNKNOWN). HARD EXCLUDES all non-Android work - platform/harmony, harmony/*, ios/*, release/*, and Harmony/iOS native files are never touched under any circumstance. For develop -> master, use promote-to-master.
---

# merge — Autonomous Android sweep

Sweep every Android feature branch into `develop`. **Fully autonomous after the initial queue confirmation** — the agent fixes common refusals itself instead of stopping. The user should be able to invoke `/merge`, walk away, and come back to a clean develop.

This skill is **Android-only**. Harmony and iOS work commits directly to their respective lane branches (`platform/harmony`, `ios/*`) and is **never** swept here.

For `develop -> master` promotion (publish), use the `promote-to-master` skill instead.

## Operating principle

Treat each candidate branch as "should be mergeable." When GitHub or git refuses, **diagnose and fix in-place**, then retry. Only escalate to the user when a fix is genuinely outside the skill's authority (real code conflict that needs human judgment, CI failure that's a real bug, protected-branch refusal, auth issue).

The skill keeps a running ledger of what it did and reports once at the end.

## Step 1: Preflight

1. Working tree must be clean:
   ```bash
   git status --porcelain
   ```
   If dirty: STOP. List dirty files. Tell the user to `/cnp` first.

2. Fetch everything:
   ```bash
   git fetch --all --prune
   ```

3. Current branch:
   - On `master`: switch to `develop` automatically.
   - On `platform/harmony` or `ios/*`: STOP. "merge is Android-only. Lane branches commit directly."
   - On `develop`: continue.
   - On a feature branch: checkout `develop` automatically (the sweep operates from develop).

4. Bring `develop` current:
   ```bash
   git checkout develop
   git pull --ff-only origin develop
   ```
   If non-FF on develop itself: STOP. "develop has local commits not on origin — manual review required."

## Step 2: Build the sweep queue

Find every branch (local OR remote) that is ahead of `origin/develop` and qualifies as Android.

Inclusion: ahead of `origin/develop` AND **not** in any of:
- `master`, `develop`, `platform/harmony`, `HEAD`
- matches `harmony/*`, `ios/*`, `release/*`

```bash
git for-each-ref --format='%(refname:short)' refs/heads refs/remotes/origin \
  | sed 's#^origin/##' \
  | sort -u \
  | grep -Ev '^(master|develop|platform/harmony|HEAD)$' \
  | grep -Ev '^(harmony/|ios/|release/)' \
  | while read b; do
      ahead=$(git rev-list --count "origin/develop..origin/$b" 2>/dev/null \
              || git rev-list --count "origin/develop..$b" 2>/dev/null)
      [ "${ahead:-0}" -gt 0 ] && echo "$b $ahead"
    done
```

For each candidate, gather:
- branch name, commits ahead
- existing PR (if any) targeting `develop`:
  ```bash
  gh pr list --head "$b" --base develop --state open --json number,url,isDraft,mergeable,mergeStateStatus
  ```

If queue is empty: report "Nothing to sweep" and STOP.

## Step 3: Confirm the queue (single prompt)

Show the user one summary, then run unattended:

```
Sweep queue (Android → develop):
  1. feat/foo            3 commits   PR #123 (open)
  2. feat/bar            1 commit    no PR yet (will create)
  3. fix/baz             5 commits   PR #119 (DRAFT — skipped)
Proceed with 1 and 2? [Y/n]
```

- Drafts are skipped (reported, not merged).
- Single Y/N for the whole queue. After this, **no further prompts** unless self-healing genuinely fails.
- If user declines: STOP.

## Step 4: Per-branch autonomous sweep

For each accepted branch in queue order, run the loop below. The loop is **stateful per branch** and self-heals.

```
For branch $b with PR $PR (or none):

  attempt = 0
  while attempt < 4:
    attempt++

    [4a] Sync local branch with origin
        git checkout $b
        git fetch origin $b
        git reset --hard origin/$b   # local is throwaway; origin is source of truth
                                     # (only safe because cnp already pushed)

    [4b] Bring branch up to date with develop (in-place rebase)
        git fetch origin develop
        if branch is behind develop:
          try: git rebase origin/develop
          on conflict:
            attempt simple auto-resolve:
              - For lockfiles, regenerated artifacts (yarn.lock, package-lock.json,
                gradle build outputs, generated res files): take "theirs" (develop side)
                via `git checkout --theirs <file> && git add <file>`, continue rebase.
              - For all other files: ABORT rebase (`git rebase --abort`),
                record "real-conflict:<files>", break out of loop.
            after auto-resolve, `git rebase --continue`. If it conflicts again on
            non-trivial files, abort and record real-conflict.
          push the rebased branch:
            git push --force-with-lease origin $b
          (force-with-lease is safe because we just synced from origin in 4a)

    [4c] Open PR if missing
        if no PR exists for $b → develop:
          create with title from branch/commits and body from generated template

    [4d] Refresh PR view
        gh pr view $PR --json mergeable,mergeStateStatus,state,statusCheckRollup,reviewDecision
        - state=MERGED → success, exit loop
        - state=CLOSED (not merged) → record "pr-closed", break

    [4e] Diagnose and act on mergeStateStatus + check rollup

        BLOCKED reasons we self-heal:
          - mergeStateStatus=BEHIND        → continue (4b already rebased; loop)
          - mergeStateStatus=UNKNOWN       → wait 10s, refresh, retry up to 3x within attempt
          - mergeStateStatus=DIRTY (conflict on develop) → loop back to 4b
          - check status PENDING/QUEUED    → `gh pr checks $PR --watch` until terminal
          - check status FAILURE on a known-flaky check (configurable list, default
            includes any check whose name contains "flaky", "intermittent", or which
            failed only at the network layer) → `gh run rerun <run-id>` for that
            check, then `gh pr checks $PR --watch`.
          - mergeStateStatus=BLOCKED with reviewDecision=REVIEW_REQUIRED →
            record "needs-review", break (do not self-approve).
          - mergeStateStatus=BLOCKED with required check missing →
            record "missing-required-check:<name>", break.

        Real failures we escalate (record + break, do not bypass):
          - Real merge conflict on source files
          - CI failure on non-flaky check (real test/lint/build failure)
          - Branch-protection rule that requires human review
          - gh auth / permission errors

    [4f] Merge
        if mergeable=MERGEABLE and all required checks GREEN:
          gh pr merge $PR --merge --delete-branch
          verify: gh pr view $PR --json state  # MERGED
          delete local ref: git checkout develop && git pull --ff-only origin develop
                            && git branch -D $b 2>/dev/null
          exit loop with success.

  if loop exhausted attempts: record "exhausted-retries:<last-state>", continue
  to next branch (do not stop the whole sweep on one stuck branch).
```

After each branch (success or recorded failure), continue to the next one. The sweep does not abort on a single branch's failure — it records and moves on.

## Step 5: Final report

After the loop:

```
Swept N branches into develop:
  ✓ feat/foo  → PR #123 merged, branch deleted
  ✓ feat/bar  → PR #128 created, rebased onto develop, merged, branch deleted
  ✓ feat/qux  → PR #131 self-healed (flaky android-tests rerun), merged

Skipped:
  - fix/baz (#119)  → draft

Needs human attention:
  - feat/big-refactor (#127)  → real-conflict: app-core/src/Foo.kt, Bar.kt
  - chore/lint (#130)         → CI failure: ./gradlew lint (non-flaky)
  - feat/perm (#132)          → blocked: requires code-owner review

develop is at <SHA>.
Next: /promote-to-master to publish, or sync develop into platform/harmony.
```

## Self-heal scope (what the agent IS allowed to do unattended)

- `git fetch`, `git reset --hard origin/<branch>` on the feature branch (local is throwaway after cnp pushed).
- `git rebase origin/develop` on the feature branch.
- Resolve conflicts in **regenerable artifacts only** (lockfiles, build outputs, generated resources) by taking develop's version.
- `git push --force-with-lease` after a clean rebase. Never plain `--force`.
- Re-run flaky CI checks via `gh run rerun`.
- Wait/poll for `mergeStateStatus=UNKNOWN` to resolve.
- Create the PR if missing.

## What the agent is NOT allowed to do

- Never resolve conflicts in source files (`.kt`, `.java`, `.ets`, `.ts`, `.tsx`, `.json5`, etc.) — those go to the human.
- Never `--no-verify`, never bypass branch protection, never `gh pr merge --admin`.
- Never force-push to `develop`, `master`, `platform/harmony`.
- Never approve or self-approve PRs.
- Never merge a PR with non-flaky CI failure.
- Never delete `develop`, `master`, `platform/harmony`, or any branch the loop didn't successfully merge.

## Hard rule: Android-only scope (NON-NEGOTIABLE)

This skill **only ever touches Android work**. Non-Android branches and non-Android files are invisible to it. There is no flag, override, or future iteration in which `/merge` operates on Harmony, iOS, or any other platform.

**Branches always excluded** (never queued, never checked out, never rebased, never merged, never deleted):
- `master`
- `platform/harmony` and anything matching `harmony/*`
- anything matching `ios/*`
- anything matching `release/*`, `hotfix/*` (handled by separate release flow)
- any branch whose name begins with a non-Android platform prefix added in the future

**Files always excluded from auto-conflict-resolution** (if a candidate branch's rebase produces conflicts in any of these paths, the skill records "non-android-conflict" and escalates without touching the file):
- `platforms/harmony/**`
- `platforms/ios/**` (future)
- `.kiro/`, `.gemini/`, `.roo/`, `.windsurf/` agent config trees, when owned by non-Android pipelines
- any file with HarmonyOS-native extensions: `.ets`, `.ets5`, `module.json5`, `app.json5`, `oh-package.json5`
- any iOS-native extension (future): `.swift`, `.xcodeproj/**`, `.xcworkspace/**`, `Podfile*`

**Base branch is always `develop`.** This skill never targets `platform/harmony`, `master`, or any other base. If a queued branch's PR somehow has a non-develop base, skip it and record "wrong-base:<base>".

**Lane-direct work is sacred.** Harmony and iOS work commits directly to their lane branches via `/cnp`. The merge skill must never imply, suggest, or perform any operation that would route lane work through `develop`.

If the skill ever finds itself about to operate on a non-Android artifact, it must abort that branch's loop immediately, record the violation, and continue to the next branch. There is no recovery mode that bypasses this — the rule is structural, not preferential.

## Rules

- `develop -> master` is publishing — use `promote-to-master`.
- Working tree must be clean before sweep starts; refuse otherwise.
- One confirmation up front; then unattended end-to-end.
- Per-branch failures do NOT abort the sweep — they are recorded and reported at the end.
- Always `--delete-branch` on successful merge.
- `--force-with-lease` is the only force variant ever used, and only on Android feature branches the skill just rebased.
