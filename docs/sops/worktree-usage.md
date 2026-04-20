# Worktree Usage SOP

> **Purpose**: Build, test, or install a different branch without disturbing the current working tree. Especially useful when the active branch has uncommitted multi-platform work that must not be paused.

---

## When to Use

- You need to install a build from another branch onto a device, but the current branch has uncommitted work.
- You are doing cross-platform work (e.g., Harmony in flight on one branch, Android fix needed on another) and the platforms are isolated by design.
- You want to verify a remote branch end-to-end before merging it.
- You need two parallel build directories (e.g., A/B compare two branches on the same hardware).

If you only need to read files from another branch, use `git show` or `git fetch` + GitHub UI instead. Worktrees are for full builds and installs.

---

## Create a Worktree

```bash
# from repo root
git fetch origin <branch-name>
git worktree add .worktrees/<short-name> origin/<branch-name>
```

- `.worktrees/` is the convention for in-repo worktree storage.
- Creates a detached HEAD at the remote tip — safe, no local branch contamination.
- For a tracking branch (so you can commit and push back), use `git worktree add -b feat/foo .worktrees/foo origin/feat/foo`.

---

## Provision the Worktree

A new worktree starts without gitignored files. You must re-provision them before building:

```bash
cp local.properties .worktrees/<short-name>/local.properties
```

Files that need copying for an Android build:
- `local.properties` (Android SDK path, signing keys)
- Any `.env` or `secrets/*` files the build references
- `keystore/*.jks` if release signing is required

Skip if the worktree is for read-only inspection.

---

## Build and Install

Treat the worktree as a fresh checkout. From the worktree directory:

```bash
cd .worktrees/<short-name>
./gradlew :app-core:installDebug
```

The version stamp on the resulting APK will reflect the worktree's branch and commit (see `app-core/build.gradle.kts` — `gitBranchValue`/`gitDescribeValue`). Verify in Settings → About on device, or:

```bash
adb shell dumpsys package com.smartsales.prism | grep versionName
```

---

## Clean Up

When done:

```bash
# from any worktree
git worktree remove .worktrees/<short-name>
```

This deletes the directory and unregisters the worktree. If the worktree had a tracking branch, the branch remains; delete with `git branch -D` if no longer needed.

If the worktree directory was deleted manually (broken state), run `git worktree prune` to clean the registry.

---

## Cross-Platform Isolation Pattern

Worktrees operationalize the multi-platform isolation principle in `docs/specs/platform-governance.md`: Android work and Harmony work do not block each other.

Example: Harmony scheduler work in flight on `harmony/scheduler-phase-2b`, Android Wi-Fi repair fix needed from `feature/android-wifi`. Instead of stashing or context-switching:

```bash
git worktree add .worktrees/wifi-fix origin/feature/android-wifi
cp local.properties .worktrees/wifi-fix/local.properties
cd .worktrees/wifi-fix
./gradlew :app-core:installDebug
# verify on device
git worktree remove .worktrees/wifi-fix
```

The Harmony tree is never touched. The dirty `.ets` files stay where they are.

---

## Anti-Patterns

- **Do not** `git checkout` between branches when one has uncommitted work spanning a different platform. That's exactly what worktrees prevent.
- **Do not** commit worktree paths to git. The `.worktrees/` directory should be in `.gitignore` if it isn't already.
- **Do not** share `local.properties` between machines via worktree — each machine has its own SDK path.
- **Do not** forget to clean up. Stale worktrees confuse `git status` and gradle daemons.
