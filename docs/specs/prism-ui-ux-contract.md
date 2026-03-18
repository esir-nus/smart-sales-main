# Prism UI/UX Contract (The Skin)

> **Status:** Active Index & Policy Rule
> **Last Updated:** 2026-03-18
> **Architecture Companion:** [Architecture.md](./Architecture.md)
> **Terminology:** [GLOSSARY.md](./GLOSSARY.md)

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
| Home Screen | Shipped | [HomeScreen.md](../cerb-ui/agent-intelligence/spec.md) |
| Chat Interface | Shipped | [ChatInterface.md](../cerb-ui/agent-intelligence/spec.md) |
| Scheduler Drawer | Shipped | [SchedulerDrawer.md](../cerb-ui/scheduler/contract.md) |
| History Drawer | Shipped | [HistoryDrawer.md](../cerb/session-history/spec.md) |
| Audio Drawer | Shipped | [AudioDrawer.md](../cerb/audio-management/spec.md) |
| Connectivity Modal | Shipped | [ConnectivityModal.md](../cerb/connectivity-bridge/spec.md) |
| User Center | Shipped | [UserCenter.md](../cerb/user-habit/spec.md) |
| Analyst Mode | In Progress | [AnalystMode.md](../cerb/model-routing/spec.md) |
| Knot FAB | Deferred | [KnotFAB.md](../cerb-ui/agent-intelligence/spec.md) |

### Components

| Component | Status | Spec File |
|-----------|--------|-----------|
| Agent Activity Banner | Shipped | [AgentActivityBanner.md](./components/AgentActivityBanner.md) |

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
