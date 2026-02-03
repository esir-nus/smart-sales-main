# Memory Center

> **Cerb-compliant spec** — Core storage layer for Prism.

---

## Overview

Memory Center manages persistent storage and retrieval of user interactions, entity information, and scheduled tasks. Uses a **Two-Zone Model** for lifecycle management.

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [Relevancy Library](../relevancy-library/spec.md) | Entity lookup, disambiguation |
| [User Habit](../user-habit/spec.md) | Behavioral patterns, learning |

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
fun checkConflict(start: Long, durationMin: Int, excludeId: String? = null): ConflictResult {
    val proposedEnd = start + (durationMin * 60_000L)
    
    val overlaps = getDay(start.toLocalDate())
        .filter { slot ->
            slot.entryId != excludeId &&  // Exclude self
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

## Wave Plan

| Wave | Focus | Status |
|------|-------|--------|
| **1** | ScheduleBoard + Two-Phase Pipeline | ✅ SHIPPED |
| **2** | Hot/Cement Zone Compaction | 🔲 (needs behavior spec) |
