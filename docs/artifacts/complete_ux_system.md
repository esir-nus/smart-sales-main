# Complete Scheduler UX System (Top Drawer)

**Role**: Global Top Drawer (Agent Context)
**Trigger**: Swipe Down (Global)
**Theme**: Aurora Premium (Glassmorphism)

---

## 1. The "Auto-Drop" Launch (System Behavior)

**Trigger**: App Launch
**Behavior**: The Drawer *automatically* slides down (animation: 400ms spring) to cover the screen.
**Metaphor**: "The Agent briefs you first."

```ascii
[ APP LAUNCH ]
      │
      ▼
┌─────────────────────────────────────────┐
│ [ DRAWER DROPS DOWN automatically ]     │
│  Hello Frank. Here is your day.         │
│  [ Month/Date Header ]                  │
│  [ Timeline Content  ]                  │
│                                         │
│          ==== [ HANDLE ] ====           │ ◀── User drags UP to dismiss
└─────────────────────────────────────────┘     and answer Chat.
```

---

## 2. The "Calendar Dashboard" (Expanded View)

This is the default view effectively serving as the Home Screen overlay.

```ascii
┌─────────────────────────────────────────┐
│ [ HEADER: Month Carousel ]              │
│   <  DEC    [ JAN ]    FEB    MAR  >    │ ◀── Horizontal Carousel
│                                         │
├─────────────────────────────────────────┤
│ [ SUB-HEADER: Date Strip ]              │
│   < MON    TUE    [ WED ]    THU  >     │ ◀── Horizontal Carousel
│     19     20      [ 21 ]     22        │
│     •              [ ●  ]               │ ◀── "Dot" indicates tasks
├─────────────────────────────────────────┤
│          ==== [ HANDLE ] ====           │ ◀── Drag DOWN for Full Month Grid
├─────────────────────────────────────────┤
│ [ CONTENT: Vertical Timeline ]          │
│                                         │
│   09:00 ──○  [ COMPLETE ]               │
│           │  ┌───────────────────────┐  │
│           │  │ [✓] Daily Standup     │  │ ◀── Dimmed + Strikethrough
│           │  └───────────────────────┘  │
│           │                             │
│   10:00 ──● [NOW] ───────────────────   │
│           │                             │
│ ║ 12:00 ──║  [ PRIORITY + ALARM ]       │
│ ║         ║  ┌───────────────────────┐  │
│ ║         ║  │ [P0] Client Call   ⏰ │  │ ◀── Glowing Border + Bell Icon
│ ║         ║  │ Prep required         │  │     (Unmissable)
│ ║         ║  └───────────────────────┘  │
│           │                             │
│   14:00 ──○                             │
│           │  ┌───────────────────────┐  │
│           │  │ Focus Time            │  │
│           │  └───────────────────────┘  │
│                                         │
├─────────────────────────────────────────┤
│          ==== [ HANDLE ] ====           │ ◀── Drag UP to Dismiss
└─────────────────────────────────────────┘

```

---

## 3. The "Full Month View" (Interaction)

**Action**: User drags the Date Strip handle **DOWN**.
**Result**: The strip expands into a full calendar grid.

```ascii
┌─────────────────────────────────────────┐
│ [ JAN 2026 ]                            │
│                                         │
│  M   T   W   T   F   S   S              │
│      1   2   3   4   5   6              │
│  7   8   9   10  11  12  13             │
│  14  15  16  17  18  19  20             │
│  21  22  23  24  25  26  27             │
│  ●   •   •   •   •                      │ ◀── Heatmap dots
│  28  29  30  31                         │
│                                         │
│          ==== [ HANDLE ] ====           │ ◀── Drag UP to collapse
└─────────────────────────────────────────┘     back to Date Strip
```

## 4. Task State Visuals (UX Specialist)

| State | Visual Indicator | Micro-Interaction |
|-------|------------------|-------------------|
| **Complete** | Dimmed (50% opacity), Strikethrough text, Checkmark icon. | Tapping toggles completion (with haptic click). |
| **Priority (P0)** | **Outer Glow** (Aurora Red), Bold Title, "P0" Badge. | Always sorted to top of collision groups. |
| **Alarm Set** | Small ⏰ Bell Icon near time. | Pulse animation 5m before trigger. |
| **Conflict** | Yellow/Gold Border + "⚠️" Warning Badge. | Tap opens conflict resolution dialog. |

---

## 5. Visual Specs (UI Director)

### 5.1 Carousels
-   **Month**: `Type.H3` (Unselected: Gray-400, Selected: White ExtraBold). Center focus.
-   **Date Strip**: High-density touch targets.
    -   *Selected*: Capsule background (`AuroraCyan` 20%).
    -   *Today*: Small glowing indicator below text.
    -   **Behavior**: Snaps to center. Infinite scroll (if feasible) or lazy load.

### 5.2 Timeline
-   **Connector Line**: `2dp` width, dashed for past, solid for future.
-   **Task Cards**: Glassmorphism (`White` 10% alpha).
    -   *Detail View*: Tapping a card expands it in place (Accordion style) or opens a Bottom Sheet details pane.

### 5.3 Motion
-   **Launch**: `DropIn` (Spring, Damping: 0.7).
-   **Month Expansion**: `ExpandVertically`. The Date Strip *morphs* into the Grid row.

---

## 6. Implementation Notes

-   `ScheduleDrawer` needs three main Composable sections: `MonthCarousel`, `DateStrip` (expandable), and `DayTimeline`.
-   **State Management**: `ExpandedState` (Collapsed/Strip/Grid).
-   **Data Source**: `Room` database query must support `getTasksForMonth(month)` to populate dots efficiently.
