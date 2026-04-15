# Dirty Tree Quarantine Audit

> **Purpose**: Audit the current mixed dirty worktree into bounded operational lanes before branch promotion or Android beta baseline tagging.
> **Date**: 2026-04-04
> **Primary Tracker**: `docs/plans/dirty-tree-quarantine.md`
> **Primary Law**: `docs/specs/platform-governance.md`

---

## Summary

The current worktree on `focus/20260402-runtime-home-shell-fidelity` is not a single coherent batch.

Repo evidence shows:

- branch divergence vs `master`: `0` behind / `884` ahead
- dirty counts at audit time:
  - code paths: `102`
  - docs paths: `36`
  - prototype paths: `5`
  - handoff paths: `4`
  - other paths: `5`

Conclusion:

- branch promotion is unsafe until the dirty tree is split into bounded lanes
- the existing tracker stack is strong for feature memory, but it does not yet act as a lane/build-state ledger for 4-6 parallel coding agents
- the existing `handoffs/` folder is already a valid project seam and should be formalized instead of replaced
- the quarantine process must also track doc-code alignment explicitly so no lane can be treated as promotion-ready while docs and implementation still drift

---

## Findings

### 1. The dirty tree is operationally mixed, not just large

The current modifications span at least these distinct themes:

- onboarding and quick-start
- scheduler intelligence and reminder delivery
- connectivity and OEM hardening
- runtime shell / SIM chrome
- realtime voice and pipeline support
- cross-cutting docs / governance / guardrails
- prototype and handoff assets

Treating this as one batch would hide lane collisions instead of reducing them.

### 2. Feature trackers exist, but no lane tracker currently owns the mixed worktree

The repo already has mature specialist trackers such as:

- `docs/plans/god-tracker.md`
- `docs/plans/ui-tracker.md`
- `docs/plans/bug-tracker.md`

These trackers are useful but they do not answer the operational question:

- which lane currently carries which dirty files
- whether a lane should stay `Active`, move to `Accepted`, or be `Deferred`
- which handoff artifact is current when an agent pauses

### 3. `handoffs/` is already a live artifact pattern

Current handoff files already cover real slices such as:

- audio drawer compose transplant
- schedule quick start compose handoff
- notification compose handoff
- OEM permission diagnosis

This means the repo does not need a new artifact class; it needs a stricter contract tying handoffs to lane state.

### 4. The main collision risk is cross-feature docs and residual cross-surface runtime files

The highest contamination risks in the audited state are:

- cross-cutting docs being edited by multiple feature lanes without an integration lane
- residual shared runtime files such as `RealUnifiedPipeline`, realtime recognizers, and compiler models quietly absorbing multiple feature concerns
- local artifact residue that has no product ownership at all

### 5. Promotion risk is not only file collision; it is also doc-code drift

The audited state already spans both code and docs, which means branch cleanup is not finished just because files are sorted into lanes.

Operational implication:

- each active lane needs an explicit doc-code alignment state
- no lane should be treated as ready for `Accepted` until its owning docs, implementation, and focused verification agree
- parked residue should be recorded as `Deferred` rather than disappearing into an implicit drop bucket

---

## Recommended Lane Map

| Lane | Recommended Theme | Recommended Tracker Status | Why |
|------|-------------------|----------------------------|-----|
| `DTQ-01` | Onboarding and quick-start | `Active` | Large but coherent feature seam with clear docs/tests/handoff |
| `DTQ-02` | Scheduler intelligence and reminders | `Active` | Shared routing/reminder behavior is coherent and has focused evidence surfaces |
| `DTQ-03` | Connectivity and OEM hardening | `Active` | Transport/OEM work is coherent and already has an evidence-first bug-tracker pattern |
| `DTQ-04` | Runtime shell and SIM chrome | `Active` | Shell/audio/home/history polish is a recognizable UI lane with existing handoff support |
| `DTQ-05` | Voice runtime and pipeline support | `Active` | Residual but still bounded cross-surface runtime seam; monitor carefully for further split |
| `DTQ-06` | Governance, trackers, and repo guardrails | `Active` | Needed immediately to keep the cleanup process legible |
| `DTQ-07` | Cross-feature handoff/workflow assets | `Deferred` | Reusable tool/asset question should be decided after the urgent dirty-tree split |
| `DTQ-99` | Foreign local artifact | `Deferred` | No product ownership signal found in repo evidence; exclude it from the current promotion path |

---

## Acceptance Condition For The Cleanup Phase

The quarantine phase is operationally successful only when:

- every dirty path is assigned to exactly one lane, including any explicit `Deferred` row for parked or excluded residue
- every active lane has one current build state at a time
- every paused/transferred lane has a current handoff file
- no lane moves to `Accepted` while doc-code alignment is still unresolved
- the main tracker can point to one quarantine ledger instead of forcing humans to infer state from raw `git status`

Until then, the repo is still effectively operating as one mixed dirty tree.
