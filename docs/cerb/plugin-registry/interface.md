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
     * @return A Flow of UiState allowing the OS shell to surface bounded progress
     *         and the plugin to yield a final Result/Response.
     */
    fun executeTool(
        toolId: String,
        request: PluginRequest,
        gateway: PluginGateway
    ): kotlinx.coroutines.flow.Flow<com.smartsales.prism.domain.model.UiState>
}

// Future Expansion (Wave 2)
interface PrismPlugin {
    val metadata: AnalystTool
    val requiredPermissions: Set<CoreModulePermission>
    fun execute(
        request: PluginRequest,
        gateway: PluginGateway
    ): kotlinx.coroutines.flow.Flow<com.smartsales.prism.domain.model.UiState>
}

enum class CoreModulePermission {
    READ_SESSION_HISTORY,
    WRITE_SESSION_HISTORY,
    READ_CRM_MEMORY,
    WRITE_CRM_MEMORY,
    READ_USER_HABITS,
    USE_RL_ENGINE,
    HIJACK_AUDIO_STREAM
}

interface PluginGateway {
    suspend fun getSessionHistory(turns: Int): String
    suspend fun emitProgress(message: String)

    /**
     * Runtime-granted permissions for this execution.
     * Plugins declare what they need via `requiredPermissions`;
     * the OS runtime decides what is actually granted.
     */
    fun grantedPermissions(): Set<CoreModulePermission> = emptySet()
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

## T3 Runtime Contract Notes

- The first delivered runtime gateway bundle is intentionally narrow:
  - `READ_SESSION_HISTORY`
  - bounded progress signaling
- The active T3 gateway surface is read-only; history append remains out of scope until a later write-capability wave.
- Plugin-caused writes remain out of scope for this interface wave and re-enter later through the mutation lane.
- `Flow<UiState>` remains a transitional shell for the outer plugin lane, but the gateway itself is the ownership boundary for runtime capabilities and permissions.
