# History Drawer Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped (Pro Max V1)
> **Source**: Extracted from prism-ui-ux-contract.md L356-424
> **Updated**: 2026-03-26 (Wave C Contract Sync)

---

## Overview

Left-side drawer for session history navigation.
**Style**: Light-theme floating glass drawer with a device capsule header, collapsible session groups, and a floating footer dock inside the full-app shell.

---

## Layout Structure

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│   (Floating Capsule)                                       │
│   [ 🔋 85%  |  📶 SmartBadge  • 正常 ]                    │
│                                                            │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [📌] 置顶会话                                     v  │  │
│  │ ──────────────────────────────────────────────────── │  │
│  │  张总                                                │  │
│  │  Q4预算审查                                          │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [📅] 今天                                         >  │  │
│  └──────────────────────────────────────────────────────┘  │
│   (Collapsed Group)                                        │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [🗓️] 最近30天                                     v  │  │
│  │ ──────────────────────────────────────────────────── │  │
│  │  李财务                                              │  │
│  │  采购谈判中                                          │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│            (Scrollable List - Glass Cards)                 │
│                                                            │
│   (Floating Glass Dock)                                    │
│  [ [👤 Avatar]  Frank Chen     [PRO]              [⚙️] ]   │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## Components

### 1. Device State Header (Floating Capsule)
*Style: Glassmorphism, Floating, Pill Shape*

| Element | Content | Style |
|---------|---------|-------|
| **Container** | Floating Pill | `bg-white/80`, `backdrop-blur-md`, `border-white/40` |
| **Battery** | Icon + "85%" | Text Green-600 |
| **Divider** | Vertical Line | `bg-black/10` |
| **Device** | Icon + "SmartBadge" | Text Primary |
| **Status** | "• 正常" | Text Secondary, small |

**Action**: Tap opens **Connectivity Modal**.

### 2. Session List (Collapsible Cards)
*Style: Structured Cards, floating light cards inside the drawer slab*

**Groups (Collapsible):**
- **Header**: Icon + Title + Chevron.
- **Behavior**: tap header to toggle collapse/expand.
- **Icons**:
  - Pinned: `Pin` (Amber)
  - Today: `Calendar` (Blue)
  - Recent: `CalendarDays` (Indigo)
  - Archives: `Archive` (Gray)

**Item Format:**
- **Layout**: Horizontal row with title, summary, and visible overflow action affordance.
- **Title**: Bold, primary color, left aligned.
- **Summary**: Secondary color, single-line truncation.
- **Actions**: visible overflow menu is the primary entry for `Pin / Rename / Delete`; long-press may remain as a supplemental entry.
- **Decoration**: no right arrows. Data-sourced emojis must be stripped.

### 3. User Footer (Floating Dock)
*Style: Glassmorphism, Floating Dock at bottom*

| Element | Content | Style |
|---------|---------|-------|
| **Container** | Floating Dock | `bottom-6`, `mx-4`, `rounded-[20px]`, `shadow-glass` |
| **Avatar** | User Icon | Gradient Background |
| **Info** | Name + Plan | Dynamic from UserProfile, e.g. "老库", "高级会员" |
| **Settings** | `Settings` Icon | Right aligned |

**Interaction**:
- tap avatar or name area → opens **User Center**
- tap settings icon → opens the same **User Center** overlay seam

## Current Contract Freeze

- the full-app `HistoryDrawer` stays inside `AgentShell`
- hamburger tap is the only open contract
- close contract is `tap scrim` or `tap session`; the full-app drawer does not currently claim swipe-close
- device capsule tap opens the connectivity modal
- footer profile tap and footer settings tap both enter the shared `User Center` overlay seam
- visible overflow menu is the primary row-action entry for `Pin / Rename / Delete`
- the older header `+` action is not part of the shipped full-app drawer contract

---

## Interactions

| Trigger | Action |
|---------|--------|
| **Open** | Hamburger `[☰]` button ONLY (no edge swipe) |
| **Close** | Tap scrim OR tap session |
| **Tap Header** | Toggle Collapse/Expand for that group |
| **Tap Session** | Load session, close drawer |
| **Tap Device Capsule** | Opens Connectivity Modal |
| **Row Overflow Menu** | Exposes Pin / Rename / Delete |
| **Long Press** | Optional supplemental entry for the same row actions |
| **Tap Footer Profile** | Opens User Center |
| **Tap Footer Settings** | Opens User Center |
