# Scheduler

> **Cerb-compliant spec** — Self-contained, all content inline.  
> **OS Model**: Consumer of RAM (reads entity context from SessionWorkingSet Section 1)
> **State**: PARTIAL

---

## Overview

Scheduler manages task creation, timeline display, alarm cascading, and LLM-powered parsing. Uses **Memory Center's ScheduleBoard** for conflict detection.

**OS Model Note**: Scheduler reads entity context (key persons, locations) from RAM Section 1 instead of doing its own entity extraction via `buildWithClues`. The Kernel populates entity context; Scheduler just consumes what's on the RAM.

> [!IMPORTANT]
> **CRM Entity Creation Policy**: Scheduler creates PERSON and ACCOUNT entities for **business-relevant** contacts via `EntityWriter`. Personal contacts (family, friends) are filtered at the LLM prompt level (business relevance gate).
>
> **Pipeline**:
> 1. LLM extracts `keyPerson` (business contacts only) and `keyCompany` (from input or conversation history)
> 2. `entityWriter.upsertFromClue(keyPerson, PERSON)` — creates/finds PERSON entity
> 3. If `keyCompany` present: `entityWriter.upsertFromClue(keyCompany, ACCOUNT)` — creates/finds ACCOUNT entity
> 4. Links PERSON → ACCOUNT via `entityWriter.updateProfile(personId, accountId)`
>
> **L1/L2 `next_action` Cache (Phase B — not yet implemented)**: Only L1/L2 urgency ONGOING tasks will sync to `EntityEntry.next_action` — a computed cache field for fast context reads. Entity *creation* happens for all business-relevant mentions regardless of urgency. Infrastructure exists (`nextAction` field, EntityWriter tracking, ContextBuilder surfacing) but automated sync is Phase B work.

**Key Distinction:**
- `ScheduleBoard` = Memory Center index (conflict check infrastructure)
- `ScheduledTaskRepository` = Scheduler feature (task CRUD, **SSD Storage**)

---

## Voice Command Scope

All scheduler voice commands work **globally within scheduler mode** — no card context required. User speaks freely; LLM classifies intent and routes accordingly.

> [!IMPORTANT]
> **Scheduler-mode only.** Voice commands modify schedules exclusively when the scheduler drawer is active. Other modes (Coach/Analyst) may READ schedule data but do NOT mutate it — they guide the user to switch to scheduler mode instead.
> **Active session only.** Commands operate on the current session's schedule. Past/finished sessions are not in scope.

| Classification | Input Examples | Action | Card Context? |
|----------------|---------------|--------|---------------|
| `schedulable`  | "明天下午2点开会" | Create task | Not needed |
| `deletion`     | "取消会议", "不去开会了" | Fuzzy match → delete | Not needed (Wave 7) |
| `reschedule`   | "把会推迟两小时", "会改到后天" | Fuzzy match → create-and-delete | Not needed (Wave 11) |
| `inspiration`  | "以后想学吉他" | Save to inspiration | Not needed |
| `non_intent`   | "你好" | Reject (no action) | N/A |

**Card-level mic** (with `replaceItemId`) remains as a power-user shortcut for unambiguous single-task edits.

---

## Task Completion Lifecycle

Tasks have a checkbox that toggles `isDone` state. Completed tasks remain in storage but are visually deactivated and excluded from voice command scope.

```
[ ] Active Task          →  tap checkbox  →  [✓] Completed Task
(normal colors)                               (grey, strikethrough)
(in voice command scope)                      (excluded from voice scope)
(alarms active)                               (alarms cancelled)

[✓] Completed Task      →  tap checkbox  →  [ ] Reactivated Task
(grey, strikethrough)                         (normal colors restored)
(excluded)                                    (back in voice scope)
(no alarms)                                   (alarms re-scheduled if future)
```

| State | Visual | Voice Scope | Alarms |
|-------|--------|-------------|--------|
| `isDone = false` | Normal colors | ✅ Included in `upcomingItems` | Active |
| `isDone = true` | Grey text, strikethrough, filled checkmark | ❌ Excluded from `upcomingItems` | Cancelled |

**Auto-Expiry**: Tasks whose `endTime` (or `startTime + durationMinutes`) is in the past are automatically marked `isDone = true`. Sweep triggers: (1) scheduler drawer opens, (2) day switch, (3) ViewModel init, (4) real-time alarm fire via `SchedulerRefreshBus` (DEADLINE tier emits → ViewModel sweeps). Uses `endTime`, NOT alarm fire time — cascade alarms `[-1h, -15m, 0m]` firing early do NOT trigger auto-expiry. Today-only sweep; multi-day sweep deferred as tech debt.

> [!NOTE]
> **Reactivation Safety (Safe Fallback)**: Unchecking a completed task restores it to active state. All data is preserved in Room — no re-scheduling required. Alarms are re-scheduled only if the task's start time is still in the future. This ensures users can undo completion without losing any information.

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
| **7** | NL Deletion | ✅ SHIPPED | Fuzzy match + voice delete, no card context |
| **8** | Pipeline Unification | 🔧 IN PROGRESS | Eliminated `processSchedulerAction`, unified to `createScheduledTask` |
| **9** | Smart Tips | 🚢 SHIPPED | LLM-generated contextual tips per task card |
| **10** | CRM Hierarchy Wiring | ✅ SHIPPED | Business gate + ACCOUNT creation + `accountId` linking |
| **11** | Global Reschedule | ✅ SHIPPED | Voice reschedule via fuzzy match + create-and-delete |
| **12** | Task Completion Wiring | ✅ SHIPPED | `toggleDone`, alarm lifecycle, UI strikethrough |

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
        val urgencyLevel: UrgencyLevel = UrgencyLevel.L3_NORMAL,
        val isDone: Boolean = false,
        val hasAlarm: Boolean = false,
        val isSmartAlarm: Boolean = false,
        val startTime: Instant,
        val endTime: Instant? = null,
        val durationMinutes: Int = 0,
        val durationSource: DurationSource = DurationSource.DEFAULT,
        val conflictPolicy: ConflictPolicy = ConflictPolicy.EXCLUSIVE,
        val dateRange: String = "",
        val location: String? = null,
        val notes: String? = null,
        val keyPerson: String? = null,         // Business contact name from LLM (personal contacts filtered)
        val keyPersonEntityId: String? = null,  // Populated by entity resolution in pipeline
        val highlights: String? = null,
        val tips: List<String> = emptyList(),  // Wave 9: LLM-generated context tips
        val tipsLoading: Boolean = false,       // Wave 9: Generation animation state
        val alarmCascade: List<String> = emptyList() // e.g. ["-1h", "-15m", "-5m"]
    ) : TimelineItemModel()
    
    data class Inspiration(
        override val id: String,
        override val timeDisplay: String,
        val title: String
    ) : TimelineItemModel()
    
    data class Conflict(
        override val id: String,
        override val timeDisplay: String,
        val conflictText: String,
        val taskA: ScheduleItem,
        val taskB: ScheduleItem
    ) : TimelineItemModel()
}
```

### LintResult

```kotlin
sealed class LintResult {
    data class Success(
        val task: TimelineItemModel.Task,
        val urgencyLevel: UrgencyLevel,
        val parsedClues: ParsedClues = ParsedClues()
    ) : LintResult()
    
    data class Error(val message: String) : LintResult()
}

data class ParsedClues(
    val person: String? = null,        // 商务人物（个人关系已过滤）
    val company: String? = null,       // 关联公司/组织
    val location: String? = null,
    val briefSummary: String? = null,
    val durationMinutes: Int? = null
)
```

### UrgencyLevel

LLM-classified urgency determines alarm cascade, conflict policy, and duration defaults.

```kotlin
enum class UrgencyLevel {
    L1_CRITICAL,   // 赶飞机、签约、面试 — 错过=不可逆损失
    L2_IMPORTANT,  // 会议、电话、汇报 — 错过=影响他人
    L3_NORMAL,     // 回邮件、买东西、日常 — 错过=无大碍
    FIRE_OFF       // 喝水、站起来走走 — 即时单次提醒（0m）
}
```

| Level | Cascade | Conflict Policy | Duration Default |
|-------|---------|-----------------|------------------|
| `L1_CRITICAL` | `-2h, -1h, -30m, -15m, -5m, 0m` | EXCLUSIVE | LLM-decided |
| `L2_IMPORTANT` | `-1h, -15m, -5m, 0m` | EXCLUSIVE | LLM-decided |
| `L3_NORMAL` | `-15m, -5m, 0m` | EXCLUSIVE | LLM-decided |
| `FIRE_OFF` | `0m` | COEXISTING | 0 |

---

## Scheduler Pipeline

Task creation follows a **gated pipeline** with conflict resolution.

> [!NOTE]
> **CRM entity writes in this pipeline.** When `keyPerson` is present, Scheduler calls `entityWriter.upsertFromClue()` for PERSON (and ACCOUNT if `keyCompany` present). Business relevance gate at LLM prompt level filters personal contacts.

```
User Voice Input
    ↓
Orchestrator (MODE_SCHEDULER)
    ↓
LLM Parse → JSON { title, startTime, endTime?, location?, keyPerson?, keyCompany?, ... }
    ↓
SchedulerLinter.lint() → LintResult.Success(parsedClues) | Error
    ↓
ScheduleBoard.checkConflict() → Clear | Conflict
    ↓
[If Conflict] → UI shows resolution options → User picks
    ↓
EntityWriter.upsertFromClue(keyPerson, PERSON) → PERSON entity
    ↓
[If keyCompany] EntityWriter.upsertFromClue(keyCompany, ACCOUNT) → ACCOUNT entity
    ↓
[If ACCOUNT] EntityWriter.updateProfile(personId, {accountId}) → linked
    ↓
Repository.insertTask(enrichedTask) → ID returned
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
    → [-2h, -1h, -30m, -15m, -5m, 0m]
    ↓
AlarmScheduler.scheduleAll()
```

### buildCascade

```kotlin
fun buildCascade(level: UrgencyLevel): List<String> = when (level) {
    L1_CRITICAL  -> listOf("-2h", "-1h", "-30m", "-15m", "-5m", "0m")
    L2_IMPORTANT -> listOf("-1h", "-15m", "-5m", "0m")
    L3_NORMAL    -> listOf("-15m", "-5m", "0m")
    FIRE_OFF     -> listOf("0m")
}
```

### LLM Prompt for Urgency

```
紧急程度分类 (urgency):
- L1: 赶飞机、签约、面试（错过=不可逆损失）
- L2: 会议、电话、汇报（错过=影响他人）
- L3: 回邮件、买东西、日常任务（错过=无大碍）
- FIRE_OFF: 喝水、站起来走走、看新闻（即时单次提醒 0m）
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

First gate: classify user input before parsing. See **[Voice Command Scope](#voice-command-scope)** for the canonical classification table.

> [!NOTE]
> `reschedule` classification added in Wave 11. All other classifications shipped in Wave 4.0.

- **Ship Criteria**: Non-scheduling input (e.g., "你好") does NOT create bogus task
- **Ship Criteria (Wave 4.3)**: Input without explicit clock time routes to inspiration, not schedulable
- **Test Cases**:
    - [ ] "你好" → AwaitingClarification
    - [ ] "以后想学吉他" → classification=inspiration
    - [ ] "明天找Jake" → classification=inspiration (有日期无时间点)
    - [ ] "明天下午2点开会" → classification=schedulable (有具体时间点)
- **Deliverables**: Prompt update in `DashscopeExecutor`, classification handling in `IntentOrchestrator`

#### 4.1: Multi-Task Splitting

Handle input with multiple tasks.

> **Design Note**: Split tasks are independent creates, not concurrent operations. Direct insertion allows faster UX (user can delete if LLM misunderstood). Earlier versions included confirmation, but this was removed as it added friction without benefit.

- **Ship Criteria**: "8点吃面 9点开会" creates 2 tasks immediately
- **Test Cases**:
    - [ ] Multi-task detected → batch create directly
    - [ ] Toast shows: "✅ 已创建 N 个任务"
    - [ ] Each task gets conflict check (warning appended if any conflict)
    - [ ] All tasks get alarms if reminder type specified
- **Deliverables**: Direct batch insert in `UnifiedPipeline`, conflict warning in toast

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
- **Auto-Expiry**: `autoCompleteExpiredTasks()` on ViewModel init — sweeps today's expired tasks

**Ship Criteria**: Reschedule from card produces same behavior via create+delete + auto-expiry works
- **Test Cases**:
    - [ ] "推迟两个小时" from card → new task at +2h, old deleted
    - [ ] "取消吃饭" from card → task deleted (Wave 7 preserved)
    - [ ] Conflict RESCHEDULE → task replaced correctly
    - [ ] "明天开会" → no regression
    - [x] Past-due task auto-marked isDone on drawer open
    - [x] Task with cascade alarm: early alarm fire does NOT auto-expire
- **Deliverables**: Updated `Orchestrator` interface, rewritten `IntentOrchestrator`, cleaned `SchedulerViewModel`
- **Code Removed**: `processSchedulerAction`, `buildReschedulePrompt`, `SchedulerActionResult`, date regex hack (~173 lines)

### Wave 9: Smart Tips ✅ SHIPPED

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
- **4-Layer Fix (Coach Output Quality)**:
  1. **Plain Text Prompt**: System prompt rewritten to avoid `##`/`**` syntax mirroring.
  2. **Data Envelope**: Entity data wrapped in `<KNOWN_FACTS>` tags.
  3. **Positive Framing**: "Your memory is limited to KNOWN_FACTS" (no negative priming).
  4. **Sanitizer**: `MarkdownSanitizer` strips leaked formatting before UI render.

### Wave 10: CRM Hierarchy Wiring ✅ SHIPPED

Scheduler creates PERSON + ACCOUNT entities for business-relevant contacts. Personal contacts filtered at LLM prompt level.

- **Scope**:
  - LLM prompt extracts `keyCompany` from input or conversation history (last 6 turns)
  - Business relevance gate: `keyPerson` only for business contacts (skip 爷爷/奶奶/老婆/朋友)
  - `ParsedClues.company` carries extracted company through pipeline
  - `UnifiedPipeline`: upsert PERSON → upsert ACCOUNT (if company present) → link via `updateProfile(accountId)`
- **Ship Criteria**: ✅ Business-relevant scheduled tasks create PERSON + ACCOUNT entities with CRM linking
- **Test Cases**:
    - [x] "去墨生态拜访蔡总" → PERSON(蔡总) + ACCOUNT(墨生态) + linked
    - [x] "打电话给爷爷" → no entity created (`keyPerson: null`)
    - [x] Build passes with all CRM wiring
    - [ ] L2: Multi-turn context — history: "跟墨生态的蔡总" → "安排跟他开会"

---

### Wave 11: Global Reschedule ✅ SHIPPED

Add `reschedule` classification to the scheduler LLM prompt. Reuses Wave 7's fuzzy match + Wave 4.2's create-and-delete atomicity. No card context required.

- **LLM returns**: `{ classification: "reschedule", targetTitle: "会", newInstruction: "推迟两小时" }`
- **Matching**: Same `contains`-based fuzzy against `ScheduleBoard.upcomingItems` (active tasks only, `isDone` filtered)
- **Results**:
  - 0 matches → toast "未找到匹配的任务"
  - 1 match → `createScheduledTask(newInstruction, replaceItemId=matchedId)`
  - 2+ matches → toast "找到多个匹配，请更具体"
- **Design Decision**: Single-task only. No batch reschedule — aligns with voice UX mental model (user has one specific task in mind)
- **Amber Glow**: `UiState.SchedulerTaskCreated.isReschedule` field signals ViewModel to add date to `rescheduledDates`
- **Ship Criteria**: ✅ "把会推迟两小时" modifies the matching task without opening card

#### Implementation Notes

**Prompt Distinction (deletion vs reschedule)**:
- `deletion`: 取消、不去 → task disappears
- `reschedule`: 推迟、延迟、提前、改到、推后 → task time changes

**Recursive Call Guard**: When `replaceItemId != null`, context injection explicitly tells LLM `【正在修改现有任务，请返回 schedulable 分类】` to prevent infinite reschedule loop.

- **Test Cases**:
    - [x] "起床时间推迟一个小时" → matched task rescheduled (21:49 → 22:49), amber glow
    - [ ] "改到后天" with 1 upcoming task → rescheduled
    - [ ] "改会" with 3 tasks → toast asks for specificity
    - [ ] "改不存在的" → toast "未找到匹配"
    - [ ] Badge input in scheduler mode → global reschedule works

---

### Wave 12: Task Completion Wiring ✅ SHIPPED

Wire `isDone` toggle through ViewModel + alarm lifecycle.

- **ViewModel**: `toggleDone(taskId)` → `repository.updateTask(task.copy(isDone = !current))` → `triggerRefresh()`
- **Alarm Cancel**: When `isDone = true`, `alarmScheduler.cancelReminder(taskId)`
- **Alarm Restore**: When `isDone = false` (reactivation), re-schedule alarms only if `startTime > now`
- **UI Feedback**: Grey background, strikethrough title, muted alarm icon (SchedulerCards L128-189)
- **Voice Scope**: `RealScheduleBoard.upcomingItems` filters `!it.isDone` — completed tasks excluded from fuzzy match
- **Ship Criteria**: ✅ Checkbox toggles completion, grey/strikethrough applied, alarms managed
- **Test Cases**:
    - [x] Tap checkbox → task turns grey, strikethrough title
    - [x] Completed task excluded from voice fuzzy match (`RealScheduleBoard.kt:44`)
    - [x] Unchecking restores normal visual + re-enters voice scope
    - [x] Alarm cancelled on completion (`SchedulerViewModel.kt:358`)
    - [x] Alarm re-scheduled on reactivation if future time (`SchedulerViewModel.kt:362-370`)
    - [x] Alarm NOT re-scheduled on reactivation if past time (`SchedulerViewModel.kt:371-372`)

---

## Verification Commands

```bash
# Unit tests
./gradlew testDebugUnitTest --tests "*SchedulerLinter*"

# Build check
./gradlew :app-core:assembleDebug

# All Prism tests
./gradlew :app-core:testDebugUnitTest
```
