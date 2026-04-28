# Delivered Behavior Map: connectivity-bridge + badge-session

Sprint: `docs/projects/bake-transformation/sprints/02-connectivity-dbm.md`
Date: 2026-04-28
Lane: develop

This map records delivered Android behavior from current docs and code. It is not a target core-flow rewrite and does not claim fresh hardware proof.

## Current Inputs

- Core behavioral intent comes from `docs/core-flow/badge-connectivity-lifecycle.md` and `docs/core-flow/badge-session-lifecycle.md`; those docs define the north-star readiness ladder, registered-badge repair rules, active-device ownership, and audio sync fencing.
- The supporting implementation contracts are `docs/cerb/connectivity-bridge/spec.md`, `docs/cerb/connectivity-bridge/interface.md`, and `docs/cerb/interface-map.md`. They describe `ConnectivityBridge` as the consumer-facing boundary for BLE + HTTP media work and `DeviceRegistryManager` as active-device owner.
- Public bridge callers use `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt`. Current code exposes connection state, manager state, `/list`, `/download`, `/delete`, `isReady()`, notification streams, command-end signaling, and Wi-Fi repair events.
- Connectivity runtime inputs include `DeviceConnectionManager.state`, `BadgeStateMonitor.status`, foreground network query results, BLE notification events, active endpoint snapshots, session-store credentials, and registered-device state in `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt` and `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt`.
- Active-device inputs are persisted registry rows and session-store material managed by `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt`.
- Audio sync inputs are manual or auto sync triggers, bridge readiness/list/download calls, endpoint runtime keys, registered active device state, queued badge filenames, and SIM audio metadata in `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`, `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryRuntime.kt`, and `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`.
- `rec#` auto-download input is the `audioRecordingNotifications()` stream consumed by `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`.

## Current Outputs

- `RealConnectivityBridge.connectionState` emits shared shell transport states. It maps `WifiProvisioned`, `WifiProvisionedHttpDelayed`, and `Syncing` to `BadgeConnectionState.Connected`; it maps `Connected` and `AutoReconnecting` to shared `Connected` only when `BadgeStateMonitor` reports BLE connected, `BadgeState.CONNECTED`, and a usable badge IP.
- `RealConnectivityBridge.managerStatus` emits richer manager-only states. It can show `Unknown`, `BlePairedNetworkUnknown`, `BlePairedNetworkOffline`, `Connecting`, `Ready`, and `Error`, but current code flattens internal `WifiProvisionedHttpDelayed` into `BadgeManagerStatus.Ready`.
- `ConnectivityBridge.isReady()` returns feature-level HTTP readiness. It resolves a current runtime endpoint, probes HTTP reachability, invalidates stale endpoints, may refresh once, and may run bounded saved-credential replay after solid-IP media failure.
- `listRecordings()`, `downloadRecording()`, and `deleteRecording()` reuse the active runtime endpoint when valid. `/list` failure invalidates the endpoint and may attempt saved-credential replay before retrying once. `/download` and `/delete` invalidate the endpoint on failure.
- `recordingNotifications()` emits full downloadable `log_...wav` names only when transport is ready. `audioRecordingNotifications()` emits `rec#` audio notifications through the same transport-ready gate.
- Battery, firmware-version, SD-card-space, command-end, and voice-volume signaling pass through `DeviceConnectionManager` BLE event/send APIs.
- SIM badge sync creates drawer placeholders, queue entries, progress updates, ready imported files, failed entries, and telemetry through `SimAudioRepositorySyncSupport`.
- `SimBadgeAudioAutoDownloader` creates a queued placeholder immediately on `rec#`, starts the download service, downloads in the background, imports a valid WAV, removes empty WAV placeholders, marks failed downloads, and sends `Command#end` at terminal points.

## State Transitions

- First pairing selects a peripheral, provisions Wi-Fi, persists session and known network, starts the notification listener, enters `ConnectionState.WifiProvisioned`, then starts heartbeat in `DeviceConnectionManagerConnectionSupport`.
- Launch restore loads a stored session in `DeviceConnectionManagerConnectionSupport.restoreSession()` and `RealDeviceRegistryManager.initializeOnLaunch()` seeds the default device session before scheduling auto reconnect when manual-disconnect state allows it.
- Reconnect uses `DeviceConnectionManagerReconnectSupport`. It sets `AutoReconnecting`, snapshots the current session, calls `connectUsingSession()`, and promotes only a `WifiProvisioned` outcome to live state + heartbeat; failure returns to `Disconnected` after recording reconnect metadata.
- `connectUsingSession()` cancels existing heartbeat/notification listener jobs, disconnects old GATT, reconnects or scans fallback, publishes BLE connected, starts notification listening, performs one foreground network query, and resolves either `WifiProvisioned` or a Wi-Fi-disconnected error.
- Foreground network query results update `BadgeStateMonitor`. Usable IP promotes transport readiness; unusable IP records offline and avoids shared `Connected`.
- Manual soft disconnect marks the active registry row manually disconnected, cancels transport jobs, publishes BLE disconnected, preserves session, and sets `Disconnected`.
- Remove/unpair clears session only when the active device is removed or `forgetDevice()` is called. Registered-badge Wi-Fi repair does not remove the registry row.
- Heartbeat failure demotes to `Disconnected`, publishes BLE disconnected, and schedules auto reconnect when allowed.
- Audio sync transitions through readiness preflight, `/list`, placeholder creation, queueing, background download, import/fail/skip, and queue drain. Active-device change cancels queued and active manual badge downloads through `SimAudioRepository.observeActiveDeviceChanges()`.

## Active-Device Ownership

- `docs/core-flow/badge-session-lifecycle.md` says `DeviceRegistryManager` owns the active registered badge, `DeviceConnectionManager` owns connection mechanics, `ConnectivityBridge` owns BLE/HTTP feature operations, and `SimAudioRepository` owns audio inventory/sync/download work.
- `RealDeviceRegistryManager.registerDevice()` registers a paired badge and sets `_activeDevice` to the new row. First device becomes default; later devices remain registered and the newly paired badge becomes active.
- `RealDeviceRegistryManager.switchToDevice()` is mutex-protected. It seeds `SessionStore` for the target device, publishes the target active device, records last-connected, and calls `forceReconnectToSession(targetSession)`.
- `DeviceConnectionManagerConnectionSupport.connectUsingSession()` cancels old heartbeat and notification listener jobs before connecting the target session, so BLE transport jobs are bounded to the current session path.
- `DeviceConnectionManagerReconnectSupport.launchReconnect()` cancels a previous reconnect job before starting the next one.
- `SimAudioRepository` observes `DeviceRegistryManager.activeDevice`. When the MAC changes from a non-null previous value, it calls `cancelAllBadgeDownloads(previousMac)`.
- `SimAudioRepositorySyncSupport` records `ownerBadgeMac` from the endpoint runtime key first and falls back to the active registry row. It discards `/list` results if the runtime MAC changes before queueing, scopes placeholders by `badgeMac`, and checks the runtime MAC again after download success before import.
- Delivered tests in `app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupportTest.kt` cover active-device cancellation, same remote filename scoped per badge, runtime-key ownership when registry active lags, and stale list discard.
- Gap: `SimBadgeAudioAutoDownloader` fences successful `rec#` imports by checking current runtime MAC after download, but it is not cancelled by `SimAudioRepository.cancelAllBadgeDownloads()` because it owns its own launched job rather than the repository queue.

## HTTP Media Readiness

- Core flow separates `Connected` transport readiness from HTTP `:8088` media readiness. Delivered code follows that split for feature work: `isReady()` probes HTTP and SIM sync calls readiness before strict manual/auto sync.
- `RealConnectivityBridge.resolveBaseUrl()` first reuses an active endpoint snapshot when the runtime key still matches and refresh is not required. It can seed from `BadgeStateMonitor` when connected with usable IP, otherwise it performs a foreground network query.
- Endpoint snapshots are in-memory runtime state. The code does not persist a last-known badge IP across app sessions; `lastKnownBadgeIp` is volatile process memory used for isolation probing.
- `isReady()` checks reachability, invalidates the endpoint on HTTP miss, retries once after refresh, skips media recovery if no usable IP candidate exists, and otherwise attempts saved-credential replay before post-credential readiness probes.
- `/list` uses the same endpoint path. On first `/list` error it invalidates the endpoint, runs the saved-credential media-failure replay path, resolves again, and retries `/list` once.
- `/download` returns `NOT_CONNECTED` when no base URL can be resolved; HTTP errors map to `FILE_NOT_FOUND` or `DOWNLOAD_FAILED` and invalidate the endpoint.
- `SimAudioRepositorySyncSupport` arms a known-HTTP-unreachable latch when likely connectivity failures happen and suppresses later auto sync for the same runtime endpoint until runtime or endpoint changes.
- Gap: `BadgeManagerStatus.Ready` currently also represents internal `WifiProvisionedHttpDelayed`; docs say this must not be treated as media readiness, but manager presentation does not expose a distinct public HTTP-delayed state.

## Wi-Fi Repair

- `RealConnectivityService.updateWifiConfig()` trims SSID/password and rejects blank values before BLE writes.
- Manual repair obtains the current or stored session, writes credentials through `WifiProvisioner.provision()`, persists session/known network on write success, then calls `DeviceConnectionManager.confirmManualWifiProvision(credentials)`.
- `DeviceConnectionManagerConnectionSupport.confirmManualWifiProvision()` cancels auto retry, waits for bounded manual confirmation, and either starts heartbeat for transport-confirmed outcomes or returns to disconnected/error.
- `waitForManualProvisionOnline()` emits `WifiRepairEvent` milestones, performs up to 3 confirmation attempts, treats unusable IP as offline, verifies badge SSID against the submitted SSID when both are known, treats usable IP plus matching SSID as transport confirmed, then probes HTTP.
- If HTTP is reachable, manual repair returns `WifiProvisioned`. If transport is confirmed but HTTP stays unreachable at the final attempt, it persists credentials and returns `WifiProvisionedHttpDelayed`, which `RealConnectivityService` maps to `WifiConfigResult.TransportConfirmedHttpDelayed`.
- Reconnect/media failure credential replay uses latest saved credentials, is bounded to 3 attempts, and is not used for `IP#0.0.0.0`; the offline branch routes to Wi-Fi repair instead.
- `ConnectivityViewModel` consumes `wifiRepairEvents()` for UI progress states and `ConnectivityPromptCoordinator` can route suspected isolation or mismatch prompts into the connectivity surface.
- Gap: SIM manual sync still calls `promptWifiMismatch()` with `phoneWifiProvider.currentNormalizedSsid()` as a UI hint, even though core recovery decisions should not depend on phone SSID. The implementation path treats it as presentation input, not a connectivity proof.

## Audio Sync

- Manual and auto badge sync enter `SimAudioRepositorySyncSupport.syncFromBadgeInternal()`. Strict preflight calls `canSyncFromBadge()`, which calls `ConnectivityBridge.isReady()` unless a badge download is already active.
- Auto sync is skipped when known HTTP unreachable is latched for the current endpoint, when readiness fails, or when a download is already active.
- Successful `/list` results are normalized, deduplicated, filtered against existing badge files and pending remote deletes, and scoped by `badgeMac`.
- Placeholders are created as `AudioSource.SMARTBADGE` with `AudioLocalAvailability.QUEUED` and a `badgeMac`. Queue processing marks entries `DOWNLOADING`, updates progress, imports valid WAV files as `READY`, drops sub-1KB WAVs as empty, and records failures.
- Pending badge deletes are reconciled before queueing and can trigger remote `deleteRecording()`.
- Active-device fencing exists at three points: active-device flow cancellation, stale list discard when runtime MAC changes after `/list`, and post-download MAC check before import.
- `rec#` auto-download creates/imports badge audio outside the manual sync queue but tags entries with `ownerBadgeMac` and discards successful cross-device downloads before import.
- Gap: the manual queue stores filenames globally in `queuedBadgeDownloads`; delivered fencing prevents known cross-device downloads, but the queue item itself does not carry MAC ownership. The core-flow notes this as a minimal implementation direction rather than the stronger final shape.

## Telemetry

- Connectivity runtime uses `ConnectivityLogger` and `AudioPipeline` log tags for reconnect, network query, endpoint resolution, HTTP readiness, saved-credential replay, repair, notification drops, and download failures.
- `SimAudioRepositorySyncSupport` emits `PipelineValve.Checkpoint.UI_STATE_EMITTED` telemetry for badge sync requested, badge sync completed, and connectivity-unavailable sync failures.
- SIM badge sync logs include trigger, readiness, list count, badge MAC, queue counts, failures, known-HTTP-unreachable latch arm/clear, and background queue progress.
- Wi-Fi repair event flow emits structured milestones: credentials dispatched, usable IP observed, target SSID observed, transport confirmed, HTTP ready, HTTP delayed, definitive mismatch, badge offline, and credential replay failure.
- Existing unit tests provide local evidence for active-device fencing and sync paths, but this sprint did not run hardware. No fresh `adb logcat` proof is claimed.

## Known Gaps

- Hardware evidence gap: current delivered behavior for reconnect, Wi-Fi repair, isolation, BLE notifications, and HTTP media readiness still needs fresh device/emulator `adb logcat` evidence before claiming L3 runtime proof. This map is docs/code evidence only.
- Public docs lag code: `docs/cerb/connectivity-bridge/interface.md` does not list `audioRecordingNotifications()` or `wifiRepairEvents()`, while `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt` exposes both.
- Manager state gap: `WifiProvisionedHttpDelayed` is flattened to `BadgeManagerStatus.Ready` in `RealConnectivityBridge.mapToManagerStatus()`, so UI consumers cannot distinguish transport-ready/HTTP-delayed from manager `Ready` unless they consume repair events or feature preflight.
- Runtime-key scope gap: `RealConnectivityBridge.runtimeKeyForState()` only returns a key for `WifiProvisioned` and `Syncing`, not `WifiProvisionedHttpDelayed`; endpoint reuse and ownership logic may be more conservative during HTTP-delayed state.
- `RealDeviceRegistryManager.seedSessionForDevice()` creates a new secure token when seeding a registry device rather than restoring a previously persisted per-device token. Current runtime behavior may still work because MAC is the physical reconnect target, but the session identity is not a durable per-device token.
- `SimBadgeAudioAutoDownloader` is fenced after download success but not explicitly cancelled on active-device change by the repository queue cancellation path.
- Manual sync's `promptWifiMismatchIfManual()` still passes current phone SSID as suggested copy. This is acceptable only as a UI hint; it must not be interpreted as proof of phone/badge network topology.
- Local tests exist for many active-device and sync branches, but this sprint did not rerun the full unit suite because the contract required evidence commands only and the work was docs-only.
