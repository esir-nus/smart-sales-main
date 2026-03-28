# Onboarding Interactive Handshake Execution Brief

Date: 2026-03-27
Status: Active

## Objective

Reopen onboarding for one approved behavior-change slice:

- keep `WELCOME` and `PERMISSIONS_PRIMER` as the intro prefix on both hosts
- replace the old abstract `VOICE_HANDSHAKE` with two real interaction steps:
  - `VOICE_HANDSHAKE_CONSULTATION`
  - `VOICE_HANDSHAKE_PROFILE`
- keep `HARDWARE_WAKE -> SCAN -> DEVICE_FOUND -> PROVISIONING -> COMPLETE` unchanged
- keep SIM completion routing on `SETUP -> MANAGER`

This slice is not a retroactive reinterpretation of Wave A or the SIM intro-dark follow-up. It is a new approved onboarding interaction slice with new AI/audio/profile ownership.

## Source Stack

- visual interaction source: `prototypes/onboarding-family/onboarding_interactive_prototype.html`
- onboarding flow source of truth: `docs/specs/flows/OnboardingFlow.md`
- onboarding interaction ownership: `docs/cerb/onboarding-interaction/spec.md`
- onboarding interaction interface: `docs/cerb/onboarding-interaction/interface.md`
- pairing runtime source of truth: `docs/cerb/device-pairing/spec.md`
- SIM routing source of truth: `docs/cerb/sim-connectivity/spec.md`
- UI tracker: `docs/plans/ui-tracker.md`

## Allowed Work

- add the two new onboarding intro steps on both hosts
- implement phone-mic hold-to-speak interaction before pairing begins
- transcribe onboarding audio through `AsrService`
- generate a short consultation reply through the existing executor/model registry path
- generate typed profile extraction JSON through a dedicated onboarding extraction seam
- persist extracted profile data into `UserProfileRepository` only after explicit CTA on the profile step
- allow calm retry and profile-save skip behavior without mutating pairing ownership
- update static review, previews, and design-browser presets for the new states
- sync docs, tests, and tracker rows for the new step contract

## Forbidden Work

- moving pairing ownership away from `PairingFlowViewModel`
- changing `HARDWARE_WAKE`, `SCAN`, `DEVICE_FOUND`, `PROVISIONING`, or completion routing semantics
- adding badge-audio or Tingwu dependencies to onboarding intro steps
- introducing a second profile schema outside `UserProfileRepository`
- silently parsing freeform LLM prose into saved profile data
- broad `OnboardingScreen.kt` cleanup beyond the minimal seam extraction needed for this slice

## Locked Host Matrix

- `FULL_APP`: `WELCOME -> PERMISSIONS_PRIMER -> VOICE_HANDSHAKE_CONSULTATION -> VOICE_HANDSHAKE_PROFILE -> HARDWARE_WAKE -> SCAN -> DEVICE_FOUND -> PROVISIONING -> COMPLETE`
- `SIM_CONNECTIVITY`: `WELCOME -> PERMISSIONS_PRIMER -> VOICE_HANDSHAKE_CONSULTATION -> VOICE_HANDSHAKE_PROFILE -> HARDWARE_WAKE -> SCAN -> DEVICE_FOUND -> PROVISIONING -> COMPLETE`

## Evidence Expectations

- refresh onboarding flow tests for the new step matrix
- add unit coverage for the onboarding interaction state owner, extraction parser, and experience-level derivation
- refresh preview/design-browser coverage for consultation, profile extraction, and failure states
- leave fresh L3 visual/device evidence as the next screenshot-validation gate if not collected in the same session
