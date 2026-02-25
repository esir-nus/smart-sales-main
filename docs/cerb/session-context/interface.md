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
    fun resetSession()  // Wave 3: 清空 RAM，创建新 WorkingSet
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
    val audioTranscripts: List<TranscriptBlock> = emptyList(),  // 音频转录
    val imageAnalysis: List<VisionResult> = emptyList(),        // 图像分析
    val memoryHits: List<MemoryHit> = emptyList(),              // Section 1 (Distilled Memory)
    val entityKnowledge: String? = null,                        // Section 1 (Entity Knowledge Graph JSON)
    val entityContext: Map<String, EntityRef> = emptyMap(),     // Section 1 (Entity Pointers)
    val modeMetadata: ModeMetadata = ModeMetadata(),            // 模式元数据 (mode, sessionId, turnIndex)
    val sessionHistory: List<ChatTurn> = emptyList(),           // 会话历史
    val lastToolResult: ToolArtifact? = null,                   // 上次工具结果
    val executedTools: Set<String> = emptySet(),                // 已执行工具集
    val currentDate: String? = null,                            // LLM 日期上下文
    val currentInstant: Long = 0,                               // epoch millis
    val habitContext: HabitContext? = null,                      // Sections 2+3 (auto-populated)
    val scheduleContext: String? = null                          // Sticky Notes
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
| Reset session context manually | Call `contextBuilder.resetSession()` via interface |
| Pass `entityIds` to `getHabitContext()` | Habits are auto-populated in RAM Section 3 |
| Read repos directly from Application code | Route through the Working Set (RAM) |

---

## Dependencies

This interface is consumed by:

| Consumer | How |
|----------|-----|
| `RealCoachPipeline` | Calls `contextBuilder.build()` |
| `PrismOrchestrator` | Calls `contextBuilder.build()` / `buildWithClues()` |
| `PrismViewModel` | Calls `contextBuilder.resetSession()` on new session |
| `ChatViewModel` | Calls `contextBuilder.recordUserMessage()` / `recordAssistantMessage()` |

---

## Internals (Do NOT Read)

Implementation details are in [spec.md](./spec.md). Consumers should NOT read:
- Entity state machine logic
- Path indexing implementation
- Cache eviction policies
