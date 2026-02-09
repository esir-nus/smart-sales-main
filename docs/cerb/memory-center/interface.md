# Memory Center Interface

> **Blackbox contract** — For consumers (Scheduler, Chat, Analyst). Don't read implementation.

---

## You Can Call

### MemoryRepository

```kotlin
interface MemoryRepository {
    suspend fun getActiveEntries(sessionId: String): List<MemoryEntry>
    suspend fun search(query: String, limit: Int = 10): List<MemoryEntry>
    fun observeActiveEntries(sessionId: String): Flow<List<MemoryEntry>>
    suspend fun save(entry: MemoryEntry)
    suspend fun markAsArchived(entryId: String)
}
```

### EntityRepository

See [Entity Registry Interface](../entity-registry/interface.md) for full contract.


### ScheduleBoard (Conflict Check)

```kotlin
interface ScheduleBoard {
    /**
     * Current schedule items (reactive, always current).
     */
    val upcomingItems: StateFlow<List<ScheduleItem>>
    
    /**
     * Hardcoded conflict check — no LLM, instant.
     * @param excludeId Skip this task ID (prevents self-conflict after insert)
     */
    suspend fun checkConflict(
        proposedStart: Long,
        durationMinutes: Int,
        excludeId: String? = null
    ): ConflictResult
    
    /**
     * Force refresh from MemoryRepository.
     */
    suspend fun refresh()
}

sealed class ConflictResult {
    object Clear : ConflictResult()
    data class Conflict(val overlaps: List<ScheduleItem>) : ConflictResult()
}
```

---

## Input/Output Types

### MemoryEntry
```kotlin
data class MemoryEntry(
    val id: String,
    val workflow: String,
    val title: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val sessionId: String,
    val contentWithMarkup: String,
    val structuredJson: String?
)
```

### EntityEntry (formerly RelevancyEntry)

See [Entity Registry Interface](../entity-registry/interface.md) for full `EntityEntry` definition (includes CRM extension fields).

### ScheduleItem
```kotlin
data class ScheduleItem(
    val entryId: String,
    val title: String,
    val scheduledAt: Long,
    val durationMinutes: Int,
    val durationSource: DurationSource,
    val conflictPolicy: ConflictPolicy,
    val participants: List<String>,
    val location: String?
)

enum class DurationSource { USER_SET, INFERRED, FOLLOW_UP, LEARNED, DEFAULT }
enum class ConflictPolicy { EXCLUSIVE, COEXISTING, BACKGROUND }
```

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `getActiveEntries` | Returns active + 14-day scheduled items |
| `findByAlias` | Returns all entities matching alias (for disambiguation) |
| `save` | Persists immediately, triggers observers |
| `markAsArchived` | Sets `isArchived = true` |

---

## You Should NOT

- ❌ Read `RealMemoryRepository.kt` implementation
- ❌ Read `MemoryDAO.kt` (Room layer)
- ❌ Access database directly
- ❌ Implement your own Active/Archived zone logic
- ❌ Parse `metricsHistoryJson` manually (use provided helpers)

---

## When to Read Full Spec

Read `spec.md` only if:
- You are working **ON** Memory Center itself (not just consuming it)
- You need to understand disambiguation algorithm details
- You are modifying update policies

Otherwise, **trust this interface**.
