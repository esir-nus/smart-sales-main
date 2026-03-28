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
- transcribe onboarding audio through an onboarding-owned device speech recognizer fast lane
- generate a short consultation reply through onboarding-owned deterministic local logic
- generate typed profile extraction data through onboarding-owned deterministic local logic
- persist extracted profile data into `UserProfileRepository` only after explicit CTA on the profile step
- allow calm retry and profile-save skip behavior without mutating pairing ownership
- allow invisible deterministic fallback with a short artificial dwell when device STT is unavailable or fails
- update static review, previews, and design-browser presets for the new states
- sync docs, tests, and tracker rows for the new step contract

## Handshake Footer Motion Contract

The shared onboarding mic footer for `VOICE_HANDSHAKE_CONSULTATION` and `VOICE_HANDSHAKE_PROFILE` must follow this transplant contract:

- render a 6-bar handshake above the mic button with `6.dp` bar width and `6.dp` gap inside a vertical slot that can visibly grow up to `40.dp`
- keep the sample-prompt hint line visible between the bars and the mic button while the footer is present
- idle handshake auto-starts after `600ms` and loops on a `3000ms` breathing cycle between `8.dp` and `20.dp` height with staggered per-bar phase offsets and subdued opacity
- recording switches on pointer-down to a `600ms` faster cycle between `10.dp` and `40.dp` height with full cyan intensity
- release must not hide the handshake or mic button immediately; processing returns to the idle-breathing handshake while the processing label is shown
- the footer disappears only when the next visible result state takes over:
  - consultation: transcript / AI reply / completion reveal
  - profile: transcript / acknowledgement / extraction card / CTA state
- prefer one `rememberInfiniteTransition` with math phase offsets over per-bar coroutine animators

## Forbidden Work

- moving pairing ownership away from `PairingFlowViewModel`
- changing `HARDWARE_WAKE`, `SCAN`, `DEVICE_FOUND`, `PROVISIONING`, or completion routing semantics
- adding badge-audio or Tingwu dependencies to onboarding intro steps
- routing onboarding happy-path voice through the main batch `AsrService`
- routing onboarding happy-path reply/extraction through the main business executor/model registry path
- introducing a second profile schema outside `UserProfileRepository`
- silently parsing freeform LLM prose into saved profile data
- broad `OnboardingScreen.kt` cleanup beyond the minimal seam extraction needed for this slice

## Locked Host Matrix

- `FULL_APP`: `WELCOME -> PERMISSIONS_PRIMER -> VOICE_HANDSHAKE_CONSULTATION -> VOICE_HANDSHAKE_PROFILE -> HARDWARE_WAKE -> SCAN -> DEVICE_FOUND -> PROVISIONING -> COMPLETE`
- `SIM_CONNECTIVITY`: `WELCOME -> PERMISSIONS_PRIMER -> VOICE_HANDSHAKE_CONSULTATION -> VOICE_HANDSHAKE_PROFILE -> HARDWARE_WAKE -> SCAN -> DEVICE_FOUND -> PROVISIONING -> COMPLETE`

## Evidence Expectations

- refresh onboarding flow tests for the new step matrix
- add unit coverage for the onboarding interaction state owner, extraction parser, and experience-level derivation
- refresh preview/design-browser coverage for consultation idle / recording / processing / complete, profile idle / recording / processing / extracted, and the existing failure states
- leave fresh L3 visual/device evidence as the next screenshot-validation gate if not collected in the same session
