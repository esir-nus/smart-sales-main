# Plugin Registry

> **OS Layer**: App Infrastructure / Hardware Abstraction
> **State**: PARTIAL

---

## Overview

The `plugin-registry` is responsible for formally managing the "Hands" of the AI assistant. While the Orchestrator acts as the brain (System II) that decides *what* to do, the Plugin Registry manages the actual executable Kotlin workflows (Plugins).

Currently, tools are hardcoded in a `FakeToolRegistry` with a synchronous interface. This spec defines the path to a real, extensible Plugin SDK where tools can be dynamically registered, executed asynchronously, and interact with the UI without tightly coupling the core Orchestrator to specific features (like Tingwu or CRM Export).

**Key Principles**:
1. **Separation of Concerns**: The Orchestrator knows *about* tools via `getAllTools()`, but does not know *how* they work.
2. **Safe Execution Boundary**: Plugins must execute purely in Kotlin space. The LLM only maps intents to `toolId`s; it does not write executable code.
3. **Capability Gateway First**: Plugins should consume approved runtime capabilities, not directly wire themselves into RAM loaders, repositories, or UI state machines.
4. **Async Yield Contract**: Real tools take time. The registry may still yield asynchronous `Flow<UiState>` during the transitional shell, but capability calls and permissions must be owned by the OS runtime.

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Interface Contract** | ✅ SHIPPED | Define `PluginRegistry` spec and interface. Document current fake debt. |
| **2** | **Plugin SDK Definition** | ✅ SHIPPED | Create `PrismPlugin` interface and standard `PluginRequest` / `PluginResponse` DTOs. |
| **3** | **Dynamic Registry** | ✅ SHIPPED | Implement `RealToolRegistry`. Use Dagger/Hilt Multibinding (`@IntoSet`) to inject all registered `PrismPlugin` implementations dynamically. |
| **4** | **Async Execution Wiring** | ✅ SHIPPED | Refactor `executeTool` to return or emit asynchronous UI states to prevent thread blocking during long-running tasks. |
| **5** | **Runtime Gateway Foundation** | 🚧 IN PROGRESS | Replace silent/no-op gateways with real runtime capability access, enforce `requiredPermissions`, and deliver the first bounded read-only capability bundle (`READ_SESSION_HISTORY` + progress signaling). |

---

## T3 Narrow Capability Foundation

Wave 21 `T3` does **not** attempt a full plugin rewrite.

It delivers the first real runtime capability gateway with these explicit boundaries:

- **Allowed in T3**
  - bounded session/history read through OS-owned session memory
  - bounded progress signaling through the plugin gateway
  - runtime enforcement of declared plugin permissions
  - real valve coverage for dispatch -> capability call -> yield/failure

- **Deferred to T4+**
  - plugin-caused SSD writes
  - generic write-result contracts
  - broad "query anything" SDK surfaces
  - repo-wide plugin result re-architecture

The first gateway bundle should stay intentionally narrow:

1. `READ_SESSION_HISTORY`
2. progress signaling

History append is not part of the active T3 gateway surface and remains deferred to a later write-capability wave.
