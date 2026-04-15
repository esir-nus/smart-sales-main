## Design Brief: Scheduler "Double Pill" System & Alignment Fix

### User Goal
"The alignment is still off. The chevron is distracting—use a horizontal handler (pill) instead. And the Drawer Handler should be at the bottom (since it's a top drawer)."

### Translation
1.  **Visual Alignment**: The Date Numbers must be mathematically centered under the Weekday Headers. The specific "drifting" suggests we need to sync the Header Grid and Date Grid perfectly (maybe use one grid?).
2.  **Calendar Handle (Expansion)**: Replace the Chevron/Arrow with a clear **Horizontal Pill** (`w-12 h-1 rounded-full`). This implies "Drag", not "Click".
3.  **Drawer Handle (Global Move)**: Since the Drawer hangs from the top (`top-0`), the handle to control it/push it back up should be at the **Bottom Edge** of the drawer card (near the `rounded-b-40px`).

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **`SchedulerDrawer.tsx`**:
    - **Visual**: Move the Drawer Grip from Top to Bottom (Absolute position maybe?).
    - **Visual**: Replace Chevron with Pill.
    - **Grid**: Use `grid-cols-7` with `text-center` and `justify-items-center` on both Header and Body.
    - **Gestures**: Ensure Drag works on the handles.

### 🚫 Out of Scope
- **Week Swipe**: Keep it.

---

## Visual Specifications

### 1. Date Grid Alignment
- **Root Cause**: Likely mismatched padding or flex behavior between Header Row and Date Row.
- **Fix**: Ensure both the Header Row (`一 二...`) and Date Row use **Identical Grid Columns**.
- **Container**: `grid grid-cols-7 w-full`.
- **Items**: `place-items-center`.

### 2. Calendar Expansion Handle (The "Pull Down")
- **Location**: Below the Date Grid.
- **Visual**: Small Pill (`w-8 h-1 bg-gray-200 rounded-full`).
- **Interaction**: Drag Y to Expand.

### 3. Drawer Global Handle (The "Pull Up/Down")
- **Location**: Absolute Bottom Center of the Drawer Card.
- **Visual**: Medium Pill (`w-16 h-1.5 bg-gray-300 rounded-full`).
- **Interaction**: Drag Y to Move Drawer.

## Acceptance Criteria
1. [ ] Date numbers alignment is perfect (pixel check).
2. [ ] No "Arrow/Chevron" animations. Only Pills.
3. [ ] Drawer Handle is visible at the bottom of the card.

## Handoff
Execute with `/ui-ux-pro-max`.
