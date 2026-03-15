package com.smartsales.prism.data.audio

import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.PipelineState
import com.smartsales.prism.domain.audio.SchedulerResult
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.domain.model.UiState
import com.smartsales.core.pipeline.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.smartsales.prism.domain.scheduler.FastTrackParser
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository

/**
 * Real Badge Audio Pipeline — 录音 → 转写 → 调度
 * 
 * Wave 3: Real Implementation
 * @see docs/cerb/badge-audio-pipeline/spec.md
 */
@Singleton
class RealBadgeAudioPipeline @Inject constructor(
    private val connectivityBridge: ConnectivityBridge,
    private val asrService: AsrService,
    private val unifiedPipeline: UnifiedPipeline,
    private val fastTrackParser: FastTrackParser,
    private val scheduledTaskRepository: ScheduledTaskRepository
) : BadgeAudioPipeline {
    
    companion object {
        private const val TAG = "AudioPipeline"
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _events = MutableSharedFlow<PipelineEvent>(
        replay = 3,
        extraBufferCapacity = 1
    )
    override val events: SharedFlow<PipelineEvent> = _events.asSharedFlow()
    
    private val _currentState = MutableStateFlow(PipelineState.IDLE)
    override val currentState: StateFlow<PipelineState> = _currentState.asStateFlow()
    
    init {
        // 启动录音监听
        scope.launch {
            connectivityBridge.recordingNotifications().collect { notification ->
                when (notification) {
                    is RecordingNotification.RecordingReady -> {
                        android.util.Log.d(TAG, "New recording detected: ${notification.filename}")
                        processFile(notification.filename)
                    }
                }
            }
        }
    }
    
    override suspend fun processFile(filename: String) {
        var currentStage = PipelineEvent.Stage.DOWNLOAD // Track stage for catch-all
        try {
            // 1. Download
            currentStage = PipelineEvent.Stage.DOWNLOAD
            _currentState.value = PipelineState.DOWNLOADING
            _events.emit(PipelineEvent.Downloading(filename))
            android.util.Log.d(TAG, "Downloading: $filename")
            
            val downloadResult = connectivityBridge.downloadRecording(filename)
            if (downloadResult is WavDownloadResult.Error) {
                _currentState.value = PipelineState.IDLE
                _events.emit(PipelineEvent.Error(
                    stage = PipelineEvent.Stage.DOWNLOAD,
                    message = downloadResult.message,
                    filename = filename
                ))
                android.util.Log.w(TAG, "Download failed: ${downloadResult.message}")
                return
            }
            
            val localFile = (downloadResult as WavDownloadResult.Success).localFile
            android.util.Log.d(TAG, "Downloaded ${downloadResult.sizeBytes} bytes")
            
            // 2. Transcribe
            currentStage = PipelineEvent.Stage.TRANSCRIBE
            _currentState.value = PipelineState.TRANSCRIBING
            _events.emit(PipelineEvent.Transcribing(filename, downloadResult.sizeBytes))
            android.util.Log.d(TAG, "Transcribing...")
            
            val asrResult = asrService.transcribe(localFile)
            if (asrResult is AsrResult.Error) {
                _currentState.value = PipelineState.IDLE
                _events.emit(PipelineEvent.Error(
                    stage = PipelineEvent.Stage.TRANSCRIBE,
                    message = "转写失败: ${asrResult.message}",
                    filename = filename
                ))
                android.util.Log.w(TAG, "Transcription failed: ${asrResult.message}")
                localFile.delete()
                return
            }
            
            val transcript = (asrResult as AsrResult.Success).text
            android.util.Log.d(TAG, "Transcribed: $transcript")
            
            // 3. Schedule
            currentStage = PipelineEvent.Stage.SCHEDULE
            _currentState.value = PipelineState.PROCESSING
            _events.emit(PipelineEvent.Processing(transcript))
            android.util.Log.d(TAG, "Scheduling task...")
            
            val unifiedId = java.util.UUID.randomUUID().toString()
            android.util.Log.d(TAG, "processFile: Minted unifiedId=\$unifiedId for scheduling task")
            
            val pipelineInput = com.smartsales.core.pipeline.PipelineInput(
                rawText = transcript,
                isVoice = true,
                isBadge = true,
                intent = com.smartsales.core.pipeline.QueryQuality.CRM_TASK,
                unifiedId = unifiedId
            )
            
            // --- PATH A: The Town (Fast-Track Optimistic UI) ---
            val optimisticTask = fastTrackParser.parseToOptimisticTask(unifiedId, transcript)
            scheduledTaskRepository.upsertTask(optimisticTask)
            
            val schedulerResult = SchedulerResult.TaskCreated(
                taskId = optimisticTask.id,
                title = optimisticTask.title,
                dayOffset = 0,
                scheduledAtMillis = optimisticTask.startTime.toEpochMilli(),
                durationMinutes = optimisticTask.durationMinutes
            )

            android.util.Log.d(TAG, "Path A Scheduled Optimistic Task: \$schedulerResult")
            
            // 4. Cleanup (Emit Completion for Path A to close Drawer)
            currentStage = PipelineEvent.Stage.CLEANUP
            _currentState.value = PipelineState.IDLE
            _events.emit(PipelineEvent.Complete(schedulerResult, filename, transcript))
            
            val deleted = connectivityBridge.deleteRecording(filename)
            localFile.delete()
            android.util.Log.d(TAG, "Path A Cleanup: badge=\${if (deleted) \"✓\" else \"✗\"}, local=✓")
            
            // --- PATH B: The Highway (Heavyweight CRM Disambiguation) ---
            scope.launch(Dispatchers.IO) {
                try {
                    android.util.Log.d(TAG, "Path B starting UnifiedPipeline for \$unifiedId")
                    unifiedPipeline.processInput(pipelineInput).collect { pResult ->
                        when(pResult) {
                            is com.smartsales.core.pipeline.PipelineResult.ToolDispatch -> {
                                android.util.Log.d(TAG, "Path B Enriched Task via ToolDispatch, plugin handles Upsert.")
                                // TODO: Plugin registry handles the final upsert asynchronously.
                            }
                            is com.smartsales.core.pipeline.PipelineResult.DisambiguationIntercepted -> {
                                val existing = scheduledTaskRepository.getTask(unifiedId) ?: optimisticTask
                                val state = com.smartsales.prism.domain.scheduler.ClarificationState.MissingInformation("需要进一步确认")
                                scheduledTaskRepository.upsertTask(existing.copy(clarificationState = state))
                            }
                            is com.smartsales.core.pipeline.PipelineResult.ClarificationNeeded -> {
                                val existing = scheduledTaskRepository.getTask(unifiedId) ?: optimisticTask
                                val state = com.smartsales.prism.domain.scheduler.ClarificationState.MissingInformation(pResult.question)
                                scheduledTaskRepository.upsertTask(existing.copy(clarificationState = state))
                            }
                            else -> { /* Ignore intermediate states from RealUnifiedPipeline */ }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Path B Highway failed: \${e.message}", e)
                }
            }
            
        } catch (e: Exception) {
            _currentState.value = PipelineState.IDLE
            _events.emit(PipelineEvent.Error(
                stage = currentStage, // Use tracked stage, not hardcoded CLEANUP
                message = e.message ?: "未知错误",
                filename = filename
            ))
            android.util.Log.e(TAG, "Pipeline error at stage \$currentStage", e)
        }
    }
    
    override fun isIdle(): Boolean = currentState.value == PipelineState.IDLE
    
    // mapToSchedulerResult is deprecated and removed since we directly map PipelineResults now.
}
