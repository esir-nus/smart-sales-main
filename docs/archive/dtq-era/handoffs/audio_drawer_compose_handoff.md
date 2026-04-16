# Handoff: DTQ-04 Runtime Shell and SIM Chrome

> **Lane ID**: `DTQ-04`
> **Registry Lane ID**: `DTQ-04`
> **Branch**: `not yet isolated from the integration tree`
> **Recommended Worktree**: `not yet assigned; create a dedicated DTQ-04 lane worktree before resuming feature edits`
> **Scope**: Runtime shell, SIM chrome, dynamic-island, and audio-drawer presentation lane for the current dirty tree.

## Scope

This handoff owns runtime shell chrome and SIM-only visual/presentation work in the current dirty tree, including the shell host, audio drawer, history/home chrome, dynamic-island presentation, and shell-only routing affordances.

## Owned Paths

- `app-core/src/main/java/com/smartsales/prism/MainActivity.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`
- shell/audio/history/home files under `app-core/src/main/java/com/smartsales/prism/ui/sim/**`, especially `SimAudioDrawer*.kt`, `SimHistoryDrawer.kt`, `SimHomeHeroShell.kt`, `SimSessionTitlePresentation.kt`, `SimHomeHeroTokens.kt`, `SimUserCenterDrawer.kt`, and `SimShellDynamicIslandCoordinator.kt`
- shell/UI tests under `app-core/src/test/**` and `app-core/src/androidTest/**`
- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/audio-management/interface.md`
- `docs/cerb-ui/dynamic-island/spec.md`
- `docs/plans/ui-tracker.md`
- shell/audio prototypes under `prototypes/sim-shell-family/**`

## Current Repo State / Implementation Truth

The current dirty tree already contains live shell and SIM chrome work, not just a design transplant note. This includes runtime shell host behavior, dynamic-island presentation, shell drawer polish, audio drawer UI behavior, and the shell-side consumption of Harmony compat gating plus session-title presentation state.

The detailed Compose translation notes below remain useful historical implementation memory for the audio drawer surface, but current truth lives in the live shell/audio code and the owning docs listed here.

## What Is Finished

- The runtime-shell/SIM-chrome lane already has a bounded write scope in the quarantine tracker.
- The 2026-04-11 governance pass now explicitly assigns `SimAudioRepositorySyncSupport.kt` to this lane so audio-drawer sync presentation no longer sits outside the shell boundary.
- The historical audio drawer transplant notes remain preserved as implementation detail memory.
- This handoff now records lane ID, registry linkage, drift state, and exact owning docs so the next pass can resume cleanly.

## What Is Still Open

- Keep shell/audio/history/home chrome changes aligned with the shell/audio docs and UI tracker.
- Keep the Harmony compat scheduler-disabled shell behavior as a shell consumer of `DTQ-06` registration surfaces rather than silently taking ownership of flavor/build law.
- Keep session-title display and dynamic-island presentation here, but leave parser/session-title generation semantics and persistence contract cleanup with `DTQ-05`.
- Re-check whether any shell work is quietly absorbing scheduler truth or connectivity transport logic; split instead of broadening.
- Do not mark the lane `Accepted` until focused shell/UI verification and doc sync are complete.

## Doc-Code Alignment

- **Owning source-of-truth docs**:
  - `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
  - `docs/core-flow/sim-shell-routing-flow.md`
  - `docs/cerb/audio-management/spec.md`
  - `docs/cerb/audio-management/interface.md`
  - `docs/cerb-ui/dynamic-island/spec.md`
  - `docs/plans/ui-tracker.md`
  - `docs/plans/tracker.md`
- **Current alignment state**: `Both pending`
- **Docs still needing sync or final confirmation before `Accepted`**:
  - `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
  - `docs/core-flow/sim-shell-routing-flow.md`
  - `docs/cerb/audio-management/spec.md`
  - `docs/cerb/audio-management/interface.md`
  - `docs/cerb-ui/dynamic-island/spec.md`
  - `docs/plans/ui-tracker.md`
  - `docs/plans/tracker.md`
- **Rule**: no shell/audio chrome in this lane should be treated as landed while these docs still trail or contradict the implementation.

## Required Evidence / Verification

- Focused shell/UI tests for the touched runtime shell, drawer, dynamic-island, and audio presentation branches.
- Visual/behavior proof for chrome changes that affect user-visible shell state.
- If behavior depends on runtime delivery rather than pure UI composition, capture concrete evidence instead of inferring from screenshots.

## Safe Next Actions

- Continue only inside the shell/audio-owned paths listed above.
- Sync shell/audio docs in the same session as any behavior change.
- Split scheduler truth back to `DTQ-02` and connectivity transport logic back to `DTQ-03` if either starts leaking into this lane.
- Treat `AppFlavor.kt`, `PrismApplication.kt`, and Android/Harmony flavor wiring as `DTQ-06` registration surfaces even when shell behavior consumes them.
- Treat parser/session-title generation and session-history contract changes as `DTQ-05` even when shell surfaces render their results.

## Do Not Touch / Collision Notes

- Do not absorb shared scheduler-routing or reminder-truth ownership from `DTQ-02`.
- Do not absorb connectivity bridge/service ownership from `DTQ-03`.
- Do not use shell chrome as a backdoor owner for governance/control-plane work.
- Do not claim parser/session-title generator removal or session-history contract law from `DTQ-05`.

## Detailed Historical Notes

> **Scope**: Transplant handoff for the full SIM Audio Drawer component, specifically focusing on the new Pull-to-Sync interaction and `audio-card` layouts.

## A. Architecture & Top-Level Context
- **Component Role**: A persistent, bottom-anchored contextual drawer managing recorded audio files (demo, transcribing, downloading). It natively includes a "Pull-to-Sync" mechanic that triggers data polling from the paired hardware badge.
- **Placement**: Renders behind the system's "Bottom Monolith" navigation block (`z-index: 30` vs `z-index: 40`), sliding slightly underneath it to create overlapping depth.
- **Insets/SOT**: Placed physically near the bottom of the screen. Requires padding to account for the overlapping `BottomMonolith` navigation space instead of strict system `WindowInsets.navigationBars`.

## B. Component Decomposition (HTML to Compose)

### 1. `AudioDrawer` (Root Overlay)
- **HTML Element**: `<div class="audio-drawer">`
- **Compose Mapping**: `Box` or `Column` modified with a custom vertical drag tracker (`pointerInput(Unit) { detectVerticalDragGestures(...) }`).
- **Surface Styling**:
  - Background: `Color(0xD9141416)` (mapped from `rgba(20, 20, 22, 0.85)`).
  - Blur Effect: Requires `Modifier.blur(radius = 28.dp)` on background components (using fallback if API < 31).
  - Corners: `RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)`.
  - Border: `Modifier.border(0.5.dp, Color(0x14FFFFFF))` on top/left/right only. 
  - Shadow: Native `Modifier.shadow` or specialized canvas shadow for `0 -20px 50px rgba(0,0,0,0.7)`.

### 2. `DrawerGripArea` & Handle
- **HTML Element**: `<div class="drawer-grip-area">` containing `.drawer-grip` and `.grip-helper-text`.
- **Compose Mapping**: `Box(Modifier.fillMaxWidth().height(32.dp).padding(top = 8.dp))`
- **Default Handle (`.drawer-grip`)**:
  - Size: `36.dp` x `4.dp`, `RoundedCornerShape(2.dp)`
  - Color: `Color(0x33FFFFFF)` (mapped from `rgba(255,255,255,0.2)`)
- **Armed Threshold Handle (`.drawer-grip.pulling-threshold`)**:
  - Transitions to: `44.dp` x `6.dp`
  - Color: `Color(0xFF0A84FF)` (`--color-accent-blue`) with ambient glow shadow.
- **Helper Text (`.grip-helper-text`)**:
  - `Row` with Icon + Text `"松开同步"`. Sizes `14.dp` and `12.sp`.
  - Initial state: `opacity = 0`, `offsetY = 10.dp`. 
  - Armed state: `opacity = 1`, `offsetY = 0.dp`. Base un-armed offset is `top = -24.dp` relative to grip area.

### 3. `DrawerHeaderV2` & Smart Capsule
- **HTML Element**: `<div class="drawer-header-v2">`
- **Compose Mapping**: `Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp))`
- **Title (`.drawer-title`)**: `Text("录音笔记")`. Size: `20.sp`, Weight: `600`, letter spacing `-0.5sp`. Next to it: `Text("3 项")` badge with `Color(0x0FFFFFFF)` background.
- **Smart Capsule`: `Box` representing sync state (Disconnected, Connected, Syncing, Error, Synced).
  - Size: `minWidth = 120.dp`, `height = 32.dp`.
  - Background: `Color(0x0CFFFFFF)`, Border: `0.5.dp`, `Color(0x14FFFFFF)`.
  - Uses `AnimatedContent` for internal icons/text swap depending on sync state.

### 4. `AudioCard` (List Item)
- **HTML Element**: `<div class="audio-card">`
- **Compose Mapping**: `SwipeToDismissBox`. 
- **Delete Area**: Right-side layout containing `TrashIcon` on red background (`Color(0xFFFF453A)`). Exposed by Left Swipe.
- **Card Surface**:
  - Background: `Color(0x0CFFFFFF)` (`rgba(255,255,255,0.05)`).
  - Shape: `RoundedCornerShape(12.dp)`.
  - Border: `0.5.dp`, `Color(0x26FFFFFF)`. Padding: `16.dp`.
- **Text Styling**:
  - Title: `15.sp`, Weight `600`, `Color.White`.
  - Meta/Date: `13.sp`, `Color(0xFFAEAEC2)`.

## C. State Machine & Interaction Logic

### 1. The Pull-to-Sync Engine
- **Backing State**: `var drawerDragOffset by remember { mutableFloatStateOf(0f) }`
- **Drag Mechanics**:
  - Catch downward `deltaY` (moving finger UP screen means negative drag).
  - Apply rubber-band damping: `dragOffset = minOf(dragTotal * 0.35f, 90f)`. Maximum travel distance is `90.dp`.
- **Threshold Arming**:
  - Active if `dragOffset > 55.dp`.
  - Triggers the visual expansion of the handle and fade-in of "松开同步" text.
- **Release Action**:
  - If `isArmed` upon `onDragEnd` -> trigger Sync API calls ([triggerSyncDemo(true)](file:///home/cslh-frank/main_app/prototypes/sim-shell-family/sim_audio_drawer_shell.html#722-746) equivalent) and bounce back to 0.
  - If NOT `isArmed` -> quietly snap back to 0.

### 2. Swipe-To-Delete
- **Constraint**: Audio cards only swipe LEFT to delete (unlike the Inspiration Box prototype which swiped right).

## D. Animation & Motion Choreography
- **Spring Release (`.spring-release`)**:
  - When snapping back to 0 on drag release, use a spring spec mapped from CSS `cubic-bezier(0.34, 1.56, 0.64, 1)`.
  - Compose approximation: `spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)`. It must over-shoot slightly and settle.
- **Smart Capsule Click (`.smart-capsule:active`)**:
  - Click down applies `graphicsLayer { scaleX = 0.96f; scaleY = 0.96f }`.
- **Card Delete Fade**:
  - Card translates past threshold and fades opacity sequentially to `0f`. 

## E. Anti-Drift & Defensive Implementation Rules

1. ⚠️ **Drag Event Isolation (Vertical vs Horizontal)**: Card swiping (Horizontal) and Pull-to-Sync (Vertical) exist in close proximity. You MUST configure `pointerInput` correctly to disambiguate the drag intent. If user drags vertically, Card Swipe event must not consume it.
2. ⚠️ **SwipeToDismiss Background Rule**: When conditionally rendering the red delete background, base visibility on `dismissState.targetValue != SwipeToDismissBoxValue.Settled`, **not** strictly on `dismissDirection`.
3. ⚠️ **Grip Bar Shake Animation**: If sync fails or is disconnected when pulled, play horizontal shake animation (`flash-grip-denied`). This is required to communicate API refusal without toast popups.
