# God Wave 2C Execution Brief

**Status:** Planned  
**Date:** 2026-03-24  
**Wave:** 2C  
**Mission:** `SimAudioRepository.kt` structural cleanup  
**Primary Tracker:** `docs/plans/god-tracker.md`  
**Structure Law:** `docs/specs/code-structure-contract.md`  
**Validation Report:** pending until implementation

---

## 1. Purpose

Wave 2C is the SIM audio data cleanup slice in Wave 2.

It targets `SimAudioRepository.kt`, which currently mixes persistence, artifact IO, session binding, and sync/transcription coordination inside one large repository file.

Wave 2C should reduce that file into stable SIM-owned support files while keeping the consumer-facing repository seam source-compatible.

---

## 2. Governing Docs

Wave 2C remains bounded by current SIM audio and connectivity ownership:

- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/audio-management/interface.md`
- `docs/cerb/badge-audio-pipeline/spec.md`
- `docs/cerb/badge-audio-pipeline/interface.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`
- `docs/cerb/connectivity-bridge/spec.md`

Wave 2C must preserve SIM namespace isolation and current badge-sync behavior.

---

## 3. Wave 2C Law

Wave 2C may do:

- public-seam reduction for `SimAudioRepository.kt`
- extraction of persistence/store support
- extraction of artifact IO support
- extraction of session-binding support
- extraction of sync/transcription coordination support
- local structure-test and guardrail updates needed for the accepted shape
- tracker/doc sync for the delivered split

Wave 2C must **not** do:

- consumer-facing repository API changes
- SIM audio/chat behavior expansion beyond current accepted docs
- storage namespace changes that would contaminate the smart app runtime
- product-default ingress changes under the banner of structural cleanup

---

## 4. Planned Structure

Wave 2C leaves `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt` as the public seam file.

Planned extraction map:

- repository seam
- persistence/store support
- artifact IO support
- session-binding support
- sync/transcription coordinator

Exact filenames may follow the accepted ownership shape, but the split must keep SIM-owned storage and audio/chat boundaries obvious.

---

## 5. Verification Target

Wave 2C acceptance should use focused app-core verification:

- `SimAudioRepositoryNamespaceTest`
- `SimAudioRepositoryRecoveryTest`
- `SimAudioDrawerViewModelTest`
- `SimAudioRepositoryStructureTest`
- `GodStructureGuardrailTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

---

## 6. Wave 2C Acceptance Bar

Wave 2C is complete only when:

- `SimAudioRepository.kt` is under budget or backed by a valid active exception
- the tracker row moves from `Proposed` to `Accepted`
- focused repository behavior and structure tests stay green
- SIM namespace/storage behavior remains compatible
- the public repository seam remains source-compatible

---

## 7. Related Documents

- `docs/plans/god-tracker.md`
- `docs/plans/tracker.md`
- `docs/plans/god-wave2-execution-brief.md`
- `docs/specs/code-structure-contract.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
