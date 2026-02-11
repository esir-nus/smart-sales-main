# Conflict Resolver

> **Cerb-compliant spec** — Self-contained, all content inline.
> **State**: SHIPPED

---

## Overview

Conflict Resolver handles user-driven resolution of schedule conflicts detected by Memory Center's `ScheduleBoard`. Uses LLM to parse natural language resolution intent and executes corresponding actions.

**Relationship to Scheduler:**
- Scheduler detects conflicts via ScheduleBoard
- Conflict Resolver processes user's resolution choice
- Resolution actions feed back to Scheduler (delete/reschedule)

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Resolution Service | ✅ SHIPPED | `RealConflictResolver`, `ConflictAction` |
| **2** | UI Integration | ✅ SHIPPED | `ConflictCard`, ViewModel wiring |
| **3** | Cross-Mode Conflict Reminder | ⏸️ BLOCKED | Requires Coach/Analyst modes |

---

## Domain Models

### ConflictResolution & ConflictAction

```kotlin
// 冲突解决结果 — 支持复合指令（如"取消A，改期B"）
data class ConflictResolution(
    val actions: List<ConflictAction>,
    val reply: String
)

data class ConflictAction(
    val action: ActionType,
    val taskToRemove: String?,
    val taskToReschedule: String?,
    val rescheduleText: String?
)

enum class ActionType {
    KEEP_A,      // Remove taskB, keep taskA
    KEEP_B,      // Remove taskA, keep taskB
    RESCHEDULE,  // Reschedule one task to new time
    COEXIST,     // Keep both tasks, clear overlap warning
    NONE         // Parsing failed / unclear intent
}
```

---

## Resolution Pipeline

```
ConflictCard (user types intent)
    ↓
RealConflictResolver.resolve(userMessage, taskA, taskB)
    ↓
DashscopeExecutor (LLM call with conflict prompt)
    ↓
parseConflictAction() → ConflictAction
    ↓
SchedulerViewModel.handleConflictResolution()
    ↓
[KEEP] → taskRepository.deleteItem() + clearWarning()
[RESCHEDULE] → onReschedule(id, text) + clearWarning()
[COEXIST] → clearWarning()
    ↓
ScheduleBoard.refresh()
```

---

## LLM Prompt Structure

```
你是日程助手。用户有两个时间重叠的任务（重叠是可以接受的，未必一定要删除一个）：
任务A: "${taskA.title}" (${formatTime(taskA.scheduledAt)})
任务B: "${taskB.title}" (${formatTime(taskB.scheduledAt)})

用户说: "$userMessage"

返回JSON:
{
  "action": "keep_a" | "keep_b" | "reschedule" | "coexist" | "none",
  "taskId": "要删除或改期的任务ID",
  "reply": "友好的回复文本",
  "target": "要改期的任务ID (仅RESCHEDULE)",
  "time": "改期时间 (仅RESCHEDULE)"
}

action 解释:
- keep_a: 保留A，删除B
- keep_b: 保留B，删除A
- reschedule: 改期其中一个
- coexist: 两个都保留 (用户表示都没问题/都要做)
- none: 无法理解
```

---

## Wave Ship Criteria

### 🔬 Wave 1: Core Resolution Service (SHIPPED)

**Shipped**: 2026-02-04

- **Ship Criteria**: LLM parses resolution intent into structured action
- **Test Cases**:
    - [x] "保留第一个" → KEEP_A
    - [x] "取消会议" → KEEP_B (if B is meeting)
    - [x] "把午餐改到明天" → RESCHEDULE
    - [x] Invalid response → NONE with fallback message
- **Deliverables**: `ConflictAction.kt`, `RealConflictResolver.kt`

### 🔬 Wave 2: UI Integration (SHIPPED)

**Shipped**: 2026-02-05

- **Ship Criteria**: End-to-end flow from UI tap to action execution
- **Test Cases**:
    - [x] ConflictCard displays taskA vs taskB
    - [x] User input triggers resolution
    - [x] Action callback reaches ViewModel
    - [x] Task deleted or rescheduled correctly
- **Deliverables**: `ConflictCard.kt`, `SchedulerTimeline.kt` wiring, `SchedulerViewModel.handleConflictResolution()`

---

## Visual Conflict Indicators (Wave 2.5)

**Shipped**: 2026-02-09

When a conflict is detected, visual indicators appear on task cards to guide users toward resolution:

### Visual Behavior

| Card Type | Visual Treatment |
|-----------|-----------------|
| **Causing card** (newest) | Amber border (3dp) + Amber background pulse |
| **Conflicted cards** (existing overlap) | Amber border (2dp) static, alpha 0.6 |
| **Normal cards** | No border |

> **UX Note**: Animation stops when card is expanded to prevent distraction during resolution.

### Animation Spec

**Breathing glow** (causing card only, when collapsed):
- **Background**: Amber tint pulse (alpha `0.0 → 0.12 → 0.0`)
- **Border**: Amber stroke (alpha `0.6 → 1.0 → 0.6`)
- Duration: `1.5s` per cycle
- Easing: `FastOutSlowInEasing`

**Collapsed Header Layout**:
1. Warning icon + "时间重叠" label
2. Task A details (Title + Time + Duration)
3. Task B details (Title + Time + Duration)
4. Overlap duration (e.g., "重叠 30 分钟")

### State Model

```kotlin
// UI State
enum class ConflictVisual {
    NONE,       // 正常状态，无冲突
    IN_GROUP,   // 琥珀色边框 (冲突组内的已有卡片)
    CAUSING     // 琥珀色边框 + 呼吸发光 (引发冲突的新卡片)
}

// ViewModel Tracking
conflictedTaskIds: Set<String>  // 所有冲突任务 ID
causingTaskId: String?          // 引发冲突的任务 ID
```

### Resolution Behavior

All resolution actions (`KEEP_A`, `KEEP_B`, `RESCHEDULE`, `COEXIST`) clear both visual indicators and the text warning banner immediately.

### Accessibility

**Text warning banner** (`conflictWarning: String?`) is retained alongside visual indicators to ensure accessibility compliance.

---

### 🔬 Wave 3: Cross-Mode Conflict Reminder (⏸️ BLOCKED)

> **Goal**: Coach and Analyst modes naturally remind users about unresolved conflicts by reading ScheduleBoard state.
>
> **⚠️ BLOCKED**: Requires Coach/Analyst modes to be implemented first.

#### Overview

Unresolved conflicts persist in ScheduleBoard with metadata. When Coach/Analyst builds context, they see pending conflicts and can inject natural reminders into their responses.

**Key Principle**: Coach/Analyst **do not resolve** conflicts. They only **remind** users to open Scheduler drawer.

#### Data Flow

```
ScheduleBoard
    ↓ (1) stores unresolved conflicts with timestamp
    ↓
ConflictState: { conflictId, taskA, taskB, detectedAt, resolvedAt? }
    ↓
    ↓ (2) Coach/Analyst ContextBuilder reads pending conflicts
    ↓
pendingConflicts: List<ConflictState> where resolvedAt == null
    ↓
    ↓ (3) LLM prompt injection (if count > 0)
    ↓
"[System Note: 用户有 {N} 个日程冲突待处理，可以提醒用户下拉日程抽屉处理]"
    ↓
    ↓ (4) LLM naturally mentions in response
    ↓
Coach: "对了，你还有个日程冲突没处理，记得下拉查看哦。"
```

#### New Model: ConflictState

```kotlin
data class ConflictState(
    val conflictId: String,         // Generated on detection
    val taskAId: String,
    val taskBId: String,
    val detectedAt: Long,           // When conflict was first detected
    val resolvedAt: Long?,          // Null if pending, timestamp if resolved
    val resolvedBy: ResolvedBy?     // How it was resolved
)

enum class ResolvedBy {
    KEEP_A,         // User chose to keep A, delete B
    KEEP_B,         // User chose to keep B, delete A
    RESCHEDULE,     // User rescheduled one task
    AUTO_EXPIRED    // One or both tasks passed without action
}
```

#### ScheduleBoard Enhancement

```kotlin
interface ScheduleBoard {
    // ... existing methods ...
    
    // NEW: Conflict state tracking
    val pendingConflicts: StateFlow<List<ConflictState>>
    
    suspend fun recordConflict(taskA: ScheduleItem, taskB: ScheduleItem): String  // Returns conflictId
    suspend fun resolveConflict(conflictId: String, resolvedBy: ResolvedBy)
    // DEFERRED: expireOldConflicts() — auto-expire logic for passed tasks
}
```

#### Context Injection Rules

| Condition | Inject? | Prompt Fragment |
|-----------|---------|-----------------|
| `pendingConflicts.count == 0` | ❌ | — |
| `pendingConflicts.count == 1` | ✅ | "用户有 1 个日程冲突待处理" |
| `pendingConflicts.count >= 2` | ✅ | "用户有 {N} 个日程冲突待处理" |
| Last reminded < 10 min ago | ❌ | Cooldown (avoid nagging) |

#### Reminder Cooldown

```kotlin
data class ReminderCooldown(
    val lastRemindedAt: Long?,
    val cooldownMinutes: Int = 10
) {
    fun shouldRemind(now: Long): Boolean {
        return lastRemindedAt == null || 
               (now - lastRemindedAt) > cooldownMinutes * 60_000L
    }
}
```

#### Ship Criteria

- **Primary**: Coach/Analyst responses naturally mention pending conflicts when appropriate
- **Secondary**: No nagging (respects cooldown)

#### Test Cases

- [ ] Create conflict → `pendingConflicts.count == 1`
- [ ] Resolve conflict → `pendingConflicts.count == 0`
- [ ] Coach context includes system note when conflict pending
- [ ] Coach context excludes note when cooldown active
- [ ] _(DEFERRED)_ Task expires → conflict auto-marked as `AUTO_EXPIRED`
- [ ] LLM produces natural Chinese reminder (not robotic)

#### Deliverables

| Layer | File | Change |
|-------|------|--------|
| **Memory Center** | `ScheduleBoard.kt` | Add `pendingConflicts`, `recordConflict()`, `resolveConflict()` |
| **Memory Center** | `RealScheduleBoard.kt` | Implement conflict state tracking |
| **Domain** | `ConflictState.kt` | New model |
| **Context** | `RealContextBuilder.kt` | Inject conflict system note |
| **ViewModel** | `SchedulerViewModel.kt` | Call `resolveConflict()` after resolution |

#### Dependencies

- **Produces**: `ConflictState` for ContextBuilder consumption
- **Consumes**: Existing `checkConflict()` from ScheduleBoard
- **No UI changes**: Reminder is LLM-generated text, not UI component

---

## File Map

| Layer | Files |
|-------|-------|
| **Domain** | `ConflictAction.kt` |
| **Data** | `RealConflictResolver.kt` |
| **UI** | `ConflictCard.kt`, `SchedulerTimeline.kt` (Conflict branch), `SchedulerStates.kt` (Conflict model) |
| **ViewModel** | `SchedulerViewModel.kt` (`handleConflictResolution`, `toggleConflictExpansion`) |
| **DI** | `HiltComponentProvider.kt` (entry point for ConflictCard) |

---

## Error Handling

| Scenario | Response |
|----------|----------|
| LLM timeout | `NONE` + "请求超时，请重试" |
| Invalid JSON | `NONE` + "解析失败，请重试" |
| Missing taskId | `NONE` + fallback reply |
| Network error | `NONE` + "网络错误" |

---

## Verification Commands

```bash
# Build check
./gradlew :app-prism:assembleDebug

# Unit tests (if added)
./gradlew testDebugUnitTest --tests "*ConflictResolver*"
```

---

## Dependencies

- **Consumes**: `ScheduleItem` from Memory Center
- **Uses**: `DashscopeExecutor` for LLM calls
- **Publishes to**: `ScheduledTaskRepository` (deletes), `SchedulerViewModel.onReschedule()` (reschedules)
