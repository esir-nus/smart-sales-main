# History Drawer Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped (Pro Max V1)
> **Source**: Extracted from prism-ui-ux-contract.md L356-424
> **Updated**: 2026-02-02 (Variant 4 Hybrid)

---

## Overview

Left-side drawer for session history navigation. 
**Style**: "Hybrid Intelligence" — Floating Glass styling for header/footer (Aurora), Structured Collapsible Cards for content.

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
*Style: Structured Cards, "Contrast Background" for container, White Cards*

**Groups (Collapsible):**
- **Header**: Icon (Lucide) + Title + Chevron.
- **Behavior**: Click header to toggle collapse/expand.
- **Icons**:
    - Pinned: `Pin` (Amber)
    - Today: `Calendar` (Blue)
    - Recent: `CalendarDays` (Indigo)
    - Archives: `Archive` (Gray) (No Emojis!)

**Item Format:**
- **Title**: Bold, Primary Color (e.g., "张总").
- **Summary**: Regular, Secondary Color below title.
- **Hover**: Gradient highlight on left edge.
- **Decoration**: No right arrows `>` (Clean). Data-sourced emojis must be stripped.

### 3. User Footer (Floating Dock)
*Style: Glassmorphism, Floating Dock at bottom*

| Element | Content | Style |
|---------|---------|-------|
| **Container** | Floating Dock | `bottom-6`, `mx-4`, `rounded-[20px]`, `shadow-glass` |
| **Avatar** | User Icon | Gradient Background |
| **Info** | Name + Plan | "Frank Chen", "Premium Plan" |
| **Settings** | `Settings` Icon | Right aligned |

---

## Interactions

| Trigger | Action |
|---------|--------|
| **Open** | Hamburger `[☰]` button ONLY (no edge swipe) |
| **Close** | Tap scrim OR swipe drawer left |
| **Tap Header** | Toggle Collapse/Expand for that group |
| **Tap Session** | Load session, close drawer |
| **Long Press** | Context menu (Pin, Rename, Delete) |
