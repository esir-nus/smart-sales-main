# SIM Wave 14 Execution Brief: Voice-Draft Composer

Status: Implemented
Date: 2026-03-28
Owner: Codex

## Objective

Reopen the SIM home/chat composer for one narrow interaction upgrade:

- blank draft shows a right-side mic action
- press-and-hold uses Android device speech recognition
- recognized text is inserted into the existing input field as an editable draft
- once drafted text exists, the right-side action becomes send

This slice is a SIM-only composer upgrade. It does not reopen the main smart-agent chat and it does not widen scheduler-follow-up voice mutation scope.

## Source Of Truth

- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`
- `docs/cerb/onboarding-interaction/interface.md`
- `docs/plans/sim-tracker.md`

## Scope

- SIM launcher-core home/chat composer
- active plain chat and active audio-grounded chat composer states
- onboarding-style six-bar handshake motion reused with SIM styling
- point-of-use microphone permission
- device-STT draft only, explicit send still required

## Out Of Scope

- main smart-agent chat
- scheduler drawer mic lane
- scheduler follow-up voice mutation widening
- badge audio ingestion or Tingwu flow changes
- Android file-picker routing changes

## Implementation Law

- keep `IAgentViewModel` unchanged
- keep `DeviceSpeechRecognizer` unchanged and reuse it from SIM
- own SIM voice-draft state inside `SimAgentViewModel`
- cancel stale voice sessions on background/disposal so late recognizer results cannot overwrite the draft
- keep attach on the left unchanged
- failure/no-match/permission-denied resets back to draftable composer without auto-send

## Verification Target

- `./gradlew :app-core:compileDebugKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimAgentViewModelTest --tests com.smartsales.prism.ui.sim.SimComposerContractTest`
- `./gradlew :app-core:compileDebugAndroidTestKotlin`
