## Design Brief: History Drawer "Variant Parade"

### User Goal
"Check the screenshots... now we have far less elements than before... layout all designs next to each other... split to 4 phases."

### Translation
1.  **Visual Pivot**: Adopt **V17 Streamlined**. This is a reductionist design. "Less is more".
    *   *Before*: Rows with buttons, metadata, avatars.
    *   *Now*: Just Title, Time, Summary, Star. Actions are hidden behind gestures.
2.  **Museum Layout**: Display multiple full phone frames side-by-side (`Flex Row`) to A/B test complete ecosystem vibes, not just list items.
3.  **Process**: 4-Phase rollout.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope (V17 DEFINITION)
- **Minimalism**: REMOVE all visible action buttons (Play, Delete).
- **Gestures**: ADD visual cues for "Swipe" (e.g., "← 滑动转写").
- **Typography**:
    - Title: Bold, Primary Color.
    - Timestamp: Small, Muted, Top-Right.
    - Summary: 2 lines max, Muted.
- **Icons**:
    - Yellow Star (⭐) for Important/Pinned.
    - Outline Star (☆) for Standard.
- **States**:
    - Active: Blue Progress Bar (Visual only).
    - Idle: Clean.

### ✅ Layout Strategy (The Museum)
- **Container**: `body { display: flex; overflow-x: auto; gap: 50px; }`
- **Frames**: Multiple `.device-frame` instances.
    - Frame A: V17 (Light)
    - Frame B: V17 (Dark)
    - Frame C: Legacy V12 (Comparison)

### ⚠️ Constrained (Modify With Care)
- **CSS Classes**: Do **NOT** alter the existing CSS definitions of the variants (V8, V9, etc.) unless they are broken. The goal is to *display* what we have, not redesign it yet.
- **Master Drawer (V1/V6)**: The "shipping candidate" (Section 6.6 in Style Guide) must be presented as the **Anchor/Reference** at the top.

### 🚫 Out of Scope (Do NOT Touch)
- **Kotlin Code**: Absolutely NO changes to Android code.
- **Interaction Logic**: We are focusing on *visual visuals*. Complex drawer drag physics are secondary to the *look* of the list items and container.

### 🛡️ Functional Invariants
- **Gestural Affordance**: Even though it's clean, the user must know interactions are possible (use the "Swipe Hint" visual).
- **Density**: 8 Items per frame.

---

## Acceptance Criteria
1.  [ ] **Museum View**: At least 3 Phone Frames visible side-by-side.
2.  [ ] **V17 Compliance**: Matches screenshot (Title + Summary + Star + Time). No trashcans.
3.  [ ] **Localization**: "Q4_年度预算审计.m4a", "客户访谈_李总_V2.wav", "团队周会_2026-01-14.m4a".
4.  [ ] **Phased Delivery**: Plan reflects 4 distinct phases.

## Reference Examples
- **Apple Design Resources**: Comparison of list styles (Plain vs Grouped vs Inset).
- **Material 3**: Catalog styling.

## Handoff
- **Executor**: `/13-web-prototype` (You are already in the right mode).
- **Action**: Update `design_system_prototype.html` to include this gallery.
