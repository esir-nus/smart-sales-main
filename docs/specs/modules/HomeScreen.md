# Home Screen Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md L36-96

---

## Overview

Chat-Centric "Executive Desk" layout. The base layer revealed after dismissing Scheduler Drawer.

---

## Layout Structure

```
┌────────────────────────────────────────────────────────────┐
│ [☰] [📶]  Session: CEO Wang_ Procurement...   [🐞]  [➕]   │ ← Header
│  ↑   ↑          (Editable Title)               ↑     ↑     │
│  │   └─ Device State (BLE/WiFi)             Debug   New    │
│  └─ History Drawer Trigger                          Session│
│                                                            │
│ ========================================================== │
│ ||               TOP DRAWER: SCHEDULER                  || │
│ ||           (Auto-Drops on Launch)                     || │
│ ||            [ v Pull Handle ]                         || │
│ ========================================================== │
│                                                     [≣]    │ ← Menu (Tingwu)
│                                                            │
│               [ BASE LAYER: HOME HERO ]             [📦]   │ ← Artifacts
│                                                            │
│                [  Breathing Aura  ]                        │
│                     (✨)                                   │
│            "下午好, Frank"                                 │
│                                                            │
│             [ Toggle: Coach | Analyst ]                    │
│ ────────────────────────────────────────────────────────── │
│  [ 📎 ]  |  Type something to start... (Shimmering)     |  │
│ ────────────────────────────────────────────────────────── │
│                                                            │
│ ========================================================== │
│ ||            BOTTOM DRAWER: AUDIO MANAGER              || │
│ ========================================================== │
└────────────────────────────────────────────────────────────┘
```

---

## Header Interactions

| Element | Action | Result |
|---------|--------|--------|
| `[☰]` | Tap | Opens History Drawer (Left) |
| `[📶]` | Tap | Opens Connectivity Modal |
| `Title` | Tap | Rename current session |
| `[🐞]` | Tap | Toggle Debug HUD (Beta only) |
| `[➕]` | Tap | Start New Session (Clears context) |

---

## Right Toolbar

| Element | Action | Result |
|---------|--------|--------|
| `[≣]` | Tap | Menu (Tingwu Chapter Preview / Highlights) |
| `[📦]` | Tap | Opens Artifacts Drawer (PDF/CSV/generated) |

---

## Input Area

| Element | Behavior |
|---------|----------|
| **Mode Toggle** | Sets Intent only (`Coach` = Purple, `Analyst` = Blue). Does NOT navigate. |
| **Input Tap** | On Focus → Navigate to Chat Interface using selected mode |
| **Upload `[📎]`** | Reveals menu: 📄 文件 | 🖼️ 图片 | 🎙️ 音频 |
| **Placeholder** | Shimmering "输入消息..." |
| **Send `[➤]`** | Dismisses keyboard automatically |

---

## Auto-Drop Scheduler

On App Launch: Scheduler Drawer drops *over* this interface. Dismissing Scheduler reveals this "Clean Desk".

---

## Component Registry

> See [ui_element_registry.md](../ui_element_registry.md) for states and physics.
