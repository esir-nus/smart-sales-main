# Audio Drawer Data Flow & Architecture Explainer

## 🎧 You Open the Audio Drawer (or Tap a Recording)

### Step 1: UI Observes Audio Repository State
**What**: The `AudioDrawer` continuously listens to a flow of audio files. When the drawer opens, it simply displays what the repository has.
**Where**: [AudioViewModel.kt L30-36](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt#L30-L36)
**Input**: `audioRepository.getAudioFiles()`
**Result**: A list of `AudioItemState` objects representing each file (Transcribed, Transcribing, or Pending).

### Step 2: You Trigger an Action (e.g., Swipe to Transcribe)
**What**: You swipe right on a `PENDING` recording. The UI calls `onTranscribe`, which delegates to the ViewModel, which launches a background task in the Repository.
**Where**: [AudioCard.kt L77](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/ui/drawers/AudioCard.kt#L77) -> [AudioViewModel.kt L44-48](file:///home/cslh-frank/main_app/app-prism/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt#L44-L48)
**Input**: `audioId`
**Result**: The repository begins the transcribe process and updates the file's status to `TRANSCRIBING`.

### Step 3: Real Repository Wiring (Future State)
**What**: Once real logic is wired in, the `RealAudioRepository` will handle actual API/Service calls (like `AsrService.transcribe()` for transcription or `ConnectivityBridge.downloadRecording()` for syncing) instead of using artificial delays.
**Where**: The upcoming `RealAudioRepository.kt` implementation.
**Input**: Audio Metadata / WAV files
**Result**: Actual database updates and real summaries parsed from ASR services.

---

## 🔀 System Diagram: Audio Management

```text
┌─────────────────┐       User Events      ┌─────────────────┐
│                 │   (Swipe, Tap, etc)    │                 │
│                 ├───────────────────────►│                 │
│ AudioDrawer UI  │                        │ AudioViewModel  │
│ (AudioCard.kt)  │◄───────────────────────┤                 │
│                 │      StateFlow         │                 │
└─────────────────┘   (AudioItemState)     └────────┬────────┘
                                                    │
                                                    │
                                                    ▼
┌───────────────────────────┐           ┌───────────────────────┐
│                           │           │                       │
│ BadgeAudioPipeline        │           │  AudioRepository      │
│ (Auto Ingest - Wave 3)    │           │  (Interactive Hub)    │
│                           │           │                       │
└─────────────┬─────────────┘           └─────┬───────────┬─────┘
              │                               │           │
              │                               │           │
              ▼                               ▼           ▼
   ┌───────────────────┐               ┌─────────┐   ┌─────────┐
   │ConnectivityBridge │               │  ASR    │   │ History │
   │ (Download WAVs)   │               │ Service │   │ Repo    │
   └───────────────────┘               └─────────┘   └─────────┘
```

## 🔍 Debug Checkpoints (For Upcoming Real Implementation)

| Step | Tag to Look For | Expected Log |
|------|-----------------|--------------|
| 1 | AudioViewModel | "User action: startTranscription(id=...)" |
| 2 | RealAudioRepo | "Starting ASR for file: ..." |
| 3 | AsrService | "ASR Complete: Summary generated." |

Run: `adb logcat -s AudioViewModel:D,RealAudioRepo:D,AsrService:D`
