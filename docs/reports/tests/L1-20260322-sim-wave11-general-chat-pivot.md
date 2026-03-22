# L1 SIM Wave 11 General Chat Pivot Validation

Date: 2026-03-22
Status: Accepted
Owner: Codex

## Scope

Validate the Wave 11 SIM chat pivot:

1. blank/new SIM chat answers normally instead of showing audio-only guidance
2. persona plus user metadata are part of the SIM general-chat prompt
3. audio can be attached into the current session without discarding prior turns

## Source of Truth

- `docs/plans/sim-tracker.md`
- `docs/to-cerb/sim-standalone-prototype/concept.md`
- `docs/to-cerb/sim-standalone-prototype/mental-model.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-audio-chat/spec.md`

## What Was Checked

### 1. General chat is now real

Examined `SimAgentViewModel` general-send behavior:

- general sessions no longer return the old audio-only guidance string
- SIM now builds a normal chat prompt with user profile metadata and recent session history
- general replies still stay inside the SIM boundary and do not claim smart-agent tooling

Evidence:

- `SimAgentViewModelTest.general send uses persona backed reply instead of audio only guidance`

### 2. Mid-session audio attachment keeps session continuity

Examined the audio handoff path:

- selecting audio while a normal SIM chat session is active reuses the current session
- prior turns remain in history
- the session gains or switches active audio binding instead of forcing a new audio-only thread

Evidence:

- `SimAgentViewModelTest.selectAudioForChat reuses current general session and preserves prior turns`

### 3. Existing SIM audio/chat behavior remains intact

Focused regression coverage also stayed green for:

- persisted artifact-grounded replies
- artifact-history fallback when repository copy is missing
- explicit missing-artifact guidance
- shell handoff helpers used by SIM routing

Evidence:

- `SimAgentViewModelTest`
- `SimShellHandoffTest`

## Verification Run

### Build examiner

- `./gradlew :app-core:compileDebugKotlin`

### Focused L1 validation pack

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimAgentViewModelTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest`

## Verdict

Accepted for the current Wave 11 implementation slice.

Current evidence is sufficient to say:

- normal SIM chat is directly usable again
- SIM general chat now uses persona plus user metadata instead of blocking on audio first
- audio can be attached into the same ongoing SIM session without losing prior chat memory
