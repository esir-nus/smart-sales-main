# L2/L3 On-Device Test Record: Wave 6 Transparent Mind & AgentViewModel

**Date**: 2026-03-11
**Tester**: CSLH-Frank
**Target Build**: `:app:assembleDebug`

---

## 1. Test Context & Entry State
* **Objective**: Verify Wave 6 Transparent Mind state streaming and UI rendering.
* **Testing Medium**: L3 Physical Device Test (Real LLM)
* **Initial Device State**: Fresh app launch.

## 2. Execution Plan
* **Trigger Action**: Sent prompt "请帮我安排周四下午2点回访一下鼎盛".
* **Input Payload**: Ambiguous entity name ("鼎盛").

## 3. Expected vs Actual Results

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | Response Bubble shows "Thinking..." phase followed by a single "Awaiting Clarification" message. | The prompt immediately resolved to "Awaiting Clarification", but the bubble was duplicated twice. | ❌ |
| **Log Evidence** | `D/RealUnifiedPipeline: 🚀 Starting unified pipeline ETL...` | Did not reach ETL phase; Disambiguation intercepted immediately. | 🟡 |
| **Negative Check**| The Response Bubble should not hang on a spinner. | It didn't hang, but it duplicated the terminal state. | ❌ |

---

## 4. Deviation & Resolution Log

* **Attempt 1 Failure**: UI duplicate "需要更多信息" bubbles appeared.
  - **Root Cause**: `AgentViewModel` appended terminal states (Response, Clarification, Tasks) to the `ChatMessage` history, but failed to clear the active `_uiState.value` back to `UiState.Idle`. Because `AgentChatScreen.kt` renders BOTH the history list and a floating "Legacy Fallback" bubble for the active `_uiState`, the states rendered twice concurrently.
  - **Resolution**: Modified `AgentViewModel` to immediately reset `_uiState.value = UiState.Idle` after pushing terminal states into `_history`. Fixed associated `AgentViewModelTest` assertions to rely on History bounds instead of ephemeral UI state. Complete pass.

## 5. Final Verdict
**[❌ FAILED -> REWORKING]**. The duplication bug is resolved in code, but the full 3-phase Transparent Mind flow still needs to be visually verified on L3 with a successful intent (one that doesn't intercept on ambiguous names).
