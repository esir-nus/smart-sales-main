## Design Brief: Swipe-First Scheduler

### User Goal
"When I identify a UI element as a 'Carousel' (Months or Dates), I expect to **SWIPE** it to navigate, not just click buttons."

### Translation
Implement **Gesture-Based Navigation** for the Scheduler.
1.  **Date Carousel (Week View)**: Must allow swiping left/right to change weeks.
2.  **Month Carousel**: Must maintain horizontal scrollability (already native, but ensure it feels fluid).

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **`SchedulerDrawer.tsx`**:
    - **Week View**: Wrap days in a `motion.div` with `drag="x"`.
    - **Logic**: Detect drag delta to trigger `changeWeek(-1)` or `changeWeek(1)`.
    - **Visuals**: Minimize or remove the "← →" buttons (User implies swipe is the *primary* logic). Let's hide them to declutter, or make them subtle.
    - **Animation**: Add `AnimatePresence` to slide the weeks in/out (Slide transition).

### 🚫 Out of Scope
- **Month Grid (Expanded)**: This is a vertical expansion. Swipe left/right on the grid *could* change months, but for now focus on the "Carousel" (Week View).
- **Date Selection Logic**: Remains the same.

---

## Interaction Specifications

### 1. Date Carousel (Week View)
- **Gesture**: Pan Left/Right on the days row.
- **Threshold**: Swipe > 50px triggers change.
- **Feedback**: Row should follow finger slightly (elastic) then snap.
- **Transition**:
    - Swipe Left -> New Week enters from Right.
    - Swipe Right -> New Week enters from Left.

### 2. Month Carousel
- **Existing**: `overflow-x-auto` (Native Scroll).
- **Refinement**: Ensure `no-scrollbar` class is active (it is). Ensure touch targets are fat (pills).

## Acceptance Criteria
1. [ ] User can change the week by swiping the date row.
2. [ ] Week transition is animated (Slide).
3. [ ] Arrows are removed (or made secondary/hidden) to rely on swipe.

## Handoff
Execute with `/ui-ux-pro-max` (Framer Motion expert).
