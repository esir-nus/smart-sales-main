# Input Parser (Turbo Router)

> **Cerb-compliant spec** — Semantic parsing, alias disambiguation, and session auto-renaming trigger.
> **OS Layer**: RAM Application (Gateway before Kernel context assembly)
> **State**: 🔲 PLANNED

---

## Overview

The `input-parser` (Turbo Router) is the **Step 1 Gateway** for all voice and text inputs into Smart Sales. Before any module (Coach, Scheduler, Analyst) builds its context, the Input Parser uses a fast LLM (`qwen-turbo`) fed with a lightweight **Semantic Mapping Payload** (a JSON dictionary of known entity IDs, names, and aliases) to disambiguate the user's intent.

**Key Principles**:
1. **Extraction First, LLM Semantic Mapping**: We do not use SQL `LIKE` or Kotlin string-distance (Levenshtein) math for name resolution. Instead, we pass a lightweight "Contact Sheet" to `qwen-turbo`. The LLM's vast semantic knowledge natively resolves typos, homophones, and even missing nicknames (e.g., mapping "CEO" to "Tim Cook").
2. **Clarification Loop**: If the Alias Index yields low confidence (multiple matches or missing CRM data), the parser halts the pipeline and yields a clarification request back to the user.
3. **Auto-Renaming Hook**: The parsed semantic intent is immediately routed to the `SessionTitleGenerator` in the background.

---

## Architecture & Components

### 1. Semantic Mapping Payload (The "Contact Sheet")
A lightweight JSON dictionary built in memory before calling the LLM. 
- **Format**: `[{"index": 1, "id": "uuid...", "name": "孙工", "aliases": ["小孙"]}]`
- **Purpose**: Gives `qwen-turbo` the exact universe of known entities. We use an integer `index` in the prompt, asking the LLM to return `{"resolved_indices": [1]}`, which mathematically prevents UUID hallucination.

### 2. InputParserService
The coordinator for Step 1.
- Fetches all `PERSON` and `ACCOUNT` entities from `EntityRepository` (lightweight, < 2000 rows).
- Formats them into the Semantic Mapping Payload.
- Prompts `qwen-turbo` with the user's raw input + the Payload, instructing it to extract time, intent, and map mentions to the provided indices.
- Converts the LLM output back into `EntityID`s.

### 3. ParseResult (The Contract)
A sealed class representing the outcome of Step 1.

```kotlin
sealed class ParseResult {
    /** High confidence. Pass these Exact IDs to the ContextBuilder. */
    data class Success(
        val resolvedEntityIds: List<String>, 
        val temporalIntent: String?,
        val rawParsedJson: String // For auto-renaming
    ) : ParseResult()

    /** Low confidence. Halt pipeline and ask user. */
    data class NeedsClarification(
        val ambiguousName: String,
        val suggestedMatches: List<EntityRef>,
        val clarificationPrompt: String
    ) : ParseResult()
    
    /** User explicitly provided a declarative cure (e.g. "He is the CTO"). */
    data class EntityDeclaration(
        val name: String,
        val company: String?,
        val jobTitle: String?,
        val aliases: List<String>,
        val notes: String?
    ) : ParseResult()
}
```

---

## Interaction Rules

| Rule | Enforcer | Explanation |
|------|----------|-------------|
| **Gateway Enforcement** | `PrismOrchestrator` | `InputParserService` must run *before* `ContextBuilder.build()`. |
| **Declarative Cure Precedence** | `InputParserService` | If an explicit `EntityDeclaration` is parsed, it MUST return that result immediately, bypassing any `unknown_names` identification arrays to allow the `EntityWriter` to proceed. |
| **Fail-Safe** | `ParseResult` | If `NeedsClarification` is returned, the Orchestrator must NOT call the main LLM. It must echo the `clarificationPrompt` to the user. |
| **No DB Flooding** | `ContextBuilder` | The Kernel only loads data for the exact `resolvedEntityIds` returned by this parser. |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Contracts & Payload Generation** | 🔲 PLANNED | `InputParserService`, `ParseResult`, Semantic Mapping JSON generation. |
| **2** | **Turbo LLM Disambiguation** | 🔲 PLANNED | `RealInputParserService` injecting Payload into `qwen-turbo`. |
| **3** | **Orchestrator Wiring** | 🔲 PLANNED | Mount at the front of `PrismOrchestrator`. Handle Clarification loop UI state. |
| **4** | **Auto-Renaming Hook** | 🔲 PLANNED | Fire `LlmSessionTitleGenerator` using the parsed JSON. |

---

## OS Layer Adherence
- Acts purely in RAM Application Layer.
- Translates unstructured intent + a lightweight RAM taxonomy into concrete IDs.
- Emits IDs that the Kernel (`ContextBuilder`) uses to selectively read heavy data (metrics, logs) from SSD (`EntityRepository`).
