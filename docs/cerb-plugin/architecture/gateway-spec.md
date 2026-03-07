# Cerb Plugin Ecosystem (System III)

> **OS Layer**: App Infrastructure / Extensibility
> **Status**: INCEPTION

---

## The "Town and Highway" Mental Model
The `app-prism` architecture is a continuous conversational operating system.
- The **Main Highway** is the `UnifiedPipeline`, managing the continuous stream of user audio/text, evaluating intent, and coordinating core systems (System I: Mascot, System II: Analyst).
- The **Towns** are the Plugins. They are temporary deviations from the highway to accomplish specific, bounded tasks (e.g. Exporting a PDF, running a Negotiation Simulation) before returning the user to the highway.

Plugins are **not** isolated REST endpoints. They are continuations of a living conversation. Because of this, they must have access to the same "car" and "engine" (the OS Core Modules) that the Unified Pipeline uses.

### The Problem with "Naked Plugins"
If plugin developers directly instantiate DAOs, repositories, or RL engines:
1. **Security**: A rogue plugin (or hallucinating LLM) could corrupt the CRM database.
2. **Coupling**: The core team could not refactor `:core:context` without breaking every third-party plugin.
3. **Boilerplate Hell**: Every plugin would require 50 lines of setup code just to get the current `sessionId`.

### The Solution: The Gateway Protocol
To achieve a "Cloud API" developer experience, the OS provides a strict, curated **Gateway**. 
1. The Plugin declares a **Manifest** of the permissions it needs.
2. The Orchestrator constructs a **Gateway** object that only satisfies those permissions.
3. The Plugin executes entirely through the Gateway, remaining a pure "Black Box" to the raw SQL databases and internal state machines.

---

## Architectural Contracts

### 1. The Plugin Manifest (Developer Contract)
Every plugin must implement `PrismPlugin` and declare its intent.

```kotlin
interface PrismPlugin {
    val toolId: String // e.g., "EXPORT_PDF"
    val schema: JsonObject // The JSON Schema fed to the LLM for argument extraction
    
    // The Access Key: What Core Modules does this plugin need to talk to?
    val requiredPermissions: Set<CoreModulePermission>

    // The Execution Socket. Notice there are NO direct Repositories here.
    fun execute(
        inputParams: JsonObject, 
        gateway: PluginGateway
    ): Flow<PluginState>
}
```

### 2. The Plugin Gateway (The Cloud API)
The Gateway is the *only* way a plugin interacts with the Smart Sales OS.

```kotlin
interface PluginGateway {
    // ---- SCENARIO 1: The Background Task (e.g. PDF Export) ----
    
    // Read context without knowing how the DB works
    suspend fun getSessionContext(): SessionContext 
    suspend fun getCrmProfile(clientId: String): CrmProfile
    
    // Write data safely (Gateway handles transactions and validation)
    suspend fun appendToHistory(message: String)
    suspend fun emitToChat(message: String, isTransient: Boolean)
    

    // ---- SCENARIO 2: The Stream Hijack (e.g. Negotiation Simulator) ----
    
    // Request temporary control of the Unified Pipeline's I/O
    fun requestStreamLock(): Flow<UserAction>
    fun releaseStreamLock()
}
```

### 3. Core Permissions List
A plugin may only call Gateway functions if it holds the corresponding permission.

```kotlin
enum class CoreModulePermission {
    READ_SESSION_HISTORY,
    WRITE_SESSION_HISTORY,
    READ_CRM_MEMORY,
    WRITE_CRM_MEMORY,
    READ_USER_HABITS,
    USE_RL_ENGINE,
    HIJACK_AUDIO_STREAM // High-risk permission for interactive plugins
}
```

---

## Developer "Cookbook" Workflow
To add a new Hand/Plugin to the ecosystem, a developer will:
1. Write a `spec.md` in `docs/cerb-plugin/[plugin-name]/` using the `/cerb-plugin-template`.
2. Implement `PrismPlugin` and define the required `CoreModulePermission`s.
3. Write the business logic using *only* the `PluginGateway` to fetch data and emit results.
4. Bind the plugin into the system via Dagger `@IntoSet`.
