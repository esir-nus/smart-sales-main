# Prism UI/UX Contract

> **Status:** Draft  
> **Last Updated:** 2026-01-24  
> **Architecture Companion:** [Prism-V1.md](./Prism-V1.md)  
> **Supersedes:** ux-contract.md, ux-experience.md, ui-ux-contract-v2.md

---

## Table of Contents

| Section | Description | Lines |
|---------|-------------|-------|
| [Global Gesture Model](#global-gesture-model) | Swipe directions for drawers | 10-18 |
| [§1 Interface Blueprints](#1-interface-blueprints) | Home Screen, Scheduler Drawer ASCII layouts | 20-247 |
| [Component Registry (Scheduler)](#component-registry-scheduler) | Task/Inspiration/Conflict card states, Smart Alarm | 248-404 |
| [Component Registry (Audio Drawer)](#component-registry-audio-drawer) | Audio file states, swipe gestures, transcription | 405-647 |
| [Component Registry (Chat Interface)](#component-registry-chat-interface) | Chat bubbles, streaming states | 648-663 |
| [§3 User Flows](#3-user-flows) | Disambiguation, Correction/Rethink, Notifications, Coach/Analyst modes | 664-1004 |
| [§4 Prism Spec Validation](#4-prism-spec-validation-matrix) | Cross-reference matrix to Prism-V1.md | 1005-1021 |

---

## Global Gesture Model

| Direction | Gesture | Opens | Notes |
|-----------|---------|-------|-------|
| **↓ Top → Down** | Pull from top edge | Scheduler Drawer | Calendar + Timeline |
| **↑ Bottom → Up** | Pull from bottom edge | Audio Drawer | Recordings |
| **→ Left → Right** | Swipe from left edge | History Drawer | Sessions |

---

## §1. Interface Blueprints

### 1.1 Home Screen (Session List)

**Layout Structure:**
```
┌────────────────────────────────────────────────────────────┐
│  [☰]  [📱]   SmartSales Assistant                  [✏️]    │ ← Header
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ 📅 今天 14:30                                          ││
│  │ 客户Q4预算会谈摘要                                      ││ ← Today
│  │ 财务部需要我们在下周一前提供新的报价方案...              ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ 📅 昨天                                                ││
│  │ 💡 灵感：竞品分析策略                                   ││ ← Yesterday
│  │ 需要重点关注A公司在华东市场的价格变动...                 ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ 📅 2026-01-20                                          ││
│  │ 张总初次拜访记录                                        ││ ← Older
│  │ 客户对A3打印机的兴趣度较高，但担心售后...                ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│                                                 [  🦠  ]   │ ← Knot FAB
└────────────────────────────────────────────────────────────┘
```

---

### Home Hero (Empty State)

Visible only when session list is empty (new install or all cleared).

```
┌────────────────────────────────────────────────────────────┐
│  [☰]  [📱]   SmartSales Assistant                  [✏️]    │
├────────────────────────────────────────────────────────────┤
│                                                            │
│                                                            │
│          ( Aurora / Breathing Animation )                  │
│                                                            │
│          欢迎回来，Frank                                    │
│          今天想聊点什么？                                   │
│                                                            │
│  [ 📅 安排日程 ]   [ 💡 记录灵感 ]   [ 📊 分析数据 ]      │
│                                                            │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

### Knot FAB ("Living Intelligence")

Top-layer Floating Action Button. Always visible.

| State | Visual | Interaction |
|-------|--------|-------------|
| **Idle** | Breathing organic shape | Tap → Suggest Tip |
| **Thinking** | Fast spin/pulse | (Non-interactive) |
| **Tip Shown** | Tooltip bubble visible | Tap → Dismiss |

```
Tip Shown:
                   ┌──────────────────────────┐
                   │ 试试 "帮我总结昨天会议"    │
                   └──────────┬───────────────┘
                              │
                          [  🦠  ]
```

---

### Component Registry (Home Screen)

| Component | User Sees | States | Internal Logic |
|-----------|-----------|--------|----------------|
| **Session Card** | Preview text + Date | `idle`, `swided-left`, `selected` | Opens Chat View |
| **New Session [✏️]** | Pencil icon | `idle`, `pressed` | Creates new empty session |
| **Knot FAB** | Organic orb | `breathing`, `spinning`, `tip` | Context-aware suggestions |
| **History Drawer [☰]**| Hamburger icon | `idle`, `open` | Opens left drawer |
| **Device Status [📱]**| Phone/Badge icon | `connected` (green), `disconnected` (gray) | Deep link to Device Manager |

---

### 1.3 Scheduler Drawer (Top-Down)

**Layout Structure:**
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
│         │ ☐ 与张总会议 (A3项目)               [⏰][⋮]    ││
│         └─────────────────────────────────────────────────┘│
│                          ↕ gap-4                           │
│  10:30  ┌─────────────────────────────────────────────────┐│
│         │ 💡 研究竞品报价策略                 [问AI][⋮]  ││
│         └─────────────────────────────────────────────────┘│
│                          ↕ gap-4                           │
│  12:00  ┌─────────────────── ⚠️ CONFLICT ─────────────────┐│
│         │ 李总电话 vs 午餐会议                 [展开]     ││
│         └─────────────────────────────────────────────────┘│
│                          ↕ gap-4                           │
│  14:00  ┌─────────────────────────────────────────────────┐│
│         │ ✓ 提交季度报告 (已完成)                        ││
│         └─────────────────────────────────────────────────┘│
│                                                            │
│  (No empty hour slots - cards stack at actual times)       │
└────────────────────────────────────────────────────────────┘
```

**Interaction Model (Natural Expansion):**
- **Folded (Default):** Shows only the **Active Week** (1 row).
- **Expanded:** Drag Handle down to reveal the **Full Month** (5 rows).
- **Object Permanence:** The Active Week row stays in place; other rows appear above/below it.
- **Data Source:** Single Unified 35-day Grid (no data swapping).

**Card Types:**

| Type | Icon | Actions | Description |
|------|------|---------|-------------|
| **Task** | ☐ | `[⏰]` Alarm, `[⋮]` Menu | Scheduled item |
| **Inspiration** | 💡 | `[问AI]` Ask AI, `[⋮]` Menu | Standalone note, not time-bound |
| **Conflict** | ⚠️ | `[展开]` Expand | Two overlapping items |
| **Done** | ✓ | — | Completed task (dimmed) |

---

### Card States (Task Card Example)

```
┌───────────────────────────────────────────────────────────┐
│  COLLAPSED (Default)                                      │
│  ───────────────────────────────────────────────────────  │
│  ☐ 与张总会议 (A3项目)                           [⏰][⋮] │
│  (Gestures: Swipe Left → Delete)                          │
└───────────────────────────────────────────────────────────┘
        │
        │ tap card body
        ▼
┌───────────────────────────────────────────────────────────┐
│  EXPANDED (Chat & Context Mode)                           │
│  ───────────────────────────────────────────────────────  │
│  ☐ 与张总会议 (A3项目)                           [⏰][⋮] │
│  ─────────────────────────────────────────────────────── │
│  [AI - System]                                            │
│  已为您安排 08:00。地点：北京办公室。发现3份相关历史文档。   │
│  需要摘要吗？                                              │
│                                                           │
│  [USER - Blue Bubble]                                     │
│  Yes, summary please.                                     │
│                                                           │
│  [ Input: "Ask for details or change..." ]        [Mic]   │
└───────────────────────────────────────────────────────────┘
```

**Interactions:**
1.  **Swipe Left**: **Delete** immediately (with undo toast).
2.  **Tap Body**: **Expand** to **Unified Chat Interface**.
3.  **Agent Input**: Natural language modification (e.g., "Reschedule to 3pm", "Show me related docs").
4.  **Inspiration**: Icon-Only Action (Sparkles).
4.  **Ask AI Icon**: Only on **Inspiration Cards**. Triggers synthesis/brainstorming mode.

---

### Conflict Card (Expanded Chat Resolution)

```
┌───────────────────────────────────────────────────────────┐
│  ⚠️ 冲突：李总电话 vs 午餐会议                            │
├───────────────────────────────────────────────────────────┤
│  [AI - System]                                            │
│  ⚠️ 发现日程冲突。'团队午餐' (12:00) 优先级较低。建议保留    │
│  '审查预算' (12:00)。是否自动调整午餐时间？                 │
│                                                           │
│  [USER - Blue Bubble]                                     │
│  保留预算                                                  │
│                                                           │
│  [AI - System]                                            │
│  好的，已更新。                                            │
│                                                           │
│  [ Input: "Reply..." ]                            [Mic]   │
└───────────────────────────────────────────────────────────┘
```

**Resolution Flow:**
1.  **Detection**: AI proactively explains the conflict.
2.  **Proposal**: AI suggests a resolution based on priority/context.
3.  **Action**: User replies naturally (e.g., "Yes", "Keep lunch").

---

### Inspiration Card (Multi-Select via Ask AI)

**Flow:** Tap `[问AI]` on any Inspiration → enters multi-select mode

```
STEP 1: User taps [问AI] on Card A
┌─────────────────────────────────────────────────────────────┐
│  💡 灵感：研究竞品报价策略                        [问AI][⋮] │ ← Tapped
└─────────────────────────────────────────────────────────────┘
        │
        ▼
STEP 2: Multi-select mode activates
┌─────────────────────────────────────────────────────────────┐
│  ● 灵感：研究竞品报价策略                              [⋮] │ ← Auto-selected
├─────────────────────────────────────────────────────────────┤
│  ○ 灵感：客户痛点整理                                  [⋮] │ ← Checkbox appears
├─────────────────────────────────────────────────────────────┤
│  ○ 灵感：Q2目标复盘                                    [⋮] │ ← Checkbox appears
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│              [ 问AI (1) ]                                   │ ← Bottom bar
└─────────────────────────────────────────────────────────────┘

STEP 3: User selects more (optional)
┌─────────────────────────────────────────────────────────────┐
│  ● 灵感：研究竞品报价策略                              [⋮] │
├─────────────────────────────────────────────────────────────┤
│  ● 灵感：客户痛点整理                                  [⋮] │ ← Now selected
├─────────────────────────────────────────────────────────────┤
│  ○ 灵感：Q2目标复盘                                    [⋮] │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│              [ 问AI (2) ]                                   │ ← Count updates
└─────────────────────────────────────────────────────────────┘

STEP 4: Tap [问AI (N)] → Opens Coach with combined context
```

**Exit Multi-Select:**
- **Left edge swipe** → Cancel, return to normal mode
- **Tap [问AI (N)]** → Proceed to Coach
- **Deselect all** → Auto-exit multi-select

---

## Component Registry (Scheduler)

| Component | User Sees | States | Internal Logic |
|-----------|-----------|--------|----------------|
| **Month Carousel** | Scrollable month pills | `idle`, `scrolling`, `selected` | Updates `selectedMonth` |
| **Day Carousel** | Scrollable day pills | `idle`, `scrolling`, `selected` | Updates `selectedDay` |
| **Grid Container** | 56px Row Grid | `folded` (H=56), `expanded` (H=300) | Animates Height/Y |
| **Handle Bar** | Gray-200 Pill | `idle`, `dragging` (Pull >50px) | Triggers Expansion/Collapse |
| **Task Dot** | Blue/White Dot | `idle` (Blue), `selected` (White) | Indicates task presence |
| **Time Label** | Mono Font Text | `visible` (Adaptive), `hidden` | Shows only near existing items |
| **Task Card** | Collapsible card | `collapsed`, `expanded`, `editing`, `done` | Links to `MemoryEntry` |
| **Inspiration Card** | Selectable card | `collapsed`, `selected`, `multi-selected` | `isArchived=false`, no `scheduledAt` |
| **Conflict Card** | Expandable resolver | `collapsed`, `expanded`, `resolving` | Triggers Prism §4.7 Rethink |
| **Alarm Button** | Clock icon | `idle`, `set`, `ringing` | Triggers system notification |

**Gesture Notes:**
- **Tap [问AI]** on Inspiration card → enters multi-select mode
- **Left edge swipe** → exit multi-select (standard back gesture)
- **[问AI (N)]** appears at bottom when in multi-select

---

### 1.4 Audio Drawer (Bottom-Up)

**Layout Structure:**
```
┌────────────────────────────────────────────────────────────┐
│  ══════════════════ PILL HANDLE ══════════════════════════ │
│                         ───                                │
├────────────────────────────────────────────────────────────┤
│  [ ↻ 同步中... ]                                    [编辑] │ ← Header (sync auto on open)
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ ☆ Q4_年度预算会议_Final.wav               14:20       ││ ← Audio Card
│  │ ──────────────────────────────────────────────────────  ││
│  │ 财务部关于Q4预算的最终审核意见，重点讨论了...          ││ ← Transcript preview
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ ○ 客户拜访_张总_20260124.wav              08:45       ││
│  │ ──────────────────────────────────────────────────────  ││
│  │ ░░░░░░░░░░ 右滑转写 >>> ░░░░░░░░░░                     ││ ← Shimmer placeholder
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ ○ meeting_notes.wav                        03:12       ││
│  │ ──────────────────────────────────────────────────────  ││
│  │ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 转写中... 45%          ││ ← Transcribing
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [+] 上传本地音频                                       ││ ← Upload button
│  └────────────────────────────────────────────────────────┘│
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

### Audio Card States

```
┌───────────────────────────────────────────────────────────┐
│  COLLAPSED (Default)                                      │
├───────────────────────────────────────────────────────────┤
│  ☆ Q4_年度预算会议.wav                    14:20          │
│  财务部关于Q4预算的最终审核意见...                        │
└───────────────────────────────────────────────────────────┘
        │
        │ tap card body
        ▼
┌───────────────────────────────────────────────────────────┐
│  EXPANDED                                                 │
├───────────────────────────────────────────────────────────┤
│  ☆ Q4_年度预算会议.wav                    14:20          │
│  ─────────────────────────────────────────────────────── │
│  📅 2026-01-23 15:30                 ┌──────────────┐    │
│  📁 来源：SmartBadge                 │    问AI      │    │
│                                      └──────────────┘    │
│  ─────────────────────────────────────────────────────── │
│  ┌─────────────────────────────────────────────────────┐ │
│  │ [转写内容 - 可折叠]                            [∧]  │ │
│  │ ─────────────────────────────────────────────────── │ │
│  │ 财务部关于Q4预算的最终审核意见，重点讨论了SaaS     │ │
│  │ 订阅模式的成本结构。李总提出需要在下周一之前...     │ │
│  │                                            [查看全部]│ │
│  └─────────────────────────────────────────────────────┘ │
│  ─────────────────────────────────────────────────────── │
│  [ ▶ 00:00 / 14:20   ●───────────────       🔊 ]        │ ← Audio Player Bar
└───────────────────────────────────────────────────────────┘
```

---

### Audio Card Gestures

| Gesture | Action | Notes |
|---------|--------|-------|
| **Swipe RIGHT →** | Instant Transcribe | Only if not transcribed. Shimmer: "转写 >>>" |
| **Swipe LEFT ←** | Reveal tray: `[Play]` `[Delete]` `[Rename]` | Quick actions |
| **Tap Body** | Expand Card | Shows detailed player + transcript |
| **Tap [问AI]** | Open Coach with transcript as context | Located in metadata section |
| **Hard Press** | Multi-select mode | For batch delete |

```
Swipe RIGHT (Not Transcribed) → Instant Transcribe:
┌─────────────────────────────────────────────────────────────┐
│  ░░░░░ (Shimmer) 转写 >>> ░░░░       Card Content →        │
└─────────────────────────────────────────────────────────────┘


Swipe LEFT → Management Tray:
┌───────────────────────────┬─────────┬─────────┬─────────────┐
│   Card Content            │  Play   │ Delete  │   Rename    │
│                           │ (Blue)  │ (Red)   │   (Gray)    │
└───────────────────────────┴─────────┴─────────┴─────────────┘
```

**Multi-Select Flow (from ux-experience.md):**
1. **Entry:** Tap `[Edit]` or **long press** any card
2. **Header:** `[Edit]` → `[Cancel]` `[Selected: N]` `[Delete]`
3. **Cards:** Stars (☆) → Checkboxes (○)/(●)
4. **Swipes:** LOCKED during multi-select
5. **Exit:** `[Cancel]` or successful delete

---

### Transcription States

| State | User Sees | Microcopy |
|-------|-----------|-----------|
| `not_transcribed` | Shimmer-Placeholder | "右滑转写 >>>" (Shimmering) |
| `transcribing` | Progress bar | "转写中... {%}" |
| `transcribed` | Preview text (120 chars) | — |
| `transcribing:folded` | Collapsed progress | "转写中..." |
| `transcribed:unfolded` | Full transcript | — |
| `error` | Error badge | "转写失败，请重试" |

**Simulated Streaming:**
- Linter validates complete response first
- Then animates text at 20ms/char for "typing" effect
- User can fold/unfold during animation

---

### Sync Flow (Auto on Open)

```
Drawer Opens
    │
    ▼
[↻ 同步中...] ──auto───▶ Check Badge for new recordings
    │
    ├── New files found → Add cards to list
    │
    └── Complete → [✓] (icon changes to checkmark, fades)
```

---

## Component Registry (Audio Drawer)

| Component | User Sees | States | Internal Logic |
|-----------|-----------|--------|----------------|
| **Pill Handle** | Drag handle | `idle`, `dragging` | Opens/closes drawer |
| **Sync Indicator** | Spinner/check | `idle`, `syncing`, `done`, `error` | ESP32 file list check |
| **Audio Card** | Collapsible card | `collapsed`, `expanded`, `playing`, `transcribing` | Links to `MemoryEntry` |
| **Transcript Box** | Foldable text | `folded`, `unfolded`, `streaming` | Simulated streaming |
| **Upload Button** | [+] row | `idle`, `picking`, `uploading` | Local file picker |

---

### 1.2 Chat Interface (Coach/Analyst Toggle)

**Coach Mode** — Fast, conversational. No visible "thinking" trace.

**Layout Structure:**
```
┌────────────────────────────────────────────────────────────┐
│  [☰]  [📱]   销售技巧交流                          [+]    │ ← Header
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [USER]                                                 ││
│  │ 帮我分析一下张总这个客户的购买意向                      ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [AI - Coach]                                           ││
│  │                                                        ││
│  │ 根据您与张总的交流记录，我发现他对A3打印机方案表现出   ││ ← Direct Response
│  │ 较高兴趣，但对售后服务有顾虑。建议下次拜访时重点强调   ││
│  │ 我们的本地化服务团队...                                ││
│  │                                                        ││
│  │ ┌─────────────────────────────────────────────────┐   ││
│  │ │ 💡 这看起来需要深度分析，建议切换到分析师模式     │   ││ ← Analyst suggestion
│  │ │          [ 切换到分析师 ]                        │   ││   (optional, LLM decides)
│  │ └─────────────────────────────────────────────────┘   ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│          [  💬 Coach▼ |  🔬 Analyst  ]                    │ ← Mode Toggle
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [+] │    输入消息...                            [➤]  ││ ← Input Bar
│  └────────────────────────────────────────────────────────┘│
└────────────────────────────────────────────────────────────┘
```

**Coach Mode Logic (Internal, Invisible to User):**
1. Parse user intent
2. Query Relevancy Library if needed (no UI feedback)
3. Generate response directly
4. Decide if deeper analysis is warranted → append suggestion block

> **Note:** Coach does NOT show Thinking Box. The Thinking Box is reserved for **Analyst Mode** (Planner trace).

---

### Analyst Mode (Initial Plan Generation)

**Flow:** User Request → Thinking (Analyst) → Plan Card Created

```
┌────────────────────────────────────────────────────────────┐
│  [☰]  [📱]   张总客户分析                          [+]    │
├─────────────────────────────────────────────────────────┬──┤
│                                                         │☰ │
│  ┌────────────────────────────────────────────────────┐ │📁│
│  │ [USER]                                             │ │🔴│
│  │ 分析张总的购买决策，附带了5张聊天截图和1段录音       │ └──┘
│  │ [🖼️ img1] [🖼️ img2] ... [🎤 audio.wav]           │      │
│  └────────────────────────────────────────────────────┘      │
│                                                              │
│  ┌────────────── THINKING BOX (Streaming) ─────────────┐     │
│  │ 🧠 正在构建分析计划...                         [∧] │     │
│  │ > 识别多模态输入 (5图, 1音频)...                    │     │
│  │ > 提取关键信息...                                   │     │
│  └─────────────────────────────────────────────────────┘     │
│                                                              │
│            (Thinking Complete ⬇️ Plan Appears)              │
│                                                              │
│  ┌──────────────── PLAN CARD (New) ─────────────────────┐    │
│  │ 📋 分析计划                                          │    │
│  │ ──────────────────────────────────────────────────── │    │
│  │ ☐ 1. 音频关键点提取                           [运行] │    │
│  │ ☐ 2. 图片OCR与语义分析                        [运行] │    │
│  │ ☐ 3. 综合决策模型构建                         [运行] │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│          [  💬 Coach  |  🔬 Analyst▼ ]                       │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ [+] │    继续追问...                            [➤]  │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

### Mixed Media Input (Context Builder)

**Capability:** Send Text + Images + Audio in one turn.

**UI State:**
```
┌────────────────────────────────────────────────────────────┐
│ [× 🖼️ chart.jpg]  [× 🖼️ email.png]  [× 🎤 voice.wav]    │ ← Attachment Staging
│ ────────────────────────────────────────────────────────── │
│ [+] │  帮我结合这些资料分析一下...                  [➤]  │
└────────────────────────────────────────────────────────────┘
```

**Logic:**
- **Max:** 5 Images, 1 Audio, 1000 char text.
- **Processing:** Sent as single multi-modal payload.
- **Context Builder:** Backend aligns timestamps and content. "User sends, System organizes."

---

### Plan Card Behavior (Persistent)

Once generated, the Plan Card **docks to the top** for follow-up turns.

```
┌────────────────────────────────────────────────────────────┐
│  ┌──────────────── PLAN CARD (Persistent) ───────────────┐ │
│  │ ...                                                   │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                            │
│  [USER]: OK, run step 1 and 2.                             │
│                                                            │
│  [AI]: Thinking... > Executing Tools...                    │
└────────────────────────────────────────────────────────────┘
```

---

### Plan Card Behavior

| State | User Action | Result |
|-------|-------------|--------|
| **Plan Generated** | AI outputs plan | Card appears, persists during session |
| **Tap [运行]** | Click tool button | Execute that step, show result |
| **Follow-up Chat** | User types new message | Card stays visible, chat continues below |
| **Switch to Coach** | Tap toggle | Card **stays visible** (can return to it) |
| **Complete All** | All items checked | Card shows completion summary (see below) |

#### Plan Completion State

After all steps executed, the Plan Card shows a **completion summary** with crossed-off items:

```
┌─────────────────────────────────────────────────────────┐
│  ✅ 计划已完成                                          │
│  ─────────────────────────────────────────────────────  │
│  ☑️ ~~音频关键点提取~~                                   │
│  ☑️ ~~图片OCR与语义分析~~                                │
│  ☑️ ~~综合决策模型构建~~                                 │
│  ─────────────────────────────────────────────────────  │
│  📊 3 项任务完成 · 用时 45s                             │
└─────────────────────────────────────────────────────────┘
```

**Completion Behavior:**
- Items struck through (semantic `line-through` or ☑️ prefix)
- Summary footer shows task count + elapsed time
- Card fades to "Complete" badge after 5s (tappable to expand)

---

### Thinking Box (Dynamic, API-Bound)

The Thinking Box is **not mode-specific**. It appears whenever the Qwen3 API response contains a non-empty `thinking` field.

| Trigger | Display |
|---------|--------|
| API response has `thinking` field | Show Thinking Box |
| `thinking` field absent or empty | Skip directly to response |

```
UNFOLDED (Auto, 3s):
┌──────────────────────────────────────────────────────────┐
│ 🧠 思考中...                                        [∧] │
│ ──────────────────────────────────────────────────────── │
│ > 检索Relevancy Library...                              │
│ > 找到3条相关记录                                        │
│ > 分析客户偏好...                                        │
│ > 生成回复...                                            │
└──────────────────────────────────────────────────────────┘

        ↓ (auto-fold after 3s)

FOLDED (Collapsed):
┌──────────────────────────────────────────────────────────┐
│ 🧠 思考完成                                         [∨] │
└──────────────────────────────────────────────────────────┘
```

**Fold/Unfold Behavior:**
| Rule | Behavior |
|------|----------|
| Initial state | Unfolded (streaming content visible) |
| Auto-fold | After 3 seconds of being unfolded |
| User tap `[∨]` | Unfold again |
| User tap `[∧]` | Fold immediately |

> **Note:** The Thinking Box is bound to the presence of the `thinking` field in the API response, not to the mode. Coach mode naturally skips thinking (model doesn't use it), Analyst mode naturally shows thinking (model uses it).

---

### Conflict Resolution (In Chat)

When AI detects a conflict (from Relevancy Library):

```
┌────────────────────────────────────────────────────────────┐
│ [AI - Coach]                                               │
│                                                            │
│ 我注意到您之前提到周三要拜访李总，但现在又安排了张总会议。  │
│                                                            │
│ ┌───────────────── ⚠️ 时间冲突 ─────────────────────────┐ │
│ │                                                        │ │
│ │  ○ 保留李总拜访，取消张总会议                          │ │
│ │  ○ 保留张总会议，延后李总拜访                          │ │
│ │  ○ 我来重新安排...                                     │ │
│ │                                                        │ │
│ │                                      [ 确认 ]          │ │
│ └────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

---

### Mode Toggle States

| State | Visual | Behavior |
|-------|--------|----------|
| **Coach Active** | `[💬 Coach▼ | 🔬 Analyst]` | Purple theme, no right dock |
| **Analyst Active** | `[💬 Coach | 🔬 Analyst▼]` | Blue theme, right dock visible |
| **Switching** | Toggle slides with animation | Toast: "已切换到分析师模式" |
| **Auto-Switch Suggestion** | Toggle pulses/glows | User must confirm |

---

## Component Registry (Chat Interface)

| Component | User Sees | States | Internal Logic |
|-----------|-----------|--------|----------------|
| **Mode Toggle** | Segmented control | `coach`, `analyst`, `switching` | Sets `currentMode` in Orchestrator |
| **Thinking Box** | Foldable trace | `hidden`, `folded`, `unfolded`, `complete` | DashScope native streaming |
| **Plan Card** | Persistent checklist | `visible`, `executing`, `complete` | Prism §4.6 Planner-Centric |
| **Tool Button** | Clickable item | `idle`, `running`, `done` | Executes plan step |
| **Conflict Picker** | Radio options | `visible`, `selected`, `confirming` | Prism §4.7 Rethink |
| **Analyst Suggestion** | Inline prompt | `visible`, `dismissed`, `accepted` | LLM Check A (§2.3) |
| **Response Bubble** | Chat bubble | `streaming`, `complete` | Simulated streaming (§3.1) |

---

---

## §3. User Flows

### Scheduler Flows

#### 3.1 ESP32 Voice → Transcribe → Schedule
**Scenario:** User records "Schedule a meeting with Zhang next Tuesday at 3pm." via badge.

```
USER (Audio)     SYSTEM (Background)                USER INTERFACE
────────────     ───────────────────                ──────────────
Records Voice -> Badge Uploads WAV -> Syncs ->      Audio Drawer [Cards: +1]
                 Tingwu Transcribes ->              Coach Chat (Hidden)
                                                    ↓
                 Orchestrator Detects Intent ->     Toast: "Detected Schedule Intent"
                 Extacts <Schedule> Tag ->          
                                                    ↓
                 Scheduler Adds Pending Task ->     Scheduler Drawer Opens (Auto)
                                                    Card Appears:
                                                    ┌───────────────────────────────┐
                                                    │ ☐ Meeting with Zhang  [Pending]│
                                                    │   Tue, 3:00 PM                │
                                                    │   [Confirm]  [Edit]           │
                                                    └───────────────────────────────┘
                                                    
User Taps [Confirm] -> Task Active (Alarm Set) ->   Card State: [⏰] Normal
```

#### 3.1.1 Voice Correction (MODIFY Intent)
**Scenario:** User records follow-up voice note to fix a mistake (mispronunciation or poor audio).

**Trigger Phrases:**

| Phrase | Example |
|--------|---------|
| "不是X，是Y" | "不是三点，是四点" |
| "改成" | "改成周三" |
| "错了" | "时间错了，应该是下午" |
| "重说" | "我重说一遍" |

**Matching Rule:** Most recent PENDING task (< 5 min old).

```
USER (Audio)     SYSTEM (Background)                USER INTERFACE
────────────     ───────────────────                ──────────────
Records Fix ->   Tingwu Transcribes ->              
                 Detects MODIFY Intent ->           
                 Finds Pending Task (< 5min) ->     
                 Override with New Values ->        
                 Re-query Relevancy Library ->      (Conflict Check)
                         ↓
           ┌─────────────┴──────────────┐
           ▼                            ▼
      No Conflict                  Conflict Found
           ↓                            ↓
  ┌─────────────────────────┐   ┌─────────────────────────────┐
  │ ☐ Meeting Zhang [Updated]│   │ ⚠️ Meeting Zhang  [Conflict]│
  │   Tue, 4:00 PM           │   │   Tue, 4:00 PM              │
  │   📝 3:00 → 4:00 PM      │   │   ──────────────────────────│
  │   [Confirm] [Edit]       │   │   ⚠️ 与李总电话冲突 (4:00 PM)│
  └─────────────────────────┘   │   [解决冲突] [仍然保留]      │
                                 └─────────────────────────────┘
```

**Conflict Resolution:** Same as §3.4 (Rethink flow applies).

**Fallback:** If no pending task found, create as new task with toast: "未找到待确认任务，已创建新日程".


#### 3.2 Inspiration → Ask AI → Coach
**Scenario:** User wants to develop an idea from a note.

```
USER ACTION                          SYSTEM ACTION
───────────                          ─────────────
Open Scheduler Drawer                Displays Inspiration Card
Tap [问AI] on "Competitor Analysis"  Opens Coach Chat
                                     Context: "Competitor Analysis note..."
                                     Prompt: "What do you want to do with this?"
                                     
User types: "Make a plan"            Coach responds with analysis/plan
```

#### 3.3 Inspiration Multi-Select (Combined Context)
**Scenario:** User combines two cards to form a larger context.

```
USER ACTION                          INTERFACE STATE
───────────                          ───────────────
Tap [问AI] on Card A                 Multi-select Mode Active
                                     Card A: (●)
                                     Bottom: [ 问AI (1) ]

Tap Card B Body                      Card B: (●)
                                     Bottom: [ 问AI (2) ]

Tap [问AI (2)]                       Opens Coach Chat
                                     Context: Note A + Note B
                                     Prompt: "I've loaded 2 notes. How can I help?"
```

#### 3.4 Scheduler Conflict Resolution
**Scenario:** New task conflicts with existing one.

**Multi-Channel Notification (Tiered):**

| Channel | Trigger | Behavior |
|---------|---------|----------|
| **Card Visual** (⚠️) | Always | Card shows `[Conflict]` badge, positioned at collision time |
| **Toast** | App foreground | "⚠️ 检测到日程冲突" (auto-dismiss 3s) |
| **Badge Vibration** | Conflict from badge voice input | 2× short vibration + audio: "检测到冲突" via BLE |
| **Pop-up Modal** | App backgrounded > 30s | Show on next app open (deferred) |

> **Design Principle:** Badge feedback only when user just used badge. Avoids random vibrations.

```
SYSTEM STATE                         INTERFACE STATE
────────────                         ───────────────
Conflict Detected                    1. Card Badge: ⚠️ [Conflict]
(Time collision)                     2. Toast: "检测到日程冲突"
                                     3. (If from badge) → BLE: vibrate + audio
                                     
USER ACTION                          
───────────                          
Tap [Expand] on Conflict Card        Card Expands:
                                     ┌───────────────────────────────────┐
                                     │ ⚠️ Conflict: Lunch vs Call        │
                                     │ ○ Keep Lunch (Cancel Call)        │
                                     │ ○ Keep Call (Postpone Lunch)      │
                                     │ ○ Manual Reschedule...            │
                                     └───────────────────────────────────┘

Select Option A -> Tap [Confirm]     Orchestrator Executes Rethink (§4.7):
                                     1. Updates Schedule DB
                                     2. Conflict Card Removed
                                     3. Toast: "Schedule Updated"
```

**Deferred Pop-up (On Return):**
```
┌────────────────────────────────────────────┐
│  ⚠️ 您有 1 个日程冲突待处理                 │
│  ──────────────────────────────────────    │
│  Lunch with Zhang ↔ Call with Li (12:00)   │
│                                            │
│           [查看]    [稍后处理]              │
└────────────────────────────────────────────┘
```

| Button | Action |
|--------|--------|
| **[查看]** | Scheduler Drawer opens → Scroll to conflict time → Card auto-expanded |
| **[稍后处理]** | Dismiss pop-up, conflict card remains in scheduler (user handles later) |



#### 3.5 Task Card Lifecycle
`Pending` (if from Voice) → `Active` → `Alarm Ringing` → `Done`

**Reminder Timing (No Picker UI):**

| User Input | Agent Action |
|------------|--------------|
| "提醒我提前5分钟" | Set single alarm at T-5min |
| "提前一小时提醒" | Set single alarm at T-60min |
| No preference | Apply Smart Cascade (agent-determined) |

**Smart Cascade (Agent-Determined, if no user preference):**

| Task Type | Cascade |
|-----------|---------|
| Meetings/Calls | T-30min (prep) → T-5min (join) |
| Deadlines | T-1h → T-15min |
| Personal/Other | T-10min |

> **Learning:** Agent observes User Habit (Prism §5.9). If user frequently snoozes first alarm, agent auto-adjusts offset.

**Card Visual States:**

| State | Card Shows |
|-------|-----------|
| User-specified | `[⏰ 5分钟前提醒]` |
| Agent-set | `[⏰ 智能提醒]` |

```
STATE: Active                        [⏰ 智能提醒] Visible
   ↓
(Time Reached) -> System Notification + Full Screen Pop-up
                  ┌────────────────────────┐
                  │ ⏰ Meeting with Zhang  │
                  │    3:00 PM             │
                  │ [Dismiss] [Snooze 10m] │
                  └────────────────────────┘
   ↓
Tap [Dismiss] -> Task marked Done    Card State: Done (✓) Dimmed
```

#### 3.6 Alarm Display (No Manual Picker)
Tap `[⏰]` on card → Shows current reminder setting (read-only). To change, user speaks: "改成提前1小时提醒".


---

### Coach Flows

#### 3.7 Simple Chat
**Scenario:** User chats about sales techniques.
Top-layer chat UI. No special cards.

#### 3.8 Memory Search Trigger
**Scenario:** User asks vague question "What about that price issue?"
**System:**
1. LLM identifies ambiguity.
2. Triggers `MemorySearch`.
3. UI shows Thinking Box:
   `Note: > Searching "price issue" in Relevancy Library...`

#### 3.9 Suggest Analyst Switch
**Scenario:** User asks complex data question.
**System:**
1. LLM (Check A) identifies "Deep Analysis Needed".
2. Appends suggestion block to response.
3. UI renders: `[ 💡 Deep analysis suggested... Switch to Analyst ]`
**User:** Taps button → Mode toggles to Analyst (Blue theme).

#### 3.10 Conflict Resolution (In Chat)
**Scenario:** Coach detects schedule conflict during conversation.
**System:** Renders `ConflictCard` inline in chat stream (same component as Scheduler).

---

### Analyst Flows

#### 3.11 Full Analysis Analysis (Plan Card)
**Scenario:** User: "Analyze Zhang's buying cycle."

```
USER ACTION                          SYSTEM RESPONSE
───────────                          ───────────────
Switch to Analyst Mode               Theme: Blue. Right Dock Visible.
Input: "Analyze Zhang..."            Thinking Box: Active (Streaming)
                                     
                                     Generate Plan Card:
                                     ┌──────────────────────────────┐
                                     │ 📋 Analysis Plan             │
                                     │ ☑ 1. Profile Summary  [Done] │
                                     │ ☐ 2. Cycle Calc       [Run]  │
                                     │ ☐ 3. Strategy Gen     [Run]  │
                                     └──────────────────────────────┘
                                     
Tap [Run] on Step 2                  Tool executes. Result appears below.
                                     Card updates: Step 2 [Done]
```

#### 3.12 Follow-up Chat
**Scenario:** User asks question while Plan is active.
**UI:** Plan Card **remains visible** at top of chat stream (re-injected context). New Q&A happens below it.

---

### Audio & Entity Flows

#### 3.13 Local Audio Upload
Tap `[+]` in Audio Drawer → System Picker → File added to list → Auto-transcribe starts.

#### 3.14 Badge Sync (Pull-to-Refresh)
Pull down Audio Drawer → Sync Indicator activates → New files populate list.

#### 3.15 Entity Recognition (Snakebar)
**Scenario:** Real-time extraction during analysis.
**UI:** Unobtrusive Snackbar at bottom:
`✨ Learned new entity: "Project A3" (Project)`
**Logic:** Updates `RelevancyLibrary` in background.

#### 3.16 Ambiguous Entity (Disambiguation)

**Scenario:** User mentions "张总", system knows multiple matches.

**Two UI States** (based on scoring from Prism-V1 §5.4):

---

**STATE A: Auto-Resolved (High Confidence)**

When `topScore > 0.85 AND secondScore < 0.3`, system auto-resolves with disclosure:

```
┌────────────────────────────────────────────────────────────┐
│ [AI - Coach]                                               │
│                                                            │
│  我理解您指的是张总（A3项目负责人）                         │ ← Inline disclosure
│  根据最近的A3项目讨论...                                   │
│                                                            │
│  [ 更改 ]                                                  │ ← Override button
│                                                            │
└────────────────────────────────────────────────────────────┘
```

| Element | Behavior |
|---------|----------|
| **Inline disclosure** | Bold entity name + context hint |
| **[更改] button** | Tap → expands to picker (STATE B) |

---

**STATE B: Picker (Multiple Candidates)**

When confidence is low or user taps `[更改]`:

```
┌────────────────────────────────────────────────────────────┐
│  您指的是哪位张总？                                        │
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ ○ 张总（A3项目负责人）                      ⭐ 常联系   ││ ← Sorted by score
│  ├────────────────────────────────────────────────────────┤│
│  │ ○ 张总（B2项目）                                       ││
│  ├────────────────────────────────────────────────────────┤│
│  │ ○ 张司机                                               ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│                                             [ 确认 ]       │
└────────────────────────────────────────────────────────────┘
```

| Element | Behavior |
|---------|----------|
| **⭐ 常联系 badge** | Shown if `confirmationCount >= 5` |
| **Sort order** | Highest score first (Prism §5.4 algorithm) |
| **[确认]** | Persists selection → triggers reinforcement (+1 count) |

---

**Reinforcement Feedback:**

| User Action | System Response |
|-------------|-----------------|
| Selects from picker | Toast: "已记住偏好" (fade after 1.5s) |
| Overrides auto-resolution | Toast: "已更正" + counter adjustment |

---

## §4. Prism Spec Validation Matrix

| Flow ID | Prism Section | Component/Mechanic | Status |
|---------|---------------|--------------------|--------|
| **3.1** | §4.5 | Plan-Once Execution (Voice Intent) | ✅ |
| **3.3** | §3.2 | Combined Context (Multi-select) | ✅ |
| **3.4** | §4.7 | Conflict Resolution (Rethink) | ✅ |
| **3.9** | §2.3 | LLM Check A (Analyst Suggestion) | ✅ |
| **3.11**| §4.6 | Planner-Centric (Plan Card Persistence) | ✅ |
| **3.15**| §5.0 | Incremental Learning (Relevancy Lib) | ✅ |
| **1.3** | §4.7 | Conflict Card UI | ✅ |
| **1.4** | §3.1 | Shared Buffer (Audio Transcript) | ✅ |

---

> **End of Contract V2 Specification**
