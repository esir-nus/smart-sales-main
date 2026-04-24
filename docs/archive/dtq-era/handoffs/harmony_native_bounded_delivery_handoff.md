# Handoff: DTQ-08 Harmony-native Bounded Delivery

> **Lane ID**: `DTQ-08`
> **Registry Lane ID**: `DTQ-08`
> **Branch**: `lane/DTQ-08/harmony-native-bounded-delivery`
> **Recommended Worktree**: `/home/cslh-frank/lane-worktrees/DTQ-08-harmony-native-bounded-delivery`
> **Scope**: Harmony-native app roots plus the Harmony program/verification overlays for the current dirty tree.

## Scope

This handoff owns the current Harmony-native bounded-delivery slice:

- `platforms/harmony/tingwu-container/**`
- `platforms/harmony/ui-verification/**`
- Harmony program-summary, backend-first, signing-ledger, and UI-verification overlay docs

The lane is intentionally `Paused` in the registry. The source paths are now explicitly governed, but the integration tree is still blocked until this work moves into its own Harmony lane worktree.

## Owned Paths

- `platforms/harmony/tingwu-container/**`
- `platforms/harmony/ui-verification/**`
- `docs/projects/harmony-native/tracker.md`
- `docs/projects/harmony-ui-translation/tracker.md`
- `docs/projects/harmony-native/sprints/01-scheduler-backend-phase1.md`
- `docs/platforms/harmony/tingwu-container.md`
- `docs/platforms/harmony/scheduler-backend-first.md`
- `docs/platforms/harmony/test-signing-ledger.md`
- `docs/platforms/harmony/ui-verification.md`

## Current Repo State / Implementation Truth

The current dirty tree already contains real Harmony-native source, generated build residue, and program overlays. The problem is no longer missing ownership; it is that the Harmony feature work is still sitting in the integration tree instead of a dedicated worktree.

This lane stays paused to make that blocked state explicit. Registry ownership does not legalize continued Harmony feature editing in the root tree.

## What Is Finished

- The Harmony-native roots and overlays are now assigned to one explicit lane instead of being left as unowned dirty-tree residue.
- The lane has a real branch/worktree target and a resumable handoff.
- `DTQ-06` now keeps governance/framework ownership while this lane owns Harmony-native program and app-root delivery surfaces.

## What Is Still Open

- Move Harmony-native work into the recommended DTQ-08 worktree before resuming implementation.
- Keep the current reduced-capability posture honest; do not widen the public Harmony surface by implication.
- Do not claim the integration tree is unblocked until the Harmony feature paths are physically removed from root-tree dirty state.

## Doc-Code Alignment

- **Owning source-of-truth docs**:
  - `docs/projects/harmony-native/tracker.md`
  - `docs/platforms/harmony/tingwu-container.md`
  - `docs/platforms/harmony/scheduler-backend-first.md`
  - `docs/platforms/harmony/ui-verification.md`
  - `docs/platforms/harmony/test-signing-ledger.md`
- **Current alignment state**: `Both pending`
- **Docs still needing sync or final confirmation before `Accepted`**:
  - the current source/docs bundle is still in-flight and still mixed with generated Harmony build residue inside the owned roots

## Required Evidence / Verification

- `python3 scripts/lane_guard.py validate-registry`
- `scripts/lane validate dirty`
- Harmony lane-local build/signing/runtime verification only after the work is resumed inside the DTQ-08 worktree

## Safe Next Actions

- Create the dedicated DTQ-08 worktree and attach the lane before any new Harmony feature edits.
- Keep Harmony governance/framework docs in `DTQ-06` and shared scheduler/session semantics in their existing shared-contract lanes.
- Treat `.hvigor` and nested build outputs under the owned Harmony roots as residual debt to be cleaned from inside the Harmony lane, not as a reason to widen `DTQ-06`.

## Do Not Touch / Collision Notes

- Do not move Harmony-native files back into `app-core/**`, `core/**`, `data/**`, or `domain/**`.
- Do not let this lane absorb shared scheduler semantics, shared session-history semantics, or repo governance law.
- Do not mark the lane active again until a real DTQ-08 worktree exists.
