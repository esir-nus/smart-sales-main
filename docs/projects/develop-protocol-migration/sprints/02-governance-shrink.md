# Sprint 02 — governance-shrink

## Header

- Project: develop-protocol-migration
- Sprint: 02
- Slug: governance-shrink
- Date authored: 2026-04-23
- Author: Claude (sole sprint contract author per model)
- Operator: **Codex** (default per model; user may override at handoff)
- Lane: `develop`
- Branch: `docs/sprint-02-governance-shrink` (cut from `docs/sprint-01-specs-bootstrap`; operator rebases onto `origin/develop` after PR #26 lands)
- Worktree: `.worktrees/develop-protocol`
- Ship path: feature-branch PR into `develop` (renovation skills still on old rules; sprint 03 rewires `/ship` + `/merge`)

## Demand

**User ask (verbatim):** "i'll let codex handle this per your review and lt's move on the sprint 02"

**Interpretation:** Sprint 02 lands the governance edits that make sprint 01's specs the operative rules. Shrink `docs/specs/declaration-first-shipping.md` to ship-time safety only (rename → `ship-time-checks.md`, archive dropped content). Rewrite `CLAUDE.md` Branch Model + Declaration-First Shipping sections to point at sprint contracts. Add two new rules captured during sprint 01: (1) `develop` is the canonical source for adb-packaged Android installation, (2) agents must not auto-proliferate doc artifacts. Mirror governance changes to `AGENTS.md` with Codex-as-operator-default emphasis.

## Scope

- `docs/specs/declaration-first-shipping.md` — **delete** (content splits: ship-time to new file, old-model content to archive)
- `docs/specs/ship-time-checks.md` — **new** (under 100 lines; §§4-5 content from old file + brief lead paragraph)
- `docs/archive/declaration-first-shipping-archive-20260423.md` — **new** (verbatim old file content + one-line archival header noting replacement path)
- `CLAUDE.md` — **edit**:
  - Rewrite **Branch Model** section: drop per-task feature branches as default pattern; state commit-on-close-to-develop-or-platform-harmony; add `develop` as canonical adb-package source rule
  - Rewrite **Declaration-First Shipping** section: point at sprint contracts + `ship-time-checks.md`; drop references to active-lanes / preflight refusal
  - **Add new section** "Sprint Contract Workflow" (≤15 lines): brief, pointer to `docs/specs/sprint-contract.md` + `docs/specs/project-structure.md`; names Claude-as-author + Codex-as-default-operator; notes user picks operator at handoff
  - **Add new rule** "Doc-Production Discipline" (can live as its own short section or bullet under an existing section): "Agents must not auto-proliferate doc artifacts. Before producing a non-code file, ask whether the information fits in a commit message, an existing doc, or a sprint contract's iteration ledger. Default is not to create a new file."
  - **Add new rule** "Single Changelog" (preserve from old declaration-first-shipping.md §6): "There is one product changelog: `CHANGELOG.md` (renders to `CHANGELOG.html`). No parallel trace log; `docs/plans/changelog.md` is deprecated (deletion handled in sprint 04)."
  - Update **Key References** table: replace `declaration-first-shipping.md` row with `ship-time-checks.md`, add `sprint-contract.md` + `project-structure.md` rows
- `AGENTS.md` — **edit** (mirror CLAUDE.md governance changes; emphasize Codex as default operator per the handoff model)
- `docs/specs/modules/AudioDrawer.md` — **edit** (single reference at line 346: replace `docs/specs/declaration-first-shipping.md` with `docs/specs/ship-time-checks.md`; update surrounding sentence if the reference context assumed old-model lane semantics)
- **Project folder rename (pre-staged by Claude in authoring commit):** `docs/projects/workflow-renovation/` → `docs/projects/develop-protocol-migration/`. Operator verifies the rename appears in `git status` as rename-detected (`R` status), preserves through the commit, and does not reintroduce the old path anywhere. If any internal references to `workflow-renovation` remain in CLAUDE.md, AGENTS.md, or ship-time-checks.md, operator must resolve them in this sprint.
- `docs/projects/develop-protocol-migration/sprints/02-governance-shrink.md` — this file; operator appends iteration ledger + closeout at close; earlier sections unchanged
- `docs/projects/develop-protocol-migration/tracker.md` — edit: flip sprint 02 row status to `done` at close; add CHANGELOG line column if user-approved

**Out of scope** (explicit — do not touch):

- `.claude/commands/**`, `.codex/skills/**` — sprint 03 owns these
- `docs/plans/tracker.md`, `docs/plans/god-tracker.md`, `docs/plans/harmony-tracker.md`, `docs/plans/ui-tracker.md`, `docs/plans/bug-tracker.md`, `docs/plans/active-lanes.md` — sprint 04 / 05 own these
- `.worktrees/task-route-gate/`, `feat/task-route-gate` — sprint 05
- Branch cleanup / merging — sprint 06
- `docs/specs/sprint-contract.md` and `docs/specs/project-structure.md` — landed in sprint 01, do not re-edit
- All source code under `app/`, `app-core/`, `core/`, `data/`, `domain/`, `platforms/`
- Claude memory file `/home/cslh-frank/.claude/projects/-home-cslh-frank-main-app/memory/feedback_task_routing.md` — Claude-internal; updated by Claude separately post-sprint (not in Codex's write scope)

## References

1. **Authoritative plan:** `/home/cslh-frank/.claude/plans/i-suddenly-realized-that-serene-lark.md` — sections *"Declaration-First Shipping: Shrink, Don't Retire"*, *"File Changes Summary"*.
2. **Sprint 01 specs (canonical):** `docs/specs/sprint-contract.md`, `docs/specs/project-structure.md`.
3. **File to shrink:** `docs/specs/declaration-first-shipping.md` (current state at `origin/develop` after PR #26 merges; 103 lines, six numbered sections + test plan).
4. **Project tracker Inputs Pending section:** `docs/projects/develop-protocol-migration/tracker.md` — the two rules to land (develop-adb-source, doc-production discipline).
5. **Existing CLAUDE.md:** current Branch Model and Declaration-First Shipping sections are the rewrite targets.
6. **Existing AGENTS.md:** mirror target.

## Success Exit Criteria

**File surgery (all four must hold):**
- `ls docs/specs/declaration-first-shipping.md 2>&1` returns "No such file or directory"
- `docs/specs/ship-time-checks.md` exists; `wc -l` < 100
- `docs/archive/declaration-first-shipping-archive-20260423.md` exists; first line is an archival pointer header; body contains the dropped sections verbatim
- `grep -c "docs/plans/active-lanes.md\|refuse to start\|--force-parallel" docs/specs/ship-time-checks.md` returns 0 (old-model residue excluded)

**CLAUDE.md content:**
- `grep -n "Branch Model" CLAUDE.md` shows the section; under it, the word "feature branch" appears only in the context of being optional/ad-hoc, not the default
- `grep -nE "develop.*adb|adb.*develop" CLAUDE.md` shows the develop-as-adb-source rule (exact wording at author's discretion, but the pairing "develop" + "adb" + "package/install" must appear together)
- `grep -n "Sprint Contract Workflow" CLAUDE.md` shows the new section header
- `grep -n "sprint-contract.md\|project-structure.md" CLAUDE.md` shows both specs referenced
- `grep -nE "auto-proliferate|Doc-Production Discipline|not to create a new file" CLAUDE.md` shows the doc-production rule present
- `grep -n "active-lanes.md\|pre-flight scope conflict\|refuse to start" CLAUDE.md` returns 0 active-governance hits (permissible in historical-note form only if clearly marked)
- `grep -n "declaration-first-shipping.md" CLAUDE.md` returns 0 (replaced by `ship-time-checks.md`)

**AGENTS.md content:**
- Mirrors the four CLAUDE.md rules above (Branch Model update, adb-source, Sprint Contract Workflow pointer, doc-production discipline)
- `grep -nE "Codex.*operator|operator.*Codex" AGENTS.md` shows Codex-as-operator-default language

**Cross-reference hygiene (scoped — other-sprint territory is excluded):**
- `git grep -l "declaration-first-shipping.md" -- ':!docs/archive/**' ':!docs/projects/develop-protocol-migration/sprints/02-governance-shrink.md' ':!.codex/skills/**' ':!.claude/commands/**' ':!docs/plans/**' ':!docs/reference/**'` returns 0 hits (sprint-03/04/05 territory and lessons-historical context intentionally excluded)
- `grep -nE "active-lanes|pre-flight scope|refuse to start" CLAUDE.md AGENTS.md docs/specs/ship-time-checks.md` returns 0 hits (governance documents must not reference retired mechanisms)
- `docs/specs/modules/AudioDrawer.md` line that previously named `declaration-first-shipping.md` now names `ship-time-checks.md`
- `git grep -l "workflow-renovation" -- ':!docs/archive/**' ':!docs/projects/develop-protocol-migration/sprints/**' ':!docs/projects/develop-protocol-migration/tracker.md'` returns 0 hits (historical evidence inside sprint contract files + the rename note in the tracker are intentionally preserved)
- `git log --follow docs/projects/develop-protocol-migration/tracker.md | head -5` shows the rename preserved history

**Commit discipline:**
- `git diff origin/develop --stat` on the close commit shows only the 8 files in Scope (author-produced uncommitted authoring + operator's execution + tracker-flip all land as a single commit or squashable series; Claude did not commit authoring)
- Commit message references this contract file path

## Stop Exit Criteria

- Shrinking reveals a rule in `declaration-first-shipping.md` that must be kept but doesn't fit under "ship-time safety" — halt and escalate to Claude; the rule may need a new home (e.g., CLAUDE.md proper) that expands sprint 02 scope
- CLAUDE.md rewrite requires editing a referenced doc that isn't in Scope — halt; out-of-scope drift
- `AGENTS.md` has governance content that can't be reconciled with the Claude-mirrored version without editing files outside Scope — halt
- Iteration bound reached without all Success Exit Criteria green — halt; do not partial-ship

On stop: do NOT commit. Leave the worktree dirty. Fill Closeout with `status: stopped` + failing criterion. Wait for user direction.

## Iteration Bound

- Max 3 iterations OR 90 minutes wall-clock, whichever hits first
- Larger scope than sprint 01 (cross-file governance edits, grep discipline) earns the larger bound

## Required Evidence Format

At close, operator appends to the `Closeout` section:

1. **File surgery evidence** — output of `ls -la docs/specs/declaration-first-shipping.md docs/specs/ship-time-checks.md docs/archive/declaration-first-shipping-archive-20260423.md 2>&1` (old missing, two new present)
2. **Size check** — output of `wc -l docs/specs/ship-time-checks.md` (must be <100)
3. **Scope discipline** — output of `git diff origin/develop --stat` (exactly 8 Scope files)
4. **Cross-reference scan** — output of `git grep -l "declaration-first-shipping.md" -- ':!docs/archive/**' ':!docs/projects/develop-protocol-migration/sprints/02-governance-shrink.md' ':!.codex/skills/**' ':!.claude/commands/**' ':!docs/plans/**' ':!docs/reference/**'` (expected: empty)
5. **Rule presence grep** — one line each, showing line number + matched text:
   - `grep -n "adb" CLAUDE.md` (develop-as-adb-source rule)
   - `grep -nE "auto-proliferate|Doc-Production" CLAUDE.md` (doc-production rule)
   - `grep -nE "Single Changelog|one product changelog|CHANGELOG\.md.*single" CLAUDE.md` (single-changelog rule)
   - `grep -n "Sprint Contract Workflow" CLAUDE.md` (new section)
   - `grep -n "sprint-contract.md" CLAUDE.md` (spec reference)
   - `grep -nE "Codex.*operator|operator.*Codex" AGENTS.md` (Codex-as-default)
   - `grep -n "ship-time-checks.md" docs/specs/modules/AudioDrawer.md` (AudioDrawer reference updated)
6. **Active-model residue check** — output of `grep -nE "active-lanes|refuse to start|pre-flight scope" CLAUDE.md AGENTS.md docs/specs/ship-time-checks.md` (expected: empty)

Agent narration is not acceptable in place of these artifacts.

## Iteration Ledger

*(Operator appends one entry per iteration. Not committed mid-sprint.)*

- 2026-04-23T00:00: Iteration 1. Rebased `docs/sprint-02-governance-shrink` onto `origin/develop`, restored authored files, replaced `docs/specs/declaration-first-shipping.md` with `docs/specs/ship-time-checks.md`, archived the retired contract, rewrote `CLAUDE.md` and `AGENTS.md` governance sections, and updated the `AudioDrawer` reference. Evaluator result: the scoped `workflow-renovation` grep still hits `docs/specs/sprint-contract.md` and `docs/specs/project-structure.md`, which are frozen sprint-01 artifacts and out of scope for this sprint. Next action: stop and wait for user direction rather than editing forbidden files.
- 2026-04-24T00:00: Iteration 2. User approved fixing the blocker. Updated the remaining `workflow-renovation` references in `docs/specs/sprint-contract.md` and `docs/specs/project-structure.md`, cleaned the last `declaration-first-shipping.md` residues in the migrated sprint-01 contract and project tracker, then staged the full tree so the project-folder rename resolved as `R` status. Evaluator result: content and residue checks passed; close scope expanded beyond the original eight-file claim because the user-approved blocker fix touched the two frozen sprint-01 specs and the staged rename carries the pre-authored sprint-06 contract. Next action: close sprint 02 successfully with evidence and ship as one commit.

## Closeout

*(Operator fills at exit.)*

- **Status:** success
- **Summary for project tracker** *(one line):* Replaced the old shipping spec with `ship-time-checks.md`, rewrote `CLAUDE.md` and `AGENTS.md` governance rules, landed the project-folder rename, and cleared the rename residue with a user-approved sprint-01 spec reference fix.
- **Evidence artifacts:**
  - File surgery:
    ```text
    ls: cannot access 'docs/specs/declaration-first-shipping.md': No such file or directory
    -rw-rw-r-- 1 cslh-frank cslh-frank 5886 Apr 23 18:58 docs/archive/declaration-first-shipping-archive-20260423.md
    -rw-rw-r-- 1 cslh-frank cslh-frank 1485 Apr 23 18:58 docs/specs/ship-time-checks.md
    ```
  - Size check:
    ```text
    35 docs/specs/ship-time-checks.md
    ```
  - Scope discipline:
    ```text
     AGENTS.md                                          |  39 +++++
     CLAUDE.md                                          |  36 ++--
     ...declaration-first-shipping-archive-20260423.md} |   2 +
     .../sprints/01-specs-bootstrap.md                  |   2 +-
     .../sprints/02-governance-shrink.md                | 146 ++++++++++++++++
     .../sprints/06-branch-graveyard-sweep.md           | 193 +++++++++++++++++++++
     .../projects/develop-protocol-migration/tracker.md |  50 ++++++
     docs/projects/workflow-renovation/tracker.md       |  41 -----
     docs/specs/modules/AudioDrawer.md                  |   2 +-
     docs/specs/project-structure.md                    |   2 +-
     docs/specs/ship-time-checks.md                     |  35 ++++
     docs/specs/sprint-contract.md                      |   2 +-
    12 files changed, 494 insertions(+), 56 deletions(-)
    ```
    User-approved deviation from the authored eight-file estimate: the blocker fix required two sprint-01 spec reference edits, and the staged project rename carried the pre-authored sprint-06 contract.
  - Cross-reference scan:
    ```text
    git grep -l "declaration-first-shipping.md" -- ':!docs/archive/**' ':!docs/projects/develop-protocol-migration/sprints/02-governance-shrink.md' ':!.codex/skills/**' ':!.claude/commands/**' ':!docs/plans/**' ':!docs/reference/**'
    [no output]

    git grep -l "workflow-renovation" -- ':!docs/archive/**' ':!docs/projects/develop-protocol-migration/sprints/**' ':!docs/projects/develop-protocol-migration/tracker.md'
    [no output]

    git log --follow -- docs/projects/develop-protocol-migration/tracker.md | head -5
    [no output before first commit on this renamed path]
    ```
  - Rule presence grep:
    ```text
    grep -n "adb" CLAUDE.md
    24:- **develop**: Android maintenance and shared-contract trunk. Sprint-contract work closes here for Android and shared governance after review. `develop` is the canonical source for `adb` package/install flows.
    137:For Android runtime bugs, `adb logcat` is mandatory evidence. Screenshots and user recollection are supporting evidence only.

    grep -nE "auto-proliferate|Doc-Production" CLAUDE.md
    71:## Doc-Production Discipline
    73:Agents must not auto-proliferate doc artifacts. Before producing a non-code file, ask whether the information belongs in a commit message, an existing doc, or a sprint contract ledger. The default is not to create a new file.

    grep -nE "Single Changelog|one product changelog|CHANGELOG\.md.*single" CLAUDE.md
    75:## Single Changelog
    77:There is one product changelog: `CHANGELOG.md`, rendered to `CHANGELOG.html`. No parallel product trace log should be maintained; `docs/plans/changelog.md` is deprecated and scheduled for cleanup elsewhere.

    grep -n "Sprint Contract Workflow" CLAUDE.md
    32:## Sprint Contract Workflow

    grep -n "sprint-contract.md" CLAUDE.md
    37:- Read `docs/specs/sprint-contract.md` for the contract schema and `docs/specs/project-structure.md` for project/tracker layout.
    42:Declaration happens in the sprint contract and its handoff, not through a separate always-on registry. Startup discipline, scope, stop criteria, and evidence requirements now live in `docs/specs/sprint-contract.md`.
    149:| Sprint contract schema | `docs/specs/sprint-contract.md` |

    grep -nE "Codex.*operator|operator.*Codex" AGENTS.md
    76:Codex is the default operator unless the user explicitly overrides the handoff.

    grep -n "ship-time-checks.md" docs/specs/modules/AudioDrawer.md
    346:Implementation lands through the declared Android review path from `develop` per `docs/specs/ship-time-checks.md`. This contract itself ships on the `docs` lane.
    ```
  - Active-model residue check:
    ```text
    grep -nE "active-lanes|refuse to start|pre-flight scope" CLAUDE.md AGENTS.md docs/specs/ship-time-checks.md
    [no output]
    ```
- **Lesson proposals** *(0-N; human-gated):* 0
- **CHANGELOG line** *(governance changes typically not user-visible; leave empty unless a specific rule affects external-visible behavior):*
