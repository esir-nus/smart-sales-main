# Onboarding Wave A Execution Brief

Date: 2026-03-26
Status: Historical (Superseded by later onboarding follow-up slices)

## Objective

Close the already-landed host-driven onboarding transplant through visual QA and acceptance evidence.

Wave A is a polish-and-verification slice. It is not a redesign wave, not a routing-change wave, and not a structure-cleanup wave.

## Source Stack

- visual source of truth: `prototypes/onboarding-family/onboarding_interactive_prototype.html`
- onboarding flow source of truth: `docs/specs/flows/OnboardingFlow.md`
- pairing runtime source of truth: `docs/cerb/device-pairing/spec.md`
- SIM routing source of truth: `docs/core-flow/sim-shell-routing-flow.md`
- current connectivity/runtime routing: `docs/specs/flows/OnboardingFlow.md`, `docs/cerb/connectivity-bridge/spec.md`, and `docs/cerb/connectivity-bridge/interface.md`
- external onboarding transplant brief: supporting context only, not the portable primary source

## Allowed Work

- spacing, typography, card/scrim treatment, and motion timing polish
- per-step visual cleanup inside the already-approved host flow
- host-specific completion surface polish
- screenshot-confirmed scan/provisioning retry and error presentation polish
- minimal internal preview or debug seams needed to render every approved Wave A screenshot state

## Forbidden Work

- step-order rewrites
- new onboarding tail steps
- routing-contract changes
- new production ownership seams
- silent widening into naming, account, profile, notification, or OEM guidance return
- `OnboardingScreen.kt` structural cleanup beyond the minimal capture seam
- any SIM completion change away from accepted `SETUP -> MANAGER`

## Locked Host Matrix

- historical Wave A matrix is preserved here only as original execution context; active behavior now lives in `docs/specs/flows/OnboardingFlow.md`

## Locked Invariants

- preserve the `OnboardingHost` dual-host contract
- preserve the current `OnboardingStep` sequence for each host
- preserve `PairingFlowViewModel` as the pairing runtime owner
- preserve SIM `SETUP -> MANAGER` completion behavior
- preserve the current no-auto-connect, no fake-chat voice handshake, and no account/profile/notification-tail rules

## Acceptance Evidence

Wave A closes with two reports:

- logic/build validation: `docs/reports/tests/L1-20260326-onboarding-wavea-logic-validation.md`
- visual acceptance: `docs/reports/tests/L3-20260326-onboarding-wavea-visual-validation.md`

The visual acceptance set must cover:

- `FULL_APP`: all 8 active steps
- `SIM_CONNECTIVITY`: all 6 active steps
- scan failure plus retry-ready recovery state
- provisioning failure plus retry-ready recovery state
- both completion surfaces with host-correct CTA and result copy

The capture path must use the repo-local onboarding preview/debug seam rather than ad hoc manual navigation alone.

## Drift Rule

If QA finds behavior or spec drift:

- record the drift in the Wave A acceptance report
- keep the current Wave A scope narrow
- move any behavior change into a separate follow-up slice

## Closeout Rule

`docs/plans/ui-tracker.md` moves this row from `Visual QA` to `Shipped` only after both the `L1` logic report and the `L3` visual validation report exist.
