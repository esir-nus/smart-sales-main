# Scheduler

> **Cerb-compliant spec** — Self-contained, all content inline.  
> **OS Model**: Consumer of RAM (reads entity context from SessionWorkingSet Section 1)

---

## Overview

Scheduler manages task creation, timeline display, alarm cascading, and LLM-powered parsing. Uses **Memory Center's ScheduleBoard** for conflict detection.

**OS Model Note**: Scheduler reads entity context (key persons, locations) from RAM Section 1 instead of doing its own entity extraction via `buildWithClues`. The Kernel populates entity context; Scheduler just consumes what's on the RAM.

**Key Distinction:**
- `ScheduleBoard` = Memory Center index (conflict check infrastructure)
- `ScheduledTaskRepository` = Scheduler feature (task CRUD, **SSD Storage**)

**Inspiration**:
- Standalone notes, **not time-bound**
- Displayed with 💡 icon in collapsible shelf
- Excluded from Hot Zone (not scheduled items)
- Stored via `InspirationRepository`

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Repository + Linter (Core) | 🚢 SHIPPED | `ScheduledTaskRepository`, `SchedulerLinter`, tests |
| **1.5** | ViewModel Wiring | 🚢 SHIPPED | `SchedulerViewModel`, UI connection |
| **2** | Alarm Cascade | 🚢 SHIPPED | `RealAlarmScheduler`, notification triggers |
| **3** | Smart Reminder Inference | 🚢 SHIPPED | LLM-based reminder timing suggestions |
| **4** | Input Classification + Multi-Task + Reschedule | 🚢 SHIPPED | Input gate, batch create, reschedule parsing |
| **5** | Inspiration Storage | 🚢 SHIPPED | `InspirationRepository`, `CollapsibleInspirationShelf` |

---

## Domain Models

### TimelineItemModel

```kotlin
sealed class TimelineItemModel {
    abstract val id: String
    abstract val timeDisplay: String
    
    data class Task(
        override val id: String,
        override val timeDisplay: String,
        val title: String,
        val isDone: Boolean = false,
        val hasAlarm: Boolean = false,
        val isSmartAlarm: Boolean = false,
        val startTime: Instant,
        val endTime: Instant? = null,
        val durationMinutes: Int = 30,
        val durationSource: DurationSource = DurationSource.DEFAULT,
        val conflictPolicy: ConflictPolicy = ConflictPolicy.EXCLUSIVE,
        val dateRange: String = "",
        val location: String? = null,
        val notes: String? = null,
        val keyPerson: String? = null,
        val keyPersonEntityId: String? = null,  // Wave 9: Entity ID for tip generation
        val highlights: String? = null,
        val tips: List<String> = emptyList(),  // Wave 9: LLM-generated context tips
        val tipsLoading: Boolean = false,       // Wave 9: Generation animation state
        val alarmCascade: List<String>? = null // e.g. ["-1h", "-15m", "-5m"]
    ) : TimelineItemModel()
    
    data class Inspiration(
        override val id: String,
        override val timeDisplay: String,
        val title: String
    ) : TimelineItemModel()
    
    data class Conflict(
        override val id: String,
        override val timeDisplay: String,
        val conflictText: String
    ) : TimelineItemModel()
}
```

### LintResult

```kotlin
sealed class LintResult {
    data class Success(
        val task: TimelineItemModel.Task,
        val urgencyLevel: UrgencyLevel
    ) : LintResult()
    
    data class Error(val message: String) : LintResult()
}
```

### UrgencyLevel

LLM-classified urgency determines alarm cascade, conflict policy, and duration defaults.

```kotlin
enum class UrgencyLevel {
    L1_CRITICAL,   // 赶飞机、签约、面试 — 错过=不可逆损失
    L2_IMPORTANT,  // 会议、电话、汇报 — 错过=影响他人
    L3_NORMAL,     // 回邮件、买东西、日常 — 错过=无大碍
    FIRE_OFF       // 喝水、站起来走走 — 即时提醒，无闹钟
}
```

| Level | Cascade | Conflict Policy | Duration Default |
|-------|---------|-----------------|------------------|
| `L1_CRITICAL` | `-2h, -1h, -30m, -15m, -5m, -1m` | EXCLUSIVE | LLM-decided |
| `L2_IMPORTANT` | `-1h, -15m, -5m, -1m` | EXCLUSIVE | LLM-decided |
| `L3_NORMAL` | `-15m, -1m` | EXCLUSIVE | LLM-decided |
| `FIRE_OFF` | _(none)_ | COEXISTING | 0 |

---

## Scheduler Pipeline

Task creation follows a **gated pipeline** with conflict resolution.

```
User Voice Input
    ↓
Orchestrator (MODE_SCHEDULER)
    ↓
LLM Parse → JSON { title, startTime, endTime?, location?, ... }
    ↓
SchedulerLinter.lint() → LintResult.Success | Error
    ↓
    ├─▶ RAM Section 1 → Entity context (keyPerson, location candidates)
    │   (Kernel auto-populated — no buildWithClues needed)
    ↓
ScheduleBoard.checkConflict() → Clear | Conflict
    ↓
[If Conflict] → UI shows resolution options → User picks
    ↓
Repository.insertTask() → ID returned
    ↓
ScheduleBoard.refresh() → Index updated
    ↓
AlarmScheduler.scheduleAlarm() (if reminder set)
```

### UiState for Pipeline

```kotlin
sealed class UiState {
    data class SchedulerTaskCreated(
        val taskId: String,
        val title: String,
        val dayOffset: Int
    ) : UiState()
}
```

---

## Linter Validation Rules

| Check | Rule | On Failure |
|-------|------|------------|
| **Title** | Non-empty | Error("任务标题不能为空") |
| **StartTime** | Valid ISO format | Error("无法解析开始时间") |
| **EndTime** | >= StartTime (if present) | Error("结束时间不能早于开始时间") |
| **Past Date** | StartTime >= today start | Error("不能创建过去的任务") |
| **JSON Structure** | Valid JSON | Error("JSON 解析失败") |

### DateTime Normalization

LLM sometimes outputs malformed dates. Linter normalizes:
```kotlin
// "2026-02-0303:00" → "2026-02-03 03:00"
val normalized = dateTimeStr
    .replace(Regex("(\\d{4}-\\d{2}-\\d{2})(\\d{2}:\\d{2})"), "$1 $2")
    .trim()
```

---

## Alarm Cascade (Urgency-Driven)

**Architecture**: LLM classifies urgency → Kotlin builds deterministic cascade.

```
User: "6点赶飞机"
    ↓
LLM: { "urgency": "L1" }     ← LLM classifies
    ↓
Linter: validates urgency ∈ {L1, L2, L3, FIRE_OFF}
    ↓
buildCascade(L1_CRITICAL)      ← Kotlin determines offsets
    → [-2h, -1h, -30m, -15m, -5m, -1m]
    ↓
AlarmScheduler.scheduleAll()
```

### buildCascade

```kotlin
fun buildCascade(level: UrgencyLevel): List<String> = when (level) {
    L1_CRITICAL  -> listOf("-2h", "-1h", "-30m", "-15m", "-5m", "-1m")
    L2_IMPORTANT -> listOf("-1h", "-15m", "-5m", "-1m")
    L3_NORMAL    -> listOf("-15m", "-1m")
    FIRE_OFF     -> emptyList()
}
```

### LLM Prompt for Urgency

```
紧急程度分类 (urgency):
- L1: 赶飞机、签约、面试（错过=不可逆损失）
- L2: 会议、电话、汇报（错过=影响他人）
- L3: 回邮件、买东西、日常任务（错过=无大碍）
- FIRE_OFF: 喝水、站起来走走、看新闻（即时提醒，无闹钟）
```

### Linter Validation

```kotlin
val urgency = json.optString("urgency", "L3")  // 默认 L3
val urgencyLevel = when (urgency.uppercase()) {
    "L1" -> UrgencyLevel.L1_CRITICAL
    "L2" -> UrgencyLevel.L2_IMPORTANT
    "L3" -> UrgencyLevel.L3_NORMAL
    "FIRE_OFF" -> UrgencyLevel.FIRE_OFF
    else -> UrgencyLevel.L3_NORMAL  // 无法识别 → 安全默认
}
val alarmCascade = buildCascade(urgencyLevel)
val policy = if (urgencyLevel == UrgencyLevel.FIRE_OFF) 
    ConflictPolicy.COEXISTING else ConflictPolicy.EXCLUSIVE
```

### Cascade Offset Parsing

```kotlin
fun parseCascadeOffset(offset: String): Long {
    val regex = Regex("-(\\d+)([hm])")
    val match = regex.matchEntire(offset) ?: return 0
    val value = match.groupValues[1].toLong()
    val unit = match.groupValues[2]
    return when (unit) {
        "h" -> value * 60 * 60 * 1000
        "m" -> value * 60 * 1000
        else -> 0
    }
}
```

### What Gets Removed

| Removed | Replaced By |
|---------|-------------|
| `TaskTypeHint` enum | `UrgencyLevel` enum |
| `ReminderType` enum | `buildCascade(UrgencyLevel)` |
| `inferTaskType()` Kotlin keyword matching | LLM `urgency` field |
| Hardcoded `when(reminder)` cascade | `buildCascade()` lookup |

---

## Wave Ship Criteria

### 🔬 Wave 1: Repository + Linter (SHIPPED)

Core CRUD and LLM output validation.

- **Ship Criteria**: Insert/query/delete tasks with lint validation
- **Test Cases**:
    - [x] Valid task JSON → LintResult.Success
    - [x] Missing title → Error
    - [x] Past date → Error
    - [x] End before start → Error
- **Deliverables**: `ScheduledTaskRepository.kt`, `SchedulerLinter.kt`, `SchedulerLinterTest.kt`

### 🔬 Wave 1.5: ViewModel Wiring (SHIPPED)

Connect Repository to UI layer.

- **Ship Criteria**: Tasks appear in timeline after creation
- **Test Cases**:
    - [x] simulateTranscript() → task in timeline
    - [x] Date selection → correct dayOffset items
    - [x] Delete → item removed
- **Deliverables**: `SchedulerViewModel.kt`, `SchedulerDrawer.kt` integration

### 🔬 Wave 2: Alarm Cascade

System alarm integration.

- **Ship Criteria**: AlarmManager fires at correct offsets
- **Test Cases**:
    - [ ] Single reminder → 1 alarm at -15m
    - [ ] Smart reminder → 3 alarms at -1h, -15m, -5m
    - [ ] Task delete → alarms cancelled
- **Deliverables**: `RealAlarmScheduler.kt`, notification channel

### 🔬 Wave 3: Smart Reminder Inference

LLM suggests optimal reminder timing based on task type.

- **Ship Criteria**: Meeting → smart, call → single, urgent → immediate
- **Test Cases**:
    - [ ] "会议" → SMART_CASCADE suggested
    - [ ] "电话" → SINGLE suggested
    - [ ] User override respected
- **Deliverables**: Prompt engineering in `DashscopeExecutor`

### 🔬 Wave 4: Input Classification + Multi-Task + Reschedule

Expanded Wave 4 covers the full input processing pipeline.

#### 4.0: Input Classification

First gate: classify user input before parsing.

| Classification | Route |
|----------------|-------|
| `schedulable` | Continue to task parsing |
| `deletion` | Match against ScheduleBoard, delete if 1 match |
| `inspiration` | Return for future Wave 5 storage |
| `non_intent` | Return `AwaitingClarification` |

- **Ship Criteria**: Non-scheduling input (e.g., "你好") does NOT create bogus task
- **Test Cases**:
    - [ ] "你好" → AwaitingClarification
    - [ ] "以后想学吉他" → classification=inspiration
    - [ ] "明天开会" → classification=schedulable
- **Deliverables**: Prompt update in `DashscopeExecutor`, classification handling in `PrismOrchestrator`

#### 4.1: Multi-Task Splitting

Handle input with multiple tasks.

> **Design Note**: Split tasks are independent creates, not concurrent operations. Direct insertion allows faster UX (user can delete if LLM misunderstood). Earlier versions included confirmation, but this was removed as it added friction without benefit.

- **Ship Criteria**: "8点吃面 9点开会" creates 2 tasks immediately
- **Test Cases**:
    - [ ] Multi-task detected → batch create directly
    - [ ] Toast shows: "✅ 已创建 N 个任务"
    - [ ] Each task gets conflict check (warning appended if any conflict)
    - [ ] All tasks get alarms if reminder type specified
- **Deliverables**: Direct batch insert in `PrismOrchestrator`, conflict warning in toast

#### 4.2: Reschedule Flow

Natural language rescheduling (e.g., "把明天的会改到后天").

- **Ship Criteria**: Existing task updated with new time
- **Edge Cases**:
    - **Auto-Refresh**: UI must reflect changes immediately without user interaction (e.g. pull-to-refresh).
    - **Ghost Cards**: Updates must atomically replace the old item. No duplicates allowed.
    - **Date Anchors**:
        - "Next Friday" → Anchored to TODAY (absolute).
        - "Defer 2 days" → Anchored to TASK DATE (relative).
    - **Visual Feedback**: When reschedule changes task date, highlight target date with breathing animation:
        - New task: Blue glow (`AccentBlue`)
        - Rescheduled task: Amber glow (`#FFA726`) 
        - Animation: Same breathing pattern (0.3-0.8 alpha, 1000ms cycle)
- **Test Cases**:
    - [ ] Reference existing task → correct match
    - [ ] Ambiguous reference → picker shown
    - [ ] Conflict on new time → resolution flow
    - [ ] Reschedule to different date → amber glow on target date

> [!NOTE]
> **Deferred to Wave 6+**: Ambiguous reference picker (needs UX design), Full conflict resolution picker (warning-only acceptable for MVP).

- **Deliverables**: `buildReschedulePrompt()` rewrite, `onReschedule()` in ViewModel

### � Wave 5: Inspiration Storage (SHIPPED)

Store non-schedulable input for future reference.

- **Ship Criteria**: Inspiration saved and retrievable
- **Test Cases**:
    - [x] "以后想学吉他" → saved to InspirationRepository
    - [x] Inspirations visible in `CollapsibleInspirationShelf`
    - [x] Swipe to delete inspiration
- **Deliverables**: `InspirationRepository.kt`, `RealInspirationRepository.kt`, `CollapsibleInspirationShelf.kt`

---

## File Map

| Layer | Files |
|-------|-------|
| **Domain** | `ScheduledTaskRepository.kt`, `SchedulerLinter.kt`, `AlarmScheduler.kt`, `InspirationRepository.kt`, `FakeScheduledTaskRepository.kt` |
| **Data** | `RealScheduledTaskRepository.kt`, `RealAlarmScheduler.kt`, `RealInspirationRepository.kt`, `FakeInspirationRepository.kt`, `FakeAlarmScheduler.kt` |
| **DI** | `SchedulerModule.kt` |
| **UI** | `SchedulerDrawer.kt`, `SchedulerViewModel.kt`, `SchedulerTimeline.kt`, `SchedulerCalendar.kt`, `SchedulerCards.kt`, `SchedulerConflict.kt`, `SchedulerStates.kt`, `CollapsibleInspirationShelf.kt` |
| **Test** | `SchedulerLinterTest.kt`, `AlarmSchedulerTest.kt` |

---

### Wave 7: NL Deletion ✅ SHIPPED

Natural language task deletion via creation pipeline (`processSchedulerInput`).

- **Classification**: `deletion` → LLM returns `targetTitle`
- **Matching**: `contains`-based fuzzy match against `ScheduleBoard.upcomingItems`
- **Results**: 0 matches → error, 1 match → delete + toast, 2+ matches → ask for specificity
- **Ship Criteria**: "取消会议" deletes the matching task
- **Test Cases**:
    - [x] "取消会议" → matching task deleted, toast "🗑️ 已删除'部门会议'"
    - [x] "取消xxx" (no match) → toast "未找到匹配..."
    - [x] "取消xxx" (2+ matches) → toast "找到多个匹配..."
    - [x] "你好" → non_intent (no regression)
- **Deliverables**: `LintResult.Deletion`, prompt update, orchestrator handler

### Wave 8: Pipeline Unification 🔧 IN PROGRESS

Eliminate `processSchedulerAction` and unify all scheduler operations into `createScheduledTask(input, replaceItemId?)`.

**Motivation**: `processSchedulerAction` is a 100-line second pipeline where 6 of 7 `LintResult` branches just return "不支持". The date protection regex hack (L325-340) is a symptom of wrong abstraction.

**Design**: 
- `createScheduledTask(input, replaceItemId?)` — optional replace semantics
- When `replaceItemId` set: inject old task context into prompt, create new, delete old
- Atomicity: create first, delete after success

**Ship Criteria**: Reschedule from card produces same behavior via create+delete
- **Test Cases**:
    - [ ] "推迟两个小时" from card → new task at +2h, old deleted
    - [ ] "取消吃饭" from card → task deleted (Wave 7 preserved)
    - [ ] Conflict RESCHEDULE → task replaced correctly
    - [ ] "明天开会" → no regression
- **Deliverables**: Updated `Orchestrator` interface, rewritten `PrismOrchestrator`, cleaned `SchedulerViewModel`
- **Code Removed**: `processSchedulerAction`, `buildReschedulePrompt`, `SchedulerActionResult`, date regex hack (~173 lines)

### Wave 9: Smart Tips 🔲 PLANNED

LLM-generated contextual tips (2-5) per task card, sourced from memory layer.

**Distinction**: `highlights` = scheduler-relevant reminders (from linter). `tips` = actionable intel from EntityLib, UserHabit, ClientProfileHub.

**Data Sources**:
- `EntityEntry.demeanorJson` — communication style, preferences
- `EntityEntry.attributesJson` — budget, deal stage, close date
- `UserHabit` — per-entity behavioral patterns ("Bob prefers mornings")
- `ClientProfileHub.getFocusedContext()` — related contacts, deals, timeline
- `MemoryRepository` — recent conversation history with entity

**Architecture**:
```
Card Expand (first time)
    ↓
1. tipsLoading = true → Card shows shimmer animation
    ↓
2. Resolve keyPerson → entityId via EntityRegistry
    ↓
3. ClientProfileHub.getFocusedContext(entityId)
    ↓
4. LLM Prompt: context → 2-5 tips as JSON array
    ↓
5. tips = result, tipsLoading = false → Card renders tips
    ↓
6. Cache tips on task (persist via updateTask)
```

**Lazy-Load UX**:
- Card created with essential schedule info immediately (title, time, location, highlights)
- Tips generated on first card expand only
- Shimmer/pulse animation during generation (2-4s)
- Tips cached after first generation (no re-generation on subsequent expands)
- If `keyPerson` is null (no entity context), skip tip generation entirely

**LLM Tip Prompt**:
```
你是销售助手。用户即将参加以下日程：
任务: "${task.title}" (${formatTime(task.startTime)})
关键人物: ${keyPerson}

以下是关于该关键人物的上下文信息：
${focusedContext.toPromptString()}

请生成 2-5 条简短、有用的提示，帮助用户更好地准备此次会面。
每条提示一行，JSON 数组格式：["提示1", "提示2", ...]

规则：
- 只返回有实际价值的信息，不要说废话
- 基于已知数据，绝不编造
- 如果没有足够上下文，返回空数组 []
```

**Domain Model Change**:
```kotlin
// TimelineItemModel.Task 新增字段
val tips: List<String> = emptyList()  // LLM 生成的上下文提示
val tipsLoading: Boolean = false      // 生成动画状态
```

**Ship Criteria**: Expanding a task card with `keyPerson` set triggers tip generation and displays 2-5 context-aware tips
- **Test Cases**:
    - [ ] Card with keyPerson → expand → shimmer → tips appear
    - [ ] Card without keyPerson → expand → no shimmer, no tips
    - [ ] Second expand → cached tips shown instantly (no LLM call)
    - [ ] Empty EntityLib → tips = [] (graceful degradation)
    - [ ] LLM timeout → tipsLoading = false, tips = [] (no error shown)
- **Deliverables**: `TipGenerator.kt`, prompt in `DashscopeExecutor`, ViewModel lazy-load wiring, UI shimmer + tip list

### Wave 10: OS Model Upgrade 🔲 PLANNED

Simplify entity context access by reading from RAM instead of `buildWithClues`.

- **Scope**:
  - Remove `buildWithClues()` entity extraction from Scheduler pipeline
  - Read entity context (keyPerson, location) from SessionWorkingSet Section 1
  - Wave 9 tip generation reads `entityId` from RAM instead of EntityRegistry lookup
- **Ship Criteria**: Entity context available in pipeline without explicit extraction
- **Test Cases**:
    - [ ] Task with keyPerson → entity context auto-available from RAM
    - [ ] No entity mentioned → graceful fallback (no crash)
    - [ ] Wave 7 deletion → no regression

---

## Verification Commands

```bash
# Unit tests
./gradlew testDebugUnitTest --tests "*SchedulerLinter*"

# Build check
./gradlew :app-prism:assembleDebug

# All Prism tests
./gradlew :app-prism:testDebugUnitTest
```
