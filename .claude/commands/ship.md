Ship the current working changes through the DTQ lane pipeline. This is the ONLY correct way to land work — never commit directly to master.

Lane worktrees live at `/home/cslh-frank/lane-worktrees/`. Use `git worktree list` to discover them.

## Pipeline

Execute these steps in order. Stop immediately if any gating step fails.

### Step 1: Preflight — Where Am I?

1. Run `pwd`, `git rev-parse --abbrev-ref HEAD`, and `git worktree list`.
2. Run `git diff --name-only` (unstaged) and `git diff --cached --name-only` (staged) to identify changed files.
3. If no changes exist, stop: "Nothing to ship."
4. Determine context:
   - **Case A — Lane worktree**: cwd is under `/home/cslh-frank/lane-worktrees/DTQ-*`. Use this lane directly. Skip to Step 3.
   - **Case B — Main worktree on master**: cwd is `/home/cslh-frank/main_app`, branch is `master`. Proceed to Step 2.
   - **Case C — Anything else**: Warn the user and ask how to proceed.

### Step 2: Lane Resolution (only from main worktree)

Resolve which lane worktree to target. In order of precedence:

1. **Explicit argument**: If the user passed a lane number (e.g., `/ship 04`), map it to the matching worktree directory from `git worktree list`. If no match, stop with error.
2. **Auto-detect**: Match changed file paths against lane worktree names. Use simple keyword matching against the worktree directory names (e.g., `connectivity` files → `DTQ-03-connectivity-oem`, `ui/sim` or `DynamicIsland` or `RuntimeShell` files → `DTQ-04-runtime-shell-sim`). When ambiguous, also read the "Owned Write Scope" column of section 4 in `docs/plans/dirty-tree-quarantine.md` for clarification.
3. **Ambiguous or no match**: Show the user the changed files and available lane worktrees. Ask them to pick.

Then **copy the changed files** to the target lane worktree:
- For each changed file, copy it to the same relative path in the lane worktree directory.
- After copying, verify each file landed correctly (check it exists and diff the copy against the source).
- Do NOT revert changes in the main worktree — the user may want to keep working.
- `cd` into the lane worktree for subsequent steps.

### Step 3: Validate (scoped to what changed)

Run validation **from within the lane worktree directory**. Scope to what actually changed:

- If `app-core/` files changed: `./gradlew :app-core:compileDebugKotlin`, then `scripts/run-tests.sh app`
- If `data/` files changed: compile the relevant submodule (e.g., `:data:connectivity:compileDebugKotlin`)
- If `domain/` files changed: test the relevant submodule (e.g., `:domain:scheduler:test`)
- If only `docs/` or `.github/` files changed: skip compile and test entirely
- If only config/build files changed: compile only, skip tests

If validation fails, stop and report errors. Do not commit.

### Step 4: Doc-Code Alignment Advisory

Read the target lane's row in `docs/plans/dirty-tree-quarantine.md` section 4 (Lane Board). Check the **Alignment** column value:
- `Aligned` → proceed silently
- `Both pending` or `Doc update required` → warn: "Lane DTQ-{NN} alignment is '{value}'. Consider updating owning docs before shipping."
- This is advisory only — warn, do not block.

### Step 5: Commit

Working directory: the lane worktree.

1. Stage the specific changed files (never `git add -A` or `git add .`).
2. Draft a commit message in conventional format: `feat|fix|refactor|ship(scope): description`
   - Analyze the diff to pick the right prefix.
   - Keep the first line under 72 chars.
3. Commit using a HEREDOC:
   ```
   git commit -m "$(cat <<'EOF'
   {message}

   Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
   EOF
   )"
   ```

### Step 6: Push and Create PR

1. Check if a PR already exists for this branch: `gh pr list --head {branch-name} --json number,url`
   - If PR exists: push only (`git push`), report "Pushed to existing PR #{N}".
   - If no PR: push with `-u` flag, then create PR.
2. Create PR with `gh pr create`:
   - Title: from commit message (under 70 chars)
   - Body (use HEREDOC):
     ```
     ## Summary
     - {bullet points}

     ## DTQ Lane
     DTQ-{NN}: {lane theme}

     ## Test plan
     - [ ] {verification checklist}

     🤖 Generated with [Claude Code](https://claude.com/claude-code)
     ```
   - Base: `master`

### Step 7: Return

If we started from the main worktree, `cd` back to `/home/cslh-frank/main_app`.

### Step 8: Report

Print:
- PR URL (or "pushed to existing PR #{N}")
- Lane: DTQ-{NN} ({theme})
- Worktree: {path}
- Commit: {hash} {first line}
- Validation: {pass/skip summary}
- Alignment: {status or "OK"}
