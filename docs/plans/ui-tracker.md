# UI Tracker

> Purpose: Track UI design, prototype, approval, transplant, and visual QA work separately from the main feature tracker.
>
> Scope: Screens, shared components, design-system updates, prototype reviews, and UI drift follow-ups.
>
> Status: Active
>
> Last Updated: 2026-03-26

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
| SIM History Drawer Direction | SIM-only history drawer polish inside the shared SIM shell using the approved left-edge dark slab prototype and existing SIM session actions | `prototypes/sim-shell-family/sim_history_drawer_shell.html` | Transplanting | Visual QA against prototype plus device screenshots | Current polish slice keeps the archive view narrow while tightening prototype fidelity: one-line title-only rows, no row icons/preview/date/3-dots affordance, long-press action entry for pin/rename/delete, stronger history-open shell scrim so the drawer owns the overlay, single-title header spacing that clears the status area, subtle filled current-session slab, spec-aligned grouping using `置顶` / `今天` / `最近30天` / `YYYY-MM`, and a fixed bottom user dock with avatar/name + membership text + settings affordance so the history route still exposes SIM profile/metadata controls. Deferred: live search, swipe-reveal actions, top header utility chrome such as search/device capsule, shared smart-app history rewrite, and any widening beyond the SIM support surface. |
| SIM User Center Direction | SIM-only user-center chrome transplant inside the existing shell using a right-edge dark frosted drawer while keeping current profile/settings behaviors and edit flow | `prototypes/sim-shell-family/sim_user_center_shell.html` | Transplanting | Visual QA against prototype plus device screenshots | Current slice is chrome-first: right-edge drawer slab, centered profile hero, compact section-card rhythm, profile metadata chips from live user data, scrim-backed overlay legality, and existing full-screen edit subflow preserved behind the SIM drawer entry. Deferred: broader IA cleanup, new support/privacy/license flows, legacy smart-shell adoption, and any trigger redesign beyond the current SIM settings entry path. |
| Full-App User Center Theme Fidelity | Full-app `User Center` light-theme fidelity transplant inside the real `AgentShell` overlay seam while keeping current theme behavior and current shipped IA | `prototypes/prism-web-v1/src/components/UserCenter.tsx` | Visual QA | Shipped only after L1 + L3 evidence is present | Historical gate label: User Center Wave B. Execution brief: `docs/plans/user-center-waveb-execution-brief.md`. Current slice refreshes the full-app prototype and Compose overlay for the shipped IA `Preferences / Storage / Security / About / Logout`, with `帮助中心` remaining under `About` and the older separate `Support` section retired from the full-app path. Approved now: full-app-only light-theme fidelity for the scenic sheet header, profile hero, section-card rhythm, theme selector dialog, and `AgentShell` overlay legality from both the main profile entry and the history-footer route. Deferred: SIM light-theme parity, broader full-app shell recolor, old full-app `Support`-section restoration, and any redesign outside the full-app `User Center` overlay seam. |
| Full-App History Drawer Theme Fidelity | Full-app `HistoryDrawer` light-theme fidelity transplant inside the real `AgentShell` seam while keeping current session-routing behavior and current shipped drawer actions | `prototypes/prism-web-v1/src/components/HistoryDrawer.tsx` | Visual QA | Shipped only after L1 + L3 evidence is present | Historical gate label: History Drawer Wave C. Execution brief: `docs/plans/history-drawer-wavec-execution-brief.md`. Current slice refreshes the full-app prototype and Compose drawer for the shipped contract: hamburger entry, scrim-close and session-close ownership in `AgentShell`, collapsible groups, visible overflow actions as the primary `Pin / Rename / Delete` entry, footer profile/settings handoff into the shared `User Center` overlay, and device-capsule handoff into connectivity. Approved now: full-app-only light-theme fidelity for the left-edge drawer slab, floating device capsule, group-card rhythm, row contrast, footer dock chrome, and rename/action-surface legality inside the real shell. Deferred: SIM history/theme work, broad full-app shell recolor, old header `+` action restoration, swipe-close restoration, and any redesign outside the full-app `HistoryDrawer` seam. |
| Onboarding Prototype Family | Shared host-driven onboarding family for full app and SIM connectivity using one Compose coordinator and one accepted step contract | `prototypes/onboarding-family/onboarding_prototype_family.html` plus the approved 2026-03-26 SIM-host screenshot set for `PERMISSIONS_PRIMER`, `SCAN`, `HARDWARE_WAKE`, and later state-scoped screenshot overrides as they are approved | Visual QA | Shipped only after L1 + L3 evidence is present | Historical gate label: Onboarding Wave A. Execution brief: `docs/plans/onboarding-wavea-execution-brief.md`. Current page-by-page progress is tracked under `Onboarding Prototype Page Board` below; current focus is Step 7 `PROVISIONING` form fidelity. The shared shell now also exposes a debug-only onboarding design browser that opens static host/page presets without invoking real onboarding business logic, so page review and screenshot capture no longer need gate resets or live pairing state. Approved now: dark monolith background, frosted cards, permissions primer, abstract voice handshake, manual-connect found card, unified provisioning screen, and shared completion wrapper. Scope remains locked to visual fidelity, host parity, preview/screenshot capture coverage, and acceptance evidence only. Deferred: notification/OEM guidance tail, any reintroduction of naming/account/profile steps, broader onboarding expansion, screenshot-driven redesign outside the active page board, and `OnboardingScreen.kt` cleanup beyond the minimal capture seam. |
| UI Trunk Structure Cleanup | Refactor-first cleanup of shared UI and ViewModel trunk files that are slowing approved prototype transplants | `docs/plans/god-tracker.md` | Transplanting | Continue active god-file cleanup waves outside this tracker | This is a UI-facing dependency record, not the owning structure tracker. Approved now: trunk cleanup is a prerequisite for faster future transplants. Deferred here: detailed structural wave law stays in `god-tracker.md`. |
| Dynamic Island Header | Cerb shard, header-center transplant, scheduler-only one-line island | `docs/cerb/sim-shell/spec.md` | Shipped | Reopen only if future shared-header drift is confirmed | Shared top-header island is delivered for the standard shell. Deferred: future non-scheduler lanes and broader header experiments. |
| SIM Shell High-Fidelity Migration | Prototype-faithful SIM shell chrome transplant | `prototypes/sim-shell-family/sim_home_hero_shell.html` | Shipped | Reopen only for confirmed drift or a new approved shell slice | Approved and shipped slice keeps empty home and active chat on one shell chrome. Current micro-polish add-on: idle composer may rotate one shimmering inline hint line across the base input prompt plus audio-entry guidance, and scheduler discoverability may use a first-launch-only auto-drop teaser. Deferred: future shell polish must stay surgical and must not reopen whole-shell redesign by default. |
| SIM Conversation Surface Fidelity | Prototype-faithful SIM outgoing, incoming, system, processing, artifact, and strategy surfaces inside the shared shell | `docs/reports/20260323-sim-prototype-alignment-audit.md` | Shipped | Reopen only if production drift is observed against the accepted audit | Approved and shipped within the shared SIM shell family. Deferred: non-SIM chat path redesign and unrelated conversation-lane widening. |
| SIM Shell Monolith Edge Refinement | Restore elegant fuzzy seams on the shared top and bottom monoliths without changing shell structure | `docs/reports/20260323-sim-prototype-alignment-audit.md` | Shipped | Reopen only for confirmed seam drift | Delivered neutral feathered seams on the shared monolith shell. Deferred: any stronger atmospheric effects or aurora bleed experiments. |
| SIM Scheduler Drawer Direction | Top-anchored dark frosted scheduler slab over the shipped shared SIM shell while preserving current scheduler IA | `prototypes/sim-shell-family/sim_scheduler_drawer_shell.html` | Transplanting | Visual QA against prototype plus device screenshots | First Compose slice is now in code with shared `SchedulerDrawer` SIM presentation seam; execution brief: `docs/plans/sim-scheduler-drawer-first-slice-brief.md`. Current polish scope also includes page-mode coverage so the SIM chat composer is not left visible under the scheduler page, plus start-time-first timeline cleanup so the SIM rail/header no longer surface dangling end-time placeholders such as `HH:mm - ...`. Approved now: SIM-only visual seam first, shared shell behavior stays shared, inspiration shelf remains visible, and lightweight note/tips content may preserve user-spoken task detail without widening into smart-runtime intelligence. Deferred: persistent peek behavior, broader scheduler intelligence, deeper smart tips, unrelated shell redesign, and any widening beyond the first surgical Compose slice. |

---

### Tracker Organization Model

This tracker now defaults to page-first or surface-first organization for prototype-led UI delivery.

Organization rule:

- prefer prototype-page or real-surface progress tracking over large abstract wave ladders
- keep one summary row per campaign or surface family, then add a focused page board when the prototype sequence is the most intuitive execution model
- historical wave labels may remain in execution-brief names, acceptance reports, or row notes for continuity, but they are no longer the primary tracker grammar
- tracker rows and boards record visual-delivery state only; behavior/state authority still belongs to the owning docs/specs

Current interpretation:

- onboarding now uses the per-page board below because the work is being reviewed in prototype-page order
- SIM shell family should also log page/surface-local polish under the owning page section when a change is narrower than the family summary row
- single-surface campaigns such as the full-app light-theme rows above remain valid as row-level tracking because each row already maps to one concrete delivered surface
- future prototype-led multi-page UI campaigns should prefer the same page-board pattern when it improves review clarity

---

### SIM Shell Prototype Page Board

Owner row: `SIM Shell Prototype Family`

Progress law:

- use this board for SIM shell family page/surface-local UI work when the change is narrower than the family summary row
- keep family-level direction in the summary row above, and log page/surface polish in the page board below
- behavior/state authority remains in the owning SIM shell and scheduler docs; this board records UI execution state only

| Page / Surface | Prototype ID | Production Scope | Stage | Source Lock | Notes |
|----------------|--------------|------------------|-------|-------------|-------|
| `home_shell` | `sim_home_hero_shell.html` | empty home shell chrome + active chat shell chrome | Shipped | Prototype HTML + accepted audit | Shipped shell chrome basis for the SIM family. |
| `audio_drawer` | `sim_audio_drawer_shell.html` | SIM audio drawer browse/select surfaces | In Transplant | Prototype HTML | Keep browse/select fidelity and swipe legality scoped to the approved drawer slice. |
| `history_drawer` | `sim_history_drawer_shell.html` | SIM history drawer slab, grouping, and footer dock | In Transplant | Prototype HTML | Current work remains SIM-only history drawer polish. |
| `user_center` | `sim_user_center_shell.html` | SIM user center drawer chrome and profile/settings sheet | In Transplant | Prototype HTML | Current work remains chrome-first without widening IA. |
| `scheduler_drawer` | `sim_scheduler_drawer_shell.html` | top scheduler slab, calendar strip, timeline rails, task-card surfaces, and scheduler page-mode polish | In Transplant | Prototype HTML + `docs/plans/sim-scheduler-drawer-first-slice-brief.md` | Logged 2026-03-26: SIM-only start-time-first timeline polish landed for the scheduler drawer page. Rail/header no longer surface dangling end-time placeholders such as `HH:mm - ...`; page-mode coverage remains in scope; visual polish tightened SIM rail spacing, dot/line weight, and task-card rhythm without widening scheduler behavior. |

---

### Onboarding Prototype Page Board

Owner row: `Onboarding Prototype Family`

Progress law:

- page order follows `prototypes/onboarding-family/onboarding_prototype_family.html`
- this board tracks visual-delivery progress only; onboarding docs/specs remain the behavior/state source of truth
- behavior and host/routing law stay anchored to `docs/specs/flows/OnboardingFlow.md` and related onboarding/pairing docs
- prototype owns visual direction; approved screenshots may override the prototype for the exact depicted states only
- shared aura background should reuse the existing onboarding/home visual family rather than inventing a separate background system per page
- the debug-only onboarding design browser is the preferred shell-side convenience seam for static page review and screenshot capture prep

Stage law:

- `Pending` = page not yet active
- `In Prototype` = active UI-agent prototype task is out
- `Prototype Review` = prototype returned and is waiting screenshot/human review
- `Prototype Approved` = visual direction is locked for transplant
- `In Transplant` = Compose landing is active
- `Visual Check` = preview/device comparison is active
- `Shipped` = page-level fidelity is accepted
- `Polish Reopen` = user reopened a shipped page for more fidelity work
- `Recheck` = reopened page is back in screenshot/device review

Auto-update evidence law:

- update page stages only from meaningful evidence, not from every internal edit
- allowed evidence sources are: prototype artifact update, approved screenshot, user screenshot correction, preview/device comparison result, or acceptance-report update
- when Codex creates a UI-agent handoff prompt for a page, it should add or update the corresponding page row and move it to `In Prototype`
- when the prototype returns, human approval happens, transplant starts, or screenshot/device QA reopens a page, Codex should update the board in the same session

| Page | Prototype ID | Production Scope | Stage | Source Lock | Notes |
|------|--------------|------------------|-------|-------------|-------|
| P1 | `welcome` | `FULL_APP` `WELCOME` | Pending | Prototype HTML | Full-app only intro surface. |
| P2 | `permissions` | `PERMISSIONS_PRIMER` | Recheck | Prototype HTML + approved screenshots | Shared host surface; keep informational-first permission law. |
| P3 | `handshake` | `FULL_APP` `VOICE_HANDSHAKE` | Pending | Prototype HTML | No fake-assistant theater reintroduction. |
| P4 | `wake` | `HARDWARE_WAKE` | Recheck | Prototype HTML + approved screenshots | Shared host surface. |
| P5 | `scan` | `SCAN` | Recheck | Prototype HTML + approved screenshots | Shared host surface; no auto-connect. |
| P6 | `found` | `DEVICE_FOUND` | Pending | Prototype HTML | Manual-connect law must stay intact. |
| P7 | `wifi` | `PROVISIONING` form + progress | In Prototype | Prototype HTML | Tracks two production substates: form and progress. Current focus: form fidelity first, behavior unchanged. |
| P8 | `timeout` | scan/provisioning recovery surfaces | Pending | Prototype HTML + production error contracts | Recovery page, not a happy-path step. |
| P9 | `complete` | `COMPLETE` | In Prototype | Prototype HTML + approved screenshots | SIM handoff must remain `SETUP -> MANAGER`. |

Tracking rule:

- page stage changes should reflect meaningful checkpoint transitions only
- the overall onboarding row may remain `Visual QA` while individual pages move independently through this board
- P7 stays one prototype-ordered row even though production splits it into two substates

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
