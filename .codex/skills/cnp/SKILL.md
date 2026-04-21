---
name: cnp
description: Commit and Push the working tree. Use when the user says /cnp. Lane-aware - Android trunks (develop, master) auto-branch into a feat/<slug> branch; platform lanes (platform/harmony, ios/*) commit in place since they ARE the per-platform delivery branch. Does NOT merge or create PRs.
---

# cnp — Commit and Push

A predictable, lane-aware save button. Stages the working tree, commits, pushes to origin.

Branch model recap:

- `master` — protected, publish-only.
- `develop` — Android maintenance + shared-contracts trunk. Android work flows into here via feature branches → `/merge`.
- `platform/harmony` — HarmonyOS lane. Harmony work commits **directly here** (no feature branches, no PR sweep).
- `ios/*` (future) — same pattern as `platform/harmony`.
- `feat/*`, `fix/*`, `chore/*`, etc. — Android feature branches, base = `develop`.

Behavior summary:

- **On a feature branch**: commit and push in place.
- **On a platform lane** (`platform/harmony`, `ios/*`): commit and push in place. The lane branch IS the delivery surface.
- **On `develop` or `master`**: refuse to commit on trunk. Auto-create a `feat/<slug>` branch based on `develop`, switch to it, then commit and push there. The user later runs `/merge` to sweep it into `develop`.

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

3. Classify the branch:
   - `master` or `develop` → **android trunk** → run Step 1b (auto-branch) before staging.
   - `platform/harmony` or matches `ios/*` → **platform lane** → commit in place, continue.
   - Anything else → **feature branch** → continue.

4. Check there is something to commit:
   ```bash
   git status --porcelain
   ```
   If empty AND no unpushed commits exist (`git rev-list @{u}..HEAD --count` returns 0): STOP. "Nothing to commit or push."

## Step 1b: Auto-branch (Android trunk only)

Triggered when current branch is `develop` or `master`. NOT triggered on `platform/harmony` or `ios/*` — those are lane branches and receive commits directly.

1. Base for the new branch is always `develop`. On `master`, warn the user that work was on master and is being redirected to a `develop`-based feature branch.

2. Generate a slug from the staged diff (kebab-case, ≤40 chars). Inputs in priority order:
   - Dominant changed top-level path (e.g. `app-core`, `domain`, `data`).
   - Most-changed filename stem.
   - Fallback: `wip-<YYYYMMDD-HHMM>`.

3. Compose branch name: `feat/<slug>`. If it already exists locally or on origin, append `-2`, `-3`, etc.

4. Confirm with the user before switching:
   ```
   On <trunk>. cnp will not commit to trunk.
   Proposed branch: feat/<slug> (base: develop)
   ```
   Accept user override of the name. If user declines, STOP.

5. Create and switch (working-tree changes carry over):
   ```bash
   git checkout -b "feat/<slug>" develop
   BRANCH="feat/<slug>"
   ```

6. Continue to Step 2.

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
- Branch (and note "auto-created from <trunk>" if Step 1b ran; note "platform lane, direct commit" if on `platform/harmony` or `ios/*`)
- Commit SHA + subject
- Push result (new branch / fast-forward / up-to-date)
- Next-step hint:
  - feature branch → "Run `/merge` (end-of-day) to sweep into develop."
  - platform lane → no further action; commit is live on the lane branch.

## Rules

- Never commit directly to `master` or `develop` — auto-branch first.
- Direct commit on `platform/harmony` and `ios/*` is correct; these ARE the per-platform delivery branches and do not use feature branches or PRs.
- This skill never merges, rebases, or creates PRs.
- This skill never force-pushes.
- This skill never bypasses hooks (`--no-verify`, `--no-gpg-sign`).
- Sensitive-file refusal is non-negotiable; unstage and warn, never commit them.
- Auto-branch always requires user confirmation of the proposed branch name before switching.
