# Entity Registry Interface

> **Consumer contract** — Use this, not the spec.

---

## Overview

Entity lookup and disambiguation for B2B sales context. Provides O(1) access to People, Products, Locations, Events, and **CRM entities (Accounts, Contacts, Deals)**.

---

## EntityRepository

```kotlin
interface EntityRepository {
    /**
     * Find entities by alias match (e.g., "张总" → List<EntityEntry>).
     * Returns empty list if no match found.
     */
    suspend fun findByAlias(alias: String): List<EntityEntry>
    
    /**
     * Find entity by exact ID.
     */
    suspend fun getById(entityId: String): EntityEntry?
    
    /**
     * Search entities by display name (fuzzy match).
     */
    suspend fun search(query: String, limit: Int = 10): List<EntityEntry>
    
    /**
     * Get all entities of a given type.
     */
    suspend fun getByType(type: EntityType): List<EntityEntry>
    
    /**
     * Save or update an entity.
     */
    suspend fun save(entry: EntityEntry)
}
```

---

## Input/Output Types

| Method | Input | Output |
|--------|-------|--------|
| `findByAlias` | `alias: String` | `List<EntityEntry>` |
| `getById` | `entityId: String` | `EntityEntry?` |
| `search` | `query: String` | `List<EntityEntry>` |
| `getByType` | `EntityType` | `List<EntityEntry>` |
| `save` | `EntityEntry` | Unit |

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Build custom scoring/ranking logic | Let LLM disambiguate |
| Read `demeanorJson` for UI display | Pass to LLM prompt builder |
| Assume alias is unique | Handle multiple matches |
| Cache entities across sessions | Always query fresh |
| Parse CRM fields from `attributesJson` | Use first-class CRM fields |

---

## CRM Entity Types

| Type | Description | Key Fields |
|------|-------------|------------|
| `ACCOUNT` | Company | `displayName` |
| `CONTACT` | Business contact | `accountId`, `jobTitle`, `buyingRole` |
| `DEAL` | Sales opportunity | `accountId`, `primaryContactId`, `dealStage`, `dealValue` |

---

## Related Specs

- [Memory Center](../memory-center/spec.md) — Storage layer
- [User Habit](../user-habit/spec.md) — Learning patterns
