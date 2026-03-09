# Audio Drawer UI Implementation 

**Context**: We just successfully completed the architectural decoupling of the Aliyun Tingwu Service (`TingwuPipeline`) and wired it into the `RealAudioRepository`. The Prism layer now directly emits clean `TingwuJobArtifacts` to JSON and local SSD upon meeting transcription completion. 

**Goal**: Resume work on the `AudioDrawer.kt`. We need to wire the newly decoupled pipeline states into the visual drawer for the user.

**Current State**:
- `RealAudioRepository` can be triggered via `startTranscription(audioId)`. 
- `AudioFile` domain models correctly hold the `progress` and `status` properties (`PENDING`, `TRANSCRIBING`, `TRANSCRIBED`), which are exposed as a StateFlow to `AudioViewModel`.
- A minimal fake `AudioDrawer` UI skeleton exists.

**Next Steps**:
1. Implement the `/SOP-ui-building` process specifically for `AudioDrawer`.
2. Connect the `AudioViewModel` actions to the `TingwuJobState` Flow originating from the repository.
3. Polish the multi-state UI representation of an `AudioFile` (e.g., Circular progress bar during `TRANSCRIBING`, rendering the "智能摘要" field returned by Tingwu when `TRANSCRIBED`).
4. Apply the `compose-scrim-drawer-pattern.md` rigorously to ensure the Drawer overlaps the `AgentShell` natively without dual-scrim conflicts.

Please analyze `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt` and `docs/cerb/audio-management/spec.md` to begin Phase 1: Planning for the Audio Drawer UI.
