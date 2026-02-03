# Relevancy Library Interface

> **Consumer contract** — Use this, not the spec.

---

## Overview

Entity lookup and disambiguation for B2B sales context. Provides O(1) access to People, Products, Locations, and Events.

---

## RelevancyRepository

```kotlin
interface RelevancyRepository {
    /**
     * Find entity by alias match (e.g., "张总" → RelevancyEntry?).
     * Returns null if no match found.
     */
    suspend fun findByAlias(alias: String): RelevancyEntry?
    
    /**
     * Find entity by exact ID.
     */
    suspend fun findById(entityId: String): RelevancyEntry?
    
    /**
     * Search entities by display name (fuzzy match).
     */
    suspend fun search(query: String, limit: Int = 10): List<RelevancyEntry>
    
    /**
     * Get all entities of a given type.
     */
    suspend fun getByType(type: EntityType): List<RelevancyEntry>
    
    /**
     * Save or update an entity.
     */
    suspend fun save(entry: RelevancyEntry)
}
```

---

## Input/Output Types

| Method | Input | Output |
|--------|-------|--------|
| `findByAlias` | `alias: String` | `RelevancyEntry?` |
| `findById` | `entityId: String` | `RelevancyEntry?` |
| `search` | `query: String` | `List<RelevancyEntry>` |
| `getByType` | `EntityType` | `List<RelevancyEntry>` |
| `save` | `RelevancyEntry` | Unit |

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Build custom scoring/ranking logic | Let LLM disambiguate |
| Read `demeanorJson` for UI display | Pass to LLM prompt builder |
| Assume alias is unique | Handle multiple matches |
| Cache entities across sessions | Always query fresh |

---

## Related Specs

- [Memory Center](../memory-center/spec.md) — Storage layer
- [User Habit](../user-habit/spec.md) — Learning patterns
