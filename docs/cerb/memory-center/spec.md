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

## Two-Zone Model (DEPRECATED)

> [!WARNING]
> **Hot/Cement terminology is deprecated.** Use query-time filtering only.
> See [Client Profile Hub](../client-profile-hub/spec.md) for the new relevance-first approach.

| Zone | Criteria | Contents |
|------|----------|----------|
| **Active** | `isArchived = false` | Active entries |
| **Archived** | `isArchived = true` | Completed entries |

**No physical data movement.** Zone is determined by query-time filter only.

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
| **2** | Query-Time Filtering (Lazy Compaction) | ✅ SHIPPED |
| **3** | [Client Profile Hub](../client-profile-hub/spec.md) | 🔲 PLANNING |

---

## Wave 2: Lazy Compaction Behavior

### Philosophy

**No background jobs.** Hot/Cement zones are defined by **query-time filters**, not physical data movement.

### Hot Zone Query (Default)

```kotlin
SELECT * FROM entries
WHERE isArchived = false
   OR scheduledAt > (NOW() - INTERVAL [subscriptionWindowDays] DAY)
```

### Cement Zone Query (History Browsing)

```kotlin
SELECT * FROM entries
WHERE isArchived = true
  AND scheduledAt <= (NOW() - INTERVAL [subscriptionWindowDays] DAY)
```

### Subscription Tier Configuration

| Tier | Hot Window | Cement Access |
|------|-----------|---------------|
| **Free** | 7 days | Read-only |
| **Pro** | 14 days | Full access |
| **Enterprise** | 30 days | Full access |

**Implementation**:
```kotlin
object SubscriptionConfig {
    fun getHotWindowDays(tier: SubscriptionTier): Int = when (tier) {
        SubscriptionTier.FREE -> 7
        SubscriptionTier.PRO -> 14
        SubscriptionTier.ENTERPRISE -> 30
    }
}
```

### Archive Trigger

Archiving happens **only when user completes a task**:

```kotlin
suspend fun completeTask(taskId: String) {
    repository.update(taskId, isArchived = true)
}
```

**No automatic archiving.** The `isArchived` flag is user-driven.

### Retrieval Strategy

| Scenario | Query Target | Model | Cost |
|----------|-------------|-------|------|
| Default (recent context) | Hot Zone only | qwen-plus | Low |
| Entity last seen >14 days | Cement Zone (targeted) | qwen-long | High (justified) |
| Explicit history request | Cement Zone (filtered) | qwen-long | High (user-initiated) |

**Cost guardrail**: Max 50 Cement entries per LLM request.
