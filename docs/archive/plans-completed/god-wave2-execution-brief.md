# God Wave 2 Execution Brief

**Status:** Wave 2A L1 Accepted / Wave 2B L1 Accepted / Wave 2C L1 Accepted
**Date:** 2026-03-24
**Wave:** 2
**Mission:** secondary business-logic god-file cleanup
**Primary Tracker:** `docs/projects/god-file-cleanup/tracker.md`
**Structure Law:** `docs/specs/code-structure-contract.md`
**Validation Report:** accepted per subwave

---

## 1. Purpose

Wave 2 is the first post-trunk cleanup campaign in the god-file tracker.

It intentionally shifts away from shared UI hosts and ViewModel trunks into secondary business-logic files whose mixed roles now slow safe edits, transplant support, and future feature work.

Wave 2 is explicitly **UI-safe**:

- it does not touch the large UI surfaces currently active in ongoing UI development
- it narrows the campaign to scheduler/domain, connectivity transport, and SIM audio data ownership

---

## 2. Wave 2 Law

Wave 2 may do:

- structural cleanup of `SchedulerLinter.kt`
- structural cleanup of `GattBleGateway.kt`
- structural cleanup of `DeviceConnectionManager.kt`
- structural cleanup of `SimAudioRepository.kt`
- guardrail expansion and local structure tests needed for each accepted business-logic slice
- tracker, interface-map, and execution-brief sync needed for the locked Wave 2 scope

Wave 2 must **not** do:

- `SimAudioDrawer.kt` cleanup
- `OnboardingScreen.kt` cleanup
- new public API changes just to satisfy file split goals
- new repo-wide abstractions or DI seams with no stable extracted role
- behavior expansion disguised as structural cleanup

---

## 3. Planned Delivery Order

Wave 2 is delivered as three ordered subwaves:

- **Wave 2A**: `SchedulerLinter.kt`
- **Wave 2B**: `GattBleGateway.kt` and `DeviceConnectionManager.kt`
- **Wave 2C**: `SimAudioRepository.kt`

Execution order is strict:

- do not start 2B before 2A docs and ownership are locked
- do not start 2C before 2B is accepted or deliberately paused

Current progress:

- Wave 2A is accepted
- Wave 2B is accepted
- Wave 2C is accepted
- Wave 2 is now complete as a business-logic-only cleanup wave

---

## 4. Deferred UI Safety Lane

These tracked files are intentionally out of Wave 2 scope:

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingScreen.kt`

Reason:

- the repo is still in an active UI-development window
- widening Wave 2 into large UI-surface cleanup would increase delivery risk
- those files remain tracked debt, but they belong to a later UI-safe cleanup wave

---

## 5. Guardrail Strategy

Wave 2 extends the Wave 1A guardrail model instead of replacing it.

Guardrail split:

- `GodStructureGuardrailTest` remains the tracker/contract validity guardrail
- each subwave adds a local structure test for the files it owns

Planned local structure tests:

- `SchedulerLinterStructureTest`
- `ConnectivityStructureTest`
- `SimAudioRepositoryStructureTest`

---

## 6. Wave 2 Closeout Bar

Wave 2 is complete only when:

- `docs/projects/god-file-cleanup/tracker.md` shows all three Wave 2 targets as accepted rows
- `docs/plans/tracker.md` reflects Wave 2C L1 acceptance
- this umbrella brief and the three subwave briefs remain synced to the delivered support-file shapes
- each subwave keeps its focused structure test and tracker/contract guardrail coverage green

---

## 7. Related Documents

- `docs/projects/god-file-cleanup/tracker.md`
- `docs/plans/tracker.md`
- `docs/plans/god-wave2a-execution-brief.md`
- `docs/plans/god-wave2b-execution-brief.md`
- `docs/plans/god-wave2c-execution-brief.md`
- `docs/specs/code-structure-contract.md`
