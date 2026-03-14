# Prism UI/UX Contract (The Skin)

> **Status:** Active Index & Policy Rule
> **Last Updated:** 2026-03-14
> **Architecture Companion:** [Prism-V1.md](./Prism-V1.md)
> **Terminology:** [GLOSSARY.md](./GLOSSARY.md)

---

## 🧠 Brain, 🦾 Body, and 🎭 Skin (Parallel UI Development)

To enable **Parallel UI Development** without being blocked by backend pipeline restructuring (Project Mono), the UI layer (Layer 4) enforces strict decoupling:

1. **The Contract is `StateFlow` + `Sealed Classes`**
   The UI layer ONLY observes a `StateFlow<FeatureUiState>` exposed by its ViewModel. The UI does not know about LLM context, `UnifiedMutation`, or database entities.
2. **Fake ViewModels for Vibe Coding**
   UI developers MUST implement `FakeViewModel` (used in `@Preview` and debug L2 scenario runners) that emit mocked `UiState` sequences. This allows the "Skin" to be fully built, animated, and tested while the "Brain" and "Body" are still under construction.
3. **No Domain Spillage**
   UI states represent purely what the user sees (e.g., `Loading`, `Disambiguating(options)`, `Success(msg)`), not the raw DB rows.

> **Rule:** Never add raw UI state descriptions to this document. The Single Source of Truth for *what* states exist belongs in the feature's `docs/cerb/[feature]/spec.md`.

---

## 📋 Federated Spec Index

> **Role**: This document is strictly an **INDEX** mapping features to their authoritative Cerb specs.

### Modules

| Module | Status | Spec File |
|--------|--------|-----------|
| Home Screen | ✅ Shipped | [HomeScreen.md](../cerb-ui/agent-intelligence/spec.md) |
| Chat Interface | ✅ Shipped | [ChatInterface.md](../cerb-ui/agent-intelligence/spec.md) |
| Scheduler Drawer | ✅ Shipped | [SchedulerDrawer.md](../cerb-ui/scheduler-drawer/spec.md) |
| History Drawer | ✅ Shipped | [HistoryDrawer.md](../cerb/session-history/spec.md) |
| Audio Drawer | ✅ Shipped | [AudioDrawer.md](../cerb/audio-management/spec.md) |
| Connectivity Modal | ✅ Shipped | [ConnectivityModal.md](../cerb/connectivity-bridge/spec.md) |
| User Center | ✅ Shipped | [UserCenter.md](../cerb/user-habit/spec.md) |
| Analyst Mode | 🟡 In Progress | [AnalystMode.md](../cerb/model-routing/spec.md) |
| Knot FAB | 🔲 Deferred | [KnotFAB.md](../cerb-ui/agent-intelligence/spec.md) |

### Components

| Component | Status | Spec File |
|-----------|--------|-----------|
| Agent Activity Banner | ✅ Shipped | [AgentActivityBanner.md](./components/AgentActivityBanner.md) |

### Flows

| Flow | Status | Spec File |
|------|--------|-----------|
| Onboarding | ✅ Shipped | [OnboardingFlow.md](./flows/OnboardingFlow.md) |
| Scheduler | ✅ Shipped | [SchedulerFlows.md](./flows/SchedulerFlows.md) |
| Audio & Entity | ✅ Shipped | [AudioEntityFlows.md](./flows/AudioEntityFlows.md) |

---

## Global Gesture Model

| Direction | Gesture | Opens | Notes |
|-----------|---------|-------|-------|
| **↓ Top → Down** | Pull from top edge | Scheduler Drawer | Calendar + Timeline |
| **↑ Bottom → Up** | Pull from bottom edge | Audio Drawer | Recordings |
