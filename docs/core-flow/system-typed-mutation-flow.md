# Core Flow: System Typed Mutation Write-Through

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Architecture -> Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: The architecture-level mutation lane from strict structured emission through typed decode, centralized writing, write-through RAM update, and committed state.
> **Parent Constitution**: `docs/specs/Architecture.md`
> **Testing Directive**: Validate one mutation universe or one failure branch per run. Do not mix multiple mutation kinds in a single PU test.

---

## How To Read This Doc

This document defines **what the system mutation lane must do**.

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as transitional first unless the architecture itself is obsolete.
- Feature mutation flows may specialize this behavior, but they must remain inside these laws.

This file is allowed to be ahead of the codebase.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Feature Core Flow** | Feature mutation behavior | Scheduler Path A writes, CRM profile updates, future plugin-owned writes |
| **Spec** | How the mutation lane is encoded | unified-mutation specs, writer specs, confirmation specs, repository specs |
| **Code** | Delivered behavior | unified pipeline decode path, entity writer, repositories, write-through hooks |
| **PU Test** | Behavioral validation | decode tests, write-through tests, failure-safe tests, valve audits |

---

## Non-Negotiable Invariants

1. **Structured mutation is typed**: Mutation payload must terminate in strict typed deserialization, not free-form guessing.
2. **Invalid payload fails safely**: If the payload cannot decode into the contract, the system must halt safely before touching SSD truth.
3. **Writers are centralized**: SSD mutation must flow through approved writer or repository paths, not scattered direct writes from arbitrary layers.
4. **Write-through preserves RAM coherence**: If SSD truth changes for the active session, RAM-visible state should be refreshed through approved write-through behavior.
5. **Mutation and conversation are separate currencies**: Conversational text is not itself permission to mutate SSD.
6. **Proposal and commit may differ**: Some flows may yield a proposal first and commit later, but both phases must stay typed and observable.
7. **Feature-owned writes stay in their lane**: Scheduler writes, CRM writes, plugin writes, and RL writes may differ operationally, but all must honor typed boundaries and central ownership.
8. **Presentation is downstream of committed truth**: UI state may reflect a committed change, but UI representation is not the mutation truth itself.
9. **The lane is observable**: The brain emission, decode, write, and resulting state transition must be traceable.

---

## Canonical Valves

- `LLM_BRAIN_EMISSION`
- `LINTER_DECODED`
- `DB_WRITE_EXECUTED`
- `UI_STATE_EMITTED`

---

## Master Routing Flow

```text
                 +----------------------+
                 | System II Brain      |
                 | Emits Structured     |
                 | Mutation Payload     |
                 +----------+-----------+
                            |
                            v
                 +----------------------+
                 | Strict Typed Decode  |
                 +----------+-----------+
                            |
              +-------------+-------------+
              |                           |
              v                           v
      [Decode Failure]             [Decode Success]
              |                           |
              v                           v
      [Safe Halt / Reply]        +----------------------+
                                 | Route To Owning      |
                                 | Writer / Repository  |
                                 +----------+-----------+
                                            |
                           +----------------+----------------+
                           |                                 |
                           v                                 v
                  [Proposal First]                  [Immediate Commit]
                           |                                 |
                           v                                 v
                 [Confirm / Auto-Commit]          +----------------------+
                                                  | SSD Write            |
                                                  +----------+-----------+
                                                             |
                                                             v
                                                  +----------------------+
                                                  | RAM Write-Through    |
                                                  | / Refresh            |
                                                  +----------+-----------+
                                                             |
                                                             v
                                                  +----------------------+
                                                  | UI / State Visible   |
                                                  +----------------------+
```

---

## Core Universes

### Uni-M1: Immediate Centralized Commit
**Scenario**: A valid typed mutation is routed directly into its owning writer path.

```text
[Brain Output] Valid structured mutation payload
      |
      v
+----------------------+
| Brain Emission       | --> Structured payload emitted
+----------+-----------+
  [Valve: LLM_BRAIN_EMISSION]
           |
           v
+----------------------+
| Strict Decode        | --> Payload decoded into typed data class
+----------+-----------+
  [Valve: LINTER_DECODED]
           |
           v
+----------------------+
| Central Writer       | --> Owning writer/repository accepts the mutation
+----------+-----------+
           |
           v
+----------------------+
| SSD Write            | --> Truth is committed
+----------+-----------+
  [Valve: DB_WRITE_EXECUTED]
           |
           v
+----------------------+
| RAM Refresh          | --> Active session sees the committed change
+----------+-----------+
           |
           v
+----------------------+
| UI State Emitted     | --> User-visible state reflects committed truth
+----------------------+
  [Valve: UI_STATE_EMITTED]
```

---

### Uni-M2: Proposal Then Commit
**Scenario**: The system yields a structured proposal first, then commits after confirmation or approved auto-commit.

```text
[Brain Output] Valid structured mutation payload
      |
      v
+----------------------+
| Brain Emission       | --> Structured payload emitted
+----------+-----------+
  [Valve: LLM_BRAIN_EMISSION]
           |
           v
+----------------------+
| Strict Decode        | --> Payload decoded into typed proposal
+----------+-----------+
  [Valve: LINTER_DECODED]
           |
           v
+----------------------+
| Proposal Yield       | --> User or approved runtime sees commit-ready proposal
+----------+-----------+
           |
           v
+----------------------+
| Confirm / Auto-Commit| --> Commit decision occurs in approved lane
+----------+-----------+
           |
           v
+----------------------+
| Central Writer       | --> Mutation is committed through owning path
+----------+-----------+
           |
           v
+----------------------+
| SSD + RAM Sync       | --> Truth and active session converge
+----------------------+
  [Valve: DB_WRITE_EXECUTED]
```

---

### Uni-M3: Decode Failure Safe Halt
**Scenario**: The payload cannot be decoded into the typed mutation contract.

```text
[Brain Output] Invalid or malformed mutation payload
      |
      v
+----------------------+
| Brain Emission       | --> Payload emitted
+----------+-----------+
  [Valve: LLM_BRAIN_EMISSION]
           |
           v
+----------------------+
| Strict Decode        | --> Decode fails
+----------+-----------+
           |
           v
+----------------------+
| Safe Halt            | --> No SSD mutation is allowed
+----------+-----------+
           |
           v
+----------------------+
| UI State Emitted     | --> User gets failure-safe response
+----------------------+
  [Valve: UI_STATE_EMITTED]
```

---

## What Must Sync Next

1. mutation and writer specs
   - unified mutation contract
   - central writer ownership
   - confirmation vs commit rules

2. feature core-flow docs
   - scheduler Path A write flow
   - CRM/profile update flow
   - plugin-owned write flows

3. code and tests
   - ensure all major mutation paths terminate in strict typed decode
   - ensure decode failure blocks writes
   - ensure active-session write-through remains coherent
