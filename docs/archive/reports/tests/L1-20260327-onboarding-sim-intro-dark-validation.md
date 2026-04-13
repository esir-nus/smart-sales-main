# L1 Validation: SIM Onboarding Intro + Dark Lock

Date: 2026-03-27
Status: Passed
Scope: focused logic/build validation for the SIM onboarding intro-extension and dark-only onboarding treatment

## Commands

- `./gradlew :app-core:testDebugUnitTest --tests "com.smartsales.prism.ui.onboarding.OnboardingFlowTransitionTest" --tests "com.smartsales.prism.ui.onboarding.SimConnectivityPairingFlowTest" --tests "com.smartsales.prism.ui.sim.SimConnectivityRoutingTest"`

## Verified

- `OnboardingHost.SIM_CONNECTIVITY` now starts at `WELCOME`
- SIM onboarding now walks `WELCOME -> PERMISSIONS_PRIMER -> VOICE_HANDSHAKE -> HARDWARE_WAKE -> SCAN -> DEVICE_FOUND -> PROVISIONING -> COMPLETE`
- connectivity-owned pairing classification is still limited to `HARDWARE_WAKE`, `SCAN`, `DEVICE_FOUND`, and `PROVISIONING`
- SIM connectivity routing still preserves `NeedsSetup -> setup`, replay-to-setup behavior, and `SETUP -> MANAGER` completion behavior

## Notes

- onboarding preview/design-browser coverage for SIM intro states was updated in code during the same slice
- dark-only onboarding visuals and system-bar appearance were implemented, but no fresh L3 screenshot/device evidence was collected in this L1 pass
- `docs/reports/tests/L1-20260326-onboarding-wavea-logic-validation.md` remains historical evidence for the pre-follow-up SIM contract and should not be treated as the latest host-sequence proof
