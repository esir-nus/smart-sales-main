# Sprint 01 — specs-bootstrap

## Header

- Project: workflow-renovation
- Sprint: 01
- Slug: specs-bootstrap
- Date authored: 2026-04-23
- Author: Claude (sole sprint contract author per model)
- Operator: **Claude** (explicit user override — docs work, author-executes-own-work)
- Lane: `develop`
- Branch: `docs/sprint-01-specs-bootstrap` (created from `origin/develop` at `c9d21be1b`)
- Worktree: `.worktrees/develop-protocol`
- Ship path: feature-branch PR into `develop` (renovation skills not yet rewired — use current `/ship`)

## Demand

**User ask (verbatim):** "Go. Claude can author the first sprint contract as the bootstrap."

**Interpretation:** Sprint 01 produces the two foundational spec files that every downstream sprint references. Docs-only, no code, no skill changes, no tracker migrations. The contract schema and project structure get codified as canonical specs. This sprint also doubles as the exemplar — its own contract file is the first real instance of what `sprint-contract.md` codifies.

## Scope

Holistic (per the project model — docs + code + UI together when relevant; this sprint is docs-only):

- `docs/specs/sprint-contract.md` (new) — the contract schema spec
- `docs/specs/project-structure.md` (new) — the `docs/projects/<slug>/` layout + lifecycle spec
- `docs/projects/workflow-renovation/sprints/01-specs-bootstrap.md` (this file — operator appends iteration ledger + closeout; must not edit earlier sections)
- `docs/projects/workflow-renovation/tracker.md` (existing; operator updates sprint 01 row to `done` on close)

**Out of scope** (explicit — do not touch):

- `CLAUDE.md`, `AGENTS.md`, `docs/specs/ship-time-checks.md` — sprint 02 owns these
- `.claude/commands/*`, `.codex/skills/*` — sprint 03 owns these
- `docs/plans/tracker.md`, `docs/plans/god-tracker.md`, `docs/plans/harmony-tracker.md`, the retired lane-registry file, `docs/reports/**` — sprint 04 / 05 own these
- `.worktrees/task-route-gate/`, `feat/task-route-gate` branch — sprint 05 owns these
- All source code under `app/`, `app-core/`, `core/`, `data/`, `domain/`, `platforms/`

## References

Operator reads these, not the full repo. In priority order:

1. **Authoritative plan:** `/home/cslh-frank/.claude/plans/i-suddenly-realized-that-serene-lark.md` — sections *"Contract Schema"*, *"Project Structure"*, *"Agent Roles & Handoff"*. This is the primary spec source.
2. **This contract file** — doubles as the exemplar. `sprint-contract.md` must accommodate the shape of this file without contortions.
3. **Index+details template:** `.agent/rules/lessons-learned.md` (short index) and `docs/reference/agent-lessons-details.md` (details). Both new specs propagate this pattern.
4. **Baseline tag:** `pre-renovation-baseline` at `origin/develop` `c9d21be1b`. Rollback point if needed.

## Success Exit Criteria

All of the following, literally checkable:

**`docs/specs/sprint-contract.md`:**
- Exists and is under 200 lines
- Defines the 10 required sections: Header, Demand, Scope, References, Success Exit Criteria, Stop Exit Criteria, Iteration Bound, Required Evidence Format, Iteration Ledger, Closeout
- Includes an explicit rule: Success Exit Criteria must be *literally checkable* (commands, file paths, line counts, test names) — no prose-only criteria
- Includes an explicit rule: Required Evidence must include ≥1 external artifact (log excerpt, command output, screenshot path) — agent narration alone is insufficient
- Includes an explicit rule: operator does NOT commit mid-sprint; commit + push only on close (success) OR leaves uncommitted (stopped/blocked)
- Includes an explicit rule: Claude is the sole sprint contract author; operator is chosen by the user at handoff (Codex default, Claude on explicit override)
- States that the spec is template-is-the-spec (the spec itself demonstrates the schema)

**`docs/specs/project-structure.md`:**
- Exists and is under 200 lines
- Defines the `docs/projects/<slug>/` layout: `tracker.md`, `sprints/<NN>-<slug>.md`, `evidence/<NN>-<slug>/` (optional)
- Defines the project tracker's required fields: objective, status, sprint index, cross-sprint decisions, lessons pointer
- Defines the project lifecycle: open → sprints execute → close (tracker archives, CHANGELOG line aggregates if user-visible)
- States the ~6-sprint decomposition guideline and the review prompt that fires when a project exceeds it
- Propagates the index+details pattern as a repo-wide convention for any governance doc that crosses ~200 lines

**Sprint file discipline:**
- This contract file (`01-specs-bootstrap.md`) has `Iteration Ledger` and `Closeout` filled; all other sections unchanged from author state
- `tracker.md` has sprint 01 row set to `done` with a one-line summary in the Summary column

**Commit discipline:**
- `git diff origin/develop --stat` on the close commit shows ONLY the four files listed in Scope
- Single commit on `docs/sprint-01-specs-bootstrap` at close (multiple commits during iteration are fine to amend/squash into one before push)

## Stop Exit Criteria

Halt and surface — do not force past these:

- Either spec cannot be kept under 200 lines without losing an essential rule listed in Success Exit Criteria → the schema itself needs revision; escalate to Claude for a contract amendment before proceeding
- Writing either spec requires editing a file not in Scope → out-of-scope drift; escalate
- Iteration bound reached (see below) without all Success Exit Criteria green → stop; do not partial-ship

On stop: do NOT commit. Leave the working tree dirty on `docs/sprint-01-specs-bootstrap`. Fill `Closeout` with `status: stopped` + the specific criterion that failed, and wait for user direction.

## Iteration Bound

- Max 3 iterations OR 45 minutes wall-clock, whichever hits first
- One iteration = draft → self-review against Success Exit Criteria → adjust
- Hitting the bound without green = stop per above

## Required Evidence Format

At close, operator appends to the `Closeout` section of this file:

1. **Spec size check** (external artifact): output of `wc -l docs/specs/sprint-contract.md docs/specs/project-structure.md`
2. **Scope discipline check** (external artifact): output of `git diff origin/develop --stat` — must show exactly the four Scope files
3. **Self-application check** (one line each, not prose-only — point to specific sections):
   - Which section of `sprint-contract.md` describes the 10 required sections used by THIS contract file?
   - Which section of `project-structure.md` describes the folder containing this contract?

Agent narration ("I reviewed the criteria and they look right") without these three artifacts is not acceptable evidence.

## Iteration Ledger

- **Iteration 1 (2026-04-23, operator Claude):**
  - Tried: drafted `docs/specs/sprint-contract.md` (10-section schema, template-is-the-spec) and `docs/specs/project-structure.md` (folder layout, tracker fields, lifecycle, six-sprint guideline, index+details, doc-production discipline).
  - Evaluator saw: both specs under 200 lines (58 and 81); all 10 required sections present in contract spec in correct order; four scope files and no others in git intent-to-add preview.
  - Next action: fill ledger and closeout, flip tracker row, commit as one.

## Closeout

- **Status:** success
- **Summary for project tracker:** Wrote `sprint-contract.md` (58 lines) and `project-structure.md` (81 lines); both under the 200-line ceiling. Schema is self-applying (this file conforms to it).
- **Evidence artifacts:**
  - `wc -l` output:
    ```
    58 docs/specs/sprint-contract.md
    81 docs/specs/project-structure.md
    ```
  - `git diff --stat` output (intent-to-add preview against `origin/develop`):
    ```
    docs/projects/workflow-renovation/sprints/01-specs-bootstrap.md |  122 +++++
    docs/projects/workflow-renovation/tracker.md                    |   41 +++
    docs/specs/project-structure.md                                 |   81 +++++
    docs/specs/sprint-contract.md                                   |   58 +++
    4 files changed
    ```
    Exactly the four Scope files; no out-of-scope drift.
  - Self-application pointers:
    - `sprint-contract.md` § **Required Sections** (lines 13–26) describes the 10 required sections that THIS contract file implements.
    - `project-structure.md` § **Folder Layout** (lines 7–21) describes `docs/projects/<slug>/sprints/` where this contract file lives.
- **Lesson proposals:** none. No new trigger/symptom pair emerged from writing docs-only specs.
- **CHANGELOG line:** none. Internal-governance sprint; not user-visible.
