# 📋 Review Conference Report: Audio Drawer Real Logic Wiring

**Subject**: Wiring in real business logic for the Audio Drawer feature (Wave 2 of Audio Management)
**Panel**:
1. `/01-senior-reviewr` (Chair) — Architecture sanity, patterns
2. `/17-lattice-review` — Layer compliance, contract integrity
3. `/08-ux-specialist` — Interaction flows, user experience

---

## 📅 Conference Agenda

**Subject**: Moving `AudioRepository` from Fake Phase (Wave 1) to Real Phase (Wave 2).
**Time Budget**: Standard

**Questions for Each Panelist**:

- **`/17-lattice-review`**: The `RealAudioRepository` will need to persist data, call `ConnectivityBridge` for downloading WAV files, and call `AsrService` for transcription. Does this violate any domain/platform layer boundaries according to the Lattice architecture? Specifically, should the persistence be Room OR simple file/SharedPreferences given the Wave 2 spec notes?

- **`/08-ux-specialist`**: The current UI handles "Pending", "Transcribing", and "Transcribed" states via `TranscriptionStatus` from the domain. Currently, the `AudioCard` "Swipe to Transcribe" triggers the action and snaps back immediately. There's no explicit error shape if the ASR API fails. How should the UI respond to a transcription error (e.g., should the card turn red, show a toast, or introduce an `ERROR` state in `AudioStatus` that requires a retry swipe)?

- **`/01-senior-reviewr`**: The spec outlines linking the UI-driven `AudioRepository` with the Event-driven `BadgeAudioPipeline` in the future (Wave 3). Can we build `RealAudioRepository` now in a way that makes ingesting `PipelineEvent.Completed` trivial later without coupling the two layers tightly?

---

### Panel Input Summary

#### `/17-lattice-review` — Layer Compliance
- **Insight 1**: `RealAudioRepository` belongs in the `platform` or `data` module, implementing the `AudioRepository` interface defined in the `domain` module.
- **Insight 2**: The `domain` module MUST NOT depend on Room, Retrofit, or any Android-specific libraries. The `data` implementation can depend on these, fulfilling the pure Kotlin contract. 
- **Insight 3**: For persistence in Wave 2, since Room isn't currently in `app-prism/build.gradle.kts` (as noted in the spec), starting with a robust JSON file-backed `StateFlow` or DataStore is acceptable to prevent scope creep, provided the Domain interface remains agnostic to the storage mechanism.

#### `/08-ux-specialist` — User Experience
- **Insight 1**: We need an `AudioStatus.ERROR` or similar representation in the UI, otherwise silent failures during ASR will leave the user confused as to why the recording is stuck in "Pending" or disappears.
- **Insight 2**: The `AudioCard` currently has a Swipe-to-Action. If transcription fails, we should drop a localized toast ("转写失败，请重试") and leave the status as `PENDING`, allowing the user to swipe again. We don't necessarily need a permanent red error card state if a toast provides immediate feedback.
- **Insight 3**: Let's ensure the `AudioViewModel` intercepts errors from `AudioRepository.startTranscription` and drives the toast/Snackbar, rather than crashing the flow.

---

### 🔴 Hard No (Consensus)
- Do not add `androidx.room.*` dependencies or Room DAOs to the `domain/` module.
- Do not let the `FakeAudioRepository` leak into the Dagger bindings for the production variant once `Real` is ready. 
- Do not couple `RealAudioRepository` directly to `BadgeAudioPipeline` class; they should remain decoupled, perhaps eventually combining their output streams in a use case or having the pipeline emit events that the repository consumes.

### 🟡 Yellow Flags
- **Error Handling**: The domain model `TranscriptionStatus` has an `ERROR` enum, but `AudioStatus` in `AudioDrawerStates.kt` does not. We need to decide if errors are transient (Toast) or persistent (Card State).
- **Storage Strategy**: If we use file-based JSON storage, we must ensure atomicity when writing to avoid data corruption if the app is killed during a transcription update.

### 🟢 Good Calls
- The `FakeAudioRepository` successfully defined the UI contract. The `AudioDrawer` is purely state-driven via `AudioViewModel` `StateFlow`, making the swap to `RealAudioRepository` seamless for the UI layer.

### 💡 Senior's Synthesis
The architecture is sound. The separation between the interactive UI repository (`AudioRepository`) and the background pipeline (`BadgeAudioPipeline`) is a classic and correct pattern. 

**Recommendation:**
1. **Model Update**: Keep UI simple. Map Domain `ERROR` to UI `PENDING` but trigger a one-shot error event (like a Toast) from the ViewModel to inform the user it failed.
2. **Storage**: Implement a simple JSON-backed file storage for `RealAudioRepository` to satisfy Wave 2 without touching gradle files for Room. 
3. **Architecture**: Implement `RealAudioRepository` in the `data/audio` package. Inject `ConnectivityBridge` and `AsrService`. Ensure all mapping from external DTOs to internal Domain models happens strictly within the data layer.

---

### 🔧 Prescribed Tools / Next Steps

Based on this review, here is the execution plan:

1. Create `RealAudioRepository.kt` in `app-prism/src/main/java/com/smartsales/prism/data/audio/`.
2. Implement file-backed JSON storage for metadata to satisfy Wave 2 without Room dependency.
3. Wire in `AsrService.transcribe()` for real ASR.
4. Update `AudioModule.kt` Dagger bindings to swap `Fake` for `Real`.
5. Update `AudioViewModel` to handle errors (Toast/Snackbar) when transcription fails.

Ready to proceed to implementation planning.
