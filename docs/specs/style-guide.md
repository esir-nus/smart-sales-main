# SmartSales Mobile UI Style Guide

> **Scope & Ownership**
>
> **Status**: 📝 **LIVING DOCUMENT** (Owned by UI Designer Persona)
>
> This document defines the **visual language** for SmartSales mobile.
>
> **Ownership & Hierarchy Logic:**
> 1. **Golden Master**: The **"Valid Design" App Prototype Screenshots** are the ultimate source of visual truth. If this text contradicts the *vibe* of the screenshots (blur softness, shadow tint, gradient blend), **the screenshots win**.
> 2. **UI Designer Authority**: The **UI Director (@[/12-ui-director])** owns this file.
> 3. **Code Matches Design**: If code implements a pattern that looks *better* than the doc/screenshots (and is approved), update this doc.
> 4. **Behavior vs. Visuals**: `ux-contract.md` owns *behavior*. This doc owns *pixels*.
>
> **Goal**: A living, authoritative definition of the "SmartSales" premium aesthetic.
>
> Documentation language: all documentation prose must be English. Chinese is allowed only inside fenced code blocks as code comments, and should be Simplified Chinese.

---

## 🎨 Design Tokens (Source of Truth)

All color, spacing, typography, and component values are defined in:

| File | Purpose |
|------|---------|
| [`design-tokens.json`](file:///home/cslh-frank/main_app/docs/design/design-tokens.json) | **Master token file** — primitives, semantics, scales |
| [`AppColors.kt`](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/feature/chat/home/theme/AppColors.kt) | Compose color consumers |
| [`AppTypography.kt`](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/feature/chat/home/theme/AppTypography.kt) | Compose typography consumers |
| [`AppSpacing.kt`](file:///home/cslh-frank/main_app/feature/chat/src/main/java/com/smartsales/feature/chat/home/theme/AppSpacing.kt) | Compose spacing/radius/elevation consumers |

> [!IMPORTANT]
> When updating design values, edit `design-tokens.json` first, then update the Kotlin consumers to match.

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

7. **MANDATE: Zero-Chrome & Glass Slabs**
   - **Zero-Chrome**: ABSOLUTELY NO browser artifacts. No scrollbars, no focus rings, no selection highlights. The web prototype is an *emulator* of a native app.
   - **Glass Slabs**: Light mode UI must use "Frosted Ice" (High blur + White Border) to separate layers. Never use flat gray.

---

## 2. Color System

### 2.1 Concept: "Living Intelligence"
The palette uses clean, neutral surfaces to let the **AI Intelligence** (Chroma Wave) shine.

> **Performance Note**: The Aurora gradients are complex. On low-end devices, implementing the `AuroraBrush` directly may cause overdraw. **Caching the background as a bitmap** is a valid optimization if frame rates drop below 60fps.

### 2.2 Palette Tokens

| Token / Role        | Light Mode | Dark Mode | Usage |
| ------------------- | ---------- | --------- | ----- |
| **AppBackground**   | `Aurora`   | `#0D0D12` | **Target UI**: Soft Blue/Cyan Mesh Gradient (Dark Default). |
| **SurfaceCard**     | `FrostedIce` | `#1C1C1E` | Light Mode = High Saturation Blur + White Border. |
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
    // Frosted Ice (Light Mode Slab)
    val FrostedIceBorder = Color(0xFFFFFFFF).copy(alpha = 0.65f)
    val FrostedIceShadow = Color(0x14000000) // 8% opacity black
    )
    // ...
}
```

...

### 6.2 Home Hero (`ChatWelcome`) (V12 Blueprint)

**A. Top Bar Layout (Target UI):**
*   **Left**: `[≡]` Hamburger (Open History Drawer) + `[⚡]` Device Icon (Device Status/Manager).
*   **Center**: `New Session` (Placeholder Title) or Current Session Title.
*   **Right**: `[+]` Icon (Create New Session).
*   **Removed**: Avatar (moved to Drawer), Profile Pill.

**B. Hero Content (Center Canvas):**
*   **Visibility**: Only active when session is empty.
*   **Greeting**: "你好, [SmartSales 用户]" (Gradient Text).
*   **Subtitle**: "我是您的销售助手" (Body, Secondary).
*   **Hero Actions**: Row of 3 Large Translucent Pills (`Smart Analysis`, `PDF`, `CSV`).

### 6.3 Chat Input (Floating Capsule)

**Target UI Pattern**:

*   **Container**:
    *   Shape: **Stadium / Capsule** (Fully rounded sides).
    *   **Shadow**: **Custom Blue-Tinted Shadow** (`spotColor = Color(0xFF007AFF)`) — Levitation effect.
    *   Insets: Floats 16dp above bottom, 16dp from sides.
    *   Content: `+` Button (Left) | Text Field (Center).
    *   **NO Knot**: Knot is moved to external FAB.

*   **Affordance**:
    *   **Scan Shine**: Subtle sheen traversing the placeholder text "输入消息..." in idle state.


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

### 6.6 History Drawer ("Memory Stream")

**Concept**: A deep glass overlay that feels like peering into the system's memory. It features organic "Aurora" light bleeding in from the edge.

* **Container**:
  * Width: Fixed `320dp`.
  * Background: `Surface` with `alpha = 0.75`.
  * **Effect**: `glassBlurHuge` (30dp) + `glassSaturation` (1.8x).
  * Radius: `24dp` (Top-Right, Bottom-Right).
  * Shadow: `Elevation.xl` + expanding shadow on open.

* **Structure**:
  1.  **Neural Link Card** (Top):
      *   Visual: Glass surface with green pulse if connected.
      *   Content: Smart Badge status.
  2.  **Memory Stream** (Middle):
      *   List of past sessions.
      *   **Animation**: Staggered slide-in (`easeOutExpo`, `50ms` delay per item).
  3.  **User Profile** (Bottom):
      *   Fixed footer with settings entry.

* **Behavior**:
  *   Scrim tap closes drawer.
  *   Aurora bleed overlay (`mix-blend-mode: screen`) adds depth.

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

### 6.10 Aurora Background (V16)

**Concept**: The Aurora consists of three distinct, animated radial gradient blobs that create a subtle, living atmosphere. NOT a mesh gradient—each blob is a separate light source.

* **Geometry**:
  * **Blob 1 (Top-Left)**: `center = (w * 0.2f, h * 0.1f)`, `radius = w * 0.5f`
  * **Blob 2 (Center-Right)**: `center = (w * 0.8f, h * 0.45f)`, `radius = w * 0.5f`
  * **Blob 3 (Bottom-Left)**: `center = (w * 0.3f, h * 0.85f)`, `radius = w * 0.5f`

* **Colors (with Alpha)**:
  | Blob | Color | Alpha |
  |------|-------|-------|
  | Top-Left | `#0A84FF` (Blue) | `0.28f` |
  | Center-Right | `#5E5CE6` (Indigo) | `0.24f` |
  | Bottom-Left | `#64D2FF` (Cyan) | `0.20f` |

* **Animation**: Blobs drift slowly via `sin/cos` offsets to create organic movement.

* **Design Principle**: "Distinct Blobs"—intentionally reduce radius and spread positions to prevent muddy overlap. Each blob should be recognizable as a separate light source.

### 6.11 Knot FAB & Tip Bubble (V18) — "Living Intelligence Avatar"

**Concept**: The Knot is the UI's "soul"—an always-visible, breathing infinity symbol that represents the AI assistant. Tapping it reveals a contextual tip.

* **Knot FAB**:
  *   **Type**: External Floating Action Button (FAB).
  *   **Position**: Fixed, bottom-right, `160dp` from bottom, `24dp` from edge.
  *   **Container**: `56dp` touch target, transparent background.
  *   **Icon**: `50dp` KnotSymbol (leaves breathing room).
  * **Stroke**: Core `1.5dp`, Glow `3dp` (atmosphere layer at `0.5f` alpha).
  * **Animation**: "Breathing" scale (`1.0f` → `1.35f`, 2s cycle). Spins when `isThinking = true`.
  * **Ripple**: Standard Material 3 ripple on tap.

* **Tip Bubble**:
  * **Position**: Floats above the Knot (`64dp` padding above FAB).
  * **Shape**: Asymmetric rounded corners `(16, 16, 4, 16)` to imply speech.
  * **Style**: Glassmorphism (`surface.copy(alpha = 0.9f)`, `8dp` shadow, subtle border).
  * **Content**: Rotates through localized tips on open (e.g., "试试 '帮我分析财报'").
  * **Animation**: `fadeIn + scaleIn` from bottom-right origin.

### 6.12 Input Placeholder Shimmer (V17)

**Concept**: A subtle "text scan" effect on the placeholder text ("输入消息...") that suggests the input is ready and attentive.

* **Target**: Placeholder text ONLY, not the entire input bar container.
* **Implementation**: Animated `Brush.horizontalGradient` applied via `TextStyle(brush = shimmerBrush)`.
* **Colors**:
  * Base: `onSurfaceVariant.copy(alpha = 0.4f)`
  * Shine: `onSurfaceVariant.copy(alpha = 1.0f)` (bright white peak)
* **Animation**: `shimmerOffset` animates from `-200f` to `600f` over 2 seconds, with 500ms delay between cycles.
* **Gradient Width**: `150f` pixels for the shine band.

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
