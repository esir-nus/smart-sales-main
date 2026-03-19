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
     * @param toolId The semantic entry ID of the tool
     *        (e.g. "artifact.generate", "audio.analyze")
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
    HIJACK_AUDIO_STREAM,
    READ_ARTIFACT_CONTEXT,
    READ_AUDIO_CONTEXT,
    READ_SIMULATION_CONTEXT,
    MINT_ARTIFACT,
    EXPORT_ARTIFACT
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
    val requiredParams: Map<String, String> = emptyMap() // e.g. "ruleId" -> "executive_report"
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

## Semantic Entry IDs

The outer orchestrator should route plugins by stable semantic entry IDs, not by opaque numeric slots.

Current preferred entry IDs:

- `artifact.generate`
- `audio.analyze`
- `crm.sheet.generate`
- `simulation.talk`

Routing law:

- `toolId` selects the plugin family
- `ruleId` inside `PluginRequest.parameters` selects the behavior variant within that family
- deep capability choreography remains internal to the plugin lane

Example:

```json
{
  "toolId": "artifact.generate",
  "parameters": {
    "ruleId": "executive_report",
    "targetRef": "account:huawei",
    "audience": "executive"
  }
}
```

## T4 Capability-Facing Direction

The current Kotlin interface still reflects the transitional T3 shell.

For T4+, the plugin-facing contract should evolve toward:

- semantic `toolId` at dispatch time
- bounded `ruleId`-driven specialization
- reusable capability calls behind `PluginGateway`
- bounded result types that distinguish:
  - transient progress
  - read-only final result
  - write-request / artifact-mint request

The semantic tool families currently expected to drive that evolution are:

| `toolId` | Primary Capabilities |
|------|-----------------------|
| `artifact.generate` | `READ_ARTIFACT_CONTEXT`, `MINT_ARTIFACT`, `EXPORT_ARTIFACT` |
| `audio.analyze` | `READ_AUDIO_CONTEXT`, `READ_SESSION_HISTORY` |
| `crm.sheet.generate` | `READ_CRM_MEMORY`, `READ_SESSION_HISTORY`, later artifact mint |
| `simulation.talk` | `READ_SIMULATION_CONTEXT`, `READ_SESSION_HISTORY` |

## T3 Runtime Contract Notes

- The first delivered runtime gateway bundle is intentionally narrow:
  - `READ_SESSION_HISTORY`
  - bounded progress signaling
- The active T3 gateway surface is read-only; history append remains out of scope until a later write-capability wave.
- Plugin-caused writes remain out of scope for this interface wave and re-enter later through the mutation lane.
- `Flow<UiState>` remains a transitional shell for the outer plugin lane, but the gateway itself is the ownership boundary for runtime capabilities and permissions.
- Current outer-loop delivery contract:
  - non-voice plugin dispatch is surfaced as a proposal and requires the existing `"确认执行"` commit step before real registry execution begins
  - approved voice plugin dispatch may auto-enter the plugin lane, but it must still yield bounded execution states back to the OS shell
  - the OS currently observes plugin entry and yield through `PluginExecutionStarted` and `PluginExecutionEmittedState`
