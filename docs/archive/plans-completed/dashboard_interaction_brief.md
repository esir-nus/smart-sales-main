## Design Brief: Dashboard "Clean State" Navigation

### User Goal
"When I click a Dashboard button (e.g., Analyst Mode), I expect the app to immediately show that mode. Currently, the Scheduler Drawer remains open, blocking the view, forcing me to manually close it to verify the change."

### Translation
Implement **Mutually Exclusive States** for the Dashboard navigation. Activating one primary mode should clean up overlays (Drawers, Modals) from others.

---

## Guardrails (WHAT TO TOUCH)

### ✅ In Scope
- **`App.tsx` / `handleDashboardNav`**: Update logic to enforce state cleanup.
- **State Logic**:
    - `Home`: Close Drawer, Hide Onboarding, Reset Mode.
    - `Analyst`: Close Drawer, Hide Onboarding, Trigger Plan.
    - `Onboarding`: Close Drawer, Show Onboarding.
    - `Scheduler`: Open Drawer (Onboarding can stay hidden or be hidden).

### 🚫 Out of Scope
- **Visual Design**: The dashboard look is fine. Only the *wiring* needs fixing.

### 🛡️ Functional Invariants
- **Persistence**: The Dashboard itself must remain visible.
- **Data**: Do not clear the chat history unless "Reset Home" is clicked (which implies a reset).

---

## Interaction Specifications

### 1. Home (Reset)
- **Action**: Click Home icon.
- **State**:
    - `isDrawerOpen` -> `false`
    - `isOnboarding` -> `false`
    - `showPlan` -> `false`
    - `mode` -> `'coach'`

### 2. System II (Orchestrator Focus)
- **Action**: Click Analyst icon.
- **State**:
    - `isDrawerOpen` -> `false` (CRITICAL FIX)
    - `isOnboarding` -> `false`
    - `mode` -> `'analyst'`
    - Trigger `runScenario('plan')`

### 3. Onboarding
- **Action**: Click Rocket icon.
- **State**:
    - `isDrawerOpen` -> `false` (Optional, as onboarding overlays, but cleaner to close)
    - `isOnboarding` -> `true`

### 4. Scheduler
- **Action**: Click Calendar icon.
- **State**:
    - `isDrawerOpen` -> `true`
    - `isOnboarding` -> `false`

## Acceptance Criteria
1. [ ] Clicking "Analyst" while Scheduler is open **automatically closes** the Scheduler.
2. [ ] Clicking "Home" while Scheduler is open **automatically closes** the Scheduler.
3. [ ] User sees immediate visual confirmation of the new state without extra clicks.

## Handoff
Execute with `/ui-ux-pro-max`.
