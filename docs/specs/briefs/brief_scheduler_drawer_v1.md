# Design Brief: Scheduler Drawer (Smart Agenda) V1

> **Role**: UI Director
> **Status**: 🟡 DRAFT (Waiting for User Approval)
> **Target**: Mobile Phone (~375-430px width)

---

### User Goal
"I want a 'Morning Briefing' style scheduler that feels like a premium assistant managing my day. It should drop down from the top, look like 'time flowing through glass,' and handle my schedule intelligence."

### Translation
Implement the **Top-Down Scheduler Drawer** ("Auto-Drop") using the **Aurora Premium** visual language. This is a comprehensive overlay that replaces the traditional calendar view with an intelligent, glassmorphism-based timeline.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope (May Modify Freely)
- **Top Drawer Container**: New full-screen overlay entering from TOP (`slideInVertically`).
- **Header Navigation**: Dual-Carousel system (Month Strip + Date Strip) or Date Pivot logic.
- **Timeline UI**: Vertical list of schedule items with glass effects.
- **Aurora Background**: Specific "Aurora Top-Down" variant (focused at top).

### ⚠️ Constrained (Modify With Care)
- **Typography**: Must use `AppTypography` styles; do not introduce new custom fonts.
- **Colors**: Must use tokens from `design-tokens.json` (Aurora, FrostedIce).
- **Icons**: Use standard icon set (Lucide/Material) consistent with existing app.

### 🚫 Out of Scope (Do NOT Touch)
- **Bottom Navigation**: The bottom chat input/knot area (Scheduler sits *above* or obscures this).
- **Existing Chat/History**: This is a separate mode; do not break existing Chat UI.

### 🛡️ Functional Invariants
- [ ] **Zero Chrome**: No visible scrollbars.
- [ ] **Auto-Drop**: Must support programmatic "slide down" on app launch.
- [ ] **Dismissal**: Swipe UP from bottom handle to close.
- [ ] **Localization**: **100% Simplified Chinese** for all text.

---

## UI Specifications (Aurora Premium)

### 1. The Container ("Glass Plate")
- **Behavior**: Slides down from top, covering 100% height (or leaving a peek at bottom).
- **Background**: `Surface + Glass Blur (High)` or `AuroraBrush` if heavily transparent.
- **Vibe**: "Frosted Ice" — clean, cool, organized.

### 2. Header: The Date Pivot
- **Month Strip**: Horizontal scroll or static display of current month (e.g., "2026年 1月").
- **Date Strip**: Horizontal carousel of days (Mon/Tue/Wed...).
- **State**: Selected day is highlighted (White pill or glowing dot).

### 3. The Timeline ("River of Time")
- **Layout**: Vertical scroll.
- **Connector**: A subtle vertical line connecting events (The "Thread").
- **Current Time**: A horizontal "Now" line cutting through the timeline.

### 4. Schedule Cards
- **Normal Item**:
  - Background: `FrostedIce` (White @ 65% alpha).
  - Content: Time (Left) | Title + Desc (Right).
- **P0 Priority Item** (Critical):
  - **Glow**: Subtle Orange/Red outer shadow or border.
  - **Visuals**: "Heatmap Intelligence" — feels warmer/urgent.
- **Conflict Item**:
  - Visuals: Warning indication (Amber tint).

### 5. Bottom Handle
- **Visual**: A small capsule/pill at the bottom center.
- **Interaction**: Drag UP to dismiss the drawer.

---

## Design Token Updates (If New Tokens Needed)
- [ ] `Scheduler.Background.Blur`: High blur value for the glass plate.
- [ ] `Scheduler.Item.P0.Glow`: Color token for priority item glow.
- [ ] `Scheduler.Timeline.Line`: Color for the vertical connector.

## Acceptance Criteria (5-Layer Audit)

### Layer 0: Platform
- [ ] **Mobile Width**: Layout fits 375-430px comfortably.
- [ ] **Thumb Zone**: Dismiss handle is easily reachable at bottom.

### Layer 1: Vibe ("Aurora Premium")
- [ ] **Glass Physics**: Cards feel like they are floating on a frosted layer.
- [ ] **Living**: Subtle pulse or animation when opening.

### Layer 2: Fidelity
- [ ] **Zero Chrome**: No scrollbars visible in the timeline.
- [ ] **Localization**: All text is Chinese ("日程" not "Schedule", "今天" not "Today").

### Layer 3: Composition
- [ ] **Legibility**: Times and Titles are clearly readable against the blur.
- [ ] **Hierarchy**: P0 items stand out against normal items.

### Layer 4: Functionality
- [ ] **Swipe Logic**: Swipe UP closes the drawer.
- [ ] **Scroll**: Timeline scrolls smoothly.

---

## 🔁 Regression Prevention Criteria
- [ ] **No Duplicate Elements**: Verify pure React/Compose hierarchy.
- [ ] **CSS/Style Syntax**: (If Web) Valid CSS modules.
- [ ] **Truncation**: Long task titles truncate with ellipsis.

## Handoff
- **Next Step**: After approval, invoke `@[/13-web-prototype-[mode]]` (or `/ui-ux-pro-max` if preferred) to build the Web Prototype first.
- **Database**: Implementation of `schedule_items` Room table can proceed in parallel.
