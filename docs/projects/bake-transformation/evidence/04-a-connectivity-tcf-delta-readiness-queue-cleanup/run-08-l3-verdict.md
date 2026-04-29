# Run 08 L3 Verdict

Date: 2026-04-29
Device: `fc8ede3e`

## Scenario

Attempted an active badge switch after the user restored a two-badge state.
The goal was to verify that switching away from one badge cancels or fences
badge-owned audio work and reconnects the selected target badge.

## Evidence

- `run-08-active-switch-attempt-logcat.txt`: filtered logcat for the switch.
- `run-08-active-switch-attempt-ui.xml`: UI dump after the switch attempt.
- `run-08-active-switch-attempt.png`: supporting screenshot.
- `run-08-active-switch-attempt-dump.txt`: `uiautomator dump` command output.
- `run-08-active-switch-attempt-pull.txt`: UI XML pull command output.

## Result

Pass for the active-device switch/fencing branch.

Positive evidence:

- Registry switched from the previous active badge to the target badge:
  `Registry: switching ...8F:96 -> ...E3:F6`.
- Audio sync cancelled badge-owned work on the outgoing badge:
  `SIM badge sync: all downloads cancelled (device switch) outgoingBadgeMac=1C:DB:D4:9B:8F:96`.
- The target badge session was restored and GATT was established for
  `14:C1:9F:D7:E3:F6`.
- The selected badge reported transport readiness:
  `state=CONNECTED ip=192.168.0.100` and
  `connectUsingSession: connected, ip=192.168.0.100 ssid=MstRobot`.
- The audio drawer emitted entries with no active/queued/held downloads:
  `downloading=0 queued=0 holding=0`.

Additional observation:

- The selected badge then exposed a real transport-confirmed/HTTP-failed state:
  HTTP reachability to `http://192.168.0.100:8088` failed while network status
  still reported connected. The runtime attempted saved credential replay and
  stayed in the post-credential readiness grace loop.

Bounded note:

- This run proves the active-device switch cleanup and gives real HTTP-delayed
  runtime evidence. It does not by itself prove that the manager enum rendered
  `HTTP_DELAYED`, because the UI dump after this run still carried stale
  `media readiness: true` debug text from an earlier probe.
