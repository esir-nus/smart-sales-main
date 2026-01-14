## Design Brief: Museum Gallery Expansion (The "Variant Parade")

### User Goal
"Add all other variants into the gallery from `all` in the dashboard."

### Translation
Design a comprehensive horizontal gallery ("The Parade") that displays **every** major design iteration side-by-side. This transforms the Gallery Mode into a complete visual history of the design system.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope (Implement Freely)
- **Gallery Content**: Add frames for V13, V14, V15, V16, and CH-J.
- **Layout**: Extend the `.gallery-overlay` to support 8+ phone frames horizontally.
- **Ordering**: Chronological/Logical order: V12 → V13 → V14 → V15 → V16 → V17 (Light/Dark) → Challenger J.

### ⚠️ Constrained (Modify With Care)
- **V17 Frames**: Keep existing V17 Light/Dark frames as the "Gold Standard" reference at the end (or start).
- **Performance**: Ensure 8+ DOM-heavy frames don't crash the scrolling physics.

### 🚫 Out of Scope (Do NOT Touch)
- **Main Dashboard**: Do not alter the dashboard logic (except the existing toggle button).
- **Internal Logic**: Do not change how specific variants are rendered CSS-wise (reuse existing classes).

---

## Visual Specifications

### 1. The Parade Order (Horizontal Scroll)
1.  **V12 Legacy** (Baseline)
2.  **V13** (Iteration)
3.  **V14** (Iteration)
4.  **V15** (Iteration)
5.  **V16** (Liquid Void / Aurora)
6.  **V17** (Streamlined - Light) - *Centerpiece*
7.  **V17** (Streamlined - Dark) - *Centerpiece*
8.  **Challenger J** (Holographic)

### 2. Frame Construction
- **Reuse**: The opaque phone frame container (360x780px).
- **Reuse**: The status bar and input footer (consistent context).
- **Content**: Inject 8 "Mock Items" for *each* variant using their specific CSS classes (`.prod-style-v13`, `.prod-style-v16`, `.chal-j-container`, etc.).

### 3. Localization
- **Mandatory**: All mock content in all 8 frames must be 100% Simplified Chinese.

---

## Acceptance Criteria
1.  [ ] Gallery opens with horizontal scroll containing **8 distinct phone frames**.
2.  [ ] Each frame correctly renders its specific variant style (V12-V17, CH-J).
3.  [ ] All text in all frames is Simplified Chinese.
4.  [ ] Scrolling is smooth and performant.

## Handoff
- Invoke `/ui-ux-pro-max` to execute.
