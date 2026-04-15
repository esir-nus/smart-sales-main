# L1 SIM Wave 14 Voice-Draft Validation

Date: 2026-03-28
Status: Verified
Owner: Codex

## Scope

Validate the SIM voice-draft composer slice:

1. blank SIM composer shows mic-ready behavior instead of send-only behavior
2. successful device speech recognition writes an editable draft into the input field without auto-send
3. failure/cancel paths reset safely without mutating chat history
4. the updated SIM composer and onboarding gesture tests still compile

## Source Of Truth

- `docs/plans/sim-wave14-voice-draft-execution-brief.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`

## What Was Checked

### 1. SIM ViewModel owns the voice-draft state

- `SimAgentViewModel` now owns SIM-local recording/processing/permission state
- successful recognition writes transcript into `inputText`
- send still remains explicit
- cancelled or stale recognizer results do not write late drafts

Evidence:

- `SimAgentViewModelTest.voice draft success populates input without auto send`
- `SimAgentViewModelTest.voice draft no match resets without mutating history`
- `SimAgentViewModelTest.cancel voice draft blocks late recognizer result from writing input`

### 2. Composer contract switched from send-only to mic-or-send

- SIM composer source now computes `showVoiceMic = voiceDraftEnabled && text.isBlank()`
- both the launcher-core home-hero monolith and the fallback SIM input bar use the same mic/send rule
- attach behavior remains unchanged

Evidence:

- `SimComposerContractTest.sim composer switches between mic and send based on draft state`

### 3. Gesture/UI path compiles with the new SIM and onboarding seams

- SIM Android-test composer gesture coverage compiles with the new voice-draft override path
- onboarding mic-footer instrumentation compiles after the shared handshake-bar extraction

## Verification Run

- `./gradlew :app-core:compileDebugKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimAgentViewModelTest --tests com.smartsales.prism.ui.sim.SimComposerContractTest`
- `./gradlew :app-core:compileDebugAndroidTestKotlin`

## Verdict

Verified for the current Wave 14 slice.

Current evidence is sufficient to say:

- SIM chat now supports device-STT draft input on the normal home/chat composer
- the transcript becomes editable input and does not auto-send
- the implementation stays SIM-local and does not widen the main smart-agent chat
