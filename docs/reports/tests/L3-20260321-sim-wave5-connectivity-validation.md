# L3 On-Device Test Record: SIM Wave 5 Connectivity Validation

**Date**: 2026-03-21
**Tester**: Agent/User
**Target Build**: `:app-core:installDebug`

---

## 1. Test Context & Entry State
* **Objective**: Validate the corrected SIM connectivity flow on a physical badge after replacing the legacy setup screen with the onboarding-derived pairing subset and promoting manager-backed steady-state routing.
* **Testing Medium**: L3 Physical Device Test (real badge, real BLE/Wi-Fi pairing)
* **Initial Device State**: SIM launched on device with a previously known badge session that entered `DISCONNECTED` on connectivity entry, then fell back to `NeedsSetup` during reconnect.

## 2. Execution Plan
* **Trigger Action**: Opened SIM connectivity from chat badge entry, allowed reconnect to fall through to setup, completed real pairing, then reopened connectivity again.
* **Input Payload**: Physical badge `CHLE_Intelligent`, live BLE scan, live Wi-Fi provisioning, live post-provision network checks.

## 3. Expected vs Actual Results

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | SIM should use the onboarding-derived pairing flow instead of the old placeholder screen, then land in the connection-only manager. | Physical-badge setup used the onboarding-style pairing flow and then entered the connection-only manager. User-provided screenshot confirms the manager surface rendered after success. | ✅ |
| **Telemetry (GPS)** | `UI_STATE_EMITTED` route summaries should prove direct manager entry, setup start, and setup completion to manager. | `SIM connectivity manager direct entry opened` at `14:56:04`; `SIM connectivity setup started` at `14:56:05`; `SIM connectivity setup completed to manager` at `14:57:06`; direct manager re-entry repeated at `14:57:17` and `14:57:25`. | ✅ |
| **Log Evidence** | Real pairing runtime should scan, discover the badge, provision Wi-Fi, and complete network checks. | `PairingService` logged `startScan() called`, `State → DeviceFound`, `pairBadge() called`, `WiFi provisioning successful!`, and three successful network checks with IP `192.168.0.103`. | ✅ |
| **Negative Check** | Success must not drop straight back to chat or reopen the legacy placeholder path. Configured re-entry must not reopen bootstrap setup. | Success transitioned into manager, not chat. Later entry reopened manager directly from `CONNECTED`. No legacy placeholder screen or direct post-success chat drop was observed in logs or user report. | ✅ |

## 4. Deviation & Resolution Log
* **Observation**: The manager surface is currently full-screen and feels too heavy for the SIM support-module role.
  - **Impact**: Functional acceptance is still valid, but the current presentation is UX debt rather than the preferred final form.
  - **Follow-up**: Track a Wave 5 UI containment refinement so the manager no longer uses a full-screen presentation.

## 5. Final Verdict
**✅ SHIPPED**. The Wave 5 connectivity blocker is closed at L3 for the implemented routing contract: real pairing now uses the onboarding-derived setup path, success enters the SIM manager, and configured/disconnected re-entry resolves to the manager directly. The remaining note is presentation debt: the manager currently works but should not stay full-screen.
