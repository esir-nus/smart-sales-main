# Client Profile Hub — Interface

> **Consumer contract** for CRM hierarchy and context queries.  
> **OS Layer**: File Explorer (reads SSD directly, NOT session-scoped)

---

## Methods

### getQuickContext

```kotlin
suspend fun getQuickContext(entityIds: List<String>): QuickContext
```

Returns a "6-second glance" context for the given entities.

| Input | Type | Description |
|-------|------|-------------|
| `entityIds` | `List<String>` | Entity IDs to query |

| Output | Type |
|--------|------|
| `QuickContext` | Snapshot + recent activities + suggestions |

---

### getFocusedContext

```kotlin
suspend fun getFocusedContext(entityId: String): FocusedContext
```

Returns deep dive context for a single entity (Account/Contact/Deal).

---

### observeProfileActivityState

```kotlin
suspend fun observeProfileActivityState(entityId: String): Flow<ProfileActivityState>
```

Aggregates timeline activities and actionable tasks for an entity (includes all related contacts for Accounts).

---

## Output Types

```kotlin
data class QuickContext(
    val entitySnapshots: Map<String, EntitySnapshot>,
    val recentActivities: List<com.smartsales.prism.domain.memory.MemoryEntry>,
    val suggestedNextSteps: List<String>
)

data class EntitySnapshot(
    val entityId: String,
    val displayName: String,
    val entityType: EntityType,
    val lastActivity: com.smartsales.prism.domain.memory.MemoryEntry?
)

data class FocusedContext(
    val entity: EntityEntry,
    val relatedContacts: List<EntityEntry>,
    val relatedDeals: List<EntityEntry>,
    val activityState: ProfileActivityState,
    val habitContext: HabitContext
)

data class ProfileActivityState(
    // The Actionable feed is pure, unmodified Scheduler Tasks. 
    val actionableItems: List<com.smartsales.prism.domain.scheduler.ScheduledTask>,
    
    // The Factual feed is pure, unmodified Memory Entries. 
    val factualItems: List<com.smartsales.prism.domain.memory.MemoryEntry>
)


```

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Access `RelevancyEntry` directly | Use `ClientProfileHub` methods |
| Parse `MemoryEntry.entitiesJson` manually | Use `observeProfileActivityState()` |
| Query habit data here | Use `ReinforcementLearner.getHabitContext()` |
| Assume entity exists | Check for null/empty returns |
| Depend on session state (SessionWorkingSet) | CRM Hub reads SSD directly — no session dependency |
