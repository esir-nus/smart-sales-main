## 📋 Review Conference Report

**Date**: 2026-03-12
**Subject**: Why did the implementation drift from the OS Architecture SOT? (Multi-Intent Flow)
**Panel**: `/06-audit`, `/cerb-check`, `/01-senior-reviewr`

---

### 🔍 `/06-audit` (Evidence-Based Code Audit)

**Scope**: `RealUnifiedPipeline.kt`, `IntentOrchestrator.kt`, `PromptCompiler.kt` vs `pipeline-explainer.md`, `interface-map.md`.

#### Literal Spec vs Code Comparison

| Spec Source | Spec Says (VERBATIM) | Code Says (VERBATIM) | Match? |
|-------------|----------------------|----------------------|--------|
| `pipeline-explainer.md` L38 | "Foreground (CRM LLM): The heavy LLM figures out the best response and creates a structured JSON output (including the response text and proposed calendar events)." | `RealUnifiedPipeline.kt L205`: `if (input.intent == QueryQuality.CRM_TASK) { ... } else { ... Analyst Prompt ... }` | ❌ NO |
| `pipeline-explainer.md` L45 | "The CRM Engine updates the entity profile (e.g., changes deal stage)." | `PromptCompiler.kt` JSON schemas lack any fields for `dealStage` or `budget`. Only `tasks: []` exists. | ❌ NO |
| `interface-map.md` L64 | `UnifiedPipeline` orchestrates "System II context ETL & execution" | Pipeline creates two mutually exclusive paths based on early intent classification. | ❌ NO |

**Audit Findings**: 
The code represents a **Hard Fork** rather than a **Unified Pipeline**. The spec explicitly describes a single "Foreground CRM LLM" that handles response text, CRM profile updates, AND scheduled tasks simultaneously in one JSON payload. The code split this into two separate LLM calls (Scheduler vs Analyst) that cannot run together.

---

### 🛡️ `/cerb-check` (OS Model Compliance)

**Verdict**: **FAIL**
**Reason**: Violation of the Single-Pass OS Model. 
The OS model dictates a linear Pipeline where ONE compiled context is evaluated by the Brain. By splitting the LLM into a `SchedulerPrompt` and an `AnalystPrompt` based on the `InputParser`'s Phase 0 `QueryQuality`, the implementation assumed human intents are mutually exclusive. It breaks the core premise that user input can be highly composite ("Change the stage to Won AND schedule a meeting").

---

### 💡 Senior's Synthesis (`/01-senior-reviewr`)

#### Why did this drift happen? (Root Causes)

1. **Incremental Feature Tunnel Vision (The "Scheduler" Trap)**: 
   When we built the `Scheduler` feature in Wave 3, we hyper-focused on extracting complex time expressions perfectly (e.g., "reschedule to tomorrow 3pm"). To guarantee JSON stability, we created a highly rigid, specialized `SchedulerLinter` and gave it a dedicated LLM prompt that *only* cared about `tasks: []`. We sacrificed the holistic "Unified" vision for the sake of getting the calendar feature to work predictably.

2. **Crude Phase 0 Routing**:
   The `InputParser` (Turbo Gateway) was designed to filter out junk (`NOISE`, `GREETING`) and pass the rest to the deep pipeline. But we overloaded it. By making the Turbo model guess if the input is a `CRM_TASK` vs `DEEP_ANALYSIS`, we forced a heavy routing decision too early. The code currently says: "If the fast prompt sees '明天' (tomorrow), route the entire deep pipeline to the Scheduler track and ignore all Analyst/CRM mutation logic."

3. **The "Motor Cortex" Detachment**:
   We built incredible "Hands" in the recent waves. `RealEntityWriter` has a sophisticated `updateProfile` method and `UnifiedActivity` timeline tracing. But we never wired the "Motor Cortex" to it. We updated the App layer to mutate the database, but we never updated `PromptCompiler.kt` to actually teach the LLM *how* to output those mutations. The JSON schema for the brain is stale.

#### 💡 The Refactoring Path (How we fix it)

We don't need to rebuild the OS. We just need to align the code back to the `pipeline-explainer.md` spec. We need to **Nuke and Pave the Hard Fork**.

Here is the blueprint:

1. **One Master Intent Payload (`PromptCompiler.kt`)**: 
   Combine the Analyst and Scheduler prompts into ONE single System II prompt that outputs a multi-intent JSON schema:
   ```json
   {
     "thought": "User wants to update deal stage and schedule a follow-up.",
     "response": "已为您起草更新...",
     "scheduled_tasks": [...],
     "profile_mutations": [
       {"entityId": "e-123", "field": "dealStage", "value": "won"},
       {"entityId": "e-123", "field": "budget", "value": "1200000"}
     ]
   }
   ```
2. **Upgrade the Evaluators**: 
   Update `SchedulerLinter` to become a `UnifiedLinter` that maps `profile_mutations` directly to `EntityWriter.updateProfile()` calls, and maps `scheduled_tasks` to the Scheduler repository.
3. **Remove the Fork (`RealUnifiedPipeline.kt`)**: 
   Delete the `if (input.intent == QueryQuality.CRM_TASK)` logic. Pass ALL substantive intents through the One Master Payload, letting the LLM's superior reasoning decide what arrays to fill based on the context.
