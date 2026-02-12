# Coach Mode

> **Cerb-compliant spec** — Lightweight conversational AI for sales coaching.  
> **OS Model**: Consumer of RAM (reads habits + entity context from SessionWorkingSet)
> **State**: SHIPPED

---

## Overview

Coach Mode is the **default conversational mode** in Prism. It provides fast, lightweight responses for sales technique discussions, quick questions, and general coaching interactions.

**Key Principle**: Coach is conversational and responsive. It should NOT show heavy "thinking" UI — users expect quick answers.

---

## Dependencies (via Cerb Interfaces)

| Interface | Cerb Shard | Purpose | OS Model Note |
|-----------|------------|---------|---------------|
| `Entity Knowledge (RAM Section 1)` | [entity-registry](../entity-registry/interface.md) | Structured entity graph ("Kotlin loads, LLM searches") | Kernel loads entities at session start |
| `ReinforcementLearner.loadUserHabits()` | [rl-module](../rl-module/interface.md) | Global user habits | Kernel → RAM Section 2 |
| `ReinforcementLearner.loadClientHabits()` | [rl-module](../rl-module/interface.md) | Entity-specific habits | Kernel → RAM Section 3 |
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
    └─▶ Entity Knowledge (RAM Section 1) ─▶ Structured entity graph as JSON
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

### 3.7 Simple Chat
User chats about sales techniques. Direct response, no special cards.

### 3.8 Memory-Enriched Response
User asks question at start of session ("What about that price issue?").
1. Kernel loads entity knowledge at session start (`EntityRepository.getAll()`)
2. Structured entity graph injected into LLM prompt (aliases, attributes, metrics, relationships)
3. LLM reads graph, matches aliases and cross-language references naturally
4. UI shows: response only (entity loading is transparent)

### 3.9 Suggest Analyst Switch
User asks complex data question.
1. LLM response includes `suggestAnalyst = true`
2. UI appends suggestion block: `[ 💡 Switch to Analyst ]`
3. User taps → Mode toggles

### 3.10 Conflict Detection (Inline)
Coach detects schedule conflict during conversation.
1. Context includes upcoming schedule from `ScheduleBoard.upcomingItems`
2. LLM mentions conflict in response
3. UI renders `ConflictCard` inline via `ConflictResolver` integration

### 3.11 Schedule Guidance (User Education)
When Coach surfaces schedule data and user expresses modification intent ("我想改时间", "能推迟吗"):
1. Coach recognizes modification intent but does NOT mutate schedule
2. Coach responds naturally and guides: "好的！你可以按录音键，直接说出修改需求，比如'推迟两小时'或'取消这个会议'"
3. User switches to scheduler mode → speaks change → handled by scheduler pipeline

> [!NOTE]
> **No cross-mode delegation.** Coach never calls `createScheduledTask()` or any scheduler mutation. Schedule modifications happen exclusively in scheduler mode. This is intentional — it keeps mutation ownership clean and educates users on the voice-first workflow.
>
> **System prompt addition**: 当用户在对话中表达修改日程的意图时，引导用户使用录音按钮："你可以按录音键，说出你的修改需求，比如'推迟两小时'或'取消会议'"。不要尝试代替用户修改日程。

---

## UI Contract

| Aspect | Behavior | Implementation |
|--------|----------|----------------|
| **ThinkingBox** | ✅ Shown (Truncated) | `AgentActivityBanner(autoCollapse = true)` |
| **Thinking Trace** | Truncated to 3 lines | `ThinkingPolicy` controls trace output |
| **Memory Search** | ⏳ Deferred | Will be shown via `AgentActivityController` trace |
| **Mode Indicator** | Purple theme | "Coach" label |
| **Analyst Suggestion** | Optional block | Appended to response content |

### ThinkingBox Behavior

- **Coach Mode**: `autoCollapse = true`. Shows "Thinking..." then collapses after 3 lines. User *feels* speed.
- **Analyst Mode**: `autoCollapse = false`. Fully expanded. Transparency matters.

### Analyst Suggestion Block

```
┌─────────────────────────────────────────────────┐
│ 💡 这看起来需要深度分析，建议切换到分析师模式     │
│          [ 切换到分析师 ]                        │
└─────────────────────────────────────────────────┘
```

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
