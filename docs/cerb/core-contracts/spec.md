# Core Contracts Spec

> **OS Layer**: OS: App
> **State**: SPEC_ONLY

## 1. Overview
The Core Contracts module defines the absolute boundary between the LLM ("The Brain") and the Application ("The Body") in Project Mono. 

Instead of treating the LLM as a creative text generator, the system forces the LLM to act as a **Data Entry Clerk**. It is handed a mathematical "Multiple Choice Form" (a strict Kotlin `data class`) via the `PromptCompiler`, and it must return exactly that form.

This **"One Currency Rule"** eliminates translation layers, regex parsing, and "Ghosting" (hallucinated data) across all downstream modules.

## 2. The Relationship Map (Brain to Body)

The `UnifiedMutation` data class is the central nervous system. When the Brain processes an intent, it outputs this single currency, which is then routed to the specific, battle-tested downstream modules:

### A. The RL Module (Memory Context) -> Brain
- **Before execution**, the RL (Reinforcement Learning) module and `UserHabit` inject the user's implicit preferences (e.g., "Always schedule Frank's meetings as 1 hour") into the Brain's prompt (RAM Section 2/3).
- **Result**: The Brain already knows the user's default parameters *before* it fills out the `UnifiedMutation` form.

### B. Brain -> EntityWriter (CRM Updates)
- The Brain extracts CRM facts from the conversation (e.g., "Bob was promoted to VP").
- It populates the `profile_mutations` array in the `UnifiedMutation`.
- **Target**: This array perfectly maps to the `EntityWriter.upsertFromClue()` contract. The Linter passes these mutations directly to the Writer, which handles the SSD write-through safely.

### C. Brain -> Scheduler
- The Brain calculates timelines based on the prompt instructions and RL context.
- It populates the `tasks` array (and its rich metadata: urgency, location, clues).
- **Target**: These strictly-typed tasks are passed to the `SchedulerViewModel` and `ScheduleBoard` for conflict resolution and presentation. The UX code relies on the data class being perfectly formed.

### D. Brain -> IntentOrchestrator (Classification)
- The Brain categorizes the raw input using the `classification` enum (`schedulable`, `deletion`, `reschedule`, `non_intent`).
- **Target**: This allows the `UnifiedPipeline` to cleanly route the request or immediately trigger an error/clarification flow without attempting to parse deeper structures.

## 3. Technical Decisions
- **`kotlinx.serialization` Supremacy**: All models must be marked `@Serializable`. The pipeline relies on a strict `Json.decodeFromString<UnifiedMutation>()`. If the LLM generates an invalid field or wrong type, the deserializer throws safely immediately.
- **Dynamic Prompting (The Teller Rule)**: The `PromptCompiler` MUST use `serializer<UnifiedMutation>().descriptor` to inform the LLM of the exact schema. This ensures the prompt can mathematically never drift from the hardcoded Kotlin logic.
- **Domain Purity**: This module lives entirely in pure Kotlin (`:domain:scheduler` or `:domain:core`). It must never contain imports from `android.*`.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Contract Definition | ✅ SHIPPED | `UnifiedMutation`, `ProfileMutation`, `TaskMutation` data classes |
| **2** | Prompt Compiler Integration | ✅ SHIPPED | Serializer extraction and schema generation in Prompt |
| **3** | Linter Decoupling | 🔲 PLANNED | Replacing `SchedulerLinter` `org.json.JSONObject` string parsing with strict `decodeFromString<UnifiedMutation>()` |
