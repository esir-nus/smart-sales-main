## Design Brief: Expandable Scheduler Calendar

### User Goal
1.  **Restore Month Carousel**: Use a scrolling list of months (e.g., "1月", "2月"...) instead of a static header.
2.  **Expandable Calendar**: The Week View should be a "drawer" (or collapsible section) that can be pulled down to reveal the **Complete Month Calendar**.
3.  **Adaptive Indicators**: Visual cues (dots/colors) on dates to show schedule density.

### Translation
Refactor `SchedulerDrawer.tsx` to support two states for the calendar section: **Collapsed (Week)** and **Expanded (Month)**.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **Month Carousel**: Restore the horizontal scrollable month list (localized "1月", "2月"...).
- **Calendar Container**: A flexible container that animates height between `WeekHeight` (~80px) and `MonthHeight` (~300px).
- **Gesture**: Drag down on the week row (or a specific handle) to expand.
- **Indicators**: Add a small dot `Create a visual indicator` on days that have mock tasks (e.g., random or static data).

### 🚫 Out of Scope
- **Real Calendar Data**: Use mock "hasTask" boolean for indicators.

### 🛡️ Functional Invariants
- **Selection**: Selecting a day in Month view must reflect in Week view (and vice versa).
- **Localization**: All text Simplified Chinese.

---

## Visual Specifications

### 1. Month Carousel (Restored)
- **Layout**: Horizontal Scroll (`overflow-x-auto`).
- **Items**: `1月` ... `12月`.
- **Active**: Highlighted/Bold.

### 2. Expandable Grid
- **State A (Week)**: Single row of 7 days (Mon-Sun).
- **State B (Month)**: Full grid (Mon-Sun headers + 4-6 rows of dates).
- **Transition**: `layout` animation (Framer Motion) resizing the container.
- **Trigger**: Drag gesture or "Expand" icon button if drag is too complex for prototype. *Recommendation*: Use a small "Handle" below the dates to drag down.

### 3. Adaptive Indicators
- **Style**: Small dot (Prism Accent color) below the date number.
- **Logic**: Randomly assign `hasTask=true` to ~30% of days for demo.

## Acceptance Criteria
1. [ ] Month List is scrollable (not just "2026年 2月").
2. [ ] User can expand Week View to Month View.
3. [ ] Dates have visual indicators for tasks.
4. [ ] 100% Chinese.

## Handoff
Execute with `/ui-ux-pro-max`.
