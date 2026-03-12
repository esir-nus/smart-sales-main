# Project Mono: Master Architectural Guide

> **Preamble**: This is the authoritative framework for the "Project Mono" (Data-Oriented OS) architecture migration. Any agent executing tasks, writing specs, or generating code *must* read this document first.

---

## 1. The Philosophy & Purpose

### What is Project Mono?
Project Mono resolves the "Brain/Body Disconnect" by fundamentally changing the contract between the AI Engine (The Brain) and the Android Application (The Body). 

### The Core Problem ("Essay Questions")
Previously, the system relied on "Behavioral Contracts." The AI was asked to write an essay (free-form JSON) based on a hardcoded, handwritten string block inside the `PromptCompiler`. If the database schema changed, developers had to manually update the prompt strings and the regex-heavy Linters. This caused "Ghosting" (the AI hallucinating fields the DB didn't support).

### The Solution ("Multiple Choice")
In Project Mono, **The SSD (Memory) is the center of the universe.**
The interface between the LLM and the application is purely a Kotlin `data class`. 
- **Interaction is Selection**: The AI does not invent; it selects from options loaded into RAM.
- **The Contract**: The LLM output MUST be a strict, auto-generated JSON schema matching the SSD target entity. The PromptCompiler dynamically reads the Kotlin definition.
- **The Linter**: The Linter is reduced to a pure 1-line Deserializer. If it doesn't match the Kotlin object, it crashes safely before touching the disk.

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

- **Before**: "Remind me to call Frank at 50000 budget." -> The system hallucinates a reminder card with a broken budget line.
- **After (Project Mono)**: "Remind me to call Frank at 50000 budget." -> The Linter flags that `TaskMutation` does not accept `budget`. The pipeline gracefully fails into a `Clarification` state ("I can set a reminder, but I cannot attach a budget to a task. Which do you want?"). 

**UX Rule**: Project Mono enforces that the UI only ever attempts to display mathematically validated SSD records. If there is an anomaly, it yields to the User via `UiState.AwaitingClarification`.
