# SIM Connectivity Spec

> **Scope**: Badge BLE/Wi-Fi connection management as a decoupled support module inside the standalone SIM prototype
> **Status**: SPEC_ONLY
> **Behavioral Authority Above This Doc**:
> - `docs/to-cerb/sim-standalone-prototype/concept.md`
> - `docs/core-flow/sim-shell-routing-flow.md`
> - `docs/specs/connectivity-spec.md`
> - `docs/cerb/connectivity-bridge/spec.md`
> - `docs/cerb/connectivity-bridge/interface.md`
> **Audit Evidence**:
> - `docs/reports/20260319-sim-standalone-code-audit.md`
> - `docs/reports/20260321-sim-wave5-boundary-audit.md`

---

## 1. Purpose

`SIM Connectivity` defines how the standalone SIM prototype reuses the mature connectivity module without coupling it back into the smart-agent runtime.

This shard exists so SIM can:

- expose a shell-level connectivity entry/icon
- let users manage badge BLE/Wi-Fi connection state
- keep connectivity behavior as isolated infrastructure/support logic

This shard does not turn connectivity into a third smart feature lane.

---

## 2. Included Scope

- shell-level entry into badge connection management
- SIM shell-owned route between connectivity modal and setup
- reuse of existing connectivity bridge/service contracts
- setup, reconnect, disconnect, and adjacent connection-management behavior already defined by connectivity specs
- return path from connectivity management back into normal SIM shell use

---

## 3. Excluded Scope

- redefining BLE/Wi-Fi protocol behavior locally in SIM
- coupling scheduler execution to badge connectivity by default
- coupling audio discussion chat to smart-agent runtime through connectivity
- treating connectivity as a hidden orchestration lane

---

## 4. Ownership

`SIM Connectivity` owns:

- the SIM-facing shell entry contract
- the isolation rule between connectivity and the two main feature lanes

`SIM Connectivity` reuses:

- `ConnectivityBridge`
- `ConnectivityService`
- existing connectivity UI flows where they can be mounted without smart-agent contamination

`SIM Connectivity` must not assume:

- direct imports from legacy connectivity internals are acceptable in new SIM domain code
- connectivity state should reshape SIM scheduler/audio business rules by default

### T5.2 Boundary Freeze

#### Reuse Unchanged

- `ConnectivityBridge` remains the source of truth for badge connection state, readiness, recording notifications, and badge file operations
- `ConnectivityService` remains the source of truth for reconnect, disconnect, unpair, update, and Wi-Fi config mutation behavior
- `ConnectivityViewModel` remains the connection-state projection layer for SIM modal/manager UI
- `PairingService` remains the setup runtime owner for BLE scan, Wi-Fi provisioning, and post-provision network checks
- the onboarding pairing subset remains the setup UI/runtime owner for the SIM `SETUP` branch

#### Shell Wraps Only

- connectivity entry sources and route selection
- `MODAL`, `SETUP`, and `MANAGER` surface state
- overlay layering and dismiss semantics
- manager close-back-to-chat behavior
- SIM-only route telemetry and logs

#### Forbidden Coupling

- scheduler must not observe or import connectivity contracts
- connectivity must not own scheduler or chat shell navigation
- onboarding account/profile/notification behavior is not part of SIM connectivity
- connectivity must not become a hidden prerequisite for the two main SIM lanes

#### Explicit T5.3 Debt

- `domain/pairing/PairingService` still leaks `legacy.BlePeripheral` through `DiscoveredBadge`
- `OnboardingViewModel` still carries `UserProfileRepository` even though SIM setup does not use onboarding profile/account behavior
- manager presentation debt is deferred UI work and not part of this boundary freeze

---

## 5. Hard Migration Rule

Connectivity is a strong hard-migration candidate because it is already mature and decoupled.

SIM may:

- reuse existing connectivity bridge and service contracts
- reuse connectivity modal/setup surfaces with shell-level adaptation
- preserve current BLE/Wi-Fi behavior as long as SIM routing stays isolated

SIM may not:

- rebuild connectivity logic from scratch without evidence-based need
- use connectivity reuse as permission to pull smart-app shell/runtime dependencies into SIM

---

## 6. Shell Entry Rule

The SIM shell should expose a connectivity entry/icon as a support action.

Expectations:

- entry is reachable from normal shell use
- `ConnectivityModal` is the bootstrap-only entry for `NeedsSetup`
- `开始配网` transitions into the onboarding connectivity subset (`HARDWARE_WAKE` -> `SCAN` -> `DEVICE_FOUND` -> `BLE_CONNECTING` -> `WIFI_CREDS` -> `FIRMWARE_CHECK`) as a nested SIM-owned full-screen overlay
- setup success transitions into a full-screen SIM connectivity manager surface
- once a device/session already exists, later connectivity entry opens the manager directly instead of reopening the bootstrap modal
- closing the manager returns the user to SIM chat

This entry does not change the rule that SIM has only two main product lanes.

### T5.1 Locked Behavior

- `AgentIntelligenceScreen` badge/device action and history drawer device action both open the same SIM-owned connectivity route
- when connection state is `NeedsSetup`, connectivity entry opens the bootstrap modal
- when a device/session already exists, connectivity entry opens the full-screen connectivity manager directly
- the setup branch reuses onboarding pairing UI/business logic rather than the legacy `DeviceSetupScreen`
- setup success enters the SIM connectivity manager instead of returning directly to chat or continuing into onboarding naming/account/profile steps
- the SIM manager remains a connection-only steady-state surface in this slice; it does not recover the historical full `DeviceManager` file-list/product scope
- scrim dismissal applies to the modal route only; the full-screen setup and manager branches are owned by explicit actions/events

---

## 7. Human Reality

### Organic UX

Users expect badge connection management to exist because the product still talks to hardware.
They do not expect connection management to behave like a smart-agent feature.

### Data Reality

Connection state is already defined by the existing connectivity contracts.
SIM should inherit those contracts rather than invent a second meaning of "connected."

### Failure Gravity

The worst failure is letting connectivity reuse drag smart runtime assumptions into SIM.

---

## 8. Wave Plan

| Wave | Focus | Status | Deliverable |
|------|-------|--------|-------------|
| 1 | SIM connectivity contract freeze | PLANNED | SIM connectivity spec/interface |
| 2 | Shell entry and reuse decision | PLANNED | SIM entry wiring plan |
| 3 | Isolation validation | PLANNED | proof connectivity stays decoupled |

---

## 9. Done-When Definition

SIM connectivity is ready only when:

- a SIM shell entry/icon exists for badge connection management
- existing connectivity contracts remain the behavioral source
- connectivity use does not contaminate scheduler/audio/chat runtime behavior
- the shell/connectivity ownership split is explicit enough for T5.3 isolation work to proceed without new product decisions
