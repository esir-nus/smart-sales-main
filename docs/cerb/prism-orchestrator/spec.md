# Prism Orchestrator (System II)

> **OS Layer**: RAM Application (operates on SessionWorkingSet via ContextBuilder)
> **State**: PARTIAL

---

## Overview

The `prism-orchestrator` is the central traffic cop for the Dual-Engine Architecture. It operates in the persistent chat feed. It replaces the legacy JSON `PlannerTable` and `PROPOSAL` state machine with a completely stateless, chat-first Ambient Tool Stream.

### The Analyze Gateway & Plugin Routing
The Orchestrator acts as a Launchpad for complex tasks:
1. **Phase 0 Intent Routing**: It uses `LightningRouter` to instantly classify input as `NOISE`, `GREETING`, or `DEEP_ANALYSIS`.
2. **System I vs System II**: `NOISE/GREETING` are routed out-of-band to `MascotService`. `DEEP_ANALYSIS` intents natively flow into the `Analyzer` Plugin.
3. **Conversational Tool Discovery (The Ambient Stream)**: Tools are no longer presented as explicit UI buttons. The `Analyzer` outputs pure natural language text (e.g., "I recommend the Executive Report, but I need the budget amount").
4. **Tool Execution**: Once a tool is explicitly requested and all parameters are met, the Orchestrator delegates execution entirely to the `PluginRegistry`.

**Key Principles**:
1. **Stateless Routing**: The orchestrator does not force rigid steps (`PROPOSAL` -> `INVESTIGATING`). It merely routes chat strings to LLMs or Plugins.
2. **Context, Not Raw SSD Queries**: The orchestrator relies on the Kernel (`ContextBuilder`) to furnish the `EnhancedContext` (RAM).
3. **OS Execution Bypass**: The orchestrator collects `Flow<UiState>` from `ToolRegistry.executeTool()` to pass progressive loading states directly to the UI without blocking the backend.

---

## Architecture & Components

```text
┌────────────────────────────────────────────────────────────┐
│ USER INTERFACE                                              │
│ 💬 Chat Stream (Pure Text Flow)                             │
└──────────────────────────┬─────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│ PRISM ORCHESTRATOR (Stateless Router)                       │
│ Routes NOISE → Mascot                                       │
│ Routes ANALYSIS → Analyzer Plugin                           │
└──────────┬───────────────────────────────┬─────────────────┘
           │                               │
       System I                         System II
       (Mascot)                        (Analyzer Plugin)
           │                               │
           ▼                               ▼
┌──────────────────────┐    ┌──────────────────────────────┐
│ MascotService        │    │ LLM: qwen3-max               │
│ (Out-of-band UI)     │    │ (Conversational Tool         │
│                      │    │  Recommendations)            │
└──────────────────────┘    └──────────────▲───────────────┘
                                           │
                                           │
                              ┌────────────┴───────────┐
                              │ ContextBuilder         │ ← The Kernel (OS)
                              │ Returns:               │
                              │ EnhancedContext (RAM)  │
                              └────────────────────────┘
```

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Lightning Routing** | ✅ SHIPPED | Extraction of P0 intent parsing. Split traffic perfectly between NOISE (System I) and ANALYSIS (System II). |
| **2** | **Analyzer Delegation** | 🔲 PLANNED | Ripping out the legacy `AnalystPipeline` state machine and replacing it with a direct call to the `Analyzer` plugin for conversational tool discovery. |
| **3** | **Plugin Execution Wiring** | 🔲 PLANNED | Collecting the async `Flow<UiState>` from `ToolRegistry` and mapping it to the UI's loading states. |
