---
scope: base-runtime-active
---

# Badge Connectivity Lifecycle Core Flow

## Purpose

This document defines the intended badge connectivity lifecycle for Smart Sales. It is the behavioral source of truth above the ESP32 protocol spec, Cerb connectivity specs, and current code.

Connectivity is layered readiness, not one vague connected state. A lower layer becoming true must not imply that higher layers are ready.

## Readiness Ladder

The app must model badge readiness in this order:

1. BLE detected or in range.
2. BLE GATT connected.
3. Persistent notification listener active.
4. Badge Wi-Fi status known from `IP#...` and Wi-Fi-name `SD#...` fragments.
5. Badge has usable IP and SSID.
6. HTTP `:8088` service ready for `/list`, `/download`, and `/delete`.
7. Feature-level readiness for audio sync and downstream actions.

Invariants:

- **MUST:** BLE detection only means the registered badge is nearby enough to attempt connection.
- **MUST:** BLE GATT connected only means the app can use the BLE transport.
- **MUST:** notification-listener active means unsolicited badge events can be observed for the active session.
- **MUST:** Wi-Fi status known means the app has a bounded foreground answer from the badge, even if the answer is offline.
- **MUST:** usable IP and SSID mean the badge has joined a network, but do not prove HTTP media service readiness.
- **MUST:** HTTP readiness is required for audio `/list`, `/download`, and `/delete`.
- **MUST:** feature-level readiness may add its own preconditions, including active-device fencing, queue ownership, storage availability, and downstream pipeline state.

## State Semantics

Shared and manager states must not overlap:

- `BleDetected`: scanner saw a registered badge in range; no GATT/session readiness is implied.
- `BlePairedNetworkUnknown`: BLE is held and notifications may be active, but badge Wi-Fi status is not yet confirmed.
- `BlePairedNetworkOffline`: BLE is held, but the badge reports no usable IP or no usable Wi-Fi transport.
- `Connected`: shared transport readiness for shell/history routing. It requires active GATT, active notification listener, and usable badge network status. It does not prove HTTP `:8088`.
- `WifiProvisionedHttpDelayed`: internal service state where BLE plus badge Wi-Fi transport are confirmed, but HTTP `:8088` is still warming or unreachable inside a bounded grace window.
- `Ready`: connectivity-manager state for transport-ready operation. It must mean at least the same transport readiness as `Connected`; it must not hide an HTTP-delayed state when the UI needs to communicate that media service readiness is pending.
- `ConnectivityBridge.isReady()`: feature preflight for HTTP media operations. It returns true only when the active runtime endpoint is available and HTTP `:8088` responds.

`Connected` and `Ready` are not synonyms for all feature operations. Audio sync must still call `isReady()` or an equivalent feature preflight before HTTP work.

## Delivered Behavior Alignment

Sprint 02 delivered-behavior map:
`docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`

Delivered behavior:

- Current Android code keeps shared `Connected` separate from HTTP media readiness for feature work; `ConnectivityBridge.isReady()` is the HTTP media preflight for `/list`, `/download`, and `/delete`.
- Runtime endpoint snapshots are in-memory and scoped to the current runtime; the app does not persist a last-known badge IP across app sessions.
- Manual Wi-Fi repair rejects blank credentials locally, sends credentials through the registered-badge repair path, confirms usable IP plus submitted SSID, and treats transport-confirmed HTTP delay as non-fatal.
- Solid-IP HTTP media failure can trigger bounded saved-credential replay; `IP#0.0.0.0` does not silently replay credentials and remains a repair-form branch.

Target behavior:

- `WifiProvisionedHttpDelayed` must remain visible as transport-confirmed/media-delayed and must not be flattened into feature readiness. If manager UI needs to communicate this state, code must add an explicit public manager state rather than overloading `Ready`.
- Runtime identity used for endpoint reuse and media ownership must cover every transport-confirmed state, including HTTP-delayed states, so endpoint recovery does not become ambiguous during grace windows.
- Public connectivity interfaces and supporting docs must list the notification and repair event streams that current code exposes, including `audioRecordingNotifications()` and `wifiRepairEvents()`.
- Phone SSID may be passed only as presentation copy or diagnostic context. Core reconnect, repair, and media-readiness decisions must use badge-reported IP/SSID, active runtime endpoint evidence, and remembered user-confirmed credentials.

Gap:

- Current code still flattens internal `WifiProvisionedHttpDelayed` into manager `Ready`, and the Cerb interface doc lags the current bridge API. These are lower-layer sync targets for the BAKE contract and follow-up implementation sprints, not reasons to weaken this core-flow target.

## Lifecycle Flows

### First Pairing And Setup

1. User enters first-time setup with no registered badge.
2. App scans/selects a BLE peripheral.
3. App establishes GATT and starts the notification listener.
4. App sends Wi-Fi credentials using the ESP32 two-step protocol.
5. App waits for bounded Wi-Fi status confirmation.
6. Registry stores the badge, first badge becomes default and active.
7. App promotes to shared `Connected` only after active transport readiness is confirmed.
8. HTTP readiness remains a feature preflight.

### Post-Pairing Quick Promotion

After successful setup, the app may reuse fresh pairing evidence to avoid redundant background BLE polling.

Rules:

- **MUST:** reuse only current runtime evidence, not persisted badge IP.
- **MUST:** keep `Connected` separate from HTTP `isReady()`.
- **MUST:** start audio sync only after feature readiness is confirmed.

### App Relaunch Restore

1. Registry loads the default or active badge.
2. SessionStore is seeded for that badge before reconnect.
3. Manual-disconnect intent suppresses auto reconnect.
4. Auto reconnect targets the active badge session snapshot.
5. Reconnect establishes GATT, starts notifications, then performs foreground Wi-Fi status resolution.

Rules:

- **MUST NOT:** relaunch restore route a registered badge through first-time pairing.
- **MUST NOT:** reuse a persisted badge IP across process/session boundaries.

### Manual Reconnect

Manual reconnect is user-initiated foreground recovery for the active badge.

Rules:

- **MUST:** cancel or supersede stale reconnect and repair UI work.
- **MUST:** await actual connection/query outcome, not fixed-delay polling.
- **MUST:** route missing or unsafe Wi-Fi credentials to the repair form.
- **MUST:** keep registry rows intact.

### Auto Reconnect After Disconnect, Heartbeat, Or BLE Detection

Auto reconnect may start after heartbeat failure, BLE detection of the registered active badge, or allowed app relaunch restore.

Rules:

- **MUST:** respect soft manual disconnect.
- **MUST:** target the current active badge only.
- **MUST:** stop when the active device changes.
- **MUST:** surface BLE-detected as proximity only until GATT and network status catch up.

### Network Change, Hotspot Switch, Or Subnet Isolation

When the surrounding network changes, badge connectivity may be partially valid. The app must not depend on reading the phone's current SSID; OEM restrictions make that input unavailable or non-authoritative on target devices.

Credential history is the app's recovery hint, not proof of current phone network topology.

Rules:

- **MUST NOT:** require, trust, or branch core recovery logic on the phone's current SSID.
- **MUST:** use the latest user-confirmed pairing/repair credential as the default recovery candidate.
- **MUST:** if the badge reports `IP#0.0.0.0`, classify the branch as `WifiMediaUnavailable` and prompt for editable Wi-Fi repair credentials; do not treat the badge as unregistered and do not silently replay credentials first.
- **MUST:** if the badge reports an SSID different from the latest saved credential, route to editable Wi-Fi repair with saved credentials as a hint.
- **MUST:** if the badge reports usable IP/SSID but HTTP `:8088` is unreachable or `/list` fails, silently replay the latest saved user-confirmed credential up to 3 bounded attempts, then re-check HTTP readiness through the active runtime endpoint.
- **MUST:** if saved credential replay does not restore HTTP media readiness, route to editable Wi-Fi repair with a network-switch hint.
- **MUST:** recognize that badge and phone can share the same Wi-Fi credential/SSID while receiving different subnet addresses or AP/client-isolated routing that prevents direct HTTP reachability.
- **MUST:** never interpret HTTP isolation, subnet separation, or AP client isolation as proof that BLE/session registration is invalid.

### Registered-Badge Wi-Fi Repair

Registered-badge repair keeps the badge row and BLE/session material.

Rules:

- **MUST:** enter the Wi-Fi credential repair form, not full add-device pairing.
- **MUST:** prefill from the latest saved user-confirmed credential when available while keeping SSID editable.
- **MUST:** require manual password entry and explicit send confirmation.
- **MUST:** reject blank SSID/password locally before BLE writes.
- **MUST:** send credentials through the existing manual `updateWifiConfig()` repair path.
- **MUST:** confirm against the user-submitted SSID, not against any phone-network snapshot.
- **MUST:** treat usable IP plus matching SSID as transport confirmed.
- **MUST:** treat HTTP delayed after transport confirmation as non-fatal and visible to the user.
- **MUST:** after transport confirmation, classify HTTP failure as HTTP delayed or network isolation/subnet separation before asking the user to re-enter credentials.
- **MUST:** if manual credential recovery still fails after 3 bounded confirmation attempts, escalate copy toward phone hotspot or a simple non-isolated Wi-Fi network.
- **MUST NOT:** delete registry rows, clear the session, or route through add-device pairing.

### Multi-Device Switch

Device switch is a hard active-device boundary.

Rules:

- **MUST:** cancel old reconnect, heartbeat, notification listener, and feature workers before the incoming badge can own runtime work.
- **MUST:** seed SessionStore with the incoming badge before reconnect.
- **MUST:** bind all feature work to the active badge identity.
- **MUST NOT:** allow old-device audio filenames to download through the new badge connection.

### Add Second Device

Adding a second device uses pairing/provisioning runtime, but it is not the same as registered-badge repair.

Rules:

- **MUST:** scan for unregistered devices.
- **MUST:** after successful pairing, register the new badge and make it active in the current Android flow.
- **MUST:** keep existing registered badges unless the user explicitly removes them.
- **MUST NOT:** use add-device scanning to repair Wi-Fi for the already registered active badge.

### Soft Disconnect Versus Remove/Unpair

Soft disconnect preserves the registered badge and session material.

Remove/unpair clears the explicit badge ownership chosen by the user.

Rules:

- **MUST:** soft disconnect stop heartbeat and notifications while preserving reconnect eligibility.
- **MUST:** soft disconnect suppress auto reconnect until the user requests it.
- **MUST:** remove/unpair may clear registry/session state for the removed badge.
- **MUST NOT:** use remove/unpair as a recovery strategy for Wi-Fi repair.

## Evidence Requirements

Runtime diagnosis must use `adb` and logcat when hardware is involved.

Minimum evidence classes:

- `adb devices`
- filtered logcat for launch restore
- filtered logcat or prior evidence for first/post-pairing connection
- filtered logcat for reconnect
- filtered logcat for network change, hotspot switch, or subnet/isolation branch
- filtered logcat for registered-badge Wi-Fi repair
- filtered logcat for multi-device switch when hardware state allows
- explicit adb/hardware blocker when a branch cannot be captured

## Lower-Layer Sync Targets

Lower layers must align to this document:

- `docs/specs/esp32-protocol.md` owns wire facts only.
- `docs/cerb/connectivity-bridge/spec.md` owns app implementation semantics beneath this lifecycle.
- `docs/cerb/connectivity-bridge/interface.md` owns consumer-facing contracts.
- Sprint contracts and evidence files own current audit findings and follow-up implementation scope.
