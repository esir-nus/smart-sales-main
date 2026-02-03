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

### Reinforcement (Future)

After user confirms entity, write to `AliasMapping` for future LLM hints:

| Event | Action |
|-------|--------|
| User confirms | Store alias → entityId mapping |
| User corrects | Update mapping |

> **Note**: Confidence levels and finetuned prompts for disambiguation are deferred. Ship basic flow first.

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

### Phase 1 Output (Frozen 4 Fields)

LLM Linter extracts exactly 4 fields from user input:

| Field | Example | Notes |
|-------|---------|-------|
| `person` | "张总" | Raw alias, unresolved |
| `startTime` | 2026-02-05 09:00 | Parsed datetime |
| `location` | "会议室" or null | Optional |
| `briefSummary` | "开会" | Action/purpose |

> **Duration is NOT parsed in Phase 1.** It requires follow-up clarification or inference from task type.

### Pipeline Flow

```
PHASE 1: PARSE & CLUES
├─ 1.1 LLM Parse → 4 frozen fields (person, startTime, location, briefSummary)
├─ 1.2 Duration Acquisition:
│      ├─ Infer from task type (e.g., "开会" = 1h default)
│      └─ OR follow-up: "这个会议大概多长时间？"
├─ 1.3 ScheduleBoard Check (Kotlin) — startTime + duration → conflict?
└─ 1.4 If conflict → User choice: [调整时间] [保持共存] [取消原任务]

    ↓ (Only proceed if clean time arrangement)

PHASE 2: CONTEXT ENRICHMENT (LLM)
├─ 2.1 Entity Resolution — LLM uses clues + context to resolve "张总"
├─ 2.2 RelevancyLib Query — entity history, relationship context
├─ 2.3 UserHabit Nudge — behavioral patterns, warnings
└─ 2.4 Final Synthesis — combine all context into response

    ↓

PERSIST: ScheduleBoard.upsert(task with final duration)
```

### ScheduleBoard as Living Board

| When | ScheduleBoard State |
|------|---------------------|
| Phase 1 conflict check | Uses **estimated** duration (default by task type) |
| After Phase 2 complete | Updates with **real** duration from user clarification |

> **The Kanban updates AFTER Phase 2** — not before. Phase 1 conflict check is preliminary; final write has the confirmed duration.

### Multi-Round Conversation

The scheduler supports multi-round clarification:

```
User: "下周开会"
       │
       ▼
Phase 1: startTime = null (ambiguous)
         → No conflict check possible
         → LLM asks: "请问具体是哪天？"
       │
User: "周三下午3点"
       │
       ▼
Phase 1 RE-RUNS: startTime = 2026-02-05 15:00
         → Kanban conflict check executes
         → Continue pipeline...
```

> **Kanban checking stays ON throughout all follow-up rounds.** Each clarification re-triggers Phase 1 parsing.

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
