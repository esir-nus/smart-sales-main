# Plugin Registry

> **OS Layer**: App Infrastructure / Hardware Abstraction
> **State**: SPEC_ONLY

---

## Overview

The `plugin-registry` is responsible for formally managing the "Hands" of the AI assistant. While the Orchestrator acts as the brain (System II) that decides *what* to do, the Plugin Registry manages the actual executable Kotlin workflows (Plugins).

Currently, tools are hardcoded in a `FakeToolRegistry` with a synchronous interface. This spec defines the path to a real, extensible Plugin SDK where tools can be dynamically registered, executed asynchronously, and interact with the UI without tightly coupling the core Orchestrator to specific features (like Tingwu or CRM Export).

**Key Principles**:
1. **Separation of Concerns**: The Orchestrator knows *about* tools via `getAllTools()`, but does not know *how* they work.
2. **Safe Execution Boundary**: Plugins must execute purely in Kotlin space. The LLM only maps intents to `toolId`s; it does not write executable code.
3. **Async UI Contract**: Real tools take time. The registry must eventually support returning asynchronous state flows (`Flow<UiState>`) so the UI can show loading states (e.g., "Exporting PDF...") before the final result.

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Interface Contract** | 🔲 PLANNED | Define `PluginRegistry` spec and interface. Document current fake debt. |
| **2** | **Plugin SDK Definition** | 🔲 PLANNED | Create `PrismPlugin` interface and standard `PluginRequest` / `PluginResponse` DTOs. |
| **3** | **Dynamic Registry** | 🔲 PLANNED | Implement `RealToolRegistry`. Use Dagger/Hilt Multibinding (`@IntoSet`) to inject all registered `PrismPlugin` implementations dynamically. |
| **4** | **Async Execution Wiring** | 🔲 PLANNED | Refactor `executeTool` to return or emit asynchronous UI states to prevent thread blocking during long-running tasks. |
