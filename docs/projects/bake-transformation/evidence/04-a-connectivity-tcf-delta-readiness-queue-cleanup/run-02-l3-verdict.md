# Run 02 L3 Verdict

Date: 2026-04-29
Device: `fc8ede3e`

## Scenario

Preserved app data and resumed the L3 loop after the user confirmed the device
and badge were connected. The loop cold-launched the already installed debug
build without `pm clear`, then captured filtered logcat, UI XML, and
screenshots.

## Evidence

- `run-02-adb-devices.txt`: device was visible to adb.
- `run-02-launch.txt`: launched `com.smartsales.prism/.MainActivity`.
- `run-02-launch-logcat.txt`: filtered launch and badge restore logs.
- `run-02-launch-ui.xml`: UI dump after launch.
- `run-02-launch.png`: supporting screenshot after launch.
- `run-02-after-sync-logcat.txt`: later filtered logs while badge download was
  still active.
- `run-02-after-sync-ui.xml`: later UI dump.
- `run-02-after-sync.png`: later supporting screenshot.

## Result

Pass for restored badge connectivity, HTTP readiness, and badge-owned audio
sync ingress. Terminal audio import was not observed in the bounded capture
window.

Positive evidence:

- Session restore: `Restored session: 1C:DB:D4:9B:8F:96 knownNetworks=1`.
- Registry restore: `Registry: default device CHLE_Intelligent (...8F:96)`.
- BLE/GATT: `Persistent GATT session established: 1C:DB:D4:9B:8F:96`.
- Badge network: `state=CONNECTED ip=192.168.0.103` and
  `connectUsingSession: connected, ip=192.168.0.103 ssid=MstRobot`.
- HTTP readiness: `BadgeHttpClient.isReachable ... code=200 reachable=true`
  and `isReady preflight: result=ready`.
- Audio sync: `/list` succeeded with `count=1`.
- Queue ownership: sync outcome and download start both carry
  `badgeMac=1C:DB:D4:9B:8F:96`.
- Download path: `Downloading:
  http://192.168.0.103:8088/download?file=2speakerTesting.wav`.

Bounded observation:

- The capture ended with the drawer projection still reporting
  `downloading=1 queued=0 holding=0`. No success/failure terminal import was
  observed before the capture ended.

Not exercised:

- HTTP-delayed readiness branch did not occur because badge HTTP returned 200.
- Active-device switch fencing was not exercised on device; it remains covered
  by focused L1/L2 tests in this sprint.
