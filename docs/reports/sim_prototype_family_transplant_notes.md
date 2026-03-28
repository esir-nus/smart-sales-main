# SIM Shell Family: Transplant Notes

## 1. State List Mapped

This prototype covers the single home/hero shell family across these required states:

- **State A (Empty Home / Hero)**: Initial greeting canvas, idle Dynamic Island, visible composer.
- **State B (Active Plain Chat)**: Standard conversation flow with user and assistant bubbles. Dynamic Island simulates an "upcoming" item.
- **State C (Chat + System Sheet)**: Continuation of the chat shell embedding a full-width, frosted `SystemSheet` primitive to distinguish system-authored content from normal dialogue.
- **State D (Chat Session with Attached Context)**: Demonstrates inline context anchoring (`ContextMarker` primitive) without routing to a dedicated product surface.
- **State E (Chat Session with Pending/Processing)**: Shows an inline `ProcessingSheet` with a spinner, keeping the composer active and the shell grounded.
- **State F (Longer Active Session)**: A multi-turn conversation that proves the heavy monoliths and spacing scale appropriately when the canvas fills up.

## 2. Primitive Inventory

The prototype was constructed using these exact, reusable DOM primitives (which map directly to future Compose equivalents):

1. `TopMonolith`: Fixed header, heavy blur background, contains the Island and controls.
2. `BottomMonolith`: Fixed bottom anchored input bar, heavy blur background.
3. `DynamicIsland`: Centered pill with 3 distinct state transitions (`idle`, `upcoming`, `conflict`).
4. `UserBubble`: Right-aligned, semi-transparent accent background, slight blur.
5. `AssistantBubble`: Left-aligned, transparent background, plain text projection.
6. `SystemSheet`: Full-width rounded card, frosted background, distinct from bubbles.
7. `GreetingCanvas`: Centered hero elements for State A.
8. `ContextMarker`: Inline pill used inside assistant or user bubbles.
9. `ProcessingSheet`: Full-width inline loader replacing the need for full-page blocking.

## 3. Token List (CSS Variables)

The styling is explicitly tokenized into the following logical groups:

- **Shell / Monoliths**:
  - `--shell-background`: True black (`#000000`)
  - `--monolith-background`: Heavy dark (`rgba(20, 20, 20, 0.85)`)
  - `--monolith-shadow`: Deep elevation drop shadow (`0 8px 32px rgba(0, 0, 0, 0.8)`)
- **Aurora (Ambient Canvas background)**:
  - `--aurora-1`, `--aurora-2`, `--aurora-3`: Low opacity blues, purples, and mints.
- **Surfaces**:
  - `--user-bubble-accent`: `rgba(255, 255, 255, 0.1)`
  - `--assistant-bubble-surface`: `transparent`
  - `--system-sheet-surface`: `rgba(30, 30, 35, 0.6)`
  - `--processing-sheet-surface`: `rgba(40, 40, 50, 0.5)`
  - `--context-marker-surface`: `rgba(50, 50, 60, 0.4)`
- **Dynamic Island**:
  - `--island-neutral`: `rgba(30, 30, 30, 0.9)`
  - `--island-upcoming`: `rgba(40, 100, 200, 0.9)`
  - `--island-conflict`: `rgba(200, 60, 60, 0.9)`
- **Typography / Borders**:
  - `--text-primary`, `--text-secondary`, `--text-tertiary`
  - `--border-light`, `--border-strong`
- **Radii / Effects**:
  - `--blur-heavy`, `--blur-medium`, `--blur-light` (10px to 40px ranges)
  - `--radius-sm` through `--radius-pill`
- **Geometry**:
  - `--top-monolith-height`: 110px
  - `--bottom-monolith-height`: 100px

## 4. Short Transplant Notes for Compose

1. **Monolith Positioning**: Do not use `Scaffold` topBar/bottomBar if they clip content underneath. Use a `Box` where the canvas `LazyColumn` spans `.fillMaxSize()` with `contentPadding` equal to the height of the monoliths.
2. **Dynamic Island**: Map the `data-state` CSS transitions directly to a Compose `AnimatedContent` or `animateDpAsState` for the width.
3. **Aurora Background**: The prototype uses CSS keyframes over blurred circles. In Compose, standard circles drawn on a `Canvas` with `.blur()` modifiers inside an infinite repeatable animation block will replicate this perfectly.
4. **Z-Index**: Ensure the Z-map is strictly: Shell Canvas < Aurora Particles < Chat Scrollable Canvas < Monoliths.
