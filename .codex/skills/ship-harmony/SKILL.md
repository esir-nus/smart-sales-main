---
name: ship-harmony
description: Run the Harmony push preflight explicitly before shipping a harmony/* or platform/harmony branch, enforce the dedicated-worktree and sprint-scope gates, and print the remediation commands when the push is not ready.
---

# Ship Harmony

Use this skill when the operator asks for `/ship-harmony` or wants the Harmony push boundary checked explicitly before `git push`.

This skill exists to add friction upfront at the Harmony push boundary:

- require a dedicated linked worktree on `platform/harmony` or `harmony/*`
- require a clean tree
- require a lane-pure diff
- require an Active or Blocked HS-NNN sprint whose scope covers the diff
- require no divergence behind `origin/platform/harmony`

## Step 1: Confirm the worktree context

Run:

```bash
git rev-parse --abbrev-ref HEAD
git status --porcelain
git worktree list --porcelain
```

If the current directory is not a clean linked Harmony worktree, stop and print:

```bash
git worktree add -b harmony/<name> .worktrees/<name> origin/platform/harmony
cp local.properties .worktrees/<name>/local.properties
```

Do not try to untangle a dirty parent worktree at push time.

## Step 2: Inspect the branch diff against Harmony trunk

Run:

```bash
git diff --name-only origin/platform/harmony...HEAD
```

Classify the result:

- allowed Harmony lane files: `platforms/harmony/**`
- allowed Harmony evidence/companion docs: `docs/platforms/harmony/**`, `docs/plans/harmony-tracker.md`, `docs/plans/sprint-tracker.md`
- allowed shared-contract docs: `docs/core-flow/**`, `docs/cerb/**`, `docs/cerb-ui/**`, `docs/specs/**`

If Android files, repo-root markdown, or unrelated tooling/docs appear, stop and tell the operator to split the branch.

## Step 3: Verify the sprint contract

Read `docs/plans/sprint-tracker.md` and confirm:

- the current branch has an entry whose `Branch:` matches the branch name
- the entry status is `Active` or `Blocked`
- every diff file is listed under that sprint's `Scope`

If the contract is missing or too narrow, stop and tell the operator:

- use `/sprint` to open or activate the contract
- or update the sprint `Scope` bullets before pushing

## Step 4: Run the explicit preflight

Run the same hook logic the automatic Bash gate uses:

```bash
printf '%s\n' '{"tool_input":{"command":"git push origin '"$(git rev-parse --abbrev-ref HEAD)"'"}}' | .claude/hooks/harmony-push-preflight.sh
```

If the operator intentionally needs to bypass checks 3-6, they may use the literal escape hatch:

```bash
git push origin <branch> --force-parallel "<reason>"
```

Checks 1-2 remain absolute.

## Step 5: Push only after green

Once the preflight passes, run:

```bash
git push origin $(git rev-parse --abbrev-ref HEAD)
```

If the branch is for integration trunk itself, push `platform/harmony` explicitly.

## Rules

- Never push Harmony work from the main worktree.
- Never treat a dirty tree as a pushable state.
- Never widen sprint scope silently at push time.
- Never merge `origin/platform/harmony` into the branch; rebase instead.
