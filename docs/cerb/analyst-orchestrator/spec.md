# Analyst Orchestrator (System II)

> **Cerb-compliant spec** — State machine and execution router for deep analysis.
> **OS Layer**: RAM Application (operates on SessionWorkingSet via ContextBuilder)
> **State**: PARTIAL

---

## Overview

The `analyst-orchestrator` is the central traffic cop for Analyst Mode. It operates as **System II (The Worker)** in the Dual-Engine Architecture. It is stateful, formal, and operates in the persistent chat feed. It does not perform the LLM analysis itself; instead, it manages the **Open-Loop Lifecycle**, ensuring the user is always in control before expensive investigations begin.

### The Analyze Gateway & Plugin Routing
The Orchestrator acts as a Launchpad for complex tasks:
1. **Generic Handoff**: If a user uploads heavy context (e.g., an audio transcript) and says "Analyze", the Orchestrator runs a baseline read and surfaces recommended plugins/tools (e.g., Talk Simulator, Summarizer).
2. **Expert Bypass**: Specific commands ("Give me the PDF report") skip the recommendation phase and execute the tool directly.
3. **Plugin Workflows**: Once a tool is selected, the Orchestrator delegates execution entirely to that plugin's workflow, keeping the core Open-Loop State Machine clean.

**Key Principles**:
1. **The Human-in-the-Loop Gate**: The orchestrator absolutely MUST pause at `PROPOSAL` state and wait for explicit user confirmation before executing any investigation plan.
2. **Context, Not Raw SSD Queries**: The orchestrator does not parse LLM output into Kotlin SQL queries. It relies on the Kernel (`ContextBuilder`) to furnish the `EnhancedContext` (RAM), which is then fed back to the LLM for reasoning.
3. **Phase 4 OS Execution Bypass**: "Tool execution" (e.g., Export PDF, Email) shown on the TaskBoard does NOT route back through the LLM FSM. The UI layer directly invokes the `ToolRegistry` OS-level functions to prevent infinite `CONSULTING` recursion.

---

## Architecture & Components

```text
┌────────────────────────────────────────────────────────────┐
│ USER INTERFACE                                              │
│ 💬 Chat Stream + Markdown Strategy Bubble + Task Board      │
└──────────────────────────┬─────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│ ANALYST ORCHESTRATOR (State Machine)                        │
│ States: Idle → Consulting ↔ Proposal → Investigating →     │
│         Result → Idle (Open Loop)                           │
└──────────┬───────────────────────────────┬─────────────────┘
           │                               │
   Phase 1 & 2                      Phase 3
   (Intent + Plan)                  (Investigation)
           │                               │
           ▼                               ▼
┌──────────────────────┐    ┌──────────────────────────────┐
│ LLM: qwen3-max       │    │ LLM: qwen3-max               │
│ (Both phases use     │    │ (Reads EnhancedContext from   │
│  the same model      │    │  RAM, reasons over it)        │
│  per AnalystMode.md) │    │                              │
└──────────▲───────────┘    └──────────────▲───────────────┘
           │                               │
           └───────────┬───────────────────┘
                       │
              ┌────────┴────────┐
              │ ContextBuilder   │  ← The Kernel (OS)
              │ .build()         │
              │                  │
              │ Returns:         │
              │ EnhancedContext  │  ← The ONLY data source
              └────────┬────────┘
                       │
          ┌────────────┴────────────┐
          │ SessionWorkingSet (RAM)  │
          │ Section 1: Entities     │
          │ Section 2: User Habits  │
          │ Section 3: Client Habits│
          └─────────────────────────┘
```

### 1. AnalystPipeline (The Contract)
The interface exposing `handleInput()` and the reactive `state` Flow.

### 2. State Machine
Controls the strict phasing of the open loop.

```text
┌────────────────────────────────────────────────────────────┐
│                  THE OPEN-LOOP LIFECYCLE                    │
│                                                            │
│  [Idle] ──▶ (Received Intent from PrismOrchestrator)       │
│                  │                                         │
│                  ▼                                         │
│         ContextBuilder.build(FULL)                         │
│                  │                                         │
│                  ▼                                         │
│           Phase 2: Architect Generates Plan                │
│                  │                                         │
│                  ▼                                         │
│            ┌── THE SMALL LOOP ──┐                          │
│            │  Show Strategy     │                          │
│            │  "OK to proceed?"  │                          │
│            │         │          │                          │
│            │    ┌────┴────┐     │                          │
│            │  Amend    Confirm  │                          │
│            │    │         │     │                          │
│            │    └─(back)──┘     │                          │
│            └────────────────────┘                          │
│                          │                                 │
│                     (User: OK)                             │
│                          │                                 │
│                          ▼                                 │
│         Phase 3: LLM Reads EnhancedContext (RAM)           │
│                  Reasons over entity data                   │
│                  Produces analysis                          │
│                          │                                 │
│                          ▼                                 │
│         Deliver: Analysis + Dynamic Task Board             │
│                          │                                 │
│                          ▼                                 │
│                       [Idle] ─┐                            │
│                               │                            │
│         ┌─────────────────────┘                            │
│         │ (User taps TaskBoard item)                       │
│         ▼                                                  │
│ Phase 4: OS Execution Bypass (ToolRegistry)                │
│ → Bypasses handleInput() entirely                          │
│ → Collects Flow<UiState> from ToolRegistry.executeTool()   │
│ → Emits progressive states to UI (e.g. Loading, Result)    │
└────────────────────────────────────────────────────────────┘
```

- `IDLE`: Base state. Awaiting new intent from `PrismOrchestrator` (following Phase 0 routing).
- `PROPOSAL`: Plan is rendered. Agent waits. **Execution blocked.**
  - *Small Loop: If user amends plan, back to generating a new plan.*
  - *Break Loop: If user confirms, to Investigating.*
- `INVESTIGATING`: Phase 3 processing. The investigator (`ArchitectService`) is reading the FULL RAM.
- `RESULT`: UI prints analysis. Evaluates and mounts the TaskBoard. Resets to `IDLE` after mounting.

---

## Interaction Rules

| Rule | Enforcer | Explanation |
|------|----------|-------------|
| **No Auto-Execute** | `handleInput` | If state == `PROPOSAL`, input MUST be evaluated as a confirmation before moving to `INVESTIGATING`. |
| **RAM First** | Phase 2 & 3 Calls | Always prompt the LLMs using `ContextBuilder.build()`'s `EnhancedContext`. Assume the Kernel has loaded relevant entities. |
| **Post-Facto TaskBoard** | Output Mapper | Only mount TaskBoard workflows in `RESULT` state, based on LLM suggestions. |

---

## Delivery Workflow Registry (The TaskBoard Vault)

The backend maintains a list of pure-Kotlin actionable workflows (The "Hands"). The LLM never executes these—it only recommends them by returning their `workflowId`.

Current recognized Vault IDs:
- `GENERATE_PDF`
- `EXPORT_CSV`
- `DRAFT_EMAIL`

The UI maps these IDs to clickable action buttons. **CRITICAL**: Tapping these buttons must call `ToolRegistry.executeTool()` directly. It must NEVER call `AnalystPipeline.handleInput()`.

---

## Wave Plan (Fake-First Integration)

Following the Anti-Drift Protocol, the Orchestrator will be built using a **Fake-First** Strategy to de-risk the complex UI state transitions before burning any real LLM tokens.

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Domain & Fakes** | ✅ SHIPPED | `AnalystPipeline`, `AnalystResponse` models, `FakeAnalystPipeline`. L2 UI wiring (TaskBoard + Strategy rendering). |
| **2** | **Phase 2 (Architect)** | ✅ SHIPPED | Markdown prompts and `PlanResult` to map output to the UI state. |
| **3** | **Phase 3 (Investigation)** | ✅ SHIPPED | Wire the LLM to read the `EnhancedContext` and update the UI states. |
| **4** | **Entity Disambiguation** | ✅ SHIPPED | Implement `AwaitingClarification` loop and lightweight `EntityResolverService` validation. |
| **5** | **Phase 4 (TaskBoard Bypass)** | ✅ SHIPPED | Wire `ToolRegistry.executeTool()` to bypass the LLM FSM and execute native Kotlin workflows. Add `UiState.ExecutingTool` for loading states. |
| **6** | **Analyze Gateway & Plugin Routing** | ✅ SHIPPED | Implement generic handoff, tool recommendation surface, and delegation to tool-specific plugins. |

---

## Wave 1 Ship Criteria
**Goal**: Interface + Fake that proves the completely open-loop State Machine UI wiring without LLM latency.

- **Exit Criteria**:
  - [x] `AnalystPipeline` interface in `domain/analyst/`
  - [x] `FakeAnalystPipeline` simulating the Small Loop (Chat ↔ Proposal) and the Execution trigger.
  - [x] UI successfully renders a mock `PlannerTable` and pauses for confirmation.
  - [x] Tapping "OK" artificially races through `INVESTIGATING` and prints mock `RESULT` with a TaskBoard.

- **Test Cases (L2 Simulated On-Device)**:
  - [x] L2: User types → Fake (simulating Phase 0 routing) returns Chat Bubble → UI renders normal chat.
  - [x] L2: User types → Fake (simulating Phase 0 routing) returns Plan → UI renders `Markdown Strategy Bubble` and stops.
  - [x] L2: User taps "Proceed" → UI fires `INVESTIGATING` state.
