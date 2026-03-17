# Project Mono: Master Architectural Guide

**Status:** Active SOT  
**Successor to:** Prism-V1.md, Lattice Architecture  

> **Preamble**: This is the authoritative framework for the "Project Mono" (Data-Oriented OS) architecture migration. Any agent executing tasks, writing specs, or generating code *must* read this document first.

---

## 1. Architectural Role And Precedence

This document is the **system constitution** of the repo.
It defines:

- the OS mental model
- the allowed runtime layers
- the architectural boundaries
- the canonical data and mutation rules
- the observability protocol
- the lifecycle that lower docs must follow

This document does **not** define feature-specific behavior.
That belongs to `docs/core-flow/**`.

### Repo Hierarchy

For active development, use this chain:

1. **Architecture (`docs/specs/Architecture.md`)**: what kind of system this repo is
2. **Core Flow (`docs/core-flow/**`)**: what a feature must do inside that architecture
3. **Feature Spec (`docs/specs/**`, `docs/cerb/**`)**: how to build it
4. **Code**: delivered implementation
5. **PU / Acceptance**: validation against the Core Flow and architecture

### What Belongs Here

Put only these categories into `Architecture.md`:

- stable architectural laws
- validated implementation patterns that generalize well
- observability and boundary rules
- lifecycle and precedence rules

Do not use this file as a dumping ground for feature-specific branch logic.

---

## 2. The Philosophy & Purpose

### What is Project Mono?
Project Mono resolves the "Brain/Body Disconnect" by fundamentally changing the contract between the AI Engine (The Brain) and the Android Application (The Body). It replaces the legacy, stateful "Mode" coordination of Prism-V1 with a **Dual-Engine CQRS Pipeline** running on an **Operating System (OS) Mental Model**.

### The Core Problem ("Essay Questions")
Previously, the system relied on "Behavioral Contracts." The AI was asked to write an essay (free-form JSON) based on a hardcoded, handwritten string block inside the `PromptCompiler`. If the database schema changed, developers had to manually update the prompt strings and the regex-heavy Linters. This caused "Ghosting" (the AI hallucinating fields the DB didn't support).

### The "One Currency" Rule (The Illusion of Interfaces)
Previously, modules were connected by clean interfaces (e.g., `fun process(json: String)`), but they traded in "multiple currencies" (raw Strings, custom JSON, regex). Because the data was shapeless, the LLM was forced to act as the Currency Exchange, guessing what each module required. This caused Ghosting.
In Project Mono, the architectural rule is:

- **Query lane currency**: verified IDs plus RAM context
- **Mutation lane currency**: strictly typed Kotlin `data class` payloads

More precisely:

- the sync/query side should move from candidate names to verified IDs to RAM context
- the async/mutation side should move from strict schema generation to strict deserialization to SSD write
- user-facing conversational text is not SSD currency and must not be confused with mutation payload

This keeps the LLM from acting as a shapeless currency exchange at the mutation boundary.

---

## 3. Active Runtime Layers

The modern runtime should be understood as these layers:

| Layer | Typical Component | Role |
|------|-------------------|------|
| **Presentation** | `AgentViewModel`, Scheduler UI | Receives `UiState`, renders results, never owns routing law |
| **Phase-0 Gateway** | `IntentOrchestrator`, Lightning Router | Short-circuit noise, greetings, hardware delegation, and route into the right architectural lane |
| **Kernel** | `ContextBuilder` | Owns RAM lifecycle and what enters the Session Working Set |
| **System II Pipeline** | Unified Pipeline | Performs main LLM-assisted reasoning and strict typed mutation decoding |
| **System III Plugins** | Tool / plugin registry, plugin gateways, capability SDK | Executes bounded workflows outside the core reasoning loop through approved developer-facing capabilities |
| **Writers / Repositories** | `EntityWriter`, Room repositories | Persist SSD truth and perform approved write-through updates |

Validated implementation patterns that fit this architecture well:

- `IntentOrchestrator` as the phase-0 gateway
- `ContextBuilder` as the RAM-owning kernel
- `SessionWorkingSet` as bounded RAM
- `EntityWriter` as the centralized mutation path
- background reinforcement listeners as async write-through workers

### 3.1 Plugin SDK / Capability Gateway

Plugins should not be built by manually re-wiring all major modules every time.

The preferred developer model is:

- expose narrow capability APIs based on real plugin needs
- group those capabilities behind a plugin SDK / capability gateway
- let plugins consume the SDK instead of reaching directly into RAM loaders, repositories, RL, or writer internals

Examples:

- `fetchAllBudgetData()`
- `fetchRelevantContacts()`
- `fetchTimelineWindow()`

Architectural rules:

- create APIs from real recurring needs, not from premature abstraction
- prefer narrow capability calls over generic "query anything" surfaces
- plugin reads should go through approved capability APIs
- plugin writes must still hand off into the typed mutation / central writer path when SSD truth changes
- the SDK should carry observability and ownership rules by default so each plugin does not re-implement them

---

## 4. The OS Mental Model

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
| **OS: Kernel** *(Bridge)* | `ContextBuilder` | **The Loader**. Pulls required context from SSD into RAM and owns the Session Working Set lifecycle. |
| **OS: Apps** *(Actors)* | Unified Pipeline, RL Module, bounded helpers | **The Workers**. They operate on RAM and approved typed contracts. |
| **OS: Explorer** *(UI)* | `CRM Hub`, Dashboards | **The Viewer**. Reads SSD directly for historical dashboard views outside a chat session. |

### 4.1 The RAM Context (Session Working Set)

The Kernel (`ContextBuilder`) incrementally builds the RAM into three bounded sections. Applications (like the RL Module or Coach) simply read this canvas; they do not need to wire their own IDs.

1. **Section 1: Distilled Memory** — Resolved entity pointers and active memory graph references.
2. **Section 2: User Habits (Global)** — The user's personal preferences (e.g., "reply in Chinese"). 
3. **Section 3: Client Habits (Contextual)** — Preferences specific to the active entity in Section 1. Evolves automatically as conversation targets shift.

### 4.2 Strict Interaction Rules

1. **Applications Work on RAM**: During a chat session, the LLM Brain and helper modules should read from the Session Working Set rather than inventing their own SSD queries.
2. **Gateway Before Kernel**: The phase-0 gateway may perform lightweight routing and L1 alias/cache checks before full RAM assembly, but heavy world fetch still belongs to the SSD -> RAM path.
3. **Write-Through Persistence**: Any approved mutation to RAM must write through to the SSD in the background. There is no manual "flush" state.
4. **Kernel Owns the RAM**: Applications request logic, but only the Kernel determines what is loaded into the RAM.

---

## 5. The CQRS Dual-Engine Pattern (Master UJM)

To understand Project Mono's "Intelligence," an agent must understand the **Dual-Loop Architecture** during a Chat Session. We strictly decouple the high-speed "Sync/Query" loop from the slow, complex "Async/Command" loops.

### Architectural ASCII Map

```text
================================================================================================
                        SYNC LOOP (Fast Query & RAM Assembly)
================================================================================================
                                      
[User Input] 
      │
      ▼
┌───────────────┐     [NO NAME]     ┌──────────────────┐
│ Lightning     ├──────────────────►│ Clarification    │──► (Halt: Ask User "Who?")
│ Router        │                   │ Minor Loop       │
└───────┬───────┘                   └──────────────────┘
        │
   [CANDIDATE]
        │
        ▼
┌───────────────┐   [MULTIPLE]      ┌──────────────────┐
│ Alias Lib     ├──────────────────►│ Disambiguation   │──► (Halt: Yield Options to User)
│ (L1 Cache)    │   [MISSING]       │ Minor Loop       │
└───────┬───────┘                   └──────────────────┘
        │
    [EXACT ID]
        │
        ▼
┌───────────────┐
│ SSD Graph     │ (Heavy SQL Fetch)
│ Query         │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│ Living RAM    │◄──────────┐ (Injected Context for Future Turns)
│ Assembly      │           │
└───────┬───────┘           │
        │                   │
        ▼                   │
┌───────────────┐           │
│ LLM Brain     │           │
│ Generation    │           │
└───────┬───────┘           │
        │                   │
        ▼                   │
 [Chat Response]            │
                            │
============================│===================================================================
                        ASYNC LOOPS (Background Mutations)
============================│===================================================================
                            │
   [Entity Mutated]         │ 
        │                   │
        ▼                   │
┌───────────────┐           │
│ decodeFromStr │ (Linter)  │
└───────┬───────┘           │
        │                   │
        ▼                   │
┌───────────────┐           │
│ SSD Write /   │───────────┘
│ Entity Merge  │
└───────────────┘

┌───────────────┐
│ RL Module     │ (Listens passively to user input for async habit updates)
└───────────────┘
```

### 5.1 The Sync Loop (Fast RAM Assembly & Query)
This is the main interaction pipeline. Its entire goal is to build context (RAM) from the SSD with zero LLM hallucination and sub-second latency *before* generation begins.

1. **Phase-0 Gateway**: The system first evaluates whether the input should be short-circuited, delegated, clarified, or sent deeper into System II.
2. **Lightning Router (Gatekeeper)**: If the input needs deeper handling, identify candidate intent and candidate entities.
3. **Alias Lib (L1 Cache)**: Given the entity candidate, try fast alias resolution first.
4. **Disambiguation Minor Loop**: If Alias Lib returns multiple hits or unresolved ambiguity, suspend the deeper path and yield to user.
5. **SSD Graph Fetch -> RAM**: Armed with a verified ID, query the heavy SSD and assemble Living RAM.
6. **The Brain Acts (LLM)**: The LLM receives prompt + RAM and produces response and, when needed, strict structured mutation payloads.

### 5.1.1 Minor Loops (Trust-Preserving Gatekeepers)

Inside the major lanes, the system may enter **minor loops**.

A minor loop is:

- a bounded interrupt inside a parent lane
- triggered when certainty is insufficient
- designed to prevent guessing that would damage user trust
- expected to resume the parent lane after the uncertainty is repaired

The most important architecture-level minor loops are:

1. **Clarification Minor Loop**
   - triggered when the system cannot form a valid candidate or is missing required certainty
   - asks the user for the missing piece instead of inventing one

2. **Disambiguation Minor Loop**
   - triggered when multiple valid candidates remain after grounding
   - asks the user to resolve the conflict instead of choosing arbitrarily

These loops are anti-guessing gatekeepers.
They are not signs of failure.
They are trust-preserving behavior.

### 5.1.2 Minor Loop Resume Rule

When a minor loop is entered, the parent lane should follow this pattern:

1. **Suspend** the deeper path
2. **Yield** the clarification or disambiguation request to the user
3. **Receive** the user’s repair input
4. **Resume** the parent lane with the repaired certainty

The system should not treat clarification repair as a totally unrelated fresh task unless the user clearly abandons the prior thread.

### 5.2 The Async Loops (Background Mutations & Commands)
The LLM does NOT directly write to the database in the critical UX path. Writes are asynchronous background events.

1. **Decoupled Entity Writing & Merging**: When the system emits a strict mutation payload, the writer path deserializes it, writes SSD, and merges the result back into RAM via approved write-through.
2. **Decoupled Reinforcement Learning (RL)**: Every user turn may be copied to an async learner that writes habit updates and refreshes RAM without blocking the main path.
3. **Session Memory**: Recent turns may extend RAM directly without requiring full SSD reload every turn.
4. **Plugin / System III Workflows**: Bounded plugins may run asynchronously or semi-independently, but they must still honor typed boundaries, valve observability, OS ownership rules, and the plugin SDK / capability gateway model.

**Takeaway for Agents**: Do not treat the LLM as a monolithic text-to-JSON box. It is a dual-engine reasoning brain. The **Sync Loop** strictly bounds the LLM's reality via the Alias Lib and SSD IDs. The **Async Loop** strictly bounds what the LLM is allowed to mutate via Kotlin Data Classes.

---

## 6. The Pipeline Valve Protocol (Observable Architecture)

In a Data-Oriented OS, debugging requires tracking the data payload (the "Passenger") as it moves across architectural junctions (the "Cities"). We do not rely on shallow, unstructured `Log.d()` statements. We use **Pipeline Valves**.

### The Mental Model: "Google Maps"
- **The Code Functions** are the physical roads and highways (they define where data *can* go).
- **The Data (Payload)** is the actual car driving on the road.
- **The Pipeline Valves (Anchors)** are the GPS checkpoints or toll booths. 

When a bug occurs (e.g., a missing `dateRange`), you do not read 50 files of code. You check the GPS logs to see at which Toll Booth the data was lost.

### The Contract (`PipelineValve`)
Every major checkpoint in the `Core Pipeline` must invoke a standardized logging contract. This is typically implemented via a centralized logger (e.g., `PipelineValve.tag(...)`).

**Mandatory Global Checkpoints:**
1. `[INPUT_RECEIVED]` - The raw text/voice origin entering the system.
2. `[ROUTER_DECISION]` - The classification result leaving the Lightning Router (e.g., `SIMPLE_QA`, `VAGUE`, `INTENT`).
3. `[ALIAS_RESOLUTION]` - The exact `EntityID` resolved by the Alias Lib.
4. `[SSD_GRAPH_FETCHED]` - The payload shape retrieved from the database.
5. `[LIVING_RAM_ASSEMBLED]` - The final context payload handed to the LLM Prompt Compiler.
6. `[LLM_BRAIN_EMISSION]` - The raw JSON string emitted by the LLM.
7. `[LINTER_DECODED]` - The strictly typed Kotlin `data class` parsed from the LLM, ready for UI/DB consumption.

**Path-Specific Or Optional Checkpoints:**

- optimistic Path A parse / optimistic DB write
- plugin dispatch received
- plugin internal routing
- database write executed
- UI state emitted

**Rule for Agents & Developers:**
For any feature traversing the Core Pipeline, if you cannot trace the exact shape of your data through the required checkpoints plus its path-specific checkpoints via a simple log filter (e.g., `adb logcat -s VALVE_PROTOCOL`), the pipeline observability is broken and must be fixed before the feature is marked `SHIPPED`.

---

## 7. The Strict Lifecycle

Every module (Core, Scheduler, CRM, etc.) migrating to Project Mono **MUST** follow this exact lifecycle. Bypassing these steps is an automatic failure.

1. **Architecture First**: Confirm the feature fits the laws in this document.
2. **Core Flow**: If behavior is still being designed, create or update the owning `docs/core-flow/**` document first.
3. **Feature Spec / Cerb Spec**: Write or update the implementation contract in `docs/specs/**` or `docs/cerb/**`.
4. **Interface Map / Tracker**: Update ownership and module-boundary docs if the feature changes them.
5. **Plan**: Generate the implementation plan from the docs, not from guesswork.
6. **Execute**: Write the code, ensuring pure Kotlin `data classes` live in the `:domain` layer without Android imports.
7. **Validation**: The feature must withstand PU / acceptance / E2E validation appropriate to its layer before it is considered shipped.

---

## 8. What to Check (Validation Gates)

When reviewing a Project Mono PR or Plan, verify the following:

- **No Hardcoded Schemas**: If you see `{ "deal_stage": "string" }` hardcoded inside a Prompt string, **Reject it**. It must say `json.dumps(QuoteMutation.model_json_schema())` or the Kotlin equivalent via `kotlinx.serialization`.
- **Domain Purity**: Are the Mutation Data Classes inside pure Kotlin `:domain` modules? If they contain `import android.*`, **Reject it**.
- **Typed Mutation Boundary**: The mutation boundary should terminate in strict typed deserialization. Transitional front-door parsing may exist, but the architectural target is still typed decoding, not ad-hoc JSON forever.
- **Linter Simplicity**: If the Linter contains regex (`Regex("date=.*")`) or manual string-parsing math to figure out the LLM's intent, **Reject it**. It should be centered on `decodeFromString()` and typed post-processing.
- **JSON Coercion Resilience**: All `PrismJson` instances parsing LLM outputs MUST set `coerceInputValues = true`. The LLM frequently hallucinates explicit `null` tokens; if this flag is missing, `kotlinx.serialization` will crash against native Kotlin non-nullable default values (e.g., `classification = "schedulable"`). Do not solve this with regex null-stripping.
- **Defensive Deserialization (Enum Safety)**: Never use standard `enumValueOf<T>()` or `T.valueOf()` when mapping strings from the Room Database to Kotlin Enums. If the DB schema changes, the app will crash instantly. Use the centralized `safeEnumValueOf<T>(value, fallback)` function (`com.smartsales.prism.domain.core.SafeEnum`) to gracefully handle legacy or corrupted DB string variants.
- **Visual Spec Alignment**: "Spec says `最近30天`, code says `最近30天`. No synonyms."
- **Domain vs UI State Decoupling**: Pure Domain Kotlin `data classes` represent the factual SSD truth. They must NEVER be overloaded with UI-specific rendering flags (like `tipsLoading`, `isExpanded`, or `amberGlow`). UI Layer must define its own `UiState` mapping to render Domain reality. Overloading Domain Models with UI flags breaks the Brain/Body disconnect and couples the Database to the View.
- **Central Writer Rule**: SSD mutation should funnel through centralized writer/repository paths rather than scattered direct writes from random architectural layers.

---

## 9. Source of Truth (SOT) Hierarchy

For Project Mono development, resolve conflicts using this hierarchy:

1. **`docs/specs/Architecture.md`** - System constitution and architectural laws
2. **`docs/core-flow/**`** - Feature behavioral north star
3. **Feature specs (`docs/specs/**`, `docs/cerb/**`)** - Implementation contract
4. **The Kotlin `data class` and domain contract** - Concrete typed shape
5. **Tracker / interface map** - Current state and ownership boundaries

---

## 10. User POV & UX Implications

From the user's perspective, Project Mono is completely invisible, but it results in **Zero Ghosting**.

**UX Rule**: Project Mono enforces that the UI only ever attempts to display mathematically validated SSD records. If there is an anomaly, it yields to the User via `UiState.AwaitingClarification`.

---

## 11. Glossary Of Critical Terms

- **Architecture**: the stable system constitution
- **Core Flow**: feature behavior inside the architecture
- **RAM / Session Working Set**: bounded active session context
- **SSD**: persistent world knowledge and durable records
- **Kernel**: the only owner of RAM lifecycle
- **Phase-0 Gateway**: the early routing layer before deep System II work
- **System II Pipeline**: the main reasoning and typed-mutation pipeline
- **System III Plugin**: a bounded workflow outside the core reasoning loop
- **Write-Through**: RAM-visible mutation that is persisted to SSD through approved paths
- **Pipeline Valve**: standardized observability checkpoint for payload tracing
