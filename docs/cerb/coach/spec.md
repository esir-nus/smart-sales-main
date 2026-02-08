# Coach Mode

> **Cerb-compliant spec** — Lightweight conversational AI for sales coaching.

---

## Overview

Coach Mode is the **default conversational mode** in Prism. It provides fast, lightweight responses for sales technique discussions, quick questions, and general coaching interactions.

**Key Principle**: Coach is conversational and responsive. It should NOT show heavy "thinking" UI — users expect quick answers.

---

## Dependencies (via Cerb Interfaces)

| Interface | Cerb Shard | Purpose |
|-----------|------------|---------|
| `MemoryRepository.search()` | [memory-center](../memory-center/interface.md) | Search for relevant context |
| `ReinforcementLearner.getHabitContext()` | [rl-module](../rl-module/interface.md) | User/client habit enrichment |
| `ClientProfileHub.getQuickContext()` | [client-profile-hub](../client-profile-hub/interface.md) | Entity snapshot for context |
| `ScheduleBoard.checkConflict()` | [memory-center](../memory-center/interface.md) | Inline conflict detection |
| `ConflictResolver.resolve()` | [conflict-resolver](../conflict-resolver/interface.md) | LLM conflict resolution |

---

## Pipeline Flow

```
User Input
    │
    ▼
[Context Builder]
    ├─▶ Session Context (Primary: In-Chat History)
    ├─▶ ReinforcementLearner.getHabitContext() ─▶ User/client habits
    ├─▶ ClientProfileHub.getQuickContext() ─▶ Entity snapshots (optional)
    └─▶ MemoryRepository.search() ─▶ Relevant memory (First turn / High ambiguity)
    │
    ▼
[Qwen-Plus] ──stream──▶ [ChatPublisher] ──▶ UI
    │                         │
    └─ Response flags ─────▶  ├─▶ suggestAnalyst? → Suggestion block
                              └─▶ async → Memory Writer → Hot Zone
```

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
        val memoryHits: List<MemoryEntry> = emptyList()
    ) : CoachResponse()
}
```

### ChatTurn

```kotlin
data class ChatTurn(
    val role: String,  // "user" | "assistant"
    val content: String,
    val timestamp: Long? = null
)
```

---

## User Flows

### 3.7 Simple Chat
User chats about sales techniques. Direct response, no special cards.

### 3.8 Memory-Enriched Response
User asks question at start of session ("What about that price issue?").
1. Context Builder detects **First Turn** (or explicit entity mention)
2. Calls `MemoryRepository.search("price issue")`
3. LLM receives enriched context → coherent response
4. UI shows: response only (memory search is transparent)

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
| **3** | Memory + Habit | ✅ SHIPPED | `MemoryRepository.search()`, `getHabitContext()` integration |
| **4** | Analyst Suggestion | ✅ SHIPPED | `suggestAnalyst` flag parsing, UI block |

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
  - [ ] Context Builder calls `MemoryRepository.search()` on **First Turn**
  - [ ] Context Builder calls `ReinforcementLearner.getHabitContext()`
  - [ ] Habit context included in LLM prompt

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
| **Domain** | `CoachPipeline.kt`, `CoachResponse.kt`, `ChatTurn.kt` |
| **Data** | `RealCoachPipeline.kt`, `FakeCoachPipeline.kt` |
| **DI** | `CoachModule.kt` |

---

## Verification Commands

```bash
# Build check
./gradlew :app-prism:compileDebugKotlin

# Run Coach tests
./gradlew :app-prism:testDebugUnitTest --tests "*Coach*"
```
