# Plugin Registry Interface

> **Owner**: PluginRegistry Module
> **Consumers**: Analyst Orchestrator, AgentViewModel, UI Task Board

## Public Interface

```kotlin
interface ToolRegistry {
    /**
     * Get all currently available and active tools/plugins.
     */
    suspend fun getAllTools(): List<AnalystTool>

    /**
     * Execute a specific tool workflow by its ID.
     * @param toolId The ID of the tool (e.g. "EXPORT_CSV")
     * @param request The structured request containing parameters extracted by the LLM
     * @return A Flow of UiState allowing the plugin to emit progressive loading states 
     *         and a final Result/Response.
     */
    fun executeTool(toolId: String, request: PluginRequest): kotlinx.coroutines.flow.Flow<com.smartsales.prism.domain.model.UiState>
}

// Future Expansion (Wave 2)
interface PrismPlugin {
    val metadata: AnalystTool
    fun execute(request: PluginRequest): kotlinx.coroutines.flow.Flow<com.smartsales.prism.domain.model.UiState>
}
```

## Data Models

```kotlin
data class AnalystTool(
    val id: String,
    val label: String,
    val description: String,
    val requiredParams: Map<String, String> = emptyMap() // e.g. "targetDate" -> "ISO8601 string"
)

/**
 * Encapsulates the execution context for a plugin.
 * @param rawInput The original user query for fallback context
 * @param parameters The structured JSON parameters extracted by the LLM Architect
 */
data class PluginRequest(
    val rawInput: String, 
    val parameters: Map<String, Any>
)
```
