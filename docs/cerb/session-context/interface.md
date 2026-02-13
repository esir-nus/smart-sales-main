# Session Working Set — Interface

> **Consumer contract** — How other pipelines interact with the session workspace.  
> **OS Layer**: Kernel

---

## Contract

The Session Working Set is the **per-session RAM** managed by `ContextBuilder` (the Kernel). Applications do NOT access it directly — they consume it through `ContextBuilder.build()`, which populates `EnhancedContext` from the Working Set.

> **OS Model**: Applications work on RAM. The Kernel owns the lifecycle.

### Exposed via `ContextBuilder` (existing interface)

```kotlin
interface ContextBuilder {
    suspend fun build(userText: String, mode: Mode): EnhancedContext
    fun getSessionHistory(): List<ChatTurn>
    suspend fun recordUserMessage(content: String)
    suspend fun recordAssistantMessage(content: String)
}
```

**Session Context affects `build()` behavior**:
- First turn: `memoryHits` populated via `MemoryRepository.search()`
- Subsequent turns: cached entity data used, no redundant DB queries
- Entity aliases resolved via `pathIndex` (O(1) after first lookup)

### Output Types

```kotlin
// EnhancedContext is built FROM the SessionWorkingSet (RAM)
data class EnhancedContext(
    val userText: String,
    val memoryHits: List<MemoryHit>,          // From RAM Section 1 (Distilled Memory)
    val entityContext: Map<String, EntityRef>, // From RAM Section 1 (Distilled Memory)
    val habitContext: HabitContext? = null,     // From RAM Sections 2 + 3 (auto-populated)
    val sessionHistory: List<ChatTurn> = emptyList(),
    val currentDate: String? = null,
    val currentInstant: Long = 0,
    val scheduleContext: String? = null,        // Sticky Notes
    val lastToolResult: ToolArtifact? = null
)
```

> **Note**: `habitContext` is auto-populated from the Working Set. Applications should NOT pass `entityIds` explicitly — the Kernel handles entity-to-habit resolution automatically.

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Access `SessionContext` / Working Set directly | Use `ContextBuilder.build()` — it reads from RAM internally |
| Manage entity state from ViewModel | Let `ContextBuilder` (Kernel) manage entity lifecycle |
| Call `MemoryRepository.search()` yourself | Trust `ContextBuilder` to search on first turn |
| Reset session context manually | Call `RealContextBuilder.resetSession()` on mode switch |
| Pass `entityIds` to `getHabitContext()` | Habits are auto-populated in RAM Section 3 |
| Read repos directly from Application code | Route through the Working Set (RAM) |

---

## Dependencies

This interface is consumed by:

| Consumer | How |
|----------|-----|
| `RealCoachPipeline` | Calls `contextBuilder.build()` |
| `PrismOrchestrator` | Calls `contextBuilder.build()` / `buildWithClues()` |
| `ChatViewModel` | Calls `contextBuilder.recordUserMessage()` / `recordAssistantMessage()` |

---

## Internals (Do NOT Read)

Implementation details are in [spec.md](./spec.md). Consumers should NOT read:
- Entity state machine logic
- Path indexing implementation
- Cache eviction policies
