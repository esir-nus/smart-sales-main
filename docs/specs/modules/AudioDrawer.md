# Audio Drawer Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md L707-849

---

## Overview

Bottom-up drawer for managing audio recordings. Pull up from bottom edge to open.

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
│  │ [★] Q4_年度预算会议_Final.wav                   14:20  ││ ← Row 1: Star + Name + Time
│  │ [☁] 财务部关于Q4预算的最终审核意见...                  ││ ← Row 2: Source + Preview
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [☆] 客户拜访_张总_20260124.wav                  2 days ││
│  │ [📱] ░░░░░░░░░ 右滑转写 >>> ░░░░░░░░░                  ││ ← Local source (Phone)
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
│  [★] Q4_年度预算会议.wav                           14:20  │
│  [☁] 财务部关于Q4预算的最终审核意见...                    │
└───────────────────────────────────────────────────────────┘
        │
        │ tap card body (ONLY if transcribed)
        ▼
┌───────────────────────────────────────────────────────────┐
│  EXPANDED                                                 │
├───────────────────────────────────────────────────────────┤
│  [★] Q4_年度预算会议.wav                           14:20  │
│  [☁] ───────────────────────────────────────────────────  │
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

## Gestures

| Gesture | Action | Notes |
|---------|--------|-------|
| **Swipe RIGHT →** | Instant Transcribe | Only if not transcribed. Shimmer: "转写 >>>" |
| **Swipe LEFT ←** | Reveal tray: `[Play]` `[Delete]` `[Rename]` | Quick actions |
| **Tap Body** | Expand Card | Only if transcribed |
| **Tap [问AI]** | Open Coach with transcript as context | Located in expanded card |

---

## Transcription States

| State | User Sees | Microcopy |
|-------|-----------|-----------|
| `not_transcribed` | Shimmer-Placeholder | "右滑转写 >>>" (Shimmering) |
| `transcribing` | Progress bar | "转写中... {%}" |
| `transcribed` | Preview text (120 chars) | — |
| `error` | Error badge | "转写失败，请重试" |

---

## Sync Flow (Auto on Open)

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

## Component Registry

| Component | States |
|-----------|--------|
| **Pill Handle** | `idle`, `dragging` |
| **Sync Indicator** | `idle`, `syncing`, `done`, `error` |
| **Audio Card** | `collapsed`, `expanded`, `playing`, `transcribing` |
| **Transcript Box** | `folded`, `unfolded`, `streaming` |
| **Upload Button** | `idle`, `picking`, `uploading` |
