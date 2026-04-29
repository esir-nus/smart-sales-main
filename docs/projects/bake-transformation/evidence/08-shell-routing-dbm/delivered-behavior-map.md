# Delivered Behavior Map: Shell Routing

Sprint: `08-shell-routing-dbm`  
Date: 2026-04-29  
Evidence class: static DBM from docs, Kotlin source, and JVM tests. No fresh `adb logcat` was captured in this sprint, so runtime/L3 behavior is marked as Historical reference, Gap, or Unknown unless it is proven by current source/test evidence.

## Evidence Commands

- `sed -n '1,260p' docs/projects/bake-transformation/sprints/08-shell-routing-dbm.md`
- `sed -n '1,220p' docs/projects/bake-transformation/tracker.md`
- `sed -n '1,220p' docs/plans/tracker.md`
- `sed -n '1,260p' docs/core-flow/sim-shell-routing-flow.md`
- `sed -n '1,520p' docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `sed -n '1,240p' docs/specs/base-runtime-unification.md`
- `sed -n '1,260p' docs/cerb-ui/home-shell/spec.md`
- `sed -n '1,260p' docs/cerb-ui/dynamic-island/spec.md`
- `sed -n '1,220p' docs/cerb/sim-shell/spec.md`
- `sed -n '1,320p' docs/cerb/interface-map.md`
- `sed -n '1,130p' docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md`
- `sed -n '1,100p' docs/archive/reports/tests/L3-20260319-sim-wave1-shell-acceptance.md`
- `rg --files | rg 'MainActivity.kt$|SimMainActivityLogicTest.kt$|RuntimeShell|SimShell.*Test.kt$|SimConnectivityRoutingTest.kt$|SimSettingsRoutingTest.kt$|SimRuntimeIsolationTest.kt$|DynamicIslandStateMapperTest.kt$|HistoryDrawerStructureTest.kt$|BaseRuntimeShellCoreStructureTest.kt$'`
- `sed -n '1,240p' app-core/src/main/java/com/smartsales/prism/MainActivity.kt`
- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`
- `sed -n '1,620p' app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellState.kt`
- `sed -n '1,280p' app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`
- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/ui/shell/BaseRuntimeShellCore.kt`
- `sed -n '1,640p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt`
- `sed -n '1,240p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`
- `sed -n '1,260p' app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`
- `sed -n '1,220p' app-core/src/test/java/com/smartsales/prism/SimMainActivityLogicTest.kt`
- `sed -n '1,260p' app-core/src/test/java/com/smartsales/prism/ui/BaseRuntimeShellCoreStructureTest.kt`
- `sed -n '1,260p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimConnectivityRoutingTest.kt`
- `sed -n '1,260p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimSettingsRoutingTest.kt`
- `sed -n '1,320p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimRuntimeIsolationTest.kt`
- `sed -n '1,340p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellStructureTest.kt`
- `sed -n '1,260p' app-core/src/test/java/com/smartsales/prism/ui/HistoryDrawerStructureTest.kt`
- `sed -n '1,860p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`
- `sed -n '1,760p' app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinatorTest.kt`
- `sed -n '1,260p' app-core/src/test/java/com/smartsales/prism/ui/components/DynamicIslandStateMapperTest.kt`
- `rg -n "shell-routing|sim-shell-routing|RuntimeShell|Dynamic Island|connectivity takeover|Audio Reselect|Ask AI|follow-up" docs/cerb docs/cerb-ui docs/core-flow docs/specs docs/archive/reports/tests`

Path correction discovered during evidence gathering: the current production root is `app-core/src/main/java/com/smartsales/prism/MainActivity.kt`. The handoff name `app-core/src/main/java/com/smartsales/prism/ui/MainActivity.kt` does not exist.

## Source Set

Primary docs: `docs/core-flow/sim-shell-routing-flow.md`, `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, `docs/specs/base-runtime-unification.md`, `docs/cerb-ui/home-shell/spec.md`, `docs/cerb-ui/dynamic-island/spec.md`, `docs/cerb/interface-map.md`.

Historical references: `docs/cerb/sim-shell/spec.md`, `docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md`, `docs/archive/reports/tests/L3-20260319-sim-wave1-shell-acceptance.md`.

Current code and tests: `app-core/src/main/java/com/smartsales/prism/MainActivity.kt`, `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellState.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/main/java/com/smartsales/prism/ui/shell/BaseRuntimeShellCore.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`, `app-core/src/test/java/com/smartsales/prism/SimMainActivityLogicTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/BaseRuntimeShellCoreStructureTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimRuntimeIsolationTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellStructureTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimConnectivityRoutingTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSettingsRoutingTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinatorTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/components/DynamicIslandStateMapperTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/HistoryDrawerStructureTest.kt`.

## Launch

Delivered behavior: production launch mounts `MainActivity`, which injects `BadgeAudioPipeline`, onboarding/discoverability gates, theme state, and then renders `RuntimeShell(...)` inside `PrismTheme` and `PrismSystemBarsEffect`. This is source-backed in `app-core/src/main/java/com/smartsales/prism/MainActivity.kt` and structurally guarded by `app-core/src/test/java/com/smartsales/prism/ui/sim/SimRuntimeIsolationTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` requires standalone base-runtime shell launch without silently booting the old smart shell. `docs/specs/base-runtime-unification.md` says production root unification moved to `MainActivity -> RuntimeShell`.

Gap: none for the static root ownership claim. Current fresh runtime proof was not captured in this sprint.

Historical reference: `docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md` recorded an L3 launch into `com.smartsales.prism/.MainActivity`; `docs/archive/reports/tests/L3-20260319-sim-wave1-shell-acceptance.md` is older split-era proof for `SimMainActivity` and is no longer current authority.

Unknown: current installed-device launch state on 2026-04-29 because no fresh `adb logcat` or `dumpsys` was collected.

## First-Launch And Onboarding

Delivered behavior: `MainActivity` passes `forceSetupOnLaunch = !onboardingCompleted && debugFollowUpScenario == null` into `RuntimeShell`. `initialRuntimeShellState(forceSetupOnLaunch = true)` opens `RuntimeConnectivitySurface.SETUP` and sets `isForcedFirstLaunchOnboarding = true`. `RuntimeShellContent` renders `OnboardingCoordinator(host = OnboardingHost.SIM_CONNECTIVITY)` with `OnboardingExitPolicy.EXPLICIT_ACTION_ONLY` while forced, then `handleRuntimeConnectivitySetupCompleted` or `handleRuntimeConnectivitySetupSkipped` closes overlays and clears the forced flag. Evidence: `app-core/src/main/java/com/smartsales/prism/MainActivity.kt`, `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, and `app-core/src/test/java/com/smartsales/prism/ui/sim/SimConnectivityRoutingTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` expects fresh install/reinstall to force onboarding setup, then connectivity manager, then ordinary chat. `docs/core-flow/base-runtime-ux-surface-governance-flow.md` states onboarding completion returns to home and may auto-open the real scheduler drawer once.

Gap: the current code's setup completion telemetry says `target=HOME`, and the source returns to home rather than requiring connectivity manager first. This aligns with the UX governance doc but differs from the older shell-routing text that says connectivity manager comes before discussion chat.

Historical reference: `docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md` proved forced first-launch onboarding and post-onboarding scheduler auto-open on device.

Unknown: current runtime onboarding behavior without fresh `adb logcat`.

## RuntimeShell Ownership

Delivered behavior: `RuntimeShell` owns production shell composition and wires `SimAgentViewModel`, `SimSchedulerViewModel`, `SimAudioDrawerViewModel`, `ConnectivityViewModel`, `PairingFlowViewModel`, and `SimBadgeFollowUpOwner`. `RuntimeShellContent` owns rendered shell overlays and passes explicit adjunct state into shared UI. `RuntimeShellState` records active drawer, audio mode, connectivity surface, history/settings flags, scheduler hint flag, and forced-first-launch flag. `RuntimeShellReducer` adapts that state through `BaseRuntimeShellCoreState`. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellState.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/main/java/com/smartsales/prism/ui/shell/BaseRuntimeShellCore.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md`, `docs/specs/base-runtime-unification.md`, and `docs/cerb/interface-map.md` require shared UI to receive explicit shell-owned state rather than recovering legacy smart runtime behavior through wrapper defaults or downcasts.

Gap: no BAKE contract exists yet for shell-routing. This DBM is the delivered map before TCF/BAKE contract work.

Unknown: whether every runtime path avoids legacy wrapper behavior at device runtime; static tests guard key strings but do not prove every dependency graph branch.

## Chat

Delivered behavior: the shell renders `AgentIntelligenceScreen` in SIM visual mode through `RuntimeShellContent`, with `SimAgentViewModel` as the explicit chat/session/audio/follow-up owner. New session clears active badge follow-up before calling `startNewSession`. Session switch/delete clear follow-up only when switching away from or deleting the bound follow-up session. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` permits direct blank/new chat, normal message send/receive without audio precondition, and history/new-session support practices without smart-agent orchestration.

Gap: this sprint did not inspect full `SimAgentViewModel` chat pipeline behavior or run chat tests; this section only maps shell routing ownership.

Unknown: live chat send/receive behavior under current build because no runtime test was run.

## Scheduler Drawer

Delivered behavior: scheduler is always enabled in `RuntimeShell`. Dynamic-island tap or pull opens scheduler through `openRuntimeScheduler`, optionally selecting the target date from a `DynamicIslandTapAction.OpenSchedulerDrawer`. `RuntimeShellContent` renders `SchedulerDrawer` in `SchedulerDrawerVisualMode.SIM`; scheduler-open hides side header utilities and bottom composer and adds a shield/dismiss region. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellStructureTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` and `docs/core-flow/base-runtime-ux-surface-governance-flow.md` require scheduler to open above the same shell family and close back to discussion without losing context.

Gap: static tests verify clear-shell gesture gating and host wiring, but they do not reproduce all pointer conflicts or animation behavior.

Historical reference: `docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md` proved scheduler entry/exit on device and noted direct second-drawer conflict proof remained partial.

## Audio Drawer

Delivered behavior: shell bottom/attach paths call `openAudioDrawer(RuntimeAudioDrawerMode.BROWSE)` or `openAudioDrawer(RuntimeAudioDrawerMode.CHAT_RESELECT)`. `SimAudioDrawer` receives current chat audio ID, connectivity state, manual sync/import hooks, `onAskAi`, and `onSelectForChat`. Ask AI and selection bind the audio to the chat session via `chatViewModel.selectAudioForChat(...)`, `audioViewModel.bindDiscussion(...)`, close the drawer, and reset mode to `BROWSE`. Pending selections are tracked until transcription completes or fails. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md`, `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, and `docs/core-flow/sim-audio-artifact-chat-flow.md` require drawer-based audio reselect and Ask AI handoff into audio-grounded chat without default Android file management.

Gap: no current runtime proof for file-picker non-escape was captured in this sprint.

Historical reference: `docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md` proved audio open, bind, and reselect through the drawer on device.

## History

Delivered behavior: hamburger calls `handleRuntimeHistoryEntryRequest(source = "hamburger")`, emits history route telemetry, and opens history via `openBaseRuntimeHistory`. `RuntimeShellContent` renders `SimHistoryDrawer`, routes session click/pin/rename/delete through `SimAgentViewModel`, and offers settings handoff. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`, `app-core/src/test/java/com/smartsales/prism/ui/HistoryDrawerStructureTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` and `docs/core-flow/base-runtime-ux-surface-governance-flow.md` allow grouped history, rename/delete/pin, and session resume without implying smart memory.

Gap: static evidence covers routing and ownership, not current UI rendering or persistence behavior.

## Settings

Delivered behavior: settings opens from profile/history paths by `openRuntimeSettings`, which clears active drawer, resets audio mode to browse, closes connectivity, and sets `showSettings = true`. `RuntimeShellContent` renders `SimUserCenterDrawer` with deterministic slide/fade animation and scrim. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSettingsRoutingTest.kt`.

Target behavior: `docs/core-flow/base-runtime-ux-surface-governance-flow.md` treats settings as a centered/support surface reachable from lawful entry points, not a second shell root.

Gap: no current runtime proof for settings drawer dismissal or theme behavior in this sprint.

## Connectivity

Delivered behavior: connectivity entry resolves `NEEDS_SETUP` to `MODAL` and configured states to `MANAGER`. Setup and add-device routes open `OnboardingCoordinator` with `SIM_CONNECTIVITY` or `SIM_ADD_DEVICE`. `ConnectivityViewModel.promptRequests` open the modal, and `effectiveState == DISCONNECTED` schedules auto reconnect. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimConnectivityRoutingTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md`, `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, and `docs/cerb/interface-map.md` require connectivity as a support route, with transport truth outside the island renderer and RuntimeShell owning route selection between modal/setup/manager/add-device surfaces.

Gap: code includes `ADD_DEVICE` as a runtime-specific surface mapped to base `SETUP`; `BaseRuntimeShellCore` does not model add-device separately.

Unknown: actual BLE/HTTP state transition behavior requires fresh device/log evidence and is outside this docs-only DBM.

## New Session

Delivered behavior: the header new-session action calls `handleSimNewSessionAction`, clears active follow-up when present, starts a new session, and closes overlays. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` and `docs/core-flow/base-runtime-ux-surface-governance-flow.md` require new page/session to reset discussion without silently opening another product mode.

Gap: one `SimShellHandoffTest` method for new session appears to lack an `@Test` annotation, so the specific unit assertion may not execute even though the helper exists.

## Drawer Exclusivity

Delivered behavior: `BaseRuntimeShellCore` stores one `activeDrawer` and opening scheduler, history, or audio replaces the previous drawer while clearing incompatible surfaces. `RuntimeShellState` also stores one `activeDrawer` for scheduler/audio plus a separate `showHistory` adapter flag. `openRuntimeConnectivity*`, `openRuntimeSettings`, and `closeRuntimeOverlays` clear drawer state. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/shell/BaseRuntimeShellCore.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/test/java/com/smartsales/prism/ui/BaseRuntimeShellCoreStructureTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimConnectivityRoutingTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSettingsRoutingTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` and `docs/core-flow/base-runtime-ux-surface-governance-flow.md` require only one major drawer at a time.

Gap: historical L3 explicitly did not fully reproduce a second-drawer conflict while another drawer was already open.

Unknown: visual overlap under all gesture-race timing without fresh runtime evidence.

## Audio Reselect

Delivered behavior: chat attach opens `RuntimeAudioDrawerMode.CHAT_RESELECT`; `SimAudioDrawer` marks current chat audio and selection binds/switches audio in the current chat session before closing. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md`, `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, and `docs/core-flow/sim-audio-artifact-chat-flow.md` require reselect through the audio drawer, not Android file management.

Historical reference: `docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md` proved this on device during the April 4 acceptance pass.

Unknown: current runtime proof on 2026-04-29.

## Ask AI

Delivered behavior: scheduler inspiration `Ask AI` calls `handleSchedulerShelfAskAiHandoff`, emits telemetry, starts a scheduler shelf session, and closes the drawer. Audio artifact `Ask AI` emits audio route telemetry, selects audio for chat, binds the discussion, and closes the audio drawer. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` requires audio drawer -> simple chat via Ask AI, and `docs/core-flow/base-runtime-ux-surface-governance-flow.md` keeps support content inline without replacing discussion.

Gap: deeper answer quality and scheduler-origin intelligence are outside this shell-routing DBM.

## Badge Scheduler Follow-Up

Delivered behavior: `RuntimeShell` collects `BadgeAudioPipeline.events`, extracts `SchedulerResult.TaskCreated` or non-empty `MultiTaskCreated` completions, creates a badge scheduler follow-up session, and starts `SimBadgeFollowUpOwner`. `RuntimeShellContent` shows `SimSchedulerFollowUpPrompt` only when a follow-up exists, the current session is not the bound session, and no drawer/history/connectivity/settings surface is open. Tapping the prompt switches to the bound follow-up session. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`, `docs/cerb/interface-map.md`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` requires prompt-first badge-origin scheduler follow-up rather than forced chat switching.

Gap: hardware-origin follow-up requires badge runtime evidence; this sprint did not capture fresh `adb logcat`.

Historical reference: `docs/archive/reports/tests/L3-20260322-sim-wave8-follow-up-validation.md` is cited by repo search as older L3 debug-assisted follow-up proof, but this DBM did not deep-read it because the contract's required historical references were the Wave 1 and 20260404 shell reports.

## Dynamic Island Arbitration

Delivered behavior: `RuntimeShell` feeds scheduler items, `ConnectivityViewModel.connectionState`, `batteryLevel`, suppression state, and active device name into `SimShellDynamicIslandCoordinator`. Scheduler is the default lane. `CONNECTED` interrupts for 5s, `DISCONNECTED` for 3s, heartbeats every 30s with 5s or 2.5s dwell, and `RECONNECTING`, `NEEDS_SETUP`, and `PARTIAL_WIFI_DOWN` are persistent connectivity lanes. Suppression applies while scheduler drawer or any connectivity surface is active. Tap action follows visible lane; downward drag is only wired to scheduler open. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinatorTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/components/DynamicIslandStateMapperTest.kt`, `docs/cerb-ui/dynamic-island/spec.md`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` and `docs/cerb-ui/dynamic-island/spec.md` require scheduler default, connectivity interrupt/heartbeat, visible-lane tap, and scheduler-only downward drag.

Gap: code treats `PARTIAL_WIFI_DOWN` as a persistent connectivity island lane, while `docs/core-flow/sim-shell-routing-flow.md` and `docs/cerb-ui/dynamic-island/spec.md` exclude Wi-Fi mismatch/manager-only refinements from takeover. This is a delivered-vs-target drift candidate.

Historical reference: `docs/archive/reports/tests/L3-20260404-single-runtime-shell-acceptance.md` proved needs-setup takeover, visible-lane tap to connectivity, and downward drag to scheduler.

## Connectivity Takeover

Delivered behavior: takeover uses `ConnectivityViewModel.connectionState`, not `effectiveState`, for the island coordinator. Suppression state derives from scheduler drawer or any connectivity-owned surface. Active device name can replace the generic `Badge` label. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt`, `docs/cerb/interface-map.md`.

Target behavior: transport-truth takeover, not manager-only refinements, per `docs/core-flow/sim-shell-routing-flow.md` and `docs/cerb-ui/dynamic-island/spec.md`.

Gap: `PARTIAL_WIFI_DOWN` in delivered code may violate the target exclusion of Wi-Fi mismatch/manager-only states.

Unknown: current hardware transition timing without fresh `adb logcat`.

## Smart-Only Surface Blocking

Delivered behavior: static evidence shows `MainActivity` mounts `RuntimeShell`, and `SimRuntimeIsolationTest` asserts no `AgentShell`, `AgentMainActivity`, `AgentViewModel`, Hilt `AgentViewModel`, or `PrismModule` ownership in the runtime shell. `RuntimeShellContent` passes `showDebugButton = false` to `AgentIntelligenceScreen`. Evidence: `app-core/src/main/java/com/smartsales/prism/MainActivity.kt`, `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimRuntimeIsolationTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md`, `docs/specs/base-runtime-unification.md`, and `docs/cerb/sim-shell/spec.md` require normal base-runtime shell use not to revive smart-only surfaces such as mascot/debug/plugin task board.

Gap: there is no explicit `SMART_SURFACE_BLOCKED` telemetry or route-block test found in this sprint's source reads. Blocking is mostly by absence/static ownership, not a named runtime guard.

Unknown: whether all smart-only routes are unreachable under every debug/deep-link path.

## Telemetry

Delivered behavior: shell route telemetry is emitted through `PipelineValve.Checkpoint.UI_STATE_EMITTED` plus `Log.d` tags for connectivity route, history route, scheduler shelf handoff, badge follow-up ingress, persisted audio artifact open, and audio grounded chat open. Evidence: `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellReducer.kt`, `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`.

Target behavior: `docs/core-flow/sim-shell-routing-flow.md` lists canonical valves such as `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, `CHAT_VISIBLE`, `SCHEDULER_DRAWER_OPENED`, `AUDIO_DRAWER_OPENED`, `SMART_SURFACE_BLOCKED`.

Gap: delivered telemetry does not cover all canonical core-flow valves by name. Notably, no static evidence was found for `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, `CHAT_VISIBLE`, `SCHEDULER_DRAWER_OPENED`, `AUDIO_DRAWER_OPENED`, or `SMART_SURFACE_BLOCKED` as first-class emitted summaries.

Unknown: actual logcat emissions in current build because no fresh `adb logcat` was captured.

## Tests

Delivered behavior: the current static/unit test surface covers root isolation, shared shell core structure, history handoff, settings routing, connectivity routing, shell structure extraction, shell gestures/handoffs, dynamic-island projection/arbitration, and theme logic. Evidence paths include `app-core/src/test/java/com/smartsales/prism/SimMainActivityLogicTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/BaseRuntimeShellCoreStructureTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/HistoryDrawerStructureTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimConnectivityRoutingTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimSettingsRoutingTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimRuntimeIsolationTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellStructureTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinatorTest.kt`, `app-core/src/test/java/com/smartsales/prism/ui/components/DynamicIslandStateMapperTest.kt`.

Target behavior: core-flow expects PU tests for shell routes, drawer transitions, and isolation branches.

Gap: many tests are static source-structure tests, not behavioral Compose/UI tests. Runtime behavior still depends on L3 evidence for UI/device branches.

## Known Gaps

- Gap: first-launch target conflict. `docs/core-flow/sim-shell-routing-flow.md` says forced setup should go through connectivity manager before discussion chat; delivered code and `docs/core-flow/base-runtime-ux-surface-governance-flow.md` close setup to home and optionally auto-open scheduler.
- Gap: dynamic island Wi-Fi state. Delivered `PARTIAL_WIFI_DOWN` persistent lane in `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt` conflicts with target exclusion of Wi-Fi mismatch/manager-only refinements in `docs/core-flow/sim-shell-routing-flow.md` and `docs/cerb-ui/dynamic-island/spec.md`.
- Gap: telemetry coverage. Delivered telemetry does not emit every canonical shell-routing valve from `docs/core-flow/sim-shell-routing-flow.md`.
- Gap: smart-only blocking lacks a named runtime block seam or `SMART_SURFACE_BLOCKED` telemetry; current evidence is absence/static ownership.
- Gap: explicit second-drawer conflict runtime proof remains partial in historical L3 and was not rerun.
- Gap: one new-session helper assertion in `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt` appears to lack `@Test`, reducing automated coverage for that branch.
- Unknown: current device behavior for launch, onboarding, connectivity takeover, drawer overlap, audio file-picker escape, and hardware follow-up because Sprint 08 did not collect fresh `adb logcat`.

## DBM Verdict

Delivered behavior is coherent enough to close Sprint 08 successfully as a docs-only DBM: the shell is currently delivered as a unified `MainActivity -> RuntimeShell` base-runtime host with explicit state ownership, one-active-overlay reducer rules, drawer-based scheduler/audio/history/settings/connectivity routes, prompt-first follow-up, and dynamic-island arbitration. The main transformation targets for the next shell-routing TCF are first-launch wording, Wi-Fi takeover authority, canonical telemetry valves, explicit smart-surface block evidence, and runtime proof gaps.
