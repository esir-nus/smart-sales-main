# Session History — Interface

> **Consumer contract** — How UI and other modules interact with session history.
> **OS Layer**: SSD

---

## Contract

Session History provides **cross-session metadata persistence** for the History Drawer and related UI. It is NOT the per-session working set (that's `session-context`).

### HistoryRepository

```kotlin
interface HistoryRepository {
    fun getGroupedSessions(): Map<String, List<SessionPreview>>
    fun getSessions(): List<SessionPreview>
    fun getSession(sessionId: String): SessionPreview?
    fun createSession(clientName: String, summary: String, linkedAudioId: String? = null): String
    fun togglePin(sessionId: String)
    fun renameSession(sessionId: String, newClientName: String, newSummary: String)
    fun deleteSession(sessionId: String)
}
```

### Output Types

```kotlin
data class SessionPreview(
    val id: String,
    val clientName: String,
    val summary: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val linkedAudioId: String? = null
)
```

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Access `SessionDao` directly from UI | Use `HistoryRepository` interface |
| Store session metadata in `SessionWorkingSet` | Use `HistoryRepository` — different lifecycle |
| Confuse this with `session-context` | Session Context = RAM (pipeline). Session History = SSD (metadata). |
| Call `HistoryRepository` from pipeline code | Pipelines use `ContextBuilder`. History is for UI only. |

---

## Dependencies

This interface is consumed by:

| Consumer | How |
|----------|-----|
| `HistoryViewModel` | Calls `getGroupedSessions()`, `togglePin()`, `renameSession()`, `deleteSession()` |
| `AgentViewModel` | Calls `createSession()` on new session |
| `AudioViewModel` | Calls `createSession()` to link audio analysis sessions |

---

## Internals (Do NOT Read)

Implementation details are in [spec.md](./spec.md). Consumers should NOT read:
- `SessionEntity` Room schema
- `SessionDao` query details
- Grouping algorithm internals
