# Tracker Governance SOP

> **Purpose**: Define the repo-wide tracker taxonomy, ownership rules, routing logic, and staged Harmony tracking model.
> **Status**: Active governance SOP
> **Date**: 2026-04-11
> **Primary Laws**:
> - `docs/plans/tracker.md`
> - `docs/specs/platform-governance.md`
> **Related Docs**:
> - `docs/plans/harmony-tracker.md`
> - `docs/plans/harmony-ui-translation-tracker.md`
> - `docs/plans/god-tracker.md`
> - `docs/plans/ui-tracker.md`
> - `docs/plans/bug-tracker.md`
> - `docs/plans/PU-tracker.md`
> - `docs/plans/dirty-tree-quarantine.md`
> - `docs/platforms/harmony/tingwu-container.md`
> - `docs/platforms/harmony/ui-verification.md`
> - `docs/sops/harmony-operator-runbook.md`

---

## 1. Core Rule

Trackers are split by the **question being answered**, not by folder layout.

Use one primary standing tracker per active task.

Rules:

- `docs/plans/tracker.md` is the master ledger and campaign index only.
- Standing trackers own durable operational memory for one concern.
- Execution briefs own one bounded implementation slice and must link back to the standing tracker.
- A task may link one secondary evidence artifact, but it must not create duplicate ownership across standing trackers.

---

## 2. Standing Tracker Taxonomy

| Tracker | Owns | Does Not Own |
|---|---|---|
| `docs/plans/tracker.md` | Portfolio status, campaign index, branch/governance summary, cross-platform program posture | Feature-local implementation details, bug evidence logs, UI slice mechanics, dirty-lane operations |
| `docs/plans/god-tracker.md` | Structure cleanup, anti-god-file waves, trunk-shape memory | Visual polish status, generic UI scope, bug triage, branch governance |
| `docs/plans/ui-tracker.md` | UI campaign state, visual QA posture, active UI slice progress | Structural cleanup ownership, backend/dataflow proof, branch governance |
| `docs/plans/bug-tracker.md` | Bug evidence, repro history, fix status for its owning bug domain | Generic project backlog or feature progress |
| `docs/plans/PU-tracker.md` | Isolated validation universes and acceptance tasklists | Normal feature implementation progress or bug triage |
| `docs/plans/dirty-tree-quarantine.md` | Dirty-lane ownership, write-scope quarantine, alignment gating before promotion | Normal shipped feature history |
| `docs/plans/harmony-tracker.md` | Harmony program summary, bounded-lane capability limits, backend/dataflow evidence posture, restore state across active Harmony roots | Page-by-page ArkUI execution detail, shared product semantics, Android feature truth, generic branch governance outside the Harmony lane |
| `docs/plans/harmony-ui-translation-tracker.md` | Page-by-page Harmony ArkUI rewrite progress, page pass/fail state, mock-vs-docked readiness, on-device UI evidence | Backend/dataflow proof, shared product-truth changes, branch governance outside the Harmony UI lane |

---

## 3. Entry Contract

All new or reopened standing-tracker entries must carry this minimum shape:

- `ID`
- `Title`
- `Work Class`
- `Evidence Class`
- `Platform Lane`
- `Owner`
- `Status`
- `Source of Truth`
- `Required Evidence`
- `Does Not Own`
- `Last Updated`
- `Notes / Drift`

Harmony program-summary entries must also carry:

- `Capability Class` (`translation`, `curated-container`, `divergence`)
- `Supported Set`
- `Disabled Set`
- `User-visible Limitation`
- `UI Translation / Native Rewrite`
- `Backend / Dataflow Evidence`

Harmony UI page-lane entries must also carry:

- `Page ID`
- `Package Target`
- `Mock Driver Status`
- `On-Device Pass Status`
- `Docking Status`
- `Hidden / Internal Gate Status`

---

## 4. Routing Rules

Route work by ownership question:

- shared product-rule or journey change -> shared docs plus `docs/plans/tracker.md`
- Android UI polish or transplant slice -> `docs/plans/ui-tracker.md`
- structure refactor or anti-god-file cleanup -> `docs/plans/god-tracker.md`
- active bug repro and fix evidence -> `docs/plans/bug-tracker.md`
- isolated validation universe or acceptance matrix -> `docs/plans/PU-tracker.md`
- dirty-worktree split, lane collision, or promotion hygiene -> `docs/plans/dirty-tree-quarantine.md`
- Harmony backend/dataflow slice or Harmony program-summary update -> `docs/plans/harmony-tracker.md`
- Harmony ArkUI page-native rewrite, page pass, or device UI verification -> `docs/plans/harmony-ui-translation-tracker.md`
- branch/revert/restore posture -> `docs/plans/tracker.md` plus the owning governance or platform tracker

Execution brief rule:

- use a brief when one approved slice needs step-by-step implementation or validation detail
- do not create a new standing tracker just because a slice is large
- every brief must link its primary standing tracker

---

## 5. Harmony Divergence Test

Use this test before opening or updating a Harmony tracker entry.

### 5.1 Translation-only change

This stays in the Harmony tracker as translation work when:

- product intent stays the same
- data contract stays the same
- user promise stays the same
- only delivery mechanics, UI runtime, lifecycle wiring, or platform APIs change

Examples:

- ArkUI rewrite of an existing surface
- Harmony permission or lifecycle adaptation
- Harmony CLI, packaging, signing, or log/debug workflow changes

### 5.2 Real divergence

This must be tracked as a platform-capability divergence when:

- Harmony cannot honestly deliver the same capability
- a user-visible promise changes
- the acceptance criteria change because of Harmony limits
- the current Harmony app is a reduced-capability container instead of a parity app

Examples:

- scheduler-related features are hidden or blocked
- onboarding cannot promise the same outcome because scheduler or hardware capability is absent
- reminder reliability or background behavior is materially weaker
- the Tingwu container or internal UI verification package is active and intentionally bounded

---

## 6. Staged Harmony Model

Stage 2 is now the active rule for this repo phase.

### 6.1 Stage 1: Historical single-tracker phase

Stage 1 covered the first bounded Harmony lane while one tracker entry could still hold both native UI translation notes and backend/dataflow evidence without mixing unrelated work.

Historical rule:

- `docs/plans/harmony-tracker.md` acted as the one primary Harmony tracker
- each active Harmony entry separated `UI Translation / Native Rewrite` from `Backend / Dataflow Evidence`
- branch restore posture for the transient Harmony line stayed visible in the master tracker plus the Harmony tracker

Historical entries remain valid as written.

### 6.2 Stage 2: Active split model

Stage 2 is active because Harmony now has two independent active lanes:

- backend/dataflow verification inside the transient `tingwu-container` root
- page-by-page ArkUI-native rewrite and device verification inside the internal `ui-verification` root

Active Stage 2 rule:

- `docs/plans/harmony-tracker.md` stays the Harmony program-summary tracker
- `docs/plans/harmony-ui-translation-tracker.md` owns ArkUI page execution and page-pass evidence
- `docs/plans/harmony-dataflow-tracker.md` remains deferred until backend rewriting expands beyond the current bounded mini-lab and truly needs its own standing tracker
- both trackers must keep supported and disabled capability honest and must not imply public parity before the platform can actually deliver it

Human operator rule:

- `docs/sops/harmony-operator-runbook.md` is the human-facing workflow for running this model in practice
- this SOP owns tracker law; the runbook owns how the operator classifies slices, briefs agents, reviews evidence, and interprets Cerb interfaces for Harmony work

---

## 7. Migration Rule

Do not backfill the entire tracker estate in one cleanup pass.

Rules:

- historical entries remain valid as written
- all new entries must follow this SOP
- any reopened legacy entry should be normalized during that edit when practical
- backfill old entries only if they are still operationally active

This keeps the tracker system converging without turning governance work into a broad rewrite project.

---

## 8. Acceptance Rule

The tracker system is aligned only when:

- every active task maps to one primary standing tracker
- `docs/plans/tracker.md` reads as an index, not a dump of local operational detail
- execution briefs do not replace standing trackers
- Harmony unsupported capability is explicit, not implied
- Harmony page-pass evidence lives in the Harmony UI tracker rather than being buried in the program-summary tracker
- branch restore state for active transient lanes is visible without creating tracker sprawl
