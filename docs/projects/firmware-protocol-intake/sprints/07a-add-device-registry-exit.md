# Sprint 07A — add-device-registry-exit

## Header

- Project: firmware-protocol-intake
- Sprint: 07A
- Slug: add-device-registry-exit
- Date authored: 2026-04-25
- Author: Claude handoff interpreted by Codex after user correction
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none
- Ship path: one commit on `develop` at sprint close containing code, docs, contract closeout, and evidence.
- Blocked-by: none

## Demand

**User ask:** Post-onboarding `添加新设备` is not the same as first-run onboarding. The current reuse gets stuck on the onboarding success page with `请先完成一次真实日程体验。`; what is needed is a fully working post-onboarding registry/pairing sequence.

**Interpretation:** Repair the add-device route so it uses only the pairing/provisioning runtime needed to register a new badge, then exits back to the connectivity-owned surface after `PairingState.Success`. It must not enter scheduler quick start, expose a skip-to-home shortcut, or show the onboarding `COMPLETE` wrapper.

## Scope

In scope:

- `app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingStep.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingProvisioningSteps.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/onboarding/OnboardingFlowTransitionTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/onboarding/SimConnectivityPairingFlowTest.kt`
- `docs/specs/flows/OnboardingFlow.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/onboarding-interaction/interface.md`
- `docs/projects/firmware-protocol-intake/tracker.md`

Out of scope:

- New public pairing interfaces
- Pairing visual redesign beyond removing unlawful add-device skip/completion behavior
- Sprint 07 physical two-badge switching evidence
- Harmony work

## References

- `docs/specs/sprint-contract.md`
- `docs/projects/firmware-protocol-intake/tracker.md`
- `docs/specs/flows/OnboardingFlow.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/device-pairing/spec.md`
- `docs/cerb/device-pairing/interface.md`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingProvisioningSteps.kt`

## Success Exit Criteria

- `SIM_ADD_DEVICE` starts at `HARDWARE_WAKE` and does not require `SCHEDULER_QUICK_START`.
- `SIM_ADD_DEVICE` hides the local skip-to-quick-start/home shortcut on wake and provisioning/recovery surfaces.
- `SIM_ADD_DEVICE` closes the host surface directly after successful provisioning instead of routing to `COMPLETE`.
- `FULL_APP` and `SIM_CONNECTIVITY` still route through `SCHEDULER_QUICK_START -> COMPLETE`.
- Focused JVM tests pass:
  `./gradlew :app-core:testDebugUnitTest --tests '*OnboardingFlowTransitionTest*' --tests '*SimConnectivityPairingFlowTest*'`
- Build passes:
  `./gradlew :app:assembleDebug`

## Stop Exit Criteria

- The fix requires changing `PairingService`, `PairingState`, `DeviceRegistryManager`, or another public pairing interface.
- Focused tests or build fail on an unrelated pre-existing issue that cannot be resolved within this small patch.
- Device pairing still reaches the onboarding `COMPLETE` wrapper after this patch.

## Iteration Bound

- Max 2 iterations or 45 minutes, whichever hits first.

## Required Evidence Format

At close, append:

- Focused JVM test command and tail showing pass/fail.
- `:app:assembleDebug` command and tail showing pass/fail.
- If available, adb/logcat or user-observed device evidence that add-device pairing closes without the quick-start error.

## Iteration Ledger

<!-- Operator appends one entry per iteration below this line -->

### Iteration 1 — 2026-04-25

- Tried: changed `SIM_ADD_DEVICE` from an onboarding-completion route into a post-onboarding registry exit route. Successful provisioning now resets interaction state, cancels pairing state, and calls the host `onComplete()` directly. Add-device wake/provisioning surfaces no longer receive the quick-start skip action.
- Evaluator saw: focused onboarding transition tests passed, `:app:assembleDebug` passed, APK installed to connected device `fc8ede3e`.
- Next action: physical add-device pairing can now be rerun before Sprint 07 switching evidence.

## Closeout

- **Status:** success
- **Summary for project tracker:** Repaired post-onboarding `添加新设备` so successful provisioning closes back to connectivity after registration, with no quick-start requirement, skip-to-home shortcut, or onboarding `COMPLETE` wrapper.
- **Evidence artifacts:**
  - `./gradlew :app-core:testDebugUnitTest --tests '*OnboardingFlowTransitionTest*' --tests '*SimConnectivityPairingFlowTest*'` -> `BUILD SUCCESSFUL in 14s`
  - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL in 4s`
  - `adb -s fc8ede3e install -r app/build/outputs/apk/debug/app-debug.apk` -> `Success`
  - `adb -s fc8ede3e logcat -d -v time -t 300 | rg "SmartSalesConn|Onboarding|Pairing|Registry|AndroidRuntime"` -> current logcat was available; no add-device repro was performed in this run. Sample current connectivity line: `D/SmartSalesConn: RX [Notification]: Bat#100.00%`
- **Lesson proposals:** none
- **CHANGELOG line:** none
