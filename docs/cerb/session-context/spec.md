# Session Context System

> **Cerb-compliant spec** — Short-Term Memory (STM) workspace and caching for `ContextBuilder`.  
> **OS Layer**: Kernel  
> **State**: ✅ SHIPPED

---

## Overview

The Session Context System (now **Session Working Set**) is the **Short-Term Memory (STM)** of the agent. It acts as the ephemeral **per-session workspace** (the "RAM") that all pipeline modules operate through. It implements the OS Model Architecture with three distinct sections.

The STM `SessionWorkingSet` is dynamically populated using a **Lazy Loading** strategy. When the `LightningRouter` signals `info_sufficiency: true`, the ContextBuilder queries the Long-Term Memory (LTM - `EntityRegistry`) to fetch only the relevant entities and loads them into the STM. On a mobile device, we cannot load all 10,000 CRM entities into RAM.

**Lifecycle**: Created and managed by the Kernel (`ContextBuilder`) when a session starts. Destroyed when session ends or resets.

> **OS Model Reference**: [`os-model-architecture.md`](../../specs/os-model-architecture.md)

---

## Architecture & Components

### 1. SessionWorkingSet (The RAM)
The core data structure holding the session state. It is **not** a data class anymore, but a domain object with logic.

**Key Responsibilities**:
- **Section 1 (Distilled Memory)**: Path Index, Entity States, Entity Knowledge Graph.
- **Section 2 (User Habits)**: Global preferences loaded once at session start.
- **Section 3 (Client Habits)**: Contextual preferences auto-loaded when entities become ACTIVE.
- **Alias Resolution**: Caching alias → ID mappings.
- **State Tracking**: Determining when to load entity data.

### 2. ContextBuilder (The Kernel)
The only component allowed to instantiate and write to `SessionWorkingSet`.

**Key Responsibilities**:
- **Session Lifecycle**: Create/Reset working set.
- **Lazy Loading (ContextDepth)**: Support `MINIMAL`, `DOCUMENT_QA`, and `FULL` build depths to save tokens and SSD reads.
- **Read**: Expose `EnhancedContext` to Applications (read-only snapshot).
- **Write**: Implement `KernelWriteBack` for persistence synchronization.
- **Turn Counting**: Manage session turn metadata.

### 3. KernelWriteBack (The Write Channel)
Interface for `EntityWriter` to synchronize SSD writes back to RAM.

**Contract**:
- `suspend` methods (no blocking).
- `updateEntityInSession`: Syncs Section 1 & 3 after SSD upsert.
- `removeEntityFromSession`: Syncs removal.
- `recordActivity`: Logs historical events.

---

## Domain Models

### SessionWorkingSet

```kotlin
class SessionWorkingSet(
    val sessionId: String,
    val createdAt: Long
) {
    // Section 1: Distilled Memory
    val entityStates: MutableMap<String, EntityTrace>
    val pathIndex: MutableMap<String, String>        // Alias cache
    var entityKnowledge: String?                     // Knowledge Graph JSON (Rendered)
    val entityCache: MutableMap<String, EntityEntry> // RAM Cache: Delta Loading Source
    var scheduleContext: String?                     // Sticky Notes: top 3 upcoming tasks
    var documentContext: String?                     // Transient Document Artifact Payload
    val entityContext: MutableMap<String, EntityRef> // Entity Pointers
    
    // Section 2: User Habits
    var userHabitContext: HabitContext?
    
    // Section 3: Client Habits
    var clientHabitContext: HabitContext?
}
```

### EntityTrace

Tracks the lifecycle of an entity *within this session*.

```kotlin
enum class EntityState {
    UNKNOWN,    // Detected but not resolved
    MENTIONED,  // Resolved but data not loaded
    ACTIVE      // Data loaded, valid for session
}
```

---

## Interaction Rules

| Rule | Description |
|------|-------------|
| **Write-Through** | EntityWriter MUST call `KernelWriteBack` after every SSD write. RAM Section 1 & 3 are updated immediately. |
| **Kernel Owns RAM** | Only `ContextBuilder` (Kernel) holds reference to `SessionWorkingSet`. Apps see only `EnhancedContext`. |
| **Context Depth (Lazy Loading)** | `MINIMAL`: History only. `DOCUMENT_QA`: +Audio Transcript. `FULL`: 3-Section Architecture (Habits, Sticky Notes, CRM DB). |
| **3-Section Loading (FULL)** | S2 loads once (Turn 1). S1 Knowledge loads once (Turn 1). S3 loads dynamically on `markActive()`. |
| **Concurrency** | Kernel uses `Mutex` to serialize writes to the Working Set. |

---

## Wave Plan & Status

| Wave | Focus | Status | Deliverables |
|---|---|---|---|
| **1** | **Skeleton** | ✅ SHIPPED | `SessionWorkingSet.kt`, `EntityTrace.kt` |
| **2** | **Path Indexing** | ✅ SHIPPED | `pathIndex`, `resolveAlias()` |
| **3** | **Smart Triggers** | ✅ SHIPPED | `shouldLoadData()`, `markActive()` |
| **4** | **OS Model Upgrade** | ✅ SHIPPED | `KernelWriteBack`, 3-Section Architecture, `RealContextBuilder` rewrite |
| **5** | **Context Compression** | ✅ SHIPPED | `ContextDepth` enum, Lazy Loading for NOISE/QA routing |

---

## Verification Commands

```bash
# Build check
./gradlew :app-prism:compileDebugKotlin

# Run working set unit tests
./gradlew :app-prism:testDebugUnitTest --tests "*SessionWorkingSetTest*"

# Run kernel integration tests
./gradlew :app-prism:testDebugUnitTest --tests "*RealContextBuilder*"
```
