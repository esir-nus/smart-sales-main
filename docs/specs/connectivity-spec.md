# connectivity.md

> **Scope:** BLE discovery, Wi-Fi provisioning, HTTP media server reachability, session persistence, and auto-reconnect for BT311 (and future devices).

This doc defines the **contract** for the connectivity layer and the UI flows that sit on top of it. New work on `feature/connectivity`, `feature/media/devicemanager`, or DeviceSetup **must** follow this document.

---

## 1. Goals

1. **Single source of truth** for connectivity:
   `DeviceConnectionState` is the only “is the device connected?” signal.
2. **Deterministic setup flow:**
   DeviceSetup follows a linear step pipeline (Step 0…Step 6). UI and backend agree on which step we are in and what can happen next.
3. **Robust auto-reconnect:**
   After initial setup, the app automatically recovers connections without user re-entering credentials.
4. **Clear errors:**
   Distinguish “no device found”, “BLE hardware issues”, and “HTTP server unreachable”.
5. **Testable:**
   All flows have unit tests with fakes and at least one instrumentation test per major scenario.

---

## 2. System architecture

### 2.1 Layers

* **Connectivity core (`feature/connectivity`)**

  * `DeviceConnectionManager` / `DefaultDeviceConnectionManager`
  * BLE scan & profile matching (`AndroidBleScanner`, `BleProfileConfig`)
  * Wi-Fi provisioner for BT311
  * HTTP endpoint checker (`HttpEndpointChecker`)
  * Session storage (BT311 identity + HTTP base URL)

* **Setup UI (`feature/connectivity/setup`)**

  * `DeviceSetupViewModel`
  * `DeviceSetupScreen`
  * Implements the **initial setup sequence** (see §3).

* **DeviceManager UI (`feature/media/devicemanager`)**

  * `DeviceManagerViewModel`
  * `DeviceManagerScreen`
  * Shows connected/disconnected state, exposes “开始配网” and “重试连接”.

* **Home + overlays (`app/.../AiFeatureTestActivity.kt`)**

  * Controls navigation between Home, DeviceSetup, DeviceManager.
  * Does not talk to BLE/HTTP directly; only via the connection manager and VMs.

### 2.2 Ownership rules

* Only the **connectivity core** talks to:

  * Android BLE APIs
  * Wi-Fi provisioning commands
  * HTTP media server
  * Session storage

* UI/ViewModels:

  * May call public methods on `DeviceConnectionManager` (e.g. `scheduleAutoReconnectIfNeeded`, `forceReconnectNow`, “connect with new device”).
  * Must treat `DeviceConnectionState` as **read-only** state, never as a local boolean copy.
  * Must not create their own notion of “connected” beyond mapping from `DeviceConnectionState`.

---

## 3. Initial setup sequence (DeviceSetup contract)

DeviceSetup is responsible for turning a fresh app + BT311 into a connected session persisted in storage.

### 3.1 Setup steps

`DeviceSetupViewModel` maintains:

```kotlin
enum class SetupStep {
    Preflight,        // Step 0
    Scanning,         // Step 1
    NoDeviceFound,    // Step 1 error
    ConnectingBle,    // Step 2
    CheckingStatus,   // Step 2 cont.
    WifiForm,         // Step 3 input
    ProvisioningWifi, // Step 3 in-progress
    ConnectingHttp,   // Step 4
    SavingSession,    // Step 5
    Completed,        // Step 6
    Error             // generic fatal error
}
```

and UI state:

```kotlin
data class DeviceSetupUiState(
    val step: SetupStep,
    val isScanning: Boolean,
    val canRetryScan: Boolean,
    val lastError: SetupError? = null
)
```

All DeviceSetup UI is derived from these fields.

#### Step 0 – Preflight

* Check Bluetooth + runtime permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, location if required).
* If anything missing:

  * `step = Preflight`
  * Show explanation and buttons to open system dialogs.
* Once satisfied, VM moves to `Scanning`.

#### Step 1 – Discover device (BLE scan)

* `step = Scanning`, `isScanning = true`, `canRetryScan = false`
* Start BLE scan with `startScan(null, settings, callback)`.
* For each `ScanResult`, build:

  * `deviceName` (from `BluetoothDevice` if BLUETOOTH_CONNECT granted, otherwise from `scanRecord`)
  * Advertised service UUIDs
* Apply `BleProfileConfig.matches`:

  * **BT311 profile**: `nameKeywords ∋ "BT311"/variants` OR service UUID ∈ {Nordic UART UUID, vendor UUID}.
* On first match:

  * Stop scanning.
  * Cache the matching `peripheral`.
  * `step = ConnectingBle`.
* Timeout (e.g. 10–15 s) with no match:

  * Stop scanning, `isScanning = false`, `canRetryScan = true`.
  * `step = NoDeviceFound`, `lastError = NoDeviceFound`.

Retry:

* When user taps “重新扫描”:

  * `step = Scanning`, `isScanning = true`, `canRetryScan = false`.
  * Start scan again.

#### Step 2 – Connect & query device status

* `step = ConnectingBle`
* VM instructs connectivity core to connect to the chosen peripheral.
* Once BLE connected, query network status over BLE:

  * If device reports “Wi-Fi not configured”:

    * `step = WifiForm` (ask for credentials).
  * If “Wi-Fi configured but offline”:

    * `step = Error` (with a dedicated reason like `WifiOffline`).
  * If “Wi-Fi configured & online”:

    * Move directly to `ConnectingHttp`.

Errors:

* BLE connect failure → `step = Error` with `BleConnectFailed`.
* Device not found mid-connect → `step = NoDeviceFound`.

#### Step 3 – Configure Wi-Fi

* `step = WifiForm` shows SSID/password form.
* On submit:

  * `step = ProvisioningWifi`
  * Provisioner sends credentials and waits for success or timeout.
* On success:

  * Move to `ConnectingHttp`.
* On failure:

  * `step = Error` with `WifiProvisionFailed` and offer “重试配置 Wi-Fi”.

#### Step 4 – HTTP endpoint & health check

* `step = ConnectingHttp`
* Connectivity core:

  * Builds base URL (e.g. `http://<ip>:8000`).
  * Checks health using `HttpEndpointChecker`:

    * Prefer `HEAD /`, fallback to `GET /` if server doesn’t support HEAD.
* Success:

  * Move to `SavingSession`.
* Failure:

  * `DeviceConnectionState.Error(EndpointUnreachable)`
  * DeviceSetup: `step = Error` with reason `EndpointUnreachable` + “重试连接媒体服务” button.

#### Step 5 – Save session & mark connected

* `step = SavingSession`
* Connectivity core:

  * Persists session (device identity + HTTP base URL + any other metadata).
  * Sets `DeviceConnectionState.Connected(session)`.
* When VM observes `Connected`:

  * `step = Completed`.

#### Step 6 – Finish

* `step = Completed`
* UI shows success message and “返回首页”.
* Host activity navigates back to Home overlay stack. DeviceManager will treat `Connected` as “ready”.

### 3.2 Interaction with `DeviceConnectionState`

DeviceSetup must **observe** `DeviceConnectionState`:

* When in any setup step and connection state becomes `Connected`:

  * `step = Completed`.
* When in BLE/Wi-Fi/HTTP phases and state becomes `Error(…)`:

  * Map error reason to an appropriate `SetupError` and show an error step.
* When state is `NeedsSetup`:

  * It is safe to run DeviceSetup (no session stored).

DeviceSetup must **not** set `DeviceConnectionState` directly. It should call public APIs on `DeviceConnectionManager` and trust the manager to emit the correct state.

---

## 4. Connection state machine (global contract)

`DeviceConnectionState` is the global connectivity truth.

### 4.1 States

* `NeedsSetup`

  * No stored session.
  * Neither auto-reconnect nor retry can work.
  * DeviceManager shows “需要先完成设备配网”; only “开始配网” button is valid.

* `Disconnected`

  * There is a stored session, but we’re not connected right now.
  * Auto-reconnect may run; user can hit “重试连接”.

* `AutoReconnecting`

  * Auto-reconnect has been scheduled and is performing BLE+HTTP checks.
  * DeviceManager shows in-progress status; destructive actions are disabled.

* `Connecting` (optional / internal)

  * A foreground connect attempt (e.g. from DeviceSetup or manual retry) is in progress.

* `Connected(session)`

  * BLE and HTTP endpoint verified healthy as of last check.
  * DeviceManager/Home treat this as “ready”.

* `Error(reason)`

  * Last attempt failed with an explicit error.
  * Reasons include:

    * `DeviceNotFound`
    * `BleConnectFailed`
    * `EndpointUnreachable`
    * `WifiProvisionFailed`
  * DeviceManager maps each to tailored copy.

### 4.2 Guarantees

* **Connected** means “BT311 reachable + HTTP `/` (or equivalent health) returned OK”. Anything less must be `Disconnected` or `Error`.
* Auto-reconnect never mutates user credentials; it only reuses stored session.
* Methods:

  * `scheduleAutoReconnectIfNeeded()`:

    * May be called from DeviceManager/Home on init.
    * No-op if already Connected or connecting.
    * Uses backoff; will not spam BLE/HTTP.
  * `forceReconnectNow()`:

    * Manual retry; bypasses backoff, but still requires a stored session.
    * Safe to call only in `Disconnected`/`Error` states.

---

## 5. DeviceManager behavior contract

DeviceManager is the primary consumer of `DeviceConnectionState` after setup.

### 5.1 NeedsSetup

* `DeviceConnectionState = NeedsSetup`
* UI:

  * Top card: “设备未连接，需要先完成设备配网”.
  * Shows **“开始配网”** button; hides “重试连接”.
  * Tapping “开始配网” opens DeviceSetup via `openDeviceSection(forceSetup = true)`.

### 5.2 Disconnected / Error

* Stored session exists.
* UI:

  * Shows connection status card with summary message.
  * Shows **“重试连接”** button.
  * Tapping “重试连接” calls `forceReconnectNow()` on `DeviceConnectionManager`.

### 5.3 AutoReconnecting / Connecting

* UI:

  * Shows “正在尝试重新连接设备…” or similar.
  * “重试连接” either hidden or disabled.
  * Refresh/upload buttons disabled until state returns to `Connected`.

### 5.4 Connected

* UI:

  * Shows “设备已连接”.
  * File list, preview, and actions available.

---

## 6. BLE profile & scan contract

### 6.1 BT311 profile

* **Name keywords:** `"BT311"`, `"BT-311"`, `"BT 311"` (update if hardware names differ).
* **Service UUIDs:** Nordic UART service + vendor service (see constants in `BleProfileConfig`).

### 6.2 Matching rule

* `BleProfileConfig.matches(scanResult)` returns true when:

  * `deviceName` contains any `nameKeyword` **OR**
  * `advertisedServiceUuids` intersects with profile’s UUID list.

### 6.3 Logging and diagnostics

In debug builds:

* For the first N scan results per session:

  * Log address, name, advertised UUIDs.
  * Log `matches(...)` result for active profiles.
* Use a stable log tag (e.g. `BT311Scan`) so logcat filters stay consistent.

---

## 7. Error handling & UX mapping

The connectivity core and UI must align:

| Reason (core)         | DeviceSetup step    | DeviceManager/Home copy (example) |
| --------------------- | ------------------- | --------------------------------- |
| `DeviceNotFound`      | `NoDeviceFound`     | “未发现设备，请检查设备电源或蓝牙后重新扫描。”          |
| `BleConnectFailed`    | `Error`             | “无法通过蓝牙连接设备，请靠近设备并重试。”            |
| `WifiProvisionFailed` | `Error`             | “Wi-Fi 配网失败，请检查密码和网络后重试。”         |
| `EndpointUnreachable` | `Error` / HTTP step | “媒体服务不可用，请检查设备网络后重试。”             |
| `NeedsSetup`          | `Preflight` / start | “需要先完成设备配网。”                      |

---

## 8. Testing guidelines

Every change in `feature/connectivity` or `feature/media/devicemanager` must include tests.

### 8.1 Unit tests

* **Connectivity core**

  * `DefaultDeviceConnectionManager`:

    * Stored session + BLE OK + HTTP OK → `Connected`.
    * BLE OK + HTTP fail → `Error(EndpointUnreachable)` → `Disconnected`.
    * No stored session → `NeedsSetup`.
    * Backoff behavior for auto-reconnect.
* **DeviceSetupViewModel**

  * Preflight → Scanning after permissions are available.
  * Scan timeout → `NoDeviceFound` with `canRetryScan = true`.
  * Device found → progress through steps into `Completed` when connection state becomes `Connected`.
* **DeviceManagerViewModel**

  * NeedsSetup → `canStartSetup = true`, `canRetryConnect = false`.
  * Disconnected/Error → `canRetryConnect = true`.

### 8.2 Instrumentation tests

At least:

* DeviceManager:

  * NeedsSetup: sees “开始配网”, no “重试连接”.
  * Disconnected: sees “重试连接”.
* DeviceSetup:

  * “未发现设备” retry button appears after scan timeout.
  * Happy path with fakes: scanning → Wi-Fi → Connected → Completed.

---

## 9. Implementation rules (“do / don’t”)

**Do:**

* Always route connectivity operations through `DeviceConnectionManager`.
* Use `DeviceConnectionState` as the only truth for “connected”.
* Drive DeviceSetup UI from `SetupStep`, not from scattered booleans.
* Maintain back-compat with BT311 backend (`wifi.py`) when changing HTTP logic.

**Don’t:**

* Don’t set `Connected` based only on BLE; HTTP health check is required.
* Don’t show “重试连接” when state is `NeedsSetup`.
* Don’t start BLE scan without giving user a way to retry if it fails.
* Don’t catch connectivity exceptions and silently ignore them—surface them as `Error(reason)` and map to UI.

---

This document is the contract for the connectivity layer going forward.

When adding new devices or flows, extend the same patterns:

* Add a new `BleProfileConfig` for the device.
* Reuse `DeviceConnectionState` and the setup step pipeline.
* Keep UI and backend aligned through these shared models.
