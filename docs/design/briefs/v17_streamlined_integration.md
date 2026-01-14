## Design Brief: V17 Streamlined Integration (Master Prototype)

### User Goal
"Integrate V17 Streamlined (Light) into the master prototype for a try."

### Translation
Transplant the **V17 Streamlined (Light)** Audio Manager design—previously verified in the Gallery—into the **Master Prototype** as a fully navigable page. This replaces the current placeholder "Transcription" page with the high-fidelity list view, enabling a "real app" experience.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **Page Update**: Replace content of `#page-transcription` (or create `#page-audio`) with the V17 Streamlined List.
- **Theme**: Enforce **Light Mode** styling for this page (White/Grey "Frosted Ice"), overriding global dark mode if necessary for this view.
- **Components**:
    - **Header**: "录音管理" (Audio Manager) - prominent, aligned with V17 spec.
    - **List Cards**: Use `.v17-museum-card` styling (Light variant) as seen in Gallery Frame 2 (but Light).
    - **FAB**: Add the "Blue Plus" floating action button.
- **Navigation**: Ensure the "Chat" or "Recording" entry point in the dashboard leads here.

### ⚠️ Constrained
- **Data**: distinct sample data from the Gallery (e.g., "Team Meeting", "Q4 Audit", "Client Interview") must be preserved.
- **Zero-Chrome**: Scrollbars must remain hidden (`::-webkit-scrollbar { display: none }`).

### 🚫 Out of Scope
- **Gallery Mode**: Do not break the existing Gallery Overlay (keep it accessible via "Exhibition" button if it exists, or ensure it's hidden).
- **Other Pages**: Do not alter Home or Device pages significantly.

---

## Acceptance Criteria
1.  [ ] **Integration**: The V17 audio list appears inside the phone frame (not an overlay).
2.  [ ] **Aesthetic**: Matches "V17 Streamlined (Light)" – Clean, white/glass cards, high legibility.
3.  [ ] **Interaction**: List is scrollable (vertically).
4.  [ ] **Navigation**: Accessible via the main dashboard controls.

## Handoff
- Invoke `/ui-ux-pro-max` to execute the transplant.
