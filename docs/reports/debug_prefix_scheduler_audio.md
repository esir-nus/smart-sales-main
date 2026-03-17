### Pre-Fix Report: Scheduler UI Audio Dropping ToolDispatches

**1. Evidence-Based Root Cause**:
The `adb logcat` output exactly corroborates the symptom. 
The LLM accurately parsed the intent and the linter decoded it cleanly:
`03-16 13:37:24.505 I VALVE_PROTOCOL: 🚦 [LINTER_DECODED] | Size: 1 | Linter successfully decoded strict data class`

By tracing the Kotlin pipeline from this point, the root cause is crystal clear in `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt:266`:
```kotlin
intentOrchestrator.processInput(result.text, isVoice = true).collect { res ->
    if (res is PipelineResult.ConversationalReply) { ... }
    else if (res is PipelineResult.MutationProposal) { ... }
    // ❌ BUG: PipelineResult.ToolDispatch is completely IGNORED
}
```
During Wave 16, Fast-Track scheduling was demoted from `IntentOrchestrator` to a System III Plugin (`ToolDispatch("CREATE_TASK")`). While `AgentViewModel` (the global chat) was updated to execute `ToolDispatch` events, `SchedulerViewModel` (which powers the dedicated UI "rec" button) was overlooked. Thus, it swallows the LLM's response silently.

**2. Spec Alignment Gate (from 01-senior-reviewr)**:
- **Requirement**: "The `rec` button should work exactly as the physical badge tap button that triggers the workflow."
- **Current State**: `SchedulerViewModel` lacks Plugin routing wires.
- **Verdict**: This is a pure Code Bug stemming from incomplete Wave 16 module extraction routing.

**3. Proposed Fix**:
- Inject `ToolRegistry` (System III) directly into `SchedulerViewModel`.
- In `processAudio()`, intercept `PipelineResult.ToolDispatch` events.
- Invoke `toolRegistry.executeTool(res.toolId, ...)` inline.
- Collect the execution success state, update `_pipelineStatus.value = "✅ 搞定"` and trigger `_refreshTrigger.tryEmit(Unit)` so the dashboard cards instantly update without needing complex Markdown renders.

**4. Proactive Risk Assessment & Readiness (Crucial)**:
- **Readiness Score**: **95%** (Verified/Total Assumptions × 60) + Evidence(15) + Risk(20). 
- **Potential Gaps**: The Scheduler Tool Plugin usually emits complex `UiState.MarkdownStrategyState` cards back. The Scheduler Drawer UI doesn't have a chat window to render these. The fix must intercept the `Response` state and convert it to a simple status toast (`_pipelineStatus`) to match the Drawer's UX paradigm.
- **Blast Radius**: Modifying `SchedulerViewModel`'s constructor will break its unit tests if the `ToolRegistry` dependency is not mocked in the test suite. We will need to update TestFakes or the DI configuration simultaneously.
- **L1/L2 Testability**: HIGH. We can mock `PipelineResult.ToolDispatch` being emitted and assert `_pipelineStatus` and `_refreshTrigger` behavior mathematically in L1 tests.
