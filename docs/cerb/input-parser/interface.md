# Input Parser Interface

> **Owner**: input-parser
> **Consumers**: prism-orchestrator, unified-pipeline, scheduler

## Public Interface

```kotlin
interface InputParserService {
    /**
     * Parses the raw user input into a structured semantic intent.
     * Parses the raw user input into a structured semantic intent.
     * Extracts time, location, and entities.
     * Instructs qwen-turbo to map the entities against a lightweight Contact Sheet payload.
     * 
     * @param rawInput The text spoken or typed by the user.
     * @return ParseResult (Success with precise IDs, or NeedsClarification)
     */
    suspend fun parseIntent(rawInput: String): ParseResult
}
```

## Data Models

```kotlin
sealed class ParseResult {
    data class Success(
        val resolvedEntityIds: List<String>, 
        val temporalIntent: String?,
        val rawParsedJson: String 
    ) : ParseResult()

    data class NeedsClarification(
        val ambiguousName: String,
        val suggestedMatches: List<EntityRef>,
        val clarificationPrompt: String
    ) : ParseResult()
}
```

## You Should NOT
- Call `parseIntent()` from within the `ContextBuilder` (it must be called *before* context assembly to know what to fetch).
