# Sprint 05 - connectivity-card-stable-order

## Header

- Project: ui-ux-polish
- Sprint: 05
- Slug: connectivity-card-stable-order
- Date authored: 2026-04-29
- Author: Codex
- Operator: Codex
- Lane: `develop`
- Branch: `develop`

## Demand

Keep connectivity manager card order stable while the user switches the active badge.

## Scope

Files in scope:

- `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModelTest.kt`
- `docs/projects/ui-ux-polish/tracker.md`
- `docs/projects/ui-ux-polish/sprints/05-connectivity-card-stable-order.md`
- `docs/core-flow/base-runtime-ux-surface-governance-flow.md`
- `docs/cerb/connectivity-bridge/spec.md`

Out of scope:

- BLE scanner priority or registry selection behavior.
- `setDefault()` switching active device, seeding session, or reconnecting.
- Device persistence schema changes.
- Harmony lane work.

## Contract

- The default registered badge is pinned as the first card.
- All non-default registered badges are ordered by `RegisteredDevice.registeredAtMillis` ascending.
- `macAddress` is the deterministic tie-breaker when registration timestamps match.
- Active selection must not affect visual ordering; active state is shown only by the existing selected-card styling and active-card content.
- `lastConnectedAtMillis` must not affect visual ordering.
- Calling `setDefault()` may move the new default card to the top, but remains passive and does not switch active badge.

## Verification Plan

- Focused unit test:

```bash
./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest'
```

- Runtime evidence when a device is available:

```bash
adb devices
adb logcat -c
adb logcat -d -s ConnectivityVM SmartSalesConn
```

Then open the connectivity modal and confirm by UI XML/screenshot that selecting a second or third card changes only the active border/content, not card order.

## Iteration Ledger

### Iteration 1 - 2026-04-29

- Updated `ConnectivityViewModel.sortedDevices` to pin default first and sort non-default cards by `registeredAtMillis`, with `macAddress` as the tie-breaker.
- Added focused `ConnectivityViewModelTest` coverage for active non-default selection, active-selection switching, passive default promotion, and oldest-pairing order.
- Synced the connectivity UI contract docs.
- Focused verification: `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.ui.components.connectivity.ConnectivityViewModelTest'` -> `BUILD SUCCESSFUL in 15s`.
- Build verification: `./gradlew :app-core:assembleDebug` -> `BUILD SUCCESSFUL in 12s`.
- Runtime verification on `fc8ede3e`: installed `app-core-debug.apk`, opened the connectivity modal through the audio drawer `徽章管理` entry, captured default-first order, tapped the second card, and captured the modal again with the default card still first while connected active content moved to the second card.
- Evidence:
  - `docs/projects/ui-ux-polish/evidence/05-connectivity-card-stable-order/adb-devices.txt`
  - `docs/projects/ui-ux-polish/evidence/05-connectivity-card-stable-order/modal-before-switch-ui.xml`
  - `docs/projects/ui-ux-polish/evidence/05-connectivity-card-stable-order/modal-before-switch.png`
  - `docs/projects/ui-ux-polish/evidence/05-connectivity-card-stable-order/modal-before-switch-logcat.txt`
  - `docs/projects/ui-ux-polish/evidence/05-connectivity-card-stable-order/modal-after-second-card-tap-ui.xml`
  - `docs/projects/ui-ux-polish/evidence/05-connectivity-card-stable-order/modal-after-second-card-tap.png`
  - `docs/projects/ui-ux-polish/evidence/05-connectivity-card-stable-order/modal-after-second-card-tap-logcat.txt`

## Closeout

- Status: done
- One-liner for tracker: Connectivity manager cards keep the default badge pinned first while non-default badges stay in oldest successful-pairing order; active selection is visual only.
