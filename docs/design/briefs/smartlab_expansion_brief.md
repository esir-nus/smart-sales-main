## Design Brief: SmartLab Expansion + App Prototype Cleanup

### User Goal
"Design Lab should be put in with SmartLab. The Design Lab should use a dashboard to serve like a navigation bar to stay organized (e.g., 'these are audio pages' category). Also for App Prototype, clean off the excessive buttons and codes to avoid confusing the agent."

### Translation
1. **SmartLab**: Evolve from "Gallery-only" to "Design Lab Hub" with:
   - Dashboard navigation bar (categories: Audio Gallery, Typography, Components, etc.)
   - The existing 14-frame Audio Gallery becomes one section
   - Future: Typography, Components, Persona sections

2. **App Prototype**: Strip to essentials for Phone Emulator:
   - Keep: THEME (Aurora/GlassOS), MODE (Light/Dark), PAGE navigation
   - Remove: VARIANT VIEW (V12-V17), DESIGN LAB section, STATE controls

---

## Senior Reviewer Lens

### 🔴 Hard No (Current Anti-Pattern)
- App Prototype has **17+ dashboard buttons** that confuse AI agents reading the code
- Design Lab buttons (Type, Comp, Persona) trigger page switches but those pages don't exist in App Prototype
- VARIANT VIEW buttons (V12-V17, CH-J) are gallery-specific, not app-specific

### 🟢 Good Calls
- The separation into two files was correct
- SmartLab already has the Gallery content

### 💡 What I'd Actually Do
1. **SmartLab**: Add a minimal dashboard with category tabs (Audio, Typography, Components)
2. **App Prototype**: Delete ~60% of dashboard buttons, keep only what controls the phone emulator

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope: SmartLab
- **Add**: Dashboard navigation bar at top
- **Add**: Category tabs: "录音展览" (Audio Gallery), "字体规范" (Typography), "组件库" (Components)
- **Keep**: The 14-frame Gallery as the "录音展览" section
- **Keep**: Aurora Canvas animation

### ✅ In Scope: App Prototype
- **Remove**: VARIANT VIEW section (buttons V12-V17, CH-J, ALL)
- **Remove**: DESIGN LAB section (Type, Comp, Persona, Values buttons)
- **Remove**: STATE section (Idle, Listen, Think buttons)
- **Remove**: Toggle HUD button
- **Remove**: 展览 button (Gallery is now in SmartLab)
- **Keep**: THEME section (Aurora, GlassOS)
- **Keep**: MODE section (Light, Dark)
- **Keep**: PAGE section (首页, 对话, 录音, 设备, 引导, 设置)

### 🚫 Out of Scope
- **Phone Emulator content**: Do not touch the device frame or pages inside
- **CSS Styles**: Do not modify shared styling
- **Gallery Frame HTML**: Do not modify the 14 phone frames in SmartLab

### 🛡️ Functional Invariants
- [ ] App Prototype phone emulator remains fully functional
- [ ] SmartLab Gallery remains scrollable with Aurora background
- [ ] Both files remain standalone (no external dependencies)

---

## Proposed Dashboard for SmartLab

```
┌─────────────────────────────────────────────────────────┐
│  CATEGORY: [ 录音展览 ] [ 字体 ] [ 组件 ] [ 色彩 ]     │
│                                                         │
│  THEME: [ Aurora ] [ GlassOS ]   MODE: [ Light ] [ Dark ]│
└─────────────────────────────────────────────────────────┘
```

- Default category: "录音展览" (Audio Gallery) - shows the 14 frames
- Other categories can show placeholder content initially

---

## Proposed Dashboard for App Prototype (Cleaned)

```
┌─────────────────────────────────────────────────────────┐
│  THEME: [ Aurora ] [ GlassOS ]   MODE: [ Light ] [ Dark ]│
│                                                         │
│  PAGE: [ 首页 ] [ 对话 ] [ 录音 ] [ 设备 ] [ 引导 ] [ 设置 ] │
└─────────────────────────────────────────────────────────┘
```

---

## Acceptance Criteria

### SmartLab
1. [ ] Dashboard visible at top with category tabs
2. [ ] "录音展览" tab shows the 14-frame Gallery
3. [ ] Theme/Mode controls still work
4. [ ] Aurora animation visible

### App Prototype
1. [ ] Dashboard reduced to ~8 buttons (from 17+)
2. [ ] Removed sections: VARIANT VIEW, DESIGN LAB, STATE, Toggle HUD, 展览
3. [ ] Page navigation still works (首页, 对话, 录音, 设备)
4. [ ] Phone emulator fully functional

---

## Verification Plan

### Browser Testing (Both Files)
1. Open `smartlab.html` → verify dashboard + gallery visible
2. Open `app_prototype.html` → verify minimal dashboard + phone emulator

---

## Handoff
- Invoke `/ui-ux-pro-max` to execute
