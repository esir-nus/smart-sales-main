# Session Working Set — Interface

> **Consumer contract** — How other pipelines interact with the session workspace.  
> **OS Layer**: Kernel

---

## Contract

The Session Working Set is the **per-session RAM** managed by `ContextBuilder` (the Kernel). Applications do NOT access it directly — they consume it through `ContextBuilder.build()`, which populates `EnhancedContext` from the Working Set.

> **OS Model**: Applications work on RAM. The Kernel owns the lifecycle.

### Exposed via `ContextBuilder` (existing interface)

```kotlin
enum class ContextDepth {
    MINIMAL,      // Only UserText + History
    DOCUMENT_QA,  // MINIMAL + Audio Transcript
    FULL          // The complete RAM snapshot (Habits, Pointers, Knowledge)
}

interface ContextBuilder {
    suspend fun build(
        userText: String, 
        mode: Mode, 
        resolvedEntityIds: List<String> = emptyList(), 
        depth: ContextDepth = ContextDepth.FULL
    ): EnhancedContext
    
    fun getSessionHistory(): List<ChatTurn>
    suspend fun recordUserMessage(content: String)
    suspend fun recordAssistantMessage(content: String)
    fun resetSession()
    fun getActiveSessionId(): String
    fun loadSession(sessionId: String, history: List<ChatTurn>)
    fun loadDocumentContext(payload: String)
    suspend fun applyHabitUpdates(observations: List<com.smartsales.prism.domain.rl.RlObservation>)
}
```

**Session Context affects `build()` behavior**:
- First turn: `EntityContext` populated via `MemoryRepository.search()`
- Subsequent turns: cached entity data used, no redundant DB queries
- Entity aliases resolved via `pathIndex` (O(1) after first lookup)

### Runtime Admission Contract

Live runtime must feed recent-turn context through the Kernel, not only through UI-local history.

- user turns must call `recordUserMessage()` before the parent lane proceeds
- assistant turns that become visible chat history must call `recordAssistantMessage()`
- clarification and disambiguation prompts are not UI-only; they are repair prompts and must be admitted into session memory
- follow-up repair input resumes the parent lane through `build(...).sessionHistory`, not through a hidden UI cache

### Output Types

```kotlin
// EnhancedContext is built FROM the SessionWorkingSet (RAM)
data class EnhancedContext(
    val userText: String,
    val audioTranscripts: List<TranscriptBlock> = emptyList(),
    val imageAnalysis: List<VisionResult> = emptyList(),
    val entityKnowledge: String? = null,
    val entityContext: Map<String, EntityRef> = emptyMap(),
    val modeMetadata: ModeMetadata = ModeMetadata(),
    val sessionHistory: List<ChatTurn> = emptyList(),
    val lastToolResult: ToolArtifact? = null,
    val executedTools: Set<String> = emptySet(),
    val currentDate: String? = null,
    val currentInstant: Long = 0,
    val documentContext: String? = null,
    val habitContext: HabitContext? = null,
    val scheduleContext: String? = null,
    val schedulerPatternContext: SchedulerPatternContext? = null,
    val systemPromptOverride: String? = null
)
```

> **Note**: `habitContext` is auto-populated from the Working Set. Applications should NOT pass `entityIds` explicitly — the Kernel handles entity-to-habit resolution automatically.
>
> `schedulerPatternContext` is a separate summarized scheduler-owned signal for RL user-habit learning. It is not raw `scheduleContext`, and it does not authorize default client/entity-habit inference.

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Access `SessionContext` / Working Set directly | Use `ContextBuilder.build()` — it reads from RAM internally |
| Manage entity state from ViewModel | Let `ContextBuilder` (Kernel) manage entity lifecycle |
| Call `MemoryRepository.search()` yourself | Trust `ContextBuilder` to search on first turn |
| Reset session context manually | Call `contextBuilder.resetSession()` via interface |
| Pass `entityIds` to `getHabitContext()` | Habits are auto-populated in RAM Section 3 |
| Read repos directly from Application code | Route through the Working Set (RAM) |

---

## Dependencies

This interface is consumed by:

| Consumer | How |
|----------|-----|
| `RealCoachPipeline` | Calls `contextBuilder.build()` |
| `IntentOrchestrator` / `UnifiedPipeline` | Calls `contextBuilder.build()` / `buildWithClues()` |
| `AgentViewModel` | Calls `resetSession()`, `loadSession()`, `recordUserMessage()`, and `recordAssistantMessage()` |

---

## Internals (Do NOT Read)

Implementation details are in [spec.md](./spec.md). Consumers should NOT read:
- Entity state machine logic
- Path indexing implementation
- Cache eviction policies
