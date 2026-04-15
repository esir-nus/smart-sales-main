# Design Brief: SmartLab Single Phone Emulator

## User Goal
"I need **one phone emulator only** to preview different designs. This emulator is a **clone of the prototype** but with **no UX logic** — it just faithfully displays UI design."

## Translation
Replace the 14-frame gallery with a **single phone emulator** that:
- Visually matches `app_prototype.html`
- Swaps content based on category tab selection
- Has no working drawers, page navigation, or interactive UX

---

## Architecture

```
┌────────────────────────────────────────────────────┐
│ Dashboard                                          │
│ [录音展览] [字体规范] [组件库] [色彩系统] [Theme] [Mode] │
└────────────────────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────┐
│                                                    │
│         ┌───────────────────────────┐              │
│         │  SINGLE PHONE EMULATOR    │              │
│         │  (320×692, 44px radius)   │              │
│         │                           │              │
│         │  Content = f(category)    │              │
│         │  - 录音展览 → Audio page   │              │
│         │  - 字体规范 → Type page    │              │
│         │  - 组件库 → Comp page      │              │
│         │  - 色彩系统 → Color page   │              │
│         │                           │              │
│         └───────────────────────────┘              │
│                                                    │
└────────────────────────────────────────────────────┘
```

---

## Guardrails

### ✅ In Scope (DELETE + REPLACE)
- **DELETE**: All 14 gallery frames in `#category-audio`, `#category-typography`, `#category-components`, `#category-colors`
- **CREATE**: Single `.phone-emulator` element with 4 content slots
- **CLONE**: Phone structure from `app_prototype.html` (status bar, input footer, aurora bg)

### ⚠️ Constrained
- **Dashboard**: Keep existing category tabs, just rewire JS to swap content in single phone

### 🚫 Out of Scope
- `app_prototype.html` — do not modify
- UX logic (drawer opening, page navigation)

### 🛡️ Functional Invariants
- [ ] Categories switch phone content (no alerts)
- [ ] Theme/Mode toggles affect the phone
- [ ] Aurora background visible on page

---

## Content Per Category

| Category | Phone Displays |
|----------|----------------|
| **录音展览** | V17 Audio Manager page (recording cards, no drawer) |
| **字体规范** | Typography specimen (Display → Micro scale) |
| **组件库** | Buttons, Cards, Chips (from prototype style) |
| **色彩系统** | Color palette grid (Aurora + Semantic) |

---

## Acceptance Criteria

1. [ ] Single phone emulator centered on page
2. [ ] Clicking category tabs swaps phone content
3. [ ] Phone visually matches `app_prototype.html` style
4. [ ] Theme/Mode toggles work on the phone
5. [ ] No gallery scroll — single phone only

---

## Handoff
- Execute via `/ui-ux-pro-max`
