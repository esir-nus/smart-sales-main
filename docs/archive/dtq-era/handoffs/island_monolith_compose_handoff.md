# UI Transplant Handoff: Dynamic Island & Ambient Icons

## 🎯 Objective
Translate the finalized web prototype for the `sim_home_hero_shell` Top Monolith, Dynamic Island, and new Ambient Flanking Icons into Jetpack Compose. This handoff bridges the implementation gap, specifically addressing the recent visual drift detected between the prototype and the actual `com.smartsales.prism.ui.sim.SimHomeHeroShell.kt` code.

## 🖼 Reference Prototype States
````carousel
![Default Upcoming State](/home/cslh-frank/.gemini/antigravity/brain/7140bca9-beb7-469b-b7d3-e1129ff1b81b/state1_default_upcoming_task_1775122065941.png)
<!-- slide -->
![Connected State (Breathing Ambient Icons)](/home/cslh-frank/.gemini/antigravity/brain/7140bca9-beb7-469b-b7d3-e1129ff1b81b/state2_connected_ambient_icons_visible_1775122190657.png)
<!-- slide -->
![Idle/Hidden State](/home/cslh-frank/.gemini/antigravity/brain/7140bca9-beb7-469b-b7d3-e1129ff1b81b/state3_idle_hidden_icons_gone_1775122212156.png)
<!-- slide -->
![Disconnected State](/home/cslh-frank/.gemini/antigravity/brain/7140bca9-beb7-469b-b7d3-e1129ff1b81b/state4_disconnected_status_visible_1775122226567.png)
````

---

## 🛑 Identified Drift in Current `DynamicIsland.kt`
The current `DynamicIsland.kt` has drifted significantly from the prototype's "borderless pure chroma" UI contract. The following elements MUST be ripped out or updated:

1.  **Remove `PrismSurface` & Solid Backgrounds**:
    *   **Current Issue**: `DynamicIsland` uses `PrismSurface` with a solid background and `.border()`.
    *   **Correction**: The island is meant to be completely transparent/frameless in the web prototype (`background: transparent; box-shadow: none; backdrop-filter: none;`). Remove `PrismSurface` wrapper.
2.  **Remove Battery indicator from INSIDE the Island**:
    *   **Current Issue**: `DynamicIsland` component defines a `DynamicIslandBatteryIndicator` inside the text container.
    *   **Correction**: The battery indicator has been moved OUTSIDE the island to act as the right-side ambient flanking icon. The `showsBattery` logic should be removed from the core island item.
3.  **Correct Island Dot Alignment**:
    *   **Current Issue**: Layout is `Icon` -> `Text`.
    *   **Correction**: Layout should be `Dot (Canvas, 6x6 max)` -> `Spacer(8.dp)` -> `Gradient Text`.

---

## 🏗 Top-Level Compose Mapping (`SimHomeHeroTopCap`)

The monolith itself acts as the anchor. It contains three layout slots:
*   **Left Slot**: Menu Button + New Left Flank Icon (absolute offset)
*   **Center Slot**: Dynamic Island (centered in remaining space)
*   **Right Slot**: Add Session Button + New Right Flank Icon (absolute offset)

### Ambient Flanking Icons Logic
Two new icons flank the monolith.
*   **Visibility**: They are ONLY visible when `dynamicIslandState` represents `DynamicIslandVisualState.CONNECTIVITY_CONNECTED`.
*   **Alignment**: Horizontally placed at an absolute offset `left = 72.dp` and `right = 72.dp` from the edges of the parent container, and vertically centered (`alignment = Alignment.CenterVertically`).
*   **Animation Contract**:
    1.  **Fade In/Out**: `0.4s cubic-bezier(0.2, 0.8, 0.2, 1)` opacity transition bound to the visibility state. Note: When disappearing (invisible), remove the transition delay so it fades out immediately (`transition-delay: 0s`).
    2.  **Continuous Breathing**: When visible, both icons pulse continuously using a 3-second `infiniteRepeatable` keyframe:
        *   `0% / 100%`: scale(0.95f), alpha(0.35f)
        *   `50%`: scale(1f), alpha(0.7f)
    *   **Staggering**: The left (Bluetooth/Chain) icon has a `0.1s` transition delay on entry to stagger its appearance slightly behind the right icon.

### 📐 Dimension & Styling Specs

#### Left Flanking Icon (Link/Chain)
*   **Size**: `16.dp` x `16.dp` Icon Canvas.
*   **Color**: `#34C759` (Connected Green mapping to `SimHomeHeroTokens.IslandConnectedColor`).
*   **Icon Data**: Chain/Link metaphor (Material `Icons.Filled.Link` or custom path).
*   **Position**: `padding(start = 72.dp)`. Vertical center via modifier.

#### Right Flanking Icon (Battery shell)
*   **Construction**: A custom drawn battery glyph.
*   **Core Size**: `18.dp` width, `10.dp` height.
*   **Container Border**: `1.5dp` solid `#34C759`, radius `2.dp`.
*   **Inner Fill Track**: A gradient or solid fill covering ~78% of the width (leaving empty space indicating battery level, though currently static for ambient use). Fill color `#34C759`.
*   **Battery Nub (Right side)**: `2.dp` width, `5.dp` height. Positioned `offset(x = 18.dp)` touching the right border. Radius `0.dp, 1.dp, 1.dp, 0.dp`. Vertical center.

---

## 🎨 Island Redesign Mapping (`DynamicIsland.kt`)

### Core Structure
```kotlin
Row(
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(14.dp, 6.dp) // Wait! Prototype had padding removed or kept minimal inside the text.
) {
    // 1. Island Dot
    Canvas(Modifier.size(6.dp)) {
       // Draw filled dot according to state chroma (e.g. #FF453A for upcoming)
    }
    
    Spacer(Modifier.width(8.dp))
    
    // 2. Island Text 
    Text(
        text = state.item.displayText,
        // Apply textGradient via Brush.linearGradient() based on chroma
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    )
}
```

### Motion Choreography Updates
*   **Island Heartbeat Pulse**: The connected and reconnecting states use an inner pulse on the dot layer. This logic already exists in `SimHomeHeroShell.kt` (`SimHomeHeroIslandChroma`) but should be strictly guarded.
*   **State Transition**: When the island `text` changes, use `AnimatedContent` for smooth crossfade, avoiding layout snapping.

---

## 🛡 Defensive Implementation Rules
1.  **Do NOT use `.clickable()` on ambient icons**: They are purely decorative and sit beneath the transparent touch layer using `pointerEvents: none` in web CSS. Do not attach interaction sources to them.
2.  **Hardcoded Offset Warning**: The `72.dp` absolute offsets for the ambient flanking icons are critical to avoid colliding with the hamburger and `+` buttons. Ensure they are placed using absolute offsets (`Modifier.absoluteOffset` or `Box` alignment overlapping the main row), not packed in the `Arrangement.SpaceBetween` row.
3.  **Color Safety**: In `DynamicIsland.kt`, the gradient text uses `Brush.linearGradient()`. Ensure the colors are extracted directly from the existing `SimHomeHeroIslandChroma` definitions (e.g., `#A4E38A` to `#34C759` for Connected).

---
*Generated by Antigravity via `/ui-transplant-handoff`*
