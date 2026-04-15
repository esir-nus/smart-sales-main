# Agent Intelligence UI: Interface
> **Context Boundary**: `docs/cerb-ui/agent-intelligence/`
> **OS Layer**: Display (RAM Reader) & Keyboard (Event Writer)
> **Status**: 🟡 In Progress

## 1. Responsibility
This interface defines the data contract for rendering the "Agent Intelligence" wait-state UX. It visualizes the processing pipeline (Agent Thoughts, Tool Execution, Plan Generation) without exposing the user to raw JSON or blocking the conversation flow.

## 2. Dependencies
- **Consumer**: Any ViewModel or Pipeline outputting `UiState` (e.g., `IntentOrchestrator`, `UnifiedPipeline`).
- **Data Source**: Replaces legacy `AgentActivityBanner`, `ThinkingBox`, and components now managed natively by Compose `ResponseBubble`.

## 3. Data Contract (Input States)

The UI component requires the following State classes from the RAM layer (`com.smartsales.prism.domain.model.UiState`):

### 3.1 Transitory States
Visualize the latency of network calls.
- `UiState.Idle`: Resting state of the pipeline.
- `UiState.Loading`: Generic spinner.
- `UiState.Thinking`: Displays a pulsing brain and a textual hint.
- `UiState.Streaming`: Shows raw tokens arriving sequentially.
- `UiState.ExecutingTool`: Bypasses standard output while an internal tool runs.

### 3.2 Actionable States
Require user interaction to proceed.
- `UiState.AwaitingClarification`: The LLM missing required details (e.g. Schedule duration) or entities.
- `UiState.MarkdownStrategyState`: A rich text output (plan, summary) supporting CTAs.

### 3.3 Final Render States
Asynchronous or immediate background notifications.
- `UiState.Response`: Standard text response holding structural JSON.
- `UiState.AudioArtifacts`: Durable structured artifact turn for SIM audio-grounded chat history.
- `UiState.SchedulerTaskCreated`: Confirmation of a created task.
- `UiState.SchedulerMultiTaskCreated`: Confirmation of batch task creation.
- `UiState.Toast`: Fire-and-forget lightweight feedback.
- `UiState.BadgeDelegationHint`: Wave 6 hardware delegation overlay.
- `UiState.Error`: Pipeline malfunction or network outage.

## 4. Event Contract (Output Actions)

The UI component emits these events back to the RAM layer (`AgentViewModel`):

| Event | Trigger | Expected Action |
|-------|---------|-----------------|
| `onConfirmPlan` | User taps the "确认执行" CTA on a `MarkdownStrategyBubble`. | Triggers `intentOrchestrator.processInput("确认执行")` |
| `onAmendPlan` | User taps "修改计划" CTA on a `MarkdownStrategyBubble`. | Surfaces a Toast prompting the user to type their amendments. |
| `onSuggestionClick` | User taps a suggestion action card when conversation is idle. | Calls `viewModel.updateInput(suggestionTitle)` then `viewModel.send()`, sending the suggestion as a user message. |

## 5. You Should NOT
- **Do not** bind these UI components directly to the network or the SSD (Database). They strictly read the emitted `UiState`.
- **Do not** pass raw `qwen-max` response models into this UI. Parse them in the Domain layer into the `UiState` data classes defined above.
- **Do not** pass `ViewModel` instances (e.g., `AgentViewModel`) directly into these UI components. They must be "Dumb UI" components passing callbacks (e.g., `onConfirmPlan: () -> Unit`). This ensures they remain decoupled and reusable on any screen.
- **Do not** allow the UI components, nor the enclosing ViewModel, to handle intent routing (e.g., evaluating NOISE, skipping LLM, etc.). Intent routing must occur downstream in Layer 3 (e.g., `IntentOrchestrator`). The UI is strictly a dumb state-collector.

## 6. Code Reusability & Pipeline Integration (The "Plugin" Rule)
- **Zero-UI Plugin Addition**: Because the UI only listens to the generic `UiState` emitted by the pipeline, any new tool plugged into the backend automatically gets the full Agent Intelligence visual treatment (Streaming, Loading, Testing) for free.
