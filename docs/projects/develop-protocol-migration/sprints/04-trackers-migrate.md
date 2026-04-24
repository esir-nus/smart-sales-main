# Sprint 04 — trackers-migrate

## Header

- Project: develop-protocol-migration
- Sprint: 04
- Slug: trackers-migrate
- Date authored: 2026-04-24
- Date revised: 2026-04-24 (widened scope after operator blocker; supersedes the first-pass authoring from earlier same day)
- Author: Claude
- Operator: **Codex** (default operator)
- Lane: `develop` (docs + one Kotlin test file + one harness settings file)
- Branch: `docs/sprint-04-trackers-migrate` (authoring branch; cut fresh from `origin/develop`). Operator's blocked worktree at `.worktrees/docs-sprint-04-trackers-migrate/` is reusable — apply the revised contract there and continue
- Worktree: existing `.worktrees/docs-sprint-04-trackers-migrate/` from the blocked first iteration
- Ship path: one PR to `develop` containing all file moves + widened cross-reference fixes + tracker sprint-index update + contract closeout

## Demand

**User ask (verbatim from project tracker sprint index):** "Migrate `god-tracker`, `harmony-tracker`, `ui-tracker`, `bug-tracker`, wave briefs into `docs/projects/<slug>/`."

**Interpretation:** Pure relocation sprint. Move six files out of `docs/plans/` into per-project `docs/projects/<slug>/tracker.md` files so each tracker lives next to its owning project's sprints (per `docs/specs/project-structure.md`). No content rewrites beyond relative-path adjustments in cross-references. DTQ-03 disposition and the `docs/plans/tracker.md` shrink are explicitly split out to sprints 07 and 08 respectively; this sprint stops at the relocation.

**Revision (2026-04-24) — widened for non-docs consumers:** first-pass execution blocked on `GodStructureGuardrailTest.kt`, which is a runtime parser of `docs/projects/god-file-cleanup/tracker.md` rather than a prose reference. Full repo sweep also surfaced one hook echo string in `.claude/settings.json`. Both are now in scope. No other runtime or build-time consumers exist. The original stop rule on unknown non-docs consumers remains — if a new consumer surfaces during execution, still stop.

## Scope

Files to move (six):

| From | To | Notes |
|------|----|-------|
| `docs/projects/god-file-cleanup/tracker.md` | `docs/projects/god-file-cleanup/tracker.md` | Slug derived from file's own "god-file cleanup" framing |
| `docs/projects/harmony-native/tracker.md` | `docs/projects/harmony-native/tracker.md` | Canonical Harmony program tracker |
| `docs/projects/ui-campaign/tracker.md` | `docs/projects/ui-campaign/tracker.md` | UI design/prototype/transplant campaign |
| `docs/projects/connectivity-bug-triage/tracker.md` | `docs/projects/connectivity-bug-triage/tracker.md` | File titles itself "Connectivity Bug Tracker"; scope is already narrow |
| `docs/projects/harmony-ui-translation/tracker.md` | `docs/projects/harmony-ui-translation/tracker.md` | Own project; cross-links to `harmony-native` |
| `docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md` | `docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md` | This is a bounded brief, not a tracker; lands as a historical sprint contract under `harmony-native` |

Also in scope:

- Create each new project folder (`docs/projects/<slug>/`) with the moved file as its only content. `sprints/` and `evidence/` subfolders are NOT created in this sprint; they appear when the owning project authors its first new sprint.
- Update every cross-reference inside the moved files to point at the new paths (intra-file `../plans/` links, sibling-tracker mentions).
- Update every cross-reference in the rest of the repo that points at the old `docs/plans/<tracker>.md` paths. Sweep with the widened residue-grep in Evidence below.
- Non-docs consumers (surfaced by the first-pass blocker):
  - `app-core/src/test/java/com/smartsales/prism/ui/GodStructureGuardrailTest.kt` — update the three hardcoded `docs/projects/god-file-cleanup/tracker.md` strings at lines 48, 69, 78 to the new path (`docs/projects/god-file-cleanup/tracker.md`). No structural changes to the test.
  - `.claude/settings.json` — update the single `docs/projects/harmony-native/tracker.md` reference in the hook echo string to the new path (`docs/projects/harmony-native/tracker.md`).
- Run `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest` after the move to prove the test still passes against the new tracker location. This is a hard gate; the test must stay green.
- Update `docs/projects/develop-protocol-migration/tracker.md`: flip row 04 from `authored` to `done` at closeout, and add any new cross-sprint decision surfaced during execution.

**Out of scope** (do not touch under any outcome):

- `docs/plans/tracker.md` — shrink is sprint 08's territory
- `legacy plans changelog` — deprecated per CLAUDE.md "Single Changelog" rule; disposition handled elsewhere
- `fix/scheduler-polish` / `parking/fix-scheduler-polish-20260423` branches — sprint 07 (DTQ-03 carry-over)
- Content rewrites beyond path fixes (no restructuring, no status updates to the moved trackers, no adding or removing rows inside them)
- `docs/projects/workflow-renovation/` — separate bookkeeping artifact; disposition handled in a follow-up, not this sprint
- The six moved trackers' referenced specs (`docs/specs/**`, `docs/sops/**`, `docs/cerb/**`) — leave untouched unless a path inside them references a moved tracker
- `.claude/projects/-home-cslh-frank-main-app/memory/project_smart_sales.md` — Claude's auto-memory, not a repo-tracked consumer; ignore
- Any consumer NOT listed above — if the widened residue grep surfaces an unknown consumer, stop per Stop Exit Criteria

**Slug flexibility:** operator may adjust a slug if research surfaces a more accurate name (e.g., the UI campaign already owns a canonical slug elsewhere in the repo). Record the substitution in the iteration ledger.

## References

Operator reads these, not the full repo:

1. `docs/specs/sprint-contract.md`
2. `docs/specs/project-structure.md` — particularly the Folder Layout and Naming sections
3. `docs/projects/develop-protocol-migration/tracker.md`
4. The six source files listed in Scope (read once before moving, then again after for cross-reference sweep)
5. `docs/plans/tracker.md` — read-only for cross-reference detection; do not edit

## Success Exit Criteria

Literally checkable:

- `test ! -f docs/projects/god-file-cleanup/tracker.md && test ! -f docs/projects/harmony-native/tracker.md && test ! -f docs/projects/ui-campaign/tracker.md && test ! -f docs/projects/connectivity-bug-triage/tracker.md && test ! -f docs/projects/harmony-ui-translation/tracker.md && test ! -f docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md` returns 0
- All six destination files exist: `test -f docs/projects/god-file-cleanup/tracker.md && test -f docs/projects/harmony-native/tracker.md && test -f docs/projects/ui-campaign/tracker.md && test -f docs/projects/connectivity-bug-triage/tracker.md && test -f docs/projects/harmony-ui-translation/tracker.md && test -f docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md`
- `git log --follow` on each destination file shows the source file's history preserved (moves performed via `git mv`, not copy+delete)
- Widened residue grep returns empty for outdated path references across the whole repo:
  `grep -rln "docs/plans/god-tracker\|docs/plans/harmony-tracker\|docs/plans/ui-tracker\|docs/plans/bug-tracker\|docs/plans/harmony-ui-translation-tracker\|docs/plans/harmony-scheduler-backend-phase1-brief" --exclude-dir=.git --exclude-dir=.worktrees --exclude-dir=node_modules . 2>/dev/null` returns empty (or only hits inside this contract file itself + the project tracker's sprint index + `.claude/projects/**/memory/**`)
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest` exits 0 after the move
- `wc -l` on each moved file is identical to the pre-move value ± modifications only for cross-reference path fixes
- `docs/projects/develop-protocol-migration/tracker.md` sprint 04 row shows status `done` with a one-line Closeout summary
- One merged PR to `develop` covers all moves + cross-reference fixes + tracker row update

## Stop Exit Criteria

Halt and surface:

- A moved file's cross-reference target is itself one of the six moved files AND the operator cannot determine the correct new path unambiguously from this contract — stop, surface the ambiguity, do not guess
- `git mv` fails on any source file (e.g., the file does not exist at the expected path because sprint 03 already moved it) — stop and reconcile; do not improvise with `rm` + `cp`
- Widened residue grep finds the old path referenced in a file NOT listed in the allowlist above (the Kotlin test, `.claude/settings.json`, docs + repo-root markdown, this contract, the memory file) — stop; an unknown consumer surfaced that the revised contract did not plan for
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest` fails after the move + path update — stop; do not chase the failure into out-of-scope test code
- Any destination path already exists (`docs/projects/<slug>/tracker.md` present with non-zero size before the move) — stop; the slug collision needs explicit operator decision
- Iteration bound hit without all Success Exit Criteria green — stop per above

## Iteration Bound

- Max 3 iterations OR 90 minutes wall-clock, whichever hits first. Operator's first iteration (the blocked one) counts as iteration 1.
- One iteration = all six moves + widened cross-reference sweep + Kotlin test + settings.json fix + guardrail test run + tracker row update + evidence capture
- Hitting the bound without green is a stop, not a force-close

## Required Evidence Format

At close, operator appends to Closeout:

1. **Preconditions evidence** (once, at iteration 1):
   - `wc -l docs/projects/god-file-cleanup/tracker.md docs/projects/harmony-native/tracker.md docs/projects/ui-campaign/tracker.md docs/projects/connectivity-bug-triage/tracker.md docs/projects/harmony-ui-translation/tracker.md docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md` — baseline line counts
   - `ls -la docs/projects/` — confirms no slug collisions with existing project folders
2. **Move evidence** (per source file, verbatim command output):
   - The `git mv` command and its output
   - `git log --follow --oneline <destination>` truncated to 3 lines confirming history is preserved
3. **Cross-reference sweep evidence**:
   - The widened residue grep command and its output (expected: empty or only the allowed allowlist hits — this contract file, the updated Kotlin test, the updated `.claude/settings.json`, the memory file, the project tracker's sprint index)
   - `git diff --stat` for the sweep commits, showing which files were edited for path fixes (expected files: the six moved trackers' internal cross-refs, any docs mentioning them, `CLAUDE.md`, `.claude/settings.json`, the Kotlin test)
4. **Non-docs consumer evidence** (the widening):
   - `git diff app-core/src/test/java/com/smartsales/prism/ui/GodStructureGuardrailTest.kt` showing exactly three string replacements (lines 48, 69, 78)
   - `git diff .claude/settings.json` showing one string replacement
   - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest` output with exit code 0
5. **Verification evidence** (post-run):
   - `wc -l` on each destination file vs. the preconditions baseline
   - `find docs/plans -name "*.md" -maxdepth 1 | sort` — proves only `tracker.md` and `changelog.md` remain in `docs/plans/`
   - `find docs/projects -name "tracker.md" | sort` — proves five new project trackers plus the existing ones (`develop-protocol-migration`, `workflow-renovation`)
   - The merged PR URL

Agent narration without these artifacts is not acceptable evidence.

## Iteration Ledger

- **Iteration 1 (2026-04-24, operator Claude, authoring):** inventoried the six files in `docs/plans/`, confirmed each has a natural project slug, authored this contract. Next action: execution iteration by operator (Codex default).
- **Iteration 2 (2026-04-24, operator Codex, execution — BLOCKED):** cut `docs/sprint-04-trackers-migrate` worktree at `.worktrees/docs-sprint-04-trackers-migrate/`. Ran the original residue sweep; it was scoped to `docs/ AGENTS.md CLAUDE.md` only. Before performing `git mv`, operator widened the sweep and surfaced `GodStructureGuardrailTest.kt:48,69,78` hardcoding `docs/projects/god-file-cleanup/tracker.md`. Per the original contract's Stop Exit Criteria on non-docs consumers, stopped without moving, committing, or pushing. Recorded blocked state inside the worktree's copy of this contract and flipped the worktree's project tracker row to `blocked`. Main worktree's copy of the contract + tracker stays on the revised (widening) version.
- **Iteration 3 (2026-04-24, operator Claude, re-authoring):** full repo sweep identified three relevant out-of-docs consumers (`GodStructureGuardrailTest.kt`, `.claude/settings.json`, `CLAUDE.md`). `CLAUDE.md` was already in the original sweep's allowlist. Added `GodStructureGuardrailTest.kt` and `.claude/settings.json` to scope with explicit line-level directives; added a test run as a hard gate; widened the residue grep to repo-wide. Claude's auto-memory file remains explicitly out of scope. Next action: operator resumes iteration 4 execution from the existing blocked worktree using this revised contract.
- **Iteration 4 (2026-04-24, operator Codex, execution):** reused the blocked worktree, completed the remaining `git mv` operations, rewrote repo references to the new tracker paths, updated the scoped Kotlin test + `.claude/settings.json` consumer, and reran the widened residue grep. Evaluator saw only allowlisted leftovers (this contract + Claude auto-memory), destination file line counts matched baseline exactly, `docs/plans/` shrank to `tracker.md` + `changelog.md`, and `ANDROID_HOME=/home/cslh-frank/Android/Sdk ANDROID_SDK_ROOT=/home/cslh-frank/Android/Sdk ./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest` exited 0. Next action: close sprint as success and ship the branch.

## Closeout

- **Status:** success
- **Summary for project tracker:** Moved six trackers/briefs out of `docs/plans/` into per-project `docs/projects/<slug>/` folders using `git mv`; widened sweep updated the scoped Kotlin test and `.claude/settings.json`, and `GodStructureGuardrailTest` passed against the new tracker location.
- **Evidence artifacts:**
  - Preconditions:
    - `wc -l docs/plans/god-tracker.md docs/plans/harmony-tracker.md docs/plans/ui-tracker.md docs/plans/bug-tracker.md docs/plans/harmony-ui-translation-tracker.md docs/plans/harmony-scheduler-backend-phase1-brief.md`
      ```
         685 docs/plans/god-tracker.md
         316 docs/plans/harmony-tracker.md
         350 docs/plans/ui-tracker.md
         332 docs/plans/bug-tracker.md
         119 docs/plans/harmony-ui-translation-tracker.md
          55 docs/plans/harmony-scheduler-backend-phase1-brief.md
        1857 total
      ```
    - `ls -la docs/projects/`
      ```
      total 16
      drwxrwxr-x  4 cslh-frank cslh-frank 4096 Apr 24 12:06 .
      drwxrwxr-x 22 cslh-frank cslh-frank 4096 Apr 24 12:06 ..
      drwxrwxr-x  3 cslh-frank cslh-frank 4096 Apr 24 13:04 develop-protocol-migration
      drwxrwxr-x  3 cslh-frank cslh-frank 4096 Apr 24 12:06 workflow-renovation
      ```
  - Move evidence:
    - `git mv docs/plans/god-tracker.md docs/projects/god-file-cleanup/tracker.md` (no stdout)
    - `git mv docs/plans/harmony-tracker.md docs/projects/harmony-native/tracker.md` (no stdout)
    - `git mv docs/plans/ui-tracker.md docs/projects/ui-campaign/tracker.md` (no stdout)
    - `git mv docs/plans/bug-tracker.md docs/projects/connectivity-bug-triage/tracker.md` (no stdout)
    - `git mv docs/plans/harmony-ui-translation-tracker.md docs/projects/harmony-ui-translation/tracker.md` (no stdout)
    - `git mv docs/plans/harmony-scheduler-backend-phase1-brief.md docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md` (no stdout)
    - `git log --follow --oneline -3 -- docs/projects/god-file-cleanup/tracker.md`
      ```
      5504d1347 docs(trackers): migrate plans trackers into project folders
      81fb134a2 docs: close historical-report cleanup wave and finish doc cleanup backlog
      09091955d Harden onboarding fast-lane LLM path and demote deterministic fallback to backup only
      ```
    - `git log --follow --oneline -3 -- docs/projects/harmony-native/tracker.md`
      ```
      5504d1347 docs(trackers): migrate plans trackers into project folders
      83f68a26e update scheduler signals and Harmony lane support
      37f48c4b9 docs(governance): establish Harmony native migration posture
      ```
    - `git log --follow --oneline -3 -- docs/projects/ui-campaign/tracker.md`
      ```
      5504d1347 docs(trackers): migrate plans trackers into project folders
      23163aa0e ship: land quarantine lanes and harmony tingwu container
      68e15bf93 feat: align runtime home shell with ambient island handoff
      ```
    - `git log --follow --oneline -3 -- docs/projects/connectivity-bug-triage/tracker.md`
      ```
      5504d1347 docs(trackers): migrate plans trackers into project folders
      23163aa0e ship: land quarantine lanes and harmony tingwu container
      9a384621f feat(runtime-shell): transplant SIM dynamic island connectivity lane and sync docs
      ```
    - `git log --follow --oneline -3 -- docs/projects/harmony-ui-translation/tracker.md`
      ```
      5504d1347 docs(trackers): migrate plans trackers into project folders
      e3bee00df chore: land admin-path and governance residue from dirty-tree quarantine
      3fbc25ffd chore: land admin-path and governance residue from dirty-tree quarantine
      ```
    - `git log --follow --oneline -3 -- docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md`
      ```
      5504d1347 docs(trackers): migrate plans trackers into project folders
      e3bee00df chore: land admin-path and governance residue from dirty-tree quarantine
      3fbc25ffd chore: land admin-path and governance residue from dirty-tree quarantine
      ```
  - Cross-reference sweep:
    - `grep -rln "docs/plans/god-tracker\|docs/plans/harmony-tracker\|docs/plans/ui-tracker\|docs/plans/bug-tracker\|docs/plans/harmony-ui-translation-tracker\|docs/plans/harmony-scheduler-backend-phase1-brief" --exclude-dir=.git --exclude-dir=.worktrees --exclude-dir=node_modules . 2>/dev/null`
      ```
      ./.claude/projects/-home-cslh-frank-main-app/memory/project_smart_sales.md
      ./docs/projects/develop-protocol-migration/sprints/04-trackers-migrate.md
      ```
    - `git diff --stat` captured a docs-only path sweep plus the two widened-scope consumers and the sprint docs.
  - Non-docs consumer evidence:
    - `git diff app-core/src/test/java/com/smartsales/prism/ui/GodStructureGuardrailTest.kt`
      ```
      @@
      -        val trackerFile = resolvePath("docs/plans/god-tracker.md")
      +        val trackerFile = resolvePath("docs/projects/god-file-cleanup/tracker.md")
      @@
      -                "Tracked file has unexpected structural status in docs/plans/god-tracker.md: $filePath",
      +                "Tracked file has unexpected structural status in docs/projects/god-file-cleanup/tracker.md: $filePath",
      @@
      -        val rowsByFile = parseTrackedFileRows(resolvePath("docs/plans/god-tracker.md"))
      +        val rowsByFile = parseTrackedFileRows(resolvePath("docs/projects/god-file-cleanup/tracker.md"))
      ```
    - `git diff .claude/settings.json`
      ```
      @@
      -            "command": "echo \"$CLAUDE_TOOL_INPUT\" | grep -q 'platforms/harmony/smartsales-app' && echo '[TRACKER] Harmony app file modified — check if docs/plans/harmony-tracker.md H-04 Notes/Drift needs updating.' || true"
      +            "command": "echo \"$CLAUDE_TOOL_INPUT\" | grep -q 'platforms/harmony/smartsales-app' && echo '[TRACKER] Harmony app file modified — check if docs/projects/harmony-native/tracker.md H-04 Notes/Drift needs updating.' || true"
      ```
    - `ANDROID_HOME=/home/cslh-frank/Android/Sdk ANDROID_SDK_ROOT=/home/cslh-frank/Android/Sdk ./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
      ```
      > Task :app-core:testDebugUnitTest
      BUILD SUCCESSFUL in 1m 31s
      367 actionable tasks: 141 executed, 226 from cache
      ```
  - Verification evidence:
    - `wc -l docs/projects/god-file-cleanup/tracker.md docs/projects/harmony-native/tracker.md docs/projects/ui-campaign/tracker.md docs/projects/connectivity-bug-triage/tracker.md docs/projects/harmony-ui-translation/tracker.md docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md`
      ```
         685 docs/projects/god-file-cleanup/tracker.md
         316 docs/projects/harmony-native/tracker.md
         350 docs/projects/ui-campaign/tracker.md
         332 docs/projects/connectivity-bug-triage/tracker.md
         119 docs/projects/harmony-ui-translation/tracker.md
          55 docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md
        1857 total
      ```
    - `find docs/plans -maxdepth 1 -name "*.md" | sort`
      ```
      <legacy plans changelog>
      docs/plans/tracker.md
      ```
    - `find docs/projects -name "tracker.md" | sort`
      ```
      docs/projects/connectivity-bug-triage/tracker.md
      docs/projects/develop-protocol-migration/tracker.md
      docs/projects/god-file-cleanup/tracker.md
      docs/projects/harmony-native/tracker.md
      docs/projects/harmony-ui-translation/tracker.md
      docs/projects/ui-campaign/tracker.md
      docs/projects/workflow-renovation/tracker.md
      ```
    - PR URL: `https://github.com/esir-nus/smart-sales-main/pull/31`
- **Lesson proposals:** none at authoring time; at close, consider proposing "Residue sweep for repo-wide path moves must cover source + tests + harness config, not just docs" if the blocker re-occurs elsewhere.
- **CHANGELOG line:** none. Internal-governance restructure; not user-visible.
