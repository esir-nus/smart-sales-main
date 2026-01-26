## Design Brief: Safe Area Correction

### User Goal
Fix content "covered off" at the top (Notch/Dynamic Island overlap).

### Translation
The `Header` component is currently `pt-2` (8px). The device notch is `36px`. This causes the top ~28px of the header content to be obscured.
We need to increase the top padding to account for the **Status Bar / Notch Safe Area**.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **Header Component**: Increase `pt` (padding-top) or `h` (height) to clear the notch.
- **Layout**: Ensure the headers content is centered *below* the notch area.

### 🚫 Out of Scope
- **Notch Component**: Do not remove the notch (it's a constraint).
- **Other Components**: Do not move Plan Card or Input Bar unless necessary.

---

## Visual Specifications
- **Target Top Padding**: At least `44px` (iOS Safe Area standard) or visually balanced to clear the 36px notch.
- **Recommendation**: Use `pt-12` (48px) or `pt-[44px]` for the Header container, and adjust height accordingly (e.g. `h-auto` or `min-h-[X]`).

## Acceptance Criteria
1. [ ] Header icons (Menu, Phone) and Title are fully visible *below* the black notch.
2. [ ] No visual overlap between notch and text.

---

## Handoff
Execute with `/ui-ux-pro-max` on `src/components/Header.tsx`.
