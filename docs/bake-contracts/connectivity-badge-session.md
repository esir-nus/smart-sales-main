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
last-verified: 2026-04-30
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
  `Connected` means active registered-badge GATT, active notification listener,
  and usable badge IP/SSID from badge-reported status; it does not prove HTTP
  media readiness.
- `ConnectivityBridge.managerStatus` emits manager-only diagnostics for setup,
  BLE-held network unknown/offline, BLE detected, HTTP-delayed, ready, and
  errors.
- `ConnectivityBridge.isReady()` is the feature-level HTTP media preflight and
  must pass before `/list`, `/download`, `/delete`, auto-sync, or background
  badge media work begins.
- Manual badge sync creates badge-scoped placeholders, queues badge-owned
  download items, imports valid WAV files, removes empty WAV placeholders,
  records failures, and fences results by runtime badge identity before a
  queued item can call `downloadRecording()`.
- `audioRecordingNotifications()` feeds `rec#` auto-download. Auto-download
  creates a queued placeholder immediately, downloads in the background, fences
  successful import by current runtime badge MAC, and sends `Command#end` at
  terminal points.
- Same-badge disconnect while manual queue or `rec#` auto-download work is
  active cancels the current HTTP download job immediately, preserves interrupted
  placeholder availability for HOLD rendering, and records only interrupted
  filenames for targeted resume when the same badge returns to `Ready`.
- `wifiRepairEvents()` exposes repair milestones so UI repair state is driven by
  explicit events instead of inferred manager state.
- `ConnectivityViewModel.registeredBadgeAvailabilityRequests` exposes
  registered BLE proximity availability to `RuntimeShell` separately from
  Wi-Fi mismatch prompts, so the existing `ConnectivityModal` can open as a
  connecting surface or chooser without changing registry ownership.

### Invariants

- MUST keep BLE/session transport readiness separate from HTTP media readiness.
- MUST keep badge media work gated by media-safe readiness: shared `Connected`
  plus HTTP `:8088` readiness on the active runtime endpoint.
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
- MUST treat `download#ready`, `download#ok`, `download#end`, and `wifi#off` as
  ESP32 wire-level protocol facts only. This BAKE contract does not claim that
  the restored Android runtime owns a production media-window runner.
- MUST keep default-device selection passive/cosmetic. `setDefault()` changes
  registry metadata only. BLE detection/reconnect may auto-select only the
  latest user-selected active registered badge after non-manual loss; non-active
  registered badges may be marked `bleDetected` for UI
  proximity but must not become active without explicit user action. Proximity
  is live and per badge MAC: missing scan evidence clears the affected
  registered badge after a short grace window.
- MUST derive launch restore target from the stored active `SessionStore` badge
  first, then from the registered badge with the highest
  `lastUserIntentAtMillis` if session state is missing or stale. Persisted
  registry rows migrate missing `lastUserIntentAtMillis` values by falling back
  to `lastConnectedAtMillis`, while `default` remains non-authoritative for
  automatic reconnect.
- MUST treat successful onboarding pairing, successful add-device pairing, and
  explicit user connect/tap or device switch as the only sources that make a
  badge the latest intended automatic reconnect target.
- MUST treat manual disconnect as explicit user intent: manually disconnecting
  active badge B suppresses auto-reconnect to B and suppresses auto-connect to
  nearby badge A until the user explicitly reconnects or switches.

### Error Paths

- No active or resolvable endpoint: media calls return not-ready or
  not-connected without claiming session invalidation.
- Registered badge reports `IP#0.0.0.0` or no usable network: classify as
  Wi-Fi/media unsafe and route to registered-badge Wi-Fi repair, not BLE
  disconnect, unregistered state, saved-credential replay, or full pairing.
- Solid-IP HTTP failure or `/list` failure: invalidate the active endpoint,
  attempt bounded saved-credential replay when eligible, then re-check media
  readiness.
- `wifi#off` after a badge media protocol sequence: do not treat it as a
  reconnect, repair, saved-credential replay, or disconnect trigger by itself.
- Blank Wi-Fi credentials: reject locally before BLE writes.
- Active-device change during list/download: discard stale list results, cancel
  manual queue work where possible, reject queued cross-device work before it
  calls `downloadRecording()`, and reject cross-device downloads before import.
- Active-device change during active `rec#` auto-download: cancel the outgoing
  badge's active auto-download job before the incoming badge media path can use
  the bridge; do not send `Command#end` for the cancelled outgoing job through
  the incoming badge connection.
- Same active badge `Ready -> Disconnected/Connecting` during download: cancel
  active HTTP work, keep interrupted entries visually held, then requeue only the
  interrupted filenames on same-badge `Ready`; do not treat
  `BlePairedNetworkUnknown`, `BlePairedNetworkOffline`, or `BleDetected` as
  cancellation events.
- Active badge download already running: skip readiness probing and return an
  already-running sync result instead of treating HTTP timeout as Wi-Fi failure.
- BLE detection sees multiple registered candidates: mark each live registered
  candidate as BLE detected, clear missing registered candidates after the
  proximity grace window, reconnect only when the latest user-selected active
  registered badge is one of the eligible candidates, and never switch to a
  non-active/default badge without an explicit user action.
- BLE detection sees eligible registered candidates while no badge is fully
  active: if candidates include the latest active badge, open the connectivity
  modal and reconnect only that latest badge; if candidates exclude the latest
  active badge, open the modal as a chooser and do not auto-connect any badge.
- BLE detection sees only manually disconnected registered badges: do not
  auto-reconnect and do not open the modal by default.

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

- Closed 2026-04-29: `WifiProvisionedHttpDelayed` now maps to
  `BadgeManagerStatus.HttpDelayed`, and connectivity manager UI maps that to
  `ConnectivityManagerState.HTTP_DELAYED`. Shared shell `Connected` remains
  transport-only and separate from HTTP media readiness.
- Closed 2026-04-29: runtime identity for endpoint reuse now covers
  `WifiProvisionedHttpDelayed` as well as `WifiProvisioned` and `Syncing`.
- Gap: the previous Cerb interface doc lagged the code by omitting
  `audioRecordingNotifications()` and `wifiRepairEvents()`; this sprint syncs it
  as a supporting reference beneath this BAKE contract.
- Closed 2026-04-29: `queuedBadgeDownloads` now stores
  `SimBadgeQueuedDownload(filename, ownerBadgeMac)` items. Queue processing
  checks owner MAC before `downloadRecording()` so a stale queued manual
  download fails locally instead of using the new active badge connection.
- Delivered behavior: `SimBadgeAudioAutoDownloader` now tracks active `rec#`
  jobs by `(badgeMac, filename)`, suppresses duplicate active jobs, and exposes
  disconnect cancellation so the repository can include active `rec#` filenames
  in same-badge targeted resume.
- Closed 2026-04-29: `SimAudioRepository` now cancels outgoing active `rec#`
  auto-download jobs on active-device change before cancelling manual badge
  queue work. Focused L1/L2 coverage proves a suspended `rec#` download becomes
  a failed outgoing placeholder and does not send `Command#end` after the badge
  switch. Run 33 L3-debug evidence proves the app-side
  `audioRecordingNotifications()` route starts the outgoing `rec#` job and
  cancels it during active-device switch. Physical L3 for a firmware-emitted
  live `rec#` notification interrupted by a switch remains blocked by hardware
  event availability.
- Closed 2026-04-29: Sprint 04-a L3 evidence now includes direct runtime
  telemetry for transport-confirmed HTTP delay:
  `managerStatus=HttpDelayed(...) managerState=HTTP_DELAYED`, followed by
  saved-credential replay and HTTP-ready recovery. Hardware evidence still
  remains per-branch; do not claim unexercised BLE notification or Wi-Fi repair
  branches without fresh `adb logcat`.
- Superseded 2026-04-29: Sprint 04 default-first BLE priority is
  product-rejected for automatic recovery. The current contract is active-only:
  non-manual disconnect of active badge B must not silently switch or reconnect
  to default badge A. Reconnect fallback scans for B's active session MAC for up
  to 60 seconds and leaves the session on B if only A is found.
- Updated 2026-04-30: post-backup app-side media-runner and fake-media
  blackbox claims are not part of active BAKE truth. Media-gate command names
  remain protocol facts only until a separate safe production design is approved
  and implemented.
- Closed 2026-04-30 (local verification): reconnect-owner persistence now
  uses `lastUserIntentAtMillis` separately from transport-success
  `lastConnectedAtMillis`. Successful onboarding pairing, successful add-device
  pairing, explicit user connect/tap, and explicit device switch are the only
  owner-stamping paths. Launch restore fallback and modal latest-intent choice
  now use `lastUserIntentAtMillis`, and focused/full unit tests pass.
- Closed 2026-04-30 (local verification): manual disconnect suppression now
  holds across both registry-manager auto-reconnect selection and modal
  auto-takeover selection. A manually disconnected badge stays suppressed until
  explicit reconnect/switch; focused/full unit tests pass.
- Closed 2026-04-30 (local verification): when no badge is fully active, the
  reducer now prompts from eligible `bleDetected && !manuallyDisconnected`
  changes, auto-reconnects only the latest intended badge when present, and
  otherwise opens chooser mode with no auto-connect; focused/full unit tests
  pass.
- Closed 2026-04-30 (local verification): active BLE-only cards now stay
  disconnected-first and hide connected-only metadata/actions unless shared
  `BadgeConnectionState.Connected` is live. Focused modal/ViewModel tests pass.
- Closed 2026-04-30 (local verification): stale mismatch/isolation/repair UI
  now clears on active shared `Connected`, active badge identity change from any
  owner, explicit dismiss/reset, and successful repair completion. Focused/full
  unit tests pass.
- Remaining blocker 2026-04-30: Sprint 04-b still lacks fresh L3 loop A-D
  runtime artifacts for these reconnect-law branches. `adb devices` proves the
  attached device is reachable (`fc8ede3e device`), but this session did not
  execute the bounded hardware loop set, so do not claim runtime closure yet.

## Test Contract

- Evidence class: contract-test for docs/code alignment; platform-runtime for
  any future hardware claim.
- Existing local coverage includes connectivity bridge tests, connection manager
  ingress/reconnect tests, device registry manager tests, manual sync support
  tests, and `SimBadgeAudioAutoDownloader` tests.
- Sprint 04-a focused coverage includes:
  `RealConnectivityBridgeTest.managerStatus exposes wifi provisioned http
  delayed separately from ready`,
  `ConnectivityViewModelTest.managerState maps http delayed while shell
  transport remains connected`, and
  `SimAudioRepositorySyncSupportTest.queued badge download owner prevents wrong
  badge download after active switch`, and
  `SimAudioRepositorySyncSupportTest.active device change cancels active rec
  auto download`.
- Minimum verification for this contract: prove the BAKE sections exist, prove
  the Sprint 02 gaps are recorded, prove the Cerb docs and interface map cite
  this contract, and capture `git diff --stat` for the scoped docs.
- Connectivity intent-law delta implementation is now assigned to
  `docs/projects/bake-transformation/sprints/04-b-connectivity-intent-driven-reconnect-modal-law.md`,
  which must close the listed reconnect-owner, modal, active-card, and prompt
  lifetime gaps with focused tests plus L3 evidence.
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
