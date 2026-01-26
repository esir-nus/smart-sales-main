## Design Brief: Scheduler Weekly Logic

### User Goal
1.  **Fixed Week Structure**: Week view should be Mon-Sun (Mon, Tue, Wed, Thu, Fri, Sat, Sun).
2.  **Batch Navigation**: Swiping/Clicking changes the *entire week* (7 days), not just scrolling one day.
3.  **Localization**: All text must be Simplified Chinese.

### Translation
Refactor `SchedulerDrawer.tsx` to use a "Page-based" or "Batch-based" week view instead of a scrolling list.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **Days Header**: Change formatting to `['一', '二', '三', '四', '五', '六', '日']` (Mon-Sun).
- **Date Generation**: Calculate dates based on a `currentWeekStart` state.
- **Navigation**: Arrows or Swipe to jump `currentWeekStart ± 7 days`.
- **Month Label**: Display "X月" (e.g., "3月") instead of "Mar".

### 🚫 Out of Scope
- **Complex Calendar Logic**: No need for `date-fns` heavy lifting yet, simple arithmetic is fine for prototype.
- **Real Data**: Keep using mock tasks.

### 🛡️ Functional Invariants
- **Selected Day**: Must persist or reset logic when week changes (e.g., select first day of new week, or keep same index).

---

## Visual Specifications

### 1. Header (Month/Year)
- **Format**: "2026年 3月"
- **Alignment**: Center or Left.

### 2. Week Row
- **Labels**: Fixed row `一 二 三 四 五 六 日`.
- **Dates**: Row of numbers below labels.
- **Selection**: Circle highlight (Aurora Accent) on selected date.

### 3. Interaction
- **Swipe**: (Implied) Horizontal drag on the week row changes week.
- **Tap**: Selects a date within the current week.

## Acceptance Criteria
1. [ ] Week headers are Chinese `一` to `日`.
2. [ ] Dates are organized in a standard 7-day row.
3. [ ] "Next/Prev" controls (or simulation) jump 7 days.
4. [ ] All text is in Chinese.

## Handoff
Execute with `/ui-ux-pro-max`.
