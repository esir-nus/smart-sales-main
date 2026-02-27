# Analyst Orchestrator

> **Cerb-compliant spec** — State machine and execution router for deep analysis.
> **OS Layer**: RAM Application (operates on SessionWorkingSet via ContextBuilder)
> **State**: 🚧 IN PROGRESS

---

## Overview

The `analyst-orchestrator` is the central traffic cop for Analyst Mode. It does not perform the LLM analysis itself; instead, it manages the **Open-Loop Lifecycle**, ensuring the user is always in control before expensive investigations begin.

**Key Principles**:
1. **The Human-in-the-Loop Gate**: The orchestrator absolutely MUST pause at `PROPOSAL` state and wait for explicit user confirmation before executing any investigation plan.
2. **Context, Not Raw SSD Queries**: The orchestrator does not parse LLM output into Kotlin SQL queries. It relies on the Kernel (`ContextBuilder`) to furnish the `EnhancedContext` (RAM), which is then fed back to the LLM for reasoning.
3. **No Dumb Tools**: "Tool execution" in Analyst Mode is actually an LLM Reasoning pass over the RAM context, not a hardcoded Kotlin data fetch. Actionable tools (Export PDF, Email) are strictly deferred to the `TaskBoard` mounted at the end.

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
│  [Idle] ──▶ (User Input)                                   │
│                  │                                         │
│                  ▼                                         │
│         ContextBuilder.build() ── Assembles RAM Snapshot   │
│                  │                                         │
│                  ▼                                         │
│         Phase 1: LLM Evaluates Intent                      │
│                  │                                         │
│          ┌───────┴───────┐                                 │
│          │               │                                 │
│    info_sufficient    info_sufficient                       │
│       = false            = true                            │
│          │               │                                 │
│          ▼               ▼                                 │
│    Ask Clarification   Phase 2: LLM Generates Plan         │
│    (Loop back ↑)         │                                 │
│                          ▼                                 │
│              ┌── THE SMALL LOOP ──┐                        │
              │                    │                        │
              │  Show Strategy     │                        │
              │  "OK to proceed?"  │                        │
              │         │          │                        │
│              │    ┌────┴────┐     │                        │
│              │  Amend    Confirm  │                        │
│              │    │         │     │                        │
│              │    └─(back)──┘     │                        │
│              └────────────────────┘                        │
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
│                       [Idle] ◀── Ready for next request    │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

- `IDLE`: Base state. Awaiting new intent.
- `CONSULTING`: Calling Phase 1 (`ConsultantService`). Evaluates intent, returns `info_sufficient` and `missing_entities`.
  - *Small Loop: If missing entities found, trigger `EntityResolverService` to find exact matches. If ambiguous, stay here, ask user context.*
- `PROPOSAL`: Plan is rendered. Agent waits. **Execution blocked.**
  - *Small Loop: If user amends plan, back to Consulting.*
  - *Break Loop: If user confirms, to Investigating.*
- `INVESTIGATING`: Phase 3 processing. The investigator (`ArchitectService`) is reading the RAM.
- `RESULT`: UI prints analysis. Evaluates and mounts the TaskBoard. Resets to `IDLE` after mounting.

---

## Interaction Rules

| Rule | Enforcer | Explanation |
|------|----------|-------------|
| **No Auto-Execute** | `handleInput` | If state == `PROPOSAL`, input MUST be evaluated as a confirmation before moving to `INVESTIGATING`. |
| **RAM First** | Phase 1 & 3 Calls | Always prompt the LLMs using `ContextBuilder.build()`'s `EnhancedContext`. Assume the Kernel has loaded relevant entities. |
| **Post-Facto TaskBoard** | Output Mapper | Only mount TaskBoard workflows in `RESULT` state, based on LLM suggestions. |
| **Simple Boolean Parsing** | Consultant Parser | Phase 1's `info_sufficient` is parsed with `.optBoolean(.., false)`. Do not build complex Linters for Phase 1. |

---

## Delivery Workflow Registry (The TaskBoard Vault)

The backend maintains a list of pure-Kotlin actionable workflows (The "Hands"). The LLM never executes these—it only recommends them by returning their `workflowId`.

Current recognized Vault IDs:
- `GENERATE_PDF`
- `EXPORT_CSV`
- `DRAFT_EMAIL`

The UI maps these IDs to clickable action buttons, separating reasoning (LLM) from physical execution (Kotlin + Android Intents).

---

## Wave Plan (Fake-First Integration)

Following the Anti-Drift Protocol, the Orchestrator will be built using a **Fake-First** Strategy to de-risk the complex UI state transitions before burning any real LLM tokens.

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Domain & Fakes** | ✅ SHIPPED | `AnalystPipeline`, `AnalystResponse` models, `FakeAnalystPipeline`. L2 UI wiring (TaskBoard + Strategy rendering). |
| **2** | **Phase 1 (Consultant)** | ✅ SHIPPED | Wire `RealAnalystPipeline` for conversational routing. Implement simple boolean parsing for `info_sufficient`. |
| **3** | **Phase 2 (Architect)** | ✅ SHIPPED | Markdown prompts and `PlanResult` to map output to the UI state. |
| **4** | **Phase 3 (Investigation)** | ✅ SHIPPED | Wire the LLM to read the `EnhancedContext` and update the UI states. |
| **5** | **Entity Disambiguation** | ✅ SHIPPED | Implement `AwaitingClarification` loop and lightweight `EntityResolverService` validation. |
| **6** | **Phase 4 (TaskBoard)** | 🔲 PENDING | Parse final suggestions and mount actionable UI buttons. |

---

## Wave 1 Ship Criteria
**Goal**: Interface + Fake that proves the completely open-loop State Machine UI wiring without LLM latency.

- **Exit Criteria**:
  - [x] `AnalystPipeline` interface in `domain/analyst/`
  - [x] `FakeAnalystPipeline` simulating the Small Loop (Chat ↔ Proposal) and the Execution trigger.
  - [x] UI successfully renders a mock `PlannerTable` and pauses for confirmation.
  - [x] Tapping "OK" artificially races through `INVESTIGATING` and prints mock `RESULT` with a TaskBoard.

- **Test Cases (L2 Simulated On-Device)**:
  - [x] L2: User types → Fake returns `info_sufficient = false` → UI renders normal chat.
  - [x] L2: User types → Fake returns `info_sufficient = true` → UI renders `Markdown Strategy Bubble` and stops.
  - [x] L2: User taps "Proceed" → UI fires `INVESTIGATING` state.
