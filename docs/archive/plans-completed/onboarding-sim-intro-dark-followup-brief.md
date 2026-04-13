# Onboarding SIM Intro + Dark Lock Follow-Up Brief

Date: 2026-03-27
Status: Historical (Superseded by `docs/plans/onboarding-interactive-handshake-execution-brief.md`)

## Objective

Reopen the shared onboarding family for one narrow follow-up slice:

- keep onboarding dark-only even when the surrounding app theme is light
- extend the `SIM_CONNECTIVITY` host so it reuses `WELCOME` and the pre-interactive-handshake intro before the existing pairing path

This brief is a follow-up slice after Wave A. It must not be backfilled into `docs/plans/onboarding-wavea-execution-brief.md` as if Wave A originally allowed the host-sequence rewrite.

## Source Stack

- visual source of truth: `prototypes/onboarding-family/onboarding_interactive_prototype.html`
- onboarding flow source of truth: `docs/specs/flows/OnboardingFlow.md`
- pairing runtime source of truth: `docs/cerb/device-pairing/spec.md`
- current SIM routing source of truth: `docs/specs/flows/OnboardingFlow.md`
- shared connectivity runtime source of truth: `docs/cerb/connectivity-bridge/spec.md` and `docs/cerb/connectivity-bridge/interface.md`
- UI campaign tracker: `docs/plans/ui-tracker.md`

## Allowed Work

- historical SIM host step-sequence expansion to `WELCOME -> PERMISSIONS_PRIMER ->` the later-replaced single intro handshake `-> HARDWARE_WAKE -> SCAN -> DEVICE_FOUND -> PROVISIONING -> COMPLETE`
- dark-lock onboarding backgrounds and system-bar appearance while onboarding is visible
- keep approved onboarding background reuse anchored to the shipped SIM aurora where that page already depends on it
- preview and static-review seam updates for the added SIM intro states
- focused logic verification and doc sync for the new SIM host contract

## Forbidden Work

- new host types or launcher-routing changes
- pairing ownership changes away from `PairingFlowViewModel`
- SIM completion changes away from accepted `SETUP -> MANAGER`
- onboarding tail-step widening into naming, profile, notification, or OEM guidance
- unrelated structural cleanup inside `OnboardingScreen.kt`

## Locked Host Matrix

- this brief is now superseded by the interactive-handshake execution brief and should be treated as historical context only

## Evidence Expectations

- refresh L1 logic evidence because `docs/reports/tests/L1-20260326-onboarding-wavea-logic-validation.md` records the previous SIM-skips-intro behavior; current focused proof is `docs/reports/tests/L1-20260327-onboarding-sim-intro-dark-validation.md`
- historical note: preview/static capture coverage for SIM `WELCOME` and the old single intro-handshake page was required in this narrower slice
- leave L3 visual acceptance as a follow-up capture task if fresh screenshots are not collected in the same session
