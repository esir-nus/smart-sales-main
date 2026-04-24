Archived on 2026-04-23. Active governance moved to `docs/specs/ship-time-checks.md`.

# Declaration-First Shipping Contract

## 0. Philosophy: Friction Upfront, Not Downstream

Checks and confirmations belong at the **earliest point they can prevent a mess**, not at the latest point they can detect one.

- **Declaration is upfront** — the operator states lane and scope *before* work starts, not at ship time, because ambiguity is cheapest to resolve before code exists.
- **Conflict detection is upfront** — overlapping scope is refused at task start, not discovered mid-ship, because untangling two half-written tasks is the expensive failure.
- **Coherence is upfront** — lane/scope contradictions are caught before the first edit, not after a dirty tree has accumulated.
- **Ship-time checks are narrow** — by the time `/ship` runs, the declaration has already done most of the safety work. Ship only enforces what couldn't be checked earlier (secrets, reverse-deps, boundary violations in final state).

Corollary: **non-blockers stay non-blockers**. Branch name mismatch, unrelated dirt, cosmetic inconsistencies — these are reported, not gated, because blocking on them would push friction back downstream where it causes the exact problem this contract avoids. Every gate must justify itself as *preventing a future mess the operator cannot cheaply undo*. If it doesn't meet that bar, it's a report, not a block.

## 1. Per-Command Declaration

Every task/build/ship request must include:

- **Lane**: `android`, `harmony`, or `docs` (tag; derived-checkable against Ship Scope)
- **Ship Scope**: explicit file list or module slice being touched
- **Out-of-Scope Files**: optional, files the operator is deliberately *not* shipping
- **Verification Scope**: tests/build/device evidence for the declared lane

Declarations are never sticky across commands.

Lane semantics:

- `android` and `harmony` are **platform delivery lanes** — they carry code that produces a built artifact.
- `docs` is a **shared infrastructure lane** — documentation and repo-root markdown always land on `develop` regardless of which platform they describe. This keeps doc edits from being blocked by in-flight platform work.

## 2. Pre-Flight Scope Conflict Check (gate at task start)

Before starting any declared task, the agent computes the union of:

- (a) currently dirty files in the worktree
- (b) Ship Scopes of tasks listed in the legacy in-flight lane registry

Rules:

- **File-level intersection** with that union → **refuse to start**. List the conflicting task(s) and recommend ship-or-abandon-first.
- **Same-directory intersection, no file overlap** → warn, ask operator to confirm.
- **No intersection** → proceed; append a new task entry to the legacy in-flight lane registry.

Escape hatch: `--force-parallel "<reason>"` bypasses the gate and logs the reason to the trace.

## 3. Active Lane Registry

The legacy in-flight lane registry holds the task list. Each entry: date, lane, ship scope (file/module list), short title.

Lifecycle:

- Appended on task start (after pre-flight passes)
- Removed on successful `/ship`
- Removed manually via `/abandon <task-title>` when work is dropped

The agent refuses to start a task if the registry write would fail (keeps state honest).

## 4. Ship-Time Review Scope

`/ship` reviews only files in the declared Ship Scope for:

- secrets
- broken imports / missing required files
- boundary violations (e.g., `domain/` importing `android.*`)
- lane-scope coherence (no `platforms/harmony/` files in an `android` lane, etc.)
- **reverse-dependency check**: grep dirty out-of-scope files for imports of shipped files; flag as blocker if found

Blockers (halt ship):

- any of the above inside declared scope
- reverse-dependency hits from out-of-scope dirt

Non-blockers (report only):

- branch name mismatch with declared lane
- unrelated dirty files outside scope and with no reverse-dependency on shipped files

## 5. Lane-Scope Coherence (concrete)

- `lane: android` → no files under `platforms/harmony/`, no files under `docs/` or repo-root markdown (docs go via `lane: docs`)
- `lane: harmony` → only files under `platforms/harmony/` or shared contract paths declared in `docs/cerb/interface-map.md`; no `docs/` or repo-root markdown
- `lane: docs` → only files under `docs/`, `CLAUDE.md`, `AGENTS.md`, `SmartSales_PRD.md`, `CHANGELOG.md`, and other repo-root markdown; no code; always ships to `develop`
- all declared files must exist
- Ship Scope must be non-empty

Incoherence → blocker.

If a platform task needs accompanying doc updates, ship them as two declarations: one under the platform lane, one under `lane: docs`. This is deliberate — docs are shared infrastructure and should not be coupled to a single platform's delivery cycle.

## 6. Trace Log

`/ship` writes an entry to `docs/plans/changelog.md` automatically on success: date, lane, title, behavior summary, verification summary, branch, ignored-dirt summary, any `--force-parallel` reasons. Not manual — if it's not automated, drop it. `CHANGELOG.md` remains the public product log.

## Test Plan

- start Android task while Harmony task is in registry with no file overlap → proceeds
- start Android task whose scope overlaps an in-flight Android task at file level → refused with conflict report
- start task with same-directory but no file overlap → warn + confirm
- `--force-parallel` with reason bypasses gate and records reason in trace
- ship declared-scope blocker (secret, boundary violation) → halts
- ship with unrelated dirt that does *not* reverse-depend on shipped files → proceeds, reports
- ship with out-of-scope dirt that imports a shipped file → halts with reverse-dep report
- two consecutive commands with different lanes → no sticky carry-over
- `/abandon` removes entry; scope can then be reused
