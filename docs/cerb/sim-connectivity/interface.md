# SIM Connectivity Interface (Historical Redirect)

> **Blackbox contract** - Historical SIM connectivity contract retained in place for campaign memory.
> **Status**: Historical redirect
> **Current Reading Priority**: Historical context only; not current source of truth.
> **Current Active Truth**:
> - `docs/cerb/connectivity-bridge/interface.md`
> - `docs/cerb/connectivity-bridge/spec.md`
> - `docs/specs/flows/OnboardingFlow.md`
> - `docs/core-flow/sim-shell-routing-flow.md`

Historical note:

- the remainder of this file is a frozen snapshot of the older SIM connectivity interface framing
- it must not be used as active connectivity fallback ownership

---

## Entry Surface

### Connectivity Entry

```kotlin
interface SimConnectivityEntry {
    fun open()
    fun close()
}
```

Meaning:

- `open()` routes to either the SIM bootstrap modal or the SIM connectivity manager based on current connectivity state
- `close()` clears the SIM connectivity route and returns to normal shell use

### Route Semantics

```kotlin
enum class SimConnectivitySurface {
    MODAL,
    SETUP,
    MANAGER
}
```

Meaning:

- SIM shell owns the route state
- `MODAL` is the bootstrap-only connectivity surface used for `NeedsSetup`
- `SETUP` is a nested full-screen SIM overlay entered from the modal and backed by the onboarding pairing subset
- `MANAGER` is the contained SIM connectivity surface for all configured/non-setup states, but remains connection-only in this slice
- setup completion enters `MANAGER`
- later connectivity entry opens `MANAGER` directly when a device/session already exists

---

## Reused Connectivity Contracts

SIM should reuse the existing connectivity contracts as its behavioral backend:

```kotlin
interface ConnectivityBridge {
    val connectionState: StateFlow<BadgeConnectionState>
    val managerStatus: StateFlow<BadgeManagerStatus>
    suspend fun downloadRecording(filename: String): WavDownloadResult
    suspend fun listRecordings(): Result<List<String>>
    fun recordingNotifications(): Flow<RecordingNotification>
    suspend fun isReady(): Boolean
    suspend fun deleteRecording(filename: String): Boolean
}

interface ConnectivityService {
    suspend fun checkForUpdate(): UpdateResult
    suspend fun reconnect(): ReconnectResult
    suspend fun disconnect()
    suspend fun unpair()
    suspend fun updateWifiConfig(ssid: String, password: String): WifiConfigResult
}
```

Meaning:

- SIM shell owns the entry
- existing connectivity contracts own BLE/Wi-Fi behavior

### Boundary Ownership Rule

Meaning:

- `SimShell` owns only route selection, overlay presentation, close behavior, and SIM route telemetry
- `ConnectivityBridge`, `ConnectivityService`, and `PairingService` own the underlying connection and setup behavior unchanged
- SIM must not introduce a second local meaning of "connected"
- SIM routing must continue to read `connectionState`, not `managerStatus`
- SIM must not let connectivity contracts reshape scheduler runtime behavior
- SIM audio may consume `ConnectivityBridge` only for badge-origin recording ingress and badge file operations, not for chat/session ownership

### Active Carry Debt

Current migration debt that remains after T5.3:

- no Wave 5 interface-level carry debt is currently blocking SIM connectivity behavior

---

## UI Flow Guarantees

- connectivity is reachable from SIM shell as a support action
- connectivity opens modal-first only for the `NeedsSetup` bootstrap branch
- setup is reachable as a nested SIM branch instead of a new activity and reuses onboarding pairing flow ownership
- configured re-entry opens the connectivity manager directly
- connectivity state continues to come from the existing connectivity contracts
- connectivity does not become a hidden dependency of SIM scheduler or SIM audio discussion flows

---

## You Should NOT

- ❌ redefine connectivity protocol behavior inside SIM docs or code
- ❌ couple connectivity entry to smart-agent shell/runtime assumptions
- ❌ treat connectivity as a third smart feature lane

---

## When to Read Full Spec

Read `spec.md` if:

- you are wiring the SIM shell connectivity entry
- you are deciding how much of the existing connectivity UI to reuse
- you are validating that connectivity stays decoupled from the two main SIM lanes
