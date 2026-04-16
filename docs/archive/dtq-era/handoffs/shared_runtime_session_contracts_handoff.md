# Handoff: DTQ-05 Shared Runtime, Pipeline, and Session Contracts

> **Lane ID**: `DTQ-05`
> **Registry Lane ID**: `DTQ-05`
> **Branch**: `lane/DTQ-05/shared-runtime-session-contracts`
> **Recommended Worktree**: `/home/cslh-frank/lane-worktrees/DTQ-05-shared-runtime-session-contracts`
> **Scope**: Shared runtime/pipeline hosts, parser/session-title cleanup, and session persistence/history contract work for the current dirty tree.

## Scope

This handoff owns the current mixed contract-cleanup bundle that removed parser-driven auto-renaming, tightened SIM session-title ownership, widened session metadata around audio context history, and updated the related pipeline/session test surfaces.

## Owned Paths

- `app-core/src/main/java/com/smartsales/prism/data/real/session/**`
- `app-core/src/main/java/com/smartsales/prism/data/session/**`
- `app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentPipelineCoordinator.kt`
- `app-core/src/test/java/com/smartsales/prism/data/real/**`
- `app-core/src/test/java/com/smartsales/prism/data/session/**`
- `app-core/src/test/java/com/smartsales/prism/ui/AgentViewModelTest.kt`
- `core/pipeline/**`
- `core/test-fakes-platform/**`
- `domain/session/**`
- `docs/cerb/input-parser/spec.md`
- `docs/cerb/session-history/spec.md`
- `docs/cerb/unified-pipeline/**`

## Current Repo State / Implementation Truth

The current dirty tree bundles three related contract changes here:

- parser output no longer owns session auto-renaming
- session-title generation now belongs to the SIM session/chat layer instead of the parser path
- session persistence now records and backfills audio-context history explicitly

This lane is the shared-contract owner for those rules. Shell consumers that display the title or audio-history badge may stay in `DTQ-04`, but the contract and test truth lives here.

## What Is Finished

- The lane is now explicitly registered in `ops/lane-registry.json` instead of leaving the parser/session-title/session-history cleanup split across unowned paths.
- A resumable handoff now exists for the lane.
- The ownership split is explicit: scheduler contract tightening stays in `DTQ-02`, shell presentation stays in `DTQ-04`, and the shared parser/session contract cleanup stays here.

## What Is Still Open

- Keep parser/session-title/session-history docs and code aligned in the same session.
- Preserve the current semantics: no parser-driven auto-rename, no reintroduction of deleted `SessionTitleGenerator` seams, and no silent drift back to pipeline-owned title authority.
- Do not mark the lane `Accepted` until the focused pipeline/session test bundle is rerun from an isolated lane worktree.

## Doc-Code Alignment

- **Owning source-of-truth docs**:
  - `docs/cerb/input-parser/spec.md`
  - `docs/cerb/session-history/spec.md`
  - `docs/cerb/unified-pipeline/spec.md`
  - `docs/plans/tracker.md`
- **Current alignment state**: `Both pending`
- **Docs still needing sync or final confirmation before `Accepted`**:
  - parser/session-history docs now reflect the new session-title ownership split, but the current dirty implementation/test bundle is still in flight and not yet isolated for final verification

## Required Evidence / Verification

- Focused pipeline/session tests for the touched `core/pipeline`, `app-core` session, and `domain/session` branches.
- Negative-path proof that parser-driven auto-rename no longer fires.
- Persistence proof that legacy audio-linked or audio-grounded sessions backfill `hasAudioContextHistory` correctly.

## Safe Next Actions

- Continue only inside the owned paths listed above.
- Keep shell presentation consumers in `DTQ-04` and scheduler contract law in `DTQ-02`.
- Re-run the focused pipeline/session test bundle from a dedicated DTQ-05 worktree before changing lane state.

## Do Not Touch / Collision Notes

- Do not claim runtime shell chrome, dynamic-island presentation, or audio-drawer UI from `DTQ-04`.
- Do not claim the tightened explicit-target reschedule contract from `DTQ-02`.
- Do not move Harmony-native scaffolding or Harmony tracker overlays into this lane.
