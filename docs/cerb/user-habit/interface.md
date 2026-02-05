# User Habit Interface

> **Consumer contract** — Use this, not the spec.

---

## Overview

Behavioral pattern learning for personalization. Query habits to inform LLM prompts and provide smart defaults.

---

## UserHabitRepository

```kotlin
interface UserHabitRepository {
    /**
     * Get all global habits (not entity-specific).
     */
    suspend fun getGlobalHabits(): List<UserHabit>
    
    /**
     * Get habits for a specific entity (client/product).
     */
    suspend fun getByEntity(entityId: String): List<UserHabit>
    
    /**
     * Get a specific habit by key + optional entity.
     */
    suspend fun getHabit(key: String, entityId: String? = null): UserHabit?
    
    /**
     * Record an observation (increments counter, updates confidence).
     */
    suspend fun observe(key: String, value: String, entityId: String? = null)
    
    /**
     * Record a rejection (user overrode a suggestion).
     */
    suspend fun reject(key: String, entityId: String? = null)
}
```

---

## Input/Output Types

| Method | Input | Output |
|--------|-------|--------|
| `getGlobalHabits` | — | `List<UserHabit>` |
| `getByEntity` | `entityId` | `List<UserHabit>` |
| `getHabit` | `key`, `entityId?` | `UserHabit?` |
| `observe` | `key`, `value`, `entityId?` | Unit |
| `reject` | `key`, `entityId?` | Unit |

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Use habits for hard logic | Use as LLM hints only |
| Trust low-confidence habits | Check `confidence > 0.7` |
| Override explicit habits | Respect `isExplicit = true` |
| Cache habits long-term | Re-query each session |

---

## Related Specs

- [Memory Center](../memory-center/spec.md) — Storage layer
- [Entity Registry](../entity-registry/spec.md) — Entity lookup
