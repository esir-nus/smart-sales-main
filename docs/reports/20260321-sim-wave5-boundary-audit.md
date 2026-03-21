# SIM Wave 5 Boundary Audit

**Date:** 2026-03-21  
**Scope:** T5.2 hard-migration boundary freeze for SIM connectivity

---

## Objective

Freeze the current Wave 5 ownership split with repo evidence before starting T5.3 code isolation work.

The audit question is:

- what connectivity pieces SIM reuses unchanged
- what `SimShell` owns only as route/presentation glue
- whether scheduler or audio currently depend on connectivity
- which contamination points remain explicit debt rather than accepted architecture

---

## Evidence Summary

### 1. Shell owns connectivity routing only

Repo evidence:

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt`

Observed facts:

- `SimShell` owns `MODAL`, `SETUP`, and `MANAGER` route selection
- `SimShell` emits SIM-only route telemetry/logging
- `SimShell` does not own BLE scan, Wi-Fi provisioning, reconnect, disconnect, or badge file operations

Interpretation:

- Shell ownership is routing/presentation only
- backend connectivity truth remains outside the shell

### 2. Connectivity backend truth remains in existing contracts

Repo evidence:

- `app-core/src/main/java/com/smartsales/prism/ui/components/connectivity/ConnectivityViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityService.kt`
- `app-core/src/main/java/com/smartsales/prism/domain/pairing/PairingService.kt`
- `app-core/src/main/java/com/smartsales/prism/data/pairing/RealPairingService.kt`

Observed facts:

- `ConnectivityViewModel` consumes `ConnectivityBridge` and `ConnectivityService`
- `ConnectivityService` remains the reconnect/disconnect/unpair/Wi-Fi-update owner
- `PairingService` remains the setup runtime for scan, provisioning, and network checks
- the onboarding-derived SIM setup path reuses the existing pairing runtime rather than inventing a SIM-local backend

Interpretation:

- T5.2 should bless unchanged backend reuse rather than introducing a SIM-specific connectivity backend

### 3. Scheduler currently has no connectivity dependency

Repo evidence:

- `rg -n "ConnectivityBridge|BadgeConnectionState|connectivity" app-core/src/main/java/com/smartsales/prism/data/scheduler app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt app-core/src/main/java/com/smartsales/prism/ui/sim`

Observed facts:

- no scheduler runtime files imported `ConnectivityBridge` or `BadgeConnectionState`
- only `SimShell` carried connectivity route wiring in the searched surface

Interpretation:

- scheduler is already connectivity-free in code and the docs should freeze that as a non-negotiable rule

### 4. Audio legitimately consumes connectivity as badge ingress

Repo evidence:

- `app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`

Observed facts:

- badge audio pipeline consumes `ConnectivityBridge` for recording notifications, WAV download, and cleanup
- SIM audio repository consumes `ConnectivityBridge` for badge file listing/download sync

Interpretation:

- audio may depend on connectivity, but only as badge-origin recording ingress and badge file operations
- connectivity should not own audio/chat session flow or artifact persistence

### 5. Remaining contamination debt is real and should stay explicit

Repo evidence:

- `app-core/src/main/java/com/smartsales/prism/domain/pairing/PairingService.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingViewModel.kt`

Observed facts:

- `PairingService` leaks `legacy.BlePeripheral` through `DiscoveredBadge`
- `OnboardingViewModel` still injects `UserProfileRepository` even though the SIM pairing subset does not use onboarding profile/account behavior

Interpretation:

- these are T5.3 isolation debts, not accepted steady-state architecture

---

## Boundary Verdict

T5.2 should freeze this contract:

- reuse unchanged: `ConnectivityBridge`, `ConnectivityService`, `ConnectivityViewModel`, `PairingService`, onboarding pairing subset
- SIM shell owns only route state, overlay presentation, close behavior, and SIM route telemetry
- scheduler remains connectivity-free
- audio may use `ConnectivityBridge` only as badge-origin backend ingress/file-ops support
- the `BlePeripheral` leak and onboarding profile dependency remain explicit T5.3 debt

No `docs/cerb/interface-map.md` update is required for T5.2 because this slice documents existing edges rather than introducing new ones.
