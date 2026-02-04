# Relevancy Library

> **Cerb-compliant spec** — Entity lookup and disambiguation.

---

## Overview

Manages B2B sales entity tracking (People, Products, Locations, Events) with O(1) lookup and LLM-first disambiguation.

---

## Domain Models

### RelevancyEntry

```kotlin
data class RelevancyEntry(
    val entityId: String,        // "z-001" (Person), "p-042" (Product)
    val entityType: EntityType,  // PERSON, PRODUCT, LOCATION, EVENT
    val displayName: String,     // Canonical name ("张伟")
    val aliasesJson: String,     // ["张总", "张董事长"]
    val demeanorJson: String,    // {"communication_style": "formal"}
    val attributesJson: String,  // {"budget": "2M"} (latest snapshot)
    val metricsHistoryJson: String,  // Time-series for viz
    val relatedEntitiesJson: String,
    val decisionLogJson: String,
    val lastUpdatedAt: Long,
    val createdAt: Long
)

enum class EntityType { PERSON, PRODUCT, LOCATION, EVENT }
```

### AliasMapping (for disambiguation)

```kotlin
data class AliasMapping(
    val alias: String,           // "张总"
    val entityId: String,        // "z-001"
    val confirmationCount: Int,  // User confirmations
    val lastConfirmedAt: Long,
    val sessionContexts: List<String>  // ["A3项目", "华东区"]
)
```

---

## Field Update Policies

| Field | Policy | Rationale |
|-------|--------|-----------|
| `displayName` | First-write-wins | Canonical name shouldn't change |
| `aliasesJson` | Append (dedupe) | Growing list, bounded 3-8 items |
| `demeanorJson` | Upsert per key | Latest observation wins |
| `attributesJson` | Upsert per key | Current state snapshot |
| `metricsHistoryJson` | Append per key | Time-series, never overwrite |
| `decisionLogJson` | Append-only | History for Rethink learning |

---

## Entity Disambiguation (LLM-First)

When voice input contains ambiguous references (e.g., "张总" matches multiple people):

### Resolution Flow

```
Phase 1 Parse → person: "张总" (raw clue, unresolved)
       │
       ▼
Phase 2: LLM synthesizes with context
├─ Conversation history (recent mentions)
├─ RelevancyLib query (known entities with this alias)
├─ Session context (current topic)
       │
       ▼
LLM Decision:
├─ HIGH confidence → Use resolved entity
├─ MEDIUM confidence → Disclose + [更改] button
└─ LOW/UNKNOWN → Ask user: "请问是哪位张总？"
```

### Design Principles

| Principle | Rationale |
|-----------|-----------|
| **LLM is source of truth** | Has conversation context, can reason |
| **DB is for learning** | Stores user confirmations for future hints |
| **No Kotlin scoring** | LLM handles disambiguation naturally |
| **Cold start = ask user** | Don't guess when uncertain |

### Reinforcement (Future Wave)

After user confirms entity, write to `AliasMapping` for future LLM hints:

| Event | Action |
|-------|--------|
| User confirms | Store alias → entityId mapping |
| User corrects | Update mapping |

---

## Metrics History (Time-Series)

Stores in `metricsHistoryJson` for visualization:

| Metric | Type | Unit Examples |
|--------|------|---------------|
| `budget` | Integer (minor units) | CNY, USD, HKD |
| `price_quoted` | Integer (minor units) | CNY, USD |
| `quantity` | Integer | pcs, carton, kg |
| `deal_cycle` | Integer | days, weeks, months |
| `deal_stage` | Enum | qualification, proposal |

**Currency rule:** Store in **minor units** (分/cents).

---

## Wave Plan

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Core Model + Repository | ✅ Complete |
| **2** | LLM Disambiguation Flow | ✅ SHIPPED |
| **3** | CRM Hierarchy → [Client Profile Hub](../client-profile-hub/spec.md) | 🔲 |

