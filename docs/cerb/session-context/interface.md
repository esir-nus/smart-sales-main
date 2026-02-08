# Session Context — Interface

> **Consumer contract** — How other pipelines interact with the session cache.

---

## Contract

Session Context is an **internal optimization** of `ContextBuilder`. It is NOT directly accessed by consumers.

### Exposed via `ContextBuilder` (existing interface)

```kotlin
interface ContextBuilder {
    suspend fun build(userText: String, mode: Mode): EnhancedContext
    fun getSessionHistory(): List<ChatTurn>
    fun recordUserMessage(content: String)
    fun recordAssistantMessage(content: String)
}
```

**Session Context affects `build()` behavior**:
- First turn: `memoryHits` populated via `MemoryRepository.search()`
- Subsequent turns: cached entity data used, no redundant DB queries
- Entity aliases resolved via `pathIndex` (O(1) after first lookup)

### Output Types

```kotlin
// Already defined in EnhancedContext — no new types exposed
data class EnhancedContext(
    val memoryHits: List<MemoryHit>,     // Populated by session context logic
    val entityContext: Map<String, EntityRef>, // Populated by path index
    // ... other fields unchanged
)
```

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Access `SessionContext` directly | Use `ContextBuilder.build()` — it handles caching internally |
| Manage entity state from ViewModel | Let `ContextBuilder` manage entity lifecycle |
| Call `MemoryRepository.search()` yourself for Coach mode | Trust `ContextBuilder` to search on first turn |
| Reset session context manually | Call `RealContextBuilder.resetSession()` on mode switch |

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
