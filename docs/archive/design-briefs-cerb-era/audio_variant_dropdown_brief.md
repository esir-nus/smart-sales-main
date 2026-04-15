# Design Brief: Audio Variant Dropdown Selector

## User Goal
"Where are the audio drawer page designs? I said to have a **dropdown menu** so I can easily select and apply them."

## Translation
Add a dropdown selector to the Audio category that allows switching between all 14 audio card variant designs within the single phone emulator.

---

## Guardrails

### ✅ In Scope (Create/Modify)
- **Dropdown menu** in Audio category header (inside phone or in dashboard)
- **Restore all 14 audio variants** as switchable content:
  - V17 Streamlined (Light + Dark)
  - V16 Liquid Void (Light + Dark)
  - V15 Solar Air (Light + Dark)
  - V14 Solar Fluid (Light + Dark)
  - V12 Legacy (Light + Dark)
  - Challenger-J Holographic (Light + Dark)
- **JS logic** to swap content on dropdown selection

### ⚠️ Constrained
- **Phone emulator structure**: Keep single emulator, only swap internal content
- **Dashboard**: May add dropdown to Audio tab area, but don't restructure

### 🚫 Out of Scope
- Typography, Components, Colors slots (already working)
- App Prototype file (do not modify)

### 🛡️ Functional Invariants
- [ ] All 14 variants must be accessible
- [ ] Light/Dark mode toggle affects selected variant
- [ ] Dropdown is clearly visible and tappable

---

## Implementation Details

### Dropdown Location
**Option A**: Inside phone screen (below "录音管理" title)
**Option B**: In dashboard next to "录音展览" tab

**Recommendation**: Option A (inside phone) for faithful prototype feel.

### Dropdown Options
```
V17 Streamlined ← Default
V16 Liquid Void
V15 Solar Air
V14 Solar Fluid
V12 Legacy
Challenger-J
```

### Content Structure
Each variant has its own content slot:
- `#variant-v17` (already exists, needs rename)
- `#variant-v16`
- `#variant-v15`
- `#variant-v14`
- `#variant-v12`
- `#variant-chj`

### Dark Mode Handling
Dark mode toggle affects the **currently selected variant**. Each variant must define both light and dark styles via CSS variables.

---

## Acceptance Criteria

1. [ ] Dropdown visible in Audio category
2. [ ] All 6 variant families selectable (V12, V14, V15, V16, V17, CH-J)
3. [ ] Selecting variant changes phone content immediately
4. [ ] Dark mode toggle works on any selected variant
5. [ ] Dropdown styled to match prototype aesthetics

---

## Handoff
Execute via `/ui-ux-pro-max`
