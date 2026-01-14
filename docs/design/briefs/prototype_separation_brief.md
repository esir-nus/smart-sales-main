## Design Brief: Prototype Separation

### User Goal
"The two web pages have different purposes. Do the separation cleanly."

### Translation
Split the monolithic `design_system_prototype.html` (4452 lines) into two purpose-specific prototypes:

| File | Purpose | Contents |
|------|---------|----------|
| **`app_prototype.html`** | Phone Emulator | Dashboard + Device Frame + All Pages (Home, Chat, Audio, etc.) |
| **`smartlab.html`** | Design Lab / Gallery | VI Guides, Component Gallery, Museum Mode (14-frame parade) |

---

## Senior Reviewer Lens: Coupling Audit

### Current State (Anti-Pattern: Monolith)
```
design_system_prototype.html
├── CSS: Shared tokens (Aurora, Glass)
├── CSS: Gallery-specific styles
├── CSS: Phone Emulator styles
├── HTML: Dashboard controls (L2308)
├── HTML: Phone Frame + Pages (L2376-2690)
├── HTML: Gallery Overlay (L82-170)
├── HTML: Museum Frames (L4150-4450)
├── JS: Aurora Canvas animation
├── JS: Page switching (gotoPage)
├── JS: Gallery toggle (enterGalleryMode)
└── JS: Audio variant selector
```

### Target State (Clean Separation)
```
app_prototype.html
├── CSS: Shared tokens (import or inline)
├── CSS: Phone Emulator styles ONLY
├── HTML: Dashboard controls
├── HTML: Phone Frame + All Internal Pages
├── HTML: Status Bar, Drawer, Input Bar
├── JS: Aurora Canvas animation
├── JS: Page switching (gotoPage)
└── JS: Audio variant selector

smartlab.html
├── CSS: Shared tokens (import or inline)
├── CSS: Gallery/Museum styles ONLY
├── HTML: Minimal control bar (no dashboard)
├── HTML: Gallery Overlay (14-frame parade)
├── HTML: VI Guide sections (Typography, Colors, Components)
├── JS: Aurora Canvas animation (shared)
└── JS: Scroll handling
```

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope (May Modify Freely)
- **File Structure**: Create `app_prototype.html` and `smartlab.html`.
- **CSS Extraction**: Copy/move relevant CSS to each file.
- **HTML Extraction**: Move Dashboard + Phone Frame to `app_prototype.html`, Gallery Overlay + Museum to `smartlab.html`.
- **JS Refactoring**: Duplicate shared utilities (Aurora animation) or extract to separate `.js` file.

### ⚠️ Constrained (Modify With Care)
- **Shared Tokens**: The `:root` CSS variables must be identical in both files (or imported). Any divergence breaks token integrity.
- **Aurora Canvas**: Both files need the Aurora animation. Either duplicate the JS or extract to `aurora.js`.

### 🚫 Out of Scope (Do NOT Touch)
- **Gallery Frame Content**: The 14-frame parade content is final. Do not modify card HTML.
- **Page Content**: Home, Chat, Audio pages are final. Do not modify their HTML.
- **Aurora Animation Logic**: Do not change the blob animation algorithm.

### 🛡️ Functional Invariants
- [ ] `app_prototype.html` must display the phone emulator with all dashboard controls working.
- [ ] `smartlab.html` must display the 14-frame gallery with Aurora background.
- [ ] Both files must be **standalone** (no external dependencies except fonts).
- [ ] Zero-Chrome policy must be maintained in both files.

---

## Proposed File Structure

```
brain/[conversation-id]/
├── app_prototype.html       # Phone Emulator + Dashboard
├── smartlab.html            # Design Lab + Gallery
└── (deprecated) design_system_prototype.html  # Archive or delete
```

---

## Acceptance Criteria

1. [ ] **`app_prototype.html`**: Opens directly, shows Dashboard + Phone Frame, all page buttons work (Home, Chat, Audio).
2. [ ] **`smartlab.html`**: Opens directly, shows 14-frame Gallery with Aurora background, horizontal scroll works.
3. [ ] **Standalone**: Neither file depends on the other or external resources.
4. [ ] **No Regressions**: All existing features work identically to the monolith.
5. [ ] **Clean Delete**: The original `design_system_prototype.html` can be archived/deleted after verification.

---

## Handoff
- Invoke `/ui-ux-pro-max` to execute the separation.
