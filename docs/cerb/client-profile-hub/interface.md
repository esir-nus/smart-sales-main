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

### getUnifiedTimeline

```kotlin
suspend fun getUnifiedTimeline(entityId: String): List<UnifiedActivity>
```

Aggregates timeline activities for an entity (includes all related contacts for Accounts).

---

## Output Types

```kotlin
data class QuickContext(
    val entitySnapshots: Map<String, EntitySnapshot>,
    val recentActivities: List<UnifiedActivity>,
    val suggestedNextSteps: List<String>
)

data class EntitySnapshot(
    val entityId: String,
    val displayName: String,
    val entityType: EntityType,
    val lastActivity: UnifiedActivity?
)

data class FocusedContext(
    val entity: EntityEntry,
    val relatedContacts: List<EntityEntry>,
    val relatedDeals: List<EntityEntry>,
    val timeline: List<UnifiedActivity>,
    val habitContext: HabitContext
)

data class UnifiedActivity(
    val id: String,
    val type: ActivityType,
    val timestamp: Long,
    val summary: String,
    val location: String?,
    val assetId: String?,
    val relatedEntityIds: List<String>
)

enum class ActivityType {
    MEETING, CALL, NOTE, ARTIFACT_GENERATED, DEAL_STAGE_CHANGE, TASK_COMPLETED,

    // Change-Aware Profile Tracking
    NAME_CHANGE,       // Account/Contact renamed
    TITLE_CHANGE,      // Contact promoted/changed role
    COMPANY_CHANGE,    // Contact moved companies
    ROLE_CHANGE,       // Buying role changed

    // RL Module Insights
    INSIGHT_LEARNED    // RL discovered high-confidence habit
}
```

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Access `RelevancyEntry` directly | Use `ClientProfileHub` methods |
| Parse `MemoryEntry.entitiesJson` manually | Use `getUnifiedTimeline()` |
| Query habit data here | Use `ReinforcementLearner.getHabitContext()` |
| Assume entity exists | Check for null/empty returns |
| Depend on session state (SessionWorkingSet) | CRM Hub reads SSD directly — no session dependency |
