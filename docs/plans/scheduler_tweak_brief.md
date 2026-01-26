## Design Brief: Scheduler "Double Handler" & Centering

### User Goal
"The gestures are confusing. I want explicit **Visual Handlers** to tell me where to pull for the Drawer vs. where to pull for the Calendar Expansion. Also, fix the date alignment."

### Translation
1.  **Visual Affordances**: Implement two distinct "Grabbers".
    - **Drawer Handler**: Top of the card. Indicates "This whole thing moves".
    - **Calendar Handler**: Bottom of the calendar container. Indicates "This specific part expands".
2.  **Alignment Fix**: Mathematical centering of date numbers.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **`SchedulerDrawer.tsx`**:
    - **Drawer Handle**: Existing top handle. Ensure it looks like a semantic "Drawer Grip".
    - **Calendar Handle**: **[NEW]** Add a visual indicator (small pill or chevron) *below* the Date Grid / Gradient Mask. This handle triggers the expansion.
    - **Typography**: Fix the `flex` vs `grid` issue in date cells.
    - **Gesture Logic**:
        - Dragging **Drawer Handle** -> Moves Drawer (Top-Down Enter/Exit).
        - Dragging **Calendar Handle** -> Expands/Collapses Month View.

### 🚫 Out of Scope
- **Swipe Logic**: Keep the week swipe.

---

## Visual Specifications

### 1. The "Double Handle" System
- **Handle A (Drawer)**:
    - Location: Top Center (inside the white card).
    - Style: Wide, thick pill (`w-12 h-1.5 bg-gray-300`).
    - Action: Dragging here moves the `y` of the whole drawer.
- **Handle B (Calendar)**:
    - Location: Bottom Center of the *collapsed* week view (or bottom of expanded view).
    - Style: Small, subtle pill (`w-8 h-1 bg-gray-200`) or "Chevron".
    - Action: Dragging here expands `height` (Week -> Month).
    - **Visual Placement**: Should sit *on top* of the gradient mask or just below the dates.

### 2. Date Alignment (The Fix)
- **Container**: Change from `flex` to `grid`.
- **Align**: `place-items-center`.
- **Text**: `text-center`.
- **Objective**: The vertical axis of the digit must match the vertical axis of the day header.

## Acceptance Criteria
1. [ ] User sees TWO distinct handles.
2. [ ] Pulling Top Handle = Moves Drawer.
3. [ ] Pulling Bottom Handle = Expands Calendar.
4. [ ] Date numbers are perfectly centered.

## Handoff
Execute with `/ui-ux-pro-max`.
