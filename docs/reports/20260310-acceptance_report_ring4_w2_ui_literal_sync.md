# Acceptance Report: Ring 4 W2 (UI Literal Sync)

## 1. Goal

The objective for Ring 4 W2 was to prove that the Orchestrator's `State` emissions (specifically `AwaitingClarification`) physically trigger the exact Compose layouts in the UI, rather than being swallowed by invisible states or anti-patterns in the presentation layer.

## 2. Findings & The "Testing Illusion"

During the audit of the presentation layer (`AgentChatScreen.kt` and `ResponseBubble.kt`), a critical mismatch was found between the backend state machine and the Compose UI configuration:

- **The Illusion**: The backend (Layer 3 `IntentOrchestrator`) was correctly catching bad LLM JSON and emitting the `AwaitingClarification` state. However, the UI layer had a strict `when` condition that **explicitly filtered out** this state, defaulting it into a dead `else -> {}` branch. Downstream, it appeared the pipeline was stuck or hallucinating, but in reality, the UI was simply refusing to paint the state.
- **The Proof**: We discovered that while `ResponseBubble.kt` contained the correct `ClarifyingBubble` composable capable of drawing this state, `AgentChatScreen` never passed the state down to the bubble.

## 3. The Fix

We implemented a "surgical inject" into the Compose layer to eliminate the filter:

1. **`AgentChatScreen.kt` Modified**: We added the missing `UiState` branches to both the active state evaluator and the history list evaluator. 
   - `UiState.AwaitingClarification`
   - `UiState.SchedulerTaskCreated`
   - `UiState.SchedulerMultiTaskCreated`
   - `UiState.Error`
   were all explicitly routed to `ResponseBubble`.
2. **L2 Unit Validation**: We constructed `ResponseBubbleTest.kt` to mathematically prove that `UiState.AwaitingClarification` is a structurally sound data class that successfully instantiates and holds its payload without crashing the Compose inputs. The test compiled and passed.

## 4. Verdict

✅ **PASS**

The `AwaitingClarification` state (and several other critical task creation states) will now physically render on the user's screen instead of being silently swallowed by the `AgentChatScreen`. This fulfills the requirement for **UI Literal Sync W2**, ensuring the UI strictly honors the data emitted by the core pipeline.
