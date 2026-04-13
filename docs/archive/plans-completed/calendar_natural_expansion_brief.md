## Design Brief: Calendar "Natural Expansion"

### User Goal
"The calendar should extend from the date carousel. When folded, I see Feb 2-8. When unfolded, that same row (Feb 2-8) should stay in place, and I should see the rows above/below it revealed."

### Translation
**Current Defect**: The calendar uses TWO separate data sources (`days` for week, `monthGrid` for month). Toggling expansion *replaces* content instead of *revealing* more.

**Fix**: Use a **Single Data Source** (the full month grid). The "Week View" is just a **clipped viewport** of that grid, scrolled/offset to show the relevant week. Expansion reveals the full grid without replacing content.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **`SchedulerDrawer.tsx`**:
    - **Data Logic**: Remove dual-source (`days` vs `monthGrid`). Use `monthGrid` always.
    - **Animation**: Remove `AnimatePresence` cross-fade on content. Use only `height` animation.
    - **Clipping**: When folded, the container clips to show 1 row. The row shown is the one containing the **selected week**.
    - **Scroll Position**: When folded, if the current week is row 2 of 5, offset the container so that row is visible at the top (or use `overflow: hidden` and `translateY`).
    - **Month Sync**: The Header's month (`2月`) should reflect the currently visible context. When the grid shows February dates primarily, the header says February.

### ⚠️ Constrained
- **Week Swipe (Folded)**: When folded, swiping left/right should still change which week is shown. This now means scrolling/moving the viewport within the `monthGrid` rather than loading a new `days` array.

### 🚫 Out of Scope
- **Month Carousel**: Keep existing functionality (clicking a month changes the grid).
- **Date Selection**: Keep existing click-to-select logic.
- **Drawer Open/Close**: Don't touch.

---

## Visual Specifications

### 1. Unified Grid Logic

```typescript
// ALWAYS render the full month grid (35 dates)
const monthGrid = useMemo(() => {
    const firstOfMonth = new Date(year, month, 1);
    const startOffset = (firstOfMonth.getDay() + 6) % 7; // Mon=0
    const start = new Date(firstOfMonth);
    start.setDate(1 - startOffset);
    return Array.from({ length: 35 }, (_, i) => {
        const d = new Date(start);
        d.setDate(start.getDate() + i);
        return d;
    });
}, [year, month]);
```

### 2. Week Viewport (Folded State)

```typescript
// Determine which row (0-4) the selected/current week falls on
const currentWeekRow = useMemo(() => {
    const selectedDayOfMonth = selectedDate; // e.g., 5
    const firstOfMonth = new Date(year, month, 1);
    const startOffset = (firstOfMonth.getDay() + 6) % 7;
    const dayIndex = selectedDayOfMonth + startOffset - 1; // 0-indexed position in grid
    return Math.floor(dayIndex / 7); // Row number (0-4)
}, [selectedDate, year, month]);
```

### 3. Animation (No Shake)

```jsx
<motion.div
    animate={{ 
        height: isExpanded ? ROW_HEIGHT * 5 : ROW_HEIGHT * 1,
        // Optionally translateY to keep the current week row visible when collapsed
        y: isExpanded ? 0 : -(currentWeekRow * ROW_HEIGHT)
    }}
    transition={{ type: "spring", stiffness: 300, damping: 30 }}
    className="overflow-hidden"
>
    {/* Full 35-date grid always rendered */}
    <div className="grid grid-cols-7 ...">
        {monthGrid.map(d => <DateCell key={...} />)}
    </div>
</motion.div>
```

### 4. Month Header Sync

- When the user is viewing February's grid, display `2月`.
- When switching via Month Carousel, update the `month` state (which regenerates `monthGrid`).

---

## Acceptance Criteria

1. [ ] **No Content Jump**: When toggling fold/unfold, the visible week row stays in EXACTLY the same screen position. Other rows are revealed above/below.
2. [ ] **No Shake/Cross-fade**: No `AnimatePresence` slide animation on expansion. Purely `height` + `translateY`.
3. [ ] **Single Data Source**: `monthGrid` is always rendered; `days` array is removed.
4. [ ] **Week Swipe (Folded)**: Swiping changes `selectedDate` to shift which week is visible. No content shuffle.
5. [ ] **Month Carousel Sync**: Clicking "3月" regenerates `monthGrid` for March.

---

## Handoff
Execute with `/ui-ux-pro-max`.
