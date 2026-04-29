# Run 09 L3 Verdict

Date: 2026-04-29
Device: `fc8ede3e`

## Scenario

Captured the badge-management surface after the run-08 switch and attempted
to observe the post-switch HTTP readiness state.

## Evidence

- `run-09-post-switch-isready-logcat.txt`: filtered logcat for the observation.
- `run-09-post-switch-isready-ui.xml`: UI dump after the observation.
- `run-09-post-switch-isready.png`: supporting screenshot.
- `run-09-post-switch-isready-dump.txt`: `uiautomator dump` command output.
- `run-09-post-switch-isready-pull.txt`: UI XML pull command output.

## Result

Partial pass for the human-visible HTTP-abnormal surface.

Positive evidence:

- UI shows `连接后网络检测异常`.
- UI explains that the badge joined the network but HTTP cannot be reached:
  `徽章已接入网络，但无法通过 HTTP 访问...`.
- UI shows the affected badge IP: `徽章 IP：192.168.0.100`.
- UI still shows two registered badge rows, with one default row available.

Bounded note:

- The logcat capture for this run did not contain a useful readiness probe
  sequence. The visible `media readiness: true` text appears to be stale debug
  probe output from an earlier ready check, so this run is accepted only as UI
  evidence for the HTTP-abnormal panel, not as proof of fresh media readiness.
