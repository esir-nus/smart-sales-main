# Badge Audio Pipeline

> **Cerb-compliant spec** — End-to-end orchestration from badge recording to scheduler.

---

## Overview

Badge Audio Pipeline orchestrates the complete flow when user records audio on the ESP32 badge:

```
Badge Record Button
       ↓
ESP32 tim#get → App returns timestamp
       ↓
User speaks → ESP32 records WAV
       ↓
ESP32 log#YYYYMMDD_HHMMSS → App notified
       ↓
Download WAV from badge (HTTP)
       ↓
Transcribe via FunASR
       ↓
Feed transcript to Scheduler pipeline
       ↓
Delete WAV from badge (cleanup)
```

**Key Principle**: This is the **only** entry point for badge audio. Scheduler does NOT know about badge connectivity.

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [Connectivity Bridge](../connectivity-bridge/interface.md) | Badge BLE + HTTP access |
| [ASR Service](../asr-service/interface.md) | Audio transcription |
| [Scheduler](../scheduler/interface.md) | Task creation from transcript |

---

## Domain Models

### PipelineEvent

```kotlin
sealed class PipelineEvent {
    /** Badge started recording, waiting for user to finish */
    object RecordingStarted : PipelineEvent()
    
    /** Recording complete, downloading from badge */
    data class Downloading(val filename: String) : PipelineEvent()
    
    /** Download complete, transcribing */
    data class Transcribing(val filename: String, val sizeBytes: Long) : PipelineEvent()
    
    /** Transcription complete, processing via scheduler */
    data class Processing(val transcript: String) : PipelineEvent()
    
    /** Pipeline complete, task created */
    data class Complete(
        val result: SchedulerResult,
        val filename: String,
        val transcript: String
    ) : PipelineEvent()
    
    /** Pipeline failed at some stage */
    data class Error(
        val stage: Stage,
        val message: String,
        val filename: String?
    ) : PipelineEvent()
    
    enum class Stage {
        DOWNLOAD, TRANSCRIBE, SCHEDULE, CLEANUP
    }
}

sealed class SchedulerResult {
    data class TaskCreated(val taskId: String, val title: String) : SchedulerResult()
    data class MultiTaskCreated(val taskIds: List<String>) : SchedulerResult()
    data class InspirationSaved(val id: String) : SchedulerResult()
    data class AwaitingClarification(val question: String) : SchedulerResult()
    data object Ignored : SchedulerResult()  // 非调度意图（聊天、无效输入）
}
```


---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Interface + State Machine | ✅ SHIPPED | `BadgeAudioPipeline` interface, state model |
| **2** | Fake Pipeline | ✅ SHIPPED | `FakeBadgeAudioPipeline` for UI testing |
| **3** | Real Implementation | ✅ SHIPPED | `RealBadgeAudioPipeline`, handles all UiState variants |
| **4** | Error Recovery | 🔲 | Retry logic, partial failure handling |

---

## Wave 1 Ship Criteria

**Goal**: Interface and state machine that compiles.

- **Exit Criteria**:
  - [ ] `BadgeAudioPipeline` interface in `domain/audio/`
  - [ ] `PipelineEvent` sealed class
  - [ ] State machine definition
  - [ ] Build passes

- **Test Cases**:
  - [ ] All PipelineEvent variants compile
  - [ ] State transitions documented

---

## Wave 2 Ship Criteria

**Goal**: Fake for UI development.

- **Exit Criteria**:
  - [ ] `FakeBadgeAudioPipeline` with configurable delays
  - [ ] Emits realistic event sequence
  - [ ] Can simulate error scenarios

- **Test Cases**:
  - [ ] Happy path: RecordingStarted → ... → Complete
  - [ ] Error path: Downloading → Error(DOWNLOAD)
  - [ ] UI can display all states

---

## Wave 3 Ship Criteria

**Goal**: Real implementation connecting all components.

- **Exit Criteria**:
  - [ ] `RealBadgeAudioPipeline` orchestrates flow
  - [ ] Listens to `recordingNotifications()` from ConnectivityBridge
  - [ ] Downloads, transcribes, schedules in sequence
  - [ ] Cleans up WAV on success

- **Test Cases**:
  - [ ] L2: Record on badge → task appears in timeline
  - [ ] L2: Vietnamese input → AwaitingClarification returned
  - [ ] L2: "明天开会" → Meeting task created
  - [ ] L2: "以后想学吉他" → InspirationSaved

---

## Wave 4 Ship Criteria

**Goal**: Production error handling.

- **Exit Criteria**:
  - [ ] Download retry: 2 attempts with backoff
  - [ ] Transcription retry: 1 attempt 
  - [ ] WAV preserved on error for manual recovery
  - [ ] User notification on persistent failure

- **Test Cases**:
  - [ ] Network blip → retry succeeds
  - [ ] Badge offline → error with "请检查徽章连接"
  - [ ] ASR failure → error with "转写失败，请重试"

---

## Pipeline State Machine

```
                    ┌─────────────┐
                    │    Idle     │
                    └──────┬──────┘
                           │ log# received
                    ┌──────▼──────┐
                    │ Downloading │
                    └──────┬──────┘
                           │ download complete
                    ┌──────▼──────┐
                    │ Transcribing│
                    └──────┬──────┘
                           │ transcript ready
                    ┌──────▼──────┐
                    │  Processing │────────────┐
                    └──────┬──────┘            │
                           │ task created      │ error at any stage
                    ┌──────▼──────┐     ┌──────▼──────┐
                    │  Complete   │     │    Error    │
                    └─────────────┘     └─────────────┘
```

---

## File Map

| Layer | Files |
|-------|-------|
| **Domain** | `BadgeAudioPipeline.kt`, `PipelineEvent.kt`, `PipelineConfig.kt` |
| **Data** | `RealBadgeAudioPipeline.kt`, `FakeBadgeAudioPipeline.kt` |
| **DI** | `BadgeAudioModule.kt` |
| **UI** | (uses PipelineEvent for progress display) |

---

## Implementation Strategy

### Real Pipeline

```kotlin
class RealBadgeAudioPipeline @Inject constructor(
    private val connectivity: ConnectivityBridge,
    private val asr: AsrService,
    private val schedulerOrchestrator: PrismOrchestrator  // existing
) : BadgeAudioPipeline {
    
    private val _events = MutableSharedFlow<PipelineEvent>()
    override val events: SharedFlow<PipelineEvent> = _events.asSharedFlow()
    
    init {
        // Auto-start listening for badge recordings
        connectivity.recordingNotifications()
            .onEach { notification -> processRecording(notification) }
            .launchIn(scope)
    }
    
    private suspend fun processRecording(notification: RecordingNotification) {
        val filename = notification.filename
        
        _events.emit(PipelineEvent.Downloading(filename))
        
        // Step 1: Download
        val downloadResult = connectivity.downloadRecording(filename)
        if (downloadResult is WavDownloadResult.Error) {
            _events.emit(PipelineEvent.Error(Stage.DOWNLOAD, downloadResult.message, filename))
            return
        }
        
        val file = (downloadResult as WavDownloadResult.Success).localFile
        _events.emit(PipelineEvent.Transcribing(filename, downloadResult.sizeBytes))
        
        // Step 2: Transcribe
        val transcription = asr.transcribe(file)
        if (transcription is AsrResult.Error) {
            _events.emit(PipelineEvent.Error(Stage.TRANSCRIBE, transcription.message, filename))
            return
        }
        
        val transcript = (transcription as AsrResult.Success).text
        _events.emit(PipelineEvent.Processing(transcript))
        
        // Step 3: Schedule (reuse existing pipeline)
        // Note: Using createScheduledTask for new tasks vs processSchedulerAction for updates
        val uiState = schedulerOrchestrator.createScheduledTask(transcript)
        
        // Step 4: Cleanup
        connectivity.deleteRecording(filename)
        file.delete()
        
        // Map UiState to domain-agnostic SchedulerResult
        val schedulerResult = uiState.toSchedulerResult()
        _events.emit(PipelineEvent.Complete(schedulerResult, filename))
    }
}
```

### Integration with Scheduler

The pipeline calls `PrismOrchestrator.processSchedulerAction()` which already handles:
- Input classification (schedulable/inspiration/non_intent)
- Multi-task splitting
- Conflict detection
- Task creation

**No duplication of scheduler logic.**

---

## UI Integration

```kotlin
// In SchedulerViewModel or PrismShell
@Inject lateinit var pipeline: BadgeAudioPipeline

init {
    pipeline.events
        .onEach { event ->
            when (event) {
                is PipelineEvent.Downloading -> showProgress("正在下载录音...")
                is PipelineEvent.Transcribing -> showProgress("正在转写...")
                is PipelineEvent.Processing -> showProgress("正在处理...")
                is PipelineEvent.Complete -> {
                    hideProgress()
                    showSuccess(event.result)
                }
                is PipelineEvent.Error -> {
                    hideProgress()
                    showError(event.message)
                }
            }
        }
        .launchIn(viewModelScope)
}
```

---

## Verification Commands

```bash
# Build check
./gradlew :app-prism:compileDebugKotlin

# Check pipeline wiring
grep -rn "BadgeAudioPipeline" app-prism/src/main/java/
```
