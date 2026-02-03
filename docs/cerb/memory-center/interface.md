# Memory Center Interface

> **Blackbox contract** — For consumers (Scheduler, Chat, Analyst). Don't read implementation.

---

## You Can Call

### MemoryRepository

```kotlin
interface MemoryRepository {
    suspend fun getHotEntries(sessionId: String): List<MemoryEntry>
    suspend fun search(query: String, limit: Int = 10): List<MemoryEntry>
    fun observeHotEntries(sessionId: String): Flow<List<MemoryEntry>>
    suspend fun save(entry: MemoryEntry)
    suspend fun archive(entryId: String)
}
```

### RelevancyRepository

```kotlin
interface RelevancyRepository {
    suspend fun getById(entityId: String): RelevancyEntry?
    suspend fun findByAlias(alias: String): List<RelevancyEntry>
    suspend fun getByType(entityType: EntityType): List<RelevancyEntry>
    suspend fun save(entry: RelevancyEntry)
    suspend fun search(query: String, limit: Int = 10): List<RelevancyEntry>
}
```

### MemoryWriter

```kotlin
interface MemoryWriter {
    suspend fun write(entry: MemoryEntry)
}
```

### ScheduleBoard (Conflict Check)

```kotlin
interface ScheduleBoard {
    /**
     * Current schedule items (reactive, always current).
     */
    val upcomingItems: StateFlow<List<ScheduleItem>>
    
    /**
     * Hardcoded conflict check — no LLM, instant.
     */
    suspend fun checkConflict(
        proposedStart: Long,
        durationMinutes: Int
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

### RelevancyEntry
```kotlin
data class RelevancyEntry(
    val entityId: String,
    val entityType: EntityType,
    val displayName: String,
    val aliasesJson: String,
    val attributesJson: String,
    val lastUpdatedAt: Long
)
```

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
| `getHotEntries` | Returns active + 14-day scheduled items |
| `findByAlias` | Returns all entities matching alias (for disambiguation) |
| `save` | Persists immediately, triggers observers |
| `archive` | Moves from Hot → Cement zone |

---

## You Should NOT

- ❌ Read `RealMemoryRepository.kt` implementation
- ❌ Read `MemoryDAO.kt` (Room layer)
- ❌ Access database directly
- ❌ Implement your own Hot/Cement zone logic
- ❌ Parse `metricsHistoryJson` manually (use provided helpers)

---

## When to Read Full Spec

Read `spec.md` only if:
- You are working **ON** Memory Center itself (not just consuming it)
- You need to understand disambiguation algorithm details
- You are modifying update policies

Otherwise, **trust this interface**.
