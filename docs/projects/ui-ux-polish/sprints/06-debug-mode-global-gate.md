# Sprint 06 - debug-mode-global-gate

## Header

- Project: ui-ux-polish
- Sprint: 06
- Slug: debug-mode-global-gate
- Date authored: 2026-04-29
- Author: Codex
- Operator: Codex
- Lane: `develop`
- Branch: `develop`

## Demand

Use the existing `调试模式` toggle as the single app-level gate for debug UI. When the toggle is off, debug controls on the main shell and support drawers must be hidden. When the toggle is on, debug controls may appear on all intended debug surfaces, and the app should show a grey overlay indicating debug mode is active.

## Scope

Files in scope:

- `app-core/src/main/java/com/smartsales/prism/ui/RuntimeShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/debug/DebugModeSurfaceStructureTest.kt`
- `docs/projects/ui-ux-polish/tracker.md`
- `docs/projects/ui-ux-polish/sprints/06-debug-mode-global-gate.md`
- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`

Out of scope:

- Release-build debug controls.
- Changing debug actions or production behavior behind the controls.
- Reworking settings placement for the existing `调试模式` toggle.
- Harmony lane work.

## Contract

- `DebugModeStore.enabled` remains the persisted source for app debug mode.
- Debug controls render only when both `BuildConfig.DEBUG` and `DebugModeStore.enabled` are true.
- The gate covers the main shell debug button, audio drawer test-import action, scheduler drawer test recording/transcript controls, and existing connectivity debug probes.
- When debug mode is enabled, a non-blocking grey overlay covers the runtime shell to visually indicate debug mode.
- When debug mode is disabled, the app returns to normal visual treatment and debug controls are hidden.

## Verification Plan

- Focused source/unit checks:

```bash
./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.ui.debug.DebugModeSurfaceStructureTest' --tests 'com.smartsales.prism.ui.debug.DebugModeStoreTest'
```

- Build check:

```bash
./gradlew :app-core:assembleDebug
```

- Runtime check when a device is available:

```bash
adb devices
adb logcat -c
```

Open User Center, toggle `调试模式` off and on, and capture UI XML/screenshots proving:

- off: no main debug button, no audio test import, no scheduler debug panel, no connectivity debug probes, no grey overlay
- on: those debug surfaces are visible where applicable and the grey overlay is present

## Iteration Ledger

### Iteration 1 - 2026-04-29

- Wired `ConnectivityViewModel.debugModeEnabled` through `RuntimeShell` and legacy `SimShell` into `RuntimeShellContent`.
- Gated main-shell debug button, audio drawer test import, and scheduler drawer debug controls behind `BuildConfig.DEBUG && debugModeEnabled`.
- Preserved existing connectivity modal gate, which already uses `BuildConfig.DEBUG && debugModeEnabled`.
- Added a non-blocking grey runtime-shell overlay while debug mode is enabled.
- Added source-level structure coverage for the debug-mode gate.

### Verification - 2026-04-29

- `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.ui.debug.DebugModeSurfaceStructureTest' --tests 'com.smartsales.prism.ui.debug.DebugModeStoreTest'` - pass.
- `./gradlew :app-core:assembleDebug` - pass.
- `adb devices` - `fc8ede3e device`.
- Runtime artifacts saved under `docs/projects/ui-ux-polish/evidence/06-debug-mode-global-gate/`:
  - `adb-devices.txt`
  - `debug-mode-off-home-ui.xml`, `debug-mode-off-home.png`
  - `debug-mode-on-home-ui.xml`, `debug-mode-on-home.png`
  - `debug-mode-off-audio-ui.xml`, `debug-mode-off-audio.png`
  - `debug-mode-on-audio-ui.xml`, `debug-mode-on-audio.png`
  - `debug-mode-off-connectivity-ui.xml`, `debug-mode-off-connectivity.png`
  - `debug-mode-on-connectivity-ui.xml`, `debug-mode-on-connectivity.png`
  - `debug-mode-runtime-logcat.txt`
- Runtime verdict:
  - Off/on home screenshots show the normal shell and grey debug-mode tint difference.
  - Connectivity XML proves debug probes are absent in the off capture and present in the on capture (`Debug probes`, `isReady`, `/list`, `debug rec#`, `seed dual`, `L2.5 default`, `L2.5 manual default`).
  - Audio drawer source/unit coverage proves `导入测试音频` is gated by `showDebugSurfaces`; the saved audio runtime XML captures the drawer top state and does not scroll far enough to expose the import action.
  - Filtered logcat was captured for the runtime session; no app crash was observed.

## Closeout

- Status: done
