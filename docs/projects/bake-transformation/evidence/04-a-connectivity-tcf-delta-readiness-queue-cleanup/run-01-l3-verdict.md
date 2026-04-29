# Run 01 L3 Verdict

Date: 2026-04-29
Device: `fc8ede3e`

## Scenario

Clean install/launch baseline after `pm clear`, using the Android debug APK
built by `./gradlew :app-core:assembleDebug`.

## Evidence

- `run-01-adb-devices.txt`: device was visible to adb.
- `run-01-install.txt`: APK installed successfully.
- `run-01-pm-clear.txt`: app data cleared successfully.
- `run-01-launch.txt`: launched `com.smartsales.prism/.MainActivity`.
- `run-01-launch-logcat.txt`: filtered launch logs captured with
  `AudioPipeline`, `SmartSalesConn`, `ConnectivityService`, `ConnectivityVM`,
  and `BT311Scan`.
- `run-01-launch-ui.xml`: UI dump captured after launch.
- `run-01-launch.png`: supporting screenshot.

## Result

Partial L3 evidence only.

The device loop proves the debug APK installs and launches, and logcat shows the
expected clean-state branch:

- `SmartSalesConn`: `Registry: no devices registered`
- `AudioPipeline`: SIM audio drawer entry projection emitted
- `ConnectivityVM` / `ConnectivityService`: auto-reconnect scheduling path
  invoked

Hardware badge restore, HTTP-delayed readiness, and live badge audio-sync paths
remain blocked in this run because the clean-state device had no registered
badge after `pm clear`. No hardware-ready claim is made for those branches.
