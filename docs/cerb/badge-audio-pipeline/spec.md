---
era: post-harness
engine: backend
evidence_class: platform-runtime
lane: android
---

# Badge Audio Pipeline

> **Cerb-compliant spec** — End-to-end orchestration from badge recording to scheduler.
> **State**: SHIPPED

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
Persist completed recording into SIM audio drawer namespace
       ↓
Delete WAV from badge (cleanup after drawer ingest succeeds)
```

**Key Principle**: This is the only automatic scheduler-ingress path for badge audio. Scheduler does NOT know about badge connectivity, and drawer visibility is repaired through the pipeline rather than by teaching scheduler or shell UI about badge transport.

## Background Execution

The automatic `log#...` scheduler path now runs inside `SchedulerPipelineForegroundService`, not inside `RealBadgeAudioPipeline`'s own long-lived worker scope.

- `RealBadgeAudioPipeline` still owns the badge audio stage machine and still emits the same `PipelineEvent` / `PipelineState` contract.
- The init collector on `ConnectivityBridge.recordingNotifications()` now enqueues `log#...` filenames into `SchedulerPipelineOrchestrator`.
- `SchedulerPipelineOrchestrator` deduplicates queued + in-flight filenames, starts the foreground service when needed, and feeds one FIFO `Channel<String>` consumer.
- `SchedulerPipelineForegroundService` owns the screen-off execution envelope: foreground lifetime, one-at-a-time processing, stage-aware progress notification, outcome/failure notification dispatch, and 800 ms drain debounce before stop.
- Public `BadgeAudioPipeline.processFile(filename)` remains the manual / retry surface and still runs inline until complete. It does not route through the foreground service.

### Automatic Path Shape

```text
BLE log# notification
  -> RealBadgeAudioPipeline init collector
  -> SchedulerPipelineOrchestrator.enqueue(filename)
  -> SchedulerPipelineForegroundService
      -> download
      -> transcribe
      -> schedule
      -> drawer ingest
      -> outcome notification / fallback
```

### Stage Notification Contract

- Ongoing foreground channel: `prism_scheduler_pipeline_progress`
- Stage text:
  - `RECEIVING` -> `Receiving recording from badge...`
  - `TRANSCRIBING` -> `Transcribing your request...`
  - `SCHEDULING` -> `Creating your schedule...`
- Service stop rule: when queue + in-flight work both drain, wait 800 ms, then `stopForeground(REMOVE)` + `stopSelf()`

### Outcome Mapping

| Result | User Signal | Channel | Tap Action |
|------|-------------|---------|------------|
| `TaskCreated` | Heads-up: `Schedule created` | `prism_scheduler_pipeline_outcome` | Open app root |
| `MultiTaskCreated` | Heads-up summarizing count + titles | `prism_scheduler_pipeline_outcome` | Open app root |
| `InspirationSaved` | Heads-up: `Saved to inspirations` | `prism_scheduler_pipeline_outcome` | Open app root |
| `AwaitingClarification` | Heads-up: `Tap to answer` | `prism_scheduler_pipeline_outcome` | Open app root |
| `Ignored` | Low-priority non-heads-up status entry | `prism_scheduler_pipeline_progress` | None |
| `Error(DOWNLOAD)` | Heads-up retry guidance | `prism_scheduler_pipeline_outcome` | None |
| `Error(TRANSCRIBE)` | Heads-up retry guidance | `prism_scheduler_pipeline_outcome` | None |
| `Error(SCHEDULE)` | Heads-up rephrase guidance | `prism_scheduler_pipeline_outcome` | None |

### Permission-Denied Fallback

If `POST_NOTIFICATIONS` is denied on Android 13+:

- the pipeline still completes inside the foreground-service envelope
- the outcome notification is skipped intentionally
- the app sends one BLE badge chime via `DeviceConnectionManager.notifyTaskFired()`
- the missed outcome summary is stored in an in-memory ring buffer
- `MainActivity.onStart()` consumes that buffer and shows a toast once the user returns to foreground

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
    data class TaskCreated(
        val taskId: String,
        val title: String,
        val dayOffset: Int,
        val scheduledAtMillis: Long,
        val durationMinutes: Int
    ) : SchedulerResult()
    data class MultiTaskCreated(val tasks: List<TaskCreated>) : SchedulerResult()
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
  - [x] Persists successful badge recordings into SIM drawer storage before cleanup
  - [x] Deletes badge WAV only after drawer ingest succeeds

- **Test Cases**:
  - [ ] L2: Record on badge → task appears in timeline
  - [ ] L2: Vietnamese input → AwaitingClarification returned
  - [ ] L2: "明天开会" → Meeting task created
  - [ ] L2: "以后想学吉他" → InspirationSaved
  - [x] Successful pipeline completion produces a SIM drawer item without manual sync
  - [x] Drawer-ingest failure preserves the badge WAV for recovery/manual sync
  - [x] Empty badge recordings (< 1 KB) are silently skipped during manual sync with logged skip count

### Empty Recording Filter

Badge recordings smaller than 1 KB (`MIN_BADGE_WAV_SIZE_BYTES = 1024L`) are dropped at the download boundary in `SimAudioRepositorySyncSupport.performBadgeSyncLocked`. A valid WAV header alone is 44 bytes; files below 1 KB cannot contain meaningful audio content. Skipped files are cleaned from temp storage but remain on the badge. The skip count is logged and appended to the user-facing sync outcome message when nonzero.

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
    private val intentOrchestrator: IntentOrchestrator
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
        
        // Step 3: Delegate transcript into the shared Path A / Path B spine
        val completion = CompletableDeferred<SchedulerResult>()
        scope.launch {
            intentOrchestrator.processInput(transcript, isVoice = true).collect { result ->
                when (result) {
                    is PipelineResult.PathACommitted -> {
                        completion.complete(
                            SchedulerResult.TaskCreated(
                                taskId = result.task.id,
                                title = result.task.title,
                                dayOffset = 0,
                                scheduledAtMillis = result.task.startTime.toEpochMilli(),
                                durationMinutes = result.task.durationMinutes
                            )
                        )
                    }
                    else -> if (!completion.isCompleted) completion.complete(SchedulerResult.Ignored)
                }
            }
        }
        val schedulerResult = completion.await()

        // Step 4: Cleanup
        connectivity.deleteRecording(filename)
        file.delete()

        _events.emit(PipelineEvent.Complete(schedulerResult, filename))
    }
}
```

### Integration with Scheduler

The pipeline delegates the transcript to `IntentOrchestrator.processInput(transcript, isVoice = true)`.

Wave 19 T0 makes `IntentOrchestrator` the single live Path A writer. `RealBadgeAudioPipeline` no longer mints scheduler IDs or writes optimistic tasks directly.

The delivered split is:
- `RealBadgeAudioPipeline` owns recording, download, transcription, cleanup, and drawer-facing pipeline events.
- `IntentOrchestrator` owns voice intent classification, `unifiedId` allocation, optimistic Path A write, and downstream Path B continuation.
- `PipelineResult.PathACommitted` is the early completion checkpoint that lets the drawer close as soon as the optimistic scheduler write lands.

**No duplication of scheduler logic.**

---

## UI Integration

```kotlin
// In SchedulerViewModel or AgentShell
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
./gradlew :app-core:compileDebugKotlin

# Check pipeline wiring
grep -rn "BadgeAudioPipeline" app-core/src/main/java/
```
