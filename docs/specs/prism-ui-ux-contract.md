# UI Surface Contract (Legacy filename: `prism-ui-ux-contract.md`)

> **Status:** Active Index & Policy Rule
> **Last Updated:** 2026-03-22
> **Architecture Companion:** [Architecture.md](./Architecture.md)
> **Terminology:** [GLOSSARY.md](./GLOSSARY.md)

---

## Working Role In UI Dev Mode

This file remains the compatibility path used across the repo, but its practical role is simple:

- it is the UI surface contract
- it defines the boundary between UI and lower layers
- it points readers toward the feature-level UI source of truth

For the overall UI workflow, also read:

- [`../sops/ui-dev-mode.md`](../sops/ui-dev-mode.md) for the developer operating model
- [`style-guide.md`](./style-guide.md) for visual identity rules
- [`ui_element_registry.md`](./ui_element_registry.md) for shared element behavior and invariants
- feature-specific `docs/cerb-ui/**` or `docs/cerb/**` docs for real screen/state ownership

Rule:

- use intuitive names in discussion
- keep this path stable until the repo deliberately migrates references
- when a standalone mode such as SIM has its own Cerb shard, treat that shard as the visual/source-of-truth layer for that mode's shell composition and support-surface presentation

---

## Brain, Body, and Skin

This document is a policy and index layer for Layer 4 UI work.
Feature-specific UI behavior, state shapes, and visual contracts belong in the owning `docs/cerb-ui/**` or `docs/cerb/**` documents.

To enable parallel UI development without being blocked by backend pipeline restructuring, the UI layer enforces strict decoupling:

1. **The contract is `StateFlow` plus sealed classes**
   The UI layer only observes a `StateFlow<FeatureUiState>` exposed by its ViewModel. The UI does not know about LLM context, `UnifiedMutation`, or database entities.
2. **Fake ViewModels are allowed for parallel UI work**
   UI developers may implement fake or preview ViewModels that emit mocked `UiState` sequences for previews and debug scenario runners while the lower layers are still moving.
3. **No domain spillage**
   UI states represent what the user sees, not raw database rows or domain internals.

Rule: Do not add raw feature state descriptions to this document. The source of truth for feature UI states belongs in the owning feature contract, typically under `docs/cerb-ui/**` for UI-facing contracts or `docs/cerb/**` for feature specs.

---

## Federated Spec Index

Role: This document is strictly an index that maps UI modules, components, and flows to their authoritative specs.

### Modules

| Module | Status | Spec File |
|--------|--------|-----------|
| Home Screen | Shipped | [home-shell/spec.md](../cerb-ui/home-shell/spec.md) |
| Chat Interface | Shipped | [ChatInterface.md](../cerb-ui/agent-intelligence/spec.md) |
| SIM Shell | Shipped | [sim-shell/spec.md](../cerb/sim-shell/spec.md) |
| Scheduler Drawer | Shipped | [SchedulerDrawer.md](../cerb-ui/scheduler/contract.md) |
| History Drawer | Shipped | [HistoryDrawer.md](../cerb/session-history/spec.md) |
| Audio Drawer | Shipped | [AudioDrawer.md](../cerb/audio-management/spec.md) |
| Connectivity Modal | Shipped | [ConnectivityModal.md](../cerb/connectivity-bridge/spec.md) |
| User Center | Shipped | [UserCenter.md](../cerb/user-habit/spec.md) |
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
| **↓ Top → Down** | Pull from top edge | Scheduler Drawer | Calendar + Timeline |
| **↑ Bottom → Up** | Pull from bottom edge | Audio Drawer | Recordings |

### Standalone Mode Note

- SIM is a standalone shell mode with its own surface contract in `docs/cerb/sim-shell/spec.md`.
- Global gestures and shared element rules still apply unless the SIM shard narrows the presentation for standalone use.
- SIM-specific shell chrome, sparse idle layout, and support-surface composition should be documented in the SIM Cerb docs rather than expanded inline here.
