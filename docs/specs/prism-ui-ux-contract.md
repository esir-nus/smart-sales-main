# Prism UI/UX Contract

> [!WARNING]
> **⚠️ DEPRECATED - REFERENCE ONLY**
> Do not use this document for implementation. The Single Source of Truth for all UI/UX states has been migrated directly into the respective `docs/cerb/[feature]/spec.md` files to prevent drift.

> **Status:** Deprecated (See Cerb Specs)  
> **Last Updated:** 2026-03-10  
> **Architecture Companion:** [Prism-V1.md](./Prism-V1.md)  
> **Terminology**: [GLOSSARY.md](./GLOSSARY.md)  
> **Supersedes:** ux-contract.md, ux-experience.md, ui-ux-contract-v2.md

---

## 📋 Federated Spec Index

> **Role**: This document is now an **INDEX** and status tracker. Detailed specs live in modular files.

### Modules

| Module | Status | Spec File |
|--------|--------|-----------|
| Home Screen | ✅ Shipped | [HomeScreen.md](../cerb-ui/agent-intelligence/spec.md) |
| Chat Interface | ✅ Shipped | [ChatInterface.md](../cerb-ui/agent-intelligence/spec.md) |
| Scheduler Drawer | ✅ Shipped | [SchedulerDrawer.md](../cerb/scheduler/spec.md) |
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

## Global Invariants (Kept in Index)

### Global Gesture Model

| Direction | Gesture | Opens | Notes |
|-----------|---------|-------|-------|
| **↓ Top → Down** | Pull from top edge | Scheduler Drawer | Calendar + Timeline |
| **↑ Bottom → Up** | Pull from bottom edge | Audio Drawer | Recordings |

---

## §1. Interface Blueprints

> **Navigation**: Use the INDEX above to find modular specs. Sections below are legacy copies retained for reference.

### 1.1 Home Screen

> 📎 **Modularized**: Authoritative spec → [HomeScreen.md](../cerb-ui/agent-intelligence/spec.md)


---




### 1.2 Chat Interface

> 📎 **Modularized**: Authoritative spec → [ChatInterface.md](../cerb-ui/agent-intelligence/spec.md)

---

### 1.3 Scheduler Drawer

> 📎 **Modularized**: Authoritative spec → [SchedulerDrawer.md](../cerb/scheduler/spec.md)



---
### 1.4 History Drawer

> 📎 **Modularized**: Authoritative spec → [HistoryDrawer.md](../cerb/session-history/spec.md)

---
### 1.5 Knot FAB

> 📎 **Modularized**: Authoritative spec → [KnotFAB.md](../cerb-ui/agent-intelligence/spec.md)

### 1.6 Onboarding Flow

> 📎 **Modularized**: Authoritative spec → [OnboardingFlow.md](./flows/OnboardingFlow.md)

### 1.6.1 Connectivity Modal

> 📎 **Modularized**: Authoritative spec → [ConnectivityModal.md](../cerb/connectivity-bridge/spec.md)
---

### 1.7 User Center

> 📎 **Modularized**: Authoritative spec → [UserCenter.md](../cerb/user-habit/spec.md)
### 1.8 Audio Drawer

> 📎 **Modularized**: Authoritative spec → [AudioDrawer.md](../cerb/audio-management/spec.md)

## §3. User Flows

### Scheduler Flows

> 📎 **Modularized**: Authoritative spec → [SchedulerFlows.md](./flows/SchedulerFlows.md)

### Audio & Entity Flows

> 📎 **Modularized**: Authoritative spec → [AudioEntityFlows.md](./flows/AudioEntityFlows.md)

---

## §4 System Notifications (Snackbar)

> **Philosophy**: Transparent system status updates without blocking the user flow.
> **Behavior**: Appears at bottom (above keyboard/nav), auto-dismisses after 4s.

| Notification Type | Trigger Condition | Content Format |
|-------------------|-------------------|----------------|
| **Clarification Needed** | Ambiguous entity (low confidence score) | `⚠️ 需要澄清: [Entity Name] (歧义请求)` |
| **Conflict Spotted** | Schedule overlap detected | `⚠️ 发现冲突: [Event A] vs [Event B]` |
| **Relevancy Found** | Background library match | `✨ 发现关联: [Entity Name] (置信度: [Score]%)` |
| **Client Info Updated** | Profile enrichment via Relevancy Lib | `👤 客户信息更新: [Name] (新增偏好/职位...)` |
| **Knowledge Added** | New fact learned from context | `📚 知识库扩充: [Topic] (来源: [Source])` |
| **Relationship Change** | Entity connection detected | `🔗 关系网变动: [Person A] -> [Person B]` |
| **Refreshed Memory** | User profile/context updated | `🧠 记忆已更新: [Key Information]` |
| **Tingwu Complete** | Transcription finished in background | `📝 转写完成: [File Name] (点击查看)` |
| **Task Created** | Voice command processed | `✅ 日程已创建: [Task Name] [Time]` |

**Visual Style:**
*   **Conflict/Warning**: Amber/Yellow accent.
*   **Relevancy/Info**: Prism Blue/Purple accent.
*   **Action**: Tapping the snackbar jumps to the relevant card or context.

---

## §5 Prism Spec Validation Matrix

| Flow ID | Prism Section | Component/Mechanic | Status |
|---------|---------------|--------------------|--------|
| **3.1** | §4.5 | Plan-Once Execution (Voice Intent) | ✅ |
| **3.3** | §3.2 | Combined Context (Multi-select) | ✅ |
| **3.4** | §4.7 | Conflict Resolution (Rethink) | ✅ |
| **3.9** | §2.3 | LLM Check A (Analyst Suggestion) | ✅ |
| **3.11**| §4.6 | Planner-Centric (Plan Card Persistence) | ✅ |
| **3.15**| §5.0 | Incremental Learning (Relevancy Lib) | ✅ |
| **1.3** | §4.7 | Conflict Card UI | ✅ |
| **1.4** | §3.1 | Shared Buffer (Audio Transcript) | ✅ |

---

> **End of Contract V2 Specification**
