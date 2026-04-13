# Onboarding Wave A Logic Validation

Date: 2026-03-26
Scope: Wave A onboarding logic/build verification for the shared full-app and SIM connectivity hosts
Verdict: Accepted

## 1. Contract Read

- `docs/specs/flows/OnboardingFlow.md`
- `docs/cerb/device-pairing/spec.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/sim-connectivity/spec.md`
- `docs/plans/onboarding-wavea-execution-brief.md`

## 2. What Changed

- updated the onboarding UI tracker row to lock the active gate as `Onboarding Wave A`
- added the Wave A execution brief with explicit allowed work, forbidden work, host matrix, and closeout rules
- added a repo-local onboarding preview/capture seam so Wave A visual QA can render exact host, step, and pairing-state combinations without changing production routing

## 3. Build And Test Evidence

Command run:

```text
./gradlew :app-core:compileDebugKotlin :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.onboarding.OnboardingFlowTransitionTest --tests com.smartsales.prism.ui.onboarding.SimConnectivityPairingFlowTest --tests com.smartsales.prism.ui.onboarding.PairingFlowViewModelTest --tests com.smartsales.prism.ui.sim.SimConnectivityRoutingTest
```

Observed result:

- `:app-core:compileDebugKotlin` passed
- `OnboardingFlowTransitionTest` passed
- `SimConnectivityPairingFlowTest` passed
- `PairingFlowViewModelTest` passed
- `SimConnectivityRoutingTest` passed

## 4. Validation Reading

### Spec examiner

- the host step contract still matches the active flow doc:
  - `FULL_APP` keeps the 8-step path
  - `SIM_CONNECTIVITY` still skips `WELCOME` and `VOICE_HANDSHAKE`
- the implementation keeps `PairingFlowViewModel` as the pairing runtime owner
- the SIM completion contract remains manager-first; no Wave A change widened or rewired that route

### Contract examiner

- no public API, route, or ownership contract changed
- the new onboarding preview/capture seam is internal-only and exists to support Wave A visual QA

### Build examiner

- focused Kotlin compile succeeded
- the four named unit-level verification targets all succeeded in one run

### Break-it examiner

- the preview seam covers the failure-state surfaces required by Wave A visual QA:
  - scan failure
  - permission-denied scan branch
  - provisioning progress
  - provisioning failure
- Wave A still does not claim L3 completion from previews alone; screenshot acceptance remains a separate gate

## 5. Remaining Open Work

- `docs/reports/tests/L3-20260326-onboarding-wavea-visual-validation.md` is still missing
- the UI tracker row must remain in `Visual QA` until the screenshot-backed L3 report is produced
