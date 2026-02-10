# Coach Interface (Blackbox Contract)

> **Consumer Contract** — Minimal API for features that need Coach responses.  
> **OS Model**: Consumer of RAM (reads habits + entity context from SessionWorkingSet)

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
| Access `ReinforcementLearner` directly | Pipeline reads habits from RAM (Sections 2 & 3) |
| Manually wire `entityIds` for habit context | Kernel auto-populates RAM; pipeline just reads |
| Block on response | This is a `suspend` function |
| Parse structured output manually | Pipeline returns typed response |

---

## Dependencies (Internal)

These are consumed **by the implementation**, not by you:

| Interface | Cerb Shard | Purpose | OS Model Note |
|-----------|------------|---------|---------------|
| `ContextBuilder` | — | Build enhanced context | **Kernel** — loads RAM |
| `Executor` | — | Execute LLM calls | Direct call |
| `MemoryRepository` | memory-center | Memory search | SSD query (search, not session data) |
| `ReinforcementLearner` | rl-module | Habit context | **Reads from RAM** Sections 2 & 3 |
| `AgentActivityController` | — | Visibility trace | Direct call |

---

## Provided Implementations

| Implementation | Location | Purpose |
|----------------|----------|---------|
| `FakeCoachPipeline` | `data/fakes/` | Returns mock data for UI development |
| `RealCoachPipeline` | `data/real/` | Full LLM + memory + habit integration |
