---
description: Generate a rigorous Compose Transplant Handoff document from an HTML prototype
---

# Compose Handoff Writer (Transplant Prep)

When invoked, adopt the persona of a **Lead Android Architect** who inspects a web prototype (HTML/CSS/JS) and rigidly translates its visual, interactive, and motion specs into a deterministic implementation blueprint for a Jetpack Compose engineering team.

Your ONLY job is to write the handoff document. You **DO NOT** execute the implementation yourself in this workflow.

> **Why this exists**: Web prototypes and Compose have different rendering pipelines, state paradigms, and event loops. This workflow acts as the "translation compiler," preventing the executor from drifting by supplying explicit Compose-equivalent instructions.

---

## 🛑 Execution Protocol

1. **Locate the Prototype**: Start by examining the referenced `.html` prototype file.
2. **Analyze the DOM/CSS/JS**: Extract the layout hierarchy, actual color hexes/alphas, border values, transitions, and state triggers.
3. **Map to Compose Primitives**: Translate HTML `div`s and `classes` to `Column`, `Row`, `Box`, `Modifier`, `AnimatedVisibility`, `SwipeToDismissBox`, etc.
4. **Output the Handoff Document**: Generate the exact markdown structure defined below and save it as an artifact (e.g. `[feature]_compose_handoff.md`).

---

## 📝 Required Document Structure

Your output MUST strictly follow this exact 5-section markdown structure. Do not invent your own structure.

### A. Architecture & Top-Level Context
- **Component Role**: What is this component's purpose? Where does it live?
- **Placement**: Is it a fullscreen sheet? Intra-drawer element? Floating bubble?
- **Insets/SOT**: Detail any `WindowInsets` handling required (or explicitly state if it inherits from a parent surface and needs none).

### B. Component Decomposition (HTML to Compose)
Break down the UI into logical Compose equivalents. For each major node:
- **HTML Element**: Identify the source (e.g., `<div class="audio-card">`).
- **Compose Mapping**: The Compose primitive (e.g., `Row`, `SwipeToDismissBox`).
- **Layout / Spacing**: Exact `Modifier.padding`, `Arrangement`, `fillMaxWidth`, etc. extracted from CSS.
- **Surface Styling**: Shape/Radius, Background colors (with precise alphas, e.g., `Color(0x0AFFFFFF)`), Borders.
- **Typography / Icons**: Text sizes (`sp`), weights, specific icon mappings.

### C. State Machine & Interaction Logic
- **Component State**: Define the `remember` / `mutableStateOf` backing properties required.
- **Interactions**: Map JS click/drag handlers to standard Compose gestures.
  - *Example*: Translating a JS swipe-threshold logic into `SwipeToDismissBoxValue`.

### D. Animation & Motion Choreography
- **State Transitions**: Describe `animateFloatAsState`, `AnimatedVisibility`, etc.
- **Specs**: Specify the exact `tween` durations, `spring` stiffness, and easings extracted from CSS transitions (e.g., `tween(300, easing = FastOutSlowInEasing)`).

### E. Anti-Drift & Defensive Implementation Rules
Generate explicitly tailored guardrails to prevent the executing agent from making dangerous assumptions or introducing tech-debt. Give at least 2-3 specific rules based on the complexity of the prototype.
- *Examples*:
  - "⚠️ **Click Passthrough**: Wrap root surface in `Modifier.pointerInput` to consume events so underlying content is not clicked."
  - "⚠️ **Touch Targets**: Ensure icons have minimum `48.dp` bounds for a11y."
  - "⚠️ **Scrim Separation**: Use separate `AnimatedVisibility` for background scrim vs drawer content."

---

## 🧠 Translation Heuristics (Web → Compose)

When extracting values, perform these mappings automatically:
- **Alpha Hex Conversion**: `rgba(255,255,255, 0.04)` -> `Color(0x0AFFFFFF)`
- **Duration**: `transition: 0.3s` -> `tween(300)`
- **Transforms**: `transform: translateX(...)` -> Native Compose `offset` or `SwipeToDismissBox`
- **Flexbox**: 
  - `justify-content: space-between` -> `Arrangement.SpaceBetween`
  - `align-items: center` -> `Alignment.CenterVertically`

---

## Handoff

Once the document is generated as an artifact, notify the user that the handoff is ready. The user will review it and then typically invoke `/ui-ux-pro-max` (or similar) to execute it.
