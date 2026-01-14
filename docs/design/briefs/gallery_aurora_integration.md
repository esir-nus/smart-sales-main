## Design Brief: Gallery Mode Aurora Integration

### User Goal
"Can't see how it really looks without the aurora background. You need to replicate the real display conditions."

### Translation
Enable the live **Aurora Animation (`#auroraCanvas`)** as the background for the **Gallery Overlay**, replacing the current solid `#0D0D12` void. This ensures visual fidelity for optical verification.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **CSS**: `body.gallery-mode #auroraCanvas` — Remove `display: none` constraint.
- **CSS**: `.gallery-overlay` — Change background to `transparent` (or semi-transparent `rgba(13,13,18,0.7)` if needed for contrast, but aim for raw aurora visibility).
- **Z-Index**: Ensure `.gallery-overlay` (z=9999) sits atop `#auroraCanvas` (z=0) but below nothing else blocking it.

### 🚫 Out of Scope
- **Aurora Logic**: Do not modify the JS animation logic itself.
- **Gallery Layout**: Do not change the horizontal scroll or frame positioning.

### 🛡️ Functional Invariants
- Gallery scrolling must remaining smooth.
- Text on "Dark Mode" frames (which are often transparent/glassy) must remain legible against the Aurora.

---

## Acceptance Criteria
1.  [ ] **Background**: The animated Aurora (Green/Blue/Purple blobs) is clearly visible behind the gallery frames.
2.  [ ] **Overlay**: The solid black void is removed.
3.  [ ] **Legibility**: Frames remain distinct from the background.

## Handoff
- Invoke `/ui-ux-pro-max` to execute CSS adjustments.
