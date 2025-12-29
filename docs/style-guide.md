# SmartSales Mobile UI Style Guide

> **Scope & Precedence**
>
> This document defines the **visual language** for **SmartSales mobile**:
>
> - React web prototype (`/ui`, Tailwind-based) – **visual reference only**
> - Android app (Jetpack Compose) – **primary implementation target**
>
> **Behavior, layout, flows, and states are defined in `docs/ux-contract.md`.**  
> This style guide:
>
> - Describes **colors, typography, spacing, shapes, and component visual patterns**.
> - Uses React `/ui` only as a **visual example**, not as a behavioral spec.
> - Must **never override `docs/ux-contract.md`**.  
>   If React or this guide disagree with `ux-contract`, **`ux-contract` wins** and this file (and React) must be updated.
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
   - Red only for errors and destructive actions.

5. **Consistent patterns across pages**

   - Hero, quick skills row, input bar, cards, drawers, and lists **look and behave the same** everywhere.
   - When in doubt, reuse an existing pattern instead of inventing a new one.

---

## 2. Color System

### 2.1 Palette Tokens

| Token / Role        | Hex        | Typical Usage                                                                 |
| ------------------- | ---------- | ---------------------------------------------------------------------------- |
| **AppBackground**   | `#F2F2F7` | Global page background, chat content background, drawer backgrounds.        |
| **SurfaceCard**     | `#FFFFFF` | Cards, chat bubbles (base), drawers, dialogs, Home hero surface.           |
| **SurfaceMuted**    | `#F2F2F7` | Muted cards, secondary surfaces, empty-state blocks.                        |
| **BorderDefault**   | `#E5E5EA` | Card borders, input borders, section dividers, ChatInput top border.       |
| **AccentPrimary**   | `#007AFF` | Primary actions, hero accent text, quick skill chips, links.               |
| **TextPrimary**     | `#000000` | Main titles, important labels.                                              |
| **TextSecondary**   | `#3A3A3C` | Secondary headings, descriptive body text, “Let’s get started”.             |
| **TextMuted**       | `#8E8E93` | Helper text, bullets, subtitles, timestamps, input placeholder.            |
| **DangerText**      | `#EF4444` | Destructive actions (Delete, Sign out), error labels.                       |
| **DangerSurface**   | `#FEF2F2` | Error banners, destructive confirm dialog backgrounds.                      |
| **BackdropOverlay** | `rgba(0,0,0,0.3)` | Scrim behind drawers, dialogs, and full-screen overlays.          |

### 2.2 Android Mapping

Use `MaterialTheme.colorScheme` or an `AppColors` object mapping these roles. Example:

```kotlin
object AppColors {
    val AppBackground   = Color(0xFFF2F2F7)
    val SurfaceCard     = Color(0xFFFFFFFF)
    val SurfaceMuted    = Color(0xFFF2F2F7)
    val BorderDefault   = Color(0xFFE5E5EA)
    val AccentPrimary   = Color(0xFF007AFF)
    val TextPrimary     = Color(0xFF000000)
    val TextSecondary   = Color(0xFF3A3A3C)
    val TextMuted       = Color(0xFF8E8E93)
    val DangerText      = Color(0xFFEF4444)
    val DangerSurface   = Color(0xFFFEF2F2)
    val BackdropOverlay = Color(0x4D000000) // 30% black
}
````

**Rules:**

* Background of `Scaffold`: `AppBackground`.
* Cards, chat bubbles, drawers: `SurfaceCard` with `BorderDefault` border where relevant.
* Primary buttons & interactive text: `AccentPrimary`.
* Error banners and critical confirmations: `DangerText` on `DangerSurface`.

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
> Behavior and exact states are defined in `docs/ux-contract.md`.
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

  * No user messages, no assistant messages, no imported transcripts.
* Once any content exists, the hero **is not rendered** for that session at all.
* Hero is **not a chat bubble** and never appears in the scrollable message list.

**Visual structure:**

* Layout: centered `Column` inside a full-size `Box`.

* Elements (top → bottom):

  1. `LOGO` text or brand mark — `HeroBrand`, `AccentPrimary`.

  2. Greeting — `HeroGreeting`, `TextPrimary`
     “Hello, {userName}”

  3. Subtitle — `HeroSubtitle`, `AccentPrimary`
     “I am your sales assistant”

  4. Bullet block (Body + TextMuted):

     * Title: “I can help you:”
     * Items:

       * “• Analyze user profile, intent, and pain points.”
       * “• Generate PDF/CSV documents and mind maps.”

  5. Ending line — `HeroSubtitle`, `TextSecondary`
     “Let’s get started”

* Under the hero: **one** `QuickSkillRow` followed by a divider.

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

   * Behavior / layout matches `docs/ux-contract.md` and `docs/Orchestrator-V1.md` (CURRENT). Archived versions are for historical reference only.
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

If any conflict is found between this file and `docs/ux-contract.md`, **update this file and React** to match the UX contract, not the other way around.
