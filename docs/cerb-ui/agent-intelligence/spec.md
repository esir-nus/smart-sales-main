# Agent Intelligence UI: Specification
> **Context Boundary**: `docs/cerb-ui/agent-intelligence/`
> **UX Philosophy**: "Transparent Autonomy" (Android Version of Antigravity)
> **Status**: PARTIAL

## 1. Wave Plan (Delivery Matrix)

| Wave | Theme | Target | Status |
|------|-------|--------|--------|
| **Wave 1** | **Wait-State Basics** | Support `UiState.Thinking`, `UiState.Streaming`, Error states. | ✅ SHIPPED |
| **Wave 2** | **UI Literal Sync** | Render actual payloads: `MarkdownStrategyBubble`, `ClarifyingBubble`, `TaskCreatedBubble`. Stop swallowing pipeline states in the Presentation layer. | ✅ SHIPPED |
| **Wave 3** | **L2 Testing Validation** | Explicitly inject isolated `UiState` test fixtures via `L2DebugHud` to mathematically prove Compose layouts map to pipeline results. | 🔲 |

## 2. Core Visual Mechanics

To convey "watching an expert work," the UI must feel alive, structured, and interruptible—replacing simplistic "Thinking..." spinners.

### 2.1 Thinking Indicator (`UiState.Thinking`)
A compact indicator that the LLM is actively reasoning.
- **Visuals**: A soft pulsing brain emoji (🧠) or generic hint text: "正在思考...".
- **Tokens Used**: `surface.thinking` (Dark background `#1A1A2E`).

### 2.2 Streaming Bubble (`UiState.Streaming`)
Visualizes partial LLM output arriving token-by-token.
- **Visuals**: Standard response card rendering plaintext with a blinking text cursor `▌` appended to the end to simulate typing.

### 2.3 Clarifying Bubble (`UiState.AwaitingClarification`)
Renders the `AwaitingClarification` state when the downstream pipeline flags ambiguity (e.g., missing scheduling duration).
- **Visuals**: An inline card prefixed with a `❓ 需要更多信息` header, styled in `accent.info` (`#88CCFF`). 

### 2.4 Markdown Strategy Bubble (`UiState.MarkdownStrategyState`)
The primary vehicle for Analyst-mode multi-step plans and structured summaries.
- **Positioning**: Inline chat stream as a distinct, actionable card.
- **Visuals**: Renders standard Markdown syntax (headers, lists, bold) via `MarkdownText`.
- **Interaction**: Features inline CTA buttons (e.g., "确认执行", "修改计划") that map back to the `IntentOrchestrator`.

### 2.5 Notification / Action Bubbles (`UiState.SchedulerTaskCreated`)
Lightweight inline confirmations for asynchronous or background executions.
- **Visuals**: Standard response bubble prefixed with "已创建任务: [标题]" or "✅ 已创建 N 个任务".

## 3. Invariants & Guardrails
- **No Mystery Meat Loading**: No spinners without associated text explaining what is happening.
- **Responsive Layout**: Designed for mobile viewports (360dp - 430dp width).
- **Universal Context Independence**: These components are foundational "Agent UI primitives." They must not assume they are living inside a Chat layout. They must be generic enough to render correctly anywhere in the app (e.g., on a Dashboard or floating over a calendar).
