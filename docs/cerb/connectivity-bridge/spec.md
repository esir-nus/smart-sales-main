# Connectivity Bridge

> **Cerb-compliant spec** — Prism-safe wrapper for legacy BLE + HTTP connectivity.
> **OS Layer**: Infrastructure (Layer 1) — Leaf service, no upstream dependencies
> **Status**: Supporting reference beneath BAKE
> **Last Updated**: 2026-04-30
> **BAKE Implementation Contract**: [`docs/bake-contracts/connectivity-badge-session.md`](../../bake-contracts/connectivity-badge-session.md)
> **Connectivity Lifecycle Authority Above This Doc**: [`docs/core-flow/badge-connectivity-lifecycle.md`](../../core-flow/badge-connectivity-lifecycle.md)
> **Behavioral UX Authority Above This Doc**: [`docs/core-flow/base-runtime-ux-surface-governance-flow.md`](../../core-flow/base-runtime-ux-surface-governance-flow.md) (`UX.CONNECTIVITY.*`)

---

## Overview

Connectivity Bridge provides a **thin, Prism-compatible interface** to legacy `feature:connectivity` module. Preserves ESP32 rate limiting and business logic while exposing clean interfaces for Prism domain code. After Sprint 04 of the BAKE transformation, this Cerb document is a supporting/reference discovery doc; the BAKE contract is the authoritative implementation record for the connectivity-bridge plus badge-session corridor.

**Key Principle**: Prism domain code MUST NOT import from `feature:connectivity` directly. All connectivity access goes through this bridge.

Connectivity Bridge implements app lifecycle semantics beneath `docs/core-flow/badge-connectivity-lifecycle.md`. The ESP32 protocol spec owns wire facts; this spec owns app state projection, foreground reconnect/repair behavior, endpoint reuse, and feature-level HTTP readiness.

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [Badge Audio Pipeline](../badge-audio-pipeline/interface.md) | Orchestrates recording → transcription → scheduler |
| [ASR Service](../asr-service/interface.md) | Audio transcription via FunASR |

---

## ESP32 Protocol Reference

### BLE Commands (Preserved)

| Command | Request | Response | Notes |
|---------|---------|----------|-------|
| **WiFi Query** | `wifi#address#ip#name` | `IP#<IP>` + `SD#<SSID>` (2 fragments) | Network status (fragmented) |
| **WiFi Connect** | `SD#<ssid>` → `PD#<password>` | (connects) | Two-step protocol |
| **WAV Get** | `wav#get` | `wav#send` | Initiates download |
| **WAV End** | `wav#end` | `wav#ok` | Completes download |
| **Time Sync** | Badge: `tim#get` | App: `tim#YYYYMMDDHHMMSS` | ESP32 clock calibration |
| **Recording Log** | Badge: `log#YYYYMMDD_HHMMSS` | (none) | Recording ready + filename |

### HTTP Endpoints (Port 8088)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/list` | GET | List WAV files on SD card |
| `/download?file=xxx` | GET | Download specific WAV file |
| `/delete` | POST | Delete WAV file from SD card |

---

## ESP32 Traffic Policy (PRESERVED)

> [!CAUTION]
> **ESP32 overload protection**. Do NOT remove or weaken these limits.

| Parameter | Value | Enforcement |
|-----------|-------|-------------|
| **BLE query floor** | 2s | `RateLimitedBleGateway.MIN_QUERY_INTERVAL_MS` |
| **Foreground query mode** | Event-driven only | connect/reconnect/setup/manual repair/explicit refresh |
| **HTTP endpoint reuse** | Current runtime only | `RealConnectivityBridge` active endpoint snapshot |
| **Inter-command gap** | 300ms | BLE command spacing |

**Offline detection**: `wifi#address#0.0.0.0#...` indicates badge has no WiFi. This is a valid offline signal and must not be treated as a parser failure.

`/list`, `/download`, and `/delete` remain valid normal HTTP traffic. The overload signature to avoid is repeated BLE `wifi#address#ip#name` querying in the background.

## Formal On-Device Debug Path

Use `scripts/esp32_connectivity_debug.sh` for live ESP32 capture and post-run summary.

Authoritative workflow:

1. run `scripts/esp32_connectivity_debug.sh capture`
2. reproduce the badge-side action
3. stop capture
4. run `scripts/esp32_connectivity_debug.sh summary <captured_log>`

Reference SOP:

- `docs/sops/esp32-connectivity-debug.md`

Active issue tracker:

- `docs/projects/connectivity-bug-triage/tracker.md`

Current known blocked hardware signature:

- BLE traffic is present
- network query fragments may still arrive
- no `log#...`
- no `Badge recording ready`
- no `AudioPipeline` ingress

That signature currently points upstream of the app pipeline rather than proving a bridge parsing failure.

---

## Domain Models

### BadgeConnectionState

```kotlin
sealed class BadgeConnectionState {
    object Disconnected : BadgeConnectionState()
    object NeedsSetup : BadgeConnectionState()   // 无 session，需要初始配网
    object Connecting : BadgeConnectionState()
    data class Connected(
        val badgeIp: String,
        val ssid: String
    ) : BadgeConnectionState()
    data class Error(val message: String) : BadgeConnectionState()
}
```

`BadgeConnectionState.Connected` means shared transport readiness for shell/history routing: active registered-badge GATT, active notification listener, and usable badge IP/SSID from badge-reported status. It does not mean HTTP `:8088` is reachable.

### BadgeManagerStatus

```kotlin
sealed class BadgeManagerStatus {
    object Unknown : BadgeManagerStatus()
    object NeedsSetup : BadgeManagerStatus()
    object Disconnected : BadgeManagerStatus()
    object Connecting : BadgeManagerStatus()
    object BlePairedNetworkUnknown : BadgeManagerStatus()
    object BlePairedNetworkOffline : BadgeManagerStatus()
    data class Ready(val badgeIp: String? = null, val ssid: String? = null) : BadgeManagerStatus()
    object BleDetected : BadgeManagerStatus()
    data class Error(val message: String) : BadgeManagerStatus()
}
```

`BadgeConnectionState` remains the strict shared transport contract.
`BadgeManagerStatus` is a manager-only refinement layer used for BLE/Wi‑Fi diagnostics in connectivity surfaces without changing shell/history routing.

State meanings must not overlap:

| State | Meaning | Must Not Imply |
|-------|---------|----------------|
| `BleDetected` | A registered badge was detected by BLE scanning. | GATT, notification listener, Wi-Fi, or HTTP readiness. |
| `BlePairedNetworkUnknown` | BLE is held and the app is waiting for usable badge network evidence. | Offline or HTTP failure. |
| `BlePairedNetworkOffline` | BLE is held, but badge Wi-Fi has no usable IP/network. | Session invalidation, registry removal, or full pairing. |
| `Connected` | Shared transport readiness: registered-badge GATT + listener + usable badge IP/SSID. | HTTP media readiness. |
| internal `ConnectionState.WifiProvisionedHttpDelayed` | Badge transport confirmed, but HTTP `:8088` is still delayed/unreachable inside bounded grace. | Full feature readiness for `/list`, `/download`, or `/delete`. |
| `Ready` | Manager transport-ready state after BLE/listener/network status are usable. | That `isReady()` has passed for media work. |
| `isReady()` | Feature preflight for HTTP media operations on the active runtime endpoint. | Global connection state changes or pairing state. |

For the current reconnect contract, shared `BadgeConnectionState.Connected` may resume as soon as persistent GATT is active and the badge reports a usable IP / transport-ready network result. HTTP file operations still keep their own endpoint validation path through `ConnectivityBridge.isReady()`.

### RecordingNotification

```kotlin
sealed class RecordingNotification {
    /**
     * Badge finished recording, file is ready for download.
     * Triggered by `log#YYYYMMDD_HHMMSS` BLE command from firmware.
     */
    data class RecordingReady(
        val filename: String  // log_YYYYMMDD_HHMMSS.wav
    ) : RecordingNotification()
}
```

### WavDownloadResult

```kotlin
sealed class WavDownloadResult {
    data class Success(
        val localFile: File,
        val originalFilename: String,
        val sizeBytes: Long
    ) : WavDownloadResult()
    
    data class Error(
        val code: ErrorCode,
        val message: String
    ) : WavDownloadResult()
    
    enum class ErrorCode {
        NOT_CONNECTED,
        BADGE_OFFLINE,
        FILE_NOT_FOUND,
        DOWNLOAD_FAILED,
        DISK_FULL
    }
}
```

---

## UI Components

> **Note**: UI components wired in Wave 2.5. See Implementation Status below.

### ConnectivityModal

**File**: [`ui/components/ConnectivityModal.kt`](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt)

**Trigger**: User taps 🔗 Bluetooth icon in top bar.

#### States

| State | Visual | Actions |
|-------|--------|---------|
| `CONNECTED` | ✅ Green pulsing BT icon<br>Active device header (name + MAC suffix + battery)<br>Registered device list | **⚡ 断开连接**<br>**🔄 检查更新**<br>Device list: **切换** / **重命名** / **设为默认** / **移除** |
| `DISCONNECTED` | ⚫ Gray BT icon<br>"🔴 离线" | **重试连接** |
| `BLE_PAIRED_NETWORK_UNKNOWN` | 🔵 BT icon<br>"已连接设备"<br>"蓝牙已连接，正在确认设备网络状态" | **重试连接** |
| `BLE_PAIRED_NETWORK_OFFLINE` | 🔵 BT icon<br>"已连接设备"<br>"蓝牙已连接，但设备当前未接入可用网络" | **重试连接** |
| `NEEDS_SETUP` | ⚠️ Amber Warning icon<br>"⚙️ 设备未配网" | **📱 开始配网** → Onboarding |
| `RECONNECTING` | Spinner | (Auto) |
| `CHECKING_UPDATE` | Spinner |  (Auto) |
| `UPDATE_FOUND` | 📥 Amber icon<br>"发现新版本" | **立即同步**<br>**稍后** |
| `UPDATING` | Progress bar | (Auto) |
| `WIFI_MISMATCH` | ⚠️ Warning<br>WiFi form or isolation repair prompt | **忽略**<br>**更新配置**<br>Isolation only: **修复 Wi-Fi 配置** |

> [!NOTE]
> After user taps "断开连接", soft disconnect preserves session.
> Shared shell routing still treats that as `DISCONNECTED`.
> `NEEDS_SETUP` only shows on cold boot (no session ever existed).
> Tapping "重试连接" from a disconnected manager state triggers auto-reconnect using persisted session.
> Tapping `开始配网` from `NEEDS_SETUP` still enters the full shared onboarding coordinator with `host = SIM_CONNECTIVITY`.
> Tapping the manager `添加设备` action enters the streamlined add-device coordinator with `host = SIM_ADD_DEVICE`, reusing pairing/provisioning runtime while skipping intro handshake, scheduler quick start, and the onboarding completion wrapper. The route closes back to the connectivity-owned surface only after successful provisioning/registration.
> Tapping "更新配置" from `WIFI_MISMATCH` first performs local validation plus an explicit send-confirm dialog; only the confirmed valid submit enters reconnect/progress and runs the repair flow without requiring a second manual retry.
> Tapping "修复 Wi-Fi 配置" from a suspected-isolation prompt for an already registered active badge enters the same `WIFI_MISMATCH` credential form. It must keep the active registry row and must not navigate to add-device pairing.
> The richer `BLE_PAIRED_NETWORK_*` states are manager-only refinements derived from `ConnectivityBridge.managerStatus`; they must not redefine global transport readiness.
> Internal `WifiProvisionedHttpDelayed` is transport-confirmed/media-delayed. It must not be flattened into feature readiness when the UI is communicating whether audio sync can start. If a future UI needs a direct manager state, add it in code and sync this spec/interface in the same sprint.
> Debug builds may additionally expose a temporary `断开连接` action in `BLE_PAIRED_NETWORK_*` states for hardware testing convenience; that control must not be treated as a release-surface contract.
> Closing the connectivity modal/manager clears transient reconnect or mismatch override state so later reopen reflects live manager truth rather than a retained stale repair screen.
> Supporting docs beneath core-flow and BAKE do not bless stale reconnect, mismatch, or isolation UI after live transport or active badge identity has changed. Those prompts must clear on active shared `Connected`, any active badge identity change, explicit dismiss/reset, or successful repair completion. Sprint 04-b records the remaining implementation delta where current code still lags this law.
> The modal now renders a frosted glass overlay (matching `SimHomeHeroTokens`) with active device header, registered device list, and per-device management actions. `ConnectivityViewModel` sources device state from `DeviceRegistryManager.registeredDevices` and `DeviceRegistryManager.activeDevice`. Inline rename uses a `DeviceHeader` component with editable text field. Device switch is mutex-protected and routes through `DeviceRegistryManager.switchToDevice()`.
> Device cards are ordered for wayfinding stability: the default registered badge is pinned first, all non-default badges follow the successful-pairing timeline using `RegisteredDevice.registeredAtMillis` ascending, and `macAddress` breaks ties. Active selection is visual only through the selected card border and active-card content; switching active badges or updating `lastConnectedAtMillis` must not move the card.
> Registered-badge availability prompts are separate from Wi-Fi mismatch prompts. When no badge is fully active, any eligible registered-badge availability change deterministically opens the existing `ConnectivityModal`. Automatic opens use a distinct entrance treatment and auto-reminder intro card so the interruption is recognizable, while manual opens keep the standard manager presentation. If the latest intended active badge is among detected eligible candidates, only that badge may auto reconnect and the modal shows it as connecting. If the latest intended active badge is absent, the modal opens as a chooser and waits for an explicit card tap.

#### Data Flow

``` 
User Action → ConnectivityViewModel → ConnectivityService → ConnectivityBridge → Legacy
```

Multi-device management path:

```
ConnectivityViewModel → DeviceRegistryManager → DeviceRegistry (SharedPrefs)
                                              → SessionStore (session seeding)
                                              → DeviceConnectionManager (reconnect)
```

BLE detection monitor priority:

- passive registered-MAC scanning marks each registered badge's own `bleDetected` proximity flag independently
- a registered badge missing from scan evidence clears `bleDetected=false` after a short grace window so stale ready copy does not stick
- only the latest intended active registered badge may trigger automatic reconnect after non-manual loss such as distance loss or power cut
- when passive or direct BLE candidates include a registered non-active badge, the registry logs `non-active BLE candidates marked only; active remains <activeMac>` with `source=`, `candidates=`, and `nonActiveCandidates=` on the same line for L3 evidence
- latest intended active badge selection is seeded only by successful onboarding pairing, successful add-device pairing, explicit user connect/tap, or explicit device switch; launch fallback uses `SessionStore` first, then the latest intended active ownership record, never `default`
- non-active registered badges, including the default badge, remain proximity/card state only until the user manually taps/switches to them
- a `manuallyDisconnected=true` badge is skipped for automatic reconnect
- `setDefault()` itself remains cosmetic/passive and must not switch active device, reseed `SessionStore`, reconnect, or override the latest intended active badge
- manual disconnect of active badge B suppresses both auto-reconnect to B and auto-connect to nearby badge A until explicit user action

Device-card proximity copy is secondary detail only. An active badge that is BLE-detected but not transport-ready remains primarily disconnected; optional subtitle copy may hint that it was detected and can be retried, but must not replace disconnected-first state or connected transport metadata. Non-active BLE-detected badges remain chooser-only.

Connectivity modal card presentation must be reduced from one live snapshot of shared `BadgeConnectionState`, manager diagnostics, active device, sorted registered devices, BLE proximity, battery, firmware, and transient UI override. The active card may render `已连接`, connected dot, battery/firmware metadata, `断开连接`, `检查更新`, or update affordances only when that same snapshot has shared `BadgeConnectionState.Connected`. A lagging `BadgeManagerStatus.Ready`, battery notification, firmware notification, checking-update state, or update state must not by itself render connected UI when shared state is disconnected, error, setup, BLE-only, paired-network-offline/unknown, or HTTP-delayed.

Registered availability prompt flow:

```
DeviceRegistryManager.registeredDevices + activeDevice + ConnectivityBridge.connectionState
    → ConnectivityViewModel.registeredBadgeAvailabilityRequests
    → RuntimeShell opens ConnectivityModal
```

Prompt rules:

- any eligible availability change while no badge is fully active: emit prompt through the existing modal rather than silently waiting for a manual open
- latest intended active badge detected and not manually disconnected: emit prompt, show that latest badge as reconnecting, and reconnect only that badge
- latest intended active badge absent but another non-manually-disconnected registered badge detected: emit prompt as chooser; no auto-connect
- only manually disconnected badges detected: do not emit availability prompt
- default badge detection must not reseed `SessionStore`, switch `activeDevice`, or start reconnect by itself

Manager-only refinement path:

```
BadgeStateMonitor.status + DeviceConnectionManager.state
    → ConnectivityBridge.managerStatus
    → ConnectivityViewModel.managerState
    → ConnectivityModal / ConnectivityManagerScreen
```

Reconnect rule:

- reconnect must feed `BadgeStateMonitor` immediately after persistent GATT reconnect succeeds
- reconnect must publish the foreground network-query result into the monitor synchronously
- `ConnectivityBridge.isReady()` may retry once after an HTTP reachability miss by invalidating the active endpoint and forcing a fresh BLE network-status resolution; if a usable badge IP/SSID still exists and HTTP media readiness remains unavailable, it may trigger one bounded latest-saved-credential replay sequence before returning not-ready.
- `ConnectivityBridge.isReady()` is the feature-level gate for `/list`, `/download`, and `/delete`; it must not be treated as the definition of shared `Connected`
- manager-only BLE/Wi‑Fi diagnostics must not wait for a background poll tick to become visible
- reconnect/setup/manual repair may use bounded foreground confirmation loops, but the runtime must not resume continuous BLE Wi‑Fi polling after those checks complete
- connectivity UI must treat reconnect and manual Wi‑Fi repair as one exclusive foreground operation so duplicate taps do not stack concurrent repair attempts

Reconnect credential rule:

- target firmware stores one last-working Wi‑Fi credential and auto-reconnects itself after reconnect / power recovery
- app-side `SessionStore` still owns the multi-network remembered Wi‑Fi list
- app may store multiple known networks, keyed by exact normalized SSID
- phone current SSID is not a supported decision input because OEM restrictions make it unavailable or non-authoritative
- when BLE reconnect succeeds, recovery is driven by badge-reported network status plus remembered credential history:
  - if the badge reports `IP#0.0.0.0`, skip silent credential replay and route the manager UI to `WIFI_MISMATCH` with editable Wi-Fi credentials
  - if the badge reports a usable IP and the same SSID as the latest saved credential, treat transport as confirmed and validate HTTP separately
  - if the badge reports a usable IP on a different SSID than the latest saved credential, route the manager UI to `WIFI_MISMATCH` with the saved credential as an editable hint
  - if no saved credential exists, route the manager UI to `WIFI_MISMATCH` with an empty editable form
  - if HTTP `:8088` is unreachable after usable IP/SSID or `/list` fails on the active endpoint, silently replay the latest saved user-confirmed credential up to 3 ESP32-safe attempts, then re-check media readiness
  - if saved replay fails, route to `WIFI_MISMATCH` with copy that hints the user may have switched networks
- reconnect-driven and isolation-driven `WIFI_MISMATCH` should prefill from latest saved user-confirmed credentials when available, not from phone SSID
- the badge and phone may share the same Wi‑Fi credential/SSID while receiving different subnet addresses or AP/client-isolated routing; in that case credential repair is not the root fix even though BLE and Wi‑Fi credentials are valid
- when the user submits manual SSID/password from `WIFI_MISMATCH`, the UI must trim both fields, reject blank values locally, require an explicit confirmation dialog, and only then show reconnect/progress while `updateWifiConfig()` runs its provision-plus-confirm repair path
- manual repair must not fall back into the generic reconnect path after sending credentials
- manual repair confirmation must:
  - keep the current BLE session
  - poll badge network status with the same bounded tolerance used by setup (`3` attempts, `1500ms` interval)
  - validate against the user-submitted SSID
- the manual repair form should prefill the latest saved user-confirmed SSID when available, but keep the SSID editable and keep password blank/manual unless a deliberate credential-history reuse UI is added
- empty or whitespace-only SSID/password must never be sent to the badge from the manual repair flow
- invalid manual repair input must stay on `WIFI_MISMATCH`, surface a local error message, and must not transition into reconnect/progress
- manual repair may return to `WIFI_MISMATCH` only when the badge proves it came online on a different SSID than the submitted one
- manual repair must not route back to `WIFI_MISMATCH` merely because phone Wi‑Fi is unreadable/unavailable or because HTTP is unreachable after badge transport is confirmed
- if the user closes the connectivity surface mid-repair, only the transient override is cleared; underlying bridge/manager truth remains authoritative on next reopen
- reconnect foreground network query must tolerate the BLE 2s minimum query floor and retry after the guard delay instead of failing immediately on a throttle timeout
- badge IP is reused only as an in-memory active runtime endpoint snapshot; the app must not persist a last-known badge IP across sessions
- HTTP sync/delete flows should reuse that active runtime endpoint instead of re-querying BLE Wi‑Fi status before every `/list` / `/download` / `/delete`
- shared transport readiness stays strict: BLE-held + Wi‑Fi-offline is not `Connected`

#### Implementation Status

| Component | Status | Wave |
|-----------|--------|------|
| `ConnectivityModal.kt` | ✅ Shipped | Pre-existing + Wave 2.5 + Multi-device rewrite |
| `ConnectivityViewModel.kt` | ✅ Shipped | Pre-existing + Wave 2.5 + DeviceRegistryManager integration |
| `ConnectivityService` (interface) | ✅ Shipped | Pre-existing |
| `RealConnectivityService` | ✅ Shipped | Wave 2.5 |
| `ConnectivityBridge` | ✅ Shipped | Wave 2 |
| `DeviceRegistry` + `SharedPrefsDeviceRegistry` | ✅ Shipped | Multi-device |
| `DeviceRegistryManager` + `RealDeviceRegistryManager` | ✅ Shipped | Multi-device |
| `DeviceRegistryModule` (Hilt DI) | ✅ Shipped | Multi-device |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Interface + Fake | ✅ SHIPPED | `ConnectivityBridge` interface, `FakeConnectivityBridge` |
| **2** | Real Implementation (Backend) | ✅ SHIPPED | `RealConnectivityBridge` wrapping legacy |
| **2.5** | UI Wiring | ✅ SHIPPED | `RealConnectivityService`, DI binding, modal integration, `NeedsSetup` routing |
| **2.7** | Session Persistence + Soft Disconnect | ✅ SHIPPED | `BaseRuntimeOnboardingGate`, `SessionStore`, `disconnectBle()`, `unpair()` |
| **3** | Recording Log Handler | ✅ SHIPPED | `log#YYYYMMDD_HHMMSS` BLE listener, notification flow |
| **4** | Battery Level Reporting | 🔲 | Real BLE battery characteristic (pending hardware) |

---

## Wave 1 Ship Criteria

**Goal**: Prism-safe interface that compiles without legacy imports in domain layer.

- **Exit Criteria**:
  - [x] `ConnectivityBridge` interface in `domain/connectivity/`
  - [x] `FakeConnectivityBridge` returns mock data
  - [x] Zero imports from `feature:connectivity` in Prism domain
  - [x] Build passes: `./gradlew :app-core:compileDebugKotlin`

- **Test Cases**:
  - [x] Fake returns Connected state
  - [x] Fake simulates download with 500ms delay
  - [x] Fake emits RecordingReady notification

---

## Wave 2 Ship Criteria

**Goal**: Real connectivity via legacy wrapper.

- **Exit Criteria**:
  - [x] `RealConnectivityBridge` delegates to `DeviceConnectionManager`
  - [x] Rate limiting enforced at bridge level (via legacy `RateLimitedBleGateway`)
  - [ ] WAV download works end-to-end (L2 test pending)

- **Test Cases**:
  - [ ] L2: Connect to real badge, query status
  - [ ] L2: Download WAV file, verify contents
  - [ ] Rate limit: Rapid queries throttled correctly

---

## Wave 2.5 Ship Criteria

**Goal**: Wire existing UI to RealConnectivityBridge.

- **Exit Criteria**:
  - [x] `RealConnectivityService` implemented
  - [x] DI binding: `ConnectivityService` → `RealConnectivityService`
  - [x] `ConnectivityModal` displays correct badge info
  - [x] Disconnect/Reconnect buttons work
  - [x] `NeedsSetup` state routes to onboarding
  - [x] Disconnect pins UI to `DISCONNECTED` (prevents NeedsSetup flash)

- **Test Cases**:
  - [ ] L2: Tap 🔗 icon → modal shows real badge data
  - [ ] L2: Disconnect → modal shows "离线" (not "设备未配网")
  - [ ] L2: Reconnect → badge reconnects or routes to setup
  - [ ] L2: Cold boot → modal shows "设备未配网" → "开始配网" → onboarding


---

## Wave 2.7 Ship Criteria

**Goal**: Session persistence + soft disconnect — reconnection without re-onboarding.

### Problem Summary

1. **Onboarding reset on app restart**: the old split-era root used ephemeral UI state → not persisted across process death → always showed onboarding
2. **Session lost on disconnect**: `DeviceConnectionManager` stored BLE session in-memory only → lost after app restart → reconnection impossible
3. **Disconnect nuked session**: `disconnect()` called `forgetDevice()` → cleared session → forced full re-onboarding

### Solution

| Fix | Component | Implementation |
|-----|-----------|----------------|
| **1. Onboarding Gate** | `BaseRuntimeOnboardingGate` | SharedPreferences-backed `StateFlow<Boolean>`, survives process death and now bridges the former split-era gate flags |
| **2. Session Store** | `SessionStore` interface + impls | Persists `BleSession` + `WifiCredentials` to SharedPrefs (production) or memory (tests) |
| **3. Soft Disconnect** | `disconnectBle()` vs `forgetDevice()` | `disconnectBle()` keeps session (reconnect possible), `forgetDevice()` clears (requires onboarding) |

### Semantics

#### DeviceConnectionManager (Connectivity Module)

| Method | Heartbeat | Session | Credentials | Next Reconnect |
|--------|-----------|---------|-------------|----------------|
| `disconnectBle()` | ❌ Cancel | ✅ Keep | ✅ Keep | Auto-reconnect possible |
| `forgetDevice()` | ❌ Cancel | ❌ Clear | ❌ Clear | Requires full onboarding |

#### ConnectivityService (Prism Layer)

| Method | Calls | Use Case |
|--------|-------|----------|
| `disconnect()` | `deviceManager.disconnectBle()` | User taps "断开连接" |
| `unpair()` | `deviceManager.forgetDevice()` | User explicitly unpairs device (future) |

### Exit Criteria

- **Exit Criteria**:
  - [x] `BaseRuntimeOnboardingGate` with SharedPreferences persistence
  - [x] `SessionStore` interface + `SharedPrefsSessionStore` + `InMemorySessionStore`
  - [x] `DefaultDeviceConnectionManager` loads session on init, saves on provisioning success, clears on unpair
  - [x] `disconnectBle()` added to `DeviceConnectionManager` interface
  - [x] 5 fake implementations updated (canonical + 4 inline test fakes)
  - [x] `ConnectivityService.unpair()` added for hard disconnect
  - [x] `RealConnectivityService.disconnect()` calls `disconnectBle()`
  - [x] UI override workaround removed from `ConnectivityViewModel`
  - [x] Build passes: `:app-core:testDebugUnitTest` (23/23) + `:app-core:compileDebugKotlin`

- **Test Cases**:
  - [x] Unit: `DefaultDeviceConnectionManagerTest` with `InMemorySessionStore`
  - [ ] L2: Fresh install → onboarding shown
  - [ ] L2: Complete onboarding → force-close app → relaunch → **RuntimeShell shown (not onboarding)**
  - [ ] L2: Disconnect → reconnect → **reconnects without onboarding**

### Files Changed

**New** (3):
- `app-core/src/main/java/com/smartsales/prism/data/onboarding/BaseRuntimeOnboardingGate.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/SessionStore.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/SessionStoreImpl.kt`

**Modified** (11):
- `app-core/src/main/java/com/smartsales/prism/MainActivity.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/ConnectivityModule.kt`
- `app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/DefaultDeviceConnectionManagerTest.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/FakeDeviceConnectionManager.kt`
- `feature/media/src/test/java/com/smartsales/feature/media/devicemanager/DeviceManagerViewModelTest.kt`
- `app/src/test/java/com/smartsales/aitest/audio/DeviceHttpEndpointProviderImplTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/setup/DeviceSetupViewModelTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/setup/DeviceSetupViewModelRobustnessTest.kt`
- `app-core/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityService.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityService.kt`
- `app-core/src/main/java/com/smartsales/prism/data/fakes/FakeConnectivityService.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`


---

## Wave 3 Ship Criteria

**Goal**: Automatic recording detection via BLE `log#` notification.

**Implementation**: BLE notification listener from `DeviceConnectionManager.recordingReadyEvents` → `ConnectivityBridge.recordingNotifications()` → `BadgeAudioPipeline`.

- **Exit Criteria**:
  - [x] BLE `log#YYYYMMDD_HHMMSS` notification listener on persistent GATT session
  - [x] `RecordingNotification.RecordingReady` emitted with full downloadable filename `log_YYYYMMDD_HHMMSS.wav`
  - [x] Only fires when transport is truly ready (`WifiProvisioned` / `Syncing`)
  - [x] HTTP polling removed (obsolete)

- **Test Cases**:
  - [x] L1: `GattBleGatewayNotificationParsingTest`
  - [x] L1: `DefaultDeviceConnectionManagerIngressTest`
  - [x] L1: `RealConnectivityBridgeTest`
  - [x] L1/L2 seam: `RealBadgeAudioPipelineIngressTest`
  - [ ] L2: Record on badge → BLE notification arrives within 1s
  - [ ] L2: Notification triggers full pipeline (download → ASR → schedule)
  - [ ] L2: Disconnected state → recording notifications stop

> [!NOTE]
> 2026-03-22 Wave 10 repair hardened connection truth so optimistic BLE-only states no longer surface as healthy bridge connectivity. Device-level re-entry validation is still required before reopening SIM Wave 9 hardware follow-up proof.

> [!NOTE]
> Protocol SOT: ESP32 Firmware Protocol (handled via BLE GATT notifications)


---

## File Map

| Layer | Files |
|-------|-------|
| **Domain** | `ConnectivityBridge.kt`, `ConnectivityService.kt`, `BadgeConnectionState.kt`, `RecordingNotification.kt`, `WavDownloadResult.kt` |
| **Data** | `RealConnectivityBridge.kt`, `RealConnectivityService.kt`, `FakeConnectivityBridge.kt` |
| **DI** | `ConnectivityBridgeModule.kt` |


---

## Legacy Wrapping Strategy

```kotlin
// app-core/data/connectivity/RealConnectivityBridge.kt
class RealConnectivityBridge @Inject constructor(
    private val legacyManager: DeviceConnectionManager,  // from :feature:connectivity
    private val httpClient: BadgeHttpClient,             // from :feature:connectivity
    private val bleGateway: BleGateway                   // from :feature:connectivity
) : ConnectivityBridge {
    
    override val connectionState: StateFlow<BadgeConnectionState>
        get() = legacyManager.connectionState.map { /* transform to Prism type */ }
    
    override suspend fun downloadRecording(
        filename: String,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
    ): WavDownloadResult {
        // Delegate to httpClient.downloadWav() with onProgress forwarded
        // Transform result to Prism type
    }
    
    override fun recordingNotifications(): Flow<RecordingNotification> {
        // Listen for "log#YYYYMMDD_HHMMSS" BLE command (or HTTP polling fallback)
        // Map to RecordingNotification.RecordingReady
    }
}
```

> [!IMPORTANT]
> Only `data/` layer imports from `:feature:connectivity`. Domain layer NEVER imports legacy.

---

## Verification Commands

```bash
# Check no legacy imports in domain
grep -rn "feature.connectivity" app-core/src/main/java/**/domain/ && echo "FAIL"

# Build check
./gradlew :app-core:compileDebugKotlin
```
