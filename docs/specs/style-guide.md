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
| **AppBackground**   | `#F7F7F7`  | `#0D0D12` | Global page background. |
| **SurfaceCard**     | `#FFFFFF`  | `#1C1C1E` | Cards, chat bubbles (assistant), drawers, dialogs. |
| **SurfaceMuted**    | `#F2F2F7`  | `#2C2C2E` | Secondary surfaces, input bar background. |
| **BorderDefault**   | `#E5E5EA`  | `#38383A` | Subtle dividers, card borders. |
| **AccentPrimary**   | `#007AFF`  | `#0A84FF` | Primary actions, links, user bubbles. |
| **TextPrimary**     | `#000000`  | `#FFFFFF` | Main titles, important labels. |
| **TextSecondary**   | `#3A3A3C`  | `#EBEBF5` | Subtitles, body text. |
| **TextMuted**       | `#8E8E93`  | `#98989D` | Helper text, timestamps, placeholders. |
| **DangerText**      | `#EF4444`  | `#FF453A` | Destructive actions, errors. |
| **GradientBrand**   | `Blue-Purple` | `Blue-Cyan` | Brand icon gradient, some hero text. |

### 2.3 Chroma Wave Gradients (The Signature)

**Visual Reference**: ![Chroma Wave V3](file:///home/cslh-frank/main_app/docs/home_chroma_wave_v3_1767947501094.png)

**Design Intent**: The wave is a **Harmonic, Multi-Layered Signal**, not a solid bar.
- **Organic**: Uses filled gradients with sinusoidal curves.
- **Depth**: Multiple layers (3+) moving at different speeds/phases.
- **Fluidity**: Transitions are water-like, not mechanical.

**Behavior Rule**: The wave is **TRANSIENT**. It is not a fixed footer.
* **Enters**: Gracefully when AI starts listening or processing.
* **Exits**: Gracefully fades out when generation is done.
* **Exception**: On the Home Hero, a subtle "breathing" version may appear briefly to show the app is ready, but should not persist annoyingly.

| State | Colors (Gradient Stops) | Behavior |
|-------|-------------------------|----------|
| **Hidden** | Transparent | Default state when waiting for user. |
| **Listening** | `#34C759` → `#00C7BE` (Teal-Green) | Expansion ripple effect from bottom. |
| **Thinking** | `#AF52DE` → `#FF2D55` (Purple-Pink) | Active lateral flow / shimmer. |
| **Error** | `#D70015` → `#FF3B30` (Red-Orange) | Brief tint then fade out. |

### 2.4 Android Mapping

```kotlin
object AppColors {
    // Light
    val LightBackground = Color(0xFFF7F7F7)
    val LightSurface    = Color(0xFFFFFFFF)
    
    // Dark
    val DarkBackground  = Color(0xFF0D0D12)
    val DarkSurface     = Color(0xFF1C1C1E)
    
    // Wave Gradients
    val WaveIdle = Brush.horizontalGradient(listOf(Color(0xFF007AFF), Color(0xFFA259FF)))
    val WaveActive = Brush.horizontalGradient(listOf(Color(0xFFAF52DE), Color(0xFFFF2D55)))
}
```

---

## 3. Typography

We follow a semantic type scale. Tailwind classes are examples for React `/ui`; Android uses `AppTypography`.

### 3.1 Roles

| Role             | Tailwind size | Approx px | Usage                                         |
| ---------------- | ------------- | --------- | --------------------------------------------- |
| **AppTitle**     | `text-lg`     | 17–18 px  | Top bar title: “AI Assistant”, “Chat History”.     |
| **HeroBrand**    | `text-4xl`    | 32–36 px  | “LOGO” in `ChatWelcome`.                      |
| **HeroGreeting** | `text-2xl`    | 24 px     | “Hello, {userName}”.                             |
| **HeroSubtitle** | `text-xl`     | 20 px     | “I am your sales assistant”.                     |
| **SectionTitle** | `text-base`   | 16 px     | Section headers: “Device Manager”, “Audio Library”, “Export”. |
| **Body**         | `text-sm`     | 14–15 px  | Normal descriptive text, bullets, row labels. |
| **Caption**      | `text-xs`     | 12–13 px  | Timestamps, subtle metadata, helper tips.     |

### 3.2 Android Mapping

Example `AppTypography`:

```kotlin
object AppTypography {
    val AppTitle      = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val HeroBrand     = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
    val HeroGreeting  = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    val HeroSubtitle  = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)
    val SectionTitle  = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val Body          = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val Caption       = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
}
```

**Rules:**

* Top bar titles: **AppTitle** + `TextPrimary`.
* Hero text: `HeroBrand`, `HeroGreeting`, `HeroSubtitle` with Accent/Primary colors.
* Buttons & quick skills: **SectionTitle** or **Body** with `fontWeight = Medium`.
* Timestamps & subtle metadata: **Caption** + `TextMuted`.

---

## 4. Spacing & Layout

### 4.1 Base Spacing Scale

Everything is built on a 4-pt rhythm.

| Token         | Value    | Usage                                   |
| ------------- | -------- | --------------------------------------- |
| **SpacingXS** | 4 dp     | Tiny gaps, icon padding.                |
| **SpacingS**  | 8 dp     | Between label & value, small chip gaps. |
| **SpacingM**  | 12 dp    | Inside chips/buttons.                   |
| **SpacingL**  | 16 dp    | Standard between sections & list items. |
| **SpacingXL** | 20–24 dp | Block spacing, hero vertical gaps.      |

### 4.2 Screen Padding

* Horizontal padding for most screens: **16 dp**.
* Top padding below app bar: **12–16 dp**.
* Bottom padding above ChatInput / primary button: **12–16 dp**.

Example:

```kotlin
val ScreenHorizontalPadding = 16.dp
val ScreenVerticalPadding   = 16.dp
```

Apply to `Column` / `LazyColumn` content consistently.

### 4.3 Layout Rules

* **Single scrollable column** per screen.
  Avoid nested `verticalScroll` + `LazyColumn` that cause infinite height issues.
* Top bar is **pinned** in `Scaffold`.
* For drawers/overlays, the drawer content scrolls within its own column; background stays fixed.

---

## 5. Shape & Elevation

### 5.1 Radius

| Component               | Radius                               |
| ----------------------- | ------------------------------------ |
| App header buttons      | 8–12 dp (rounded)                    |
| Quick skill chips       | 16 dp (pill-like)                    |
| Chat bubbles            | 16 dp (top) / 20 dp (bottom corners) |
| Cards & list rows       | 12–16 dp                             |
| ChatInput container     | 16–20 dp                             |
| Drawers / bottom sheets | 16–20 dp top corners                 |
| Dialogs                 | 16–20 dp                             |

Keep radii soft and consistent; avoid sharp corners and mixed styles on the same surface.

### 5.2 Shadows / Elevation

* Cards: subtle `shadow-sm` equivalent.
* Floating bars (ChatInput, header avatar): slightly stronger, like `shadow-md`.
* Drawers: small shadow against background plus **BackdropOverlay** scrim.
* Avoid heavy/dark shadows; keep them soft and short-range.

---

## 6. Component Patterns

> **Reminder:**
> Behavior and exact states are defined in `docs/specs/ux-contract.md`.
> This section describes **how components look** when in those states.

### 6.1 App Shell (Top Bar & Background)

* Background: `SurfaceCard` over `AppBackground`.
* Title: `AppTitle` + `TextPrimary`.
* Left icon: menu/hamburger for history drawer.
* Right icons: device manager, audio library, user/profile.
* Bottom border: `BorderDefault` 1 dp.

Android example (visual intent only):

```kotlin
TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = AppColors.SurfaceCard,
        titleContentColor = AppColors.TextPrimary
    ),
    /* ... */
)
```

### 6.2 Home Hero (`ChatWelcome`)

**Behavior summary (for quick reference)**
Full details: `ux-contract.md` §6.

* Hero appears **only when the current session is empty**:
* Hero is **not a chat bubble** and never appears in the scrollable message list.

**Visual structure:**

* Layout: centered `Column` inside a full-size `Box`.
* **Knot Symbol**: The visual anchor. A knot/infinity symbol (geometric, soft corners).
  * Color: `GradientBrand` (Blue-Purple).
  * Size: 64–80 dp.
  * **No text "LOGO"**.

* Elements (top → bottom):

  1. **Brand Mark** (The Knot Symbol) - animated on entry.

  2. **Greeting** — `HeroGreeting`, `TextPrimary`
     “你好” (Hello)

  3. **Subtitle** — `HeroSubtitle`, `TextSecondary`
     “我是您的智能助手” (I am your intelligent assistant)

  4. **Action Grid** (2x2):
     * Replaces the old bullet list.
     * 4 cards: "New Task", "Summarize", "Ideas", "Schedule".
     * Style: `SurfaceCard` with subtle shadow, icon + text.

* **Chroma Wave Integration**:
  * Position: Bottom of screen (overlay).
  * Behavior: **Reactive**. Shows only during interaction (Thinking/Listening).
  * Implies "I am working on your request."

### 6.3 Chat Input & Quick Skill Row

**Shared visual rules:**

* ChatInput container:

  * Background: `SurfaceCard`.
  * Top border: `BorderDefault`.
  * Shadow: medium (slightly lifted above content).
  * Radius: 16–20 dp.
* Text field:

  * Fill width, height ~48–56 dp.
  * Placeholder: `TextMuted`.
* Send button:

  * Primary state: filled chip with `AccentPrimary` background, white text.
  * Disabled state: lower opacity, no strong border.

**Quick skill chips:**

* Background: `SurfaceMuted` or `SurfaceCard` with subtle border.
* Text: `Body` + `TextSecondary`.
* Selected/active state: border and text in `AccentPrimary`, subtle background tint.

**Placement (must align with `ux-contract.md`):**

* **Empty session (hero visible):**

  * `Hero` (full screen)
    ↓
    `QuickSkillRow` directly under hero
    ↓
    ChatInput bar
* **Active session:**

  * No hero.
  * `QuickSkillRow` is rendered **inside the input area**, directly above the text field.
* There is **never more than one quick skill row visible**.

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
