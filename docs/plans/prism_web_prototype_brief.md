# Prism Web Prototype Design Brief & Persona Discussion

**Status:** Draft / For Review
**Date:** 2026-01-26
**Target:** Prism V1 Interaction Validation (Chat + Scheduler)

---

## 👥 Persona Discussion Notes

### 1. 🟢 Senior Engineer (The Realist)
> "We are not building a backend today. We are building a *behavior simulator*."

*   **Constraint**: The prototype must strictly follow the **Prism V1 Data Flow** (§3.1 in `Architecture.md`), specifically the **Simulated Streaming** (20ms/char). We are *not* hooking up a real LLM yet; we need to validate the *feel* of the interaction first.
*   **Architecture**: Use a simple local state store (Zustand or React Context) to mimic the `Orchestrator` state. I don't want to see complex Redux boilerplate for a prototype.
*   **Mock Strategy**: We need hardcoded "Scenario Data" (e.g., "Scenario A: Complexity Analysis", "Scenario B: Schedule Conflict") to force the UI into specific states (Thinking Box, Plan Card, Conflict Card) without relying on random AI output.

### 2. 🧠 UX Specialist (The User Advocate)
> "The critical test here is the 'Mode Switch' and the 'Drawer Physics'."

*   **Friction Check**: The transition from **Coach** (Chat) to **Analyst** (Plan Card) needs to feel seamless. The Plan Card must *persist* at the top even if I switch back to Coach. This is a core Prism behavior (§4.6).
*   **Scheduler Interaction**: The Top Drawer (Scheduler) is gesture-heavy. On web, this needs to work with *mouse drag* as well as touch. If it feels janky, the whole "Top-Down" metaphor fails.
*   **Feedback Loops**: When a user resolves a conflict (Rethink), they need immediate visual feedback. The conflict card should transform into a resolved state instantly.

### 3. 🎨 UI Director (The Guardian of Vibe)
> "It must look like a native app or it fails Layer 1."

*   **Mandate: Aurora & Glass**: Light Mode is not white. It is **Frosted Ice**. The background must be the animated Aurora Gradient (as defined in `style-guide.md` §6.10).
*   **Mandate: Zero-Chrome**: Implement this inside a **Phone Frame** container (375px width centered). No scrollbars. No browser focus rings. 
*   **The Knot**: The FAB must "breathe". This is non-negotiable. It's the heartbeat of the app.

---

## 📋 Prototype Scope (Phase 1)

### 1. App Shell
*   **Container**: Fixed 375x812px mobile viewport simulation.
*   **Background**: Live Aurora Animation (Canvas/CSS).
*   **Navigation**:
    *   Top: Header (Home/Chat Dynamic).
    *   Bottom Right: **Knot FAB** (Breathing).
    *   Bottom Center: **Floating Capsule** Input.

### 2. Chat Interface (Dual-Engine)
*   **System I (Mascot)**:
    *   Ephemeral chat bubbles (out-of-band overlay).
    *   Simulated streaming (20ms/char).
*   **System II (Orchestrator)**:
    *   **Thinking Box**: Accordion-style, auto-folding after 3s.
    *   **Task Board**: Persistent card at top of chat stream.
    *   **Tasks**: Checkbox items with "Running" states.

### 3. Scheduler Interface (Top Drawer)
*   **Gesture**: Pull-down from top.
*   **Layout**:
    *   Month Carousel (Pills).
    *   Day Carousel (Pills).
    *   Timeline View (Vertical).
*   **Cards**:
    *   **Task Card**: Standard time-block.
    *   **Conflict Card**: "Rethink" UI (Resolve A vs B).

---

## 🛠 Implementation Plan

### Step 1: Foundation (Vite + Tailwind)
*   Setup mobile frame wrapper.
*   Implement `design-tokens.json` as CSS variables.
*   Create `AuroraBackground` component.

### Step 2: Component Library (The "Prism Kit")
*   `KnotFAB` (with breathing animation).
*   `GlassCard` (Standard & Highlight).
*   `ChatBubble` (User & Assistant).
*   `ThinkingBox` (Stateful accordion).
*   `PlanCard` (Checklist logic).

### Step 3: View Assembly
*   **View A (Chat)**: Implement Orchestrator logic to toggle modes.
*   **View B (Scheduler)**: Implement Framer Motion drag gestures for drawer.

### Step 4: Scenario Wiring
*   Hardcode "Scenario: A3 Project Analysis" (Triggers Thinking -> Plan Card).
*   Hardcode "Scenario: Schedule Conflict" (Triggers Scheduler Conflict Card).

---

## ❓ Alignment Questions
*   **Q1**: Are you okay with mocking the data for now (Scenario-based) rather than connecting to an API? (Senior Eng recommends Yes).
*   **Q2**: Should we focus strictly on **Light Mode** (Frosted Ice) for this prototype to perfect the Aurora effect first? (UI Director recommends Yes).
