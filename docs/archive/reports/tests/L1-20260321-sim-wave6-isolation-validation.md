# L1 SIM Wave 6 Isolation Validation

Date: 2026-03-21
Status: Accepted
Owner: Codex

## Scope

Validate `T6.4` for the SIM standalone prototype:

1. prove smart and SIM runtime roots are distinct enough
2. prove persistence cross-contamination is controlled

## Source of Truth

- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`
- `docs/plans/sim-tracker.md`

## What Was Checked

### 1. Runtime-root isolation

Examined the SIM entry path and shell composition boundary:

- `SimMainActivity` mounts `SimShell`
- `SimMainActivity` does not boot `AgentShell`
- `SimShell` acquires `SimAgentViewModel` and `SimAudioDrawerViewModel`
- `SimShell` does not route through the smart chat root

Evidence:

- source-backed boundary test: `SimRuntimeIsolationTest`
- existing SIM shell behavior tests: `SimShellHandoffTest`

### 2. Persistence isolation

Examined the SIM-only persistence path:

- audio metadata persists in `sim_audio_metadata.json`
- SIM audio blobs use `sim_<audioId>.<ext>`
- SIM artifact files use `sim_<audioId>_artifacts.json`
- session metadata persists in `sim_session_metadata.json`
- durable per-session history persists in `sim_session_<sessionId>_messages.json`
- SIM session history filters out transient UI state
- cold start restores grouped sessions without auto-resuming an active session

Evidence:

- `SimAudioRepositoryNamespaceTest`
- `SimAudioRepositoryRecoveryTest`
- `SimSessionRepositoryTest`
- `SimAgentViewModelTest`

## Break-It Findings

Negative-path coverage exercised and green:

- dangling audio `boundSessionId` is cleared on startup
- missing audio-side binding is restored from a valid linked session
- duplicate persisted sessions claiming one audio item are normalized to the newest session
- deleting a linked session clears the audio binding
- empty SIM session store does not seed demo sessions and does not auto-select a session
- transient chat UI states are not written into durable session history

## Verification Run

### Build examiner

- `GRADLE_USER_HOME=/home/cslh-frank/main_app/.gradle ./gradlew :app-core:compileDebugKotlin`

### Focused L1 validation pack

- `GRADLE_USER_HOME=/home/cslh-frank/main_app/.gradle ./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimRuntimeIsolationTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest --tests com.smartsales.prism.ui.sim.SimAgentViewModelTest --tests com.smartsales.prism.data.session.SimSessionRepositoryTest --tests com.smartsales.prism.data.audio.SimAudioRepositoryNamespaceTest --tests com.smartsales.prism.data.audio.SimAudioRepositoryRecoveryTest`

## Drift / Notes

- A small unrelated unit-test compile drift in `ReminderReliabilityAdvisorTest` was repaired during this validation so the focused SIM pack could compile and run.
- This report is L1 only. It does not replace later L3 feature or isolation acceptance.

## Verdict

Accepted for `T6.4`.

Current evidence is sufficient to say:

- the SIM runtime root is distinct enough from the smart shell to continue safely
- SIM persistence now uses controlled, namespaced storage paths instead of silently sharing smart-app chat/audio history files

Remaining work moves to Wave 7 acceptance rather than more Wave 6 isolation proof.
