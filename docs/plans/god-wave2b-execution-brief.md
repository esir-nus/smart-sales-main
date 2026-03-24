# God Wave 2B Execution Brief

**Status:** Planned  
**Date:** 2026-03-24  
**Wave:** 2B  
**Mission:** `GattBleGateway.kt` and `DeviceConnectionManager.kt` structural cleanup  
**Primary Tracker:** `docs/plans/god-tracker.md`  
**Structure Law:** `docs/specs/code-structure-contract.md`  
**Validation Report:** pending until implementation

---

## 1. Purpose

Wave 2B is the connectivity transport cleanup slice in Wave 2.

It targets:

- `GattBleGateway.kt`
- `DeviceConnectionManager.kt`

These files are coupled enough in ownership that they should be cleaned as one connectivity slice rather than as isolated refactors.

`GattBleGateway.kt` is oversized and role-mixed. `DeviceConnectionManager.kt` is under the nominal budget, but it still remains structurally wrong because orchestration, reconnect policy, and ingress/state handling are mixed together.

---

## 2. Governing Docs

Wave 2B remains bounded by current connectivity ownership and SIM setup rules:

- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/cerb/sim-connectivity/spec.md`
- `docs/cerb/sim-connectivity/interface.md`
- `docs/specs/flows/OnboardingFlow.md`

Wave 2B must preserve current onboarding and SIM setup reuse boundaries.

---

## 3. Wave 2B Law

Wave 2B may do:

- public-seam reduction for `GattBleGateway.kt`
- extraction of transport/session support, payload parsing, fragment merge, and gateway policy helpers
- public-seam reduction for `DeviceConnectionManager.kt`
- extraction of connection orchestration, reconnect/backoff policy, and ingress/state support helpers
- local structure-test and guardrail updates needed for the accepted connectivity shape
- tracker/doc sync for the delivered split

Wave 2B must **not** do:

- public `GattSessionLifecycle` or `DeviceConnectionManager` API changes
- onboarding flow behavior rewrites
- SIM setup scope expansion
- protocol changes that alter current BLE/Wi-Fi behavior under the banner of structural cleanup

---

## 4. Planned Structure

Wave 2B leaves both source files as public seams.

Planned extraction map:

- `GattBleGateway.kt`
  - gateway seam
  - transport/session support
  - payload parser / fragment merge support
  - gateway policy/command helpers
- `DeviceConnectionManager.kt`
  - manager seam
  - connection orchestration support
  - reconnect/backoff policy support
  - ingress/state support

Exact filenames may follow the accepted ownership shape, but the split must remain discoverable and transport-owned.

---

## 5. Verification Target

Wave 2B acceptance should use focused app-core verification:

- `GattBleGatewayNotificationParsingTest`
- `DefaultDeviceConnectionManagerIngressTest`
- `RealConnectivityBridgeTest`
- `SimConnectivityRoutingTest`
- `ConnectivityStructureTest`
- `GodStructureGuardrailTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

---

## 6. Wave 2B Acceptance Bar

Wave 2B is complete only when:

- `GattBleGateway.kt` is under budget or backed by a valid active exception
- `DeviceConnectionManager.kt` is accepted as role-clean even if LOC was not the original blocker
- both tracker rows move from `Proposed` to `Accepted`
- focused connectivity behavior and structure tests stay green
- the public connectivity seams remain source-compatible
- onboarding and SIM setup ownership boundaries remain unchanged

---

## 7. Related Documents

- `docs/plans/god-tracker.md`
- `docs/plans/tracker.md`
- `docs/plans/god-wave2-execution-brief.md`
- `docs/specs/code-structure-contract.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/sim-connectivity/spec.md`
