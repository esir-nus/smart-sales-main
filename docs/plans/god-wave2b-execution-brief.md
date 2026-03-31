# God Wave 2B Execution Brief

**Status:** L1 Accepted  
**Date:** 2026-03-24  
**Wave:** 2B  
**Mission:** `GattBleGateway.kt` and `DeviceConnectionManager.kt` structural cleanup  
**Primary Tracker:** `docs/plans/god-tracker.md`  
**Structure Law:** `docs/specs/code-structure-contract.md`  
**Current Reading Priority:** Historical execution reference only; not current source of truth.  
**Current Active Truth:** `docs/plans/god-tracker.md`, `docs/plans/tracker.md`, `docs/specs/code-structure-contract.md`, `docs/cerb/connectivity-bridge/spec.md`, `docs/cerb/connectivity-bridge/interface.md`, `docs/specs/flows/OnboardingFlow.md`  
**Historical Deprecated Context:** `docs/cerb/sim-connectivity/spec.md`, `docs/cerb/sim-connectivity/interface.md`  
**Validation Report:** `docs/reports/tests/L1-20260324-god-wave2b-connectivity.md`

---

## 1. Purpose

Wave 2B is the connectivity transport cleanup slice in Wave 2.

It targets:

- `GattBleGateway.kt`
- `DeviceConnectionManager.kt`

These files are coupled enough in ownership that they should be cleaned as one connectivity slice rather than as isolated refactors.

`GattBleGateway.kt` is oversized and role-mixed. `DeviceConnectionManager.kt` is under the nominal budget, but it still remains structurally wrong because orchestration, reconnect policy, and ingress/state handling are mixed together.

---

## 2. Current Active Truth

Wave 2B should now be read against the shared connectivity and onboarding docs:

- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/specs/flows/OnboardingFlow.md`

Historical deprecated context at the time:

- `docs/cerb/sim-connectivity/spec.md`
- `docs/cerb/sim-connectivity/interface.md`

Wave 2B must preserve current onboarding and connectivity reuse boundaries.

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

## 4. Delivered Structure

Wave 2B leaves both source files as public seams.

Delivered extraction map:

- `GattBleGateway.kt`
  - host seam only (`76 LOC`)
  - `GattBleGatewayRuntime.kt`
  - `GattBleGatewaySessionSupport.kt`
  - `GattBleGatewayProtocolSupport.kt`
- `DeviceConnectionManager.kt`
  - host seam only (`137 LOC`)
  - `DeviceConnectionManagerRuntime.kt`
  - `DeviceConnectionManagerConnectionSupport.kt`
  - `DeviceConnectionManagerReconnectSupport.kt`
  - `DeviceConnectionManagerIngressSupport.kt`

Delivered guardrail changes:

- `ConnectivityStructureTest` now enforces the host-only seam shape and extracted ownership seams
- `GodStructureGuardrailTest` now tracks both Wave 2B files as accepted rows under the service/manager/linter/gateway budget
- both public seams remain source-compatible for current callers

---

## 5. Verification Status

Wave 2B acceptance used focused app-core verification:

- `GattBleGatewayNotificationParsingTest`
- `DefaultDeviceConnectionManagerIngressTest`
- `RealConnectivityBridgeTest`
- `SimConnectivityRoutingTest`
- `ConnectivityStructureTest`
- `GodStructureGuardrailTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

Executed commands:

- `./gradlew :app-core:compileDebugUnitTestKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.connectivity.legacy.gateway.GattBleGatewayNotificationParsingTest --tests com.smartsales.prism.data.connectivity.legacy.DefaultDeviceConnectionManagerIngressTest --tests com.smartsales.prism.data.connectivity.RealConnectivityBridgeTest --tests com.smartsales.prism.ui.sim.SimConnectivityRoutingTest --tests com.smartsales.prism.data.connectivity.ConnectivityStructureTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`

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
- `docs/specs/flows/OnboardingFlow.md`
- `docs/reports/tests/L1-20260324-god-wave2b-connectivity.md`
