# Prism Web Prototype (V1) Walkthrough

**Status**: Ready for Review
**Location**: `prototypes/prism-web-v1`
**Tech Stack**: React + Vite + Tailwind + Framer Motion

---

## 🚀 How to Run

1.  Open terminal:
    ```bash
    cd /home/cslh-frank/main_app/prototypes/prism-web-v1
    npm run dev
    ```
2.  Open the provided Localhost URL (e.g., `http://localhost:5173`).

---

## 📱 Interaction Guide

This prototype simulates the **Prism V1** interaction model using hardcoded scenarios.

### 1. The Vibe Check (Layer 1)
![Top Drawer Fixed](/home/cslh-frank/.gemini/antigravity/brain/7f638731-ae8f-4b61-8c6c-b0a347b1f160/scheduler_top_drawer_final_1769415617621.png)
<!-- slide -->
![Swipe Gesture](/home/cslh-frank/.gemini/antigravity/brain/7f638731-ae8f-4b61-8c6c-b0a347b1f160/scheduler_swipe_active_1769416320459.png)
<!-- slide -->
![Absolute No Drift](/home/cslh-frank/.gemini/antigravity/brain/7f638731-ae8f-4b61-8c6c-b0a347b1f160/scheduler_absolute_no_drift_1769418758505.png)
<!-- slide -->
![Premium Polish](/home/cslh-frank/.gemini/antigravity/brain/7f638731-ae8f-4b61-8c6c-b0a347b1f160/scheduler_pretty_v2_1769413740817.png)
*   **Aurora**: Observe the background. It should be a breathing mesh of Blue, Indigo, and Cyan (NOT plain white).
*   **Knot FAB**: Look at the bottom right. The "Knot" should complete a breathing cycle every 4 seconds.
*   **Zero-Chrome**: The app runs in a 375px phone frame. There should be no scrollbars.

### 2. Scenario A: "Analyst Mode" (Click Input Bar)
*   **Action**: Click the **Input Bar** (Floating Capsule).
*   **Result**:
    1.  User message appears ("Run deep analysis...").
    2.  Mode switches to **ANALYST**.
    3.  **Plan Card** slides down at the top (with "Running" spinner).
    4.  **Knot FAB** spins (Thinking state).

### 3. Scenario B: "Scheduler Drawer" (Drag or FAB)
*   **Action**: Click the **Knot FAB** OR drag from the top edge of the screen causing the drawer to appear.
*   **Result**:
    1.  **Scheduler Drawer** slides down with sticky physics.
    2.  Review the **Month/Day Carousel** (Pills).
    3.  See the **Conflict Card** (Orange styling).

---

## 🛠 Design Implementation Details

*   **Frosted Ice**: Implemented via `backdrop-blur-xl` and `bg-white/65`.
*   **Physics**: Drawer uses `framer-motion` for spring-based snap physics.
*   **Typography**: Using `Inter` with `t-hero`, `t-body` utility classes.

## ✅ Verification Checklist
- [x] Aurora Background visible?
- [x] Knot FAB breathing?
- [x] Drawer draggable?
- [x] Plan Card animations smooth?
