# Project Structure

Projects are the top-level unit of work organization. One project = one stated objective + a bounded set of sprints that accomplish it. Each project owns holistic scope (code + docs + UI + backend together — no artificial lane-splitting at the project boundary).

## Folder Layout

Every project lives at `docs/projects/<slug>/` with this layout:

```
docs/projects/<slug>/
    tracker.md                          # objective, sprint index, decisions, lessons pointer
    sprints/
        01-<sprint-slug>.md             # sprint contract (see sprint-contract.md)
        02-<sprint-slug>.md
        ...
    evidence/                           # optional; created when a sprint attaches artifacts
        01-<sprint-slug>/
            logcat.txt
            screenshot-<scene>.png
            ...
```

The tracker is always present. The `sprints/` folder accumulates one file per sprint. The `evidence/` folder appears only when a sprint emits attached artifacts that don't fit inline in the contract's Closeout.

## Project Tracker (`tracker.md`)

Required fields — short. The tracker is an index, not a log. When it exceeds ~150 lines the project has drifted toward log-style and needs to split or archive.

- **Objective** — one paragraph. What the project accomplishes. Include baseline reference (a git tag or SHA) if relevant.
- **Status** — `open` | `closed`.
- **Sprint index** — a table, one row per sprint: `# | slug | status | summary | link-to-contract`. Status values: `planned` | `authored` | `in-progress` | `done` | `stopped` | `blocked`.
- **Cross-sprint decisions** — brief bullets. Decisions that affect more than one sprint in this project.
- **Lessons pointer** — link to `.agent/rules/lessons-learned.md` entries that originated from or affect this project. Do not duplicate lesson content here.
- **Ongoing-justification** — required only when the project stays open past sprint 6 (see Size Discipline below).

Optional field, when relevant:

- **Inputs pending for later sprints** — backlog of decisions or rules captured mid-project that feed an authored-but-not-yet-started sprint.

## Project Lifecycle

1. **Open** — user proposes the objective; Claude creates `docs/projects/<slug>/tracker.md` with an initial sprint index. First sprint contract is authored into `sprints/01-<slug>.md`.
2. **Sprints execute** — each sprint runs per `docs/specs/sprint-contract.md`. On sprint close, the tracker's sprint row flips to `done` (or `stopped`/`blocked`) and the Closeout one-line summary populates the Summary column.
3. **Close** — user confirms the objective is met. The tracker's Status flips to `closed`. Any user-visible work lines flow into CHANGELOG.md at project close (operator collects from all closed sprints' optional CHANGELOG lines).
4. **Archive** — closed project folders stay in place under `docs/projects/`; they are no longer auto-loaded as active context. The top-level project index (`docs/plans/tracker.md`) lists closed projects separately.

## Size Discipline — the Six-Sprint Guideline

Projects that don't close within ~6 sprints should decompose or justify staying open. When sprint 7 is authored against the same project, the tracker must gain an Ongoing-justification note explaining the continuation. This prevents large domains ("harmony migration", "scheduler cleanup") from becoming permanent open projects that accumulate trackers indefinitely.

The six-sprint number is a guideline, not a hard limit. The forcing function is the declaration: continuation must be explicit, not drift.

## Index + Details Pattern

Propagated across the repo's governance surface — the project tracker obeys the same rule as `.agent/rules/lessons-learned.md`:

- **Index** — short, always-loaded, scannable. The tracker is the project's index.
- **Details** — long, loaded conditionally. Individual sprint contracts are the details. Evidence artifacts are deeper details.

When any governance doc crosses ~200 lines, it is drifting from index-style into log-style. Either split (extract sections into their own files) or archive old content. This pattern is the primary defense against document bloat in this repo.

## Doc-Production Discipline

Agents must not auto-proliferate doc artifacts. Before producing a non-code file, ask whether the information fits in a commit message, an existing doc, or the sprint contract's iteration ledger. Default to NOT creating a new file. A closed sprint with zero new docs is a well-run sprint if its code and commit message carry the knowledge.

This rule applies repo-wide; it is enforced by convention, reinforced in CLAUDE.md + AGENTS.md.

## Naming

- Project slug: lower-kebab-case, short (e.g., `workflow-renovation`, `god-file-cleanup`, `harmony-native`).
- Sprint slug: lower-kebab-case; sprint files are numbered `NN-<slug>.md` zero-padded to two digits.
- Evidence folder: matches the sprint slug (`evidence/NN-<sprint-slug>/`).

## Relationship to the Top-Level Project Index

`docs/plans/tracker.md` is the top-level project index — a ~30-line table of open projects, with a short closed-projects section. Individual project trackers are its details. The top-level tracker contains no narrative, no inline epics, no wave lists; those live inside per-project trackers or archive.

## See Also

- `docs/specs/sprint-contract.md` — the contract schema that every sprint file implements.
- `.agent/rules/lessons-learned.md` — the template for the index+details pattern.
