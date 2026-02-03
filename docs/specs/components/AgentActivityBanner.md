# AgentActivityBanner Component

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md
> **Code**: `AgentActivityBanner.kt`

---

## Overview

The AgentActivityBanner uses a **two-tier structure** to showcase agent activity comprehensively. This is a core product differentiator.

---

## Structure

| Layer | Role | Example |
|-------|------|---------|
| **Phase** | High-level task (always visible) | "📝 规划分析步骤", "⚙️ 执行工具: PDF生成" |
| **Action** | Specific operation (optional) | "🧠 思考中...", "📚 检索记忆..." |
| **Trace** | Streaming content (optional) | Native CoT, transcript, memory hits |

---

## Visual States

```
HEADER (Grey, Shimmering — always visible):
┌──────────────────────────────────────────────────────────────┐
│ 思考中...  正在规划分析步骤                             [∨] │
└──────────────────────────────────────────────────────────────┘

↓ Trace auto-unfolds when streaming starts

EXPANDED (≤3 lines, grey, no shimmer):
┌──────────────────────────────────────────────────────────────┐
│ 思考中...  正在规划分析步骤                             [∧] │
│ ───────────────────────────────────────────────────────────── │
│ ┃ 检索Relevancy Library...                                  │
│ ┃ 找到3条相关记录                                            │
│ ┃ 分析客户偏好...                                            │
└──────────────────────────────────────────────────────────────┘

↓ Auto-collapse when trace exceeds 3 lines

COLLAPSED (Header ONLY — user taps [∨] to expand):
┌──────────────────────────────────────────────────────────────┐
│ 思考中...  正在规划分析步骤                             [∨] │
└──────────────────────────────────────────────────────────────┘
```

---

## State Machine

| State | Visual | Trigger |
|-------|--------|---------|
| `hidden` | Not visible | No streaming activity |
| `folded` | Header only | Default, or trace > 3 lines |
| `unfolded` | Header + Trace | Trace starts, or user taps `[∨]` |
| `complete` | Fades out | Streaming ends |

---

## API Binding

| Trigger | Phase | Action | Trace Source |
|---------|-------|--------|--------------|
| Analyst plan generation | `PLANNING` | `THINKING` | Qwen3-Max CoT |
| Tool execution (chart/report) | `EXECUTING` | `THINKING` | Tool's CoT |
| Image parsing | `EXECUTING` | `PARSING` | Qwen-VL output |
| Audio transcription | `EXECUTING` | `TRANSCRIBING` | Tingwu stream |
| Memory retrieval | `RESPONDING` | `RETRIEVING` | Entity matches |
| Context assembly | `RESPONDING` | `ASSEMBLING` | Source list |
| Error | `ERROR` | `null` | Error message |

---

## Fold/Unfold Behavior

| Rule | Behavior |
|------|----------|
| Initial state | Collapsed (Header only) |
| Trace starts | Auto-Unfold (show trace) |
| Trace > 3 lines | Auto-Collapse (Header only) |
| User tap `[∨]` | Expand all |
| User tap `[∧]` | Collapse (Header only) |

---

## Design Principle

> The AgentActivityBanner is bound to the presence of streaming activity, **not to the Mode**.
> Coach mode naturally skips thinking (model doesn't use it), Analyst mode naturally shows thinking (model uses it).
