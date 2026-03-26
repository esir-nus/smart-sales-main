# Device Pairing Interface

> **For Consumers**: Prism Onboarding & Connectivity Modal  
> **Blackbox Rule**: Import ONLY this interface, never internal implementations

---

## Public Contract

### PairingService

```kotlin
package com.smartsales.prism.domain.pairing

interface PairingService {
    /**
     * 配对状态流
     * 
     * 从 Scanning → Success 或 Error
     */
    val state: StateFlow<PairingState>
    
    /**
     * 开始扫描 BLE 设备
     * 
     * 自动超时：12秒
     */
    suspend fun startScan()
    
    /**
     * 配对并配网
     * 
     * @param badge 扫描发现的设备
     * @param wifiCreds WiFi 凭证
     * @return 成功返回 Badge ID，失败返回 Error
     */
    suspend fun pairBadge(
        badge: DiscoveredBadge,
        wifiCreds: WifiCredentials
    ): PairingResult
    
    /**
     * 取消配对流程
     */
    fun cancelPairing()
}
```

---

## Domain Models

### PairingState

```kotlin
sealed class PairingState {
    /** 空闲状态 */
    object Idle : PairingState()
    
    /** 正在扫描 */
    object Scanning : PairingState()
    
    /** 发现设备 */
    data class DeviceFound(val badge: DiscoveredBadge) : PairingState()
    
    /** 正在配对 (包含 WiFi 配网 + 网络检查) */
    data class Pairing(val progress: Int) : PairingState()
    
    /** 配对成功 */
    data class Success(val badgeId: String, val badgeName: String) : PairingState()
    
    /** 配对失败 */
    data class Error(val message: String, val reason: ErrorReason, val canRetry: Boolean) : PairingState()
}
```

### DiscoveredBadge

```kotlin
data class DiscoveredBadge(
    val id: String,              // BLE MAC address
    val name: String,            // 设备名称
    val signalStrengthDbm: Int   // 信号强度
)
```

### WifiCredentials

```kotlin
data class WifiCredentials(
    val ssid: String,
    val password: String
)
```

### PairingResult

```kotlin
sealed class PairingResult {
    data class Success(val badgeId: String) : PairingResult()
    data class Error(val message: String, val reason: ErrorReason) : PairingResult()
}
```

### ErrorReason

```kotlin
enum class ErrorReason {
    SCAN_TIMEOUT,           // 扫描超时，未发现设备
    DEVICE_NOT_FOUND,       // 设备丢失（配对过程中断开）
    WIFI_PROVISIONING_FAILED, // WiFi 配网失败
    NETWORK_CHECK_FAILED,   // 网络检查失败（设备未上线）
    PERMISSION_DENIED,      // BLE 权限被拒绝
    UNKNOWN                 // 未知错误
}
```

---

## Usage Example

```kotlin
@HiltViewModel
class PairingFlowViewModel @Inject constructor(
    private val pairingService: PairingService
) : ViewModel() {

    val pairingState = pairingService.state

    fun startScan() {
        viewModelScope.launch {
            pairingService.startScan()
        }
    }

    fun pairBadge(badge: DiscoveredBadge, wifi: WifiCredentials) {
        viewModelScope.launch {
            pairingService.pairBadge(badge, wifi)
        }
    }
}
```

---

## State Transitions

```
Idle → startScan() → Scanning
Scanning → (timeout) → Error(SCAN_TIMEOUT)
Scanning → (found) → DeviceFound
DeviceFound → pairBadge() → Pairing(0%)
Pairing → (progress) → Pairing(50%)
Pairing → (network check OK) → Success
Pairing → (wifi fail) → Error(WIFI_PROVISIONING_FAILED)
Error → cancelPairing() → Idle
Success → [Done]
```

---

## Design Decisions

1. **Simplified from Legacy 9-State**  
   Legacy `DeviceSetupViewModel` has 9 states; Prism collapses to 6 core states.

2. **Progress Reporting**  
   `Pairing(progress: Int)` combines WiFi provisioning + network check into one UI state.

3. **Timeout Handling**  
   Scan timeout (12s) built into service, not UI concern.

4. **Error Recovery**  
   `canRetry` flag indicates if the error is retriable (e.g., scan timeout = YES, permission denied = NO).

5. **Provisioning Completion Contract**
   WiFi connect is a credential-dispatch step (`SD#<ssid>` then `PD#<password>`). Consumers must not assume an immediate BLE provisioning ack; later network-status validation decides online success or `NETWORK_CHECK_FAILED`.

---

## You Should NOT

- ❌ Import `RealPairingService` directly — use `PairingService` interface
- ❌ Access `DeviceConnectionManager` or `BleScanner` — service encapsulates these
- ❌ Assume scan completes instantly — observe `state` flow for results
- ❌ Call `pairBadge()` without valid `DiscoveredBadge` from `DeviceFound` state
- ❌ Bypass the scan-step permission gate — UI still requests Bluetooth permission at point-of-use before entering service work
