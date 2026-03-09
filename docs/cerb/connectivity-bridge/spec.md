# Connectivity Bridge

> **Cerb-compliant spec** — Prism-safe wrapper for legacy BLE + HTTP connectivity.
> **OS Layer**: Infrastructure (Layer 1) — Leaf service, no upstream dependencies
> **State**: SHIPPED

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

## Rate Limiting Policy (PRESERVED)

> [!CAUTION]
> **ESP32 overload protection**. Do NOT remove or weaken these limits.

| Parameter | Value | Enforcement |
|-----------|-------|-------------|
| **Network query TTL** | 2s | `queryNetworkStatus()` |
| **Polling interval** | 10s | Badge state monitor |
| **Min poll gap** | 5s | Prevents burst |
| **Max consecutive failures** | 3 | Circuit breaker |
| **Inter-command gap** | 300ms | BLE command spacing |

**Offline detection**: `wifi#address#0.0.0.0#...` indicates badge has no WiFi.

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

### RecordingNotification

```kotlin
sealed class RecordingNotification {
    /**
     * Badge finished recording, file is ready for download.
     * Triggered by `log#YYYYMMDD_HHMMSS` BLE command from firmware.
     */
    data class RecordingReady(
        val filename: String  // YYYYMMDD_HHMMSS.wav
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
| `DISCONNECTED` | ⚫ Gray BT icon<br>"🔴 离线" | **连接设备** |
| `NEEDS_SETUP` | ⚠️ Amber Warning icon<br>"⚙️ 设备未配网" | **📱 开始配网** → Onboarding |
| `RECONNECTING` | Spinner | (Auto) |
| `CHECKING_UPDATE` | Spinner |  (Auto) |
| `UPDATE_FOUND` | 📥 Amber icon<br>"发现新版本" | **立即同步**<br>**稍后** |
| `UPDATING` | Progress bar | (Auto) |
| `WIFI_MISMATCH` | ⚠️ Warning<br>WiFi form | **忽略**<br>**更新配置** |

> [!NOTE]
> After user taps "断开连接", soft disconnect preserves session.
> State naturally becomes `DISCONNECTED` (no UI override needed).
> `NEEDS_SETUP` only shows on cold boot (no session ever existed).
> Tapping "连接设备" from `DISCONNECTED` triggers auto-reconnect using persisted session.

#### Data Flow

```
User Action → ConnectivityViewModel → ConnectivityService → ConnectivityBridge → Legacy
```

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
| **2.7** | Session Persistence + Soft Disconnect | ✅ SHIPPED | `OnboardingGate`, `SessionStore`, `disconnectBle()`, `unpair()` |
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

1. **Onboarding reset on app restart**: `AgentMainActivity` used `rememberSaveable` → not persisted across process death → always showed onboarding
2. **Session lost on disconnect**: `DeviceConnectionManager` stored BLE session in-memory only → lost after app restart → reconnection impossible
3. **Disconnect nuked session**: `disconnect()` called `forgetDevice()` → cleared session → forced full re-onboarding

### Solution

| Fix | Component | Implementation |
|-----|-----------|----------------|
| **1. Onboarding Gate** | `OnboardingGate` (Prism-native) | SharedPreferences-backed `StateFlow<Boolean>`, survives process death |
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
  - [x] `OnboardingGate` with SharedPreferences persistence
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
  - [ ] L2: Complete onboarding → force-close app → relaunch → **AgentShell shown (not onboarding)**
  - [ ] L2: Disconnect → reconnect → **reconnects without onboarding**

### Files Changed

**New** (3):
- `app-core/src/main/java/com/smartsales/prism/data/onboarding/OnboardingGate.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/SessionStore.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/SessionStoreImpl.kt`

**Modified** (11):
- `app-core/src/main/java/com/smartsales/prism/AgentMainActivity.kt`
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
  - [ ] `RecordingNotification.RecordingReady` emitted with correct filename
  - [ ] Only fires when badge is connected
  - [x] HTTP polling removed (obsolete)

- **Test Cases**:
  - [ ] L2: Record on badge → BLE notification arrives within 1s
  - [ ] L2: Notification triggers full pipeline (download → ASR → schedule)
  - [ ] L2: Disconnected state → recording notifications stop

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
