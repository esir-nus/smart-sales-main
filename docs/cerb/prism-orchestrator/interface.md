# PrismOrchestrator Interface

> **Owner**: Analyst Orchestrator (Top-Level)
> **Consumers**: User Interface (`PrismViewModel`)

## Public Interface

```kotlin
interface PrismOrchestrator {
    /**
     * The single unified entry point for all user chat messages.
     * Evaluates intents, routes to Mascot (System I) or Plugins (System II),
     * and manages the central chat string.
     */
    suspend fun processInput(input: String)
    
    /**
     * The reactive flow of the current chat UI state.
     */
    val state: StateFlow<PrismState>
}
```

## Data Models

```kotlin
sealed class PrismState {
    data object Idle : PrismState()
    data class Processing(val statusText: String) : PrismState() // e.g., "Thinking...", "Analyzing..."
    
    // Asynchronous tool execution loading states passed through from PluginRegistry
    data class ExecutingTool(val toolName: String) : PrismState() 
}
```
