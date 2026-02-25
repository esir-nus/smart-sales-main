# Analyst Orchestrator Interface (Blackbox Contract)

> **Consumer Contract** -- Minimal API for features that need Analyst Mode responses.  
> **OS Model**: RAM Application (reads context from SessionWorkingSet via ContextBuilder)

---

## Interface

```kotlin
interface AnalystPipeline {
    /**
     * Handle any user input in Analyst Mode.
     * The orchestrator decides the current phase and routes accordingly.
     *
     * @param input User message text
     * @param sessionHistory Prior messages in current session
     * @return AnalystResponse (one of the sealed variants)
     */
    suspend fun handleInput(
        input: String,
        sessionHistory: List<ChatTurn> = emptyList()
    ): AnalystResponse

    /**
     * Observe the current orchestrator state.
     * UI uses this to decide what to render (chat, planner table, task board).
     */
    val state: StateFlow<AnalystState>
}
```

---

## Output Types

### AnalystState

```kotlin
enum class AnalystState {
    IDLE,           // Waiting for user input
    CONSULTING,     // Phase 1: Evaluating intent
    PROPOSAL,       // Plan generated, waiting for user confirmation
    INVESTIGATING,  // Phase 3: LLM reading EnhancedContext, reasoning
    RESULT          // Analysis delivered, Task Board mounted
}
```

### AnalystResponse

```kotlin
sealed class AnalystResponse {
    /**
     * Phase 1: Conversational clarification.
     * Consumer renders as normal chat bubble.
     */
    data class Chat(
        val content: String,
        val memoryHits: List<MemoryHit> = emptyList()
    ) : AnalystResponse()

    /**
     * Phase 2: Structured plan for user confirmation.
     * Consumer renders as PlannerTable bubble.
     * User must confirm before investigation begins.
     */
    data class Plan(
        val title: String,
        val summary: String,
        val steps: List<AnalysisStep>
    ) : AnalystResponse()

    /**
     * Phase 3 complete: Final analysis with optional follow-up actions.
     * Consumer renders analysis in chat + dynamic Task Board.
     */
    data class Analysis(
        val content: String,
        val suggestedWorkflows: List<WorkflowSuggestion> = emptyList()
    ) : AnalystResponse()
}

data class AnalysisStep(
    val stepId: String,
    val description: String,
    val status: StepStatus
)

enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class WorkflowSuggestion(
    val workflowId: String,
    val label: String
)
```

---

## Usage Example

```kotlin
// In ViewModel
val response = analystPipeline.handleInput(userInput, chatHistory)

when (response) {
    is AnalystResponse.Chat -> {
        // Normal chat bubble (Phase 1 clarification)
        displayMessage(response.content)
    }
    is AnalystResponse.Plan -> {
        // Render PlannerTable bubble with confirmation gate
        displayPlannerTable(response)
        showConfirmationButton()  // "OK to proceed?"
    }
    is AnalystResponse.Analysis -> {
        // Render final analysis + mount Task Board
        displayMessage(response.content)
        if (response.suggestedWorkflows.isNotEmpty()) {
            mountTaskBoard(response.suggestedWorkflows)
        }
    }
}
```

---

## You Should NOT

| Don't | Do Instead |
|-------|------------|
| Call for Coach-mode queries | Use `CoachPipeline` instead |
| Access `ContextBuilder` directly | Pipeline calls `ContextBuilder.build()` internally |
| Access `MemoryRepository` directly | Pipeline reads via EnhancedContext (RAM) |
| Access `ClientProfileHub` directly | Pipeline implementation reads File Explorer (`ClientProfileHub`) when RAM is insufficient |
| Auto-execute investigation without user OK | Always wait for confirmation at `PROPOSAL` state |
| Parse LLM JSON output manually | Pipeline returns typed `AnalystResponse` |
| Render Task Board before analysis completes | Task Board appears only after `RESULT` state |

---

## Dependencies (Internal)

These are consumed **by the implementation**, not by you:

| Interface | Cerb Shard | Purpose | OS Model Note |
|-----------|------------|---------|---------------|
| `ContextBuilder` | session-context | Build EnhancedContext | **Kernel** -- loads RAM |
| `Executor` | -- | Execute LLM calls (qwen3-max) | Direct call |
| `AgentActivityController` | -- | Thinking Trace visibility | Direct call |
| `ClientProfileHub` | client-profile-hub | Deep entity context (if RAM insufficient) | **File Explorer** -- reads SSD |

---

## Provided Implementations

| Implementation | Location | Purpose |
|----------------|----------|---------|
| `FakeAnalystPipeline` | `data/fakes/` | Returns mock data for UI development and L2 testing |
| `RealAnalystPipeline` | `data/real/` | Full LLM + context integration |
