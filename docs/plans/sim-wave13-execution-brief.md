# Wave 13 Execution Brief: SIM Launcher-Core Theme Visibility

## Objective

Make the normal SIM launcher path honor the shared `Dark / Light / System` theme preference so a user can open the app, switch theme in SIM `User Center`, and immediately see launcher-core visuals change.

This is not full SIM light-theme parity.

## Visual Source Stack

- `prototypes/sim-shell-family/sim_home_hero_shell_light.html`
- `prototypes/sim-shell-family/sim_history_drawer_shell_light.html`
- `prototypes/sim-shell-family/sim_user_center_shell_light.html`
- supporting family context: `prototypes/sim-shell-family/sim_prototype_family.html`

## Included Production Scope

- `SimMainActivity` theme application and system-bar contrast
- normal launcher-path SIM shell root/background/chrome
- empty home shell and active chat shell
- launcher-core conversation/system/status/artifact surfaces
- top header / dynamic-island chrome
- `SimHistoryDrawer`
- `SimUserCenterDrawer`

## Deferred Scope

- scheduler drawer
- audio drawer
- connectivity manager / modal
- onboarding / setup flows
- deeper non-launcher SIM overlays

Deferred surfaces may remain dark under light mode in this slice. They stay functional but do not count as shipped light-theme parity.

## Invariants

- keep `SimMainActivity` as the normal launcher host
- keep existing SIM routing, shell ownership, and drawer choreography
- keep history -> settings handoff unchanged
- keep shared `ThemePreferenceStore` / `PrismThemeMode` behavior unchanged
- do not widen scheduler/audio/connectivity behavior scope

## Verification

- focused `:app-core:compileDebugKotlin`
- focused `PrismThemeTest`
- focused `ThemePreferenceStoreTest`
- focused `SimRuntimeIsolationTest`
- write L1 evidence to `docs/reports/tests/L1-20260326-sim-wave13-launcher-core-theme-validation.md`
- leave L3/device screenshot validation open until launcher-core light-theme screenshots are reviewed
