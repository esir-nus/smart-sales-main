# Memory Center

> **Cerb-compliant spec** — Self-contained, all content inline.

---

## Overview

Memory Center manages persistent storage and retrieval of user interactions, entity information, and scheduled tasks. Uses a **Two-Zone Model** for lifecycle management and a **Relevancy Library** for O(1) entity lookup.

---

## Two-Zone Model

| Zone | Criteria | Contents |
|------|----------|----------|
| **Hot** | `isArchived = false` OR `scheduledAt` within 14 days | Active entries, upcoming/recent tasks |
| **Cement** | `isArchived = true` AND `scheduledAt` > 14 days ago | Completed entries, old schedules |

**Same schema, different flag.** Entries age from Hot → Cement via background compaction.

**Hot Zone includes:**
- All active entries (`isArchived = false`)
- All scheduled items within 14 days (past or future)
- **Excludes:** Inspirations (standalone notes)

---

## Domain Models

### MemoryEntry

```kotlin
data class MemoryEntry(
    val id: String,
    val workflow: String,        // COACH, ANALYST, SCHEDULER
    val title: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val sessionId: String,
    val outcomeStatus: String?,  // ONGOING, SUCCESS, PARTIAL, FAILED
    
    // Content & Structure
    val contentWithMarkup: String,   // Raw LLM output with tags
    val displayContent: String,      // Clean UI text
    val entitiesJson: String,        // Parsed entity IDs
    val structuredJson: String?,     // Extracted entities (JSON)
    
    // JSON blobs
    val artifactsJson: String?,      // ArtifactMeta (file refs)
    val outcomeJson: String?,        // Outcome with deliverables
    val payloadJson: String          // WorkflowPayload (mode-specific)
)
```

**Workflow Payloads** (stored in `payloadJson`):

| Workflow | Key Fields |
|----------|------------|
| **Coach** | `messages: List<ChatMessage>`, `topic: String?` |
| **Analyst** | `chapters: List<AnalysisChapter>`, `keyInsights: List<String>` |
| **Scheduler** | `scheduledAt: Instant?`, `priority: Priority`, `status: TaskStatus` |

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

## Entity Disambiguation

When voice input contains ambiguous references (e.g., "张总" matches multiple people):

### Scoring Algorithm

```
score = (confirmationCount × 0.4)
      + (recencyDecay(lastConfirmedAt) × 0.3)
      + (contextMatch(currentSession) × 0.3)
```

| Component | Weight | Notes |
|-----------|--------|-------|
| `confirmationCount` | 40% | Bounded at 20 |
| `recencyDecay` | 30% | `1.0 - min(days/90, 1.0)` |
| `contextMatch` | 30% | Jaccard similarity |

### Resolution Rules

- **1 candidate** → Auto-resolve
- **Multiple, top > 0.85 AND second < 0.3** → Auto-resolve with disclosure + [更改] button
- **Otherwise** → Show picker ordered by score

### Reinforcement

| Event | Action |
|-------|--------|
| User picks from picker | `+1` count, update timestamp |
| User overrides auto | `-1` wrong, `+1` correct |

### NotFound Handling (Hybrid Approach)

When `EntityResolver.resolve(alias)` returns `NotFound`:

```
Pipeline Level (NOT in EntityResolver):

1. Clarification UI → "我还不认识「张总」，请问是哪位？"
   └─ User provides: "张伟，华东区销售总监"

2. Entity Onboarding → Create new RelevancyEntry
   └─ displayName: "张伟"
   └─ aliases: ["张总"]
   └─ attributes: {"role": "销售总监", "region": "华东区"}

3. Retry Resolution → EntityResolver.resolve("张总") = AutoResolved
```

**Design Principles**:
- **EntityResolver stays pure** — No LLM dependency, just Kotlin lookup
- **NotFound triggers clarification** — Pipeline asks user, not LLM guess
- **New entities are onboarded** — Next time, Kotlin fast path works
- **LLM is NOT used for disambiguation** — Too slow, too unpredictable

**Pipeline State for NotFound**:
```kotlin
sealed class ResolutionResult {
    data class AutoResolved(val entry: RelevancyEntry) : ResolutionResult()
    data class AmbiguousMatches(val candidates: List<RelevancyEntry>) : ResolutionResult()
    data object NotFound : ResolutionResult()  // Triggers onboarding flow
}
```

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

## ScheduleBoard (Conflict Index)

A **materialized view** optimized for fast time conflict detection. Hardcoded Kotlin logic — no LLM needed.

### ScheduleItem

```kotlin
data class ScheduleItem(
    val entryId: String,
    val title: String,
    val scheduledAt: Long,
    val durationMinutes: Int,         // Inferred or user-provided
    val durationSource: DurationSource,
    val conflictPolicy: ConflictPolicy,
    val participants: List<String>,   // Entity IDs
    val location: String?             // Entity ID
)

enum class DurationSource {
    USER_SET,      // User explicitly said "1小时"
    INFERRED,      // LLM guessed from task type
    FOLLOW_UP,     // Agent asked, user clarified
    DEFAULT        // System default (30 min)
}

enum class ConflictPolicy {
    EXCLUSIVE,     // Normal — conflicts with overlaps
    COEXISTING,    // Can run parallel — user marked "共存"
    BACKGROUND     // Low priority — auto-yield to new tasks
}
```

### Duration Acquisition

Users rarely provide end times proactively. Duration is acquired through:

| Source | Trigger | Example |
|--------|---------|---------|
| **Explicit** | User says duration | "开会半小时" → 30 min |
| **Inferred** | LLM guesses from task type | "开会" → 60 min |
| **Follow-up** | Agent asks for clarity | "这个会议大概多久？" |
| **Learned** | User Pattern (UserHabit) | User's meetings average 45 min |

**Follow-up also catches user's time calculation errors** — e.g., user thinks 3-5pm is "2 hours" but schedules something at 4pm.

### Duration Inference Rules

| Task Type | Default Duration |
|-----------|-----------------|
| 开会/会议 | 60 min |
| 电话/通话 | 15 min |
| 午餐/晚餐 | 60 min |
| 出差 | 480 min (full day) |
| 提醒/reminder | 5 min (point-in-time) |
| 拜访客户 | 120 min |

### Conflict Check (Hardcoded Kotlin)

```kotlin
fun checkConflict(start: Long, durationMin: Int): ConflictResult {
    val proposedEnd = start + (durationMin * 60_000L)
    
    val overlaps = getDay(start.toLocalDate())
        .filter { slot ->
            slot.conflictPolicy == EXCLUSIVE &&
            slot.scheduledAt < proposedEnd && 
            start < slot.scheduledAt + (slot.durationMinutes * 60_000L)
        }
    
    return when {
        overlaps.isEmpty() -> ConflictResult.Clear
        else -> ConflictResult.Conflict(overlaps)
    }
}
```

---

## Two-Phase Scheduler Pipeline

Scheduling tasks follows a **gated pipeline** — Phase 2 only executes after Phase 1 is fully resolved.

```
PHASE 1: CONFLICT CHECK (Gate)
├─ 1.1 Basic Parse (LLM) — time, entity alias, action
├─ 1.2 ScheduleBoard Check (Kotlin) — hardcoded overlap detection
├─ 1.3 Conflict Resolution (if conflict) — LLM proposes options
│      Options: [调整时间] [保持共存] [取消原任务]
├─ 1.4 User Confirmation — wait for user choice
└─ 1.5 Entity Resolution (if ambiguous) — picker if multiple matches

    ↓ (Only proceed if clean time arrangement)

PHASE 2: COMPREHENSIVE OUTPUT
├─ 2.1 RelevancyLib Query — entity context, relationship history
├─ 2.2 UserHabit Nudge — behavioral patterns, warnings
└─ 2.3 Final Synthesis (LLM) — combine all context into response
```

### Pipeline States

```kotlin
sealed class SchedulerPipelineState {
    // Phase 1
    data class Parsing(val input: String) : SchedulerPipelineState()
    data class ConflictDetected(val conflicts: List<ScheduleItem>) : SchedulerPipelineState()
    data class AwaitingConflictResolution(val options: List<ResolutionOption>) : SchedulerPipelineState()
    data class AwaitingEntityResolution(val candidates: List<RelevancyEntry>) : SchedulerPipelineState()
    
    // Phase 2
    data class EnrichingContext(val resolvedItem: ScheduleItem) : SchedulerPipelineState()
    data class Complete(val response: SchedulerResponse) : SchedulerPipelineState()
}

sealed class ResolutionOption {
    object Reschedule : ResolutionOption()
    object Coexist : ResolutionOption()
    data class Cancel(val entryId: String) : ResolutionOption()
}
```

### Why Two Phases?

| Benefit | Impact |
|---------|--------|
| **Gate before RelevancyLib** | Don't query memory if time slot blocked |
| **User confirms before Phase 2** | Clean data in = intelligent output |
| **LLM call reduction** | If user cancels conflict, never hit Phase 2 |
| **Error correction** | Follow-up catches user's own time mistakes |

---

## Supporting Types

```kotlin
data class EntryRef(
    val entryId: String,
    val date: Long,
    val workflow: String,
    val title: String,
    val snippet: String?
)

data class DecisionRecord(
    val timestamp: Long,
    val conflictDescription: String,
    val userResolution: String,
    val resultingEntryId: String?
)
```

---

## Structured Output Model

LLM outputs are split into two sections — structured (agent-only) and display (user-visible).

```
┌─────────────────────────────────────────────────────────┐
│                   LLM OUTPUT                            │
├─────────────────────────────────────────────────────────┤
│  [STRUCTURED SECTION] (agent + linter only)             │
│  {                                                      │
│    "entities": [                                        │
│      {"type": "person", "id": "z-001", "name": "张总"}, │
│      {"type": "date", "raw": "周三",                    │
│       "resolved": "2026-01-29"}                         │
│    ]                                                    │
│  }                                                      │
│                                                         │
│  [USER CONTENT] (clean, user-visible)                   │
│  「张总希望在周三于北京办公室讨论A3打印机方案」            │
└─────────────────────────────────────────────────────────┘
```

### Schema Linter Rules

| Check | Rule | Failure Action |
|-------|------|----------------|
| **JSON Valid** | `structuredJson` must be valid JSON | Reject, retry |
| **Entity Type** | Must be: `person`, `product`, `location`, `date`, `event` | Reject, retry |
| **ID Format** | ID must match `[a-z]-[0-9]{3}` (e.g., `z-001`) | Reject, retry |
| **Required Fields** | Each entity must have `type`, `id`, `name` | Reject, retry |

```kotlin
data class ExtractedEntity(
    val type: String,      // "person", "product", "location", "date", "event"
    val id: String,        // "z-001"
    val name: String,      // "张总"
    val context: String?,  // Optional additional context
    val resolved: String?  // For dates: ISO format
)
```

---

## User Habit (Learned Patterns)

Behavior patterns learned automatically. Used by Context Builder for personalization.

```kotlin
data class UserHabit(
    val habitKey: String,          // e.g., "preferred_meeting_time"
    val habitValue: String,        // e.g., "morning"
    val entityId: String?,         // For per-client habits (null = global)
    val isExplicit: Boolean,       // true = user-set, false = inferred
    val confidence: Float,         // 0.0-1.0
    val observationCount: Int,
    val rejectionCount: Int,
    val lastObservedAt: Long,
    val createdAt: Long
)
```

| Field | Purpose |
|-------|---------|
| `confidence` | Strength of inference (obs / (obs + rej)) |
| `isExplicit` | User explicitly set vs system inferred |
| `entityId` | Habit specific to one client vs global |

---

## Metrics History JSON Example

```json
{
  "budget": [
    {"date": "2026-01-15", "value": 200000000, "unit": "CNY", "source": "session-abc"},
    {"date": "2026-02-10", "value": 250000000, "unit": "CNY", "source": "session-xyz"}
  ],
  "quantity": [
    {"date": "2026-01-15", "value": 100, "unit": "pcs", "source": "session-abc"}
  ],
  "deal_stage": [
    {"date": "2026-01-10", "value": "qualification", "source": "session-abc"},
    {"date": "2026-02-01", "value": "proposal", "source": "session-xyz"}
  ]
}
```

**Typing Rules:**
- **Currency:** Minor units (分/cents) as integer, with `unit` field
- **Quantity:** Integer with `unit` (pcs, kg, carton)
- **Duration:** Integer with `unit` (days, weeks, months)
- **Stage/Enum:** String from predefined values
