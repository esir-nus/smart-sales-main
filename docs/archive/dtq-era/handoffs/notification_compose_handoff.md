# Handoff: DTQ-02 Scheduler Intelligence and Reminder Surfaces

> **Lane ID**: `DTQ-02`
> **Registry Lane ID**: `DTQ-02`
> **Branch**: `not yet isolated from the integration tree`
> **Recommended Worktree**: `not yet assigned; create a dedicated DTQ-02 lane worktree before resuming feature edits`
> **Scope**: Shared scheduler-routing, reminder-surface, banner/alarm, and scheduler-drawer lane for the current dirty tree.

## Scope

This handoff covers scheduler intelligence and reminder delivery surfaces, including shared routing through the scheduler stack, alarm activity behavior, in-app reminder/banner presentation, and scheduler-facing reminder guidance.

## Owned Paths

- `app-core/src/main/java/com/smartsales/prism/data/scheduler/TaskReminderReceiver.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/alarm/**`
- scheduler/reminder surfaces such as `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/**`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimScheduler*.kt`, and `app-core/src/main/java/com/smartsales/prism/ui/sim/SimReminderBanner*.kt`
- `app-core/src/main/java/com/smartsales/prism/data/memory/RealScheduleBoard.kt`
- scheduler domain timing/reschedule files under `domain/scheduler/**`
- scheduler retrieval fake support: `core/test-fakes-domain/src/main/java/com/smartsales/core/test/fakes/FakeActiveTaskRetrievalIndex.kt`
- reminder/scheduler tests under `app-core/src/test/**`, `app-core/src/androidTest/**`, and `domain/scheduler/src/test/**`
- `docs/cerb/notifications/spec.md`
- scheduler-routing and scheduler-surface docs under `docs/cerb/scheduler-path-a-*/**`, `docs/cerb/sim-scheduler/**`, `docs/cerb-ui/scheduler/contract.md`, `docs/core-flow/scheduler-fast-track-flow.md`, and `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/plans/sim-tracker.md`
- reminder/scheduler prototype assets under `prototypes/sim-shell-family/**`

## Current Repo State / Implementation Truth

The current dirty tree already contains real shared scheduler-contract and reminder-surface work. This is no longer just a notification UI translation note; it now spans alarm delivery, reminder banners, retrieval-board truth, and the tightened explicit-target reschedule contract.

The tightened contract now requires an explicit target plus a new exact time in the current utterance, resolves globally across active exact tasks, and safe-fails on omitted target, ambiguity, no match, and non-exact time. The detailed UI notes below remain useful implementation memory for the alarm/banner surfaces, but current repo truth lives in the active code paths and the owning scheduler/notification docs listed here.

## What Is Finished

- The lane is isolated in the quarantine tracker with a stable reminder/scheduler seam.
- The 2026-04-11 governance pass now explicitly assigns the tightened scheduler core-flow/spec docs and retrieval-board support files to this lane instead of leaving them ungoverned.
- Historical UI translation notes remain preserved for the alarm/banner surfaces.
- This handoff now carries explicit source-of-truth and drift state so the next pass can resume without re-auditing from scratch.
- DTQ-02 audit on 2026-04-04 found no scheduler/reminder-specific contradiction between the shared scheduler docs, reminder/banner docs, and the live `TaskReminderReceiver` / `AlarmActivity` / `SimSchedulerViewModel` code plus the scheduler-owned retrieval/domain seams.
- Focused DTQ-02 verification passed in this audit session:
  - `./gradlew :core:pipeline:testDebugUnitTest --tests 'com.smartsales.core.pipeline.SchedulerIntelligenceRouterTest' --tests 'com.smartsales.core.pipeline.IntentOrchestratorTest'`
  - `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.ui.sim.SimSchedulerViewModelTest' --tests 'com.smartsales.prism.ui.sim.SimReminderBannerSupportTest' --tests 'com.smartsales.prism.ui.alarm.AlarmActivityStateTest'`

## What Is Still Open

- Finalize code/doc alignment for the shared scheduler contract and reminder presentation.
- Keep the explicit-target reschedule rule honest across docs, retrieval support, and downstream pipeline execution; do not reintroduce newest-task bias, selected-task authority, or delta-only mutation.
- Keep the lane focused on scheduler/reminder behavior; if changes broaden into onboarding flow ownership or connectivity transport, split the work.
- Do not mark this lane `Accepted` yet while the dirty scheduler/reminder lane is still unlanded and this audit session does not include a fresh runtime/device rerun for reminder delivery and alarm presentation.

## Doc-Code Alignment

- **Owning source-of-truth docs**:
  - `docs/cerb/notifications/spec.md`
  - `docs/cerb-ui/scheduler/contract.md`
  - `docs/cerb/scheduler-path-a-spine/interface.md`
  - `docs/cerb/scheduler-path-a-uni-a/spec.md`
  - `docs/cerb/scheduler-path-a-uni-b/spec.md`
  - `docs/cerb/sim-scheduler/spec.md`
  - `docs/core-flow/scheduler-fast-track-flow.md`
  - `docs/core-flow/sim-scheduler-path-a-flow.md`
  - `docs/plans/sim-tracker.md`
  - `docs/plans/tracker.md`
- **Current alignment state**: `Both pending`
- **Docs still needing sync or final confirmation before `Accepted`**:
  - no scheduler/reminder doc/code contradiction was found in the 2026-04-04 audit across the shared routing and reminder surfaces
  - hold this lane at `Both pending` until fresh runtime/device evidence closes the remaining reminder-delivery and alarm-presentation verification gap for the still-dirty scope
- **Rule**: no scheduler/reminder behavior in this lane should be treated as landed while runtime verification for the current dirty scope is still incomplete.

## Required Evidence / Verification

- Focused scheduler-contract and reminder verification, including the affected `domain/scheduler`, retrieval-support, and `app-core` test surfaces.
- Current audit evidence:
  - `SchedulerIntelligenceRouterTest`
  - `IntentOrchestratorTest`
  - `SimSchedulerViewModelTest`
  - `SimReminderBannerSupportTest`
  - `AlarmActivityStateTest`
  - scheduler routing audit reference: `docs/reports/scheduler_intelligence_routing_audit.md`
- Alarm/banner behavior proof when `AlarmActivity`, `TaskReminderReceiver`, or scheduler-facing warning copy changes.
- Remaining gap: no fresh runtime/device rerun was executed in this session for EARLY banner delivery, DEADLINE alarm presentation, or Android/OEM reminder delivery behavior.
- If runtime delivery behavior is disputed, capture runtime evidence instead of inferring from UI state alone.

## Safe Next Actions

- Continue only inside the scheduler/reminder-owned paths listed above.
- Keep shared scheduler-contract changes synced with the owning core-flow/spec docs in the same session.
- Split onboarding-specific staged-flow behavior back to `DTQ-01` and OEM-specific delivery hardening back to `DTQ-03`.

## Do Not Touch / Collision Notes

- Do not claim onboarding flow ownership from `DTQ-01`.
- Do not absorb connectivity bridge/service or OEM settings ownership from `DTQ-03`.
- Do not treat runtime shell chrome as scheduler-owned implementation territory unless the change is strictly reminder/scheduler presentation.
- Do not claim the parser/session-title cleanup bundle from `DTQ-05`; pipeline-hosted consumers may depend on the scheduler contract, but the parser/session-history ownership split is separate.

## Detailed Historical Notes

This document defines the strict translation of the `sim_notification_shell.html` prototype into deterministic Jetpack Compose engineering blueprints. It establishes the exact architecture, composition layout, state bindings, and motion choreographies to ensure zero visual drift.

## A. Architecture & Top-Level Context

- **Component Role**: The Notification UI system consists of two distinct Compose surfaces:
  1.  **Full-Screen Alarm Overlay (`AlarmScreen` / `AlarmActivity`)**: A system-level lockscreen/fullscreen activity triggered at `0m` deadlines.
  2.  **In-App Early Warning Banner (`InAppNotificationBanner`)**: A transient top dropdown overlay within the standard app shell for `-15m` and `-5m` warnings.
  *Note: The Notification Shade Tray from the prototype defines the visual contract for the `NotificationCompat.Builder` OS-level alerts, this handoff focuses on the in-app Jetpack Compose surfaces.*
- **Placement**:
  - `AlarmScreen`: Fills the entire display, rendering over the `window` natively. It relies on `WindowCompat.setDecorFitsSystemWindows(window, false)` for proper edge-to-edge rendering.
  - `InAppNotificationBanner`: Lives inside the main `Scaffold` hierarchy or a top-level `Box` floating above the `TopAppBar`.
- **Insets/SOT**:
  - `AlarmScreen`: Must consume `Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)` or handle manual safe-drawing to prevent the card stack from hiding behind the gesture bar.

---

## B. Component Decomposition (HTML to Compose)

### 1. Full-Screen Alarm Overlay (`alarm-overlay`)

- **HTML Source**: `<div class="alarm-overlay">`
- **Compose Mapping**: `Box(modifier = Modifier.fillMaxSize().background(Color(0xEB0A0A0C)))` 
  - *Hex mapped from `rgba(10, 10, 12, 0.92)`*
- **Layout**: The inner layout is a `Column` with centered horizontal alignment for the header area and a `LazyColumn` or scrollable `Column` for the alarm cards.
- **Glass Card Item (`.glass-alarm-card`)**:
  - **Mapping**: `Card` or a rounded `Box`.
  - **Spacing**: `padding(20.dp)` inner padding, `Arrangement.spacedBy(12.dp)` for the stack.
  - **Surface**: Background is `Color(0x0DFFFFFF)`. Border is `BorderStroke(0.5.dp, Color(0x26FFFFFF))`. Corner radius is `24.dp`.
  - **Critical Accent**: Implement the left red accent rail (`::before` in CSS) using `Modifier.drawBehind` or an explicit `Box` of `width 4.dp`, `background = Color(0xFFFF453A)` with a `blur(10.dp)` shadow.
- **Stacked Secondary Card (`.stacked-secondary`)**:
  - Requires depth projection `graphicsLayer { scaleX = 0.96f; translationY = -8.dp.toPx() }` and `alpha = 0.7f`.
- **Pulsing Alarm Icon (`.alarm-vibe-icon` & `.pulse-ring`)**:
  - **Icon Container**: `Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0x1AFF453A)))`
  - **Pulse Ring**: Separated bordered `Box` animated by an infinite transition.

### 2. In-App Warning Banner (`.in-app-banner-container`)

- **HTML Source**: `<div class="in-app-banner-container">`
- **Compose Mapping**: `AnimatedVisibility(enter = slideInVertically(...), exit = slideOutVertically(...))` Wrapping a `Row`.
- **Layout**: `padding(horizontal = 16.dp, vertical = 12.dp)`.
- **Surface**: `Color(0xD9141416)` with `Blur(28.dp)` modifier applied to background if supported, otherwise a solid approximation. Corner radius `20.dp`. Border `Color(0x1AFFFFFF)`.
- **Icon Background (.banner-icon-bg)**: `Box(size = 36.dp, clip = CircleShape, background = Color(0x260A84FF))` for Blue(-15m) or `Color(0x26FF9F0A)` for Orange(-5m).
- **Typography**: 
  - `banner-title`: `15.sp`, `FontWeight.W600`, `Color.White`.
  - `banner-desc`: `13.sp`, `FontWeight.Normal`, `Color(0xFF86868B)`.

---

## C. State Machine & Interaction Logic

### Alarm Activity State (`AlarmUiState`)
- **State Model**: 
  - `isStacked: Boolean`: Determines if `alarm-badge-count` and secondary cards are rendered.
  - `alarms: List<AlarmEntity>`: Feeds the UI.
- **Interactions**:
  - **Dismiss Action**: Tapping the "知道了" button dismisses the *top* alarm card. This triggers a removal index. If `alarms.size` drops to 1, the `isStacked` state visually diffuses back to the single layout.
  - **Card Dismissal**: Swipe-to-dismiss can be implemented via `SwipeToDismissBox`, updating the active alarm list dynamically.

### In-App Banner State
- **State Model**:
  - `val activeBanner: BannerEvent?`: Nullable state. When non-null, `AnimatedVisibility` reveals.
- **Interactions**:
  - **Auto-Dismiss**: A `LaunchedEffect(activeBanner)` should run a `delay(5000L)` and then `activeBanner = null` to retract.

---

## D. Animation & Motion Choreography

1. **Bell Icon Shake (`shake-bell`)**:
   - Use `rememberInfiniteTransition()`. Animate `rotationZ` from `-15f` to `15f` with `tween(500, easing = LinearEasing)` and `RepeatMode.Reverse`.
2. **Bell Pulse Ring (`pulse-ring-anim`)**:
   - `rememberInfiniteTransition()`. 
   - Animate Scale: `1f` to `1.8f` over `2000ms`, `cubic-bezier(0.2, 0.8, 0.2, 1)`.
   - Animate Alpha: `0.6f` to `0.0f` concurrently.
3. **Banner Slide-In**:
   - Use exactly `slideInVertically(initialOffsetY = { -it }, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(tween(300))`. 

---

## E. Anti-Drift & Defensive Implementation Rules

- ⚠️ **Hardware Key Passthrough in AlarmScreen**: The full screen alarm may be launched on top of the lockscreen. Ensure you use `WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED` and correctly consume the Back key press (via `BackHandler`) to prevent the user from accidentally escaping the 0m alarm without explicitly dismissing it.
- ⚠️ **Scrim Priority vs Layout**: Implement the `AlarmScreen` background tint explicitly separate from the animated cards. If an `AnimatedVisibility` block wraps the whole screen, ensure the *backdrop* fades in (`fadeIn(500)`), but the *cards* slide or zoom in.
- ⚠️ **No Raw Alpha Math**: Map `.glass-alarm-card` alpha strictly to `Color(0x0DFFFFFF)`. Do not use `Color.White.copy(alpha=...)` inline inside `@Composable` drawing routines to limit recomposition overhead.
- ⚠️ **Multiple Overlays Depth**: If the banner renders inside the main app shell, it MUST draw on top of everything except the `SchedulerDrawer` and `AudioDrawer`. Define an explicit top-level `Z-Index` (`Modifier.zIndex(100f)`).
