## Design Brief: V17 Audio Manager Refinement

### User Goal
"Update the audio manager: remove redundant header, fix text indent, remove input bars, and style as a drawer with 10% gap."

### Translation
Refine the **V17 Audio Manager** implementation to closer match a "Sheet/Drawer" interaction model rather than a full page. This involves removing specific UI elements (Header, Input Bar), enforcing strict layout rules for text alignment and summary length, and visually detaching the view from the top of the screen.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
-   **Structure**: Style `#page-transcription` as a bottom drawer (`top: 10%`, `border-radius`, `box-shadow`).
-   **Content**:
    -   Remove `h1` Title ("录音管理").
    -   Remove bottom `input` bar.
-   **Typography**:
    -   Force `.v17-museum-summary` to **1 line** (`white-space: nowrap`, `text-overflow: ellipsis`).
-   **Layout**:
    -   **Grid Alignment**: Ensure Title text starts at the exact same X-position for ALL rows, regardless of whether a "Star" icon exists. Suggestion: Use a fixed-width slot (e.g., `24px`) for the leading icon/space.

### 🚫 Out of Scope
-   **Data**: Keep the existing list items.
-   **FAB**: Keep the "Blue Plus" FAB.

### 🛡️ Functional Invariants
-   List navigation/scrolling must work within the new "Drawer" height.
-   Click targets must remain accessible.

---

## Acceptance Criteria
1.  [ ] **Drawer**: Page has ~10% gap from top, rounded top corners.
2.  [ ] **Headerless**: "Audio Manager" title is gone.
3.  [ ] **Clean**: No input bar at bottom.
4.  [ ] **Alignment**: All card titles align perfectly on the left edge.
5.  [ ] **One Line**: Summaries truncate after one line.

## Handoff
-   Invoke `/ui-ux-pro-max` to execute.
