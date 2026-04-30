# Sprint 04-b Physical L3 Choreography

- Date prepared: 2026-04-30
- Evidence class: physical `L3`
- Device under test: Android phone reported by `adb devices` as `fc8ede3e`
- App package: `com.smartsales.prism`
- Artifact directory:
  `docs/projects/bake-transformation/evidence/04-b-connectivity-intent-driven-reconnect-modal-law/`

This runbook is not evidence by itself. It is the exact manual collaboration
plan and command set to use when the required badges and network conditions are
available. A loop may pass only after its manual items are performed inside the
capture window and the saved artifacts satisfy the pass criteria below.

## Required Physical Identities

Before running any loop, record the exact hardware identities here:

- Phone: `fc8ede3e`
- Latest intended active badge A MAC: `TBD`
- Alternate registered badge B MAC: `TBD`
- Badge A display name: `TBD`
- Badge B display name: `TBD`
- Wi-Fi SSID used for normal connected state: `TBD`
- Isolation or mismatch network used for loop D: `TBD`

Block physical L3 if badge A or badge B cannot be identified by exact MAC in
the registry and by a physical label on the hardware.

## Common Setup

Run these before each loop unless the loop explicitly says to preserve state:

```bash
export EVID_DIR=docs/projects/bake-transformation/evidence/04-b-connectivity-intent-driven-reconnect-modal-law
export GRADLE=/home/cslh-frank/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle
export JAVA_HOME=/home/cslh-frank/jdks/jdk-17.0.11+9
export OPENSHIFT_INTERNAL_IP=127.0.0.1
export GRADLE_USER_HOME=/tmp/gradle-user-home
$GRADLE :app-core:assembleDebug
adb install -r app-core/build/outputs/apk/debug/app-core-debug.apk
adb devices > "$EVID_DIR/run-09-preflight-adb-devices.txt"
adb shell am force-stop com.smartsales.prism
adb shell am start -n com.smartsales.prism/.MainActivity --ez sim_debug_connectivity_manager true
```

Use preserved app data when the loop depends on two registered badges and a
latest-intent registry history. If app data is cleared, the operator must
re-register both physical badges and explicitly connect/switch to badge A before
starting the capture.

Confirm registry identities before each physical loop:

```bash
adb shell run-as com.smartsales.prism cat /data/user/0/com.smartsales.prism/shared_prefs/device_registry.xml \
  > "$EVID_DIR/run-09-preflight-device-registry.xml"
adb shell run-as com.smartsales.prism cat /data/user/0/com.smartsales.prism/shared_prefs/ble_session_store.xml \
  > "$EVID_DIR/run-09-preflight-session-store.xml"
```

Use this capture shape for each loop after manual pre-state is established:

```bash
adb logcat -c
# Perform exactly the manual action declared by the loop.
sleep 8
adb logcat -d -v time | rg -n "SmartSalesConn|ConnectivityVM|ConnectivityService|ConnectivityPrompt|ConnectivityModal|BLE|bleDetected|Auto-reconnect|connect\\(\\)|reconnect|managerStatus|WIFI_MISMATCH|isolation" \
  > "$EVID_DIR/<run-name>-logcat.txt"
adb exec-out uiautomator dump /dev/tty > "$EVID_DIR/<run-name>-ui.xml"
adb exec-out screencap -p > "$EVID_DIR/<run-name>.png"
```

## Loop A: Latest Intended Active Present

Run name:

- `run-10-loop-a-physical-l3`

Manual collaboration items:

- Human owner: operator
- Badge identity: badge A, exact MAC recorded above
- Pre-state: badge A and badge B are registered; badge A is the latest intended
  active badge after an explicit user connect or device switch; app shared state
  is not `Connected`
- Timing: after `adb logcat -c`, power on or move badge A into BLE range while
  badge B remains powered off or out of range
- Expected observation: app opens `ConnectivityModal`; reconnect work targets
  badge A only
- Pass condition: logcat and UI prove badge A is the only auto-reconnect target
- Block condition: badge A cannot be made unavailable/available on demand, or
  exact badge MAC cannot be confirmed

Pass criteria:

- Positive log/UI evidence:
  - BLE detection or reconnect telemetry names badge A MAC
  - modal UI is visible after badge A becomes available
  - active card is badge A
  - if transport is not ready yet, badge A renders disconnected or reconnecting,
    not as a connected card
- Negative evidence:
  - no `connect()` or auto-reconnect target for badge B
  - no active-device switch to badge B
  - no connected-only metadata shown unless shared state becomes `Connected`

## Loop B: Latest Intended Active Absent, Other Badge Present

Run name:

- `run-11-loop-b-physical-l3`

Manual collaboration items:

- Human owner: operator
- Badge identity: badge A exact MAC is latest intended active; badge B exact MAC
  is another registered badge
- Pre-state: badge A is latest intended active after explicit connect/switch;
  badge A is powered off or physically out of range; badge B is registered and
  starts out of range
- Timing: after `adb logcat -c`, power on or move only badge B into BLE range
- Expected observation: app opens `ConnectivityModal` as a chooser and does not
  auto-connect badge B
- Pass condition: badge B is shown as available to tap, but there is no automatic
  connect/reconnect to badge B
- Block condition: badge A or badge B physical range/power state cannot be
  controlled independently

Pass criteria:

- Positive log/UI evidence:
  - badge B becomes BLE detected
  - modal UI is visible
  - badge B card is chooser-only, such as detected/click-connect copy
  - badge A remains the latest intended active identity
- Negative evidence:
  - no `Auto-reconnect triggered by BLE detection for <badge B MAC>`
  - no `connect() called for <badge B MAC>` unless the operator taps badge B
  - no `SessionStore` reseed or active-device switch to badge B

## Loop C: Manually Disconnected Badge Reappears

Run name:

- `run-12-loop-c-physical-l3`

Manual collaboration items:

- Human owner: operator
- Badge identity: badge A exact MAC
- Pre-state: badge A is the latest intended active badge and was manually
  disconnected in the app by tapping the disconnect action; badge A is then out
  of range or powered off
- Timing: after `adb logcat -c`, power on or move badge A into BLE range
- Expected observation: app does not auto-reconnect badge A; the card remains
  disconnected-first until explicit user reconnect
- Pass condition: badge A proximity can be shown as secondary evidence, but
  transport ownership does not resume automatically
- Block condition: manual disconnect state cannot be confirmed in registry or UI

Pass criteria:

- Positive log/UI evidence:
  - registry or UI shows badge A `manuallyDisconnected=true` or disconnected
    card state before reappearance
  - after badge A reappears, badge A card remains disconnected-first
  - modal does not auto-start a reconnect for badge A
- Negative evidence:
  - no auto-reconnect for badge A
  - no `managerStatus=Connecting` caused solely by badge A reappearing
  - no transition to `Connected` until the operator explicitly taps reconnect

## Loop D: Reconnect Success Clears Stale Mismatch Or Isolation UI

Run name:

- `run-13-loop-d-physical-l3`

Manual collaboration items:

- Human owner: operator
- Badge identity: badge A exact MAC
- Pre-state: badge A is active; app is showing stale Wi-Fi mismatch or suspected
  isolation UI for badge A; badge A can be restored to the normal Wi-Fi network
  without changing active badge identity
- Timing: after `adb logcat -c`, restore badge A network reachability and tap
  reconnect or perform the declared physical reconnect action
- Expected observation: shared state reaches `Connected`, and stale
  mismatch/isolation UI clears for the active badge
- Pass condition: the same active badge reconnects successfully and the repair
  card/prompt disappears
- Block condition: the mismatch/isolation state cannot be created reliably, or
  the badge cannot be restored to a reachable network inside one capture window

Pass criteria:

- Positive log/UI evidence:
  - pre-capture UI or preflight artifact shows `WIFI_MISMATCH` or suspected
    isolation UI for badge A
  - logcat shows reconnect success for badge A and manager/shared state returning
    to connected or ready
  - final UI dump does not contain Wi-Fi mismatch form text, isolation action
    text, or stale repair error for badge A
- Negative evidence:
  - no active badge MAC change during the loop
  - no lingering `WIFI_MISMATCH` manager UI after connected state
  - no repair prompt reappears without a fresh mismatch/isolation event

## Verdict Template

Append one entry per loop after capture:

```text
- Physical L3 <loop>: PASS/BLOCKED/FAIL
  - Manual items performed: yes/no, details
  - Artifacts: <logcat>, <ui.xml>, <png>
  - Positive evidence: <specific log/UI facts>
  - Negative evidence: <forbidden log/UI states absent or present>
  - Notes: <contamination, permission dialog, timing issue, or next action>
```
