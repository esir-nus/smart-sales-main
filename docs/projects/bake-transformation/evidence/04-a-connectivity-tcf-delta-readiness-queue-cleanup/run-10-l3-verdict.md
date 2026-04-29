# Run 10 L3 Verdict

Date: 2026-04-29
Device: `fc8ede3e`

## Scenario

From the badge-management HTTP-abnormal surface, tapped the `reconnect` debug
control once and captured the resulting app state.

## Evidence

- `run-10-adb-devices.txt`: connected-device proof.
- `run-10-entry-ui.xml`: entry UI dump before tapping `reconnect`.
- `run-10-entry.png`: supporting entry screenshot.
- `run-10-reconnect-logcat.txt`: filtered logcat for the reconnect scenario.
- `run-10-reconnect-ui.xml`: UI dump after reconnect.
- `run-10-reconnect.png`: supporting screenshot after reconnect.
- `run-10-reconnect-pull.txt`: UI XML pull command output.

## Result

Pass for reconnect preserving shell connectivity while fencing HTTP media sync.

Positive evidence:

- `adb devices` shows `fc8ede3e device`.
- Reconnect started from `effectiveState=WIFI_MISMATCH`.
- `ConnectivityService.reconnect()` returned connected and the ViewModel ended
  with `effective=CONNECTED`.
- Disconnect cleanup cancelled badge-owned downloads for the reconnecting
  badge with `pendingResume=0`.
- Auto audio sync was deliberately suppressed for the known-unreachable HTTP
  endpoint: `reason=known_http_unreachable`.
- UI after reconnect shows the active badge as `已连接 · 1.0.0.1`.

Bounded note:

- This run does not prove HTTP media readiness. It proves that the shell can
  reconnect while audio sync remains fenced by HTTP reachability state, which
  is the important L3 distinction for this delta.
