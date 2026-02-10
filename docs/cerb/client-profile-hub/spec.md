# Client Profile Hub

> **Cerb-compliant spec** — CRM hierarchy + client intelligence.  
> **OS Layer**: File Explorer

---

## Overview

Client Profile Hub is the **File Explorer** in the OS Model. It reads SSD directly for dashboard and history views — NOT session-scoped.

Provides HubSpot-style CRM data hierarchy (Account → Contact → Deal) and unified timeline aggregation. Separates **on-demand profile data** from **always-loaded RL context**.

**OS Model Role**:
- **Reads SSD directly** — no RAM/session dependency
- **Not session-scoped** — shows historical data across all sessions
- **Cross-layer read** — `FocusedContext.habitContext` pulls from RL Module (RAM Application) via `getHabitContext()`. Acceptable because this is on-demand enrichment, not session state.

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [RL Module](../rl-module/spec.md) | Habit learning from structured output |
| [User Habit](../user-habit/spec.md) | Habit storage (entityId, key, confidence) |
| [Entity Registry](../entity-registry/spec.md) | Entity disambiguation, alias mapping |

---

## Entity Hierarchy

```
Account (公司)
├── Contact (联系人) → accountId FK
├── Deal (交易) → accountId + primaryContactId FK
└── Activity (活动) → derived from MemoryEntry
```

---

## Domain Models

### EntityType (Extended)

```kotlin
enum class EntityType {
    PERSON,       // Legacy (non-CRM)
    PRODUCT,
    LOCATION,     // Deprecated as standalone
    EVENT,        // Legacy (non-deal events)
    
    // CRM Types
    ACCOUNT,      // Company
    CONTACT,      // Business contact
    DEAL          // Sales opportunity
}
```

### EntityEntry (CRM Fields)

```kotlin
// New nullable fields for CRM types
val accountId: String? = null,           // FK to Account
val primaryContactId: String? = null,    // FK to Contact (for Deals)
val jobTitle: String? = null,            // Contact title
val buyingRole: String? = null,          // economic_buyer, champion, etc.
val dealStage: String? = null,           // Pipeline stage
val dealValue: Long? = null,             // Amount (minor units)
val closeDate: String? = null            // ISO 8601
```

### EntitySnapshot (Lightweight Summary)

```kotlin
data class EntitySnapshot(
    val entityId: String,
    val displayName: String,
    val entityType: EntityType,
    val lastActivity: UnifiedActivity?
)
```

### FocusedContext (Deep Dive)

```kotlin
data class FocusedContext(
    val entity: EntityEntry,
    val relatedContacts: List<EntityEntry>,
    val relatedDeals: List<EntityEntry>,
    val timeline: List<UnifiedActivity>,
    val habitContext: HabitContext  // from RL module
)
```

### UnifiedActivity (Timeline View)

```kotlin
data class UnifiedActivity(
    val id: String,
    val type: ActivityType,
    val timestamp: Long,
    val summary: String,
    val location: String?,     // Plain string (NOT entity)
    val assetId: String?,      // Artifact traceability
    val relatedEntityIds: List<String>
)

enum class ActivityType {
    MEETING, CALL, NOTE, ARTIFACT_GENERATED, DEAL_STAGE_CHANGE, TASK_COMPLETED,
    
    // Change-Aware Profile Tracking (EntityWriter Wave 2)
    NAME_CHANGE,       // Account/Contact renamed (e.g., 索尼娱乐集团 → SONY)
    TITLE_CHANGE,      // Contact promoted/changed role
    COMPANY_CHANGE,    // Contact moved companies
    ROLE_CHANGE,       // Buying role changed (champion → economic_buyer)
    
    // RL Module Insights
    INSIGHT_LEARNED    // RL discovered high-confidence habit
}
```

---

## Context Layers

| Layer | Access Pattern | Used By | OS Model Note |
|-------|---------------|---------|---------------|
| **Quick Context** | Always-loaded | Context Builder (every LLM call) | Kernel populates RAM with this |
| **Focused Context** | On-demand | Agent (when entity detected) | File Explorer reads SSD + cross-layer habit read |
| **Full Context** | Rare | Deep research, export | File Explorer reads SSD directly |

---

## Interface

```kotlin
interface ClientProfileHub {
    suspend fun getQuickContext(entityIds: List<String>): QuickContext
    suspend fun getFocusedContext(entityId: String): FocusedContext
    suspend fun getUnifiedTimeline(entityId: String): List<UnifiedActivity>
}

data class QuickContext(
    val entitySnapshots: Map<String, EntitySnapshot>,
    val recentActivities: List<UnifiedActivity>,
    val suggestedNextSteps: List<String>
)
```

---

## CRM Export Mapping

| CSV Column | Source |
|------------|--------|
| Account_Name | `Account.displayName` |
| Contact_Name | `Contact.displayName` |
| Contact_Title | `Contact.jobTitle` |
| Deal_Name | `Deal.displayName` |
| Deal_Stage | `Deal.dealStage` |
| Deal_Amount | `Deal.dealValue` |
| Activity_Type | `Activity.type` |
| Activity_Location | `Activity.location` |

**Direct field access. No JSON parsing required.**

---

## Wave Plan

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + Domain Models + Fake | ✅ SHIPPED |
| **2** | Timeline Aggregation | ✅ SHIPPED |
| **3** | CRM Export Integration | 🔲 |
| **4** | **OS Model Labeling** (File Explorer) | ✅ SHIPPED |

> **Note**: CRM schema (ACCOUNT, CONTACT, DEAL types + EntityEntry CRM fields) already shipped in Entity Registry Wave 2.5.

> **Shipped 2026-02-08 (Wave 1)**: ClientProfileHub interface, FocusedContext, QuickContext, EntitySnapshot, UnifiedActivity, FakeClientProfileHub

> **Shipped 2026-02-08 (Wave 2)**: Entity-tagged MemoryEntry via `structuredJson`, `MemoryRepository.getByEntityId()`, `getUnifiedTimeline()` with MemoryEntry→UnifiedActivity mapping, `ContextBuilder.record*()` now suspend + persists to MemoryRepository
