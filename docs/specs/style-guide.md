# SmartSales Mobile UI Style Guide

> **Scope & Ownership**
>
> **Status**: 📝 **LIVING DOCUMENT** (Owned by UI Designer Persona)
>
> This document defines the **visual language** for SmartSales mobile.
>
> **Ownership Logic:**
> 1. This file starts as a **legacy reference**.
> 2. The **UI Designer Persona (@[/12-ui-designer])** has explicit authority to **EDIT** this file to document optimal design practices discovered during development.
> 3. If the code (`Android Compose`) implements a better visual pattern than this doc, **update this doc** to match the code.
> 4. If this doc contradicts `docs/specs/ux-contract.md` (behavior), **`ux-contract.md` wins**, but you should propose a visual update here that respects the contract.
>
> **Goal**: Evolve this from a static legacy artifact into a dynamic source of truth for our visual system.
>
> Documentation language: all documentation prose must be English. Chinese is allowed only inside fenced code blocks as code comments, and should be Simplified Chinese.

---

## 1. Design Principles

1. **Single-column, calm surfaces**

   - One main column of content.
   - Plenty of whitespace; no dense dashboards.
   - Default background is soft gray, cards sit on top.

2. **Clear hierarchy**

   - Top bar: navigation + main title.
   - Main content: hero / list / cards.
   - Bottom: chat input or primary call-to-action.
   - Only one “visual hero” per screen.

3. **Readable, friendly text**

   - Chinese copy is short, concrete, and professional.
   - No sarcasm or playful slang in UI copy.
   - Use emphasis with weight/size, not random colors.

4. **Few, meaningful colors**

   - Primary blue for actions.
   - Neutral surfaces for content.
   - **Chroma Wave** gradients for AI presence (not UI chrome).

5. **Living Intelligence**

   - The app feels alive, not static.
   - Use the **Chroma Wave** to indicate state (idle, thinking, listening).
   - "Smart, not cute" — no robots or mascots.

6. **Consistent patterns across pages**

   - Hero, quick skills row, input bar, cards, drawers, and lists **look and behave the same** everywhere.
   - When in doubt, reuse an existing pattern instead of inventing a new one.

---

## 2. Color System

### 2.1 Concept: "Living Intelligence"
The palette uses clean, neutral surfaces to let the **AI Intelligence** (Chroma Wave) shine.

### 2.2 Palette Tokens

| Token / Role        | Light Mode | Dark Mode | Usage |
| ------------------- | ---------- | --------- | ----- |
| **AppBackground**   | `Aurora`   | `#0D0D12` | **Target UI**: Soft Blue/Cyan Mesh Gradient. |
| **SurfaceCard**     | `#FFFFFF`  | `#1C1C1E` | Cards, chat bubbles (assistant), drawers. |
| **FloatingCapsule** | `#FFFFFF`  | `#2C2C2E` | **Target UI**: High-elevation Input Bar. |
| **BorderDefault**   | `#E5E5EA`  | `#38383A` | Subtle dividers. |
| **AccentPrimary**   | `#007AFF`  | `#0A84FF` | Primary actions. |

...

### 2.4 Android Mapping (Aurora Update)

```kotlin
object AppColors {
    // Backgrounds
    val AuroraBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE0F7FA), // Top (Cyan tint)
            Color(0xFFF7F7F7), // Middle
            Color(0xFFFFFFFF)  // Bottom
        )
    )
    // ...
}
```

...

### 6.2 Home Hero (`ChatWelcome`)

**Visual structure (Target UI):**

* Layout: Centered.
* **Greeting**: "你好, [SmartSales 用户]" (Gradient Text).
* **Subtitle**: "我是您的销售助手" (Body, Secondary).
* **No "Let's Start"**.
* **Hero Actions (Pills)**:
  * Replaces discrete chips.
  * Style: Large, translucent pills (`SurfaceCard` + alpha).
  * Layout: Row of 3 (`Smart Analysis`, `PDF`, `CSV`).

### 6.3 Chat Input (Floating Capsule)

**Target UI Pattern**:

* **Container**:
  * Shape: **Stadium / Capsule** (Fully rounded sides).
  * Elevation: High (Floating above content).
  * Insets: Floats 16dp above bottom, 16dp from sides.
  * Content: `+` Button (Left) | Text Field (Center) | Knot Symbol (Right).

* **Knot Symbol Integration**:
  * Size: 40dp x 40dp (Fit inside capsule).
  * **Fix**: Ensure Lemniscate path scales to fit 40dp without clipping.
  * Interaction: Acts as status indicator. Morph to "Send" icon when typing (or overlay).

* **Scan Shine**:
  * Subtle sheen traversing the capsule in idle state.


### 6.4 Chat Messages (User & Assistant)

Visual only; conversational behavior in `ux-contract.md`.

* **User bubbles:**

  * Alignment: right.
  * Background: `AccentPrimary`.
  * Text: white, `Body`.
  * Radius: 16 dp top, 20 dp bottom.

* **Assistant bubbles:**

  * Alignment: left.
  * Background: `SurfaceCard`.
  * Border: `BorderDefault` (optional, very subtle).
  * Text: `Body`, `TextPrimary`.
  * Inline code / emphasis: bold or monospace as needed.

* **Spacing:**

  * Vertical gap between messages by same speaker: 4–8 dp.
  * Between different speakers: 8–12 dp.
  * Timestamp / metadata: `Caption` + `TextMuted` under or between clusters.

### 6.5 Cards (Device Manager, Audio Library, Settings)

* Background: `SurfaceCard`.
* Radius: 12–16 dp.
* Border: `BorderDefault` or none (depending on context).
* Shadow: small (card-like).
* Padding: 12–16 dp inside.
* Content:

  * Title: `SectionTitle` + `TextPrimary`.
  * Subtitle/helpers: `Body` + `TextMuted`.
  * Right side: icon or status label.

The Home screenshot pattern (two stacked cards) is the baseline card style.

### 6.6 Drawers & Overlays (History, Audio, Device)

Visual behavior shared by all drawers; specific flows in `ux-contract.md`.

* Background: `SurfaceCard`.
* Scrim: `BackdropOverlay` behind drawer.
* Width: 80–90% of screen width (for side drawers).
* Top corners: 16–20 dp on modal / bottom sheets.
* Internal layout:

  * Header row: `SectionTitle` + optional filter/actions.
  * Content: `LazyColumn` with list items.
  * Empty state text: `Body` + `TextMuted` centered or top-aligned.
  * History drawer uses a full-height column layout, with a device-status placeholder card at the top and a profile entry at the bottom; both use `SurfaceCard` style and align to drawer width.

### 6.7 Lists & Rows

Used in history, audio library, device files, etc.

* Height: 56–72 dp depending on content.
* Layout: leading icon/avatar → main text → metadata → chevron/action.
* Title: `Body` or `SectionTitle` + `TextPrimary`.
* Subtitle: `Caption` + `TextMuted`.
* Bottom divider: 1 dp `BorderDefault` spanning full width or with inset.

### 6.8 Empty States

* Use **SurfaceMuted** or naked `AppBackground` plus an icon or illustration.
* Include:

  * Title or short message (Body/SectionTitle).
  * Optional short tip in `Caption` + `TextMuted`.
* No heavy colors; keep it soft and informative.

### 6.9 Smart Badge (Header Status)

**Concept**: Replaces the generic "Device Status" text with a specialized, localized badge that pulses to indicate life.

* **Container**:
  * Shape: Pill / Capsule (`RoundedCornerShape(14.dp)`).
  * Background: `SurfaceVariant` (low alpha) or transparent with border.
  * Border: `BorderDefault` (subtle).
* **Content**:
  * Icon: Badge/ID-card icon (16dp).
  * Text: "智能工牌" (Smart Badge).
  * **Pulse Indicator**: A 6dp Green Circle (`#4CD964`).
    * Animation: `alpha` oscillates 0.4 -> 1.0 (authentic pulse).

---

## 7. Motion & Interaction

Visual guidance only; gesture behavior is in `ux-contract.md`.

### 7.1 Animation

* Use **subtle** animations:

  * Crossfade between hero and chat list.
  * Slide/fade for drawers and overlays.
* Durations:

  * Small transitions: 150–220 ms.
  * Drawer open/close: 220–260 ms.
* Easing:

  * Standard Material 3 curves (`FastOutSlowIn`, etc.).

### 7.2 Touch Targets

* Minimum target size: **48 × 48 dp**.
* Padding around icons: at least 8 dp.
* Keep destructive actions separated (spacing or grouping) from primary actions.

---

## 8. Accessibility & Contrast

* Make sure text on `SurfaceCard`/`SurfaceMuted` has enough contrast:

  * `TextPrimary` / `TextSecondary` on white/gray.
  * `TextMuted` only for less important info.
* Primary blue on white meets contrast for buttons; avoid blue on gray with low contrast.
* Do not rely on color alone; use labels and icons.

---

## 9. Usage Checklist

Before shipping a screen, verify:

1. **Doc precedence**

   * Behavior / layout matches `docs/specs/ux-contract.md` and `docs/specs/Orchestrator-V1.md` (CURRENT). Archived versions are for historical reference only.
   * This style guide has been used **only** for visuals.
   * React `/ui` was used as reference, not as the behavioral source of truth.

2. **Colors**

   * Background = `AppBackground`.
   * Cards / bubbles / drawers = `SurfaceCard`.
   * Primary actions & links = `AccentPrimary`.
   * Errors = `DangerText` on `DangerSurface`.

3. **Typography**

   * AppTitle/Hero/Section/Body/Caption roles used consistently.
   * No arbitrary font sizes.

4. **Spacing & layout**

   * 16 dp base padding on the sides.
   * 4-pt grid respected for gaps.
   * Only one main scrollable column.

5. **Shapes & elevation**

   * Soft radii (12–20 dp) for cards, input, drawers.
   * Shadows are subtle.

6. **Component patterns**

   * Home hero only appears in truly empty sessions, and never as a chat bubble.
   * Quick skill row placement matches the empty vs active rules.
   * Dialogs, drawers, lists, and buttons reuse the patterns from this guide.

If any conflict is found between this file and `docs/specs/ux-contract.md`, **update this file and React** to match the UX contract, not the other way around.
