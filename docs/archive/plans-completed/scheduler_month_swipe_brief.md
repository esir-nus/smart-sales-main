## Design Brief: Month Carousel "Mouse Swipe"

### User Goal
"I can swipe the date carousel, but I can't swipe the months with my mouse."

### Translation
**Defect**: The Month Carousel currently uses native `overflow-x-auto`, which supports **Touch Swipe** (Mobile) and **Trackpad Pan**, but NOT **Mouse Drag**.
**Solution**: Implement `framer-motion` drag logic (`drag="x"`) for the Month container to match the Date Carousel's behavior. This ensures a consistent "App-Like" feel where grab-and-drag works everywhere.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **`SchedulerDrawer.tsx`**:
    - **Header/Month Section**: Replace the `div` overflow container with a `motion.div` drag container.
    - **Constraints**: Apply `dragConstraints` to prevent scrolling into void.
    - **Interaction**: Add `cursor-grab active:cursor-grabbing` for affordance.

### 🚫 Out of Scope
- **Date Carousel**: Don't break existing date swipe.
- **Drawer Logic**: Don't break drawer pull-down.

---

## Visual Specifications

### 1. The Month Container (Drag)
- **Container**: `overflow-hidden` (Hides scrollbars completely).
- **Inner Track**: `motion.div` with `drag="x"`.
- **Drag Constraints**: Must measure logic or use strict ref constraints to prevent over-dragging.
- **Rubber Band**: `dragElastic={0.2}` for premium feel.

### 2. Implementation Approach
- **Ref Method**: Use `useRef` to calculate constraints (`scrollWidth - clientWidth`).
- **Alternative**: Just use a wide left/right constraint if exact content width is known, or keep it simple with a liberal drag range. *Reference*: Frame Motion `dragConstraints`.

## Acceptance Criteria
1. [ ] User can click-and-drag the Month list left/right with a mouse.
2. [ ] No visible scrollbars.
3. [ ] "Rubber band" effect when dragging past ends.
4. [ ] Clicking a month still selects it (Drag shouldn't block Click).

## Handoff
Execute with `/ui-ux-pro-max`.
