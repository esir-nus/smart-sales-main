# Entity Disambiguation Interface

> **Owner**: EntityDisambiguator
> **Consumers**: UnifiedPipeline

## Public Interface

```kotlin
interface EntityDisambiguationService {
    /**
     * Attempts to process the raw input for disambiguation.
     * Returns true if it intercepted and handled the input (meaning Orchestrator should yield),
     * Returns false if not in a disambiguation state and Orchestrator should proceed normally.
     * 
     * @return DisambiguationResult indicating what the Orchestrator should do next
     */
    suspend fun process(rawInput: String): DisambiguationResult
    
    /**
     * Halts the current pipeline, saves the original intent, and enters the disambiguation state.
     */
    fun startDisambiguation(originalInput: String, originalMode: Mode, ambiguousName: String, candidates: List<EntityRef>): UiState
    
    /**
     * Clears any pending state.
     */
    fun cancel()
}
```

## Data Models

```kotlin
sealed class DisambiguationResult {
    /** Input was intercepted and processed. The Orchestrator should return this UI state and yield. */
    data class Intercepted(val uiState: UiState) : DisambiguationResult()
    
    /** Disambiguation was successful. Replay the original intent. */
    data class Resumed(val originalInput: String, val mode: Mode) : DisambiguationResult()
    
    /** Disambiguation successful via explicit declaration. Pipeline should write entities. */
    data class Resolved(val declaration: ParseResult.EntityDeclaration, val originalInput: String, val mode: Mode) : DisambiguationResult()
    
    /** Not in disambiguation state, proceed with normal parsing. */
    object PassThrough : DisambiguationResult()
}
```
