# Run 22 L3 Verdict: HTTP-Delayed Manager Enum

## Scenario

- Device: `fc8ede3e`
- APK: `app-core-debug.apk`, installed with `adb install -r` to preserve badge state.
- Entry state: badge management shows `CHLE_Intelligent`, default row,
  `已连接 · 1.0.0.1`, and the `isReady` debug probe.
- Action: clear logcat, tap `isReady` once, wait for the readiness/recovery
  path to complete, then capture filtered logcat, UI XML, and screenshot.

## Evidence

- `run-22-entry-ui.xml`: connected badge-management entry state.
- `run-22-isready-logcat-unified-filter.txt`: HTTP failure, manager delayed
  emission, ViewModel enum mapping, saved-credential replay, and final HTTP
  recovery.
- `run-22-isready-ui.xml`: final manager UI after the probe, showing
  `media readiness: true` after recovery.

Key log lines:

```text
BadgeHttpClient.isReachable failure url=http://192.168.0.101:8088/
manager media endpoint delayed runtime=14:C1:9F:D7:E4:06/6c7ac2 baseUrl=http://192.168.0.101:8088
managerStatus=HttpDelayed(badgeIp=192.168.0.101, ssid=MstRobot, baseUrl=http://192.168.0.101:8088) managerState=HTTP_DELAYED
solid IP + HTTP readiness failure; attempting saved credential replay
saved credential replay result after media failure: WifiProvisioned(...)
manager media endpoint ready baseUrl=http://192.168.0.101:8088
debug media readiness result=true
```

## Verdict

Pass. The previously missing L3 proof is now captured: the runtime bridge emits
the HTTP-delayed manager condition and the UI ViewModel maps it to
`ConnectivityManagerState.HTTP_DELAYED` before recovery returns the media path
to ready.
