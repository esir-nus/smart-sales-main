## Design Brief: Scheduler Aesthetic & Gesture Polish

### User Goal
"The date numbers look misaligned (bad aesthetics). Also, since the drawer pulls down from the top, the 'Expansion' gesture (pulling down *more*) needs to feel natural and not conflict with closing."

### Translation
1.  **Grid Alignment**: Ensure Date Numbers are mathematically centered under Weekday Headers.
2.  **Gesture Hierarchy**: 
    - **Header Drag**: Should control Expansion.
    - **Handle Bar**: Primary drag target.
    - **Drawer Body**: Should NOT drag the drawer (allow scrolling content).
3.  **Visual Polish**: Lighter fonts, cleaner active states.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **`SchedulerDrawer.tsx`**:
    - **CSS Grid**: Verify `place-items-center` or Flex centering for the Date cells.
    - **Typography**: Verify `font-mono` vs `sans` alignment. Center text explicitly.
    - **Drag Logic**: Restrict `dragListener` to specific handles if possible, or refine constraints.
    - **Animation**: Smoother spring for expansion.

### 🚫 Out of Scope
- **Week Swipe**: Keep the left/right swipe logic (it works).
- **Content Cards**: Leave as is.

---

## Visual Specifications

### 1. Date Grid Alignment
- **Problem**: Current `flex-col` might be off-center.
- **Fix**: Use `grid place-items-center` for the cell container. Ensure the number itself is `text-center`.
- **Target**: The center of the '8' must align vertically with the center of '日'.

### 2. Gesture Optimization
- **Conflict**: Pulling down to expand vs Pulling up to close.
- **Rule**:
    - **Pull Down (> 50px)**: Expand to Month View.
    - **Pull Up (< -50px) whilst Expanded**: Collapse to Week View.
    - **Pull Up (< -100px) whilst Collapsed**: Close Drawer (Fold away).

## Acceptance Criteria
1. [ ] Date numbers are perfectly centered under weekday headers.
2. [ ] Expansion gesture feels distinct from Closing gesture.
3. [ ] Aesthetics look "tighter" (better spacing/alignment).

## Handoff
Execute with `/ui-ux-pro-max`.
