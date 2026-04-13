# SIM Standalone Prototype Code Audit

> **Date**: 2026-03-19
> **Purpose**: Audit the current codebase to identify the minimum safe reuse set for the standalone SIM prototype and explicitly mark contamination risks against the live smart-agent app.
> **Scope**: shell, chat, scheduler, audio drawer, audio repository, DI composition
> **Audit Method**: Core docs -> Cerb/docs -> code -> tests

---

## 1. Conclusion

The minimum-work path is **not** to fork the whole app.

The minimum safe path is:

1. keep the existing visual shell components and drawer UIs where they already expose interface seams
2. create a **prototype-only activity + shell + DI graph**
3. create **prototype-only viewmodels** for chat and scheduler
4. reuse the current audio drawer and most of the audio repository behavior, but **namespace prototype storage**

The current `AgentShell`, `AgentViewModel`, and global `PrismModule` are too entangled with the smart-agent runtime to reuse directly without contamination risk.

---

## 2. Findings

### F1. `AgentShell` is not a safe standalone reuse root

Severity: High

Evidence:

- [AgentShell.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt#L48) hard-wires `AgentViewModel` and `HistoryViewModel`.
- [AgentShell.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt#L89) always mounts `AgentIntelligenceScreen` through the current smart shell callbacks.
- [AgentShell.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt#L115) includes History Drawer wiring.
- [AgentShell.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt#L177) includes connectivity modal wiring.
- [AgentShell.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt#L209) includes Tingwu/artifact right-drawer stubs.
- [AgentShell.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt#L233) mounts debug HUD and mascot overlay.

Impact:

- Reusing this composable as the prototype root would drag in history, mascot, debug, connectivity, and current session behavior.
- That violates the user's "absolute no contamination" rule.

Recommendation:

- Create `SimMainActivity` plus `SimShell`.
- Reuse styling, layout structure, drawer choreography, and top-level visual language only.

### F2. `AgentViewModel` is deeply coupled to the smart-agent runtime

Severity: High

Evidence:

- [AgentViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L46) injects `IntentOrchestrator`, `ToolRegistry`, `MascotService`, `SystemEventBus`, `ContextBuilder`, `HistoryRepository`, and `AudioRepository`.
- [AgentViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L159) creates and resets full chat sessions through `HistoryRepository` plus `ContextBuilder`.
- [AgentViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L194) injects audio transcript and summary into `documentContext`.
- [AgentViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L314) executes tool recommendations through plugin infrastructure.
- [AgentViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L389) creates a runtime plugin gateway.

Impact:

- Direct reuse would re-import smart-agent routing, plugin execution, session-memory handling, and mascot behavior.
- This is the main contamination hotspot.

Recommendation:

- Do not reuse `AgentViewModel` for SIM.
- Reuse only the UI contract seam exposed by `IAgentViewModel`.

### F3. `AgentIntelligenceScreen` is a good UI seam

Severity: Low

Evidence:

- [AgentIntelligenceScreen.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt#L47) accepts `IAgentViewModel`.
- [IAgentViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/IAgentViewModel.kt#L16) already defines a presentation contract instead of a concrete runtime dependency.

Impact:

- The existing chat/shell skin can be reused without reusing the current smart-agent runtime.

Recommendation:

- Implement `SimAgentViewModel : IAgentViewModel`.
- Keep the visual shell and remove unsupported states at the viewmodel layer.

### F4. `SchedulerDrawer` is reusable, but `SchedulerViewModel` is not Path-A-only

Severity: Medium

Evidence:

- [SchedulerDrawer.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt#L55) accepts `ISchedulerViewModel`, which is a valid presentation seam.
- [ISchedulerViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/ISchedulerViewModel.kt#L12) defines the drawer contract cleanly enough to swap runtime implementations.
- [SchedulerViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt#L51) injects `MemoryRepository`, `InspirationRepository`, `SchedulerCoordinator`, `TipGenerator`, `AsrService`, `IntentOrchestrator`, and `ToolRegistry`.
- [SchedulerViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt#L121) merges active tasks with memory-backed crossed-off items and inspirations.
- [SchedulerViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt#L274) still routes voice audio through ASR plus `IntentOrchestrator`.

Impact:

- The drawer UI is reusable.
- The current viewmodel carries extra smart-app behavior beyond the requested SIM scheduler slice.

Recommendation:

- Reuse `SchedulerDrawer` and its child UI surfaces.
- Build `SimSchedulerViewModel : ISchedulerViewModel` with Path A-only behavior and a reduced data model.

### F5. Audio drawer and audio viewmodel are the best existing reuse candidates

Severity: Low

Evidence:

- [AudioDrawer.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt#L44) is a focused feature surface with simple callbacks.
- [AudioDrawer.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt#L185) already has a local audio picker path.
- [AudioViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt#L31) depends only on `AudioRepository` and `HistoryRepository`.
- [AudioViewModel.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt#L89) already implements `Ask AI` session creation and audio-session binding.

Impact:

- This is the shortest route to a working SIM audio feature.
- The main adjustment is replacing the current chat/session binding strategy with a prototype-safe one where needed.

Recommendation:

- Reuse `AudioDrawer`.
- Reuse `AudioViewModel` logic selectively or extract a shared helper from it after the SIM boundary exists.

### F6. `RealAudioRepository` is reusable only if storage is namespaced

Severity: Medium

Evidence:

- [RealAudioRepository.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt#L54) writes metadata to `audio_metadata.json` in app files.
- [RealAudioRepository.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt#L92) persists audio blobs as `<id>.wav`.
- [RealAudioRepository.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt#L207) persists Tingwu artifacts as `<id>_artifacts.json`.
- [RealAudioRepository.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt#L293) still deletes badge-side recordings for SmartBadge files.

Impact:

- If SIM shares the same app storage namespace, audio state will collide with the smart app.
- If SIM should stay prototype-safe, the repository needs either:
  - a distinct application ID/files dir, or
  - a prototype-prefixed storage namespace and policy split.

Recommendation:

- Reuse the repository behavior, not the exact storage names.
- Introduce `sim_audio_metadata.json` and a SIM-specific file prefix or isolated application ID.

### F7. Current DI composition is a contamination vector

Severity: High

Evidence:

- [PrismModule.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt#L44) is the global singleton graph for the smart app.
- [PrismModule.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt#L81) binds `AudioRepository`.
- [PrismModule.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt#L88) binds the real plugin registry and plugin set.
- [PrismModule.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt#L136) binds the badge audio pipeline.
- [PrismModule.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt#L141) binds mascot service.
- [PrismModule.kt](/home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt#L158) binds unified pipeline.

Impact:

- Reusing the same Hilt root as-is will silently pull the smart runtime into SIM.

Recommendation:

- Add a prototype-only composition root and module set.
- Do not point the prototype activity at the smart app’s full singleton graph without qualifiers or alternate bindings.

### F8. Verification coverage is uneven for SIM reuse

Severity: Medium

Evidence:

- [AgentViewModelTest.kt](/home/cslh-frank/main_app/app-core/src/test/java/com/smartsales/prism/ui/AgentViewModelTest.kt#L24) provides strong coverage for the current agent runtime, not the SIM runtime.
- [SchedulerViewModelAudioStatusTest.kt](/home/cslh-frank/main_app/app-core/src/test/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModelAudioStatusTest.kt#L65) proves some scheduler status behavior for the current scheduler VM.
- [RealAudioRepositoryBreakItTest.kt](/home/cslh-frank/main_app/app-core/src/test/java/com/smartsales/prism/data/audio/RealAudioRepositoryBreakItTest.kt#L26) covers concurrency and invalid deletion, but not the full SIM audio/chat contract.

Impact:

- The repo already has test anchors, but not a dedicated verification slice for SIM isolation.

Recommendation:

- Add prototype-specific tests before broad implementation:
  - shell isolation test
  - storage namespace isolation test
  - audio drawer -> chat -> drawer reselection test
  - Path A-only scheduler proof

---

## 3. Minimal-Work Build Strategy

### Reuse Directly

- `AgentIntelligenceScreen` UI skin through `IAgentViewModel`
- `SchedulerDrawer` UI through `ISchedulerViewModel`
- `AudioDrawer`
- most of `RealAudioRepository` behavior after storage namespacing

### Reuse With Wrapper or Extraction

- `AudioViewModel`
- scheduler child UI components and timeline rendering
- selected shell layout/styling from `AgentShell`

### Do Not Reuse Directly

- `AgentMainActivity`
- `AgentShell`
- `AgentViewModel`
- `SchedulerViewModel`
- `PrismModule` as the prototype composition root

---

## 4. Recommended First Implementation Slice

1. Create `SimMainActivity`
2. Create `SimShell`
3. Create `SimAgentViewModel : IAgentViewModel`
4. Create `SimSchedulerViewModel : ISchedulerViewModel`
5. Add prototype-specific DI modules and storage namespace
6. Mount existing `AudioDrawer`, `SchedulerDrawer`, and `AgentIntelligenceScreen` against the prototype-only viewmodels

This is the smallest path that preserves the current look while honoring the no-contamination rule.

---

## 5. Open Decisions

1. Separate application ID versus same app with a prototype entry activity
2. Whether SIM chat uses a minimal dedicated client or a narrowed adapter on top of existing executor infrastructure
3. Whether SIM audio inventory starts with local/manual-only imports or also includes SmartBadge sync in the first cut
