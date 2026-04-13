# Connectivity Bridge

> **Cerb-compliant spec** — Prism-safe wrapper for legacy BLE + HTTP connectivity.
> **OS Layer**: Infrastructure (Layer 1) — Leaf service, no upstream dependencies
> **Status**: SHIPPED
> **Last Updated**: 2026-04-13
> **Behavioral UX Authority Above This Doc**: [`docs/core-flow/base-runtime-ux-surface-governance-flow.md`](../../core-flow/base-runtime-ux-surface-governance-flow.md) (`UX.CONNECTIVITY.*`)

---

## Overview

Connectivity Bridge provides a **thin, Prism-compatible interface** to legacy `feature:connectivity` module. Preserves ESP32 rate limiting and business logic while exposing clean interfaces for Prism domain code.

**Key Principle**: Prism domain code MUST NOT import from `feature:connectivity` directly. All connectivity access goes through this bridge.

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

- `docs/plans/bug-tracker.md`

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
    data class Error(val message: String) : BadgeManagerStatus()
}
```

`BadgeConnectionState` remains the strict shared transport contract.
`BadgeManagerStatus` is a manager-only refinement layer used for BLE/Wi‑Fi diagnostics in connectivity surfaces without changing shell/history routing.

For the current reconnect contract, shared `BadgeConnectionState.Connected` may resume as soon as persistent GATT is active and the badge reports a usable IP / transport-ready network result. HTTP file operations still keep their own endpoint validation path.

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
| `CONNECTED` | ✅ Green pulsing BT icon<br>Badge name + ID + battery | **⚡ 断开连接**<br>**🔄 检查更新** |
| `DISCONNECTED` | ⚫ Gray BT icon<br>"🔴 离线" | **重试连接** |
| `BLE_PAIRED_NETWORK_UNKNOWN` | 🔵 BT icon<br>"已连接设备"<br>"蓝牙已连接，正在确认设备网络状态" | **重试连接** |
| `BLE_PAIRED_NETWORK_OFFLINE` | 🔵 BT icon<br>"已连接设备"<br>"蓝牙已连接，但设备当前未接入可用网络" | **重试连接** |
| `NEEDS_SETUP` | ⚠️ Amber Warning icon<br>"⚙️ 设备未配网" | **📱 开始配网** → Onboarding |
| `RECONNECTING` | Spinner | (Auto) |
| `CHECKING_UPDATE` | Spinner |  (Auto) |
| `UPDATE_FOUND` | 📥 Amber icon<br>"发现新版本" | **立即同步**<br>**稍后** |
| `UPDATING` | Progress bar | (Auto) |
| `WIFI_MISMATCH` | ⚠️ Warning<br>WiFi form | **忽略**<br>**更新配置** |

> [!NOTE]
> After user taps "断开连接", soft disconnect preserves session.
> Shared shell routing still treats that as `DISCONNECTED`.
> `NEEDS_SETUP` only shows on cold boot (no session ever existed).
> Tapping "重试连接" from a disconnected manager state triggers auto-reconnect using persisted session.
> Tapping "更新配置" from `WIFI_MISMATCH` first performs local validation plus an explicit send-confirm dialog; only the confirmed valid submit enters reconnect/progress and runs the repair flow without requiring a second manual retry.
> The richer `BLE_PAIRED_NETWORK_*` states are manager-only refinements derived from `ConnectivityBridge.managerStatus`; they must not redefine global transport readiness.
> Debug builds may additionally expose a temporary `断开连接` action in `BLE_PAIRED_NETWORK_*` states for hardware testing convenience; that control must not be treated as a release-surface contract.
> Closing the connectivity modal/manager clears transient reconnect or mismatch override state so later reopen reflects live manager truth rather than a retained stale repair screen.

#### Data Flow

``` 
User Action → ConnectivityViewModel → ConnectivityService → ConnectivityBridge → Legacy
```

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
- manager-only BLE/Wi‑Fi diagnostics must not wait for a background poll tick to become visible
- reconnect/setup/manual repair may use bounded foreground confirmation loops, but the runtime must not resume continuous BLE Wi‑Fi polling after those checks complete
- connectivity UI must treat reconnect and manual Wi‑Fi repair as one exclusive foreground operation so duplicate taps do not stack concurrent repair attempts

Reconnect credential rule:

- target firmware stores one last-working Wi‑Fi credential and auto-reconnects itself after reconnect / power recovery
- on current target OEMs, the app must assume phone Wi‑Fi SSID may be unreadable even when Wi‑Fi transport is connected; unreadable SSID is normal runtime reality, not a reconnect error by itself
- app-side `SessionStore` still owns the multi-network remembered Wi‑Fi list, but remembered credentials are for explicit manual repair assistance only
- when BLE reconnect succeeds:
  - if the badge reports a usable IP, reconnect must treat transport as connected immediately
  - reconnect must not gate `Connected` on readable phone SSID
  - reconnect must not auto-replay remembered credentials or try to switch the badge network in the background
  - if the badge remains offline or reports no usable IP, the manager may surface BLE-held offline or unknown diagnostics and offer explicit manual repair
- reconnect must not route to `WIFI_MISMATCH` solely because phone SSID is unreadable
- reconnect-driven `WIFI_MISMATCH` is no longer the default path; mismatch UI is reserved for explicit repair contexts where the user or a trusted runtime comparison has produced a concrete mismatch state
- when the user submits manual SSID/password from `WIFI_MISMATCH`, the UI must trim both fields, reject blank values locally, require an explicit confirmation dialog, and only then show reconnect/progress while `updateWifiConfig()` runs its provision-plus-confirm repair path
- manual repair must not fall back into the generic reconnect path after sending credentials
- manual repair confirmation must:
  - keep the current BLE session
  - poll badge network status with the same bounded tolerance used by setup (`3` attempts, `1500ms` interval)
  - validate against the user-submitted SSID rather than the phone's current SSID
- the manual repair form should prefill the current phone SSID only when the OS exposes one; blank SSID is expected on OEMs that hide it, and the field must remain editable with password blank/manual
- empty or whitespace-only SSID/password must never be sent to the badge from the manual repair flow
- invalid manual repair input must stay on `WIFI_MISMATCH`, surface a local error message, and must not transition into reconnect/progress
- manual repair may return to `WIFI_MISMATCH` only when the badge proves it came online on a different SSID than the submitted one
- manual repair must not route back to `WIFI_MISMATCH` merely because phone Wi‑Fi is unreadable/unavailable or because the badge is still offline during the bounded confirmation window
- manual repair confirmation failure must surface an explicit error/result and must not be collapsed into synthetic success
- if the user closes the connectivity surface mid-repair, only the transient override is cleared; underlying bridge/manager truth remains authoritative on next reopen
- reconnect foreground network query must tolerate the BLE 2s minimum query floor and retry after the guard delay instead of failing immediately on a throttle timeout
- badge IP is reused only as an in-memory active runtime endpoint snapshot; the app must not persist a last-known badge IP across sessions
- HTTP sync/delete flows should reuse that active runtime endpoint instead of re-querying BLE Wi‑Fi status before every `/list` / `/download` / `/delete`
- shared transport readiness stays strict: BLE-held + Wi‑Fi-offline is not `Connected`

#### Implementation Status

| Component | Status | Wave |
|-----------|--------|------|
| `ConnectivityModal.kt` | ✅ Shipped | Pre-existing + Wave 2.5 |
| `ConnectivityViewModel.kt` | ✅ Shipped | Pre-existing + Wave 2.5 |
| `ConnectivityService` (interface) | ✅ Shipped | Pre-existing |
| `RealConnectivityService` | ✅ Shipped | Wave 2.5 |
| `ConnectivityBridge` | ✅ Shipped | Wave 2 |

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
    
    override suspend fun downloadRecording(filename: String): WavDownloadResult {
        // Delegate to httpClient.downloadWav()
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
