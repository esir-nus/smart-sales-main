# Audio Management

> **Cerb-compliant spec** — Audio file management: sync, transcription, UI interaction.
> **State**: SPEC_ONLY

---

## Overview

Audio Management provides interactive file management for badge recordings and phone audio. Users manually trigger sync, transcription, and organization through the Audio Drawer UI.

**Key Distinction**: This is **UI-driven, interactive** management. For automatic pipeline (badge records → auto-download → auto-transcribe → scheduler), see [Badge Audio Pipeline](../badge-audio-pipeline/interface.md).

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [Connectivity Bridge](../connectivity-bridge/interface.md) | Downloads WAV files from badge |
| [ASR Service](../asr-service/interface.md) | Audio transcription via FunASR |
| [Badge Audio Pipeline](../badge-audio-pipeline/interface.md) | Auto-pipeline (independent use case) |

---

## Architecture Relationship

```mermaid
graph TB
    subgraph "Audio Management (Interactive)"
        UI[AudioDrawer UI]
        Repo[AudioRepository]
        UI -->|User actions| Repo
    end
    
    subgraph "Badge Audio Pipeline (Reactive)"
        Pipeline[BadgeAudioPipeline]
        Events[recordingNotifications]
        Events -->|Auto-trigger| Pipeline
    end
    
    Repo -->|Manual sync| ConnBridge[ConnectivityBridge]
    Repo -->|Manual transcribe| ASR[AsrService]
    Pipeline -->|Auto flow| ConnBridge
    Pipeline -->|Auto flow| ASR
    
    Pipeline -.->|Future: Auto-ingest| Repo
    
    style Repo fill:#e1f5ff
    style Pipeline fill:#fff4e1
```

**Two Layers**:
- **AudioRepository** — UI-driven (user taps "Sync", "Transcribe", "Delete")
- **BadgeAudioPipeline** — Event-driven (badge sends `log#YYYYMMDD_HHMMSS` → auto-process)

**Future Integration** (Wave 3): Pipeline emits `PipelineEvent.Completed` → Repository auto-ingests the file.

---

## Domain Models

See [interface.md](./interface.md) for:
- `AudioFile` — Core audio metadata + status
- `AudioSource` — SMARTBADGE vs PHONE
- `TranscriptionStatus` — PENDING, TRANSCRIBING, TRANSCRIBED, ERROR

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Interface + Fake | ✅ SHIPPED | `AudioRepository` interface, `FakeAudioRepository`, domain models |
| **2** | Real Repository | 🔲 | `RealAudioRepository` with persistence |
| **3** | Pipeline Integration | 🔲 | Auto-ingest from `BadgeAudioPipeline` events |
| **4** | Ask AI Flow | 🔲 | Session binding, coach context injection |

---

## Wave 1 Ship Criteria

**Goal**: Interface + Fake for UI development.

**Implementation**:
- [x] `AudioRepository` interface (8 methods)
- [x] `FakeAudioRepository` with sample data
- [x] Domain models: `AudioFile`, `AudioSource`, `TranscriptionStatus`
- [x] DI binding for fake

**Testing**:
- [x] `AudioDrawer` UI renders sample data
- [x] Fake methods callable from UI
- [x] No compile errors in domain layer

---

## Wave 2 Ship Criteria

**Goal**: Real storage-backed repository.

> [!IMPORTANT]
> **Storage Decision**: Room is NOT currently in `app-prism/build.gradle.kts`. Wave 2 implementation must either add Room dependency or use simpler file-based/SharedPreferences storage.

**Implementation**:
- [ ] `RealAudioRepository` with chosen storage
- [ ] Calls `ConnectivityBridge.downloadRecording()` for SMARTBADGE files
- [ ] Calls `AsrService.transcribe()` for manual transcription
- [ ] Progress tracking via StateFlow
- [ ] DI binding for real implementation

**Testing**:
- [ ] Add file → syncs from badge
- [ ] Start transcription → progress updates → summary appears
- [ ] Delete file → local + badge cleanup
- [ ] Toggle star → persists

---

## Wave 3 Ship Criteria

**Goal**: Auto-ingest from Badge Audio Pipeline.

**Dependencies**: Badge Audio Pipeline Wave 3 must ship first.

**Implementation**:
- [ ] Listen to `BadgeAudioPipeline.events`
- [ ] On `PipelineEvent.Completed` → auto-add to repository
- [ ] Mark as transcribed, populate summary
- [ ] Avoid duplicates (check by filename)

**Testing**:
- [ ] Badge records → pipeline processes → file appears in Audio Drawer
- [ ] No user action required

---

## Wave 4 Ship Criteria

**Goal**: Ask AI session binding.

**Implementation**:
- [ ] `bindSession(audioId, sessionId)` persists association
- [ ] Coach mode can inject transcription as context
- [ ] UI shows "Used in session X" indicator

**Testing**:
- [ ] Bind audio → Ask AI in Coach mode → summary referenced
- [ ] Unbind → no longer referenced

---

## File Map

| Layer | File | Purpose |
|-------|------|---------|
| **Domain** | `AudioRepository.kt` | Interface contract |
| **Domain** | `AudioFile.kt`, `AudioSource.kt`, `TranscriptionStatus.kt` | Domain models |
| **Data/Fakes** | `FakeAudioRepository.kt` | Wave 1 (shipped) |
| **Data/Real** | `RealAudioRepository.kt` | Wave 2 (planned) |
| **UI** | `AudioDrawer.kt`, `AudioViewModel.kt` | UI layer |
| **DI** | `AudioModule.kt` | Dependency injection |

---

## UX Reference

> **Spec**: See [AudioDrawer.md](../../specs/modules/AudioDrawer.md) for layouts, interactions, gestures, card states.
