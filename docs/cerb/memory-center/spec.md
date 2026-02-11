# Memory Center

> **Cerb-compliant spec** — Core storage layer for Prism.  
> **OS Layer**: SSD Storage
> **State**: SHIPPED

---

## Overview

Memory Center manages persistent storage and retrieval of user interactions, entity information, and scheduled tasks. Uses a **Two-Zone Model** for lifecycle management.

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [Entity Registry](../entity-registry/interface.md) | Entity lookup, disambiguation |
| [User Habit](../user-habit/interface.md) | Behavioral patterns, learning |

---

## Two-Zone Model

> [!NOTE]
> **Renamed from Hot/Cement.** Now uses **Active/Archived** terminology.
> Zone is determined by `isArchived` flag at query-time, no physical data movement.

| Zone | Criteria | Contents |
|------|----------|----------|
| **Active** | `isArchived = false` | Active entries |
| **Archived** | `isArchived = true` | Completed entries |

**No physical data movement.** Zone is determined by query-time filter only.

---

## Domain Models

### MemoryEntry

Production schema — [`MemoryModels.kt`](../../app-prism/src/main/java/com/smartsales/prism/domain/memory/MemoryModels.kt).

```kotlin
data class MemoryEntry(
    val entryId: String,
    val sessionId: String,
    val content: String,
    val entryType: MemoryEntryType,  // USER_MESSAGE, ASSISTANT_RESPONSE, TASK_RECORD, SCHEDULE_ITEM, INSPIRATION
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val scheduledAt: Long? = null,
    val structuredJson: String? = null,  // {"relatedEntityIds":["p-001","a-002"]}
    val workflow: String? = null,        // COACH, ANALYST, SCHEDULER
    val title: String? = null,           // 记忆标题
    val completedAt: Long? = null,       // 完成时间戳
    val outcomeStatus: String? = null,   // ONGOING, SUCCESS, PARTIAL, FAILED
    val outcomeJson: String? = null,     // 结果详情 JSON
    val payloadJson: String? = null,     // WorkflowPayload（模式相关数据）
    val displayContent: String? = null,  // UI 展示用文本
    val artifactsJson: String? = null    // 生成物引用
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
| **3** | Entity Knowledge Context (Kotlin loads, LLM searches) | ✅ SHIPPED |
| **4** | Rich MemoryEntry Schema (workflow, outcome, payload) | ✅ SHIPPED |

### Wave 1 Ship Criteria (✅ SHIPPED)

- **Goal**: Conflict detection via hardcoded Kotlin
- **Exit Criteria**: `checkConflict()` returns correct overlap results
- **Test Cases**: ✅ Time overlap detection, ✅ `excludeId` self-exclusion, ✅ `ConflictPolicy.COEXISTING` passthrough

### Wave 2 Ship Criteria (✅ SHIPPED)

- **Goal**: Active/Archived zones via query-time filtering
- **Exit Criteria**: `getActiveEntries()` filters by `isArchived` + subscription window
- **Test Cases**: ✅ Tier-based window logic, ✅ Archive query excludes active entries

### Wave 3 Ship Criteria (✅ SHIPPED)

- **Goal**: Entity Knowledge Context — "Kotlin loads, LLM searches"
- **Exit Criteria**: `buildEntityKnowledge()` returns structured JSON on first turn; injected into LLM prompt
- **Test Cases**: ✅ First turn loads entity knowledge, ✅ Empty entities → null, ✅ Cached on subsequent turns, ✅ Reset clears and re-enables, ✅ Groups by entity type (people/accounts/deals)

---

## File Map

| Layer | Files |
|-------|-------|
| **Domain** | `MemoryRepository.kt`, `MemoryModels.kt`, `ScheduleBoard.kt`, `ScheduleItem.kt` |
| **Data** | `RealScheduleBoard.kt`, `FakeMemoryRepository.kt` |
| **DI** | `SchedulerModule.kt` (provides ScheduleBoard binding) |

---

## Verification Commands

```bash
# Build check
./gradlew :app-prism:compileDebugKotlin

# Run memory-related tests
./gradlew :app-prism:testDebugUnitTest --tests "*Memory*"
./gradlew :app-prism:testDebugUnitTest --tests "*ScheduleBoard*"
```

---

## Wave 2: Lazy Compaction Behavior

### Philosophy

**No background jobs.** Hot/Cement zones are defined by **query-time filters**, not physical data movement.

### Active Zone Query (Default)

```kotlin
SELECT * FROM entries
WHERE isArchived = false
   OR scheduledAt > (NOW() - INTERVAL [subscriptionWindowDays] DAY)
```

### Archived Zone Query (History Browsing)

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

### Retrieval Strategy: Entity Knowledge Context

> **Principle**: "Kotlin loads, LLM searches." The Kernel loads structured entity data from `EntityRepository` into the LLM prompt. The LLM handles disambiguation, alias matching, and relevance naturally.

**Why entity data, not raw memories?**
- `EntityEntry` already contains distilled intelligence: `aliasesJson`, `attributesJson`, `metricsHistoryJson`, `relatedEntitiesJson`, `decisionLogJson`
- Raw `memory_entries` are conversation logs (rote memorization) — recalling "Feb 8 CEO had meeting" is trivia
- Entity data IS the knowledge graph: who → what company → what role → what deal stage → what buying decisions
- This enables **smart suggestions and reminders**, not just recall

**Why not LIKE search?** `MemoryDao.search()` uses `LIKE '%query%'` which fails for:
- Alias mismatches: `孙英浩` ≠ `孙扬浩`
- Cross-language: `阿米尔` ≠ `ameer`
- Type filtering: `AND entryType != 'USER_MESSAGE'` excludes seed data

**Entity Knowledge Context** = structured JSON of entity graph, injected into the system prompt:
```json
{
  "people": [
    {"name": "孙扬浩", "aliases": ["孙工"], "company": "承时利和", "role": "工程师",
     "metrics": {"visits": 2}, "decisions": ["2026-02-08: 调试桌面机械臂"]}
  ],
  "accounts": [
    {"name": "摩升泰", "contacts": ["ameer", "蔡瑞江"]}
  ]
}
```

| Read Path | Method | Scope | When |
|-----------|--------|-------|------|
| **Entity Knowledge (default)** | `EntityRepository.getAll(limit)` | Session-unscoped | Every session start |
| **LIKE search (escape hatch)** | `MemoryDao.search(query)` | Session-unscoped | Future optimization |
| **Archived Zone** | `getArchivedEntries(sessionId, tier)` | Session-scoped | Explicit history request |

**Token budget**: ~2500 tokens (~10K chars). Entity payload size logged for monitoring.

**Cost guardrail**: Max 50 Archived entries per LLM request.

