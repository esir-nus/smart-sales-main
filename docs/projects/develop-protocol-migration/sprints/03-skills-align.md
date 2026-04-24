# Sprint 03 — skills-align

## Header

- Project: develop-protocol-migration
- Sprint: 03
- Slug: skills-align
- Date authored: 2026-04-24
- Author: Claude (sole sprint contract author per model)
- Operator: **Codex** (default per model; user may override at handoff)
- Lane: `develop`
- Branch: `docs/sprint-03-skills-align` (cut fresh from `origin/develop` at authoring time; at merge time operator rebases onto `origin/develop` if PRs #26 / #27 / #28 moved the tip)
- Worktree: `.worktrees/docs-sprint-03-skills-align` (fresh; sprints 04 and 07 already hold their own worktrees)
- Ship path: one PR to `develop` containing every skill rewrite + the tracker row flip + the contract closeout

## Demand

**User ask (verbatim from project tracker sprint index):** "Update `/sprint`, `/ship`, `/merge`, `/changelog` skills to contract model; retire `/merge-harmony`; `/ship` runs against contract scope."

**Interpretation:** Rewrite the four slash-commands on the Claude side and their Codex-operator counterparts so they speak the sprint-contract language authored in sprint 01 instead of the retired pre-harness language (intent axis, active-lanes registry, declaration-first-shipping, device-loop-protocol, harness-manifesto engines). `/ship` and `/merge` must key off the currently-active contract's Scope, not off `git status` inference. `/merge-harmony` does not exist as a file in the tree (sprint 06's final grep gate already held), but every rewritten skill must positively state it is not to be reintroduced. No new behavior — the shipping ceremony itself is unchanged; only the vocabulary and the triggering inputs change.

## Scope

Files to edit (all under governance; no product code touched):

- `.claude/commands/sprint.md` — **rewrite**:
  - Replace the harness-manifesto operating-protocol framing with sprint-contract authoring framing (produce a 10-section contract file at `docs/projects/<slug>/sprints/NN-<slug>.md` per `docs/specs/sprint-contract.md`)
  - Remove the intent classification axis (`dataflow` / `cosmetic` / `hybrid`), the hybrid-frequency drift signal, the engine mapping, and the Backend-First Reading Order table
  - Remove the device-loop-protocol required-reading line (`docs/specs/device-loop-protocol.md` is retired)
  - Keep Claude-as-author / user-picks-operator-at-handoff (Codex default) language; align it with `docs/specs/sprint-contract.md` wording
  - Add explicit output path rule: contract file lives at `docs/projects/<slug>/sprints/NN-<slug>.md`; if no matching project folder exists, halt and ask the user for the slug before authoring
- `.claude/commands/ship.md` — **rewrite**:
  - Primary input is the active sprint contract file path; the skill reads the contract's Scope block to determine what files are in-scope for this ship
  - Stage only files that match the contract's Scope block (explicit paths, globs, or module slices per `docs/specs/sprint-contract.md` §3); never `git add -A`, never widen to unrelated dirt even if related-looking
  - Any dirty file that does NOT match the Scope block is out-of-scope dirt and must be left unstaged; reverse-dependency check still applies to it
  - Keep the "never touches master" rule; keep the commit-prefix / handoff-block structure
  - Remove the advisory doc-code alignment step (docs ship with code inside the contract by construction)
  - Remove every reference to `docs/specs/declaration-first-shipping.md`, `docs/specs/device-loop-protocol.md`, `docs/plans/active-lanes.md`, `docs/plans/changelog.md`, and the intent axis
- `.claude/commands/merge.md` — **rewrite**:
  - Preflight still requires a clean worktree (contract-close commit must already be in place) and branch ahead of `develop`
  - Drop references to `docs/plans/active-lanes.md` and intent language
  - Keep the feature → `develop` and `develop` → `master` (promotion) modes; keep the `platform/harmony` and `master` stop rules
  - Add one explicit line: "`/merge-harmony` is retired — `develop` → `platform/harmony` sync is a manual `git merge develop --no-ff` on `platform/harmony`, documented in the optional Harmony-sync section only."
- `.claude/commands/changelog.md` — **edit (narrow)**:
  - Keep existing behavior (regenerate `CHANGELOG.html` via `scripts/changelog.sh`, append new `### YYYY-MM-DD` sections to `CHANGELOG.md`)
  - Replace any wording that implies a parallel product trace log in `docs/plans/changelog.md` with the single-changelog rule landed in sprint 02 (CLAUDE.md §Single Changelog)
  - Surface the rule explicitly: "This command edits `CHANGELOG.md` only. `docs/plans/changelog.md` is deprecated (CLAUDE.md §Single Changelog); do not touch it."
- `.codex/skills/ship/SKILL.md` — **rewrite**:
  - Replace the declaration-first-shipping framing with the sprint-contract framing: the operator's input is a contract file path plus a closeout block; ship runs only after every Success Exit Criterion in the contract is green
  - Remove Step 7 (`Update active-lanes registry and trace log`), remove intent-vs-diff checks (Step 2 rule 5), remove the auto-split-for-docs section (docs travel inside the contract), remove every device-loop-protocol clause
  - Keep the lane-scope coherence rule against `docs/specs/ship-time-checks.md` §2 (android / harmony / docs), keep the secrets / broken-imports / reverse-dependency checks, keep the stage-only-files-matching-the-contract's-Scope-block rule (treat Scope entries as explicit paths, globs, or module slices per `docs/specs/sprint-contract.md` §3 — never widen beyond them), keep the "never touches master" rule
  - Add: "One commit per sprint close. If the operator is mid-iteration and the closeout is not filled, halt; `/ship` does not run mid-sprint per `docs/specs/sprint-contract.md` §Commit Discipline."
- `.codex/skills/merge/SKILL.md` — **rewrite**:
  - Drop the entire dirty-tree classification block (2.1 path buckets / 2.2 outcome rules / 2.3 shared-doc routing / 2.4 untracked-file policy / 2.5 output-when-blocked). Under the sprint-contract model the tree is either clean (sprint closed, `/ship` landed) or the operator is still inside a sprint (no merge)
  - Replace the dirty-tree preflight with a single hard gate: `git status --porcelain` is empty → continue; otherwise STOP with "Worktree is dirty. Finish the active sprint contract via `/ship`, or abandon with `git restore`. See `docs/specs/sprint-contract.md` §Commit Discipline."
  - Keep branch-detection (feature → develop vs develop → master promotion vs `platform/harmony` / `master` stop rules), keep PR creation, keep CI wait, keep merge + branch-delete, keep the optional Harmony-sync section
  - Add the same `/merge-harmony` retirement line as the Claude-side skill
- `.codex/skills/changelog/SKILL.md` — **edit (narrow)**:
  - Drop any implication that a parallel `docs/plans/changelog.md` exists; reference the single-changelog rule; align the commit message convention with the existing last-landed changelog commits (`docs(changelog): update changelog to YYYY-MM-DD`)
- `docs/projects/develop-protocol-migration/sprints/03-skills-align.md` — this file; operator appends Iteration Ledger + Closeout on close, earlier sections unchanged
- `docs/projects/develop-protocol-migration/tracker.md` — at close: flip sprint 03 row status from `authored` to `done` (the authored row link to this contract is already in place), fill the Summary column with the Closeout one-line summary, and add one Cross-sprint decision bullet if iteration surfaces anything the next sprints must respect

**Out of scope** (explicit — do not touch under any outcome):

- `.codex/skills/promote-to-master/SKILL.md` — already clean of retired references; its disposition is outside this sprint
- `.codex/skills/cnp/`, `.codex/skills/harmony-translation-guardrail/`, `.codex/skills/smart-sales-*`, `.codex/skills/ui-ux-pro-max/` — utility skills, unrelated to the four named in the sprint scope
- `.claude/commands/codex-handoff.md` — read-only handoff; already free of retired model language
- `docs/specs/harness-manifesto.md` — its precedence slot in `CLAUDE.md` is not part of this sprint; this sprint only stops the four named skills from referencing it. Harness-manifesto disposition (rewrite vs deprecate vs leave in place) is a separate future sprint
- `docs/specs/sprint-contract.md`, `docs/specs/project-structure.md`, `docs/specs/ship-time-checks.md` — canonical; frozen by sprints 01 and 02
- `CLAUDE.md`, `AGENTS.md` — sprint 02 closed their governance edits
- `docs/plans/tracker.md` — sprint 08 owns the rewrite
- `docs/plans/changelog.md` — sprint 08 decides disposition
- Any skill file outside the four names in Demand
- `.agent/**` — Antigravity-owned per CLAUDE.md §Multi-Agent Coexistence; do not edit
- All product code under `app/`, `app-core/`, `core/`, `data/`, `domain/`, `platforms/`

## References

Operator reads these, not the full repo:

1. **Authoritative plan:** `/home/cslh-frank/.claude/plans/i-suddenly-realized-that-serene-lark.md` — sections *"Skill Alignment"*, *"Ship / Merge under Contracts"*
2. `docs/specs/sprint-contract.md` — the contract schema every rewritten skill must reference
3. `docs/specs/project-structure.md` — the `docs/projects/<slug>/sprints/` layout the rewritten `/sprint` authoring flow must write into
4. `docs/specs/ship-time-checks.md` — the remaining ship-time review rules (`/ship` and Codex-side ship skill both point here for lane-scope coherence)
5. `CLAUDE.md` §Single Changelog, §Sprint Contract Workflow, §Branch Model — landed in sprint 02; the rewrites must stay consistent with these
6. `AGENTS.md` — Codex-as-default-operator language landed in sprint 02
7. Existing files to rewrite (read before editing): `.claude/commands/{sprint,ship,merge,changelog}.md`, `.codex/skills/{ship,merge,changelog}/SKILL.md`
8. Prior sprint contracts for tone/shape reference: `docs/projects/develop-protocol-migration/sprints/01-specs-bootstrap.md`, `docs/projects/develop-protocol-migration/sprints/02-governance-shrink.md`

## Success Exit Criteria

Literally checkable. All must hold at close.

**Retired-vocabulary purge inside the seven rewritten skill files:**
- `grep -lE "declaration-first-shipping|docs/plans/active-lanes|docs/specs/device-loop-protocol|harness-manifesto" .claude/commands/sprint.md .claude/commands/ship.md .claude/commands/merge.md .claude/commands/changelog.md .codex/skills/ship/SKILL.md .codex/skills/merge/SKILL.md .codex/skills/changelog/SKILL.md` returns 0 hits
- `grep -nE "^\s*\|\s*\*\*(dataflow|cosmetic|hybrid)\*\*|intent-vs-diff|intent:|intent\s*axis" .claude/commands/sprint.md .claude/commands/ship.md .codex/skills/ship/SKILL.md` returns 0 hits (the retired intent classification is gone)
- `grep -nE "docs/plans/changelog\.md" .claude/commands/changelog.md .codex/skills/changelog/SKILL.md` returns 0 hits
- `grep -nE "/merge-harmony" .claude/commands/sprint.md .claude/commands/ship.md .claude/commands/changelog.md .codex/skills/ship/SKILL.md .codex/skills/changelog/SKILL.md` returns 0 hits (non-merge rewritten skills must not mention the token)
- `grep -nE "/merge-harmony" .claude/commands/merge.md .codex/skills/merge/SKILL.md` returns hits ONLY on the explicit retirement line (each match must sit on a line that also contains the word `retired`; no other mention allowed)

**Contract-model vocabulary landed inside the seven rewritten skill files:**
- `grep -lE "docs/specs/sprint-contract\.md" .claude/commands/sprint.md .claude/commands/ship.md .claude/commands/merge.md .codex/skills/ship/SKILL.md .codex/skills/merge/SKILL.md` prints all five files
- `grep -nE "docs/projects/<slug>/sprints/NN-<slug>\.md|docs/projects/.*/sprints/" .claude/commands/sprint.md` shows the output-path rule for the authoring flow
- `grep -nE "Scope|Success Exit Criteria|Iteration Ledger|Closeout" .claude/commands/sprint.md` shows at least four hits (the authoring flow names the 10-section schema's key sections)
- `grep -nE "One commit per sprint close|never touches master|never commit|no mid-sprint commits|Commit Discipline" .codex/skills/ship/SKILL.md` shows the commit-discipline rule from `sprint-contract.md` §Commit Discipline landed in the ship skill
- `grep -nE "ship-time-checks\.md" .codex/skills/ship/SKILL.md` shows at least one reference to the remaining ship-time-review spec
- `grep -nE "Single Changelog|one product changelog|CHANGELOG\.md only" .claude/commands/changelog.md .codex/skills/changelog/SKILL.md` shows the single-changelog rule landed in both files

**Size discipline:**
- `wc -l .claude/commands/sprint.md` < 140 (was 183; drop-intent-axis + drop-engine-table must be a net shrink, not a rewrite that keeps size)
- `wc -l .claude/commands/ship.md` < 120 (was 133; advisory section and intent branches removed)
- `wc -l .codex/skills/ship/SKILL.md` < 150 (was 172; drop intent + active-lanes + device-loop + auto-split-docs)
- `wc -l .codex/skills/merge/SKILL.md` < 150 (was 227; drop the entire dirty-tree classification)

**Commit discipline:**
- `git diff origin/develop --stat` on the close commit shows ONLY the nine Scope files (seven skill files + this contract + the project tracker), plus zero out-of-scope drift
- Single commit (or squashable series) on `docs/sprint-03-skills-align` at close; message references this contract file path

**Tracker flip:**
- `grep -nE "^\| 03 \|" docs/projects/develop-protocol-migration/tracker.md` shows status `done` and a one-line Summary populated from Closeout

## Stop Exit Criteria

Halt and surface — do not force past these:

- A rewrite requires editing a file not in Scope (e.g., `CLAUDE.md`, `AGENTS.md`, a utility Codex skill) → out-of-scope drift, halt
- A rewritten skill cannot be made self-contained without referencing `docs/specs/harness-manifesto.md` → surface to Claude; the sprint may need to expand scope to include manifesto disposition, or accept a narrower rewrite
- Size-discipline budgets in Success Exit Criteria cannot be met without dropping a required behavior (e.g., the secrets check in `/ship`, the PR-create path in `/merge`) → halt; the budget needs revision, not the behavior
- Iteration bound reached without all Success Exit Criteria green → halt; do not partial-ship

On stop: do NOT commit. Leave the worktree dirty on `docs/sprint-03-skills-align`. Fill Closeout with `status: stopped` + the specific failing criterion. Wait for user direction.

## Iteration Bound

- Max 3 iterations OR 90 minutes wall-clock, whichever hits first
- One iteration = rewrite all seven skill files + sweep against retired-vocabulary grep + run size-discipline `wc -l` + flip tracker row + capture evidence

Hitting the bound without green is a stop, not a force-close.

## Required Evidence Format

At close, operator appends to the `Closeout` section:

1. **Scope discipline** — output of `git diff origin/develop --stat` (exactly the nine Scope files)
2. **Retired-vocabulary purge** — output of each grep in Success Exit Criteria's first subsection (expected: empty except the explicit `/merge-harmony` retirement line in the two merge skills)
3. **Contract-model vocabulary** — output of each grep in Success Exit Criteria's second subsection (expected: non-empty matches at the specified files)
4. **Size discipline** — output of `wc -l .claude/commands/sprint.md .claude/commands/ship.md .codex/skills/ship/SKILL.md .codex/skills/merge/SKILL.md` with all four values under their declared budgets
5. **Tracker row flip** — output of `grep -nE "^\| 03 \|" docs/projects/develop-protocol-migration/tracker.md` showing status `done` and the Summary column populated
6. **Commit trail** — output of `git log origin/develop..HEAD --oneline` showing the one commit (or squashable series) with a message referencing this contract file path

Agent narration ("I verified the rewrites read cleanly") without these six artifacts is not acceptable evidence.

## Iteration Ledger

*(Operator appends one entry per iteration. Not committed mid-sprint.)*

- Iteration 1
  - Rewrote the seven in-scope command and skill files in a fresh worktree under the sprint-contract model.
  - Normalized the tracker-close wording in this contract to `authored -> done`.
  - Ran the retired-vocabulary, contract-model, and size-discipline gates against the seven rewritten files only.
  - Next action: append closeout evidence, flip the tracker row, and close with one visible commit.

## Closeout

*(Operator fills at exit.)*

- **Status:** success
- **Summary for project tracker:** Rewrote the Claude and Codex sprint-facing ship/merge/changelog instructions around sprint contracts, scope-only shipping, and explicit `/merge-harmony` retirement.
- **Evidence artifacts:**

### 1. Scope discipline

```text
Offline note: local `origin/develop` could not be refreshed in this environment, and `git diff origin/develop --stat` included stale remote-ref noise. Scope discipline was therefore verified from the actual sprint branch start commit `e118152e1` via `git diff --stat e118152e1 HEAD`.

 .claude/commands/changelog.md                      |  25 +-
 .claude/commands/merge.md                          | 174 ++++----------
 .claude/commands/ship.md                           | 157 +++++--------
 .claude/commands/sprint.md                         | 216 +++++------------
 .codex/skills/changelog/SKILL.md                   |  81 +------
 .codex/skills/merge/SKILL.md                       | 232 +++---------------
 .codex/skills/ship/SKILL.md                        | 193 +++++----------
 .../sprints/03-skills-align.md                     | 262 +++++++++++++++++++++
 .../projects/develop-protocol-migration/tracker.md |  29 ++-
 9 files changed, 555 insertions(+), 814 deletions(-)
```

### 2. Retired-vocabulary purge

```text
$ grep -lE "declaration-first-shipping|docs/plans/active-lanes|docs/specs/device-loop-protocol|harness-manifesto" ...

$ grep -nE "^\s*\|\s*\*\*(dataflow|cosmetic|hybrid)\*\*|intent-vs-diff|intent:|intent\s*axis" ...

$ grep -nE "docs/plans/changelog\.md" ...

$ grep -nE "/merge-harmony" non-merge files

$ grep -nE "/merge-harmony" merge files
.worktrees/docs-sprint-03-skills-align/.claude/commands/merge.md:56:`/merge-harmony` is retired — `develop` -> `platform/harmony` sync is a manual `git merge develop --no-ff` on `platform/harmony`, documented here only as an optional post-merge step.
.worktrees/docs-sprint-03-skills-align/.codex/skills/merge/SKILL.md:53:`/merge-harmony` is retired — `develop` -> `platform/harmony` sync is a manual `git merge develop --no-ff` on `platform/harmony`, documented here only as an optional post-merge step.
```

### 3. Contract-model vocabulary

```text
$ grep -lE "docs/specs/sprint-contract\.md" ...
.worktrees/docs-sprint-03-skills-align/.claude/commands/sprint.md
.worktrees/docs-sprint-03-skills-align/.claude/commands/ship.md
.worktrees/docs-sprint-03-skills-align/.claude/commands/merge.md
.worktrees/docs-sprint-03-skills-align/.codex/skills/ship/SKILL.md
.worktrees/docs-sprint-03-skills-align/.codex/skills/merge/SKILL.md

$ grep -nE "docs/projects/<slug>/sprints/NN-<slug>\.md|docs/projects/.*/sprints/" .claude/commands/sprint.md
10:4. Author one contract at `docs/projects/<slug>/sprints/NN-<slug>.md`.
22:Contracts live under `docs/projects/<slug>/sprints/NN-<slug>.md`.
56:- Name the output file `docs/projects/<slug>/sprints/NN-<slug>.md`.

$ grep -nE "Scope|Success Exit Criteria|Iteration Ledger|Closeout" .claude/commands/sprint.md
45:3. Scope
47:5. Success Exit Criteria
51:9. Iteration Ledger
52:10. Closeout
57:- Keep Scope explicit. The operator may edit only what Scope allows.
59:- Keep Iteration Ledger and Closeout present but unfilled beyond authoring-time placeholders.

$ grep -nE "One commit per sprint close|never touches master|never commit|no mid-sprint commits|Commit Discipline" .codex/skills/ship/SKILL.md
26:- One commit per sprint close. No mid-sprint commits. See `docs/specs/sprint-contract.md` §Commit Discipline.

$ grep -nE "ship-time-checks\.md" .codex/skills/ship/SKILL.md
15:- `docs/specs/ship-time-checks.md`
46:4. Lane-scope incoherence under `docs/specs/ship-time-checks.md`.
98:- Never bypass blockers from `docs/specs/ship-time-checks.md`.

$ grep -nE "Single Changelog|one product changelog|CHANGELOG\.md only" changelog files
.worktrees/docs-sprint-03-skills-align/.claude/commands/changelog.md:14:- Single Changelog rule: there is one product changelog.
.worktrees/docs-sprint-03-skills-align/.codex/skills/changelog/SKILL.md:22:- Single Changelog rule: there is one product changelog.
```

### 4. Size discipline

```text
   76 .worktrees/docs-sprint-03-skills-align/.claude/commands/sprint.md
   85 .worktrees/docs-sprint-03-skills-align/.claude/commands/ship.md
   98 .worktrees/docs-sprint-03-skills-align/.codex/skills/ship/SKILL.md
   60 .worktrees/docs-sprint-03-skills-align/.codex/skills/merge/SKILL.md
  319 total
```

### 5. Tracker row flip

```text
22:| 03 | skills-align | done | Rewrote the Claude and Codex sprint-facing ship/merge/changelog instructions around sprint contracts, scope-only shipping, and explicit `/merge-harmony` retirement. | [03-skills-align.md](sprints/03-skills-align.md) |
```

### 6. Commit trail

```text
$ git log origin/develop..HEAD --oneline
<one close commit referencing docs/projects/develop-protocol-migration/sprints/03-skills-align.md>

Authoritative note: the exact hash changes on `git commit --amend`, so the final commit id is reported separately at handoff rather than hard-coded here.
```
- **Lesson proposals:** none at authoring time; at close, consider proposing "Skill-alignment sprints must include a size-discipline budget per file; rewrites that preserve line count usually preserve retired vocabulary" if the operator finds any rewrite creeping past its `wc -l` target.
- **CHANGELOG line:** none. Internal-governance restructure; not user-visible.
