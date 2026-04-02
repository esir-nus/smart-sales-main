# UI Surface Contract (Legacy filename: `prism-ui-ux-contract.md`)

> **Status:** Active Boundary Index
> **Last Updated:** 2026-04-01
> **Product North Star:** [`SmartSales_PRD.md`](../../SmartSales_PRD.md)
> **UX Surface North Star:** [`docs/core-flow/base-runtime-ux-surface-governance-flow.md`](../core-flow/base-runtime-ux-surface-governance-flow.md)
> **Architecture Companion:** [Architecture.md](./Architecture.md)
> **Terminology:** [GLOSSARY.md](./GLOSSARY.md)

---

## Working Role In UI Dev Mode

This file remains the compatibility path used across the repo, but its practical role is simple:

- it defines the boundary between UI and lower layers
- it points readers toward the active UX/core-flow and feature-level UI source of truth
- it keeps the long-lived compatibility path stable while authority routes elsewhere

For the overall UI workflow, also read:

- [`SmartSales_PRD.md`](../../SmartSales_PRD.md) for app identity, major surfaces, and product-level UX laws
- [`../sops/ui-dev-mode.md`](../sops/ui-dev-mode.md) for the developer operating model
- [`../core-flow/base-runtime-ux-surface-governance-flow.md`](../core-flow/base-runtime-ux-surface-governance-flow.md) for current shared UX surface behavior
- [`code-structure-contract.md`](./code-structure-contract.md) for file-shape and anti-god-file rules
- [`style-guide.md`](./style-guide.md) for visual identity rules
- [`ui_element_registry.md`](./ui_element_registry.md) for shared supporting invariants
- feature-specific `docs/cerb-ui/**` or `docs/cerb/**` docs for real screen/state ownership

Rule:

- use intuitive names in discussion
- keep this path stable until the repo deliberately migrates references
- product-level shell identity and anti-drift UX laws start in `SmartSales_PRD.md`
- treat this document as a supporting boundary/index layer rather than the primary UX owner
- route shared shell/UI ownership through `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, then the relevant lower `docs/core-flow/**`, `docs/cerb-ui/**`, and `docs/cerb/**` docs
- legacy SIM shell docs are historical migration evidence only and must not be used as shell fallback ownership
- the shared top safe-area law lives in `style-guide.md` and applies repo-wide unless an owning spec explicitly defines a top header/monolith exception

---

## Brain, Body, and Skin

This document is a policy and index layer for Layer 4 UI work.
Surface composition and shared UX interaction behavior belong first in `docs/core-flow/base-runtime-ux-surface-governance-flow.md`.
Feature-specific UI behavior, state shapes, and visual contracts belong in the owning `docs/cerb-ui/**` or `docs/cerb/**` documents beneath that UX layer.

To enable parallel UI development without being blocked by backend pipeline restructuring, the UI layer enforces strict decoupling:

1. **The contract is `StateFlow` plus sealed classes**
   The UI layer only observes a `StateFlow<FeatureUiState>` exposed by its ViewModel. The UI does not know about LLM context, `UnifiedMutation`, or database entities.
2. **Fake ViewModels are allowed for parallel UI work**
   UI developers may implement fake or preview ViewModels that emit mocked `UiState` sequences for previews and debug scenario runners while the lower layers are still moving.
3. **No domain spillage**
   UI states represent what the user sees, not raw database rows or domain internals.

Rule: Do not add raw feature state descriptions to this document. The source of truth for feature UI states belongs in the owning feature contract, typically under `docs/cerb-ui/**` for UI-facing contracts or `docs/cerb/**` for feature specs.

Structure note:

- this document owns the UI boundary and source-of-truth index
- `docs/core-flow/base-runtime-ux-surface-governance-flow.md` owns current shared base-runtime UX surface behavior
- `code-structure-contract.md` owns file-shape legality and anti-god-file policy

---

## Shell & Layout Constraints

> **System Status Bar Presumption**: All prototype designs MUST presume a native system status bar (clock, signal, battery, notifications) exists above the app's top monolith. Prototypes reserve `44px` at the top of the emulator frame for this bar. In production Compose, use `WindowInsets.statusBars` for the actual dynamic inset.

### Compose Transplant: Edge-to-Edge Insets

The `44px` prototype value is a visual approximation only. Real Android status bar height varies by device (24dp–52dp, notches, punch-holes). Never hardcode it.

**Activity setup (once):**
```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
enableEdgeToEdge()
```

**Per-surface inset consumption:**

| Surface | Prototype Pattern | Compose Pattern |
|---------|-------------------|-----------------|
| Top monolith | `top: var(--status-bar-height)` | Monolith/header surface consumes `WindowInsets.statusBars` itself before laying out header content |
| Side drawers (History, User Center) | Ad-hoc `padding-top: 48px` | Each drawer independently consumes `WindowInsets.statusBars` and keeps the blank top safe band when the owning spec requires it |
| Bottom monolith | N/A | `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` |
| Audio drawer (bottom-up) | `bottom: 30px` | `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` |
| Scheduler drawer (top-down) | `top: 64px` | If the owning shell has a persistent top monolith, keep that shell-owned alignment and add real status-bar awareness there rather than replacing it with a raw top inset |

**Legal top-edge patterns:**

- Default top-safe-band surfaces: `status-bar inset -> 16dp blank band -> first visible content`
- Monolith-aligned exception surfaces: real status-bar inset plus the owning shell's preserved top-monolith/header alignment

**Anti-patterns:**

| Do Not | Do Instead |
|--------|------------|
| `Modifier.padding(top = 44.dp)` | `Modifier.windowInsetsPadding(WindowInsets.statusBars)` |
| Hide inset ownership at the shell root | Let each independent surface own its own status or navigation bar inset |
| Apply root and child top insets blindly | Keep inset ownership local and avoid double-padding |
| Assume no notch/cutout | Check `WindowInsets.displayCutout` where content reaches notch |
| Add insets inside `Scaffold` that already handles them | Verify Scaffold's `contentWindowInsets` first |

---

## Federated Spec Index

Role: This document is strictly a supporting index that maps UI modules, components, and flows to their authoritative specs beneath the UX surface north star.

### Modules

| Module | Status | Spec File |
|--------|--------|-----------|
| Home Screen | Shipped | [home-shell/spec.md](../cerb-ui/home-shell/spec.md) |
| Chat Interface | Shipped | [ChatInterface.md](../cerb-ui/agent-intelligence/spec.md) |
| Shared Shell / Base Runtime Shell | Shipped | [../core-flow/base-runtime-ux-surface-governance-flow.md](../core-flow/base-runtime-ux-surface-governance-flow.md) plus [base-runtime-unification.md](./base-runtime-unification.md) |
| Scheduler Drawer | Shipped | [SchedulerDrawer.md](../cerb-ui/scheduler/contract.md) |
| History Drawer | Shipped | [HistoryDrawer.md](../cerb/session-history/spec.md) |
| Audio Drawer | Shipped | [AudioDrawer.md](../cerb/audio-management/spec.md) |
| Connectivity Modal | Shipped | [ConnectivityModal.md](../cerb/connectivity-bridge/spec.md) |
| User Center | Shipped | [UserCenter.md](./modules/UserCenter.md) |
| Analyst Mode | In Progress | [AnalystMode.md](../cerb/model-routing/spec.md) |

### Components

| Component | Status | Spec File |
|-----------|--------|-----------|
| Agent Activity Banner | Shipped | [AgentActivityBanner.md](./components/AgentActivityBanner.md) |
| Dynamic Island | Shipped | [dynamic-island/spec.md](../cerb-ui/dynamic-island/spec.md) |

### Flows

| Flow | Status | Spec File |
|------|--------|-----------|
| Onboarding | Shipped | [OnboardingFlow.md](./flows/OnboardingFlow.md) |
| Scheduler | Shipped | [SchedulerFlows.md](./flows/SchedulerFlows.md) |
| Audio & Entity | Shipped | [AudioEntityFlows.md](./flows/AudioEntityFlows.md) |

---

## Global Gesture Model

| Direction | Gesture | Opens | Notes |
|-----------|---------|-------|-------|
| **↓ Top → Down** | Pull down from dynamic island | Scheduler Drawer | Shipped SIM opener is the dynamic-island hit target, not the full header band; center content stays scroll-safe |
| **↑ Bottom → Up** | Pull from bottom activation band | Audio Drawer | Bottom-edge strip only; shipped SIM strip is 28dp tall and must not overlap the composer hit area |

### Standalone Mode Note

- SIM remains a real standalone implementation boundary, but it is no longer a second non-Mono UI truth separate from the shared base runtime.
- current `SIM` naming in shell/UI docs should be read as the best available base-runtime baseline rather than as permission for a second product truth.
- Shared shell/UI truth now routes through `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, `docs/specs/base-runtime-unification.md`, relevant lower core-flow docs, and shared feature docs.
- current production root code is `MainActivity -> RuntimeShell`
- Global gestures and shared element rules still apply unless a current shared spec explicitly narrows the presentation.
- Legacy SIM shell docs may still hold migration residue, but new shell truth should be absorbed into shared docs rather than extended there.
- For shipped shell behavior, the lower audio-open gesture must yield to direct composer interaction; shell gesture surfaces must never screen the attach button, text field, or send button.
