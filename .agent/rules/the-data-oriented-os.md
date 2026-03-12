# The Data-Oriented OS Protocol (Anti-Drift Strategy)

> **Core Principle**: The SSD (Memory) is the center of the universe. Interfaces are defined by Data, not by Behaviors.

## 1. The "Multiple Choice" Rule for LLMs
- **NO "ESSAY" PROMPTS:** Never manually type JSON schemas inside `PromptCompiler` strings.
- **DATA CLASS CONTRACTS:** LLM outputs must be governed by a strict Kotlin `data class` that matches the SSD target entity.
- **AUTO-GENERATION:** The `PromptCompiler` must dynamically read the Kotlin `data class` to generate the JSON instructions for the LLM. 
- *Why:* If the Database requirements change, the Prompt must fail to compile or automatically adapt. The Brain cannot guess what the Body can do.

## 2. Interaction is Selection
- The LLM should not invent new entities when parsing intents. It should select from existing Memory options loaded into RAM.
- **Example:** "Update Acme's quote" -> LLM selects `quote_id: Q-123` from RAM context.

## 3. The 90% Linter Reduction
- Linters must not use regex or complex string math to guess the LLM's intent. 
- Linters must be simple **Deserializers** (`kotlinx.serialization`). If the LLM JSON deserializes into the exact SSD Data Class perfectly, it passes. If it fails, the transaction is rejected.

## 4. Universal Translators
- The API schema used by 3rd party Plugins, the JSON schema expected from the LLM, and the data observed by the UI MUST be the exact same Kotlin object.

---
**Enforcement**: `/01-senior-reviewr` will block any PR/Plan that hardcodes a JSON schema string inside the LLM Pipeline layer instead of binding it directly to the Data layer.
