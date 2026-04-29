# Run 07 L3 Verdict

Date: 2026-04-29
Device: `fc8ede3e`

## Scenario

Ran the badge-management `isReady` debug probe to check whether the current
hardware state could produce the remaining HTTP-delayed branch.

## Evidence

- `run-07-isready-probe-logcat.txt`: filtered logcat for the probe.
- `run-07-isready-probe-ui.xml`: UI dump after the probe.
- `run-07-isready-probe.png`: supporting screenshot.
- `run-07-isready-probe-dump.txt`: `uiautomator dump` command output.
- `run-07-isready-probe-pull.txt`: UI XML pull command output.

## Result

Blocked for HTTP-delayed L3 from the current state.

Positive evidence:

- Probe started: `isReady preflight: start`.
- Probe used badge HTTP endpoint: `checking reachability baseUrl=http://192.168.0.102:8088`.
- HTTP responded successfully: `code=200 reachable=true`.
- Probe result was ready: `isReady preflight: result=ready`.
- UI showed `media readiness: true`.

Conclusion:

- The HTTP-delayed branch cannot be claimed from this run because media
  readiness is true, not delayed.
- This run confirms the current hardware state is HTTP-ready even while the
  badge-management surface is showing a disconnected/isolation message.
