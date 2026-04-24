# Ship-Time Checks

Sprint contracts own startup discipline and iteration control. This file keeps only the checks that still belong at ship time, after the sprint scope is already declared and the close commit is ready.

## 1. Ship-Time Review Scope

The ship-time operator reviews only files in the declared ship scope for:

- secrets
- broken imports or missing required files
- boundary violations (for example `domain/` importing `android.*`)
- lane-scope coherence
- reverse-dependency hits from dirty out-of-scope files that import shipped files

Blockers:

- any issue above inside the declared scope
- reverse-dependency hits from out-of-scope dirt

Non-blockers:

- branch name mismatch with the declared lane
- unrelated dirty files outside scope when they do not reverse-depend on shipped files

## 2. Lane-Scope Coherence

- `lane: android` can ship Android/shared code, but not `platforms/harmony/` content and not docs or repo-root markdown
- `lane: harmony` can ship Harmony-owned files or shared contract paths explicitly allowed by `docs/cerb/interface-map.md`, but not Android-only paths and not docs or repo-root markdown
- `lane: docs` can ship `docs/`, `CLAUDE.md`, `AGENTS.md`, `SmartSales_PRD.md`, `CHANGELOG.md`, and other repo-root markdown, but not code
- all declared files must exist
- ship scope must be non-empty

Incoherence is a ship blocker.

If a platform task needs matching doc updates, ship them as separate declarations so shared governance stays on `develop`.
