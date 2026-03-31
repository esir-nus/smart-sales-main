# God Wave 2C Execution Brief

**Status:** L1 Accepted  
**Date:** 2026-03-24  
**Wave:** 2C  
**Mission:** `SimAudioRepository.kt` structural cleanup  
**Primary Tracker:** `docs/plans/god-tracker.md`  
**Structure Law:** `docs/specs/code-structure-contract.md`  
**Current Reading Priority:** Historical execution reference only; not current source of truth.  
**Current Active Truth:** `docs/plans/god-tracker.md`, `docs/plans/tracker.md`, `docs/specs/code-structure-contract.md`, `docs/core-flow/sim-audio-artifact-chat-flow.md`, `docs/cerb/audio-management/spec.md`, `docs/cerb/audio-management/interface.md`, `docs/cerb/tingwu-pipeline/spec.md`, `docs/cerb/tingwu-pipeline/interface.md`, `docs/cerb/badge-audio-pipeline/spec.md`, `docs/cerb/badge-audio-pipeline/interface.md`, `docs/cerb/connectivity-bridge/spec.md`  
**Historical Deprecated Context:** `docs/cerb/sim-audio-chat/spec.md`, `docs/cerb/sim-audio-chat/interface.md`  
**Validation Report:** `docs/reports/tests/L1-20260324-god-wave2c-sim-audio-repository.md`

---

## 1. Purpose

Wave 2C is the SIM audio data cleanup slice in Wave 2.

It targets `SimAudioRepository.kt`, which currently mixes persistence, artifact IO, session binding, and sync/transcription coordination inside one large repository file.

Wave 2C now reduces that file into stable SIM-owned support files while keeping the consumer-facing repository seam source-compatible.

---

## 2. Current Active Truth

Wave 2C should now be read against the shared audio, Tingwu, badge-ingest, and connectivity docs:

- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/audio-management/interface.md`
- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/cerb/tingwu-pipeline/interface.md`
- `docs/cerb/badge-audio-pipeline/spec.md`
- `docs/cerb/badge-audio-pipeline/interface.md`
- `docs/cerb/connectivity-bridge/spec.md`

Historical deprecated context at the time:

- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`

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

## 4. Delivered Structure

Wave 2C leaves `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt` as the public seam file.

Delivered extraction map:

- `SimAudioRepository.kt`
  - host seam only (`105 LOC`)
- `SimAudioRepositoryRuntime.kt`
  - runtime state, scope, mutexes, metadata handle, seed definitions
- `SimAudioRepositoryStoreSupport.kt`
  - metadata persistence, namespace helpers, local import, seed backfill, session binding
- `SimAudioRepositoryArtifactSupport.kt`
  - artifact file IO, debug artifact builders, debug scenario seeding
- `SimAudioRepositorySyncSupport.kt`
  - badge-sync preflight, telemetry/message helpers, duplicate filtering, download import loop
- `SimAudioRepositoryTranscriptionSupport.kt`
  - transcription start/resume flow, OSS/Tingwu coordination, progress/completion/failure handling

Wave 2C also:

- keeps the `SimAudioRepository` constructor and callable method surface source-compatible for current SIM consumers
- folds session-binding ownership into the store support file rather than creating an extra tiny support shard
- adds a focused structure regression test for the accepted SIM audio split
- keeps SIM namespace/storage filenames unchanged

---

## 5. Verification Status

Wave 2C acceptance used focused app-core verification:

- `SimAudioRepositoryNamespaceTest`
- `SimAudioRepositoryRecoveryTest`
- `SimAudioDebugScenarioTest`
- `SimAudioDrawerViewModelTest`
- `SimAudioRepositoryStructureTest`
- `GodStructureGuardrailTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

Executed commands:

- `./gradlew :app-core:compileDebugUnitTestKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.audio.SimAudioRepositoryNamespaceTest --tests com.smartsales.prism.data.audio.SimAudioRepositoryRecoveryTest --tests com.smartsales.prism.data.audio.SimAudioDebugScenarioTest --tests com.smartsales.prism.data.audio.SimAudioRepositoryStructureTest --tests com.smartsales.prism.ui.sim.SimAudioDrawerViewModelTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`

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
- `docs/reports/tests/L1-20260324-god-wave2c-sim-audio-repository.md`
