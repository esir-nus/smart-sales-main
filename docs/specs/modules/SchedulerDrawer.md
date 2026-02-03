# Scheduler Drawer Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md L153-327

---

## Overview

Top-down drawer containing calendar and timeline. Pull down from top edge to open.

---

## Layout Structure

```
┌────────────────────────────────────────────────────────────┐
│  ══════════ MONTH CAROUSEL ══════════                      │
│  ‹ 2025    [ 1月 ] [ 2月 ] [3月▼] [ 4月 ] ...    2027 ›    │
│  (Click updates grid instantly)                            │
├────────────────────────────────────────────────────────────┤
│  ══════════ CALENDAR GRID (Foldable) ════════════════════  │
│                                                            │
│  [ 一 ] [ 二 ] [ 三 ] [ 四 ] [ 五 ] [ 六 ] [ 日 ]  ← Headers  │
│                                                            │
│  [26 ] [27 ] [28 ] [ 1 ] [ 2 ] [ 3 ] [ 4 ]  ← Active Week  │
│                                                            │
│  ─────── [ Handle ] (Pull > 50px to Expand) ───────        │
│                                                            │
│  (Expanded: Reveals full month grid above/below)           │
├────────────────────────────────────────────────────────────┤
│  ══════════ TIMELINE (Adaptive Stack) ════════════════════ │
│                                                            │
│  08:00  ┌─────────────────────────────────────────────────┐│
│         │ ☐ 与张总会议 (A3项目)               [⏰]       ││
│         └─────────────────────────────────────────────────┘│
│                          ↕ gap-4                           │
│  10:30  ┌─────────────────────────────────────────────────┐│
│         │ 💡 研究竞品报价策略                 [问AI]     ││
│         └─────────────────────────────────────────────────┘│
│                          ↕ gap-4                           │
│  12:00  ┌─────────────────── ⚠️ CONFLICT ─────────────────┐│
│         │ 李总电话 vs 午餐会议                 [展开]     ││
│         └─────────────────────────────────────────────────┘│
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---


## Card Structure & Content

### Task Card (Detailed)

| Field | Icon | Source | Description |
|-------|------|--------|-------------|
| **Title** | (None) | LLM | Concise summary |
| **Time** | (Left) | LLM | Start time (e.g. 08:00). End time optional. |
| **Key Person** | 👥 | LLM | Main contact/stakeholder |
| **Highlights** | ✨ | LLM | Critical details (身份证, dress code etc) |
| **Alarm Cascade** | ⏰ | Infer | e.g. "Smart" → -1h, -15m, -5m |
| **Location** | 📍 | LLM | Address or venue |
| **Notes** | 📝 | LLM | Other details |

### Expanded View ASCII

```
  08:00  ┌────────────────────────────────────────────────────────┐
         │ ☐ 赶飞机 (上海出差)                       [⏰] [Actions] │
         │ ────────────────────────────────────────────────────── │
         │  📅 03:00 - ... (Open Ended)                          │
         │  📍 虹桥机场 T2                                        │
         │  👥 这里的关键人物 (Key Person)                        │
         │  ✨ 必须带好身份证; 提前值机 (Highlights)              │
         │  ⏰ -1h, -15m, -5m (Smart Cascade)                     │
         │                                                        │
         │  [ Reschedule...                                  🎤 ] │
         └────────────────────────────────────────────────────────┘
```

---

## Card Types

| Type | Icon | Actions | Description |
|------|------|---------|-------------|
| **Task** | ☐ | `[⏰]` Alarm | Scheduled item |
| **Inspiration** | 💡 | `[问AI]` Ask AI | Standalone note, not time-bound |
| **Conflict** | ⚠️ | `[展开]` Expand | Two overlapping items |
| **Done** | ✓ | — | Completed task (dimmed) |

---

## Card Interactions

| Action | Trigger | Result |
|--------|---------|--------|
| **Swipe Left** | Any card | Delete immediately |
| **Tap Body** | Task/Conflict | Expand to Chat Interface |
| **Tap [问AI]** | Inspiration card | Enter multi-select mode |

---

## Multi-Select Flow (Inspiration Cards)

```
STEP 1: User taps [问AI] on Card A
┌─────────────────────────────────────────────────────────────┐
│  💡 灵感：研究竞品报价策略                        [问AI]    │ ← Tapped
└─────────────────────────────────────────────────────────────┘
        │
        ▼
STEP 2: Multi-select mode activates
┌─────────────────────────────────────────────────────────────┐
│  ● 灵感：研究竞品报价策略                              [✓] │ ← Auto-selected
├─────────────────────────────────────────────────────────────┤
│  ○ 灵感：客户痛点整理                                  [ ] │
├─────────────────────────────────────────────────────────────┤
│  ○ 灵感：Q2目标复盘                                    [ ] │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│              [ 问AI (1) ]                                   │ ← Bottom bar
└─────────────────────────────────────────────────────────────┘

STEP 3: Tap [问AI (N)] → Opens Coach with combined context
```

**Exit Multi-Select:**
- **Left edge swipe** → Cancel, return to normal mode
- **Tap [问AI (N)]** → Proceed to Coach
- **Deselect all** → Auto-exit

---

## Conflict Resolution

```
┌───────────────────────────────────────────────────────────┐
│  ⚠️ 冲突：李总电话 vs 午餐会议                            │
├───────────────────────────────────────────────────────────┤
│  [AI] 发现日程冲突。建议保留 '审查预算'。是否自动调整？     │
│                                                           │
│  [USER] 保留预算                                          │
│                                                           │
│  [AI] 好的，已更新。                                      │
└───────────────────────────────────────────────────────────┘
```

**Flow:** Detection → AI Proposal → User Reply

---

## Date Cell Glow Indicator

When a new task is created for a specific date, the corresponding calendar date cell shows a **breathing glow** animation to draw the user's attention.

| State | Visual | Trigger | Result |
|-------|--------|---------|--------|
| **New Task** | Breathing blue glow around date circle | Task created for this date | Glow animates until user taps |
| **Acknowledged** | Normal state | User taps glowing date | Glow stops |

**Notes:**
- Color: `AccentBlue`
- Multi-day tasks: First day only
- Glow is purely visual — no coupling with other logic
