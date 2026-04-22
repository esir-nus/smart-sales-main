# Agent Intelligence UI: Specification
> **Context Boundary**: `docs/cerb-ui/agent-intelligence/`
> **UX Philosophy**: "Transparent Autonomy" (Android Version of Antigravity)
> **Status**: PARTIAL

## 1. Wave Plan (Delivery Matrix)

| Wave | Theme | Target | Status |
|------|-------|--------|--------|
| **Wave 1** | **Wait-State Basics** | Support `UiState.Thinking`, `UiState.Streaming`, Error states. | SHIPPED |
| **Wave 2** | **UI Literal Sync** | Render actual payloads: `MarkdownStrategyBubble`, `ClarifyingBubble`, `TaskCreatedBubble`. Stop swallowing pipeline states in the Presentation layer. | SHIPPED |
| **Wave 3** | **L2 Testing Validation** | Explicitly inject isolated `UiState` test fixtures via `L2DebugHud` to mathematically prove Compose layouts map to pipeline results. | SHIPPED |
| **Wave 4** | **Transparent Mind Integration** | Dynamically update `UiState.Thinking(hint)` locally based on `PipelineResult.Progress` emissions. | SHIPPED |
| **Wave 5** | **Hand-Off Animation (Wave 6 T5)** | Visual bridging between voice ingestion and LLM execution, eliminating "dead air". | OPEN |
| **Wave 6** | **Conversational Polish** | Inline thinking banner, stream-into-bubble, wider response bubbles, multi-line input, suggestion action cards. | SHIPPED |

## 2. Core Visual Mechanics

To convey "watching an expert work," the UI must feel alive, structured, and interruptible—replacing simplistic "Thinking..." spinners.

### 2.1 Thinking Banner (`UiState.Thinking`)
A compact inline banner that the LLM is actively reasoning. Follows Claude Code-style thinking transparency — shows what the agent is doing without dominating the viewport.
- **Composable**: `SimThinkingBanner(hint: String?)`
- **Visuals**: Left-aligned bubble-shaped banner with pulsing sparkle icon (✦) + trace text from `hint` parameter. Uses same `SimConversationSurfacePalette` as assistant bubbles but with thinner padding (14dp h, 10dp v). Width: `fillMaxWidth(0.78f)`.
- **Animation**: Sparkle alpha pulses between 0.3 and 1.0 over 800ms, `LinearEasing`, `RepeatMode.Reverse`.
- **Fallback**: When `hint` is null, displays "SIM 正在分析...".
- **Transition**: Cross-fades out when `UiState` transitions to `Streaming` (handled by Compose state diff).
- **Shape**: Same assistant bubble corners — `20/20/20/4`.
- **Replaces**: `SimStatusSheet` which was previously used for `UiState.Thinking` (too heavy — full bordered card with icon, title, divider, body).

### 2.2 Streaming Bubble (`UiState.Streaming`)
Streams AI response content directly into a message bubble as tokens arrive — feels like the AI is typing a message, not producing a system event.
- **Composable**: `SimStreamingBubble(content: String)`
- **Visuals**: Identical shape and palette to `SimAssistantBubble`. Content rendered via `MarkdownText` with a blinking cursor `▌` appended. Width: `fillMaxWidth(0.88f)`.
- **Cursor**: Alternates visibility every 500ms via `LaunchedEffect`.
- **Empty content fallback**: When `content.isBlank()`, delegates to `SimThinkingBanner(hint = "SIM 正在生成回复...")` to avoid a blank bubble.
- **Replaces**: `SimStatusSheet` which was previously used for `UiState.Streaming` (rendered as a status card, not a conversational bubble).

### 2.3 Clarifying Bubble (`UiState.AwaitingClarification`)
Renders the `AwaitingClarification` state when the downstream pipeline flags ambiguity (e.g., missing scheduling duration).
- **Visuals**: An inline card prefixed with a textual "需要更多信息" header, styled in `accent.info` (`#88CCFF`). 

### 2.4 Markdown Strategy Bubble (`UiState.MarkdownStrategyState`)
The primary vehicle for Analyst-mode multi-step plans and structured summaries.
- **Positioning**: Inline chat stream as a distinct, actionable card.
- **Visuals**: Renders standard Markdown syntax (headers, lists, bold) via `MarkdownText`.
- **Interaction**: Features inline CTA buttons (e.g., "确认执行", "修改计划") that map back to the `IntentOrchestrator`.

### 2.5 Notification / Action Bubbles (`UiState.SchedulerTaskCreated`)
Lightweight inline confirmations for asynchronous or background executions.
- **Visuals**: Standard response bubble prefixed with "已创建任务: [标题]" or "已创建 N 个任务".

### 2.6 Voice Handling UI States (`UiState.AudioProcessing`)
Visualizes the transition from voice ingestion (Badge or Mic) to LLM execution, eliminating the "dead air" gap.
- **Visuals**: A dynamic waveform or pulsing "Transcribing..." indicator that replaces the generic `UiState.Loading` spinner.
- **Contract**: The UI layer (`AgentIntelligenceScreen`) must remain oblivious to the specific hardware (Badge vs. Phone). It strictly reacts to a unified `UiState.AudioProcessing` emitted by the `IntentOrchestrator`, preserving the OS Model boundaries.

### 2.7 Assistant Response Bubble (`UiState.Response`)
Standard completed AI response rendering with responsive width.
- **Composable**: `SimAssistantBubble(content, headline?, accent?, borderColor?)`
- **Width**: `fillMaxWidth(0.88f)` — uses 88% of available width for long-form responses. Previously capped at `300.dp`.
- **Shape**: `20/20/20/4` rounded corners (top-start, top-end, bottom-end, bottom-start).
- **Content**: Rendered via `MarkdownText` with full markdown support (headers, lists, bold, inline code).

### 2.8 Suggestion Action Cards
Full-width tappable action cards that appear after each completed AI response to guide the user toward next steps.
- **Composable**: `SimSuggestionActionCard(icon, title, description, onClick)`
- **Placement**: Rendered as the bottom-most item in `SimConversationTimeline` (visually above the input bar due to `reverseLayout = true`).
- **Visibility conditions**: Shown only when all of:
  - `uiState is UiState.Idle` (response complete)
  - `agentActivity == null` (no pipeline activity)
  - `history.isNotEmpty()` (not the empty/greeting state)
  - Last message is `ChatMessage.Ai`
- **Visuals**: Full-width card with conversation surface palette, 14dp rounded corners. Emoji icon (18sp) + title (15sp medium) on first row, description (13sp muted) below.
- **Interaction**: Tapping sends the suggestion title as a user message via `onSuggestionClick` callback.
- **Default suggestions**: Static set of 3 cards — "复盘一段客户沟通", "演练开场白或异议应对", "分析一段录音". Future: backend-driven suggestions based on `SessionKind` and conversation state.

### 2.9 Status Sheet (Pipeline Activity)
Retained for tool execution and multi-phase agent activity — heavier card treatment is appropriate for these operational states.
- **Composable**: `SimStatusSheet(title, action, body, icon, accent)`
- **Used for**: `UiState.ExecutingTool`, `AgentActivity` (multi-phase pipeline), `UiState.BadgeDelegationHint`.
- **Not used for**: `UiState.Thinking` or `UiState.Streaming` (which now use lighter inline treatments per 2.1 and 2.2).

## 3. Invariants & Guardrails
- **No Mystery Meat Loading**: No spinners without associated text explaining what is happening.
- **Responsive Layout**: Designed for mobile viewports (360dp - 430dp width).
- **Universal Context Independence**: These components are foundational "Agent UI primitives." They must not assume they are living inside a Chat layout. They must be generic enough to render correctly anywhere in the app (e.g., on a Dashboard or floating over a calendar).
- **Suggestion cards are ephemeral UI**: They are not persisted in chat history. They exist only as a UI affordance when the conversation is idle.
- **Long-press copy on chat bubbles**: Both user bubbles and completed AI response bubbles (`CompleteBubble`) respond to long-press with a single-item "复制" dropdown that writes the bubble's full plain text to the system clipboard and confirms via a "已复制" toast. Short-tap behavior (e.g., the inline "展开完整解析与原文..." affordance) is preserved — long-press is additive. Streaming, thinking, and error bubbles are intentionally excluded. HarmonyOS should mirror this behavior natively per cross-platform sync contract.
