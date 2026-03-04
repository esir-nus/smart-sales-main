## Design Brief: Prototype Navigation Dashboard

### User Goal
Access hidden pages (e.g., Onboarding) and navigate quickly without traversing the natural UI flow.

### Translation
Implement a **Developer Dashboard Overlay** (DevTools) that sits *above* the app interface. It allows direct jump-to-state navigation.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **Dev Toggle**: A small, discrete button (e.g., "🛠️") in a corner (Top-Left Safe Area or Bottom-Left).
- **Dashboard Overlay**: A glassmorphic modal/panel containing a grid or list of reachable states.
- **New States**: Add dummy/visual-only states for "Onboarding", "Permission Request", etc., if they don't exist yet.

### 🚫 Out of Scope
- **Production Navigation**: This is for the *prototype harness*, not the production Router.
- **Complex Logic**: Do not build a real state machine. Just simple `setState` switches.

### 🛡️ Functional Invariants
- **Z-Index**: Must be highest (above Drawers and Modals).
- **Zero Impact**: When closed, it must have zero visual impact on the design fidelity.

---

## Visual Specifications

### 1. The Toggle
- **Position**: Fixed, Bottom-Left (`bottom-4 left-4`) or Top-Left (`top-14 left-4`).
- **Style**: Small, semi-transparent pill. `opacity-50` by default, `opacity-100` on hover.

### 2. The Dashboard Panel
- **Style**: Dark Glass (`bg-black/80 backdrop-blur-xl`).
- **Layout**: Simple List of Buttons.
- **Content**:
    - "🔵 Home" (Reset)
    - "🚀 Onboarding" (New)
    - "📊 System II (Orchestrator Focus)" (Force Trigger)
    - "📅 Scheduler Drawer" (Force Open)

## Acceptance Criteria
1. [ ] Clicking "Dev" font/icon opens the dashboard.
2. [ ] Dashboard allows switching to "Onboarding" view (visual mock).
3. [ ] Dashboard overlay does not break underneath layout (pointer-events handling).

---

## Handoff
Execute with `/ui-ux-pro-max`. Start by creating `src/components/PrototypeDashboard.tsx`.
