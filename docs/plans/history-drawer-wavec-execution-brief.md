# History Drawer Wave C Execution Brief

## Summary

`Wave C` is the prototype-first, surgical light-theme fidelity wave for the full-app `HistoryDrawer` inside the existing `AgentShell`.

This wave closes the gap between the shipped full-app theme plumbing and the older drawer visuals. It does not widen into a whole-app recolor, a session-history behavior rewrite, or any SIM work.

## Source Stack

- visual source of truth: `prototypes/prism-web-v1/src/components/HistoryDrawer.tsx`
- production seam: `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt`
- host seam: `app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt`
- behavior source of truth: `docs/specs/modules/HistoryDrawer.md`
- workflow source of truth: `docs/plans/ui-tracker.md`, `docs/sops/ui-building.md`

## Objective

- deliver high-fidelity `LIGHT` mode for the full-app `HistoryDrawer`
- preserve the current full-app drawer ownership and session-routing behavior
- preserve `AgentShell` legality for drawer scrim, connectivity handoff, and `User Center` handoff

## Locked Scope

- full-app only
- local drawer seam only: `HistoryDrawer`, local action surfaces, and any `AgentShell` presentation adjustments needed for in-shell legality
- shipped behavior only: hamburger entry, scrim close, session close-and-switch, visible overflow menu actions, connectivity capsule handoff, and footer handoff into `User Center`

## Allowed Work

- drawer slab, border, shadow, and scrim tuning
- floating device capsule styling
- group-card rhythm, spacing, typography, and row contrast refinement
- footer dock chrome and avatar/profile treatment
- local overflow-menu and rename-dialog polish needed for light-theme legality
- local `AgentShell` visual adjustments needed for approved in-shell presentation
- prototype refresh needed to represent the shipped full-app drawer contract

## Forbidden Work

- SIM history/theme prototype or transplant
- broader full-app shell recolor
- session-history IA rewrite
- header `+` action restoration
- swipe-close restoration for the full-app drawer
- route, persistence, or ViewModel ownership changes beyond local visual polish
- redesign outside the full-app `HistoryDrawer` seam

## Acceptance Targets

- L1 logic validation: `docs/reports/tests/L1-20260326-history-drawer-wavec-theme-validation.md`
- L3 visual validation: `docs/reports/tests/L3-20260326-history-drawer-wavec-light-theme-validation.md`

The `L3` report must include:

- `LIGHT`, `DARK`, and `SYSTEM` states
- at least one in-shell capture from the hamburger entry path
- one capture with an expanded group state
- one capture with the visible overflow menu open
- one capture showing header connectivity context
- one capture showing footer dock plus `User Center` handoff context

## Deferred Work

- SIM history/theme parity
- broader full-app light-theme cleanup outside the drawer seam
- any attempt to restore old full-app swipe-close or header `+` behavior
- any redesign outside `HistoryDrawer` and its `AgentShell` ownership seam
