# UI Tracker

> Purpose: Track UI design, prototype, approval, transplant, and visual QA work separately from the main feature tracker.
>
> Scope: Screens, shared components, design-system updates, prototype reviews, and UI drift follow-ups.
>
> Status: Active
>
> Last Updated: 2026-03-22

---

## Why This Tracker Exists

`docs/plans/tracker.md` remains the main product and feature execution tracker.

This file exists because UI work has its own lifecycle:

- brief
- prototype
- review
- approval
- transplant
- visual QA

That lifecycle is often different from backend or feature-wave delivery.

Rule:

- use `docs/plans/tracker.md` for product and implementation waves
- use this file for UI-specific execution and visual drift management

---

## Status Model

| Status | Meaning |
|--------|---------|
| Briefing | UI brief or direction framing is in progress |
| Prototyping | Web prototype is being explored |
| Review Pending | Waiting for screenshot review or human feedback |
| Approved | Prototype direction is approved |
| Transplanting | Approved design is being implemented in production UI |
| Visual QA | Prototype-vs-production fidelity check is in progress |
| Shipped | UI slice is accepted and documented |
| Drift | Current production UI is known to diverge from approved direction or guide rules |

---

## Operating Rules

1. Major UI work should appear here even if the backend work is tracked elsewhere.
2. Prototype approval should be visible here before major production transplant begins.
3. Shared visual-pattern changes should note whether the Visual Identity Guide or UI Element Registry needs sync.
4. Current production UI may be inspected for logic understanding, but it is not the visual source of truth unless explicitly marked as an approved visual reference.

---

## Active UI Work

| Area | Scope | Status | Notes |
|------|-------|--------|-------|
| UI Workflow Foundation | Dedicated UI tracker, SOP clarification, aesthetic-source rule | Shipped | Initial operating model synced on 2026-03-22 |
| Dynamic Island Header | Cerb shard, header-center transplant, scheduler-only one-line island | Shipped | Shared top-header island delivered for the standard shell; future non-scheduler lanes deferred |

---

## Entry Template

Use this shape for new UI work:

| Area | Scope | Status | Prototype | Approval | Transplant | Notes |
|------|-------|--------|-----------|----------|------------|-------|
| Example: Home Screen Refresh | Hero, top bar, input dock | Prototyping | v2 pending | No | No | Reuse existing chat/input registry rules |

---

## Related Documents

- `docs/sops/ui-building.md`
- `docs/sops/ui-dev-mode.md`
- `docs/specs/prism-ui-ux-contract.md`
- `docs/specs/style-guide.md`
- `docs/specs/ui_element_registry.md`
