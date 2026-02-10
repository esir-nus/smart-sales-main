# Session Working Set System

> **Cerb-compliant spec** — In-memory workspace and caching for `ContextBuilder`.  
> **OS Layer**: Kernel

---

## Overview

The Session Working Set System is the **per-session workspace** (the "RAM") that all pipeline modules operate through. It evolved from the original Session Context (alias cache) into a unified workspace with three sections.

**Lifecycle**: Created when a session starts, destroyed when session ends or chat is deleted. No expiry timer — valid for the entire session.

> **OS Model Reference**: [`os-model-architecture.md`](../../specs/os-model-architecture.md)  
> **Note**: `SessionContextMenu.kt` in the UI layer is an unrelated component (session management menu), not part of this system.

---

## Dependencies (via Cerb Interfaces)

| Interface | Cerb Shard | Purpose |
|-----------|------------|---------|
| `EntityRepository.findByAlias()` | [entity-registry](../entity-registry/interface.md) | Resolve alias → entityId |
| `EntityRepository.getById()` | [entity-registry](../entity-registry/interface.md) | Load entity data |
| `MemoryRepository.search()` | [memory-center](../memory-center/interface.md) | First-turn memory retrieval |

---

## Domain Models

### SessionContext (evolves into SessionWorkingSet)

The current `SessionContext` data class evolves into the `SessionWorkingSet` — the RAM that all Applications read/write through.

```kotlin
data class SessionContext(
    val sessionId: String,
    val entityStates: MutableMap<String, EntityTrace> = mutableMapOf(),
    val pathIndex: MutableMap<String, String> = mutableMapOf(),
    val turnCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

### EntityTrace & State Machine

Tracks the lifecycle of an entity *within this session*.

```kotlin
data class EntityTrace(
    val entityId: String,
    val state: EntityState,
    val confidence: Float
)

enum class EntityState {
    UNKNOWN,    // Detected in text, not resolved to ID
    MENTIONED,  // Resolved to ID, but data not loaded into context
    ACTIVE      // Data loaded in EnhancedContext, valid for entire session
}
```

### State Transitions

| From | To | Trigger | Action |
|---|---|---|---|
| (null) | UNKNOWN | NER detection | Log "Unknown entity: [alias]" |
| UNKNOWN | MENTIONED | Disambiguation | Store `pathIndex[alias] = id` |
| MENTIONED | ACTIVE | `shouldLoadData()`=true | Fetch `EntityRepository.getById()`, add to Context |

> **No STALE state.** Once ACTIVE, an entity stays ACTIVE for the entire session. Cache is cleared only when the session is deleted.

---

## Path Indexing (The Cache)

**Problem**: User says "call 张总" 10 times.  
**Old Way**: 10x `search("张总")` → 10x DB hits.  
**New Way**:
1. Turn 1: "call 张总" → `findByAlias("张总")` → Resolve to `p-001`.
2. Store `pathIndex["张总"] = "p-001"`.
3. Turn 2-10: "call 张总" → `pathIndex["张总"]` → `p-001`. **Zero DB hits.**

---

## Edge Cases

| Scenario | Behavior |
|---|---|
| **App backgrounded** | Cache survives if within ViewModel scope; destroyed on process death |
| **Session deleted** | Cache destroyed — all entity states cleared |
| **pathIndex growth** | Bounded at 50 entries per session (LRU eviction) |
| **Entity updated externally** | Not detected mid-session; consistent within session scope |
| **Alias collision** | Multiple entityIds for same alias → return list, let LLM disambiguate |

---

## SessionWorkingSet Structure (The RAM)

> `SessionContext` evolves into `SessionWorkingSet` — the per-session workspace.
> All RAM Applications read/write through this structure. Only the Kernel (ContextBuilder) manages its lifecycle.

### Section 1: Distilled Memory

Resolved entity pointers + active memory references. Built by ContextBuilder during `build()`.
Drives auto-population of Section 3.

| Field | Type | Source | Existing? |
|-------|------|--------|-----------|
| `entityStates` | `Map<String, EntityTrace>` | Entity resolution | ✅ Wave 3 |
| `pathIndex` | `Map<String, String>` | Alias caching | ✅ Wave 2 |
| `memoryHits` | `List<MemoryHit>` | `MemoryRepository.search()` | ✅ In `build()` |
| `entityContext` | `Map<String, EntityRef>` | `buildWithClues()` | ✅ In `buildWithClues()` |

### Section 2: User Habits (Global)

User-level preferences. Loaded **once at session start** by the Kernel.

| Field | Type | Source |
|-------|------|--------|
| `userHabits` | `HabitContext` | `ReinforcementLearner.getHabitContext(entityIds = null)` |

### Section 3: Client Habits (Contextual)

Per-entity habits. **Auto-populated** when entities appear in Section 1.

| Field | Type | Source |
|-------|------|--------|
| `clientHabits` | `HabitContext` | `ReinforcementLearner.getHabitContext(entityIds = activeEntityIds)` |

### Auto-Population Rule

**Trigger**: When `markActive(entityId)` transitions an entity to ACTIVE state, the Kernel immediately queries the RL Module for that entity's habits and populates Section 3.

```
markActive(entityId) 
  → entityStates[entityId] = ACTIVE        (Section 1 update)
  → RL.getHabitContext(activeEntityIds)     (Section 3 auto-populate)
```

**This eliminates the Coach Mode bug**: Coach previously passed `entityIds = null` because it had no entity context. With auto-population, Section 3 is always current — any Application reading habits gets the right data without knowing about entity resolution.

---

## Interaction Rules

| Rule | Description |
|------|-------------|
| **Write-Through** | Every mutation to Sections 2/3 persists to SSD (Room) immediately. No deferred flush. |
| **Kernel Owns Lifecycle** | Only `ContextBuilder` creates, destroys, and populates the Working Set. Applications don't load data directly from repos. |
| **Concurrent Reads** | Multiple Applications can read from the Working Set concurrently. Thread-safety required. |
| **SSD = Source of Truth** | Working Set is rebuilt from SSD on session start. If process dies, no data loss. |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|---|---|---|---|
| **1** | **Skeleton** (Data classes + wiring) | ✅ SHIPPED | `SessionContext.kt`, `EntityTrace.kt`, `EntityState.kt` |
| **2** | **Path Indexing** (Cache hit logic) | ✅ SHIPPED | `resolveAlias()`, `cacheAlias()` in `SessionContext.kt` |
| **3** | **Smart Triggers** (State-driven loading) | ✅ SHIPPED | `shouldLoadData()`, `markActive()` in `SessionContext.kt` |
| **4** | **SessionWorkingSet** (OS Model upgrade) | ✅ SHIPPED | 3-section workspace, `getCombinedHabitContext()`, `typealias SessionWorkingSet` |

### 🔬 Wave 1: Skeleton

**Goal**: Data classes exist and `RealContextBuilder` holds a `SessionContext` instance.

- **Ship Criteria**: `SessionContext` wired into `RealContextBuilder`, tests pass
- **Test Cases**:
    - [ ] L1: `SessionContext` initializes with empty maps
    - [ ] L1: `resetSession()` clears `SessionContext`
    - [ ] L1: `turnCount` increments on each `build()` call

### 🔬 Wave 2: Path Indexing

**Goal**: O(1) entity resolution for repeat mentions.

- **Ship Criteria**: Second mention of same alias uses cache, zero DB hit
- **Test Cases**:
    - [ ] L1: First alias lookup → DB hit → cached
    - [ ] L1: Second alias lookup → cache hit → no DB call
    - [ ] L1: Cache eviction at 50 entries (LRU)

### 🔬 Wave 3: Smart Triggers

**Goal**: State-driven data loading decisions. Session-scoped — no expiry timer.

- **Ship Criteria**: `shouldLoadData()` returns correct boolean based on entity state
- **Test Cases**:
    - [x] L1: ACTIVE entity → false (already loaded, stays valid for entire session)
    - [x] L1: MENTIONED entity → true (first load)
    - [x] L1: Unknown entityId (no trace) → true (new entity)
    - [x] L1: UNKNOWN state → false (cannot load without resolved ID)

---

## File Map

| Layer | Files |
|-------|-------|
| **Domain** | `SessionContext.kt`, `EntityTrace.kt`, `EntityState.kt` |
| **Data** | Wired inside `RealContextBuilder.kt` (no separate impl needed) |
| **Test** | `SessionContextTest.kt`, `RealContextBuilderMemoryTest.kt` |

---

## Verification Commands

```bash
# Build check
./gradlew :app-prism:compileDebugKotlin

# Run session context tests
./gradlew :app-prism:testDebugUnitTest --tests "*SessionContext*"

# Run context builder tests
./gradlew :app-prism:testDebugUnitTest --tests "*RealContextBuilder*"
```
