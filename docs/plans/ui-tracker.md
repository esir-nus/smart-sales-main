# UI Tracker

> Purpose: Track UI design, prototype, approval, transplant, and visual QA work separately from the main feature tracker.
>
> Scope: Screens, shared components, design-system updates, prototype reviews, and UI drift follow-ups.
>
> Status: Active
>
> Last Updated: 2026-04-03

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

Annotation rule:

- `Implementation Ahead` is not a standalone status
- use it inside row or core-view notes when accepted production design/code has moved ahead of the current prototype
- when used, also say whether the prototype should catch up later or is intentionally stale

---

## Operating Rules

1. Major UI work should appear here even if backend or domain work is tracked elsewhere.
2. Prototype approval should be visible here before major production transplant begins.
3. Prototype-only work should also be tracked here when it is the current visual source of truth, even if no Compose transplant has started yet.
4. Every active row should state its current gate and visual source of truth.
5. Shared visual-pattern changes should note whether the Visual Identity Guide or UI Element Registry needs sync.
6. Current production UI may be inspected for logic understanding, but it is not the visual source of truth unless explicitly marked as an approved reference.
7. Structural blockers and anti-god-file work belong in `docs/plans/god-tracker.md`; only the UI-facing dependency summary belongs here.
8. Reports and audit notes may be linked as validation evidence or review memory, but `Visual Source` must point to the approved prototype, handoff, or owning UI doc rather than a report.

### `polishing ui` Tracker Interpretation

Standing interpretation for future UI work:

- one `polishing ui` request means one page / surface at a time
- the current deployed UI is the production baseline being polished
- the provided prototype counterpart and prototype HTML are the visual alignment source package
- the default goal is surgical high-fidelity alignment, not a hidden full-surface redesign
- tracker updates should land on the owning family row plus the relevant page / surface or core-view entry when the polish scope is narrower than the family summary

Tracker law for this shorthand:

- do not widen one polishing request into multiple pages unless the user explicitly expands scope
- log the active page / surface, source lock, current gate, and explicit defers
- if production polish lawfully moves ahead of the prototype, record that as `Implementation Ahead` instead of pretending the prototype is exhaustive

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
| UI Workflow Foundation | Dedicated UI tracker, SOP clarification, aesthetic-source rule, and standing `polishing ui` shorthand | `docs/sops/ui-dev-mode.md` | Shipped | Maintain as standing law | Approved and synced on 2026-03-26. Logged 2026-03-28: prototype-to-Compose transplant now carries a standing system-bar-awareness rule. Prototype `44px` status bars remain visual placeholders only; production surfaces must own real status/navigation bar insets locally, while spec-owned monolith-aligned exception surfaces keep their shell alignment instead of flattening to generic top padding. Defers: future SOP expansion only when new UI workflow drift appears. |
| SIM Shell Prototype Family | Standalone SIM HTML prototype family for empty home hero, active chat shell, system/status/artifact surfaces, and scheduler-drawer exploration | `prototypes/sim-shell-family/sim_prototype_family.html` | Prototyping | Keep the prototype family current as the visual source before each surgical transplant slice | Prototype work itself is active UI scope, not just a hidden precursor to Compose. Wave 13 now uses the light child sources `sim_home_hero_shell_light.html` and `sim_history_drawer_shell_light.html` as the launcher-core theme-visibility reference stack while the broader family remains the shell context source. Logged 2026-03-28: the tracker-referenced `sim_user_center_shell_light.html` file is currently missing locally, so the active SIM user-center polish now uses the user-provided `Image #1` screenshot override plus the SIM handoff/mechanics docs instead of pretending the missing HTML remains available. Logged 2026-04-02 home-shell screenshot polish: the active `home_shell` pass keys from the provided current-vs-prototype screenshot pair plus `sim_home_hero_shell.html`, keeping the shipped live-name hero and mic-when-empty composer contracts while re-centering the empty-home greeting rhythm instead of widening into shell-behavior changes. Deferred: unrelated non-SIM prototype experiments and any whole-app redesign outside the current SIM shell family. |
| SIM Audio Drawer Direction | Prototype-approved SIM audio drawer browse/select presentation transplant within the existing SIM shell and audio/chat contract | `prototypes/sim-shell-family/sim_audio_drawer_shell.html` | Transplanting | Focused Compose transplant plus visual QA against prototype/device screenshots | Prototype now covers browse-mode delete/transcribe states alongside select-mode suppression. Current surgical transplant scope is: darker frosted drawer slab, compact browse/select card rhythm, right-swipe transcribe on collapsed pending cards, left-swipe delete on collapsed pending/transcribed cards, no delete on expanded/transcribing cards, and prototype-faithful shimmer / star / truncated-preview treatment. Logged 2026-03-28: the drawer now owns navigation-bar clearance locally instead of relying on host spacing. Logged 2026-04-02 browse-header transplant: browse mode now rebuilds around a grip + title/count + single smart-capsule header, the drawer slab keeps local nav-bar ownership while using handoff-style pull-to-sync translation only as visual motion, blocked upward pull stays local-denied instead of faking sync, blocked capsule taps hand off to connectivity, and select mode continues to suppress browse-only chrome. Logged 2026-04-04 gesture/gate correction: the browse grip now checks the real upward drag distance instead of the reduced visual translation, so the handle can actually reach the manual-sync threshold on device, and READY manual sync reuses the strict badge-readiness gate before transport starts so disconnected states fail explicitly instead of feeling like dead motion. Chat-select mode remains selection-only with swipe and delete suppressed. Deferred: confirm modals, cancel-transcription semantics, runtime rewiring beyond unlink-on-delete, deeper badge transport recovery, non-SIM shell redesign, and any follow-up polish beyond the first approved transplant pass. |
| SIM History Drawer Direction | SIM-only history drawer polish inside the shared SIM shell using the approved left-edge light/dark slab prototypes and existing SIM session actions | `prototypes/sim-shell-family/sim_history_drawer_shell_light.html` | Visual QA | Shipped only after device screenshots confirm light-theme fidelity on the normal SIM host | Wave 13 now makes the launcher-core history drawer theme-aware while preserving current SIM session actions and history-to-settings ownership. Logged 2026-03-28: the SIM drawer internals are rewritten fresh on the real `SimHistoryDrawer` seam instead of layering more numeric polish on the prior variant. The current slice keeps the repo-wide blank top safe-area band and visible `历史记录` title, preserves host-level scrim ownership plus long-press-primary session actions, rebuilds grouped list/empty/dock structure from the latest light prototype under the repo's no-blur readability-first material constraint, adds explicit drawer pointer-event consumption, and adds four in-file L1 preview states for populated / empty / truncation / dense-group validation. Logged 2026-03-28 follow-up: the drawer now owns real status/navigation bar insets locally through the shared additive helper seam. Logged 2026-03-28 top-safe-area drift repair: after rechecking the light prototype, the drawer now renders the blank-band inset path through an explicit shared top-safe-area seam instead of relying only on additive modifier padding. Deferred: live search, swipe-reveal actions, top header utility chrome such as search/device capsule, shared smart-app history rewrite, and any widening beyond the SIM support surface. |
| SIM User Center Direction | SIM-only user-center chrome transplant inside the existing shell using the approved dark drawer handoff while preserving the current edit flow, theme behavior, and explicit deferred-row policy | `handoffs/user_center_compose_handoff.md` plus the recorded SIM dark reference captures | Transplanting | Visual QA against fresh SIM-host screenshots | Wave 13 keeps the launcher-core settings drawer theme-aware while preserving current profile/edit flow and shared theme selector behavior. Logged 2026-03-27: the live SIM drawer wiring removes the stale visible `X` and `面容 ID` row and keeps the protected top safe-area band with hero-first composition. Logged 2026-03-28 follow-up: the drawer owns real status/navigation bar insets locally through the shared additive helper seam. Logged 2026-03-28 screenshot-override polish: because `sim_user_center_shell_light.html` is missing locally, the active SIM pass keys fidelity from the approved dark handoff package instead of pretending the light prototype still exists; production polish tightens the drawer into a calmer near-solid slab with denser section cards, smaller metadata chips, a demoted secondary `编辑资料` pill, and explicit drawer pointer-event consumption. Logged 2026-03-30 approved IA reopen: the SIM drawer now restores the prototype section order `偏好设置 / 空间管理 / 安全与隐私 / 关于 / 退出登录` with an explicit row-behavior matrix. Real interactive rows stay limited to `编辑资料`, `主题外观`, and `消息通知`; the build-sourced `版本` row stays informational; `AI 实验室`, `已用空间`, `清除缓存`, `修改密码`, `帮助中心`, and `退出登录` return as muted deferred-disabled IA only and must not imply shipped flows. Logged 2026-03-30 dark-source polish pass: the settings overlay now uses handoff-style end-edge slide plus fade timing instead of spring motion, the dark slab/card rhythm is denser and calmer, and light theme remains non-regression-only rather than a parallel redesign target. Deferred: future SIM support/privacy/license flows, any trigger redesign beyond the current SIM settings entry path, and host-scrim alpha changes unless screenshot review proves the polished drawer still leaks too much background. |
| Full-App User Center Theme Fidelity | Full-app `User Center` light-theme fidelity transplant inside the real `AgentShell` overlay seam while keeping current theme behavior and current shipped IA | user-provided screenshot override plus `prototypes/prism-web-v1/src/components/UserCenter.tsx` fallback | Visual QA | Shipped only after refreshed L1 + L3 evidence is present | Historical gate label: User Center Wave B. Execution brief: `docs/plans/user-center-waveb-execution-brief.md`. Current slice is a screenshot-override polishing pass on the shipped IA `Preferences / Storage / Security / About / Logout`: narrow centered floating sheet, calmer centered hero, denser light cards, stronger overlay ownership, `空间管理` storage split into `已用空间` plus `清除缓存`, and biometric removed from the full-app security section. Logged 2026-03-27 follow-up polish: remove the visible top-left close affordance, keep hero-first content fully above the fold, further mute background/history readability through stronger `AgentShell` scrim plus less translucent sheet/card material, and tighten section/type/row cadence toward the latest approved full-app reference image without changing IA or behavior. Logged 2026-03-28 follow-up: inset ownership moves off the shared host root and onto the full-app sheet itself. Logged 2026-03-28 top-safe-area drift repair: after rechecking the approved full-app reference, the sheet now renders the protected top band through the shared explicit top-safe-area seam so the hero-first layout clears the native status region without relying only on root modifier padding. Implementation-ahead exceptions remain explicit: `编辑资料` stays present, `AI 实验室` remains a toggle, and lower `关于 / 退出登录` content stays shipped even though the screenshot crop is narrower. Deferred: SIM light-theme parity, broader full-app shell recolor, old full-app `Support`-section restoration, and any redesign outside the full-app `User Center` overlay seam. |
| Full-App History Drawer Theme Fidelity | Full-app `HistoryDrawer` light-theme fidelity transplant inside the real `AgentShell` seam while keeping current session-routing behavior and current shipped drawer actions | `prototypes/prism-web-v1/src/components/HistoryDrawer.tsx` | Visual QA | Shipped only after L1 + L3 evidence is present | Historical gate label: History Drawer Wave C. Execution brief: `docs/plans/history-drawer-wavec-execution-brief.md`. Current slice refreshes the full-app prototype and Compose drawer for the shipped contract: hamburger entry, scrim-close and session-close ownership in `AgentShell`, collapsible groups, visible overflow actions as the primary `Pin / Rename / Delete` entry, footer profile/settings handoff into the shared `User Center` overlay, and device-capsule handoff into connectivity. Approved now: full-app-only light-theme fidelity for the left-edge drawer slab, floating device capsule, group-card rhythm, row contrast, footer dock chrome, and rename/action-surface legality inside the real shell. Logged 2026-03-28 follow-up: top inset ownership moves off the shared host root and onto the drawer itself while preserving the floating capsule offset. Deferred: SIM history/theme work, broad full-app shell recolor, old header `+` action restoration, swipe-close restoration, and any redesign outside the full-app `HistoryDrawer` seam. |
| Onboarding Prototype Family | Shared host-driven onboarding family for full app and SIM connectivity using one Compose coordinator and one accepted step contract | `prototypes/onboarding-family/onboarding_interactive_prototype.html` plus the approved onboarding screenshot set for non-interactive pairing pages | Transplanting | Focused Compose landing plus visual QA for the new consultation/profile intro pages | Historical gate labels: Onboarding Wave A plus SIM intro-dark follow-up. Active brief: `docs/plans/onboarding-interactive-handshake-execution-brief.md`. Current page-by-page progress is tracked under `Onboarding Prototype Page Board` below. The active slice keeps `WELCOME` plus `PERMISSIONS_PRIMER`, replaces the old abstract handshake with two real intro steps (`VOICE_HANDSHAKE_CONSULTATION`, `VOICE_HANDSHAKE_PROFILE`) on both hosts, keeps pairing ownership starting at `HARDWARE_WAKE`, and adds typed profile extraction plus explicit CTA save before pairing. Logged 2026-03-30 voice-lane follow-up: onboarding now keeps first-use mic permission point-of-use without cancelling the pending request, but grant returns the user to idle with a fresh tap requirement instead of auto-resuming capture; the footer also preserves captured transcript through processing when FunASR has already emitted text. Logged 2026-03-31 runtime-proof follow-up: live logcat proved the broken branch was realtime cancellation before finger release rather than a slow timeout, so onboarding now surfaces active listening failures immediately, returns the footer to tap retry without waiting for a stop tap, preserves real transcript on downstream generation failure, and uses bounded retry UI instead of synthetic content. Logged 2026-04-03 unification follow-up: production onboarding now uses one canonical post-pairing route for both cold start and connectivity setup entry, removes the global top-right `跳过`, adds local `跳过，直接体验日程` actions only on the BLE wake page and the Wi-Fi entry / Wi-Fi recovery surfaces, always routes successful provisioning into `SCHEDULER_QUICK_START`, commits staged quick-start items only after the shared `COMPLETE` CTA, and returns every successful production path to home with the one-shot scheduler drawer reveal. Approved now: dark monolith background, frosted dark family, shared mic/footer system with onboarding-wide two-phase tap voice capture, chat-list reveal rhythm, profile extraction card, shared SIM aurora reuse, one onboarding-owned intro AI/audio lane before pairing, the real quick-start voice pipeline with scheduler Path A reuse plus organic reminder-reliability/calendar prompts, and the page-local pairing-step skip affordance. Deferred: old post-pairing profile-tail revival, badge-audio/Tingwu reuse, redesign outside the active page board, and broad `OnboardingScreen.kt` cleanup beyond the extracted interaction seam. |
| UI Trunk Structure Cleanup | Refactor-first cleanup of shared UI and ViewModel trunk files that are slowing approved prototype transplants | `docs/plans/god-tracker.md` | Transplanting | Continue active god-file cleanup waves outside this tracker | This is a UI-facing dependency record, not the owning structure tracker. Approved now: trunk cleanup is a prerequisite for faster future transplants. Deferred here: detailed structural wave law stays in `god-tracker.md`. |
| Dynamic Island Header | Shared header-center transplant, scheduler-only one-line island | `docs/cerb-ui/dynamic-island/spec.md` plus `docs/specs/prism-ui-ux-contract.md` | Shipped | Reopen only if future shared-header drift is confirmed | Shared top-header island is delivered for the standard shell. Deferred: future non-scheduler lanes and broader header experiments. |
| SIM Shell High-Fidelity Migration | Prototype-faithful SIM shell chrome transplant for the normal launcher path | `prototypes/sim-shell-family/sim_home_hero_shell_light.html` | Visual QA | Shipped only after light-theme launcher screenshots are accepted | Wave 13 reopens the shipped shell row only for launcher-core theme visibility: `SimMainActivity`, the shared home/chat shell frame, and the current header/dynamic-island chrome now follow the shared theme preference. Logged 2026-03-27: a narrow empty-home light-theme polish pass is active for the shared shell chrome plus the greeting canvas only. The hero now personalizes the title from live profile display name in `你好, {displayName}` form with `SmartSales 用户` as the blank fallback while preserving the static subtitle `我是您的销售助手`, scheduler-backed island text, and existing shell behavior. Follow-up seam polish now narrows specifically to light-theme top/bottom monolith edge blending so the shell keeps prototype-like depth without the current dark halo around the empty-home bars, with a final refinement pass further reducing the residual gray band under the top bar and above the composer. Logged 2026-03-28 follow-up: the shared SIM home/chat shell now routes status/navigation bar handling through the shared surface-owned inset helpers, removing raw SIM root inset modifiers while preserving the monolith-owned top header and bottom composer alignment. Logged 2026-03-28 voice-draft follow-up: the shared SIM composer now uses a SIM-owned mic-when-empty, send-when-drafted contract with onboarding-style handshake motion during SIM FunASR realtime capture while keeping attach-left, explicit send, and ordinary SIM chat scope. Logged 2026-03-30 auth follow-up: the earlier backend-issued short-lived DashScope token experiment was removed after SDK-guide drift audit; current shared realtime capture uses direct `DASHSCOPE_API_KEY` SDK init while keeping the same composer contract and scope lock. Logged 2026-04-01 scheduler discoverability follow-up: when SIM has no urgent tasks, the island now shows a session-only `下滑这里查看日程` teaching line until the user explicitly opens the scheduler, while the persisted first-launch teaser remains unchanged. Logged 2026-04-02 runtime-shell island transplant: the live shell now keeps scheduler as the default lane, lets connectivity interrupt in the center island using transport-truth connection states, routes tap according to the currently visible lane, and keeps downward drag scheduler-only. Deferred: real bridge-backed battery sourcing, future shell polish must stay surgical and must not reopen whole-shell redesign by default, and deferred overlays still do not claim light-theme parity. |
| SIM Conversation Surface Fidelity | Prototype-faithful SIM outgoing, incoming, system, processing, artifact, and strategy surfaces inside the shared shell | `prototypes/sim-shell-family/sim_home_hero_shell_light.html` | Visual QA | Shipped only after light-theme active-chat screenshots are accepted | Wave 13 reopens the active chat row only for launcher-core theme visibility so conversation/system/status/artifact surfaces visually follow the shared theme preference on the normal SIM host. Deferred: non-SIM chat path redesign, unrelated conversation-lane widening, and deferred SIM overlays outside the launcher-core path. |
| SIM Shell Monolith Edge Refinement | Restore elegant fuzzy seams on the shared top and bottom monoliths without changing shell structure | `prototypes/sim-shell-family/sim_home_hero_shell.html` | Shipped | Reopen only for confirmed seam drift | Delivered neutral feathered seams on the shared monolith shell. Evidence: `docs/reports/20260323-sim-prototype-alignment-audit.md` accepted the shipped seam pass against the shared SIM shell prototype. Deferred: any stronger atmospheric effects or aurora bleed experiments. |
| SIM Scheduler Drawer Direction | Top-anchored dark frosted scheduler slab over the shipped shared shell/base-runtime lane while preserving current scheduler IA | `prototypes/sim-shell-family/sim_scheduler_drawer_shell.html` | Transplanting | Visual QA against prototype plus device screenshots | First Compose slice is now in code with shared `SchedulerDrawer` presentation seams; execution brief: `docs/plans/sim-scheduler-drawer-first-slice-brief.md`. Current polish scope includes page-mode coverage so the chat composer is not left visible under the scheduler page, accepted start-time-first timeline cleanup so the rail/header no longer surface dangling end-time placeholders such as `HH:mm - ...`, and the approved full-surface follow-up covering collapsed, dense, done, off-page-attention, collapsed-conflict, expanded-details, expanded-note, expanded-tips, and expanded-conflict views. Logged 2026-03-28 follow-up: scheduler inset handling now preserves the approved top-monolith alignment while adding real status-bar awareness at the surface layer; it is intentionally not flattened into the generic blank safe-band rule. Logged 2026-03-30 full-surface polish: the drawer now adopts the centered current-month header plus unfold pill grammar, continuous timeline beam, prototype-style collapsed card composition, quieter inspiration shelf, and bottom-edge dismiss affordance while keeping behavior and interfaces unchanged. Logged 2026-03-30 review follow-up: focused drawer acceptance now runs against the production presentation path and explicitly covers done, collapsed-conflict, expanded-details, expanded-note, expanded-tips, and expanded-conflict states. Logged 2026-03-30 screenshot-correction follow-up: scheduler-open chrome now hides the hamburger and new-chat actions while the center dynamic island stays mounted, the dismiss handle moves to the bottom and dismisses by upward swipe, month chevrons page visible month without acknowledging date attention, and timeline polish now keeps one-line rail times with a shorter terminal rail tail. Logged 2026-03-30 interaction fix: the bottom handle now dismisses reliably on tap as well as upward swipe. Logged 2026-03-30 device-gesture follow-up: the scheduler handle now uses a dedicated, lower-threshold drag recognizer on the actual interactive hit target so short real-device upward swipes dismiss reliably without retuning shared shell gestures. Logged 2026-03-31 gesture correction: opening now follows a downward swipe on the dynamic island instead of the full top header band, and the visible calendar pill supports upward-swipe dismissal for the scheduler drawer. Logged 2026-04-01 empty-state guide follow-up: when SIM scheduler content is fully empty, the timeline lane now swaps to one instructional card with `用工牌录音创建日程` guidance instead of implying a real task or inspiration row. This UI row remains visual-only; scheduler ingress/create hardening is tracked in the shared scheduler spec/tracker lane, not here. Approved now: shared visual seam first, inspiration shelf remains visible, and lightweight note/tips content may preserve user-spoken task detail without widening into smart-runtime intelligence. Deferred: persistent peek behavior, broader scheduler intelligence, deeper smart tips beyond current visual transplant, unrelated shell redesign, and any widening beyond the first surgical Compose slice. |

---

### Tracker Organization Model

This tracker now defaults to **Family -> Page/Surface -> Core View when needed** for prototype-led UI delivery.

Organization rule:

- prefer prototype-page or real-surface progress tracking over large abstract wave ladders
- keep one summary row per campaign or surface family, then add a focused page board when the prototype sequence is the most intuitive execution model
- add a core-view board only when a page/surface prototype exposes meaningful review states that materially affect implementation alignment or polish tracking
- historical wave labels may remain in execution-brief names, acceptance reports, or row notes for continuity, but they are no longer the primary tracker grammar
- tracker rows and boards record visual-delivery state only; behavior/state authority still belongs to the owning docs/specs

Authority split:

- prototype = alignment source and review checkpoint set
- accepted implementation = current delivered reality
- UI tracker = progress memory, drift memory, and implementation-ahead logging

Rule:

- do not pretend the prototype is fully final once accepted implementation has moved ahead
- do not silently let implementation drift live only in code
- log accepted implementation-ahead changes in the relevant page/surface or core-view notes

Current interpretation:

- onboarding now uses the per-page board below because the work is being reviewed in prototype-page order
- SIM shell family should also log page/surface-local polish under the owning page section when a change is narrower than the family summary row
- scheduler-style surfaces that expose explicit prototype review states should also use a core-view board under the owning page/surface section
- single-surface campaigns such as the full-app light-theme rows above remain valid as row-level tracking because each row already maps to one concrete delivered surface
- future prototype-led multi-page UI campaigns should prefer the same page-board pattern when it improves review clarity

---

### SIM Shell Prototype Page Board

Owner row: `SIM Shell Prototype Family`

Progress law:

- use this board for SIM shell family page/surface-local UI work when the change is narrower than the family summary row
- keep family-level direction in the summary row above, and log page/surface polish in the page board below
- behavior/state authority remains in the shared shell/UI docs, relevant `docs/core-flow/**`, and shared scheduler/audio docs; this board records UI execution state only

| Page / Surface | Prototype ID | Production Scope | Stage | Source Lock | Notes |
|----------------|--------------|------------------|-------|-------------|-------|
| `home_shell` | `sim_home_hero_shell.html` | empty home shell chrome + active chat shell chrome | Shipped | Prototype HTML + accepted audit + screenshot comparison package | Shipped shell chrome basis for the SIM family. Logged 2026-04-02: RuntimeShell now owns dynamic-island lane arbitration in production, keeping scheduler as the default island, allowing approved connectivity interrupts/heartbeat in the same center slot, routing tap to the visible lane, and preserving scheduler-only downward drag. Logged 2026-04-02 fidelity follow-up: the dark empty-home header now matches the approved ambient-icon handoff, so the connected lane renders borderless chroma-led island text plus shell-owned decorative left/right ambient flank icons, while inline battery-inside-island rendering is retired. Logged 2026-04-02 screenshot polish follow-up: empty-home greeting placement now uses a home-hero-local layout mode so normal phone widths keep the prototype-style calmer center band instead of falling into the tighter top-biased fallback; the composer stays on the shipped mic-when-empty/send-when-drafted contract rather than reverting to the older send-only prototype affordance. Logged 2026-04-03 center-cluster polish: the header now groups the connected ambient link and battery chrome around a dedicated center cluster instead of letting them drift against the full header width, the island body keeps a steadier minimum footprint with tighter padding for the short connectivity copy, and the normal-height hero greeting stays visually calmer without reopening shell behavior. Logged 2026-04-03 connected-state linger follow-up: the connected island now stays visible for `5s` on state change and on its heartbeat pass, while the shell-owned flank chrome uses a stronger breathing cycle so the connected state reads more clearly from the home screenshot distance. Deferred: real battery sourcing is still mock/viewmodel-backed in this slice; this pass does not widen into drawer, scheduler, or whole-shell redesign work. |
| `audio_drawer` | `sim_audio_drawer_shell.html` | SIM audio drawer browse/select surfaces | In Transplant | Prototype HTML | Logged 2026-04-02: Compose now adopts the browse pull-to-sync grip, typed smart-capsule feedback, and browse-only demo helper deck while keeping select-mode suppression, card swipe legality, and local inset ownership. Logged 2026-04-02 follow-up: the standalone connectivity icon was removed, so the badge status capsule is now the only browse-header connectivity handoff. Logged 2026-04-04 interaction fix: the grip threshold now follows raw upward drag while the drawer translation stays rubber-banded visual-only, so pull-up no longer acts like inert dead motion, and focused `SimDrawerGestureTest` device coverage now passes against density-scaled gesture thresholds. Next gate remains device screenshot QA against browse collapsed / pull-threshold / transcribing / expanded artifact / select current / select pending states. |
| `history_drawer` | `sim_history_drawer_shell.html` | SIM history drawer slab, grouping, and footer dock | In Transplant | Prototype HTML | Current work remains SIM-only history drawer polish. |
| `user_center` | `sim_user_center_shell.html` | SIM user center drawer chrome and profile/settings sheet | In Transplant | Screenshot override + handoff | Logged 2026-03-27: surgical polish pass active for denser frosted material, centered hero spacing, icon-led row rhythm, stronger section-card ownership, and corrected live wiring. Logged 2026-03-28: local light-theme polish now follows `Image #1` because the tracker-referenced light HTML is missing locally. Logged 2026-03-30: the SIM drawer now restores the prototype section order while keeping real interaction limited to `编辑资料`, `主题外观`, and `消息通知`; the build-sourced `版本` row stays informational, deferred-disabled placeholder rows for AI lab, storage/cache, password, help, and logout are visible without implying shipped flows, and the stale visible `X` plus `面容 ID` row remain removed. |
| `scheduler_drawer` | `sim_scheduler_drawer_shell.html` | top scheduler slab, calendar strip, timeline rails, task-card surfaces, expanded scheduler states, and scheduler page-mode polish | In Transplant | Prototype HTML + `docs/plans/sim-scheduler-drawer-first-slice-brief.md` | Logged 2026-03-26: start-time-first timeline polish landed for the scheduler drawer page. Rail/header no longer surface dangling end-time placeholders such as `HH:mm - ...`; page-mode coverage remains in scope. Logged 2026-03-30: full-surface polish is active on the shared drawer seam, including the centered current-month header, unfold pill, continuous rail, collapsed/dense/done/conflict states, expanded detail/note/tips/conflict states, quieter inspiration shelf, and bottom-edge dismiss treatment without widening scheduler behavior. Logged 2026-03-30 review follow-up: drawer acceptance now hits the production render path and covers done, collapsed-conflict, expanded-details, expanded-note, expanded-tips, and expanded-conflict states instead of only the generic shared drawer default. Logged 2026-03-30 screenshot-correction follow-up: the production drawer now treats the prototype header side icons as stale, keeps only the dynamic island while scheduler is open, moves dismissal to a bottom upward-swipe handle, pages month view safely without attention acknowledgement, and tightens the timeline gutter / terminal rail rhythm against device feedback. Logged 2026-03-30 device-gesture follow-up: the production handle now recognizes short real-device upward swipes on the actual hit target instead of only passing large synthetic swipe tests. Logged 2026-04-01 inspiration-box transplant: the inspiration shelf now follows the prototype card grammar with `灵感记录` header copy, icon-only `Ask AI` affordance, AI-badged leading tile, and prototype-style right-swipe delete reveal while keeping shell-owned handoff behavior unchanged. Logged 2026-04-01 empty-state guide follow-up: fully empty SIM scheduler content now renders one instructional badge-recording guide card instead of a fake task row. |

---

### SIM Scheduler Drawer Core-View Board

Owner page/surface: `scheduler_drawer`

Core-view law:

- use the prototype review menu as the implementation alignment checklist for this page
- log polish and drift at the core-view level when a change affects one or more specific scheduler drawer states
- if implementation moves ahead of the prototype, mark it in the affected core-view notes as `Implementation Ahead`

| Core View | Prototype View ID | Production Scope | Stage | Source Lock | Notes |
|-----------|-------------------|------------------|-------|-------------|-------|
| `Peek` | `1-peek` | top-edge peek / pre-open shell state | Pending | Prototype HTML | Keep reserved for future peek production work; current first slice does not widen into persistent peek behavior. |
| `Open (Normal Collapsed)` | `2-open` | default collapsed open scheduler drawer with normal task cards | In Transplant | Prototype HTML + accepted implementation | Logged 2026-03-26: `Implementation Ahead` for the current timeline presentation. Accepted production now uses start-time-first collapsed rail/header treatment and removes dangling `HH:mm - ...` placeholders while preserving scheduler behavior. Prototype may catch up later. Logged 2026-03-30 screenshot-correction follow-up: production hides stale scheduler-open side-header icons, keeps the dynamic island centered, and moves the dismiss grip to the drawer bottom with upward swipe semantics. Logged 2026-04-01 empty-state follow-up: when the SIM drawer has no task or inspiration content, this view now swaps the timeline lane to a guide card that teaches badge recording instead of leaving an ambiguous blank rail. |
| `Calendar Unfolded` | `6-calendar` | calendar-expanded scheduler drawer state | In Transplant | Prototype HTML | Logged 2026-03-30: month chevrons now page visible month inside the UI layer only; attention acknowledgement still waits for explicit day tap, so this view is now `Implementation Ahead` relative to the older static prototype. |
| `Collapsed (Dense Day)` | `7-dense` | collapsed high-density day layout | In Transplant | Prototype HTML + accepted implementation | Logged 2026-03-26: collapsed SIM timeline polish also affects dense-day rail rhythm and task-card spacing. `Implementation Ahead` remains acceptable while the prototype family catches up. |
| `Collapsed (Done)` | `5-done` | completed-task collapsed state | In Transplant | Prototype HTML + accepted implementation | Logged 2026-03-30: done cards now join the full-surface SIM card transplant so completion remains visually distinct without redefining urgency. |
| `Inspiration Box` | `6-inspiration` | SIM-only inspiration shelf header, card, swipe-delete reveal, and Ask AI affordance | In Transplant | Prototype HTML + handoff | Logged 2026-04-01: shared `SchedulerDrawer` SIM mode now follows the prototype/handoff for the inspiration shelf only. Visible count text is removed in favor of `灵感记录`, the action is icon-only with accessible `Ask AI` semantics, and the right-swipe delete reveal now matches the prototype rail while preserving shell-owned handoff behavior and keeping standard mode unchanged. |
| `Off-page attention` | `4-offpage` | date-attention state when target content is off-page | In Transplant | Prototype HTML | Existing page-mode / off-page attention alignment remains active. |
| `Collapsed (Conflict)` | `3-conflict` | collapsed conflict-visible scheduler card state | In Transplant | Prototype HTML | Keep conflict-visible collapsed alignment distinct from normal collapsed state. |
| `Expanded: Details` | `8-exp-basic` | expanded details state | In Transplant | Prototype HTML + accepted implementation | Logged 2026-03-26: expanded SIM details lane now follows the same start-time-first cleanup and avoids dangling end-time placeholder rendering in the date row. `Implementation Ahead` may remain until prototype refresh. |
| `Expanded: + User Note` | `9-exp-note` | expanded details with note content | In Transplant | Prototype HTML + accepted implementation | Logged 2026-03-30: note-lane styling now lands inside the same SIM frosted detail grammar instead of falling back to the older scheduler row treatment. |
| `Expanded: + AI Tips` | `10-exp-tips` | expanded details with AI tips lane | In Transplant | Prototype HTML + accepted implementation | Logged 2026-03-30: AI tips now share the SIM detail-card material family and remain visual-only polish with no behavior widening. |
| `Expanded: Conflict` | `11-exp-conflict` | expanded conflict-resolution state | In Transplant | Prototype HTML + accepted implementation | Logged 2026-03-30: expanded conflict resolution now joins the SIM dark/frosted family so conflict states do not regress to the older scheduler styling during full-surface polish. |

---

### Onboarding Prototype Page Board

Owner row: `Onboarding Prototype Family`

Progress law:

- page order follows `prototypes/onboarding-family/onboarding_interactive_prototype.html`
- this board tracks visual-delivery progress only; onboarding docs/specs remain the behavior/state source of truth
- behavior and host/routing law stay anchored to `docs/specs/flows/OnboardingFlow.md` and related onboarding/pairing docs
- prototype owns visual direction; approved screenshots may override the prototype for the exact depicted states only
- shared aura background should reuse the shipped SIM chat aurora where that surface is the approved reference rather than inventing a page-local background system
- the debug-only onboarding static review seam remains the preferred shell-side convenience path for page review and screenshot capture prep when a static capture surface is needed
- on the SIM path, the audio-drawer debug entrance now replays the native onboarding flow instead of reopening the old fullscreen static review deck

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
| P1 | `welcome` | `WELCOME` on both hosts | In Transplant | Prototype HTML + approved screenshot | Shared intro surface. Logged 2026-03-27: screenshot-driven transplant aligns the hero spacing, calmer frosted `SS` tile, and bottom-anchored `开启旅程` CTA, replays the page on the SIM setup host too, and reuses the shared SIM aurora instead of a page-local onboarding background. |
| P2 | `permissions` | `PERMISSIONS_PRIMER` | Recheck | Prototype HTML + approved screenshots | Shared host surface; keep informational-first permission law. Logged 2026-03-27: permissions page polish reopened against the approved screenshot direction to tighten copy, remove per-card affordances, bottom-anchor and strengthen the `继续` CTA, while dark-locking the reused SIM aurora for onboarding review/capture. |
| P3A | `consultation` | `VOICE_HANDSHAKE_CONSULTATION` on both hosts | In Transplant | Interactive prototype HTML | Logged 2026-03-27: this page now owns the real two-turn phone-mic consultation before pairing. Required fidelity points: collapsing hero, anchored two-phase tap footer, real transcript + AI reply bubbles, post-round auto-scroll, and completion reveal after round two. Logged 2026-03-28 reliability follow-up: realtime FunASR callback payloads are now sanitized before reaching transcript/error UI, and the onboarding tap-started consultation lane now permits up to `60s` capture with automatic transition into processing when the cap is reached. Logged 2026-03-30 interaction follow-up: first-use mic permission grant now returns consultation to idle with a fresh tap requirement, and the footer hint slot keeps captured transcript visible through processing instead of dropping back to an empty state. Logged 2026-03-30 watchdog follow-up: if recognizer cancellation arrives only after the stop tap, consultation now exits processing through local fallback instead of sitting indefinitely in `正在识别语音...`; explicit user/reset/background cancellation still clears silently. |
| P3B | `profile` | `VOICE_HANDSHAKE_PROFILE` on both hosts | In Transplant | Interactive prototype HTML | Logged 2026-03-27: this page now owns the real phone-mic profile capture before pairing. Required fidelity points: transcript + acknowledgement bubbles, extraction card reveal, explicit CTA save, retry/skip calm-failure treatment, and CTA copy that advances to pairing rather than entering the app. Logged 2026-03-28 reliability follow-up: realtime FunASR callback payloads are now sanitized before reaching transcript/error UI, and the onboarding tap-started profile lane now permits up to `60s` capture with automatic transition into processing when the cap is reached. Logged 2026-03-30 interaction follow-up: first-use mic permission grant now returns profile to idle with a fresh tap requirement, and the footer hint slot keeps captured transcript visible through processing instead of dropping back to an empty state. Logged 2026-03-30 watchdog follow-up: if recognizer cancellation arrives only after the stop tap, profile now exits processing through local fallback instead of sitting indefinitely in `正在识别语音...`; explicit user/reset/background cancellation still clears silently. |
| P3C | `scheduler_quick_start` | `SCHEDULER_QUICK_START` on all production onboarding entries after provisioning or approved pairing-step skip | In Transplant | Interactive prototype HTML + `handoffs/schedule_quick_start_compose_handoff.md` | Logged 2026-04-03: this page now owns the real voice-driven scheduler quick-start sandbox after successful provisioning and before the shared `COMPLETE` screen. Required fidelity points: shared onboarding frame/background/footer reuse, fixed `[time][description][bells][date]` row grammar, staggered list entrance, update-highlight pulse on edited rows, organic point-of-use mic/calendar prompts, shared reminder-reliability dialog reuse for notification/exact-alarm/OEM guidance, shared onboarding two-phase tap mic control, and final persistence delayed until the shared `COMPLETE` CTA. Runtime law now reuses scheduler Path A create/update extraction seams plus a sandbox-only target resolver; it must not fork into a scripted demo lane or a parallel scheduler runtime. Logged 2026-04-03 create-route unification follow-up: onboarding and the main scheduler now share one internal Path A create interpreter for deterministic helpers plus `Uni-M` / `Uni-A` / `Uni-B`; onboarding still passes `displayedDateIso = null`, still excludes `Uni-C`, and gives `Uni-M` only a bounded onboarding-local sub-budget so batch quick-start phrases can expand without reviving the old watchdog drift. |
| P4 | `wake` | `HARDWARE_WAKE` | Pending | Prototype HTML + approved screenshots | Shared host surface. Background ownership stays on the onboarding-wide shared SIM aurora. Production now also requires the local `跳过，直接体验日程` affordance here; no global skip remains in onboarding chrome. |
| P5 | `scan` | `SCAN` | Pending | Prototype HTML + approved screenshots | Shared host surface; no auto-connect. Scanner visual QA remains separate from the intro interaction slice. |
| P6 | `found` | `DEVICE_FOUND` | Pending | Prototype HTML | Manual-connect law must stay intact. |
| P7 | `wifi` | `PROVISIONING` form + progress | Pending | Prototype HTML | Tracks two production substates: form and progress. Production now also requires the local `跳过，直接体验日程` affordance on the Wi-Fi form and Wi-Fi recovery surfaces only; progress/success stays skip-free. |
| P8 | `timeout` | scan/provisioning recovery surfaces | Pending | Prototype HTML + production error contracts | Recovery page, not a happy-path step. |
| P9 | `complete` | `COMPLETE` | Pending | Prototype HTML + approved screenshots | Production handoff must keep this screen, then enter home and auto-open the real scheduler drawer once through the shell seam. Connectivity replay and forced first-launch setup no longer enter connectivity manager first after successful onboarding. |

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
- `docs/plans/god-tracker.md`
- `docs/plans/god-wave0-execution-brief.md`

## Historical Context and Evidence

- `docs/plans/sim-tracker.md`
- `docs/reports/sim_prototype_family_transplant_notes.md`
