# Scheduler

> **Cerb-compliant spec** — Self-contained, all content inline.

---

## Overview

Scheduler manages task creation, timeline display, alarm cascading, and LLM-powered parsing. Uses **Memory Center's ScheduleBoard** for conflict detection.

**Key Distinction:**
- `ScheduleBoard` = Memory Center index (conflict check infrastructure)
- `ScheduledTaskRepository` = Scheduler feature (task CRUD)

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
| **4** | Input Classification + Multi-Task + Reschedule | 🔲 PLANNED | Input gate, batch create, reschedule parsing |
| **5** | Inspiration Storage | 🚢 SHIPPED | `InspirationRepository`, `CollapsibleInspirationShelf` |
| **6** | Batch Operations | 🔲 PLANNED | Multi-select delete, bulk reschedule |
| **7** | Insights Integration | 🔲 PLANNED | Connect to Analyst for task analytics |

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
        val highlights: String? = null,
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
        val reminderType: ReminderType?,
        val taskTypeHint: TaskTypeHint
    ) : LintResult()
    
    data class Error(val message: String) : LintResult()
}

enum class TaskTypeHint { MEETING, CALL, URGENT, PERSONAL }
enum class ReminderType { SINGLE, SMART_CASCADE }
```

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

## Task Type Inference

```kotlin
fun inferTaskType(title: String): TaskTypeHint {
    val lower = title.lowercase()
    return when {
        "会议" in lower || "meeting" in lower -> TaskTypeHint.MEETING
        "电话" in lower || "call" in lower -> TaskTypeHint.CALL
        "紧急" in lower || "urgent" in lower -> TaskTypeHint.URGENT
        else -> TaskTypeHint.PERSONAL
    }
}
```

---

## Alarm Cascade

Smart reminders fire multiple times before the event:

| Reminder Type | Cascade Pattern |
|---------------|-----------------|
| **Single** | `-15m` only |
| **Smart** | `-1h`, `-15m`, `-5m` |

Stored in `alarmCascade: List<String>?`:
```kotlin
listOf("-1h", "-15m", "-5m")
```

### Cascade Parsing

```kotlin
// "-1h" → 60 minutes before
// "-15m" → 15 minutes before
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
- **Deliverables**: `buildReschedulePrompt()` rewrite, `onReschedule()` in ViewModel

### � Wave 5: Inspiration Storage (SHIPPED)

Store non-schedulable input for future reference.

- **Ship Criteria**: Inspiration saved and retrievable
- **Test Cases**:
    - [x] "以后想学吉他" → saved to InspirationRepository
    - [x] Inspirations visible in `CollapsibleInspirationShelf`
    - [x] Swipe to delete inspiration
- **Deliverables**: `InspirationRepository.kt`, `RealInspirationRepository.kt`, `CollapsibleInspirationShelf.kt`

### 🔬 Wave 6: Batch Operations

Multi-select delete and bulk reschedule.

- **Ship Criteria**: Select N items, delete all in one action
- **Test Cases**:
    - [ ] Multi-select → delete count toast
    - [ ] Batch reschedule → all items shifted
- **Deliverables**: `toggleSelectionMode()`, batch UI

### 🔬 Wave 7: Insights Integration

Connect to Analyst for task completion analytics.

- **Ship Criteria**: "过去30天任务完成率" visible in Analyst
- **Test Cases**:
    - [ ] Completion rate calculated correctly
    - [ ] Trend visualization works
- **Deliverables**: Analyst data hook

---

## File Map

| Layer | Files |
|-------|-------|
| **Domain** | `ScheduledTaskRepository.kt`, `SchedulerLinter.kt`, `AlarmScheduler.kt`, `InspirationRepository.kt`, `FakeScheduledTaskRepository.kt` |
| **Data** | `RealScheduledTaskRepository.kt`, `RealAlarmScheduler.kt`, `RealInspirationRepository.kt`, `FakeInspirationRepository.kt` |
| **DI** | `SchedulerModule.kt` |
| **UI** | `SchedulerDrawer.kt`, `SchedulerViewModel.kt`, `SchedulerTimeline.kt`, `SchedulerCalendar.kt`, `SchedulerCards.kt`, `CollapsibleInspirationShelf.kt` |
| **Test** | `SchedulerLinterTest.kt` |

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
