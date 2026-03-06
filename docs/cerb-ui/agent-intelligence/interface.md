# Agent Intelligence UI: Interface
> **Context Boundary**: `docs/cerb-ui/agent-intelligence/`
> **OS Layer**: Display (RAM Reader) & Keyboard (Event Writer)
> **Status**: 🟡 In Progress

## 1. Responsibility
This interface defines the data contract for rendering the "Agent Intelligence" wait-state UX. It visualizes the processing pipeline (Agent Thoughts, Tool Execution, Plan Generation) without exposing the user to raw JSON or blocking the conversation flow.

## 2. Dependencies
- **Consumer**: Any ViewModel or Pipeline outputting `UiState` (e.g., `UnifiedPipeline`, `DashscopeExecutor`).
- **Data Source**: Replaces legacy `AgentActivityBanner`, `ThinkingBox`, and `PlanCard`.

## 3. Data Contract (Input States)

The UI component requires the following State classes from the RAM layer (ViewModel):

### 3.1 `ActiveTaskHorizonState`
Represents a long-running, multi-step agent process.

```kotlin
data class ActiveTaskHorizonState(
    val isVisible: Boolean,
    val overarchingGoal: String,          // e.g., "Analyzing Q3 Competitor Data"
    val currentActivity: String,          // e.g., "Reading transcripts..."
    val activeToolStream: String?,        // e.g., "[Tool: query_relevancy_lib]"
    val canCancel: Boolean                // Whether the user can manually abort
)
```

### 3.2 `ThinkingCanvasState`
Represents the live streaming Chain-of-Thought (CoT) from an LLM.

```kotlin
data class ThinkingCanvasState(
    val isVisible: Boolean,
    val streamingContent: String,         // The raw thought text
    val isCollapsed: Boolean              // True when final answer is ready
)
```

### 3.3 `LiveArtifactBuilderState`
Represents the structured JSON/Markdown payload being written as a final deliverable by the agent.

```kotlin
data class LiveArtifactBuilderState(
    val isVisible: Boolean,
    val title: String,
    val markdownContent: String,
    val conflictNode: ConflictData?       // Null unless a blocker (e.g., schedule overlap) occurs
)

data class ConflictData(
    val title: String,
    val description: String,
    val resolvable: Boolean
)
```

## 4. Event Contract (Output Actions)

The UI component emits these events back to the RAM layer:

| Event | Trigger | Expected Action |
|-------|---------|-----------------|
| `OnCancelHorizonTap` | User taps the (X) button on the Task Horizon. | Cancel the async coroutine pipeline and revert state to Idle. |
| `OnCanvasToggleTap` | User taps the collapsed "Agent Context" pill. | Expand to show the historical LLM thoughts for that specific bubble. |
| `OnResolveConflict` | User taps a resolution CTA inside the Artifact Builder. | Dispatch resolution data to the Agent pipeline to resume execution. |

## 5. You Should NOT
- **Do not** bind these UI components directly to the network or the SSD (Database). They strictly read the emitted `UiState`.
- **Do not** pass raw `qwen-max` response models into this UI. Parse them in the Domain layer into the `*State` data classes defined above.
- **Do not** pass `ViewModel` instances (e.g., `PrismViewModel`) directly into these UI components. They must be "Dumb UI" components that only accept the defined `*State` data classes and emit simple event callbacks (e.g., `onCancelClicked: () -> Unit`). This ensures they remain decoupled and reusable on any screen.

## 6. Code Reusability & Pipeline Integration (The "Plugin" Rule)
To prevent the anti-pattern of writing new UI code for every new plugin, we enforce strict **Decoupled State Emission**:
- **Plugin Ignorance**: Individual plugins (e.g., TranslationTool, WeatherPlugin) MUST NOT directly construct or emit `ActiveTaskHorizonState` or `ThinkingCanvasState`. Plugins should only emit raw domain events or simply execute their logic.
- **Orchestrator Translation**: A centralized entity (e.g., `UnifiedPipeline` or `ToolRegistry`) automatically wraps plugin execution and translates the backend progress into these standard UI data contracts.
- **Zero-UI Plugin Addition**: Adding a new "Tool 03" requires **ZERO** new UI code. Because the UI only listens to the generic `*State` emitted by the pipeline, any new tool plugged into the backend automatically gets the full Agent Intelligence visual treatment (Horizon tracking, Canvas logging) for free.
