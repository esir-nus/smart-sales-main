# Badge Audio Pipeline Interface

> **Blackbox contract** — For consumers (PrismShell, UI). Don't read implementation.

---

## You Can Call

### BadgeAudioPipeline

```kotlin
interface BadgeAudioPipeline {
    /**
     * Stream of pipeline events.
     * Emits progress updates as recording is processed.
     * Hot flow, starts listening on injection.
     */
    val events: SharedFlow<PipelineEvent>
    
    /**
     * Current pipeline state.
     * Reflects most recent event.
     */
    val currentState: StateFlow<PipelineState>
    
    /**
     * Manually trigger processing for a specific file.
     * Used for retry or manual import.
     */
    suspend fun processFile(filename: String)
    
    /**
     * Check if pipeline is idle (not processing).
     */
    fun isIdle(): Boolean
}

enum class PipelineState {
    IDLE, DOWNLOADING, TRANSCRIBING, PROCESSING
}
```

---

## Output Types

### PipelineEvent

```kotlin
sealed class PipelineEvent {
    object RecordingStarted : PipelineEvent()
    data class Downloading(val filename: String) : PipelineEvent()
    data class Transcribing(val filename: String, val sizeBytes: Long) : PipelineEvent()
    data class Processing(val transcript: String) : PipelineEvent()
    data class Complete(val result: SchedulerResult, val filename: String) : PipelineEvent()
    data class Error(val stage: Stage, val message: String, val filename: String?) : PipelineEvent()
    
    enum class Stage { DOWNLOAD, TRANSCRIBE, SCHEDULE, CLEANUP }
}

sealed class SchedulerResult {
    data class TaskCreated(val taskId: String, val title: String) : SchedulerResult()
    data class MultiTaskCreated(val taskIds: List<String>) : SchedulerResult()
    data class InspirationSaved(val id: String) : SchedulerResult()
    data class AwaitingClarification(val question: String) : SchedulerResult()
}
```

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `events` | Hot flow, buffered (3), starts on injection |
| `currentState` | Always reflects latest state |
| `processFile` | Returns after processing complete (success or error) |
| WAV cleanup | Deleted from badge and local after success |

---

## Lifecycle

Pipeline auto-starts on injection:
1. Listens to `ConnectivityBridge.recordingNotifications()`
2. Processes recordings automatically
3. Emits events for UI to display

**No manual start required.**

---

## You Should NOT

- ❌ Access ConnectivityBridge directly for recordings
- ❌ Call AsrService directly for transcription
- ❌ Implement your own download/transcribe/schedule flow
- ❌ Manage WAV files manually

---

## When to Read Full Spec

Read `spec.md` only if:
- You are implementing `RealBadgeAudioPipeline`
- You need to understand the state machine
- You are modifying error handling strategy

Otherwise, **trust this interface**.
