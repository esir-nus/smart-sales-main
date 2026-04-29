# Run 33 L3-Debug Verdict: rec# Switch Cancellation

## Scenario

Preserved badge state, launched the rebuilt debug APK into the connectivity
manager, tapped `debug rec#`, then switched from the connected default badge
`14:C1:9F:D7:E4:06` to registered badge `14:C1:9F:D7:E3:EE`.

## Positive Evidence

- `run-32-debug-connectivity-entry-ui.xml`: connectivity manager opened with a
  connected badge row, second registered badge row, and `debug rec#` control.
- `run-33-debug-rec-switch-logcat.txt`: `source=debug_rec_button` emitted
  `rec_19700101_000154.wav` through the app-side recording notification route.
- `run-33-debug-rec-switch-logcat.txt`: `rec# auto-download` received the
  notification, created a placeholder, and started downloading under
  `badgeMac=14:C1:9F:D7:E4:06`.
- `run-33-debug-rec-switch-logcat.txt`: registry switched
  `...E4:06 -> ...E3:EE`.
- `run-33-debug-rec-switch-logcat.txt`: outgoing active `rec#` job logged
  `disconnect cancel badgeMac=14:C1:9F:D7:E4:06 count=1` and terminal
  `canceled filename=rec_19700101_000154.wav badgeMac=14:C1:9F:D7:E4:06`.
- `run-33-debug-rec-switch-logcat.txt`: subsequent sync/download work used
  incoming badge `14:C1:9F:D7:E3:EE`.

## Negative Evidence

- No later successful import or `Command#end` was observed for
  `rec_19700101_000154.wav` after the active badge switch.
- No download start for `rec_19700101_000154.wav` under incoming badge
  `14:C1:9F:D7:E3:EE` was observed.

## Verdict

Pass for L3-debug app-side `rec#` cancellation. The debug ingress is intentionally
at `ConnectivityBridge.audioRecordingNotifications()`, so it validates the app
route after badge notification parsing without faking repository or UI state.
This does not prove physical firmware BLE `rec#` emission during a switch.
