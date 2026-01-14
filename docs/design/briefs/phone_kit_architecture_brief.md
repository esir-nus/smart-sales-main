# Design Brief: SmartLab Phone Kit Architecture

## User Goal
"The VI guide doesn't show up after clicking buttons. Aurora vibe only added to the large background, not the phone screens. Need a **dedicated phone kit** that's easy to replicate to all phones containing necessary prototype design that serves as a clone of the app prototype."

## Translation (UI Director)
1. **Category tabs need real content** — clicking 字体规范, 组件库, 色彩系统 should display actual VI spec content in phone frames
2. **Phone frames need internal Aurora** — Each gallery phone should have its own mini-Aurora effect, not just solid backgrounds
3. **Phone Kit = Reusable Template** — Abstract the common elements (status bar, frame chrome, footer) into a clone-able component

---

## 🔴 Senior Reviewer: Hard No (Current Anti-Pattern)

### 1. Copy-Paste Hell
Each gallery phone frame is **50+ lines of inline-styled HTML**:
```html
<div class="gallery-phone-frame">
    <div class="status-bar" style="color: black;">...</div>
    <div class="museum-frame-content" style="background: #F2F2F7;">
        <h1 style="margin: 40px 0 10px 0; font-size: 28px; padding-left: 10px; color: black;">录音管理</h1>
        <!-- 10+ card divs with inline styles -->
    </div>
</div>
```
**Problem**: Changing any shared property requires editing 14 places.

### 2. Empty Categories
Category buttons call `alert()` instead of showing content:
```javascript
if (category !== 'audio') {
    alert('该分类即将推出: ' + category);
}
```
**Problem**: Buttons promise functionality that doesn't exist.

### 3. Static Phone Interiors
Phone content areas are solid colors (`#F2F2F7`, `#000`) while the page background has Aurora animation.
**Problem**: The phones look like screenshots, not living prototypes.

---

## 🟡 Yellow Flags

- **Inline styles everywhere** — Makes AI-assisted modifications error-prone
- **No shared phone CSS class** — `.gallery-phone-frame` exists but interior styles are all inline
- **Mode toggle incomplete** — Dark mode changes page but not the phone frame displays (they're static)

---

## 💡 What I'd Actually Do: Phone Kit Pattern

### The Kit Structure
```
┌─────────────────────────────────────────┐
│  phone-kit.html (Template)              │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │ .phone-kit-frame                    │ │
│ │  ├─ .phone-status-bar (reusable)    │ │
│ │  ├─ .phone-aurora-bg (optional)     │ │
│ │  ├─ .phone-content (slot)           │ │
│ │  └─ .phone-footer (optional)        │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ CSS Variables for theme switching:      │
│ --phone-bg, --phone-text, --phone-accent│
└─────────────────────────────────────────┘
```

### Usage Pattern
```html
<div class="phone-kit-frame" data-theme="light" data-has-aurora="true">
    <!-- Status Bar auto-included via CSS ::before -->
    <div class="phone-content">
        <!-- Your page content here -->
    </div>
</div>
```

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope: Create in SmartLab
1. **Phone Kit CSS** — Reusable `.phone-kit-frame` class with variants
2. **Phone Aurora Effect** — Mini canvas/gradient inside phone frames
3. **Typography Page** — 字体规范 category content (font scales, weights)
4. **Components Page** — 组件库 category content (buttons, cards, chips)
5. **Colors Page** — 色彩系统 category content (Aurora palette, semantic colors)

### ⚠️ Constrained
- **Existing 14 Audio Frames** — Refactor to use Phone Kit, but preserve visual appearance
- **Category JS** — Replace `alert()` with actual page switching

### 🚫 Out of Scope
- **App Prototype file** — Already cleaned, don't modify
- **Aurora Canvas code** — Working fine on page background

### 🛡️ Functional Invariants
- [ ] All 14 Audio Gallery frames must remain visible and correct
- [ ] Theme/Mode toggles must work for all phone frames
- [ ] Gallery horizontal scroll must remain functional

---

## Proposed Phone Kit CSS (V1)

```css
/* PHONE KIT - Reusable Template */
.phone-kit-frame {
    --phone-bg: #F2F2F7;
    --phone-text: #000;
    --phone-accent: #007AFF;
    
    width: 320px;
    height: 692px;
    border-radius: 44px;
    background: var(--phone-bg);
    position: relative;
    overflow: hidden;
    box-shadow: 0 25px 50px rgba(0,0,0,0.3);
}

.phone-kit-frame[data-theme="dark"] {
    --phone-bg: #000;
    --phone-text: #FFF;
    --phone-accent: #0A84FF;
}

/* Internal Aurora (Optional) */
.phone-kit-frame[data-aurora="true"]::before {
    content: '';
    position: absolute;
    inset: 0;
    background: 
        radial-gradient(ellipse at 30% 20%, rgba(0,200,100,0.2), transparent 50%),
        radial-gradient(ellipse at 70% 80%, rgba(0,100,255,0.2), transparent 50%);
    pointer-events: none;
    z-index: 0;
}

.phone-kit-frame .phone-content {
    position: relative;
    z-index: 1;
    padding: 60px 16px 16px 16px; /* Space for status bar */
    height: 100%;
    overflow-y: auto;
    color: var(--phone-text);
}
```

---

## Acceptance Criteria

### Phone Kit
1. [ ] `.phone-kit-frame` class works standalone with `data-theme` and `data-aurora` attributes
2. [ ] Existing 14 Audio frames refactored to use Phone Kit (visual parity maintained)
3. [ ] Phone frames have visible internal Aurora gradient

### Category Pages
4. [ ] 字体规范 tab shows Typography specimen page (font scales, weights)
5. [ ] 组件库 tab shows Component showcase (buttons, cards, chips)
6. [ ] 色彩系统 tab shows Color palette page (Aurora colors, semantic tokens)

### Integration
7. [ ] Theme toggle (Aurora/GlassOS) affects phone frame interiors
8. [ ] Mode toggle (Light/Dark) affects phone frame interiors

---

## Verification Plan

### Browser Testing
1. Open `smartlab.html`
2. Click each category tab → verify content appears (no alerts)
3. Toggle Dark mode → verify phone frames change
4. Verify Aurora gradient visible inside phone frames
5. Horizontal scroll → all 14 Audio frames still render

---

## Handoff
- For execution: Invoke `/ui-ux-pro-max`
- Estimated scope: Create Phone Kit CSS + 3 category pages content
