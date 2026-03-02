# Analyst Mode

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: 🟡 In Progress
> **Source**: Extracted from prism-ui-ux-contract.md L854-960

---

## Core Concept

A three-layer system designed for complex B2B sales analysis.

1. **Thinking Trace (Cognition)**: Inline raw reasoning (ThinkingBox) for transparency.
2. **Markdown Strategy (Deliverables)**: A self-updating chat bubble that structures the output (not sticky).
3. **Task Board (Shortcuts)**: Sticky top tool shortcuts, replacing the header.

**Flow:** Conversation (Phase 1) → Threshold Met → Plan Generated (Phase 2) → Execution

---

## 1. TaskBoard (Sticky Top Layer)

Replaces the standard header when in Analyst scenarios. Vertical numbered list of workflow shortcuts.

```
┌────────────────────────────────────────────────────────────┐
│ 1. 📊 周度销售分析 — 汇总本周拜访数据，生成趋势报告          │
│ 2. 📈 竞品对比分析 — 对比主要竞品的价格、功能、市场策略       │
│ 3. 📝 会议纪要整理 — 从录音中提取要点、行动项、决策           │
│ 4. 💡 你也可以说出自己的需求...                             │ ← Custom prompt
└────────────────────────────────────────────────────────────┘
```

**Behavior:**
- **Sticky**: Pinned to top of chat view (below main header).
- **Vertical List**: Numbered items with full descriptions (not chips).
- **Context-Aware**: Options change based on conversation Phase.
- **Tap Action**: Re-routes entirely to OS Execution (`ToolRegistry.executeTool()`). **DOES NOT SEND AS USER MESSAGE**. Bypassing the LLM prevents the FSM from trapping the user in a new consulting loop.
- **Custom Option**: Always last item. Prompts user to voice/type a unique demand.

---

## 2. Markdown Strategy (Rich Chat Bubble)

A specialized Markdown text block rendered within a chat bubble. **Purely analytical** — displays structure and reasoning approach, no action buttons. Scrolls naturally with chat history.

**Structure:**
```
┌────────────────────────────────────────────────────────────┐
│ [AI - SmartBadge]                                          │
│ ────────────────────────────────────────────────────────   │
│ **周度客户分析报告**                                       │
│                                                            │
│ * 数据汇总：提取本周所有拜访记录。                         │
│ * 趋势分析：对比上周转化率指标。                           │
│ * 报告生成：输出分析结论。                                 │
│                                                            │
│ **当前洞察**: 拜访量上升20%，但转化率下降...                 │
│                                                            │
│ ✅ 计划已生成，请确认执行                                   │ ← Notify readiness
└────────────────────────────────────────────────────────────┘
```

**Behavior:**
- **Self-Updating**: The *same* bubble updates its content as the agent formulates its strategy.
- **Templates**: Agent auto-selects based on intent (Weekly Report, Competitor Analysis, etc.).
- **Display-Only**: No action buttons in table. Shows status and readiness.
- **Separation of Concerns**: Tool execution triggered via TaskBoard or explicit user request, NOT in-bubble buttons.

---

## 3. State Machine (Two-Phase Parsing)

### Phase 1: Conversational Planner (The "Consultant")
- **Model**: `qwen3-max-2026-01-23`
- **Mode**: `enable_thinking: true`, `stream: true`
- **Output**: Markdown text + Thinking Trace
- **Goal**: Clarify intent, gather context, decide *if* a formal plan is needed
- **UI**: Standard chat bubbles + ThinkingBox

### Phase 2: Plan Formalization (The "Architect")
- **Trigger**: `info_sufficient: true` from Phase 1 OR User explicit request ("生成计划")
- **Model**: `qwen3-max-2026-01-23` (Structured Prompt)
- **Output**: Pure Markdown Strategy text
- **UI**: Renders the **Markdown Strategy** bubble

### Phase 3: Execution (The "Worker")
- **Trigger**: User selects from TaskBoard OR explicitly requests action
- **Process**: Agent executes the selected workflow
- **UI**: New response bubble with final analysis results.

---

## Separation of Concerns

| Component | Role | Interaction |
|-----------|------|-------------|
| **Markdown Strategy** | Shows analysis structure and approach | **Read-only** |
| **TaskBoard** | Provides action options | **Write triggers** |

This preserves the "chat is truth" mental model while keeping concerns clean.

---

## Completion Behavior

- Items struck through (semantic `line-through` or ☑️ prefix)
- Summary footer shows task count + elapsed time
- Card fades to "Complete" badge after 5s (tappable to expand)

---

## Contract: Phase 1 → Phase 2 Transition

```json
{
  "info_sufficient": true,   // ← Triggers Phase 2
  "thought": "用户需求明确...",
  "response": "好的，我来帮您生成分析计划..."
}
```

When `info_sufficient: true`, controller MUST trigger Phase 2 call.
