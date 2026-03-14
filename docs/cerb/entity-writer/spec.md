# Entity Writer Spec

> **State**: Mono Wave 2 SHIPPED
> **OS Layer**: RAM Application

---

## Overview

EntityWriter is an **Application** that runs on the SessionWorkingSet (RAM). It is the centralized write path for entity mutations.

**OS Model Role**:
- **Searcher**: Queries full SSD catalog for dedup/resolution (allowed per OS Model boundary rules).
- **Writer**: Mutations write-through — **synchronous** update to RAM Section 1, and **asynchronous** persistence to SSD.
- **Kernel Invoker**: `updateProfile()` calls `ContextBuilder.recordActivity()` (App → Kernel) to emit history events asynchronously.
- **Constraint**: Callers must NOT call `EntityRepository.save()` directly. All mutations go through EntityWriter.

Ensures consistent:
1. Alias registration (dedup)
2. Field update policies (append vs replace)
3. Source tracking (provenance)
4. **Change tracking** — profile changes (name, title, company) trigger history events

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [Entity Registry](../entity-registry/spec.md) | **SSD Storage** — The full entity catalog |
| [Session Context](../session-context/spec.md) | **RAM** — Section 1 holds active entity context |
| [Client Profile Hub](../client-profile-hub/spec.md) | **File Explorer** — Reads entity data for dashboards |

---

## OS Layer & Data Flow

### The Transformation

| Old Model (Silo) | New OS Model (RAM Application) |
|------------------|--------------------------------|
| EntityWriter calls `EntityRepository.save()` directly | Mutations write-through: **Synchronous** RAM + **Async** SSD |
| Callers don't know if entity is session-active | Mutations instantly update the active session context |
| `updateProfile()` only persists to SSD | `updateProfile()` synchronously updates RAM, async saves to SSD + emits Kernel history event |
| New entities invisible until next session | New entities immediately available in current session RAM |

### 1. Search & Dedup (SSD Allowed)

**Why SSD?** Entity dedup requires the **full catalog** — not just session-active entities. The OS Model explicitly allows this (see `os-model-architecture.md` L103: "Full entity search hits SSD").

- `findByAlias()` → SSD (EntityRepository)
- `getById()` for read-modify-write → SSD (EntityRepository)

### 2. Mutations (Write-Through)

**Trigger**: Any `save()` or `delete()` call.
**Action**: Write-Through (Atomic).

1. **Update RAM (Sync)**: If entity is in Session Section 1, update the in-memory reference immediately so subsequent LLM or logic turns see the new data.
2. **Persist to SSD (Async)**: Fire-and-forget write to `EntityRepository` (Room) via `AppScope`.

```mermaid
graph TD
    Caller[Scheduler / Coach / Analyst] -->|clue, resolvedId| EW[EntityWriter Application]
    EW -->|findByAlias| SSD[EntityRepository SSD]
    EW -->|1. Persist| SSD
    EW -->|2. Update RAM| RAM[SessionWorkingSet Section 1]
    EW -->|3. recordActivity| Kernel[ContextBuilder Kernel]
```

> [!NOTE]
> **Scheduler creates PERSON + ACCOUNT entities** for business-relevant contacts. Personal contacts (family, friends) are filtered at the LLM prompt level. See [Scheduler spec §CRM Entity Creation Policy](../scheduler/spec.md).

### 3. App → Kernel Callback

`updateProfile()` detects field changes and calls `ContextBuilder.recordActivity()` — this is an **Application invoking a Kernel service** to emit `MemoryEntry` history events. The Kernel persists these to Memory Center (SSD).

---

## The "One Currency" Contract (Project Mono)

EntityWriter no longer relies on legacy regex linters or LLM-translation guessing. It is driven by strict JSON deserialization against the `UnifiedMutation` (data class defined in `domain:core`). The `ProfileMutation` elements within that parsed payload dictate the exact entity mutations to perform.

## Architecture

```
Caller (UnifiedPipeline Linter deserializing UnifiedMutation)
       │ (Passes strict JSON-extracted fields)
       ▼
[EntityWriter.upsertFromClue()]
       │
       ├─ 1. Resolve ID (if null)
       │   └─ Resolution Cascade: resolvedId → findByAlias → findByDisplayName
       │
       ├─ 2. Load Existing (read-modify-write)
       │   └─ getById() → merge fields → save()
       │
       ├─ 3. Apply Field Policies
       │   ├─ displayName: Immutable (Canonical)
       │   ├─ aliasesJson: Immutable (Curated only)
       │   ├─ attributesJson: Upsert key
       │   ├─ _sourceJson: Track provenance
       │   └─ _last_seen: Update timestamp
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

### 1. Resolution & Dedup Logic (`upsertFromClue`)

**The Resolution Cascade** (Strict Order):

1. **Resolved ID** (High Confidence): If caller provides `resolvedId`, use it.
   - If ID not found (stale), fall back to step 2.
2. **Alias Match** (Curated): `EntityRepository.findByAlias(clue)`.
   - Exact match against pre-installed aliases (e.g., "孙工").
3. **Canonical Name Match** (Homophone Resolution): `EntityRepository.findByDisplayName(clue)`.
   - Exact match against `displayName` (e.g., "孙扬浩").
   - **Purpose**: Resolves ASR homophones (e.g., "孙阳浩") at runtime without polluting data.
4. **Miss**: Create NEW entity with `clue` as `displayName`.

**Action on Hit**:
- Load existing entity.
- Update source tracking (`_last_seen`).
- **DO NOT** overwrite `displayName` or auto-add `clue` to `aliasesJson`.

When `resolvedId` is PROVIDED:

1. **Load**: `getById(resolvedId)`.
2. **If found**: Update source tracking → `save()`.
3. **If NOT found (stale ID)**: Fall back to resolution cascade (treat as null resolvedId).

### 2. Read-Modify-Write Pattern (Critical)

```kotlin
// CORRECT — EntityWriter internals
suspend fun upsertFromClue(...): UpsertResult {
    val existingId = resolvedId ?: findExistingByAlias(clue)
    
    return if (existingId != null) {
        val existing = entityRepository.getById(existingId)
            ?: return createNew(clue, type, source)  // Stale ID fallback
        
        val merged = existing.copy(
            aliasesJson = existing.aliasesJson, // Immutable: Do NOT auto-add clue to aliases
            lastUpdatedAt = now()
            // displayName: Immutable (keep existing)
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
| `displayName` | **Canonical Evolution** | `upsertFromClue` NEVER overwrites. When `updateProfile` changes it, the old name is automatically pushed to `aliasesJson`. | `updateProfile` |
| `aliasesJson` | **Curated & Historical** | `upsertFromClue` NEVER adds aliases. Tracks manual additions + historical `displayName`s via FIFO. | `registerAlias` / `updateProfile` |
| `demeanorJson` | Upsert per key | `oldMap + newMap` | `updateAttribute` |
| `attributesJson` | Upsert per key | `oldMap + newMap` | `updateAttribute` |
| `metricsHistoryJson` | Append per key | `oldTimeSeries + newEntry` | `updateAttribute` |
| `relatedEntitiesJson` | Append (dedupe) | `(oldList + newId).distinct()` | Future wave |
| `decisionLogJson` | ~~Append-only~~ **Deprecated** | Superseded by `MemoryEntry` timeline | — |
| `nextAction` | **Replace** | Latest action overwrites. `null` clears. | `updateProfile` |

#### Tracked Profile Fields (Change-Aware)

These fields trigger a `MemoryEntry` history event when changed:

| Field | ActivityType | Example |
|-------|-------------|----------|
| `displayName` | `NAME_CHANGE` | 索尼娱乐集团 → SONY |
| `jobTitle` | `TITLE_CHANGE` | 销售经理 → 销售VP |
| `accountId` | `COMPANY_CHANGE` | 承时利和 → 华为 |
| `buyingRole` | `ROLE_CHANGE` | champion → economic_buyer |
| `dealStage` | `DEAL_STAGE_CHANGE` | proposal → negotiation |
| `nextAction` | `NEXT_ACTION_SET` | 下周安排回访 |

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
| **1.5** | Wiring | ✅ SHIPPED | Wire into `UnifiedPipeline` Scheduler path — creates PERSON + ACCOUNT entities for business-relevant contacts |
| **2** | Change-Aware Profile Management | ✅ SHIPPED | `updateProfile()`, `ProfileUpdateResult`, `ProfileChange`, history emission via `recordActivity()` |
| ~~3~~ | ~~Conflict Merge~~ | ❌ KILLED | See architectural decision below |
| **4** | **OS Model Upgrade** (RAM Application) | ✅ SHIPPED | Write-through to RAM Section 1 on all mutation methods + `recordActivity()` App→Kernel callback |
| **5** | **Alignment & Disambiguation** | ✅ SHIPPED | Curated Alias Model, Resolution Cascade, Entity Confirmation Flow |
| **Mono W2**| **The Linter Upgrade (The Bouncer)** | ✅ SHIPPED | Refactoring Linters to pure Type Checkers using strict JSON deserialization against `domain:core:UnifiedMutation`. No more regex guessing. |
| **Mono W5**| **The Async Loop (CQRS Engine)** | ✅ SHIPPED | Decoupled EntityWriter SSD mutation from RAM context update. RAM sync is instant, SSD save and heavy DB decode strings are pushed to background coroutines. |

### ~~Wave 3~~ Architectural Decision: No Merge UI Needed

**Superseded by Positive Code Drift (2026-03-11).** Pure deterministic "latest-write-wins" is not sufficient. Instead, we use **FIFO History Preservation**. Rationale:

1. **Every write is grounded** — User input → LLM → RelevancyLib (entity resolution) → EntityWriter. The agent never presumes; `resolvedId` comes from RelevancyLib.
2. **User corrections preserve context** — When a user says "His name is actually X", `updateProfile` changes the `displayName` but *appends* the old name to the `aliasesJson` array (FIFO, max 8). This ensures future fuzzy searches still find the entity using the old, misspelled/outdated context.
3. **History is preserved** — `recordActivity()` tracks every change (`承时利和 → 华为 → 承时利和`), making corrections visually auditable in the timeline. Additionally, the preserved `aliasesJson` keeps the entity robust against ASR errors over time.
4. **True duplicates are a dedup problem** — If two `entityId`s exist for the same person because alias matching failed, the fix is better RelevancyLib matching, not a manual merge UI.

### Wave 4 Scope (OS Model) — ✅ SHIPPED
- ✅ `upsertFromClue` / `registerAlias` / `updateAttribute` / `delete` write-through to RAM Section 1
- ✅ `updateProfile()` calls `ContextBuilder.recordActivity()` (App → Kernel)
- SSD reads for dedup remain (full catalog search requires SSD, per os-model-architecture.md)

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
    - `aliasesJson` unchanged by upsert (curated only via `registerAlias`)
    - `EntityRepository.save()` called exactly once per write
    - Zero regressions in existing read paths

- **Test Cases**:
    - [x] **New Entity**: `upsert("张总", null)` → `UpsertResult(isNew=true, displayName="张总")`
    - [x] **Existing (Resolved)**: `upsert("张总", "z-001")` → Updates z-001 alias, `isNew=false`
    - [x] **Existing (Alias Match)**: `upsert("张总", null)` → Finds via alias, updates, `isNew=false`
    - [x] **Field Preservation**: Upsert preserves existing `demeanorJson`, `metricsHistoryJson`
    - [x] **Alias Cap**: Add 9th alias → Oldest alias dropped (FIFO tail), cap at 8
    - [x] **Missing Entity**: `upsert("X", "nonexistent-id")` → Falls back to alias dedup
    - [x] **Blank Clue**: `upsert("", null)` → Throws `IllegalArgumentException`
    - [x] **Delete**: `delete("z-001")` → Entity removed, `getById` returns null
    - [x] **Delete Missing**: `delete("nonexistent")` → No-op

- **Deliverables**:
    - `EntityWriter.kt` (Interface + `ProfileUpdateResult`, `ProfileChange`)
    - `RealEntityWriter.kt` (Implementation — rewritten from spec)
    - `FakeEntityWriter.kt` (Test double)
    - `RealEntityWriterTest.kt` (18 tests — Wave 1 + Wave 2)

---

## Constants & Limits

```kotlin
const val MAX_ALIASES = 8
const val MAX_ATTRIBUTES = 20
```
