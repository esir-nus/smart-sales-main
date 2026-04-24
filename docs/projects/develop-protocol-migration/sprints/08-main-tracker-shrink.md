# Sprint 08 — main-tracker-shrink

## Header

- Project: develop-protocol-migration
- Sprint: 08
- Slug: main-tracker-shrink
- Date authored: 2026-04-24
- Author: Claude
- Operator: **Codex** (default operator)
- Lane: `develop` (top-level tracker + deprecated changelog + one archival doc + narrow non-docs coupling)
- Branch: `docs/sprint-08-main-tracker-shrink` (cut fresh from `origin/develop`)
- Ship path: one PR to `develop` containing the rewrite + archive + delete + consumer touch-ups + project tracker close-out + project Status flip

## Demand

**User ask (tracker summary):** "Rewrite `docs/plans/tracker.md` (~1154 lines) to a thin project index per `docs/specs/project-structure.md`; archive active-epic narrative into the per-project trackers produced by sprint 04; handle `docs/plans/changelog.md` deprecation."

**Interpretation:** Reduce `docs/plans/tracker.md` from 1154 lines to a short top-level project index (~60 lines max) holding only the open/closed project table + the one runtime-truth-lock sentence the Kotlin guardrail test asserts. All remaining narrative (active campaigns, shipped slices, wave details, quick links, in-tracker changelog, tech-debt bullets) lands in a single dated archive file rather than piecemeal migration across per-project trackers — sprint 04 already created the per-project homes for the ongoing work; the wholesale narrative is historical and does not need redistributing. `docs/plans/changelog.md` is deleted outright (git history preserves it); CLAUDE.md's "Single Changelog" rule has long forbidden parallel product trace logs. Sprint 08 closes the `develop-protocol-migration` project.

## Scope

In scope:

- `docs/plans/tracker.md` — rewrite to the thin index shape defined in Success Exit Criteria
- `docs/plans/changelog.md` — delete via `git rm`
- `docs/archived/plans-tracker-narrative-20260424.md` — new file, verbatim copy of the pre-shrink `docs/plans/tracker.md` (so historical context is preserved under `docs/archived/` rather than vanishing into `git log`)
- Consumer reconciliation:
  - `app-core/src/test/java/com/smartsales/prism/ui/BaseRuntimeTruthLockGuardrailTest.kt` — this test asserts `docs/plans/tracker.md` contains the literal sentence *"Future non-Mono work must not reintroduce separate SIM-vs-full product truth."* The shrunk tracker MUST retain this sentence verbatim. The test file itself stays untouched. If retention is impossible, stop (do not weaken the test).
  - `CLAUDE.md` line 67 (`Read docs/plans/tracker.md for campaign state`) — update wording to reflect that the file is now a project index, not a campaign log.
  - `CLAUDE.md` line 77 (Single Changelog paragraph) — update the "scheduled for cleanup elsewhere" clause now that the cleanup has landed.
  - `CLAUDE.md` line 148 (Key References table) — description column updated; path unchanged.
  - `SmartSales_PRD.md` line 348 — wording update matching CLAUDE.md line 67.
- Repo-wide residue sweep for `docs/plans/changelog.md` references outside this contract, the archive file, and the pre-existing `docs/archive/**` corpus — fix any that remain.
- `docs/projects/develop-protocol-migration/tracker.md` at close: flip row 08 to `done`, append any cross-sprint decision surfaced during execution, flip project Status from `open` to `closed` per the project's own closure clause ("sprint 08 close triggers project closure").

Out of scope:

- Any edit to the per-project trackers themselves (`docs/projects/*/tracker.md`) beyond the develop-protocol-migration tracker's own closeout
- Any edit to `CHANGELOG.md` / `CHANGELOG.html` (the product changelog — untouched)
- Any code change beyond the consumer reconciliation explicitly listed above
- `docs/projects/dashboard/index.html` — generated artifact, regenerated separately
- Spec / SOP / cerb doc rewrites, even if they reference tracker content
- Archival content editing — the archive mirrors the pre-shrink file verbatim; do not clean it up, normalize it, or strip shipped sections
- Historical archives already under `docs/archive/**` — leave alone
- Creating new per-project folders for narrative threads in the pre-shrink tracker that lack an existing project home (Dynamic Island, Crucible, SIM Standalone Prototype, Doc Control Plane, Connectivity Debug APK). The archive is their home.

## References

Operator reads these, not the full repo:

1. `docs/specs/sprint-contract.md`
2. `docs/specs/project-structure.md` — "Relationship to the Top-Level Project Index" and "Project Tracker" sections
3. `docs/projects/develop-protocol-migration/tracker.md`
4. `docs/plans/tracker.md` — pre-shrink content (1154 lines); read once, copy verbatim to archive, then rewrite in place
5. `docs/plans/changelog.md` — deprecated content (226 lines); read to confirm no user-facing deltas absent from `CHANGELOG.md`, then delete
6. `CLAUDE.md` — Single Changelog rule, Doc-Reading Protocol, Key References table
7. `app-core/src/test/java/com/smartsales/prism/ui/BaseRuntimeTruthLockGuardrailTest.kt` — lines 29–32 pin the sentence the shrink must preserve

## Success Exit Criteria

Literally checkable:

- `wc -l docs/plans/tracker.md` returns a number ≤ 60
- `grep -c '^## ' docs/plans/tracker.md` returns exactly 3 (Open Projects / Closed Projects / Operational Law — or equivalent section titles; exact number gates against narrative creep)
- `grep -q "Future non-Mono work must not reintroduce separate SIM-vs-full product truth" docs/plans/tracker.md` returns 0 (the guardrail sentence is preserved verbatim)
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.BaseRuntimeTruthLockGuardrailTest` exits 0
- Every currently-open per-project tracker is linked from the shrunk tracker: `for p in connectivity-bug-triage develop-protocol-migration firmware-protocol-intake god-file-cleanup harmony-native harmony-ui-translation ui-campaign; do grep -q "docs/projects/$p/tracker.md" docs/plans/tracker.md || echo "MISSING: $p"; done` returns empty
- `test ! -f docs/plans/changelog.md`
- `test -f docs/archived/plans-tracker-narrative-20260424.md`
- Archive content equals pre-shrink tracker content: `diff <(git show HEAD:docs/plans/tracker.md) docs/archived/plans-tracker-narrative-20260424.md` returns empty — the archive captures the pre-sprint-08 version exactly
- `wc -l docs/archived/plans-tracker-narrative-20260424.md` returns 1154 (matches pre-shrink `docs/plans/tracker.md` line count)
- Residue grep: `rg -n "docs/plans/changelog\.md" /home/cslh-frank/main_app --glob '!docs/archive/**' --glob '!docs/archived/plans-tracker-narrative-20260424.md' --glob '!docs/projects/develop-protocol-migration/sprints/08-main-tracker-shrink.md'` returns 0 hits
- `docs/projects/develop-protocol-migration/tracker.md` sprint 08 row status is `done` with a one-line Closeout summary, and project Status line reads `closed` with the sprint 08 close date
- One merged PR to `develop` covers all the above in a single commit (or squashable series)

## Stop Exit Criteria

Halt and surface; do not grind past:

- `BaseRuntimeTruthLockGuardrailTest` fails after the shrink — the sentence was dropped or altered. Stop rather than weakening the test; re-insert the sentence in the shrunk tracker.
- Residue sweep surfaces a non-docs consumer of `docs/plans/tracker.md` or `docs/plans/changelog.md` not named in Scope (build script, harness hook, additional Kotlin test, `.codex/skills/**` runtime parser) — stop; sprint 08 did not plan for it
- An open project exists under `docs/projects/` but its tracker file is missing, empty, or shaped differently than the sprint-04 output expected — stop; the index cannot link to a broken target
- Any cross-reference update to `CLAUDE.md`, `SmartSales_PRD.md`, or sibling docs would require changing *behavioral* wording, not just path/description wording — stop; out-of-scope editorial call
- `git rm docs/plans/changelog.md` fails or would lose uncommitted content — stop
- Iteration bound hit without all Success Exit Criteria green — stop

## Iteration Bound

- Max 3 iterations OR 90 minutes wall-clock, whichever hits first
- One iteration = archive creation + delete + rewrite + consumer touch-ups + residue sweep + guardrail test run + project tracker closeout + evidence capture
- Hitting the bound without green is a stop, not a force-close

## Required Evidence Format

At close, operator appends to Closeout:

1. **Preconditions** (once):
   - `wc -l docs/plans/tracker.md docs/plans/changelog.md` — baseline line counts (expected 1154 + 226)
   - `find docs/projects -maxdepth 2 -name 'tracker.md' | sort` — the open-project inventory the shrunk index must cover
2. **Archive creation**:
   - `cp docs/plans/tracker.md docs/archived/plans-tracker-narrative-20260424.md` (or equivalent `git mv`-like operation; verify the archive path did not previously exist)
   - `wc -l docs/archived/plans-tracker-narrative-20260424.md` (expected 1154)
   - `diff docs/plans/tracker.md docs/archived/plans-tracker-narrative-20260424.md` before the shrink rewrite — expected empty (content identical pre-rewrite)
3. **Shrink**:
   - `wc -l docs/plans/tracker.md` post-rewrite
   - `grep -c '^## ' docs/plans/tracker.md` post-rewrite (expected 3)
   - `grep -n "Future non-Mono work must not reintroduce separate SIM-vs-full product truth" docs/plans/tracker.md` (expected one hit)
4. **Delete**:
   - `git log --oneline -1 -- docs/plans/changelog.md` confirming history tip before deletion
   - `git rm docs/plans/changelog.md` output
5. **Consumer touch-ups**:
   - `git diff CLAUDE.md SmartSales_PRD.md` showing only the wording updates named in Scope
   - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.BaseRuntimeTruthLockGuardrailTest` stdout with exit code 0
6. **Residue sweep**:
   - `rg -n "docs/plans/changelog\.md" --glob '!docs/archive/**' --glob '!docs/archived/plans-tracker-narrative-20260424.md' --glob '!docs/projects/develop-protocol-migration/sprints/08-main-tracker-shrink.md'` (expected empty)
   - `rg -n "docs/plans/tracker\.md#[a-z0-9-]+" --glob '!docs/archive/**' --glob '!docs/archived/plans-tracker-narrative-20260424.md' --glob '!docs/projects/develop-protocol-migration/sprints/08-main-tracker-shrink.md'` — any remaining anchor-link references to sections that no longer exist in the shrunk file (expected empty; if not, fix the references or surface as a stop)
7. **Project tracker closeout**:
   - `git diff docs/projects/develop-protocol-migration/tracker.md` showing the sprint 08 row flip + Status flip to `closed`
   - The merged PR URL

Agent narration without these artifacts is not acceptable evidence.

## Iteration Ledger

- **Iteration 1 (2026-04-24, operator Claude, authoring):** authored sprint 08 as the develop-protocol-migration closer. Scope pinned at tracker rewrite + changelog delete + single archive file + narrow consumer reconciliation. Non-docs consumer `BaseRuntimeTruthLockGuardrailTest` identified pre-authoring; contract preserves the asserted sentence rather than touching the test. Narrative threads without existing per-project homes (Dynamic Island, Crucible, SIM Standalone, Doc Control Plane, Connectivity Debug APK) remain in the archive rather than spawning new project folders — that is an explicit out-of-scope decision, not an oversight. Next action: operator executes.

## Closeout

(operator fills at exit — status, one-line tracker summary, the Required Evidence artifacts, optional lesson proposals, optional CHANGELOG line)
