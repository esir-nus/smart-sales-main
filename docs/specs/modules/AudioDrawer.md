# Audio Drawer Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md L707-849

---

## Overview

Bottom-up drawer for managing audio recordings. Pull up from bottom edge to open.

The drawer now has two distinct interaction modes:

- **Browse Mode**: opened directly as an audio gallery and informational artifact surface
- **Select Mode**: reopened from chat attach/upload as a static audio picker for the current chat session

The same audio inventory is reused in both modes, but the interaction language must change clearly so users do not confuse gallery gestures with selection behavior.

---

## Mode Variants

### Browse Mode

Use Browse Mode when the user opens the drawer directly.

- behaves like the spec-aligned audio gallery
- pending items keep the swipe-right-to-transcribe interaction
- transcribed items can be tapped to expand into the artifact surface
- expanded cards may expose `问AI`
- swipe and expansion affordances remain visible

### Select Mode

Use Select Mode when the drawer is opened from the chat input attach/upload affordance.

- behaves like a focused picker, not a gallery
- cards are the action surface; no dedicated bottom CTA button is required
- swipe actions are suppressed
- card expansion is suppressed
- `问AI` is suppressed
- helper copy must explain tap-to-select behavior
- header copy should clearly frame selection, such as `选择要讨论的录音`
- helper copy should clearly frame the rule, such as `点击录音卡片切换当前聊天`

---

## Layout Structure

```
┌────────────────────────────────────────────────────────────┐
│  ══════════════════ PILL HANDLE ══════════════════════════ │
│                         ───                                │
├────────────────────────────────────────────────────────────┤
│  [ ↻ 同步中... ]                                           │ ← Header (sync auto on open)
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ Q4_年度预算会议_Final.wav              14:20   [★]    ││ ← Row 1: Name + Time + Star
│  │ 财务部关于Q4预算的最终审核意见...                      ││ ← Row 2: Preview / state copy
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ 客户拜访_张总_20260124.wav                2 days  [☆]  ││
│  │ ░░░░░░░░░ 右滑开始转写 >>> ░░░░░░░░░                    ││ ← Browse pending hint
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [+] 上传本地音频                                       ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## Interactions

| Action | Trigger | Notes |
|--------|---------|-------|
| **Open** | Pull up from bottom edge (>50px) OR button tap | — |
| **Dismiss** | Drag down (>100px) OR tap scrim OR tap pill handle | — |
| **Height** | `95vh` | Leaves 5% gap at top |
| **Spring Animation** | `damping: 25, stiffness: 200` | Consistent with Scheduler |

---

## Audio Card States

```
┌───────────────────────────────────────────────────────────┐
│  COLLAPSED (Default)                                      │
├───────────────────────────────────────────────────────────┤
│  Q4_年度预算会议.wav                           14:20  [★] │
│  财务部关于Q4预算的最终审核意见...                        │
└───────────────────────────────────────────────────────────┘
        │
        │ tap card body (ONLY if transcribed)
        ▼
┌───────────────────────────────────────────────────────────┐
│  EXPANDED                                                 │
├───────────────────────────────────────────────────────────┤
│  Q4_年度预算会议.wav                           14:20  [★] │
│  ───────────────────────────────────────────────────────  │
│      📅 2026-01-23 15:30                 ┌──────────────┐ │
│                                          │    问AI      │ │
│      ─────────────────────────────────── └──────────────┘ │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ [转写内容 - 可折叠]                            [∧]  │  │
│  │ ─────────────────────────────────────────────────── │  │
│  │ 财务部关于Q4预算的最终审核意见...                   │  │
│  │                                            [查看全部]│ │
│  └─────────────────────────────────────────────────────┘ │
│  [ ▶ 00:00 / 14:20   ●───────────────       🔊 ]        │ ← Audio Player Bar
└───────────────────────────────────────────────────────────┘
```

---

## Browse-Mode Gestures

| Gesture | Action | Notes |
|---------|--------|-------|
| **Swipe RIGHT →** | Instant Transcribe | Only if not transcribed. Shimmer: "转写 >>>" |
| **Swipe LEFT ←** | Reveal tray: `[Play]` `[Delete]` `[Rename]` | Quick actions |
| **Tap Body** | Expand Card | Only if transcribed |
| **Tap [问AI]** | Open Coach with transcript as context | Located in expanded card |

Browse Mode should stay aligned with the existing gallery UX contract. Gesture hints are valid only in this mode.

---

## Select-Mode Interaction Contract

Select Mode is intentionally simpler than Browse Mode.

| Interaction | Result | Notes |
|------------|--------|-------|
| **Tap Card** | Select audio for current chat | Whole card is the tap target |
| **Tap Current Audio Card** | No-op | Disabled style; marked as current discussion |
| **Swipe RIGHT →** | Disabled | No transcribe swipe affordance in this mode |
| **Swipe LEFT ←** | Disabled | No quick-action tray in this mode |
| **Tap Body to Expand** | Disabled | Card stays collapsed; this is not the artifact-reading surface |
| **Tap [问AI]** | Hidden | `问AI` belongs to Browse Mode expanded cards only |

Select Mode cards must feel self-explanatory through state and copy rather than through button chrome.

---

## Transcription States

| State | User Sees | Microcopy |
|-------|-----------|-----------|
| `not_transcribed` | Shimmer-Placeholder | "右滑转写 >>>" (Shimmering) |
| `transcribing` | Progress bar | "转写中... {%}" |
| `transcribed` | Preview text (120 chars) | — |
| `error` | Error badge | "转写失败，请重试" |

These transcription-state microcopies apply to Browse Mode. Select Mode uses different, picker-oriented language.

---

## Select-Mode Card States

| State | User Sees | Microcopy / Behavior |
|-------|-----------|----------------------|
| `current` | Disabled card with visible preview | Inline current marker only, such as `当前讨论中 · ...`; no extra status pill |
| `transcribed` | Tappable card with truncated transcript preview | Transcript-first preview should be truncated to 1-2 lines with ellipsis and be self-explanatory without `已转写` chrome |
| `pending` | Tappable card with compact continuation copy | Use row-body copy such as `选择后在当前聊天中继续处理`; no separate `待处理` pill |
| `transcribing` | Tappable card with progress or in-flight label | Use row-body copy plus progress bar, such as `转写中，选择后将在当前聊天继续处理`; no separate `转写中` pill |
| `error` | Retry-capable or explicitly blocked card | `转写失败`; only promise chat-side retry if that route is truly supported |

For already-transcribed cards in Select Mode, the preview should help the user recognize the audio content quickly without opening the full artifact surface. Compact cards should not differentiate badge/phone origin through extra icon or source-label chrome in this mode.

---

## Sync Flow (Auto on Open, Browse Mode Only)

```
Browse-Mode Drawer Opens
    │
    ▼
[↻ 同步中...] ──auto───▶ Check Badge for new recordings
    │
    ├── New files found → Add cards to list
    │
    └── Complete → [✓] (icon changes to checkmark, fades)
```

---

## Component Registry

| Component | States |
|-----------|--------|
| **Audio Drawer** | `browse`, `select` |
| **Pill Handle** | `idle`, `dragging` |
| **Sync Indicator** | `idle`, `syncing`, `done`, `error` |
| **Audio Card** | `collapsed`, `expanded`, `playing`, `transcribing`, `current_discussion`, `selectable` |
| **Transcript Box** | `folded`, `unfolded`, `streaming` |
| **Upload Button** | `idle`, `picking`, `uploading` |
