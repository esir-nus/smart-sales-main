# Entity Writer Spec

> **Cerb-compliant spec** — Internal implementation of centralized entity writes.

---

## Overview

Centralized write component (`EntityWriter`) for mutating `EntityRepository`.
Ensures consistent:
1. Alias registration (dedup)
2. Field update policies (append vs replace)
3. Source tracking (provenance)
4. **Change tracking** — profile changes (name, title, company) trigger history events

---

## Architecture

```
Caller (Scheduler/Coach)
       │
       ▼
[EntityWriter.upsertFromClue()]
       │
       ├─ 1. Resolve ID (if null)
       │   └─ Query findByAlias()
       │
       ├─ 2. Load Existing (read-modify-write)
       │   └─ getById() → merge fields → save()
       │
       ├─ 3. Apply Field Policies
       │   ├─ displayName: Latest-write-wins (old → aliases)
       │   ├─ aliasesJson: Append unique
       │   ├─ attributesJson: Upsert key
       │   └─ _sourceJson: Track provenance
       │
       └─ 4. Persist
           └─ EntityRepository.save()
```

> [!IMPORTANT]
> `EntityRepository.save()` uses `@Insert(onConflict=REPLACE)` — **full row overwrite**.
> EntityWriter MUST do `getById()` first to preserve existing fields before calling `save()`.
> This read-modify-write is the core reason EntityWriter exists.

---

## Prerequisites (Wave 1 Blockers)

Before implementing EntityWriter, add to existing infrastructure:

| Component | Missing | Change |
|-----------|---------|--------|
| `EntityRepository` | `delete()` method | Add `suspend fun delete(entityId: String)` |
| `EntityDao` | `delete(entityId)` query | Add `@Query("DELETE FROM entity_entries WHERE entityId = :entityId")` |
| `RoomEntityRepository` | `delete()` impl | Delegate to `dao.delete(entityId)` |
| `FakeEntityRepository` | `delete()` impl | `entries.remove(entityId)` |

---

## Implementation Details

### 1. Dedup Logic (`upsertFromClue`)

When `resolvedId` is NULL (caller didn't resolve):

1. **Check Alias**: `EntityRepository.findByAlias(clue)`.
2. **Hit**: Use existing ID.
   - **Load existing**: `getById(existingId)` → merge fields → `save()`.
3. **Miss**: Create NEW entity with `clue` as `displayName`.
   - `aliasesJson` = `["clue"]`, fresh entry, direct `save()`.

When `resolvedId` is PROVIDED:

1. **Load**: `getById(resolvedId)`.
2. **If found**: Merge `clue` into `aliasesJson`, apply policies → `save()`.
3. **If NOT found (stale ID)**: Fall back to alias dedup (treat as null resolvedId).

### 2. Read-Modify-Write Pattern (Critical)

```kotlin
// CORRECT — EntityWriter internals
suspend fun upsertFromClue(...): UpsertResult {
    val existingId = resolvedId ?: findExistingByAlias(clue)
    
    return if (existingId != null) {
        val existing = entityRepository.getById(existingId)
            ?: return createNew(clue, type, source)  // Stale ID fallback
        
        val merged = existing.copy(
            aliasesJson = mergeAliases(existing.aliasesJson, clue),
            lastUpdatedAt = now()
            // displayName: Latest-write-wins (old name → aliasesJson)
            // demeanorJson: KEEP existing (not touched by upsert)
            // metricsHistoryJson: KEEP existing (not touched by upsert)
        )
        entityRepository.save(merged)
        UpsertResult(existingId, isNew = false, displayName = existing.displayName)
    } else {
        val newEntry = createNew(clue, type, source)
        newEntry
    }
}
```

### 3. Field Update Policies

| Field | Policy | Implementation | Via |
|-------|--------|----------------|-----|
| `displayName` | **Latest-write-wins** | Old name → `aliasesJson`, new name replaces | `updateProfile` |
| `aliasesJson` | Append (Cap 8) | `(old + new).distinct().takeLast(8)` — FIFO tail, oldest dropped | `upsertFromClue`, `registerAlias` |
| `demeanorJson` | Upsert per key | `oldMap + newMap` | `updateAttribute` |
| `attributesJson` | Upsert per key | `oldMap + newMap` | `updateAttribute` |
| `metricsHistoryJson` | Append per key | `oldTimeSeries + newEntry` | `updateAttribute` |
| `relatedEntitiesJson` | Append (dedupe) | `(oldList + newId).distinct()` | Future wave |
| `decisionLogJson` | ~~Append-only~~ **Deprecated** | Superseded by `UnifiedActivity` timeline | — |

#### Tracked Profile Fields (Change-Aware)

These fields trigger a `UnifiedActivity` history event when changed:

| Field | ActivityType | Example |
|-------|-------------|----------|
| `displayName` | `NAME_CHANGE` | 索尼娱乐集团 → SONY |
| `jobTitle` | `TITLE_CHANGE` | 销售经理 → 销售VP |
| `accountId` | `COMPANY_CHANGE` | 承时利和 → 华为 |
| `buyingRole` | `ROLE_CHANGE` | champion → economic_buyer |
| `dealStage` | `DEAL_STAGE_CHANGE` | proposal → negotiation |

**Change-Tracking Pattern:**

```kotlin
// Inside EntityWriter.updateProfile()
if (newJobTitle != null && newJobTitle != existing.jobTitle) {
    // 1. Record history BEFORE overwrite
    contextBuilder.recordActivity(
        entityId = entityId,
        type = ActivityType.TITLE_CHANGE,
        summary = "${existing.jobTitle} → $newJobTitle"
    )
    // 2. Update the field
    merged = merged.copy(jobTitle = newJobTitle)
}
```

**Display Format (UI):**
```
SONY (Former 索尼娱乐集团)
孙扬浩 at 华为 (Former 承时利和)
```
Former names are always in `aliasesJson`. UI reads aliases to show history context.

### 4. Source Tracking

Tracked via dedicated `_source` key in `attributesJson` to avoid collision with business attributes:

```json
{
  "budget": "2M",
  "_source": "scheduler",
  "_first_seen": 1709238492000,
  "_last_seen": 1709238492000
}
```

Convention: Keys prefixed with `_` are metadata, not business attributes.
`updateAttribute()` ignores keys starting with `_` from external callers.

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **0** | Prerequisites (delete infra) | ✅ SHIPPED | `EntityRepository.delete()` + DAO + impls |
| **1** | Core Writer | ✅ SHIPPED | `EntityWriter` interface + `RealEntityWriter` + tests |
| **1.5** | Wiring | ✅ SHIPPED | Wire into `PrismOrchestrator` Scheduler path |
| **2** | Change-Aware Profile Management | 🔲 PLANNED | `updateProfile()`, change tracking, history emission |
| **3** | Conflict Merge | 🔲 PLANNED | UI flow for merging duplicates |

---

## Ship Criteria

### 🔬 Wave 0: Prerequisites
Add `delete()` to EntityRepository infrastructure.

- **Ship Criteria**: `EntityRepository.delete(entityId)` available in Room + Fake
- **Deliverables**: 4 file changes (EntityRepository, EntityDao, RoomEntityRepository, FakeEntityRepository)

### 🔬 Wave 1: Core Writer
Centralized entity creation and updates with read-modify-write.

- **Ship Criteria**:
    - `upsertFromClue` correctly handles both "new" and "existing" cases
    - Existing entity fields preserved during update (read-modify-write verified)
    - `aliasesJson` grows but doesn't duplicate
    - `EntityRepository.save()` called exactly once per write
    - Zero regressions in existing read paths

- **Test Cases**:
    - [ ] **New Entity**: `upsert("张总", null)` → `UpsertResult(isNew=true, displayName="张总")`
    - [ ] **Existing (Resolved)**: `upsert("张总", "z-001")` → Updates z-001 alias, `isNew=false`
    - [ ] **Existing (Alias Match)**: `upsert("张总", null)` → Finds via alias, updates, `isNew=false`
    - [ ] **Field Preservation**: Upsert preserves existing `demeanorJson`, `metricsHistoryJson`
    - [ ] **Alias Cap**: Add 9th alias → Oldest alias dropped (FIFO tail), cap at 8
    - [ ] **Missing Entity**: `upsert("X", "nonexistent-id")` → Falls back to alias dedup
    - [ ] **Blank Clue**: `upsert("", null)` → Throws `IllegalArgumentException`
    - [ ] **Delete**: `delete("z-001")` → Entity removed, `getById` returns null
    - [ ] **Delete Missing**: `delete("nonexistent")` → No-op

- **Deliverables**:
    - `EntityWriter.kt` (Interface, domain layer)
    - `RealEntityWriter.kt` (Implementation)
    - `FakeEntityWriter.kt` (Test double)
    - `RealEntityWriterTest.kt`

---

## Constants & Limits

```kotlin
const val MAX_ALIASES = 8
const val MAX_ATTRIBUTES = 20
```
