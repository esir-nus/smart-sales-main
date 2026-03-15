# Project Mono Architecture: The OS Mental Model

**Status:** Active SOT  
**Successor to:** Prism-V1.md, Lattice Architecture  

---

## 1. Executive Summary

**Project Mono** is the architectural standard for the Intelligent Personal Assistant system. It replaces the legacy, stateful "Mode" coordination of Prism-V1 with a **Dual-Engine CQRS Pipeline** running on an **Operating System (OS) Mental Model**.

The core philosophy resolves the "Brain/Body Disconnect" by enforcing the **One Currency Rule**: The LLM Engine and the Android Application communicate exclusively via strictly typed Kotlin `data classes`. Untyped JSON strings and regex parsers are forbidden.

---

## 2. The OS Mental Model

Prism's memory and data flow operate like computer hardware. This ensures strict separation between transient session state and persistent world knowledge.

```
┌─────────────────────────────────────────────────────────────────┐
│                    THE OS MENTAL MODEL                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────┐       ┌─────────────────┐                 │
│   │    OS: RAM      │◄──────│    OS: SSD      │                 │
│   │ (Active Session)│ Load  │ (Knowledge Base)│                 │
│   └────────┬────────┘       └─────────────────┘                 │
│            │                         ▲                          │
│            │ Write-Through (Async)   │                          │
│            └─────────────────────────┘                          │
│                                                                 │
│   + OS: Kernel (ContextBuilder, Loader)                         │
│   + OS: Apps   (RL Module, Executor, Readers)                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

| OS Concept | Component Path | Responsibility |
|------------|----------------|----------------|
| **OS: RAM** *(Session)* | `:domain:session` | **The Workspace**. Ephemeral, session-scoped context. All modules operate here during a turn. |
| **OS: SSD** *(Knowledge)* | `:domain:crm`, `:data:*` | **The Persistent Truth**. Room DB storage for entities, habits, and entry logs. |
| **OS: Kernel** *(Bridge)* | `ContextBuilder` | **The Loader**. Pulls required context from SSD into RAM at session start. |
| **OS: Apps** *(Actors)* | `RL Module`, `Executor` | **The Workers**. Read and write *exclusively* to RAM. They never query the SSD directly. |
| **OS: Explorer** *(UI)* | `CRM Hub`, Dashboards | **The Viewer**. Reads SSD directly for historical dashboard views outside a chat session. |

### 2.1 The RAM Context (Session Working Set)

The Kernel (`ContextBuilder`) incrementally builds the RAM into three bounded sections. Applications (like the RL Module or Coach) simply read this canvas; they do not need to wire their own IDs.

1. **Section 1: Distilled Memory** — Resolved entity pointers and active memory graph references.
2. **Section 2: User Habits (Global)** — The user's personal preferences (e.g., "reply in Chinese"). 
3. **Section 3: Client Habits (Contextual)** — Preferences specific to the active entity in Section 1. Evolves automatically as conversation targets shift.

### 2.2 Strict Interaction Rules

1. **Applications Work on RAM**: During a chat session, the LLM Brain and helper modules read from the Session Working Set. They do not query the SSD.
2. **Write-Through Persistence**: Any mutation to the RAM immediately writes through to the SSD (Room DB) in the background. There is no manual "flush" or "save" state.
3. **Kernel Owns the RAM**: Applications request logic, but only the Kernel determines what is loaded into the RAM.

---

## 3. The Dual-Engine Pipeline (CQRS)

The pipeline separates high-speed user conversational queries from slow, complex database mutations.

```
┌───────────────────────────────────────────────────────────────┐
│                 DUAL-ENGINE CQRS PIPELINE                     │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│   [ User Input ]                                              │
│         │                                                     │
│   ┌─────┴──────────┐                                          │
│   │Lightning Router│ (Gatekeeper)                             │
│   └─────┬──────────┘                                          │
│         │                                                     │
│         ├──▶ [ Sync Loop ]    ──(Fast Query)──▶ [ UI Stream ] │
│         │       (Kernel Load + LLM Markdown)                  │
│         │                                                     │
│         └──▶ [ Async Loop ]   ──(Mutation)────▶ [ OS: SSD ]   │
│                 (JSON + Linter + DB Write)                    │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

### A. The Sync Loop (Fast Query)
Optimized for sub-second latency and zero hallucination.
1. **Lightning Router**: Gatekeeper that evaluates intent. Drops the old "Manual Coach/Analyst" toggles.
2. **Alias Lib (L1 Cache)**: Instantly resolves named entities (e.g., "张总" -> `z-001`).
3. **Kernel Load**: Pulls the entity graph from SSD to RAM.
4. **LLM Generation**: Streams pure conversational Markdown directly to the UI. **No mixed JSON/Markdown**.

### B. The Async Loop (Background Command)
Handles LLM-driven state mutations without blocking the UI.
1. **Data Class Emission**: LLM determines a mutation occurred and emits pure JSON matching a target schema.
2. **The Linter (`decodeFromString`)**: Strict deserialization. If it fails, the mutation halts without crashing the app.
3. **Memory Writer**: Writes the validated Kotlin `data class` to the OS: SSD and updates the RAM.

---

## 4. The "One Currency" Contract

To prevent LLM ghosting and schema drift, we enforce strict boundary definitions.

1. **No Hardcoded Prompts**: You may not hardcode a JSON schema in a prompt string. You must generate the schema programmatically from the Kotlin `data class` (e.g., via `kotlinx.serialization` or reflection).
2. **Defensive Deserialization**: Always use `safeEnumValueOf<T>(value, fallback)` when mapping strings from the Room Database to Kotlin Enums. Never use standard `enumValueOf()`.
3. **Domain Purity**: Data classes defining the contract must live in the `:domain` layer. They cannot contain `import android.*` or UI rendering flags (`isLoading`, `isExpanded`).

---

## 5. Architectural Sources of Truth (Cerb)

This document provides the high-level OS Model. For explicit implementation boundaries, refer to the following Living SOTs:

- **[interface-map.md](./../cerb/interface-map.md)**: The strict ownership table defining which module owns which data and interface edges.
- **[pipeline-valves.md](./../plans/telemetry/pipeline-valves.md)**: The Observability Protocol defining how telemetry and state changes are surfaced invisibly via unified Pipeline flow triggers.
- **[prism-ui-ux-contract.md](./prism-ui-ux-contract.md)**: Layouts, explicit Component Registry, and UI/UX state matrix.

> **Extensibility**: When creating a new feature, do not bolt it onto the monolith. It is an "App" that reads from the `OS: RAM`. Define its contract in the Interface Map, ensure its data class is in `:domain`, and rely on the Kernel for context.
