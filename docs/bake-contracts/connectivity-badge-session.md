---
protocol: BAKE
version: 1.0
domain: connectivity-badge-session
layer: L1-L4 corridor
runtime: base-runtime-active
owner: data/connectivity, app-core connectivity, app-core audio
core-flow-doc:
  - docs/core-flow/badge-connectivity-lifecycle.md
  - docs/core-flow/badge-session-lifecycle.md
last-verified: 2026-04-28
---

# Connectivity Badge Session BAKE Contract

This contract records the verified delivered implementation for the
ConnectivityBridge plus badge-session corridor. The core-flow docs remain the
behavioral north star; this BAKE contract is the implementation record and
tracks gaps explicitly.

## Pipeline Contract

### Inputs

- Active badge selection from `DeviceRegistryManager.activeDevice`.
- BLE connection state, foreground Wi-Fi query results, notification-listener
  events, and heartbeat state from the badge connection manager.
- HTTP media endpoint resolution for `/list`, `/download`, and `/delete` on the
  active runtime endpoint.
- Badge notification streams: `log#...`, `rec#...`, battery, firmware version,
  SD-card-space, and Wi-Fi repair milestones.
- Audio sync triggers from manual user action, automatic readiness, and `rec#`
  auto-download notifications.

### Outputs / Guarantees

- `ConnectivityBridge.connectionState` emits shared shell transport state.
  `Connected` means active GATT, notification listener, and usable badge network
  status; it does not prove HTTP media readiness.
- `ConnectivityBridge.managerStatus` emits manager-only diagnostics for setup,
  BLE-held network unknown/offline, BLE detected, ready, and errors.
- `ConnectivityBridge.isReady()` is the feature-level HTTP media preflight and
  must pass before normal `/list`, `/download`, or `/delete` work.
- Manual badge sync creates badge-scoped placeholders, queues downloads,
  imports valid WAV files, removes empty WAV placeholders, records failures, and
  fences results by runtime badge identity.
- `audioRecordingNotifications()` feeds `rec#` auto-download. Auto-download
  creates a queued placeholder immediately, downloads in the background, fences
  successful import by current runtime badge MAC, and sends `Command#end` at
  terminal points.
- `wifiRepairEvents()` exposes repair milestones so UI repair state is driven by
  explicit events instead of inferred manager state.

### Invariants

- MUST keep BLE/session transport readiness separate from HTTP media readiness.
- MUST keep registry ownership audio-agnostic: audio observes active-device
  changes and cancels or fences its own work; registry must not import audio.
- MUST bind badge media work to the active runtime badge identity before
  importing or exposing downloaded audio.
- MUST preserve registered badge rows during Wi-Fi repair; repair must not route
  through add-device pairing unless the user explicitly removes the badge first.
- MUST treat phone SSID only as presentation or diagnostic context. Core repair
  and media decisions use badge-reported IP/SSID, active endpoint evidence, and
  saved user-confirmed credentials.
- MUST avoid repeated background BLE Wi-Fi polling during normal HTTP media
  traffic; endpoint reuse is current-runtime only.

### Error Paths

- No active or resolvable endpoint: media calls return not-ready or
  not-connected without claiming session invalidation.
- Badge reports `IP#0.0.0.0` or no usable network: route to registered-badge
  Wi-Fi repair, not saved-credential replay or full pairing.
- Solid-IP HTTP failure or `/list` failure: invalidate the active endpoint,
  attempt bounded saved-credential replay when eligible, then re-check media
  readiness.
- Blank Wi-Fi credentials: reject locally before BLE writes.
- Active-device change during list/download: discard stale list results, cancel
  manual queue work where possible, and reject cross-device downloads before
  import.
- Active badge download already running: skip readiness probing and return an
  already-running sync result instead of treating HTTP timeout as Wi-Fi failure.

## Telemetry Joints

- [INPUT_RECEIVED]: manual sync request, auto sync request, `log#...` or
  `rec#...` badge notification, Wi-Fi repair submit, reconnect request.
- [ROUTER_DECISION]: choose setup, reconnect, Wi-Fi repair, HTTP readiness
  preflight, saved-credential replay, manual queue download, or `rec#`
  auto-download.
- [STATE_TRANSITION]: connection manager state changes, manager-status
  projection, active-device switch, sync queue state, repair milestones.
- [EXTERNAL_CALL]: BLE Wi-Fi query/provision, HTTP `/list`, HTTP `/download`,
  HTTP `/delete`, BLE `Command#end`, firmware/SD-card queries.
- [OUTPUT_EMITTED]: connection state, manager status, repair event, audio
  placeholder, progress update, ready audio import, failed audio item, command
  completion signal.
- [ERROR_CAPTURED]: HTTP unreachable latch, endpoint invalidation, stale runtime
  discard, cross-device discard, empty WAV removal, replay failure, hardware
  evidence blocker.

Current observability is local docs/code evidence plus unit tests. Hardware L3
claims still require fresh `adb logcat` evidence.

## UI Docking Surface

Connectivity UI consumes `connectionState`, `managerStatus`, `wifiRepairEvents`,
registered-device state, and badge metadata streams. Audio UI consumes repository
inventory/progress state produced by manual sync and `rec#` auto-download. UI
surfaces present readiness and repair choices only; pipeline decisions remain in
connectivity and audio owners.

## Core-Flow Gap

- Gap: `WifiProvisionedHttpDelayed` is currently flattened to
  `BadgeManagerStatus.Ready`, so manager UI cannot directly distinguish
  transport-confirmed/media-delayed from ready unless it consumes repair events
  or feature preflight.
- Gap: runtime identity for endpoint reuse covers `WifiProvisioned` and
  `Syncing`, but not every transport-confirmed HTTP-delayed state.
- Gap: the previous Cerb interface doc lagged the code by omitting
  `audioRecordingNotifications()` and `wifiRepairEvents()`; this sprint syncs it
  as a supporting reference beneath this BAKE contract.
- Gap: `queuedBadgeDownloads` stores filenames globally. Delivered manual sync
  fencing prevents known cross-device imports, but target behavior prefers queue
  items carrying badge identity.
- Gap: `SimBadgeAudioAutoDownloader` fences successful `rec#` imports by
  current runtime badge MAC, but it is not explicitly cancelled by the manual
  queue cancellation path on active-device change.
- Gap: current delivered behavior still lacks fresh hardware evidence for
  reconnect, Wi-Fi repair, isolation, BLE notifications, and HTTP media
  readiness. Do not claim L3 runtime proof without fresh `adb logcat`.

## Test Contract

- Evidence class: contract-test for docs/code alignment; platform-runtime for
  any future hardware claim.
- Existing local coverage includes connectivity bridge tests, connection manager
  ingress/reconnect tests, device registry manager tests, manual sync support
  tests, and `SimBadgeAudioAutoDownloader` tests.
- Minimum verification for this contract: prove the BAKE sections exist, prove
  the Sprint 02 gaps are recorded, prove the Cerb docs and interface map cite
  this contract, and capture `git diff --stat` for the scoped docs.
- Minimum verification for future runtime closure: `adb devices` plus filtered
  `adb logcat` for launch restore, reconnect, Wi-Fi repair, notification ingress,
  HTTP readiness, active-device switch, and badge audio sync/download branches.

## Cross-Platform Notes

- This contract is platform-neutral product truth for the badge connectivity
  and session pipeline. Android code paths are implementation evidence, not a
  native HarmonyOS or iOS delivery prescription.
- Native platform implementations must preserve layered readiness, registered
  badge repair, active-device fencing, and HTTP media preflight semantics.
- Platform-specific BLE, Wi-Fi, background transfer, notification, and logging
  mechanics belong in platform adapters or overlays. Do not claim parity on
  another platform without platform-native runtime evidence.
