# SmartSales Mobile UI Style Guide

> **Scope & Ownership**
>
> **Status**: LIVING DOCUMENT (Owned by UI Designer Persona)
> **Last Updated:** 2026-03-31
>
> This document defines the **visual language** for SmartSales mobile.
>
> **Ownership & Hierarchy Logic:**
> 1. **Approved Screenshots Are State-Scoped Reference**: Approved prototype screenshots are the visual reference only for the exact state they depict. They do not define the full state space of a feature by themselves.
> 2. **Owning Feature Specs Control Composition**: Feature-level `docs/cerb-ui/**` or `docs/cerb/**` shards own which elements appear in each state. This style guide owns the shared visual language those states should use.
> 3. **UI Designer Authority**: The **UI Director (@[/12-ui-director])** owns this file.
> 4. **Behavior vs. Visuals**: `docs/specs/prism-ui-ux-contract.md` and the owning feature shards own behavior and state composition. This doc owns pixels, material treatment, and visual pattern families.
> 5. **Code Is Not Automatic Visual Truth**: Existing app UI may reflect testing convenience or incomplete polish. Only approved visuals plus synced docs may redefine the aesthetic source of truth.
>
> **Goal**: A living, authoritative definition of the "SmartSales" premium aesthetic.
>
> Documentation language: all documentation prose must be English. Chinese is allowed only inside fenced code blocks as code comments, and should be Simplified Chinese.

---

## Design Tokens (Source of Truth)

All color, spacing, typography, and component values are defined in:

| File | Purpose |
|------|---------|
| [`design-tokens.json`](file:///home/cslh-frank/main_app/docs/design/design-tokens.json) | **Master token file** — primitives, semantics, scales |
| [`Color.kt`](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/theme/Color.kt) | Compose color definitions |
| [`Type.kt`](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/theme/Type.kt) | Compose typography definitions |
| [`Theme.kt`](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/theme/Theme.kt) | Compose theme root |

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

   - Primary black (`AccentPrimary`) for keys/actions.
   - `AccentBlue` for functional links only.
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

8. **MANDATE: Zero-Spec-Contamination & Zero-Emoji**
   - Spec ASCII diagrams (e.g., `HistoryDrawer.md`) define **STRUCTURE** only, not styling.
   - Specs and style docs must use plain professional prose with no emoji.
   - **NEVER USE EMOJI UNICODE IN UI. NEVER.** Real UI uses **design system icons** (Material Icons, custom SVGs) exclusively. This is a strictly enforced anti-pattern.
   - **Never copy spec formatting** literally. Example: textual "置顶" in spec -> Code: `Icon(PushPin) + Text("置顶")`

9. **MANDATE: Monolith Edge-to-Edge Architecture & Vibes**
   The primary structural philosophy for full-screen conversational interfaces is the **"Monolith Edge-to-Edge"** model, designed to convey the tension between strict architectural boundaries and fluid artificial intelligence.
   - **Borderless Icons**: Action icons (Menu, New Chat, Attach, Send) MUST be fully borderless. Remove artificial circular bounding boxes. The icons float completely raw.
   - **The Anchored Monoliths**: The Top Header and Bottom Input are not floating capsules. They are edge-to-edge architectural "blocks" (Monoliths) anchored to the absolute top and bottom of the screen. They utilize deep, dark backgrounds (`#020205` base + heavy shadow) to feel like physical, heavy glass hardware.
   - **The Feathered Seams**: Monoliths may use a short neutral feathered seam at their boundary to soften the hard cut between slab and canvas. The monolith body remains solid and edge-to-edge; only the seam softens. Use neutral black/white haze only, never aurora-tinted glow. The accepted SIM shell treatment is now explicit: the top seam stays restrained while the bottom seam reads heavier to keep the composer grounded.
   - **The Aurora Gap**: The space *between* the top and bottom monoliths (the chat canvas) is where the vibrant Aurora blobs glow aggressively. System cards (`SystemSheet`) stretch exactly edge-to-edge across this gap, acting as horizontal bands of frosted glass overlapping the aurora, rather than disconnected floating bubbles.
   - **Vibe Philosophy**: Grounded, heavy, architectural borders containing ethereal, highly-saturated AI plasma.

---

## 2. Color System

### 2.1 Concept: "Living Intelligence"
The palette uses clean, neutral surfaces to let the **AI Intelligence** (Chroma Wave) shine.

> **Performance Note**: The Aurora gradients are complex. On low-end devices, implementing the `AuroraBrush` directly may cause overdraw. **Caching the background as a bitmap** is a valid optimization if frame rates drop below 60fps.

### 2.2 Palette Tokens

| Token / Role        | Light Mode | Dark Mode | Usage |
| ------------------- | ---------- | --------- | ----- |
| **AppBackground**   | `#F5F5F7`  | `#0D0D12` | **Target UI**: "Sleek Glass" Pro Max Gray. |
| **SurfaceCard**     | `FrostedIce` | `#1C1C1E` | Light Mode = High Saturation Blur + White Border. |
| **FloatingCapsule** | `#FFFFFF`  | `#2C2C2E` | Legacy elevated capsule surface. SIM home/here shell now prefers the darker Bottom Monolith pattern. |
| **BorderDefault**   | `#E5E5EA`  | `#38383A` | Subtle dividers. |
| **AccentPrimary**   | `#000000`  | `#FFFFFF` | **Sleek Glass**: Stark Black keys. |
| **AccentBlue**      | `#007AFF`  | `#0A84FF` | Functional links / Analyst Mode. |



## 3. Component Patterns

### 3.1 Home / Here Surface Family

This section defines the visual family for the greeting-first home state and the SIM chat-first here state.

### 3.0 Top Safe-Area Law

Global rule for any surface that visually reaches the top edge:

* Default structure: `native status-bar inset -> 16dp blank safe band -> first visible content`.
* The 16dp band must stay empty. Do not place titles, chips, buttons, or helper content there as a spacing workaround.
* `statusBarsPadding()` alone is not sufficient to satisfy the rule.
* Exception surfaces are only those whose owning spec explicitly defines a top monolith or header slot.
* Exception surfaces may let the header itself occupy the post-inset zone, but the header's internal content must still clear the native status region.
* For top-down overlays that live under a persistent top monolith, preserve the shell-owned top-monolith alignment and add real status-bar awareness there rather than flattening the surface back to the generic blank-band rule.
* Do not invent per-screen top spacing rules when the surface fits the default pattern.

State ownership:

- `docs/cerb-ui/home-shell/spec.md` owns the generic empty-state `HomeShell` / `ChatWelcome` composition
- `docs/specs/prism-ui-ux-contract.md`, `docs/core-flow/sim-shell-routing-flow.md`, `docs/cerb-ui/dynamic-island/spec.md`, and current shared feature docs own the base-runtime shell state family
- approved prototype screenshots may act as the visual reference for one exact substate, but must not be mistaken for the whole feature contract

#### 3.1.1 Current Home Empty State (`HomeShell` / `ChatWelcome`)

Current empty-state guidance:

* **Shell Owner**: `HomeShell` owns the persistent top bar, aurora floor, and bottom input treatment.
* **Canvas Owner**: `ChatWelcome` owns only the empty-state greeting canvas.
* **Greeting**: "你好, [SmartSales 用户]" (Gradient Text).
* **Subtitle**: "我是您的销售助手" (Body, Secondary).
* **Do Not Infer**: Do not add hero skill pills, dashboard cards, or external floating accents to the home empty state unless the owning feature spec explicitly restores them.

#### 3.1.2 SIM Home / Here State Family

The SIM shell uses one visual family across multiple states rather than separate screen designs.

Required shared structure:

* **Top Monolith**: left hamburger/history trigger, centered Dynamic Island host slot, right new-session `+`.
* **Center Canvas**: the protected gap between top and bottom monoliths; this swaps content by state without changing the shell identity.
* **Bottom Monolith**: persistent composer foundation with left attach, center input, and right send-only action.
* **Shared Seam Rule**: empty home, active plain chat, and active audio-grounded chat should all keep the same monolith family and the same neutral feathered seam treatment rather than forking seam styling per state.

Required state family:

* **Empty Home**: greeting-first canvas with the normal shell chrome still visible.
* **Active Plain Chat**: sparse chat-first conversation canvas under the same shell.
* **Active Audio-Grounded Chat**: same chat shell with one attached audio context; do not redesign into a separate tool screen.
* **Pending / Transcribing Audio Chat**: use in-chat status/system presentation rather than forcing a route away from chat.
* **Support-Surface Entry State**: the same shell must remain the origin for History, Scheduler, Audio, Connectivity, and Settings support flows.

Approved prototype interpretation rule:

* The approved SIM prototype frame with the heavy dark top/bottom bars, centered island, and sparse conversation canvas is the visual baseline for the **active discussion** substate only.
* Extend from that frame to the other shell states; do not treat that one frame as the entire home/here contract.

### 3.2 Chat Input (Bottom Monolith)

**Target UI Pattern**:

*   **Container**:
    *   Shape: **Edge-to-Edge Rectangular Block** (No rounded stadium/capsule).
    *   **Background**: Deep solid base (`#020205`) with intense top shadow (`box-shadow: 0 -10px 40px rgba(0,0,0,0.8)`).
    *   Insets: Anchored explicitly to `bottom: 0`, `left: 0`, `right: 0`. Padding accommodates safe areas.
*   **Content**:
    *   Left: Sleek Attachment/Media Plus `(+)` icon (borderless); in SIM chat this reopens the Audio Drawer / selector rather than launching a generic file manager path.
    *   Center: Borderless text input field.
    *   Right: Sleek Send `(➤)` icon (borderless, send-only).
*   **Affordance**:
    *   The heavy block grounds the UI, serving as a solid physical foundation against the ethereal aurora background above it.
    *   **Scan Shine**: Subtle sheen traversing the placeholder text "输入消息..." in idle state.
    *   This same monolith pattern should persist across the empty home state and active SIM discussion states so the shell feels like one product family.


### 3.3 Conversation Surfaces (User Bubble, Assistant Bubble, System Sheet)

Visual only; conversational behavior lives in `docs/specs/prism-ui-ux-contract.md`, `docs/core-flow/sim-audio-artifact-chat-flow.md`, and the current shared audio/chat docs.

For the shipped SIM shell family, assistant bubbles, system sheets, status sheets, strategy sheets, and artifact sheets should read as one coordinated dark frosted material system rather than as unrelated cards.

Shared SIM family rule:

- background should stay dark, frosted, and restrained rather than bright glass or flat utility gray
- borders should be present but quiet
- dividers should be lower-contrast than the outer border
- these surfaces should feel premium and calm, never louder than the outgoing human bubble or the shell accent family

* **User bubbles:**

  * Alignment: right.
  * Background: a dedicated conversational accent on dark SIM shells. The current approved SIM discussion prototype uses a blue outgoing bubble rather than `AccentPrimary`.
  * Text: white, `Body`.
  * Radius: 16 dp top, 20 dp bottom.
  * Role: compact human-authored turn, not a system status panel.

* **Assistant bubbles:**

  * Alignment: left.
  * Background: `SurfaceCard`.
  * Border: `BorderDefault` (optional, very subtle). In the shipped SIM shell this border should stay softer than a normal utility card border.
  * Text: `Body`, `TextPrimary`.
  * Inline code / emphasis: bold or monospace as needed.
  * Role: plain conversational reply only.

* **System sheets:**

  * Layout: edge-to-edge or near-edge-to-edge horizontal frosted bands across the aurora gap.
  * Background: glass/slab treatment rather than a compact chat bubble.
  * Use for: guidance copy, progress/status, artifact insertion, scheduler follow-up prompt/chip presentation, or other shell/system-authored content.
  * Role: distinguish system-owned output from simple assistant dialogue.

* **Status sheets:**

  * Layout: left-anchored medium-width frosted slab rather than full-width utility card.
  * Header: compact phase/title on the left with a quieter action label on the right.
  * Divider: subtle and lower-contrast than the outer sheet border.
  * Role: in-chat processing/progress state, not a dashboard card.

* **Strategy sheets:**

  * Must stay inside the same dark frosted SIM family as assistant/system/status surfaces.
  * Primary confirm action may use the SIM blue accent, but should remain more restrained than the shell send action.
  * Secondary amend action should stay text-first and quiet, with at most a faint supporting fill.

* **Artifact sheets:**

  * Must stay in the same dark frosted family as strategy/system/status surfaces.
  * Internal sections should read as one continuous sheet with restrained dividers, not as stacked nested utility cards.
  * Transcript, summary, chapters, speakers, and adjacent result sections should preserve clear hierarchy without increasing chrome level per section.

* **Spacing:**

  * Vertical gap between messages by same speaker: 4–8 dp.
  * Between different speakers: 8–12 dp.
  * The approved SIM discussion prototype uses a tighter `10dp` chat-history rhythm as the default conversation target.
  * Timestamp / metadata: `Caption` + `TextMuted` under or between clusters.

### 3.4 Cards (Device Manager, Audio Library, Settings)

* Background: `SurfaceCard`.
* Radius: 12–16 dp.
* Border: `BorderDefault` or none (depending on context).
* Shadow: small (card-like).
* Padding: 12–16 dp inside.
* Content:

  * Title: `SectionTitle` + `TextPrimary`.
  * Subtitle/helpers: `Body` + `TextMuted`.
  * Right side: icon or status label.

Use this pattern for utility/support surfaces such as settings, device management, and non-conversational browse rows.
Do not treat generic utility cards as the default visual language for the SIM discussion canvas when a `SystemSheet` is the intended pattern.

### 3.5 History Drawer ("Memory Stream")

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

### 3.6 Lists & Rows

Used in history, audio library, device files, etc.

* Height: 56–72 dp depending on content.
* Layout: leading icon/avatar → main text → metadata → chevron/action.
* Title: `Body` or `SectionTitle` + `TextPrimary`.
* Subtitle: `Caption` + `TextMuted`.
* Bottom divider: 1 dp `BorderDefault` spanning full width or with inset.

### 3.7 Empty States

* Use **SurfaceMuted** or naked `AppBackground` plus an icon or illustration when the owning feature does not require a shell-specific empty state.
* Include:

  * Title or short message (Body/SectionTitle).
  * Optional short tip in `Caption` + `TextMuted`.
* For SIM home/here specifically, prefer the greeting-first canvas owned by the shell over generic empty-state iconography.
* No heavy colors; keep it soft and informative.

### 3.8 Smart Badge (Header Status)

**Concept**: A specialized connectivity/status badge that may appear on supporting or non-SIM shells.

* **Container**:
  * Shape: Pill / Capsule (`RoundedCornerShape(14.dp)`).
  * Background: `SurfaceVariant` (low alpha) or transparent with border.
  * Border: `BorderDefault` (subtle).
* **Content**:
  * Icon: Badge/ID-card icon (16dp).
  * Text: "智能工牌" (Smart Badge).
  * **Pulse Indicator**: A 6dp Green Circle (`#4CD964`).
    * Animation: `alpha` oscillates 0.4 -> 1.0 (authentic pulse).

Current SIM shell rule:

* Do not assume this badge is part of the default active SIM top bar.
* The SIM shell keeps the active home/here header visually balanced with hamburger, centered Dynamic Island, and new-session `+`, while connectivity entry belongs to support-surface routes.

### 3.9 Dynamic Island (Premium Context Pill)

**Concept**: A borderless, highly-integrated context indicator sitting centered in the heavy top monolith.

* **Container**: Completely borderless and transparent container. No bounding pill box. Floats natively in the monolith background.
* **Content**:
  * **Indicator Dot**: A 6dp glossy/glowing circle.
  * **Text / Chroma Font**: Text uses a hardware-like iridescent gradient fill (`linear-gradient` clipped to text).
* **States**:
  * **Upcoming**: Red chroma gradient (`#FF8A84` to `#FF453A`) with a glowing red dot.
  * **Conflict**: Yellow chroma gradient (`#FFEB85` to `#FFD60A`) with a pulsing yellow dot.
  * **Idle**: Silver chroma gradient (`#FFFFFF` to `#A0A0A5`) with a subtle white dot.
* **Prohibition**: Never use emoji. The island must look like glass-integrated hardware without cheap visual backgrounds.

### 3.10 Aurora Background (V16)

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

### 3.11 Knot FAB & Tip Bubble (Historical / Deferred)

This section is retained only as historical design reference.

Rule:

- `Knot FAB` is not part of the current home empty-state contract.
- Do not add it to the active home shell unless an owning feature spec explicitly restores it.
- If reintroduced later, its owning feature shard must define where it lives and when it appears.

### 3.12 Input Placeholder Shimmer (V17)

**Concept**: A subtle "text scan" effect on the placeholder text ("输入消息...") that suggests the input is ready and attentive.

* **Target**: Placeholder text ONLY, not the entire input bar container.
* **Implementation**: Animated `Brush.horizontalGradient` applied via `TextStyle(brush = shimmerBrush)`.
* **Colors**:
  * Base: `onSurfaceVariant.copy(alpha = 0.4f)`
  * Shine: `onSurfaceVariant.copy(alpha = 1.0f)` (bright white peak)
* **Animation**: `shimmerOffset` animates from `-200f` to `600f` over 2 seconds, with 500ms delay between cycles.
* **Gradient Width**: `150f` pixels for the shine band.

---

## 4. Motion & Interaction

Visual guidance only; gesture behavior is in `docs/specs/prism-ui-ux-contract.md` and the owning shell specs.

### 4.1 Animation

* Use **subtle** animations:

  * Crossfade between hero and chat list.
  * Slide/fade for drawers and overlays.
* Durations:

  * Small transitions: 150–220 ms.
  * Drawer open/close: 220–260 ms.
* Easing:

  * Standard Material 3 curves (`FastOutSlowIn`, etc.).

### 4.2 Touch Targets

* Minimum target size: **48 × 48 dp**.
* Padding around icons: at least 8 dp.
* Keep destructive actions separated (spacing or grouping) from primary actions.

---

## 5. Accessibility & Contrast

* Make sure text on `SurfaceCard`/`SurfaceMuted` has enough contrast:

  * `TextPrimary` / `TextSecondary` on white/gray.
  * `TextMuted` only for less important info.
* Primary blue on white meets contrast for buttons; avoid blue on gray with low contrast.
* Do not rely on color alone; use labels and icons.

---

## 6. Usage Checklist

Before shipping a screen, verify:

1. **Doc precedence**

   * Behavior / layout matches `docs/specs/prism-ui-ux-contract.md` plus the owning shared feature doc such as `docs/cerb-ui/home-shell/spec.md`, `docs/cerb-ui/dynamic-island/spec.md`, or the relevant `docs/core-flow/**` doc.
   * This style guide has been used **only** for visuals.
   * Approved state screenshots were applied only to the exact state they depict, then extended through the owning feature spec for the rest of the state family.

2. **Colors**

   * Background = `AppBackground`.
   * Cards / bubbles / drawers = `SurfaceCard`.
   * Primary keys/actions may use `AccentPrimary`, but conversational outgoing bubbles may use a dedicated SIM discussion accent.
   * Errors = `DangerText` on `DangerSurface`.

3. **Typography**

   * AppTitle/Hero/Section/Body/Caption roles used consistently.
   * No arbitrary font sizes.

4. **Spacing & layout**

   * 16 dp base padding on the sides unless a shell-owned monolith or system sheet intentionally spans edge-to-edge.
   * 4-pt grid respected for gaps.
   * Only one main scrollable column.

5. **Shapes & elevation**

   * Utility cards and drawers keep soft radii (12–20 dp).
   * SIM top/bottom monoliths may stay rectangular/heavy with stronger shadow than utility cards.

6. **Component patterns**

   * Home hero only appears in truly empty sessions, and never as a chat bubble.
   * SIM home/here screen is treated as one shell family spanning empty home, active chat, audio-grounded chat, and support-surface entry.
   * Dialogs, drawers, lists, system sheets, and buttons reuse the patterns from this guide.

If any conflict is found between this file and `docs/specs/prism-ui-ux-contract.md` or an owning feature shard, sync this file to the approved feature/UI contract rather than inferring a new rule from implementation drift.
