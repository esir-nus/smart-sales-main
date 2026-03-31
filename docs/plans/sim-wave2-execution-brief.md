# SIM Wave 2 Execution Brief

**Status:** Ready
**Date:** 2026-03-19
**Wave:** 2
**Mission:** SIM standalone prototype
**Behavioral Authority:** `docs/core-flow/sim-audio-artifact-chat-flow.md`
**Current Reading Priority:** Historical reference only; not current source of truth.
**Historical Owning Spec At The Time:** `docs/cerb/sim-audio-chat/spec.md`
**Historical Boundary Brief:** `docs/plans/sim_implementation_brief.md`
**Current Active Truth:** `docs/plans/tracker.md`, `docs/core-flow/sim-audio-artifact-chat-flow.md`, `docs/cerb/audio-management/spec.md`, `docs/cerb/audio-management/interface.md`, `docs/cerb/tingwu-pipeline/spec.md`, `docs/cerb/tingwu-pipeline/interface.md`, `docs/cerb/interface-map.md`

---

## 1. Purpose

This brief compresses Wave 2 into one practical handoff artifact.

Use it when Wave 2 implementation starts, when Wave 2 is reviewed, and when later waves need to confirm what audio/artifact work was intentionally delivered before deeper discussion-chat work begins.

This is not the product PRD and not the long-term boundary constitution.
It is the execution brief for the SIM audio informational-mode wave.

---

## 2. Required Read Order

Before coding Wave 2, read in this order:

1. `docs/plans/tracker.md`
2. `docs/core-flow/sim-audio-artifact-chat-flow.md`
3. `docs/cerb/audio-management/spec.md`
4. `docs/cerb/audio-management/interface.md`
5. `docs/cerb/tingwu-pipeline/spec.md`
6. `docs/cerb/tingwu-pipeline/interface.md`
7. `docs/cerb/interface-map.md`
8. this file as historical execution context

If code reality forces a boundary or ownership change, update the main tracker and the current shared audio/chat docs in the same session.

---

## 3. Wave Objective

Wave 2 exists to make the SIM audio drawer real as an informational artifact surface.

Wave 2 must deliver:

- SIM-safe audio persistence namespace
- real audio inventory in the SIM drawer
- transcribed-audio reuse without rerunning Tingwu
- source-led artifact rendering
- explicit transcription lifecycle states
- readability polishing with raw-output fallback
- transparent presentation states that remain clearly cosmetic

Wave 2 is successful only if users can browse audio and open readable Tingwu-backed artifacts in SIM without contaminating the smart app or quietly reviving smart runtime assumptions.

---

## 4. Out Of Scope

Do not try to finish these in Wave 2:

- full discussion-chat quality and context injection depth
- broader `Ask AI` conversation behavior beyond keeping the handoff intact
- scheduler behavior changes
- connectivity rewrites outside audio inventory input decisions
- generalized smart-agent interpretation of Tingwu output
- package/applicationId extraction

Those belong to later waves even if the audio drawer already contains the entry into discussion mode.

---

## 5. Allowed Touch Map

### New Or Expanded SIM Artifacts Expected

- `SimAudioDrawer` deeper implementation
- `SimAudioDrawerViewModel` deeper runtime state
- SIM-owned audio repository adapter or namespace wrapper
- SIM-owned artifact formatter / renderer seam
- SIM readability-polisher seam
- SIM transparent-state presentation contract

### Existing Files Safe To Reuse Through Controlled Seams

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt` only through explicit SIM-safe namespacing or an adapter
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt` only if its session/runtime assumptions are audited as SIM-safe for this wave
- shared audio domain/data models where ownership remains generic

### Conditional Reuse

- `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt` only after its state/session seam is clearly separated from shared smart runtime behavior
- SmartBadge sync may be included only if the current connectivity/audio seam proves clean enough for SIM-safe inventory loading without reopening Wave 0 boundary decisions

### Forbidden Runtime Assumptions

- using smart chat/session orchestration as the owner of SIM audio
- rerunning Tingwu for already-transcribed audio by default
- inventing missing sections locally when provider artifacts do not include them
- treating transparent activity states as if they were backend-truth telemetry

---

## 6. Execution Sequence

### Step 1: Storage Isolation First

- implement SIM audio persistence namespace before real inventory wiring
- isolate metadata, artifact files, local audio blobs, and any persisted audio/chat binding records
- do not start real audio rendering work on shared generic filenames

This is the first real contamination firewall for the audio lane.

### Step 2: Inventory Baseline Before Sync Expansion

- define the minimum Wave 2 inventory baseline as local/manual plus SIM-safe seeded or persisted inventory
- backfill missing SIM-safe seeded entries without overwriting persisted audio state so acceptance can exercise both reuse and fresh-pending branches
- treat SmartBadge sync as an extension, not as the baseline gate
- include badge sync in Wave 2 only if the existing seam is already clean enough after evidence review

Wave 2 must not stall if badge sync requires reopening shell/connectivity architecture.

### Step 3: Existing Artifact Reuse Path

- detect when selected audio is already transcribed
- load stored artifacts instead of rerunning Tingwu
- make this branch explicit in code and validation

This is a required Wave 2 proof, not optional polish.

### Step 4: Informational Artifact Surface

- make the expanded transcribed card the primary informational view
- render only provider-returned sections
- keep absent sections absent
- make the surface feel like the read-only counterpart of the chat interface

### Step 5: Transcription Lifecycle

- support pending -> transcribing -> transcribed -> failed
- keep retry explicit
- do not fake completed artifacts on failure

### Step 6: Polisher With Fallback

- add readability polishing over Tingwu output
- keep the output source-led
- if polish fails, fall back to raw or lightly formatted provider output
- never block artifact display solely because polishing failed

### Step 7: Transparent Presentation States

- support transcript unfolding/collapse
- support presentational activity labels such as summarizing / chaptering / speaker extraction
- if provider-native trace is missing, keep these states clearly cosmetic rather than fake backend truth

---

## 7. Validation Checklist

Wave 2 verification must prove:

- SIM audio persistence does not collide with smart-app audio persistence
- SIM can load a real audio inventory baseline
- already-transcribed audio loads stored artifacts without rerunning Tingwu
- new transcription flows through pending / transcribing / transcribed / failed correctly
- artifact rendering stays source-led
- missing provider sections remain absent
- polisher failure falls back to provider-led output instead of blanking the view
- transparent states remain presentational and do not imply unavailable backend fidelity
- the audio drawer still reads as informational mode, not as a file manager or separate smart workflow

Cold-start acceptance is required.
Do not validate only on warm state.

---

## 8. Doc Sync Targets

If Wave 2 changes audio ownership, storage names, or artifact behavior, sync these docs in the same session:

- `docs/plans/tracker.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/audio-management/interface.md`
- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/cerb/tingwu-pipeline/interface.md`
- `docs/cerb/interface-map.md` if ownership edges become real

If the implementation hardens or changes the inventory source decision, update:

- `docs/plans/sim_implementation_brief.md`

---

## 9. Done-When Summary

Wave 2 is done when:

- SIM audio data is storage-safe
- users can browse a real SIM audio inventory
- already-transcribed audio reuses stored artifacts
- transcribed cards display readable Tingwu-backed artifacts
- absent sections stay absent
- polish is helpful but non-blocking
- the drawer clearly behaves as informational mode inside the standalone SIM product
