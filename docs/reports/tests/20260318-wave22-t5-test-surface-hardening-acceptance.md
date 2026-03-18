# Wave 22 T5 Acceptance Report: Test Surface Hardening

**Date**: 2026-03-18
**Scope**: Wave 22 closeout for T1-T4 outcomes and the canonical automated runner surface
**Acceptance Mode**: `smart-sales-acceptance`

## Source of Truth

- No dedicated `docs/core-flow/**` document exists for the test runner itself.
- Spec authority used for this acceptance:
  - `docs/cerb/test-infrastructure/spec.md`
  - `docs/cerb/test-infrastructure/interface.md`
  - `docs/cerb-e2e-test/testing-protocol.md`
  - `docs/cerb-e2e-test/tasklist_log.md`

## What Was Checked

### Spec Examiner

- The canonical runner exposes the declared first-class automated slices: `all`, `infra`, `pipeline`, `scheduler`, and `l2`.
- `all` is explicitly defined as the curated repo-default unit-test slice, not an exhaustive aggregate.
- Infra modules now have real assertion-bearing tests instead of `NO-SOURCE`.
- L2 verification uses shared fakes for the repaired repo-owned collaboration seam in `L2CrossOffLifecycleTest`.
- Testing governance now assigns active status ownership to `docs/plans/tracker.md`, operational mirror ownership to `docs/cerb-e2e-test/tasklist_log.md`, and historical-only ownership to `docs/plans/changelog.md`.

### Contract Examiner

- The stale scheduler UI contract path exposed during `all` validation has been repaired by restoring the live Scheduler Drawer contract doc and aligning the UX index to it.
- The test-surface docs, runner help text, tracker, and governance docs now describe the same slice model and closure rules.
- No new undocumented module-ownership edge was introduced; the work remained in docs, runner wiring, and test code already owned by the relevant modules.

### Build Examiner

The following commands were executed in the Wave 22 closeout session and all completed successfully:

```bash
bash scripts/run-tests.sh infra
bash scripts/run-tests.sh l2
bash scripts/run-tests.sh pipeline
bash scripts/run-tests.sh scheduler
bash scripts/run-tests.sh all
```

Observed result:

- `infra`: passed with real tests in `:core:test`, `:core:test-fakes-domain`, and `:core:test-fakes-platform`
- `l2`: passed against `com.smartsales.prism.data.real.L2*`
- `pipeline`: passed
- `scheduler`: passed
- `all`: passed and printed the curated-slice note plus the manual L3 reminder

### Break-It Examiner

- Negative closure case addressed: a changelog line alone can no longer be treated as proof that a testing wave is closed.
- Negative docs/runtime case addressed: `all` previously exposed a dead scheduler UI contract path; the acceptance surface now points to a live contract again.
- Explicit remaining boundary: `all` is not a superset of every possible runner mode, and that limitation is now stated in the runner and docs instead of being hidden.
- Explicit remaining boundary: L3 device validation remains manual; this wave does not automate hostile on-device behavior.

## Verdict

**Accepted.**

Wave 22 closes legitimately: the shared infra modules now assert real behavior, the highest-signal L2 anti-illusion seam is repaired, the canonical runner exercises every declared first-class slice successfully, and the tracker/ledger/changelog governance now has a hard ship gate.

## Residual Risks

- Non-L2 Mockito usage still exists outside the T2 scope and remains separate debt rather than a blocker for Wave 22 acceptance.
- L3 remains manual per protocol; this acceptance does not claim device-level automation.
