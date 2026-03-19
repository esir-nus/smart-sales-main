# SIM Implementation Brief

**Status:** Boundary Frozen / Ready For Wave 1
**Date:** 2026-03-19
**Mission:** SIM standalone prototype
**Primary Product Doc:** `docs/to-cerb/sim-standalone-prototype/concept.md`
**Mental Model Doc:** `docs/to-cerb/sim-standalone-prototype/mental-model.md`
**Mission Tracker:** `docs/plans/sim-tracker.md`
**Evidence Base:**
- `docs/reports/20260319-sim-standalone-code-audit.md`
- `docs/reports/20260319-sim-clarification-evidence-audit.md`

---

## 1. Purpose

This brief freezes the Wave 0 boundary decisions so SIM can move into implementation without reopening product-shape arguments on every code change.

This is not a UI brief.
This is the runtime, ownership, naming, and isolation brief for the standalone prototype.

---

## 2. Boundary Constitution

SIM V1 will ship as a standalone product path inside the same repository and the same Android application binary, but through a separate `SimMainActivity` entry path, a standalone `SimShell` composition root, prototype-owned viewmodels/runtime seams, and namespaced SIM persistence. The smart app remains the protected product. Shared UI skins are allowed. Shared smart runtime ownership is not.

---

## 3. Frozen Decisions

### 3.1 App Entry Strategy

Decision:

- V1 uses the same Android application process
- V1 adds a separate `SimMainActivity`
- V1 must not boot through `AgentMainActivity`
- V1 must not route through `AgentShell`

Reason:

- lowest implementation cost
- avoids premature flavor/applicationId split
- still preserves a real standalone runtime boundary at the entry/composition level

Consequence:

- "standalone" for V1 means separate product path, separate root ownership, and separate state boundaries
- full binary extraction remains a future option, not a V1 requirement

### 3.2 Naming Strategy

Decision:

- SIM-owned roots and orchestrators use explicit `Sim*` naming
- shared reusable UI surfaces keep current names where ownership remains generic
- stale legacy names may be replaced only when they hide contamination risk or ownership truth

Examples:

- `SimMainActivity`
- `SimShell`
- `SimAgentViewModel`
- `SimSchedulerViewModel`
- `SimShellDependencies`

Do not rename:

- `AudioDrawer`
- `SchedulerDrawer`
- `HistoryDrawer`
- `ConnectivityModal`

unless a future evidence-based conflict appears.

### 3.3 DI Strategy

Decision:

- SIM gets a prototype-only composition root
- `PrismModule` is not the SIM root
- smart-runtime-singleton behavior must not define SIM runtime ownership
- SIM may consume low-risk leaf services through explicit seams or adapters where reuse is safe

Required rule:

- if a dependency drags smart-agent orchestration, plugin board behavior, mascot behavior, or smart shell/session assumptions into SIM, it must be wrapped, replaced, or excluded

Preferred shape:

- `SimMainActivity`
- `SimShell`
- `SimShellDependencies`
- `SimAgentViewModel : IAgentViewModel`
- `SimSchedulerViewModel : ISchedulerViewModel`
- prototype-only SIM module set or explicit dependency assembler

### 3.4 Storage Strategy

Decision:

- SIM storage is namespaced even if it shares the same application files directory
- unnamespaced audio/session artifacts are not acceptable for SIM

Minimum namespace:

- `sim_audio_metadata.json`
- `sim_<audioId>.wav`
- `sim_<audioId>_artifacts.json`
- any SIM audio/chat binding record

Reason:

- current audio storage names collide with smart-app-safe isolation assumptions
- current repo evidence already shows generic filenames in `RealAudioRepository`

### 3.5 Session and History Strategy

Decision:

- SIM chat/session history must be SIM-scoped
- raw smart history behavior must not leak into SIM by default
- if current history persistence is reused, it must be filtered/namespaced behind a SIM-owned adapter

Allowed:

- SIM-local grouping such as today / week / month
- rename / delete / pin if implemented in SIM scope

Forbidden:

- showing smart-agent sessions in SIM by accident
- assuming the current shared session/history model is safe without a SIM filter boundary

### 3.6 Audio Strategy

Decision:

- existing persisted artifacts are reused when audio is already transcribed
- `Ask AI` continues from selected audio context
- SIM does not rerun Tingwu for already-transcribed selection
- the readability-polisher layer is new work and must not be assumed to exist already
- transparent-thinking presentation is new work beyond the current progress/shimmer baseline

### 3.7 Connectivity Strategy

Decision:

- connectivity takes a hard-migration approach
- existing connectivity contracts remain authoritative
- connectivity stays a support module, not a third product lane

Allowed reuse:

- `ConnectivityBridge`
- `ConnectivityService`
- `ConnectivityModal`
- existing setup/reconnect behavior where it can be mounted without smart-runtime coupling

---

## 4. Allowed Reuse Set

Safe or conditionally safe reuse targets for V1:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/components/ConnectivityModal.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/settings/UserCenterScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/IAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/ISchedulerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/domain/connectivity/*`
- `docs/core-flow/scheduler-fast-track-flow.md` and Path A Cerb shards

Conditional reuse only after SIM-safe adaptation:

- `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt`

---

## 5. Forbidden Direct Reuse

Do not reuse directly as SIM runtime roots:

- `app-core/src/main/java/com/smartsales/prism/AgentMainActivity.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt`

Do not carry into normal SIM behavior:

- mascot overlay
- debug HUD
- plugin task board
- right-side Tingwu/artifact stubs
- Oasis-style memory assumptions

---

## 6. Required New Artifacts

Minimum expected new implementation artifacts:

- `SimMainActivity`
- `SimShell`
- `SimShellDependencies`
- `SimAgentViewModel`
- `SimSchedulerViewModel`
- SIM-specific shell state holder
- SIM-specific session/history adapter or filter boundary
- SIM-specific storage namespacing for audio artifacts
- SIM readability-polisher layer
- SIM transparent-state presentation contract

---

## 7. Implementation Order

1. shell root and entry
2. SIM shell support surfaces
3. audio informational mode with namespaced storage
4. discussion chat continuation
5. scheduler Path A runtime seam
6. connectivity hard migration
7. acceptance and isolation proof

This order follows the current mission tracker waves, but Wave 1 implementation should not begin by touching scheduler or Tingwu polish code first.

---

## 8. Wave 1 Start Gate

Wave 1 may begin if and only if the implementer accepts these as frozen:

- same app binary, separate SIM activity
- standalone SIM root ownership
- SIM-prefixed root naming
- namespaced SIM persistence
- protected smart app priority

If any of these must change, update this brief before implementation.

---

## 9. Acceptance Notes

Wave 1 through Wave 7 should be judged against two parallel truths:

1. feature proof
2. contamination proof

A feature-complete SIM that reuses smart runtime ownership incorrectly is not accepted.
