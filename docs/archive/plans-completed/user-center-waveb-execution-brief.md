# User Center Wave B Execution Brief

## Summary

`Wave B` is the prototype-first, surgical light-theme fidelity wave for the full-app `User Center` overlay.

This wave closes the gap between shipped theme plumbing and approved visual direction. It does not widen into a whole-app recolor and does not include SIM light-theme work.

## Source Stack

- visual source of truth: `prototypes/prism-web-v1/src/components/UserCenter.tsx`
- overlay host seam: `app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt`
- full-app screen seam: `app-core/src/main/java/com/smartsales/prism/ui/settings/UserCenterScreen.kt`
- behavior source of truth: `docs/specs/modules/UserCenter.md`
- workflow source of truth: `docs/projects/ui-campaign/tracker.md`, `docs/sops/ui-building.md`

## Objective

- deliver high-fidelity `LIGHT` mode for the full-app `User Center`
- preserve the shipped `Dark / Light / System` selector behavior
- preserve `AgentShell` overlay legality from both the main profile entry and the history-footer route

## Locked Scope

- full-app only
- shipped IA only: `Preferences / Storage / Security / About / Logout`
- local overlay seam only: `UserCenterScreen`, `ThemeModeDialog`, and `AgentShell` presentation details needed for fidelity

## Allowed Work

- spacing, typography, card treatment, border contrast, glass tuning, scrim/backdrop tuning, and header/profile polish
- local row styling and section rhythm refinement
- local `AgentShell` overlay adjustments needed for legal in-shell presentation
- prototype refresh needed to represent the shipped IA and approved light-theme direction

## Approved Follow-Up Polish

- strengthen the real `AgentShell` overlay seam so background history reads as muted context only
- add a stronger protected top inset inside the sheet so the hero-first composition sits below the Android status area without a replacement header row
- reduce foggy translucency in favor of a calmer, more solid light slab with cleaner border/shadow separation
- tighten hero grouping, section cadence, type scale, row height, divider inset, and icon/value alignment toward the approved `Image #2` rhythm
- remove the visible top-left `X`; dismissal remains system back plus scrim tap
- keep current shipped IA, routing, persistence, and settings behavior intact

## Forbidden Work

- SIM light-theme prototype or transplant
- broader full-app shell recolor
- settings IA expansion
- restoring the older separate `Support` section
- route, schema, or theme-plumbing changes
- redesign outside the full-app `User Center` overlay seam

## Acceptance Targets

- L1 logic validation: `docs/reports/tests/L1-20260326-user-center-waveb-theme-validation.md`
- L3 visual validation: `docs/reports/tests/L3-20260326-user-center-waveb-light-theme-validation.md`

The `L3` report must include:

- `LIGHT`, `DARK`, and `SYSTEM` states
- at least one in-shell capture from the main profile entry
- at least one in-shell capture from the history-footer route
- confirmation that history closes atomically before the overlay owns the screen

## Deferred Work

- SIM light-theme parity
- broader full-app shell light-theme cleanup
- separate `Support`-section restoration or new support/privacy/license IA
- any redesign outside `UserCenterScreen` and its `AgentShell` overlay seam
