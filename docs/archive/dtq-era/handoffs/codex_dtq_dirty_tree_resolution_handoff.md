# Handoff: DTQ-06 Dirty Tree Resolution Governance Blocker

> **Lane ID**: `DTQ-06`
> **Scope**: Governance-side resolution of the mixed dirty tree currently sitting in `/home/cslh-frank/main_app` on `master`, including lane assignment reconciliation, tracker/interface-map sync, and safe routing rules before any lane shipping resumes.

## Scope

This handoff does not own feature implementation. It owns the governance blocker that is preventing safe lane shipping:

- mixed tracked feature edits are still dirty in `/home/cslh-frank/main_app` on `master`
- active lane worktrees do not currently contain those edits
- the claimed file ownership from prior handoff notes does not fully match the live tracker board
- the DTQ-06 worktree itself has unrelated dirty governance/skill residue that should not be mixed into feature routing

This handoff exists to let the next operator resolve the blocker without inventing ownership, committing feature work on `master`, or contaminating unrelated lanes.

## Owned Paths

- `docs/plans/dirty-tree-quarantine.md`
- `docs/plans/tracker.md`
- `docs/cerb/interface-map.md`
- `handoffs/codex_dtq_dirty_tree_resolution_handoff.md`
- any other governance-only reconciliation notes created specifically for this blocker

This handoff does **not** grant DTQ-06 ownership of feature files currently dirty on `master`.

## Current Repo State / Implementation Truth

As of 2026-04-15, the live repo evidence is:

- `/home/cslh-frank/main_app` is on `master` and contains a mixed dirty tree with onboarding, connectivity, shell/audio, docs, manifests, tests, untracked tooling residue, and new connectivity-registry files.
- DTQ-01, DTQ-03, and DTQ-04 worktrees are not carrying the corresponding handoff edits yet.
- DTQ-06 has its own unrelated dirty changes in governance/skill files and is not a clean landing zone for the feature edits currently dirty on `master`.
- The repo docs for DTQ governance historically referenced `ops/lane-registry.json`, but that file is not present in this workspace. The live fallback source of truth for current routing is therefore `docs/plans/dirty-tree-quarantine.md` plus the actual file inventory in `git status --short`.
- The repo rule still stands: no direct feature commit to `master`; finished work must land through lane worktrees and then merge back.

## What Is Finished

- The blocker is identified clearly: this is a lane-routing and governance-truth problem, not a normal feature-ship problem.
- The mixed dirty inventory has been inspected from the live workspace rather than inferred from memory.
- The governance law and handoff contract were re-read before issuing any next-step guidance.
- This handoff replaces an earlier scratch note that included unsafe instructions such as a direct `master` commit and destructive cleanup steps.

## What Is Still Open

- Reconcile each dirty tracked feature file in `master` to exactly one lane owner before moving anything.
- Decide whether disputed files stay with their current tracker owner or require an explicit tracker/interface-map update first.
- Split the dirty tracked files from `master` into the correct lane worktrees.
- Keep the unrelated DTQ-06 dirt isolated unless a deliberate governance-cleanup pass reopens that exact scope.
- Verify each receiving lane with its own bounded diff before any staging, commit, or merge.

## Doc-Code Alignment

- **Owning source-of-truth docs**:
  - `docs/specs/platform-governance.md`
  - `docs/plans/dirty-tree-quarantine.md`
  - `docs/plans/tracker.md`
  - `docs/cerb/interface-map.md`
  - `handoffs/README.md`
- **Current alignment state**: `Both pending`
- **Docs still needing sync or final confirmation before `Accepted`**:
  - `docs/plans/dirty-tree-quarantine.md` must reflect the final lane assignment outcome for disputed files
  - `docs/plans/tracker.md` must remain consistent with the active governance campaign state after routing is stabilized
  - `docs/cerb/interface-map.md` must only be updated if the final routing changes cross-module ownership, not just temporary file placement
- **Rule**: do not claim the blocker is resolved until the governance docs match the final routing decision and no feature work remains implicitly owned by `master`

## Required Evidence / Verification

- `git status --short` from `/home/cslh-frank/main_app` before and after routing
- `git worktree list` to confirm the target lane worktrees exist and remain distinct
- per-lane dirty-scope checks after files are copied into their target worktrees
- focused compile/test evidence inside each receiving lane before shipping resumes

This governance handoff does not itself prove runtime behavior. Runtime and device evidence remain owned by the relevant feature lanes.

## Safe Next Actions

1. Use `/home/cslh-frank/main_app` only as a quarantine source, not as a commit target.
2. Freeze new feature edits in `master` until the current dirty tracked files are routed.
3. Group the tracked dirty files by current DTQ board ownership first:
   - DTQ-01 onboarding files
   - DTQ-03 connectivity/OEM files
   - DTQ-04 runtime-shell/SIM chrome files
   - DTQ-06 governance docs only
4. For disputed files, resolve ownership in the tracker/interface-map first if the current board does not already make the owner clear.
5. Copy files from `master` into the chosen lane worktree at the same relative path and verify the copied content matches before treating the lane as ready.
6. Run the receiving lane’s scope check and focused verification there.
7. Only after all tracked feature files are safely routed should `master` be cleaned of those tracked edits.

## Do Not Touch / Collision Notes

- Do not commit feature work directly on `master`.
- Do not use `git stash` as the routing mechanism for this blocker.
- Do not treat a clean worktree as proof that it already owns the edits currently dirty on `master`.
- Do not absorb unrelated DTQ-06 dirty governance/skill files into this blocker-resolution pass.
- Do not rewrite tracker ownership just to make the current dirty tree more convenient; ownership changes need an explicit governance reason.
- Do not clean `master` until every tracked feature file has a confirmed lane destination.

## Current Disputed / High-Risk Files

These paths need explicit care because recent discussion showed ownership ambiguity or cross-lane tension:

- `app-core/src/main/java/com/smartsales/prism/data/pairing/RealPairingService.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt`
- `app-core/src/test/java/com/smartsales/prism/data/pairing/RealPairingServiceTest.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryStoreSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/domain/audio/AudioRepository.kt`
- `app-core/src/main/AndroidManifest.xml`
- `app/src/main/AndroidManifest.xml`
- `docs/cerb-ui/**`

If any of these move against the current tracker expectation, update the governance docs first in the same session so the next operator does not have to rediscover the decision.
