# L3 SIM Wave 7 Isolation Acceptance

Date: 2026-03-22
Status: Accepted
Owner: Codex

## Scope

Close `T7.2` for the SIM standalone prototype by proving:

1. the smart root still boots correctly
2. the SIM root still boots separately
3. Wave 6 runtime/storage isolation evidence still holds with no new Wave 7 cross-contamination evidence

This is an isolation-acceptance slice, not full smart-app feature acceptance.

## Source of Truth

### SIM behavioral authority

- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/plans/sim-tracker.md`

### Smart-root acceptance basis for this gate

No dedicated smart core-flow doc was loaded for this gate.
The smart-side check is therefore limited to root/boot regression sanity based on:

- `app-core/src/main/AndroidManifest.xml`
- `app-core/src/main/java/com/smartsales/prism/AgentMainActivity.kt`
- current runtime behavior on device

### Reused lower-layer evidence

- `docs/reports/tests/L1-20260321-sim-wave6-isolation-validation.md`
- `docs/reports/tests/L3-20260322-sim-wave7-feature-acceptance.md`

## What Was Checked

### 1. Smart-root launcher and branch contract

Source-backed sanity check:

- `AndroidManifest.xml` still declares `AgentMainActivity` as the `MAIN` / `LAUNCHER` activity
- `SimMainActivity` remains present but is not the launcher
- `AgentMainActivity` still branches only to `OnboardingScreen` or `AgentShell`

Interpretation:

- this gate does not claim whole smart-app feature coverage
- it does prove the default root contract is still smart-owned rather than silently replaced by SIM

### 2. Reconfirmed lower-layer isolation pack

Re-ran the accepted Wave 6 isolation pack to avoid relying on stale assumptions:

- `SimRuntimeIsolationTest`
- `SimShellHandoffTest`
- `SimAgentViewModelTest`
- `SimSessionRepositoryTest`
- `SimAudioRepositoryNamespaceTest`
- `SimAudioRepositoryRecoveryTest`

Result:

- green
- no new drift against the previously accepted Wave 6 runtime/storage boundary proof

### 3. Device smart-root boot sanity

Executed:

- `adb shell am start -S -W -n com.smartsales.prism/com.smartsales.prism.AgentMainActivity`

Observed:

- launch returned `Status: ok`
- launch returned `Activity: com.smartsales.prism/.AgentMainActivity`
- logcat recorded:
  - `Start proc ... for top-activity {com.smartsales.prism/com.smartsales.prism.AgentMainActivity}`
  - `Displayed com.smartsales.prism/.AgentMainActivity`

Interpretation:

- the smart root still boots into `AgentMainActivity`
- no evidence in this acceptance slice shows SIM becoming the default root

### 4. Device SIM-root boot sanity

Executed:

- `adb shell am start -S -W -n com.smartsales.prism/com.smartsales.prism.SimMainActivity`

Observed:

- launch returned `Status: ok`
- launch returned `Activity: com.smartsales.prism/.SimMainActivity`
- logcat recorded:
  - `Start proc ... for top-activity {com.smartsales.prism/com.smartsales.prism.SimMainActivity}`
  - `Displayed com.smartsales.prism/.SimMainActivity`

Interpretation:

- the explicit SIM root still boots separately from the smart root
- no evidence in this slice shows `SimMainActivity` falling back into `AgentMainActivity`

### 5. Device storage namespace sanity

Executed:

- `adb shell run-as com.smartsales.prism ls files`

Observed SIM-owned files on device:

- `sim_audio_metadata.json`
- `sim_session_metadata.json`
- `sim_session_<sessionId>_messages.json`
- multiple `sim_*` audio/artifact files

Observed non-SIM files in the same listing:

- `OSSLog`
- `profileInstalled`

Not observed in this device-side check:

- `audio_metadata.json`
- generic session metadata filenames

Interpretation:

- device-side evidence supports the existing SIM namespace contract
- exact filename ownership remains anchored by the Wave 6 L1 report and tests, not by this one ad-hoc listing alone

## Verification Run

### Build / install

- `GRADLE_USER_HOME=/home/cslh-frank/main_app/.gradle ./gradlew :app-core:compileDebugKotlin`
- `GRADLE_USER_HOME=/home/cslh-frank/main_app/.gradle ./gradlew :app-core:installDebug`

### Focused L1 isolation pack

- `GRADLE_USER_HOME=/home/cslh-frank/main_app/.gradle ./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimRuntimeIsolationTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest --tests com.smartsales.prism.ui.sim.SimAgentViewModelTest --tests com.smartsales.prism.data.session.SimSessionRepositoryTest --tests com.smartsales.prism.data.audio.SimAudioRepositoryNamespaceTest --tests com.smartsales.prism.data.audio.SimAudioRepositoryRecoveryTest`

### L3 device commands

- `adb shell am start -S -W -n com.smartsales.prism/com.smartsales.prism.AgentMainActivity`
- `adb shell am start -S -W -n com.smartsales.prism/com.smartsales.prism.SimMainActivity`
- `adb shell run-as com.smartsales.prism ls files`

## Drift / Notes

- This gate proves root/boot isolation and no obvious new cross-contamination evidence on the tested build/device session.
- This gate does not claim full smart-app functional regression coverage.
- Reminder visibility acceptance is already covered elsewhere and is not part of this `T7.2` verdict.

## Verdict

Accepted for `T7.2`.

Current evidence is sufficient to say:

- the smart root still boots correctly through `AgentMainActivity`
- the SIM root still boots separately through `SimMainActivity`
- Wave 6 storage/DI isolation remains intact with no conflicting Wave 7 evidence in this acceptance slice

This report closes `T7.2`; the tracker/doc closeout was completed in the same session under `T7.3`.
