---
scope: base-runtime-active
---

# Badge Session Lifecycle Core Flow

## Purpose

This document defines the intended lifecycle for Smart Sales badge sessions across single-device and multi-device use. It is the behavioral contract above the current implementation, not a restatement of the current code.

Connectivity readiness details live in [`badge-connectivity-lifecycle.md`](badge-connectivity-lifecycle.md). This document owns badge session, active-device, and audio-sync lifecycle rules; the connectivity north-star owns BLE, Wi-Fi, HTTP, repair, reconnect, and readiness semantics.

The current architecture has clear owners:

- `DeviceRegistryManager` owns which registered badge is active.
- `DeviceConnectionManager` owns how the app connects to the active badge.
- `ConnectivityBridge` exposes BLE and HTTP badge operations to feature code.
- `SimAudioRepository` owns SIM audio inventory, manual badge sync, download queueing, and download progress.
- Audio drawer ViewModels own UI projection of repository state.

**MUST:** Lower layers implement this lifecycle without making `DeviceRegistryManager` import or know about audio-specific types.

## Section 1 - Single-Device Lifecycle

### Unprovisioned

Entered when no registered badge exists and no legacy session can be migrated from `SessionStore`.

Invariants:

- **MUST:** no BLE heartbeat is running.
- **MUST:** no badge notification listener is running.
- **MUST:** badge audio sync is not started automatically.
- **MUST:** connectivity UI routes the user to setup, not retry.

Valid transitions:

- Pairing starts when the user enters first-time setup.
- Provisioned after pairing succeeds and the registry stores the first badge.

### Provisioned

Entered when a badge has a saved registry row and session material, but the app is not currently connected to it.

Invariants:

- **MUST:** `DeviceRegistryManager.activeDevice` points at the provisioned badge selected for runtime use.
- **MUST:** `SessionStore` contains the session for the active badge before reconnect starts.
- **MUST NOT:** audio downloads start until badge readiness has been verified.

Valid transitions:

- Connecting when auto-reconnect or explicit retry starts.
- Disconnected when the user manually disconnects the active badge.
- Unprovisioned only when the last registered badge is removed.

### Connecting

Entered when the app attempts to establish BLE GATT and foreground network readiness for the active badge.

Invariants:

- **MUST:** reconnect targets the active badge session snapshot, not a stale session.
- **MUST:** old heartbeat and notification listener jobs are cancelled before connecting to the target badge.
- **MUST:** BLE connection state and HTTP media readiness remain separate concerns; media operations use `ConnectivityBridge.isReady()`.

Valid transitions:

- Active when BLE and usable network status are confirmed.
- Disconnected when connection fails or user cancels.
- Reconnecting when automatic retry/backoff is running for the same active badge.

### Active

Entered when the app has an active device, BLE GATT is established, notifications are being observed, and badge network status is usable.

Invariants:

- **MUST:** heartbeat and notification listener belong to the active badge session.
- **MUST:** unsolicited badge events such as recording-ready, battery, firmware, and SD-card-space are interpreted as belonging to the active badge.
- **MUST:** audio sync work started in this state is bound to the active badge identity.

Valid transitions:

- Disconnected when the user manually disconnects, the heartbeat fails, or the badge goes unreachable.
- Reconnecting when automatic reconnect is scheduled for the same active badge.
- Device switch when the user chooses another registered badge.

### Disconnected

Entered when the active badge remains registered but no live transport is active.

Invariants:

- **MUST:** heartbeat and notification listener are stopped.
- **MUST:** manual disconnect intent prevents automatic reconnect until the user chooses retry/switch.
- **MUST NOT:** stale UI overrides such as reconnect or Wi-Fi repair prompts outlive the disconnected surface lifecycle.

Valid transitions:

- Reconnecting when automatic reconnect is allowed or the user taps retry.
- Connecting when the user explicitly chooses the active badge.
- Device switch when the user chooses another registered badge.

### Reconnecting

Entered when the app attempts to recover the active badge after a disconnect or BLE detection event.

Invariants:

- **MUST:** reconnect is suspendable or state-driven; it must not depend on fixed-delay fire-and-poll checks.
- **MUST:** reconnect still targets the active badge session snapshot.
- **MUST NOT:** reconnect for a previous active badge continue after a device switch.

Valid transitions:

- Active when reconnect succeeds.
- Disconnected when reconnect fails or reaches backoff.
- Device switch when the user chooses another registered badge, which cancels the outgoing reconnect path.

## Section 2 - Multi-Device: Pairing A Second Badge

Pairing a second badge registers a new device and makes that newly paired badge the active runtime device after successful pairing in the current Android flow. The previously active badge becomes a provisioned registered device unless the user explicitly removes it.

Invariants:

- **MUST:** pairing success writes the new badge into `DeviceRegistry` and updates `DeviceRegistryManager.activeDevice` to match the newly paired badge.
- **MUST:** the previous badge remains registered with its saved session material and user-visible name/default metadata.
- **MUST:** any live transport work for the previous badge is not allowed to leak into the new badge session.
- **MUST:** active-device change is the boundary at which device-scoped work is evaluated for teardown.

Current implementation assessment:

- `RealDeviceRegistryManager.registerDevice()` does register the new badge and sets `_activeDevice` to the new row.
- Delivered behavior: the registry remains audio-agnostic, and audio observes `DeviceRegistryManager.activeDevice` to cancel queued and active manual badge downloads when the active MAC changes.
- Gap: `rec#` auto-download is fenced after download success, but its own launched job is not explicitly cancelled by the manual badge-download queue cancellation path.

The correct behavior is that pairing completion chooses one active badge and all device-bound workers either bind to that badge or cancel before the next active badge is used.

## Section 3 - Multi-Device: Device Switch

A device switch is a hard runtime boundary. The outgoing badge remains registered, but no in-flight operation may use the incoming badge connection on behalf of the outgoing badge.

Teardown sequence for outgoing device:

1. Cancel or fence the audio download queue.
2. Cancel or fence any active badge sync/download worker.
3. Cancel reconnect work, BLE heartbeat, and notification listener.
4. Disconnect old GATT before connecting target GATT.
5. Clear transient UI overrides tied to the outgoing device.
6. Seed `SessionStore` with the incoming device session.
7. Publish the incoming `activeDevice`.
8. Reconnect to the incoming device.

Item status:

Delivered behavior:

- Audio download queue: current manual sync cancellation clears queued filenames on active-device change; target behavior still prefers queue items that carry device identity.
- Active badge download job: current manual sync cancellation cancels the active badge download job on active-device change.
- Ongoing sync preflight/list operation: current sync captures owner badge MAC, discards stale list results if runtime identity changes before queueing, and checks owner MAC again before importing a successful download.
- BLE heartbeat: handled by `connectUsingSession()` through active transport job cancellation.
- Notification listener: handled by `connectUsingSession()` through active transport job cancellation.
- Reconnect job: replaced by the next reconnect launch; target behavior remains that reconnect cannot continue for an outgoing device after switch.
- UI observation: badge audio inventory/download progress flows are intended to stay hot while surfaces are closed.

Target behavior:

- Every badge-originated worker, including manual sync queue workers and `rec#` auto-download workers, must bind to a badge MAC/runtime identity from start to terminal state.
- A stronger queue model should carry badge identity per queued item rather than relying on a global filename set plus runtime checks.
- Active-device switch must cancel or fence all outgoing feature workers before incoming badge media operations can start.
- Session seeding must preserve durable per-device identity material where that identity is used for runtime ownership or endpoint reuse; generating transient identity material is not a target-state substitute.

Gap:

- `queuedBadgeDownloads` still stores filenames globally, `rec#` auto-download is not explicitly cancelled by active-device switch, and current registry session seeding creates a fresh secure token for the target device. The delivered fences prevent known cross-device imports, but the target flow requires explicit device identity across worker queues and session ownership.

Invariants:

- **MUST:** after `activeDevice` changes from Badge B to Badge A, no Badge B audio filename can call `downloadRecording()` through Badge A's connection.
- **MUST:** the registry remains audio-agnostic; audio observes the active-device signal and cancels/fences its own work.
- **MUST:** a switch is serialized so two target sessions cannot compete to seed `SessionStore`.

## Section 4 - Audio Sync Lifecycle

Badge audio sync starts from either manual user action or automatic readiness after connectivity becomes ready. It lists badge recordings, creates/updates drawer placeholders, and downloads queued WAV files in the background.

The sync belongs to the active badge at the time it starts. That badge identity is the owner of:

- the readiness check result,
- the `/list` response,
- the placeholder rows created from that list,
- the queued download filenames,
- the active `downloadRecording()` call,
- the progress updates emitted into `audioFiles`.

Cancellation events:

- active device changes,
- active badge is manually disconnected,
- active badge is removed,
- the user deletes/cancels the individual audio file,
- repository/test scope is closed,
- connectivity readiness fails before queueing.

Binding rule:

- **MUST:** the audio download worker is bound to a specific device MAC.
- **MUST NOT:** the worker use a different active badge connection from the one that produced its queue.
- **MUST:** any active-device mismatch before a download starts cancels the queued item or fails it without calling `downloadRecording()`.

Implementation direction:

Audio should observe `DeviceRegistryManager.activeDevice` through the existing runtime-level dependency and cancel all queued/active badge downloads on MAC change. A later stronger implementation may carry the MAC in each queue item, but the minimal contract for Sprint 02 is cancellation on active-device change plus clear logging. This preserves the dependency direction: audio reads registry state, registry does not import audio.

Delivered behavior: Sprint 02 implementation now satisfies the minimal manual-sync cancellation/fencing rule for queued and active badge downloads. Target behavior keeps the stronger per-item MAC ownership rule open for the BAKE contract and follow-up implementation.

## Section 5 - UI Observation Contract

Badge-originated data arrives when hardware and background workers emit it, not when a drawer happens to be open. UI state derived from that data must keep an always-current ViewModel value so reopening a surface does not appear to "trigger" missing updates.

Use `SharingStarted.Eagerly` for flows that carry badge-originated or background-worker-originated state:

- badge audio inventory,
- badge audio download progress,
- badge sync availability,
- hardware readiness that triggers auto sync,
- pipeline state that can change while the user is outside the drawer.

Use `SharingStarted.WhileSubscribed` only for flows that are purely UI-local or user-action-derived and can safely tolerate snapshot refresh on reopen:

- expanded row IDs,
- confirmation dialog state,
- transient one-shot events,
- view-only transformations with no background source dependency.

Invariants:

- **MUST:** audio drawer inventory and progress observation stay hot enough to reflect repository updates while the drawer is closed.
- **MUST NOT:** closing the drawer be required to refresh newly downloaded badge audio.
- **MUST:** UI surfaces display the current repository state immediately on open without requiring a second user action.

Current implementation assessment:

- `SimAudioDrawerViewModel.entries` uses `SharingStarted.Eagerly` for repository audio files.
- `AudioViewModel.audioItems` uses `SharingStarted.Eagerly` for repository audio files.
- Sprint 03 closed this debt with a focused structure test and full `:app-core:testDebugUnitTest` verification.

## Section 6 - Connectivity Isolation Repair

Network isolation prompts are part of the badge session lifecycle because they decide whether the current active badge can remain usable after the phone changes Wi-Fi or hotspot transport.

Detailed connectivity states and flows are owned by [`badge-connectivity-lifecycle.md`](badge-connectivity-lifecycle.md). The session-level contract remains:

Invariants:

- **MUST:** isolation prompts provide a real recovery action, not just a transient UI dismissal.
- **MUST:** recovery for an already registered active badge repair Wi-Fi credentials without unregistering the badge.
- **MUST NOT:** isolation recovery route an already registered active badge into full add-device pairing unless the user explicitly removes/unpairs it first.
- **MUST:** the isolation CTA use wording that communicates Wi-Fi/network repair, not full pairing.
- **MUST:** the Wi-Fi repair form prefill the latest saved user-confirmed SSID when available while keeping the SSID editable and the password manual.
- **MUST:** repair actions leave adb/logcat evidence that identifies the user action and the lifecycle transition taken.

Implementation direction:

The current add-device scan intentionally filters already registered badges. That is correct for adding a second device and must not drive isolation recovery for the active registered badge. Isolation recovery should keep the registry row and BLE/session material, enter the existing Wi-Fi credential repair form, and call the manual `updateWifiConfig()` repair path after explicit user confirmation.

## Implementing Sprints

Sprint 02, Sprint 03, Sprint 04, Sprint 05, and the bounded Badge Wi-Fi Recovery State Machine follow-up implemented or were triaged against this contract and the connectivity north-star.

Sprint 02 implements the audio sync teardown rule: queued and active badge downloads must not survive an active-device MAC change.

Sprint 03 implemented the UI observation rule: audio inventory/download-progress flows must stay hot enough to reflect badge-originated updates while drawers are closed.

Sprint 04 implemented the connectivity isolation repair rule: hotspot/network-isolation recovery must be tested with real adb evidence, and the isolation CTA must enter Wi-Fi credential repair for registered badges instead of clearing the prompt or starting full add-device pairing.

Sprint 05 was cancelled as an unoperated contract after Sprint 06 absorbed its connectivity north-star, spec sync, and runtime recovery direction. Any local Sprint 05 gap map or matrix material is pre-Sprint-06 stale audit material unless re-audited.

The Badge Wi-Fi Recovery State Machine follow-up implements the split between `TransportDisconnected` and `WifiMediaUnavailable`: BLE loss remains a BLE reconnect problem, while BLE-connected media failure keeps the active badge registered and tries bounded Wi-Fi/media recovery without add-device pairing.
