# SmartSales Mobile UI Style Guide

> **Scope**
> This guide defines the visual language for **SmartSales mobile** across:
>
> * React web prototype (`/ui` – Tailwind-based)
> * Android Compose implementation
>
> The React `/ui` is the **visual spec**. Android should match it using design tokens and theming. 交互与布局规范请参见 `docs/ux-contract.md`（UX 来源高于本文件），本文件仅覆盖颜色/字体/间距等视觉要素。

---

## 1. Design Principles

1. **Single-column, calm surfaces**

   * App background is soft grey.
   * Content sits on white cards with subtle borders and small shadows.
2. **Blue accent, never blue walls**

   * `#007AFF` is used for actions, highlights, and key text.
   * Backgrounds stay light grey/white, not solid blue/purple.
3. **Hierarchy via size & weight, not color**

   * Page titles and hero text use size/weight.
   * Secondary information uses lighter greys, not smaller opacity hacks.
4. **Neumorphic light touch**

   * Cards/buttons get soft radius + shallow shadow.
   * No heavy 3D blobs; everything stays clean and subtle.
5. **Consistent patterns across pages**

   * Hero, quick skills row, input bar shape, list rows, and drawers behave the same everywhere.

---

## 2. Color System

### Palette

| Token / Role        | Hex                  | React example                                 | Usage                                                             |
| ------------------- | -------------------- | --------------------------------------------- | ----------------------------------------------------------------- |
| **AppBackground**   | `#F2F2F7`            | `<div className="min-h-screen bg-[#F2F2F7]">` | Global page background, ChatInput container bg, sidebar headers.  |
| **SurfaceCard**     | `#FFFFFF`            | `bg-white`                                    | Cards, chat bubbles (base), drawers, dialogs.                     |
| **SurfaceMuted**    | `#F2F2F7`            | `bg-[#F2F2F7]` icon circles                   | Icon halos, muted chips, secondary surfaces.                      |
| **BorderDefault**   | `#E5E5EA`            | `border-[#E5E5EA]`                            | Card borders, header bottom border, input borders, ChatInput top. |
| **AccentPrimary**   | `#007AFF`            | `text-[#007AFF]`, active send button, skills  | Primary actions, hero accent text, skill buttons, links.          |
| **TextPrimary**     | `#000000`            | `text-black`                                  | Main titles, important labels.                                    |
| **TextSecondary**   | `#3A3A3C`            | “让我们开始吧” (`text-[#3A3A3C]`)                   | Secondary headings, descriptive text.                             |
| **TextMuted**       | `#8E8E93`            | Hero bullet text (`text-[#8E8E93]`)           | Hints, subtitles, timestamps, input placeholder.                  |
| **DangerText**      | `#EF4444` (~red-500) | `text-red-500`                                | Destructive actions (logout, delete).                             |
| **DangerSurface**   | `#FEF2F2` (~red-50)  | `bg-red-50`                                   | Icon backgrounds for dangerous options.                           |
| **BackdropOverlay** | `rgba(0,0,0,0.3)`    | `bg-black/30`                                 | Sidebar/modal scrim.                                              |

### Android mapping (recommended)

In your theme / `DesignTokens`:

```kotlin
object AppColors {
    val AppBackground = Color(0xFFF2F2F7)
    val SurfaceCard   = Color.White
    val SurfaceMuted  = Color(0xFFF2F2F7)
    val BorderDefault = Color(0xFFE5E5EA)
    val AccentPrimary = Color(0xFF007AFF)
    val TextPrimary   = Color(0xFF000000)
    val TextSecondary = Color(0xFF3A3A3C)
    val TextMuted     = Color(0xFF8E8E93)
    val DangerText    = Color(0xFFEF4444)
    val DangerSurface = Color(0xFFFEF2F2)
}
```

Use these in `MaterialTheme` or your own `DesignTokens` instead of hard-coded colors.

---

## 3. Typography

We follow Tailwind’s scale as semantic roles:

| Role             | Tailwind size | Approx px | Usage                              |
| ---------------- | ------------- | --------- | ---------------------------------- |
| **AppTitle**     | `text-lg`     | 17–18 px  | Header title “新对话”, “历史会话”.        |
| **HeroBrand**    | `text-4xl`    | 32–36 px  | “LOGO” in `ChatWelcome`.           |
| **HeroGreeting** | `text-2xl`    | 24 px     | “你好, {userName}”.                  |
| **HeroSubtitle** | `text-xl`     | 20 px     | “我是您的销售助手”.                        |
| **SectionTitle** | `text-base`   | 16 px     | “历史会话”, “用户中心” section titles.     |
| **Body**         | `text-sm`     | 14–15 px  | Regular text, bullets, row labels. |
| **Caption**      | `text-xs`     | 12–13 px  | Timestamps, chip subtext if any.   |

### Android mapping (example)

```kotlin
object AppTypography {
    val AppTitle      = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
    val HeroBrand     = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
    val HeroGreeting  = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    val HeroSubtitle  = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)
    val SectionTitle  = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val Body          = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val Caption       = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
}
```

**Rules:**

* Headers and hero text use **AppTitle / Hero*** styles.
* Buttons and chips use **SectionTitle or Body** with appropriate weight.
* Timestamps and subtle metadata use **Caption** + `TextMuted`.

---

## 4. Spacing & Layout

### Base spacing scale

Everything is built on a 4-pt rhythm.

| Token        | Value | Examples                               |
| ------------ | ----- | -------------------------------------- |
| **SpaceXS**  | 4 dp  | Tiny gaps, icon padding.               |
| **SpaceSM**  | 8 dp  | Between small controls.                |
| **SpaceMD**  | 12 dp | Chips gap, list item internal padding. |
| **SpaceLG**  | 16 dp | Page horizontal padding (`px-4`).      |
| **SpaceXL**  | 24 dp | Between hero blocks.                   |
| **Space2XL** | 32 dp | Hero top/bottom breathing room.        |

### Shell layout

* **Header height**: ~56–64 dp.
* **Page padding**:

  * Horizontal: 16 dp (`px-4`).
  * Top: header height + 8–16 dp.
  * Bottom: enough to sit above ChatInput.

On Android, use:

```kotlin
val ScreenHorizontalPadding = 16.dp
val ScreenVerticalPadding   = 16.dp
```

and apply consistently to `Column`/`LazyColumn` content.

---

## 5. Shape & Elevation

### Radius

| Component          | Radius                 |
| ------------------ | ---------------------- |
| App header buttons | 8–12 dp (rounded-lg)   |
| Cards              | 16–20 dp (rounded-2xl) |
| Chips / skill btns | 999 dp (fully pill)    |
| Dialogs / drawers  | 16–20 dp               |

### Shadows / elevation

* Cards: very subtle drop shadow (like Tailwind `shadow-sm`).
* Floating bars (ChatInput, header avatar button): slightly stronger `shadow-md`.
* Never use huge, dark shadows; keep them soft and short-range.

---

## 6. Component Patterns

### 6.1 App Shell (Layout)

**React spec** (from `Layout.js`):

* `min-h-screen bg-[#F2F2F7]`
* Sticky header with:

  * Left: hamburger (`Menu` icon)
  * Center: current page name
  * Right: avatar button (navigates to UserCenter)
* Optional sidebar drawer (for historical sessions) on **web**; on mobile activity we use hamburger → ChatHistory.

**Android rules:**

* Top `AppBar`:

  * Background: **SurfaceCard**
  * Bottom border: **BorderDefault**
  * Title: **AppTitle** (page-appropriate copy)
* Content:

  * Fills the rest of the screen on **AppBackground**.
* History / sidebar:

  * Accessed via **hamburger** (not a permanent left rail).

---

### 6.2 Home Hero (`ChatWelcome`)

From `ChatWelcome.jsx`:

* Centered column, `p-6 pb-32`.
* Content:

  * `LOGO` (HeroBrand, AccentPrimary).
  * “你好, {userName}” (HeroGreeting).
  * “我是您的销售助手” (HeroSubtitle, AccentPrimary).
  * Bullet list of capabilities (Body, TextMuted).
  * Ending line “让我们开始吧” (TextSecondary).

**Android:**

* Use a `Column` with:

  * `verticalArrangement = Arrangement.Center` when session is empty.
  * Horizontal padding = `ScreenHorizontalPadding`.
* Text:

  * Follow same copy and typography.
* When messages exist:

  * Hero can disappear or scroll away, matching React behavior.

---

### 6.3 Chat Input & Quick Skills

From `ChatInput.jsx`:

* Fixed at bottom:

  * Background: `bg-[#F2F2F7]/95`.
  * Border-top: `border-[#E5E5EA]`.
  * Slight blur & shadow.
* Inside:

  * **Skill buttons** row:

    * `flex gap-3 overflow-x-auto pb-3 mb-2`.
    * Skills: `['内容总结', '异议分析', '话术辅导', '生成日报']`.
    * Buttons: pill-shaped, `px-4 py-2`, `bg-[#F2F2F7]`, `text-[#007AFF]`.
  * **Textarea**:

    * White, bordered, padded left/right, 60–120 px tall.
  * **Send button**:

    * Right bottom, circular, **AccentPrimary** when enabled, neutral when disabled.

**Android:**

* Wrap input in a bottom-aligned `SurfaceCard` with:

  * Background: AppBackground with slight elevation.
  * Internal card for textarea: SurfaceCard + BorderDefault.
* Skill row:

  * Horizontal `Row` with `horizontalScroll`.
  * Use **chips** with pill radius, AccentPrimary text.
* Send button:

  * Circular `IconButton`:

    * Enabled: AccentPrimary, white icon.
    * Disabled: BorderDefault background, TextMuted icon.

---

### 6.4 Chat Bubbles

Pattern (implied from design):

* Max width ~70–80% of screen.
* Asymmetric corners (sender vs assistant).
* Colors:

  * User bubble: SurfaceCard + AccentPrimary text or darker text.
  * Assistant bubble: SurfaceMuted / SurfaceCard with TextPrimary.

**Rules:**

* Vertical spacing between bubbles: 8–12 dp.
* Horizontal padding: keep consistent with screen padding.
* Timestamps (if shown) use Caption + TextMuted.

---

### 6.5 Chat History

From `ChatHistory.jsx`:

* Grouped by “7天内 / 30天内 / 更早”.
* Each item:

  * Rounded card with padding.
  * Title, date, pinned indicator.
* Top bar:

  * Left back icon (`ChevronLeft`).
  * Title “历史会话”.
  * Right search icon.

**Android:**

* Group headers:

  * Use SectionTitle + TextMuted.
* Rows:

  * NeumorphicCard / card shape with padded content.
* Long-press:

  * Bottom sheet with actions: “置顶会话 / 重命名 / 删除 / 取消”.
* Search:

  * Search field in top bar or just below, styled like Input (SurfaceCard + BorderDefault).

---

### 6.6 AudioFiles & Transcript Drawer

**AudioFiles page:**

* Page of cards using NeumorphicCard:

  * File name, type, status badge.
* Status colors:

  * Default: muted grey.
  * Syncing / Transcribing: accent or subtle yellow.
  * Ready: maybe green accent (not defined above; pick a subdued green).

**Transcript drawer (`AudioDrawer.jsx`):**

* Slides over AudioFiles.
* Header with a handle bar and a title.
* Transcript text in a scrollable area, consistent with Body / Caption typography.

**Android:**

* AudioFiles:

  * Use the **same card radius & elevation** as History/UserCenter.
  * Status badges: small rounded pill, inside card.
* Drawer:

  * Modal bottom sheet or top sheet with a handle and card-like surface token.

---

### 6.7 DeviceManager

* Grid/list of device file cards:

  * Represent image/video/GIF.
  * Show name + maybe duration/size.
* Buttons “Apply to device”, “Refresh”, etc. use NeumorphicButton.

**Android:**

* Match card shape to AudioFiles / History.
* Maintain consistent spacing (grid with 16 dp gutters).
* Status/labels use Body + Caption.

---

### 6.8 UserCenter

From `UserCenter.jsx`:

* Hero card with user avatar & name.
* Menu rows:

  * Icon inside `w-10 h-10 rounded-2xl bg-[#F2F2F7]`.
  * Title + subtitle.
  * Right chevron.

**Android:**

* Use one main `NeumorphicCard` for the user header (avatar, name).
* Subsequent NeumorphicCards for grouped menu items.
* Maintain same paddings and icon treatments.

---

### 6.9 Buttons & Neumorphic Components

`NeumorphicCard.jsx` & `NeumorphicButton.jsx` define:

* Card: `neu-card` with padded content, subtle shadows, rounded-2xl.
* Button:

  * `neu-btn` for default secondary type.
  * `neu-btn-primary` for primary actions.

**Rules:**

* Use **NeumorphicButton(primary)** for main CTAs (Export, Connect, etc.).
* Use plain text buttons for low-emphasis actions (e.g., “取消” in dialogs).
* Ensure disabled state:

  * Lower opacity.
  * No hover/press scale.

---

## 7. Drawers & Overlays (HomeWithVerticalOverlays)

From `Home.jsx`:

* `pageIndex`:

  * `0` → Home.
  * `-1` → Audio (top).
  * `1` → Device (bottom).
* Vertical drag:

  * Drag down from Home → Audio.
  * Drag up from Home → Device.
* Backdrop blur/opacity to indicate stacked overlays.

**Visual rules:**

* No permanent left sidebar on mobile.
* Panels fill the width, with a visible **handle bar** at the top (and/or bottom).
* Background behind overlays uses AppBackground + subtle blur when multiple layers visible.

**Android:**

* Implement vertical drawers using the same state machine.
* Provide small handle bars on each overlay panel using SurfaceMuted + rounded corners.
* Let the Home overlay look visually identical to the React Home hero + chat.

---

## 8. Testing & Tags (for Android)

While not strictly “visual”, these ensure stability:

* Keep `PAGE_*` and `OVERLAY_*` tags stable; they represent **what** is shown, not **how** it looks.
* If you remove UI elements (like the left rail), provide **test-only hooks** to switch overlays instead of touching user-facing behavior.
* When you change copy (text), update tests that assert it; don’t weaken tests unless the feature truly changed.

---

## 9. Usage Checklist

When building or updating a screen:

1. **Background**: use AppBackground (`#F2F2F7`).
2. **Cards**: SurfaceCard + BorderDefault + standard radius.
3. **Typography**: map every text to a semantic role (AppTitle, Hero*, SectionTitle, Body, Caption).
4. **Spacing**: stick to 4-pt grid and `Space*` tokens.
5. **Buttons/Chips**: use neumorphic styles; AccentPrimary for primary actions.
6. **Drawers/Overlays**: no side rail; vertical panels with handles only.
7. **Web vs Android**: if in doubt, check the React JSX first, then replicate with tokens.

This guide should be saved as `style-guide.md` (or `mobile-style-guide.md`) in your repo, and treated as the source of truth for visual alignment between React and Android.
