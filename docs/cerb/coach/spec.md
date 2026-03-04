# Mascot (System I) - Formerly Coach

> **Cerb-compliant spec** — Lightweight, stateless, empathetic conversational UX.
> **OS Model**: RAM Application (Operates out-of-band via EventBus)
> **State**: SHIPPED (as "Coach"), Transitioning to "Mascot"

---

## Overview

The Mascot (formerly Coach Mode) handles **System I** interactions in the Dual-Engine Architecture. It is stateless, ephemeral, and acts as the proactive engagement layer.

**Key Principle**: The Mascot is conversational and responsive but **distinct** from formal analysis. It should NOT save its chatter to the main `SessionHistory`.

**Core Responsibilities**:
1. **Casual Greetings & Small Talk**: Responds to "Hello", "Thanks", etc.
2. **Noise Handling**: Gracefully ignores or deflects invalid ASR noise out-of-band.
3. **Proactive Dashboard UI**: Surfaces the Daily Briefing or Tips on app idle.

> **CRITICAL CONSTRAINT**: The Mascot does NOT handle critical system notifications (e.g., "Audio Transcription Complete", "Task Saved"). Those MUST route to native OS Toasts (`NotificationService`) for zero-latency, deterministic user feedback.

---

## Dependencies (via Cerb Interfaces)

| Interface | Cerb Shard | Purpose | OS Model Note |
|-----------|------------|---------|---------------|
| `Entity Knowledge (RAM Section 1)` | [entity-registry](../entity-registry/interface.md) | Structured entity graph ("Kotlin loads, LLM searches") | Kernel loads entities at session start |
| `ReinforcementLearner.loadUserHabits()` | [rl-module](../rl-module/interface.md) | Global user habits | Kernel → RAM Section 2 |
| `ReinforcementLearner.loadClientHabits()` | [rl-module](../rl-module/interface.md) | Entity-specific habits | Kernel → RAM Section 3 |
| `ScheduledTaskRepository.queryByDateRange()` | [scheduler](../scheduler/interface.md) | Sticky Notes — top 3 upcoming tasks as greeting context | Kernel loads at session start |
| `ClientProfileHub.getQuickContext()` | [client-profile-hub](../client-profile-hub/interface.md) | Entity snapshot for context | — |
| `ScheduleBoard.checkConflict()` | [memory-center](../memory-center/interface.md) | Inline conflict detection + schedule guidance | — |
| `ConflictResolver.resolve()` | [conflict-resolver](../conflict-resolver/interface.md) | LLM conflict resolution | — |

---

## Pipeline Flow

```
User Input
    │
    ▼
[Context Builder (Kernel)]
    ├─▶ RAM Section 1 ─▶ Entity context (who is being discussed)
    ├─▶ RAM Section 2 ─▶ User habits (global preferences)
    ├─▶ RAM Section 3 ─▶ Client habits (auto-populated by Kernel)
    ├─▶ Entity Knowledge (RAM Section 1) ─▶ Structured entity graph as JSON
    └─▶ Sticky Notes ─▶ Top 3 upcoming tasks (scheduleContext)
    │
    ▼
[Qwen-Plus] ──stream──▶ [ChatPublisher] ──▶ UI
    │                         │
    └─ Response flags ─────▶  ├─▶ suggestAnalyst? → Suggestion block
                              └─▶ async → Memory Writer → Hot Zone
```

> [!IMPORTANT]
> **OS Model Simplification**: Coach does NOT manually wire `entityIds` or call `getHabitContext(entityIds)`. The Kernel loads habit context into RAM Sections 2 & 3 automatically. Coach just reads what's on the RAM.
> This eliminates the Coach Mode missing-client-habits bug.

---

## Domain Models

### CoachResponse

```kotlin
sealed class CoachResponse {
    /**
     * Standard chat response with optional metadata.
     * 
     * @param content The coach's reply text
     * @param suggestAnalyst True if LLM recommends Analyst mode
     * @param memoryHits Context entries used (for transparency, optional)
     */
    data class Chat(
        val content: String,
        val suggestAnalyst: Boolean = false,
        val memoryHits: List<MemoryHit> = emptyList()
    ) : CoachResponse()
}
```

### ChatTurn

```kotlin
data class ChatTurn(
    val role: String,  // "user" | "assistant"
    val content: String
)
```

---

## User Flows

### 3.6 Contextual Schedule Awareness

Coach uses a **context-driven** injection strategy — schedule data is always available in the prompt, but the LLM decides when and how to mention it based on user input:

| Input Type | Schedule Context Behavior |
|------------|--------------------------|
| **Short greeting** ("你好", "嗨", "早上好") | Coach naturally brings up today's schedule and gives one suggestion |
| **Content-rich** (sharing stories, asking questions, reporting) | Coach responds to content first; schedule is background reference only |
| **Schedule-related** ("今天有什么安排", "我下午忙吗") | Coach directly addresses schedule |

**Data source**: `ScheduledTaskRepository.queryByDateRange(today, today+3)` → filter non-done → sort by urgency (L1→L3) then time → take 3.

**When schedule is empty**: No reminder section injected. Coach greets normally.

> [!NOTE]
> **Tech Debt**: A confidence-based reminder interceptor is logged for future trial — LLM classifier decides interception points (greeting/noise, agenda talk, wrap-up). Current workaround uses prompt-level guidance. See `tracker.md`.

### 3.7 Simple Chat
User chats about sales techniques. Direct response, no special cards.

### 3.8 Memory-Enriched Response
User asks question at start of session ("What about that price issue?").
1. Kernel loads entity knowledge at session start (`EntityRepository.getAll()`)
2. Structured entity graph injected into LLM prompt (aliases, attributes, metrics, relationships)
3. LLM reads graph, matches aliases and cross-language references naturally
4. UI shows: response only (entity loading is transparent)

### 3.9 Conflict Detection (Inline)
Coach detects schedule conflict during conversation.
1. Context includes upcoming schedule from `ScheduleBoard.upcomingItems`
2. LLM mentions conflict in response
3. UI renders `ConflictCard` inline via `ConflictResolver` integration

### 3.10 Schedule Guidance (User Education)
When Coach surfaces schedule data and user expresses modification intent ("我想改时间", "能推迟吗"):
1. Coach recognizes modification intent but does NOT mutate schedule
2. Coach responds naturally and guides: "好的！你可以按录音键，直接说出修改需求，比如'推迟两小时'或'取消这个会议'"
3. User switches to scheduler mode → speaks change → handled by scheduler pipeline

> [!NOTE]
> **No cross-mode delegation.** Coach never calls `createScheduledTask()` or any scheduler mutation. Schedule modifications happen exclusively in scheduler mode. This is intentional — it keeps mutation ownership clean and educates users on the voice-first workflow.
>
> **System prompt addition**: 当用户在对话中表达修改日程的意图时，引导用户使用录音按钮："你可以按录音键，说出你的修改需求，比如'推迟两小时'或'取消会议'"。不要尝试代替用户修改日程。

### 3.12 Home Hero Dashboard (Daily Brief)

When no conversation history exists (`history.isEmpty()`), the hero screen shows a **visual daily briefing** instead of a static greeting:

```
┌─────────────────────────────────────────┐
│          ✨ 下午好, Frank                │
│                                         │
│  📋 待办 (3)                            │
│  ┌─────────────────────────────────┐    │
│  │ 🔴 14:00 客户签约 (L1)          │    │
│  │ 🟡 16:30 产品演示 (L2)          │    │
│  │ ⚪ 18:00 回复邮件 (L3)          │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ✅ 已完成                              │
│  ┌─────────────────────────────────┐    │
│  │ ✓ 供应商拜访 (L1)               │    │
│  │ ✓ 团队周会 (L2)                 │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

**Data sources**:
- **Upcoming (top 3)**: `ScheduledTaskRepository.queryByDateRange(today, today+3)` → `isDone = false` → exclude `FIRE_OFF` → sort by urgency then time → take 3
- **Accomplished (top 2)**: `ScheduledTaskRepository.queryByDateRange(today-7, today)` → `isDone = true` → sort by urgency then completedAt desc → take 2
- **Greeting**: Dynamic based on hour — 早上好 (5-11) / 下午好 (12-17) / 晚上好 (18+)

**Behavior**:
- Hero disappears when conversation starts (existing behavior)
- No LLM calls — pure Kotlin → Compose, deterministic
- Urgency color: L1=Red, L2=Amber, L3=Grey
- Empty state: If no tasks exist, show greeting only (current behavior)

> [!IMPORTANT]
> This decouples the "daily brief" duty from the Coach prompt. The hero handles visual overview; the Coach handles conversational context. Clean separation.

---

## UI Contract

| Aspect | Behavior | Implementation |
|--------|----------|----------------|
| **ThinkingBox** | ✅ Shown (Truncated) | `AgentActivityBanner(autoCollapse = true)` |
| **Thinking Trace** | Truncated to 3 lines | `ThinkingPolicy` controls trace output |
| **Memory Search** | ⏳ Deferred | Will be shown via `AgentActivityController` trace |
| **Mode Indicator** | Purple theme | "Coach" label |

### ThinkingBox Behavior

- **Coach Mode**: `autoCollapse = true`. Shows "Thinking..." then collapses after 3 lines. User *feels* speed.
- **Analyst Mode**: `autoCollapse = false`. Fully expanded. Transparency matters.

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Interface + Fake | ✅ SHIPPED | `CoachPipeline` interface, `FakeCoachPipeline` |
| **2** | Real LLM + Context | ✅ SHIPPED | `RealCoachPipeline`, system prompt, session history |
| **3** | Memory + Habit | ✅ SHIPPED | `MemoryRepository.search()`, `loadUserHabits()` / `loadClientHabits()` via RAM |
| **4** | Analyst Suggestion | ✅ SHIPPED | `suggestAnalyst` flag parsing, UI block |
| **5** | OS Model Upgrade | ✅ SHIPPED | Habits read from RAM Sections 2 & 3, no manual `entityIds` wiring |

> [!NOTE]
> Wave 4 MVP uses keyword heuristic (`分析`, `数据`). LLM-based detection planned for Wave 4.5.

---

## Wave 1 Ship Criteria

**Goal**: Interface + Fake that proves wiring.

- **Exit Criteria**:
  - [ ] `CoachPipeline` interface in `domain/coach/`
  - [ ] `CoachResponse` and `ChatTurn` types
  - [ ] `FakeCoachPipeline` returns mock responses
  - [ ] Build passes: `./gradlew :app-prism:compileDebugKotlin`

- **Test Cases**:
  - [ ] L1: Fake returns `CoachResponse.Chat` with content
  - [ ] L1: Fake sets `suggestAnalyst = true` for "分析" keyword

---

## Wave 2 Ship Criteria

**Goal**: Real LLM responses with session context.

- **Exit Criteria**:
  - [ ] `RealCoachPipeline` with `Executor` integration
  - [ ] Coach system prompt (sales coaching persona)
  - [ ] Session history passed to Context Builder

- **Test Cases**:
  - [ ] L2: Chat → receive coherent sales coaching response
  - [ ] L2: Multi-turn context works

---

## Wave 3 Ship Criteria

**Goal**: Memory + Habit integration via Cerb interfaces.

- **Exit Criteria**:
  - [x] Context Builder calls `MemoryRepository.search()` on **First Turn**
  - [x] Kernel calls `loadUserHabits()` (S2) + `loadClientHabits()` (S3) via RAM
  - [x] Habit context included in LLM prompt via `## 用户偏好` section

- **Test Cases**:
  - [ ] L2: Vague query → memory-enriched response
  - [ ] L2: User habits affect response style

---

## Wave 4 Ship Criteria

**Goal**: Analyst suggestion flow.

- **Exit Criteria**:
  - [ ] LLM structured output includes `suggestAnalyst` flag
  - [ ] UI renders suggestion block when flag is true
  - [ ] Tap triggers mode switch

- **Test Cases**:
  - [ ] L2: Complex data question → suggestion block appears
  - [ ] L3: Tap suggestion → mode switches to Analyst

---

## File Map

| Layer | Files |
|-------|-------|
| **Domain** | `CoachPipeline.kt`, `CoachResponse.kt` |
| **Domain (shared)** | `EnhancedContext.kt` (contains `ChatTurn`, `MemoryHit`) |
| **Data** | `RealCoachPipeline.kt`, `FakeCoachPipeline.kt` |
| **DI** | `PrismModule.kt` (shared bindings) |

---

## Verification Commands

```bash
# Build check
./gradlew :app-prism:compileDebugKotlin

# Run Coach tests
./gradlew :app-prism:testDebugUnitTest --tests "*Coach*"
```
