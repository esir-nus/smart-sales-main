# UI Tracker

> Purpose: Track UI design, prototype, approval, transplant, and visual QA work separately from the main feature tracker.
>
> Scope: Screens, shared components, design-system updates, prototype reviews, and UI drift follow-ups.
>
> Status: Active
>
> Last Updated: 2026-03-25

---

## Why This Tracker Exists

`docs/plans/tracker.md` remains the main product and feature execution tracker.

This file exists because UI work has its own lifecycle and needs its own campaign memory:

- brief
- prototype
- review
- approval
- transplant
- visual QA

This tracker manages UI campaign state:

- current visual direction
- prototype source work and accepted prototype families
- approval gate
- transplant readiness
- visual QA state
- drift and defer follow-up

It does not manage:

- feature execution law
- implementation wave history
- acceptance history
- feature behavior ownership

Those remain in `docs/plans/tracker.md`, feature-local Class A trackers, `docs/core-flow/**`, and `docs/cerb/**`.

Rule:

- use `docs/plans/tracker.md` for product and implementation waves
- use this file for UI-specific execution and visual drift management

---

## Governance Split

Use these layers together, not interchangeably:

1. **Class A trackers** own feature and campaign delivery
   - implementation waves
   - acceptance progress
   - product execution status

2. **UI SOP stack** owns UI workflow
   - prototype-first exploration
   - screenshot critique
   - approval rhythm
   - production transplant workflow

3. **Cerb / core-flow / UI contract docs** own feature truth
   - behavior
   - states
   - ownership boundaries
   - user-facing invariants

4. **UI Tracker** owns UI campaign state
   - visual source of truth
   - current UI gate
   - approved direction memory
   - drift tracking
   - defer tracking

Rule:

- do not restate full feature behavior here
- do not use this tracker as the main implementation tracker
- do not hide defer lists inside vague notes

---

## Status Model

| Status | Meaning |
|--------|---------|
| Briefing | UI brief or direction framing is in progress |
| Prototyping | Web prototype is being explored |
| Review Pending | Waiting for screenshot review or human feedback |
| Approved | Visual direction is approved, but the production transplant may still be pending |
| Transplanting | Approved design is being implemented in production UI |
| Visual QA | Prototype-vs-production fidelity check is in progress |
| Shipped | UI slice is accepted and documented |
| Drift | Current production UI is known to diverge from approved direction or guide rules |
| Deferred | UI work is intentionally postponed outside the current slice |

Happy-path gate:

`Briefing -> Prototyping -> Review Pending -> Approved -> Transplanting -> Visual QA -> Shipped`

Side statuses:

- `Drift` tracks known divergence that needs follow-up
- `Deferred` tracks consciously postponed UI work outside the active slice

---

## Operating Rules

1. Major UI work should appear here even if backend or domain work is tracked elsewhere.
2. Prototype approval should be visible here before major production transplant begins.
3. Prototype-only work should also be tracked here when it is the current visual source of truth, even if no Compose transplant has started yet.
4. Every active row should state its current gate and visual source of truth.
5. Shared visual-pattern changes should note whether the Visual Identity Guide or UI Element Registry needs sync.
6. Current production UI may be inspected for logic understanding, but it is not the visual source of truth unless explicitly marked as an approved reference.
7. Structural blockers and anti-god-file work belong in `docs/plans/god-tracker.md`; only the UI-facing dependency summary belongs here.

---

## Current Campaign Direction

### Surgical Transplant Rule

The current repo decision is **surgical transplant / piece-by-piece migration**.

Reason:

- small high-fidelity UI slices are now under control
- shared shell chrome can be stabilized without reopening whole features
- large all-at-once UI rewrites create avoidable visual drift and landing risk

Current execution posture:

- keep prototype-only source work visible here when it is driving the next approved UI slice
- keep shared shell chrome unified when the design is shared
- migrate feature-local UI slices independently once seams are stable
- treat approved direction as a gate before production transplant
- keep defer lists explicit so later polish does not silently widen scope

---

## Active UI Work

| Area | Scope | Visual Source | Status | Next Gate | Notes / Defers |
|------|-------|---------------|--------|-----------|-----------------|
| UI Workflow Foundation | Dedicated UI tracker, SOP clarification, and aesthetic-source rule | `docs/sops/ui-dev-mode.md` | Shipped | Maintain as standing law | Approved and synced on 2026-03-22. Defers: future SOP expansion only when new UI workflow drift appears. |
| SIM Shell Prototype Family | Standalone SIM HTML prototype family for empty home hero, active chat shell, system/status/artifact surfaces, and scheduler-drawer exploration | `prototypes/sim-shell-family/sim_prototype_family.html` | Prototyping | Keep the prototype family current as the visual source before each surgical transplant slice | Prototype work itself is active UI scope, not just a hidden precursor to Compose. Current family already anchors the shipped home/chat shell slice and the approved scheduler-drawer direction through focused child prototype files and alignment notes. Deferred: unrelated non-SIM prototype experiments and any whole-app redesign outside the current SIM shell family. |
| SIM Audio Drawer Direction | Prototype-approved SIM audio drawer browse/select presentation transplant within the existing SIM shell and audio/chat contract | `prototypes/sim-shell-family/sim_audio_drawer_shell.html` | Transplanting | Focused Compose transplant plus visual QA against prototype/device screenshots | Prototype now covers browse-mode delete/transcribe states alongside select-mode suppression. Current surgical transplant scope is: darker frosted drawer slab, compact browse/select card rhythm, right-swipe transcribe on collapsed pending cards, left-swipe delete on collapsed pending/transcribed cards, no delete on expanded/transcribing cards, and prototype-faithful shimmer / star / truncated-preview treatment. Chat-select mode remains selection-only with swipe and delete suppressed. Deferred: confirm modals, cancel-transcription semantics, runtime rewiring beyond unlink-on-delete, non-SIM shell redesign, and any follow-up polish beyond the first approved transplant pass. |
| SIM History Drawer Direction | SIM-only history drawer polish inside the shared SIM shell using the approved left-edge dark slab prototype and existing SIM session actions | `prototypes/sim-shell-family/sim_history_drawer_shell.html` | Transplanting | Visual QA against prototype plus device screenshots | Current polish slice tightens the archive view only: one-line title-only rows, no row icons/preview/date/3-dots affordance, long-press action entry for pin/rename/delete, and spec-aligned grouping using `置顶` / `今天` / `最近30天` / `YYYY-MM`. Deferred: live search, swipe-reveal actions, shared smart-app history rewrite, header utility chrome, and any widening beyond the SIM support surface. |
| UI Trunk Structure Cleanup | Refactor-first cleanup of shared UI and ViewModel trunk files that are slowing approved prototype transplants | `docs/plans/god-tracker.md` | Transplanting | Continue active god-file cleanup waves outside this tracker | This is a UI-facing dependency record, not the owning structure tracker. Approved now: trunk cleanup is a prerequisite for faster future transplants. Deferred here: detailed structural wave law stays in `god-tracker.md`. |
| Dynamic Island Header | Cerb shard, header-center transplant, scheduler-only one-line island | `docs/cerb/sim-shell/spec.md` | Shipped | Reopen only if future shared-header drift is confirmed | Shared top-header island is delivered for the standard shell. Deferred: future non-scheduler lanes and broader header experiments. |
| SIM Shell High-Fidelity Migration | Prototype-faithful SIM shell chrome transplant | `prototypes/sim-shell-family/sim_home_hero_shell.html` | Shipped | Reopen only for confirmed drift or a new approved shell slice | Approved and shipped slice keeps empty home and active chat on one shell chrome. Deferred: future shell polish must stay surgical and must not reopen whole-shell redesign by default. |
| SIM Conversation Surface Fidelity | Prototype-faithful SIM outgoing, incoming, system, processing, artifact, and strategy surfaces inside the shared shell | `docs/reports/20260323-sim-prototype-alignment-audit.md` | Shipped | Reopen only if production drift is observed against the accepted audit | Approved and shipped within the shared SIM shell family. Deferred: non-SIM chat path redesign and unrelated conversation-lane widening. |
| SIM Shell Monolith Edge Refinement | Restore elegant fuzzy seams on the shared top and bottom monoliths without changing shell structure | `docs/reports/20260323-sim-prototype-alignment-audit.md` | Shipped | Reopen only for confirmed seam drift | Delivered neutral feathered seams on the shared monolith shell. Deferred: any stronger atmospheric effects or aurora bleed experiments. |
| SIM Scheduler Drawer Direction | Top-anchored dark frosted scheduler slab over the shipped shared SIM shell while preserving current scheduler IA | `prototypes/sim-shell-family/sim_scheduler_drawer_shell.html` | Transplanting | Visual QA against prototype plus device screenshots | First Compose slice is now in code with shared `SchedulerDrawer` SIM presentation seam; execution brief: `docs/plans/sim-scheduler-drawer-first-slice-brief.md`. Current polish scope also includes page-mode coverage so the SIM chat composer is not left visible under the scheduler page, with focused UI coverage guarding that shell contract. Approved now: SIM-only visual seam first, shared shell behavior stays shared, inspiration shelf remains visible, and lightweight note/tips content may preserve user-spoken task detail without widening into smart-runtime intelligence. Deferred: persistent peek behavior, broader scheduler intelligence, deeper smart tips, unrelated shell redesign, and any widening beyond the first surgical Compose slice. |

---

## Entry Template

Use this shape for new UI work:

| Area | Scope | Visual Source | Status | Next Gate | Notes / Defers |
|------|-------|---------------|--------|-----------|-----------------|
| Example: Home Screen Refresh | Hero, top bar, input dock | `prototypes/example/home_shell.html` | Prototyping | Screenshot review | Approved now: hero + dock direction. Deferred now: notification tray and settings chrome. |

---

## Related Documents

- `docs/sops/ui-building.md`
- `docs/sops/ui-dev-mode.md`
- `docs/specs/prism-ui-ux-contract.md`
- `docs/specs/code-structure-contract.md`
- `docs/specs/style-guide.md`
- `docs/specs/ui_element_registry.md`
- `docs/plans/sim-tracker.md`
- `docs/plans/god-tracker.md`
- `docs/plans/god-wave0-execution-brief.md`
- `docs/reports/sim_prototype_family_transplant_notes.md`
