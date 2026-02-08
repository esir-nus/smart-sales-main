# Session Context System

> **Cerb-compliant spec** — In-memory caching and entity state management for `ContextBuilder`.

---

## Overview

The Session Context System is an **intelligent caching layer** that optimizes entity retrieval within a conversation session. It replaces "search every turn" with "resolve once, cache forever (in session)."

**Lifecycle**: Created when a session starts, destroyed when session ends or chat is deleted. No expiry timer — cache is valid for the entire session.

---

## Dependencies (via Cerb Interfaces)

| Interface | Cerb Shard | Purpose |
|-----------|------------|---------|
| `EntityRepository.findByAlias()` | [entity-registry](../entity-registry/interface.md) | Resolve alias → entityId |
| `EntityRepository.getById()` | [entity-registry](../entity-registry/interface.md) | Load entity data |
| `MemoryRepository.search()` | [memory-center](../memory-center/interface.md) | First-turn memory retrieval |

---

## Domain Models

### SessionContext

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

## Wave Plan

| Wave | Focus | Status | Deliverables |
|---|---|---|---|
| **1** | **Skeleton** (Data classes + wiring) | ✅ SHIPPED | `SessionContext.kt`, `EntityTrace.kt`, `EntityState.kt` |
| **2** | **Path Indexing** (Cache hit logic) | ✅ SHIPPED | `resolveAlias()`, `cacheAlias()` in `SessionContext.kt` |
| **3** | **Smart Triggers** (State-driven loading) | ✅ SHIPPED | `shouldLoadData()`, `markActive()` in `SessionContext.kt` |

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
