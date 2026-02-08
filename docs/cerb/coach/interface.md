# Coach Interface (Blackbox Contract)

> **Consumer Contract** — Minimal API for features that need Coach responses.

---

## Interface

```kotlin
interface CoachPipeline {
    /**
     * Process user input and return a Coach response.
     * 
     * @param input User message text
     * @param sessionHistory Prior messages in current session
     * @return CoachResponse.Chat with content and optional flags
     */
    suspend fun process(
        input: String,
        sessionHistory: List<ChatTurn> = emptyList()
    ): CoachResponse
}
```

---

## Input/Output Types

### CoachResponse

```kotlin
sealed class CoachResponse {
    /**
     * Standard chat response.
     * @param content The coach's reply text
     * @param suggestAnalyst True if LLM recommends Analyst mode
     * @param memoryHits Context entries used (for transparency)
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

## Usage Example

```kotlin
// In ViewModel
val response = coachPipeline.process(userInput, chatHistory)

when (response) {
    is CoachResponse.Chat -> {
        displayMessage(response.content)
        if (response.suggestAnalyst) {
            showAnalystSuggestionBlock()
        }
    }
}
```

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Call for Analyst-mode queries | Use `AnalystPipeline` instead |
| Access `MemoryRepository` directly | Let pipeline handle memory search |
| Access `ReinforcementLearner` directly | Let pipeline handle habit context |
| Block on response | This is a `suspend` function |
| Parse structured output manually | Pipeline returns typed response |

---

## Dependencies (Internal)

These are consumed **by the implementation**, not by you:

| Interface | Cerb Shard | Purpose |
|-----------|------------|---------|
| `ContextBuilder` | — | Build enhanced context |
| `Executor` | — | Execute LLM calls |
| `MemoryRepository` | memory-center | Memory search |
| `ReinforcementLearner` | rl-module | Habit context |
| `AgentActivityController` | — | Visibility trace |

---

## Provided Implementations

| Implementation | Location | Purpose |
|----------------|----------|---------|
| `FakeCoachPipeline` | `data/fakes/` | Returns mock data for UI development |
| `RealCoachPipeline` | `data/real/` | Full LLM + memory + habit integration |
