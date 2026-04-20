Ship current work on `develop` or a feature branch. Never touches `master` -- only `/merge` and `promote-to-master` touch master through PRs.

Primary path: produce a structured handoff block for Codex to execute git operations.
Backup path: if Codex is unavailable, Claude executes the git operations directly.

## Pipeline

Execute these steps in order. Stop immediately if any gating step fails.

### Step 1: Preflight

1. Run `git rev-parse --abbrev-ref HEAD` and `git status --porcelain`.
2. Run `git diff --name-only` (unstaged) and `git diff --cached --name-only` (staged) to identify changed files.
3. If no changes exist, stop: "Nothing to ship."
4. Determine branch context and target action:
   - **develop**: handoff = commit + push
   - **feature/\*** or **harmony/\***: handoff = commit + push + PR to develop
   - **platform/\***: STOP. "Platform branches are shipping snapshots. Switch to develop or create a feature branch from develop."
   - **master**: STOP. "Never commit directly to master. Switch to develop or a feature branch."
   - **other**: warn, ask the user to confirm the target branch and action

### Step 2: Classify Changes

For each changed file, determine:

1. **Module ownership**: `app/`, `app-core/`, `core/*`, `data/*`, `domain/*`, `platforms/harmony/`, `docs/`, `.github/`, `.claude/`, `.codex/`, build config
2. **Commit prefix** (from diff analysis):
   - New implementation files -> `feat`
   - Bug-related fixes -> `fix`
   - Structural changes -> `refactor`
   - Test files only -> `test`
   - Doc files only -> `docs`
   - Config/governance only -> `chore`
   - Mix of code + docs -> use the code prefix (docs travel with code in cerb-compliant work)
3. **Scope string**: derive from file paths (e.g., `connectivity`, `scheduler`, `shell`, `pipeline`, `sim`, `harmony`)

### Step 3: Validate

Run targeted validation from repo root. Scope to what actually changed:

| Changed path | Compile target | Test target |
|---|---|---|
| `app-core/` | `./gradlew :app-core:compileDebugKotlin` | `scripts/run-tests.sh app` |
| `app/` | `./gradlew :app:assembleDebug` | -- |
| `data/{sub}/` | `./gradlew :data:{sub}:compileDebugKotlin` | `./gradlew :data:{sub}:test` (if test sources exist) |
| `domain/{sub}/` | -- (pure Kotlin) | `./gradlew :domain:{sub}:test` |
| `core/pipeline/` | `./gradlew :core:pipeline:compileDebugKotlin` | `scripts/run-tests.sh pipeline` |
| `core/{other}/` | `./gradlew :core:{other}:compileDebugKotlin` | -- |
| `platforms/harmony/` | skip (uses hvigor) | skip |
| `docs/` or config only | skip | skip |
| build files (`*.gradle.kts`, `settings.gradle.kts`) | `./gradlew :app:assembleDebug` | skip |

If validation fails, stop and report errors. Do not produce the handoff.

### Step 4: Doc-Code Alignment Advisory

Check whether owning docs were updated alongside code changes:

1. If code under `data/{feature}/` changed, check whether `docs/cerb/{feature}/spec.md` or `docs/cerb/{feature}/interface.md` was also modified.
2. If code under `app-core/` changed, check whether a relevant `docs/core-flow/*-flow.md` was also modified.
3. If the owning doc exists and was NOT modified in this changeset, warn:
   "Consider updating `{doc path}` before shipping."
4. This is advisory only -- warn, do not block.

### Step 5: Produce Handoff Block

Output the following structured block:

```
## Codex Ship Handoff

### Branch
- Current: {branch name}
- Action: {commit + push | commit + push + PR to develop}
- Base: {develop | N/A for direct push}

### Changed Files
{grouped by module}
- app-core/src/main/.../File.kt
- docs/cerb/.../spec.md

### Validation
- Compile: {PASS | SKIP}
- Tests: {PASS (N tests) | SKIP}

### Commit
- Prefix: {feat|fix|refactor|docs|chore|test}
- Scope: {e.g., connectivity}
- Message: {conventional format first line, under 72 chars}

### PR (if on feature branch)
- Title: {from commit message, under 70 chars}
- Base: develop
- Body:
  ## Summary
  {2-3 bullets}

  ## Test plan
  - [ ] CI passes (android-tests, platform-governance-check)
  - [ ] {feature-specific checks}

### Doc Alignment
- {OK | Advisory: consider updating {path}}

### Instructions for Codex
1. Stage these files: {explicit file list}
2. Commit:
   git commit -m "$(cat <<'EOF'
   {prefix}({scope}): {description}

   Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
   EOF
   )"
3. Push: git push {-u origin {branch} if no upstream | origin}
4. {If PR needed}: gh pr create --base {base} --head {branch} --title "{title}" --body "{body}"
```

### Step 6: User Instruction

After the handoff block, print:

> Copy the handoff block above to Codex. Codex will execute the git operations.
> If doc alignment advisory was raised, consider addressing it before or after shipping.

## Hard Rules

- `/ship` never touches master. Only `/merge` and `promote-to-master` touch master through PRs.
- Primary mode is READ-ONLY (produce handoff for Codex). Backup mode (Codex unavailable): Claude executes directly.
- Never produce a handoff for direct commits to master. Never create PRs targeting master.
- If validation fails, do not produce the handoff or execute.
- The handoff block must contain enough context for Codex to execute without re-reading the codebase.
- Always stage specific files -- never `git add -A` or `git add .`.
