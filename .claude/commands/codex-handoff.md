Produce a structured context handoff for Codex. Lightweight, no validation. Use `/ship` for the full shipping ceremony with compile/test validation.

This is a read-only skill. Claude gathers context and produces a structured block. Codex executes git operations.

## Workflow

### Step 1: Read State

1. Run `git rev-parse --abbrev-ref HEAD`.
2. Run `git status --porcelain`.
3. Run `git diff --name-only` (unstaged) and `git diff --cached --name-only` (staged).
4. Run `git log --oneline -5` for recent context.

### Step 2: Determine Intent

If `$ARGUMENTS` names an action (commit, push, PR, rebase, cherry-pick, sync, etc.), use that.

Otherwise, infer from state:
- Uncommitted changes on `feature/*` or `harmony/*` branch -> suggest: commit + push + PR to develop
- Uncommitted changes on `develop` -> suggest: commit + push
- On `platform/*` branch -> warn: "Platform branches are shipping snapshots. Switch to develop or create a feature branch."
- Clean tree, feature branch ahead of develop -> suggest: create PR to develop
- Clean tree, develop ahead of master -> suggest: promotion (route to Codex's `promote-to-master` skill)
- On `master` -> warn: "Direct work on master is not allowed. Switch to develop or a feature branch."
- If unclear, ask the user.

### Step 3: Produce Handoff Block

Output the following structured block:

```
## Codex Handoff

### Context
- Branch: {name}
- Status: {clean | N files modified, M staged}
- Recent commits:
  {last 3-5 oneline entries}

### Changed Files
{file list grouped by module, or "clean tree"}

### Suggested Action
{e.g., "Commit staged changes, push to origin, create PR to develop"}
{If promotion: "Use Codex promote-to-master skill to create promotion PR from develop to master"}

### Scope
{1-2 sentence summary of what changed and why, derived from diff analysis}

### Notes
{Any context Codex needs, e.g.:}
{- "This branch has no remote tracking yet -- use git push -u origin {branch}"}
{- "develop is 3 commits behind origin/develop -- pull before committing"}
{- "Working tree has unstaged deletions -- verify before staging"}
{- omit this section if there is nothing notable}
```

### Step 4: User Instruction

After the handoff block, print:

> Copy the handoff block to Codex. For full validation before shipping, use `/ship` instead.

## Hard Rules

- This skill is READ-ONLY. Do not stage, commit, push, or create PRs.
- Do not run compile or test commands. Use `/ship` if validation is needed.
- The handoff block must contain enough context for Codex to execute without re-reading the codebase.
