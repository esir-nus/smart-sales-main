# Entity Writer Interface

> **Blackbox contract** — For consumers (Scheduler, Coach, Audio Pipeline). Don't read implementation.

---

## Overview

Centralized write path for `EntityRepository`. Handles dedup, alias registration, and field update policies so callers don't have to.

**Read side**: Use [EntityRepository](../entity-registry/interface.md) directly for queries.
**Write side**: Use EntityWriter (this interface) for all mutations.

### Shared Types

```kotlin
// Defined in MemoryModels.kt — shared across Entity Registry and Entity Writer
enum class EntityType {
    PERSON, PRODUCT, LOCATION, EVENT,  // Core Types
    ACCOUNT, CONTACT, DEAL             // CRM Types
}
```

---

## EntityWriter

```kotlin
interface EntityWriter {
    /**
     * Upsert entity from a raw mention.
     *
     * - If resolvedId is provided → update existing entity
     * - If resolvedId is null → dedup by alias, create if new
     * - Internally does read-modify-write to preserve existing fields
     *
     * @param clue Raw mention text (e.g., "张总"), must be non-blank
     * @param resolvedId Entity ID from disambiguation (null → auto-resolve)
     * @param type Entity type
     * @param source Caller identifier ("scheduler", "coach", "audio")
     * @return UpsertResult with entityId, isNew flag, and canonical displayName
     * @throws IllegalArgumentException if clue is blank
     */
    suspend fun upsertFromClue(
        clue: String,
        resolvedId: String?,
        type: EntityType,
        source: String
    ): UpsertResult

    /**
     * Update a single attribute on an existing entity.
     * Follows upsert-per-key policy on attributesJson.
     * Keys prefixed with '_' are reserved for internal metadata.
     */
    suspend fun updateAttribute(entityId: String, key: String, value: String)

    /**
     * Register a new alias for an existing entity.
     * Deduplicates and respects the bounded alias list (max 8).
     */
    suspend fun registerAlias(entityId: String, alias: String)

    /**
     * Update tracked profile fields on an existing entity.
     * If a tracked field changes, EntityWriter emits a UnifiedActivity
     * history event BEFORE overwriting the field.
     *
     * Tracked fields: displayName, jobTitle, accountId, buyingRole, dealStage
     *
     * @param entityId Must exist
     * @param updates Map of field name → new value (only non-null fields updated)
     * @return ProfileUpdateResult with list of changes detected
     */
    suspend fun updateProfile(
        entityId: String,
        updates: Map<String, String?>
    ): ProfileUpdateResult

    /**
     * Delete an entity by ID. No-op if entity doesn't exist.
     */
    suspend fun delete(entityId: String)
}

/**
 * Result of an upsert operation.
 */
data class UpsertResult(
    val entityId: String,
    val isNew: Boolean,
    val displayName: String  // Canonical name (for write-back to caller)
)

/**
 * Result of a profile update. Lists which fields actually changed.
 */
data class ProfileUpdateResult(
    val entityId: String,
    val changes: List<ProfileChange>
)

data class ProfileChange(
    val field: String,       // e.g., "jobTitle"
    val oldValue: String?,
    val newValue: String?
)
```

---

## Input/Output Types

| Method | Input | Output |
|--------|-------|--------|
| `upsertFromClue` | clue, resolvedId?, type, source | `UpsertResult` |
| `updateAttribute` | entityId, key, value | Unit |
| `updateProfile` | entityId, updates (Map) | `ProfileUpdateResult` |
| `registerAlias` | entityId, alias | Unit |
| `delete` | entityId | Unit |

---

## Preconditions

| Method | Precondition |
|--------|--------------|
| `upsertFromClue` | `clue` must be non-blank. Throws `IllegalArgumentException` otherwise. |
| `updateAttribute` | `entityId` must exist. No-op if not found. `key` must not start with `_`. |
| `updateProfile` | `entityId` must exist. Throws `IllegalArgumentException` if not found. Only tracked fields accepted. |
| `registerAlias` | `entityId` must exist. No-op if not found. |

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `upsertFromClue` | Idempotent — same clue+resolvedId returns same entityId |
| `upsertFromClue` | Preserves all existing fields (read-modify-write internally) |
| `upsertFromClue` | `displayName` uses **latest-write-wins** policy (old name → aliases) |
| `updateProfile` | Emits `UnifiedActivity` for each tracked field that changed |
| `updateProfile` | Old `displayName` auto-appended to `aliasesJson` before overwrite |
| `registerAlias` | Bounded to 8 aliases max, deduplicates |
| `updateAttribute` | Upsert-per-key on `attributesJson` |
| `delete` | No-op if entity doesn't exist |

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Call `EntityRepository.save()` directly | Use `EntityWriter.upsertFromClue()` |
| Implement dedup logic in your feature | EntityWriter handles alias-based dedup |
| Build your own field merge logic | EntityWriter enforces update policies |
| Read EntityWriter internals | Trust this interface |
| Use `_` prefixed attribute keys | Reserved for internal metadata |

---

## Usage Example (Scheduler)

```kotlin
// After entity resolution in Phase 2:
val resolvedId = entityContext["person_candidate_0"]?.entityId

parsedClues.person?.let { personClue ->
    val result = entityWriter.upsertFromClue(
        clue = personClue,
        resolvedId = resolvedId,
        type = EntityType.PERSON,
        source = "scheduler"
    )
    // result.displayName = canonical name for write-back
    // result.isNew = true if entity was just created
}
```

---

## Related Specs

- [Entity Registry](../entity-registry/interface.md) — Read side (queries)
- [Memory Center](../memory-center/interface.md) — Storage layer
- [Session Context](../session-context/interface.md) — Alias caching
