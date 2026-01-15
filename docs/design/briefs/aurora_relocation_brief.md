# Design Brief: Aurora Background Relocation

## User Goal
"The emulator is a visual clone of the App Prototype. The Aurora background is built in the wrong place — it should be the **background of the emulator**, not the whole webpage."

## Translation
Move the Aurora animated canvas from the webpage level to INSIDE the phone emulator, matching how `app_prototype.html` renders it.

---

## Guardrails

### ✅ In Scope (Must Modify)
- **Aurora Canvas**: Move from webpage body to inside `.phone-emulator`
- **Canvas sizing**: Resize from viewport to phone dimensions (375×812)
- **Canvas positioning**: Position behind phone content (z-index layering)
- **Webpage background**: Set to simple dark color (`#111` or similar)

### ⚠️ Constrained
- **Aurora animation logic**: Keep existing blob animation, just relocate
- **Phone screen content**: Cards should remain above Aurora

### 🚫 Out of Scope
- Dashboard (keep as is)
- Variant dropdown logic (keep as is)
- Card variants (keep as is)

### 🛡️ Functional Invariants
- [ ] Aurora visible inside phone emulator
- [ ] Aurora respects Light/Dark mode toggle
- [ ] Cards remain readable above Aurora
- [ ] Webpage background is simple dark (no Aurora bleeding outside phone)

---

## Implementation Details

### Current State (Wrong)
```
┌─────────────────────────────────────┐
│ FULL WEBPAGE                        │
│ ┌─────────────────────────────────┐ │
│ │ Aurora Canvas (full viewport)  │ │
│ │                                 │ │
│ │       ┌─────────────┐          │ │
│ │       │ Phone Frame │          │ │
│ │       │ (No Aurora) │          │ │
│ │       └─────────────┘          │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

### Target State (Correct)
```
┌─────────────────────────────────────┐
│ WEBPAGE (Dark solid bg: #111)       │
│                                     │
│           ┌─────────────────┐       │
│           │ Phone Emulator  │       │
│           │ ┌─────────────┐ │       │
│           │ │Aurora Canvas│ │       │
│           │ │ (phone size)│ │       │
│           │ └─────────────┘ │       │
│           │   Content       │       │
│           └─────────────────┘       │
│                                     │
└─────────────────────────────────────┘
```

### Technical Steps
1. Remove `#auroraCanvas` from webpage body
2. Add `<canvas id="auroraCanvas">` inside `.phone-emulator`
3. Update canvas sizing to phone dimensions (375×812 minus status bar)
4. Update `resizeCanvas()` to use phone size instead of viewport
5. Set canvas `z-index: 0`, content slots `z-index: 1`
6. Set webpage body background to solid dark

---

## Acceptance Criteria

1. [ ] Aurora visible ONLY inside phone emulator
2. [ ] Webpage background is simple dark (no Aurora)
3. [ ] Aurora animates correctly at smaller scale
4. [ ] Cards visible above Aurora
5. [ ] Dark mode toggle affects both Aurora and content

---

## Handoff
Execute via `/ui-ux-pro-max`
