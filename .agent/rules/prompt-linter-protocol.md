# Prompt-Linter Contract Protocol

> **Core Principle**: The line between an LLM Prompt (The Brain) and a Kotlin Linter (The Body) is a hard system boundary. It MUST be defined by a statically typed Kotlin contract, never by string-matching or manual JSON traversal.

## 1. The Single Source of Truth (`data class`)
All LLM JSON outputs designed for machine-routing must map 1:1 to a `@Serializable` Kotlin `data class` in the `:domain` layer. 
- **NO Markdown schemas** for machine-routing.
- **NO free-form JSON**.
- The `data class` IS the spec.

## 2. Self-Documenting Attributes (The Agent API)
Use Kotlin KDoc (`/** ... */`) to teach future LLMs and developers the constraints of the schema.
- Agents reading this codebase will use these KDocs to construct their prompts.
- Document allowed enum values, formatting requirements (e.g., ISO-8601), and nullability rules.

```kotlin
@Serializable
data class ExampleExtractionContract(
    /** MUST BE one of: "action", "query", "unknown" */
    val classification: String,
    
    /** The extracted intent. MUST be provided even if unknown. */
    val intent: String,
    
    /** Optional temporal context in YYYY-MM-DD format. Null if absent. */
    val date: String? = null
)
```

## 3. Strict Linter Traversal
Linters must NEVER use manual traversal (e.g., `json.optString("key")`). 
- **NO regexing** Markdown or JSON strings for values.
- Linters must use Kotlin official serializers to fail fast if the LLM hallucinates the schema.

```kotlin
// ❌ WRONG (Silent failures, drift-prone)
val intent = json.optString("intent", "")

// ✅ RIGHT (Compiler-backed, fails explicitly)
val result = Json.decodeFromString<ExampleExtractionContract>(llmOutput)
```

## 4. Prompt Synchronization
Prompts should derive their JSON structure instruction from the `data class` properties to ensure they remain in sync when fields are renamed.

```kotlin
// ❌ WRONG (Hardcoded string schema drifts)
"Please output JSON like { 'intent': '...' }"

// ✅ RIGHT (Coupled to the contract)
"Output JSON strictly matching these keys: ${ExampleExtractionContract::classification.name}, ${ExampleExtractionContract::intent.name}"
```

## When to use Markdown vs JSON Contracts
- **Markdown**: Use for *human context* or *generative content* (e.g., "Summarize this meeting", "Draft an email").
- **JSON Contracts**: Use for *machine routing* and *state mutations* (e.g., "Extract the task date", "Classify this intent"). If the output goes into a database or triggers a UI state machine, it must be a JSON Contract.
