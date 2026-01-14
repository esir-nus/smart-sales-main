# Design Brief: SmartLab Gallery Mode (V2)

**Authorized By**: UI Director
**Date**: 2026-01-14
**Status**: 🟡 Pending Approval

---

## User Goal
"For the gallery, I need complete prototypes (for testing animation) AND gallery items (for showcasing variants)."

## Translation
The user requires a **two-layer architecture**:

| Layer | Purpose | Contains |
|-------|---------|----------|
| **Complete Prototype** | Animation Testing | Full phone screen (status bar → content → input bar) |
| **Gallery Canvas** | Variant Comparison | Multiple Complete Prototypes side-by-side |

The previous implementation showed "nested screens" which is **rejected**. The new approach shows full prototypes floating on an infinite canvas.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope

#### 1. Gallery Canvas (The "Darkroom")
- **Structure**: A full-viewport (`100vw × 100vh`) overlay with `position: fixed`.
- **Background**: Deep Void (`#0D0D12`).
- **Layout**: `display: flex; gap: 60px; overflow-x: auto; align-items: center; padding: 60px;`
- **Children**: Multiple `.gallery-item` wrappers, each containing a complete prototype.

#### 2. Gallery Item (Complete Prototype)
Each gallery item is a **standalone phone frame** containing:
- **Status Bar** (9:41, signal, battery)
- **App Header** (menu, title, actions)
- **Content Area** (scrollable, contains V17 cards or other components)
- **Input Footer** (text input, send button)
- **Dimensions**: 360px × 780px, border-radius: 44px (iPhone style).
- **Label**: Title above each frame (e.g., "V17 Streamlined (Light)").

This structure means each gallery item is **interactive** – you can scroll, tap, and test animations within it.

#### 3. Variant Content
Populate the gallery with the following complete prototypes:
1. **V17 Streamlined (Light)** — 8 localized cards, swipe hints, progress bar.
2. **V17 Streamlined (Dark)** — Same content, dark theme.
3. **V12 Legacy (Comparison)** — Old layout with action buttons for contrast.
4. *(Optional)* **Challenger Variants** — If time permits.

#### 4. Navigation
- **Entry**: A dedicated "🖼️ 设计展览" (Design Gallery) button in the dashboard.
- **Exit**: A floating "✕ 关闭" button (top-left, fixed, z-index: 10000) to close the overlay.

### ⚠️ Constrained
- **Card Content**: V17 card HTML/CSS is already approved. Do NOT modify card internals.
- **Prototype Fidelity**: Each gallery item MUST feel like opening the real app. No placeholders.

### 🚫 Out of Scope
- Modifying the main device-frame page structure.
- Changes to Home, Chat, or Settings pages.

---

## Technical Architecture

```
body
├── .dashboard (hidden in gallery mode)
├── .main-wrapper (hidden in gallery mode)
│   └── .device-frame (the standard single-phone prototype)
└── .gallery-overlay (visible in gallery mode)
    ├── .gallery-item
    │   └── .gallery-phone-frame (360x780, interactive)
    │       ├── .status-bar
    │       ├── .app-header
    │       ├── .content (scrollable)
    │       └── .input-footer
    ├── .gallery-item (second variant)
    └── .gallery-item (third variant)
```

**Toggle Logic**:
```js
function openGallery() {
    document.body.classList.add('gallery-mode');
}
function closeGallery() {
    document.body.classList.remove('gallery-mode');
}
```

```css
body.gallery-mode .dashboard { display: none; }
body.gallery-mode .main-wrapper { display: none; }
body.gallery-mode .gallery-overlay { display: flex; }
```

---

## Acceptance Criteria
1. [ ] **Complete Frames**: Each gallery item is a full interactive phone prototype (not just card content).
2. [ ] **No Nesting**: The gallery overlay is NOT inside the main device frame.
3. [ ] **Animation Ready**: Content inside each gallery phone is scrollable and interactive.
4. [ ] **Localization**: All text in Simplified Chinese.
5. [ ] **8-Card Density**: Each V17 frame contains 8 items.
6. [ ] **Theme Parity**: Light and Dark variants are visually distinct and polished.
7. [ ] **Navigation**: "设计展览" button opens gallery; "关闭" button closes it.

---

## Reference
- User's reference image: Figma-style canvas with multiple phone screens.
- ![Reference Image 1](file:///home/cslh-frank/.gemini/antigravity/brain/8a2d0f18-f03a-4dfd-957f-b40e339c808d/uploaded_image_0_1768385690614.png)
- ![Reference Image 2](file:///home/cslh-frank/.gemini/antigravity/brain/8a2d0f18-f03a-4dfd-957f-b40e339c808d/uploaded_image_1_1768385690614.png)

---

## Handoff
- **Executor**: `/ui-ux-pro-max`
- **File**: `design_system_prototype.html`
