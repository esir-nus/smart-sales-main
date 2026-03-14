# Project Mono: Master Architectural Guide

> **Preamble**: This is the authoritative framework for the "Project Mono" (Data-Oriented OS) architecture migration. Any agent executing tasks, writing specs, or generating code *must* read this document first.

---

## 1. The Philosophy & Purpose

### What is Project Mono?
Project Mono resolves the "Brain/Body Disconnect" by fundamentally changing the contract between the AI Engine (The Brain) and the Android Application (The Body). 

### The Core Problem ("Essay Questions")
Previously, the system relied on "Behavioral Contracts." The AI was asked to write an essay (free-form JSON) based on a hardcoded, handwritten string block inside the `PromptCompiler`. If the database schema changed, developers had to manually update the prompt strings and the regex-heavy Linters. This caused "Ghosting" (the AI hallucinating fields the DB didn't support).

- **The Linter**: The Linter is reduced to a pure 1-line Deserializer. If it doesn't match the Kotlin object, it crashes safely before touching the disk.

### The "One Currency" Rule (The Illusion of Interfaces)
Previously, modules were connected by clean interfaces (e.g., `fun process(json: String)`), but they traded in "multiple currencies" (raw Strings, custom JSON, regex). Because the data was shapeless, the LLM was forced to act as the Currency Exchange, guessing what each module required. This caused Ghosting (the LLM hallucinated fields a downstream module couldn't parse).
In Project Mono, **there is only one currency**: The strictly typed SSD Kotlin `data class`. 
- **Module B (Database)** defines the currency.
- **Module A (Prompt/Brain)** is forced to use that exact currency via schema generation.
- **The Linter (Teller)** verifies the currency via strict deserialization. This eliminates hallucination at the boundary.

---

## 2. The Strict Cerb Lifecycle

Every module (Core, Scheduler, CRM, etc.) migrating to Project Mono **MUST** follow this exact lifecycle. Bypassing these steps is an automatic failure.

1. **Docs/Specs**: The feature spec (`docs/cerb/[feature]/spec.md`) must be written/updated defining the exact Data Contract (the fields that mutate).
2. **Interface Map**: `docs/cerb/interface-map.md` must be updated to denote the new API boundary.
3. **Plan**: Execute the `/feature-dev-planner` workflow to generate the implementation plan based strictly on the Docs.
4. **Execute**: Write the code, ensuring pure Kotlin `data classes` live in the `:domain` layer without Android imports.
5. **E2E Test**: The migration must withstand a full end-to-end lifecycle test. If the full pipeline isn't built yet, use `WorldStateSeeder` to inject a simulated fragment. The code cannot be marked SHIPPED without a passing E2E log.

---

## 3. What to Check (Validation Gates)

When reviewing a Project Mono PR or Plan, verify the following:

- **No Hardcoded Schemas**: If you see `{ "deal_stage": "string" }` hardcoded inside a Prompt string, **Reject it**. It must say `json.dumps(QuoteMutation.model_json_schema())` or the Kotlin equivalent via `kotlinx.serialization`.
- **Domain Purity**: Are the Mutation Data Classes inside pure Kotlin `:domain` modules? If they contain `import android.*`, **Reject it**.
- **Linter Simplicity**: If the Linter contains regex (`Regex("date=.*")`) or manual string-parsing math to figure out the LLM's intent, **Reject it**. It should be a 1-line `decodeFromString()`.
- **JSON Coercion Resilience**: All `PrismJson` instances parsing LLM outputs MUST set `coerceInputValues = true`. The LLM frequently hallucinates explicit `null` tokens; if this flag is missing, `kotlinx.serialization` will crash against native Kotlin non-nullable default values (e.g., `classification = "schedulable"`). Do not solve this with regex null-stripping.
- **Visual Spec Alignment**: "Spec says `最近30天`, code says `最近30天`. No synonyms."

---

## 4. Source of Truth (SOT) Hierarchy

For Project Mono development, resolve conflicts using this hierarchy:

1. **The Kotlin `data class`** in the `:domain` module (Ultimate SSD Contract).
2. **`docs/cerb/[feature]/spec.md`** (The Feature Requirement).
3. **`docs/plans/tracker.md`** (The Active Epic constraints).
4. **`interface-map.md`** (Module Boundaries).

---

## 5. User POV & UX Implications

From the user's perspective, Project Mono is completely invisible, but it results in **Zero Ghosting**.

**UX Rule**: Project Mono enforces that the UI only ever attempts to display mathematically validated SSD records. If there is an anomaly, it yields to the User via `UiState.AwaitingClarification`.

---

## 6. The CQRS Dual-Engine Pattern (Master UJM)

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

### A. The Sync Loop (Fast RAM Assembly & Query)
This is the main interaction pipeline. Its entire goal is to build context (RAM) from the SSD with zero LLM hallucination and sub-second latency *before* generation begins.

1. **Lightning Router (Gatekeeper)**: Does this input contain an "Entity Candidate" (e.g., a person's name)? If NO -> Route to Noise/Greetings or trigger the **Clarification Minor Loop** ("Who are you talking about?").
2. **Alias Lib (L1 Cache)**: Given the Entity Candidate, look up the `EntityID` in the fast, in-memory alias dictionary.
3. **Disambiguation Minor Loop**: If the Alias Lib returns multiple hits (e.g., two Bobs) or a vague hit -> Suspend pipeline. Yield to user.
4. **SSD Graph Fetch -> RAM**: Armed with a pure `EntityID`, query the heavy SQL SSD. Pull the target's entire relationship graph into the **Living RAM Context**.
5. **The Brain Acts (LLM)**: The LLM receives the prompt + the Mega Context (RAM). It generates the chat response using verified intelligence.

### B. The Async Loops (Background Mutations & Commands)
The LLM does NOT directly write to the database in the critical UX path. Writes are asynchronous background events.

1. **Decoupled Entity Writing & Merging**: As the conversation progresses, if the LLM determines an Entity mutation occurred, it emits a strict Kotlin `data class` JSON schema payload. The Background Writer runs the `decodeFromString` Linter, mutates the SSD, and eventually merges that result back into the living RAM.
2. **Decoupled Reinforcement Learning (RL)**: Every user turn is quietly copied to the RL Module. The RL subsystem learns and injects updated "Habits" completely independent of the main session flow.
3. **Session Memory**: The raw chat transcript operates on its own growth logic, injecting recent turns directly into RAM without needing full SSD processing every time. 

**Takeaway for Agents**: Do not treat the LLM as a monolithic text-to-JSON box. It is a dual-engine reasoning brain. The **Sync Loop** strictly bounds the LLM's reality via the Alias Lib and SSD IDs. The **Async Loop** strictly bounds what the LLM is allowed to mutate via Kotlin Data Classes.
