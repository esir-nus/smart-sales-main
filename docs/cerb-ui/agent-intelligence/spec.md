# Agent Intelligence UI: Specification
> **Context Boundary**: `docs/cerb-ui/agent-intelligence/`
> **UX Philosophy**: "Transparent Autonomy" (Android Version of Antigravity)
> **Status**: PARTIAL

## 1. Core Visual Mechanics

To convey "watching an expert work," the UI must feel alive, structured, and interruptible—replacing simplistic "Thinking..." spinners.

### 1.1 Active Task Horizon (Replaces `AgentActivityBanner`)
A sticky container at the top of the chat view when a task is ongoing.

- **Positioning**: Sticky top (below main header). Does NOT scroll with chat.
- **Visuals**: Dynamic Island style. Expands horizontally based on content.
- **Tokens Used**:
  - Background: `surface.plan`
  - Border: Subtle pulsing `accent.primary` (1px solid)
- **Trace Animation**: Micro-text of `activeToolStream` flashes through actual backend actions (`[Tool: get_sales_data]`).
- **User Control**: Must ALWAYS have a clearly visible "(X) Cancel" button on the right edge.

### 1.2 Thinking Canvas (Replaces `ThinkingBox`)
A fluid container visualizing raw LLM Chain-of-Thought processing.

- **Positioning**: Inline chat stream, placed *above* the final markdown response.
- **Visuals**:
  - Background: `surface.thinking` (Glassmorphism / Blur `Color(0x881A1A2E)` where supported)
  - Typography: `text.thinking` (Monospace, `#AAFFAA`)
- **Interaction Rule (Auto-Collapse)**: The canvas MUST auto-collapse into an "Agent Context" pill the moment `ThinkingCanvasState.isCollapsed = true`.
- **Expansion Rule**: Tap the pill to expand and read full thoughts. 
- **Zero-Chrome Policy**: ABSOLUTELY NO internal scrollbars inside the canvas. It either displays fully (expanded) or collapses (pill).

### 1.3 Live Artifact Builder (Replaces `PlanCard` & `ConflictResolver`)
A specialized payload container showing a document or plan being built.

- **Positioning**: Inline chat stream. 
- **Visuals**:
  - Background: `surface.response`
- **Animation**: "Document Skeleton" layout. A glowing cursor moves down placeholder lines as new blocks arrive.
- **Conflict State**: If `conflictNode` is present, the card pauses rendering. The border and conflicting section glow in `accent.warning` (`#FFAA00`).
- **Resolution CTA**: The CTA to solve the conflict must be placed inline, immediately adjacent to the highlighted conflict text.

## 2. Invariants & Guardrails
- **No Mystery Meat Loading**: No spinners without associated text explaining what is happening.
- **Immediate Cancellation**: The "Cancel" action in the Task Horizon must respond immediately (≤200ms visual feedback even if backend takes longer to halt).
- **Responsive Layout**: Designed for mobile viewports (360dp - 430dp width). Horizon must not break layout boundaries.
- **Universal Context Independence**: These components are foundational "Agent UI primitives." They must not assume they are living inside a Chat layout. They must be generic enough to render correctly anywhere in the app (e.g., on a Dashboard or floating over a calendar).
