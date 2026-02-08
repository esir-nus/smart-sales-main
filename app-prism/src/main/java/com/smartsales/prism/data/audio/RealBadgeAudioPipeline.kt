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
import com.smartsales.prism.domain.pipeline.Orchestrator
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
    private val orchestrator: Orchestrator
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
        try {
            // 1. Download
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
            _currentState.value = PipelineState.PROCESSING
            _events.emit(PipelineEvent.Processing(transcript))
            android.util.Log.d(TAG, "Scheduling task...")
            
            val uiState = orchestrator.createScheduledTask(transcript)
            val schedulerResult = try {
                mapToSchedulerResult(uiState)
            } catch (e: IllegalStateException) {
                _currentState.value = PipelineState.IDLE
                _events.emit(PipelineEvent.Error(
                    stage = PipelineEvent.Stage.SCHEDULE,
                    message = e.message ?: "调度失败",
                    filename = filename
                ))
                android.util.Log.w(TAG, "Scheduling failed: ${e.message}")
                localFile.delete()
                return
            }
            
            android.util.Log.d(TAG, "Scheduled: $schedulerResult")
            
            // 4. Cleanup
            _currentState.value = PipelineState.IDLE
            _events.emit(PipelineEvent.Complete(schedulerResult, filename, transcript))
            
            val deleted = connectivityBridge.deleteRecording(filename)
            localFile.delete()
            android.util.Log.d(TAG, "Cleanup: badge=${if (deleted) "✓" else "✗"}, local=✓")
            
        } catch (e: Exception) {
            _currentState.value = PipelineState.IDLE
            _events.emit(PipelineEvent.Error(
                stage = PipelineEvent.Stage.CLEANUP,
                message = e.message ?: "未知错误",
                filename = filename
            ))
            android.util.Log.e(TAG, "Pipeline error", e)
        }
    }
    
    override fun isIdle(): Boolean = currentState.value == PipelineState.IDLE
    
    /**
     * Map UiState from Orchestrator to SchedulerResult for pipeline events.
     * Handles: SchedulerTaskCreated, AwaitingClarification, Toast (inspiration).
     * Throws for unexpected states.
     */
    private fun mapToSchedulerResult(uiState: UiState): SchedulerResult = when (uiState) {
        is UiState.SchedulerTaskCreated -> SchedulerResult.TaskCreated(uiState.taskId, uiState.title)
        is UiState.AwaitingClarification -> SchedulerResult.AwaitingClarification(uiState.question)
        is UiState.Toast -> SchedulerResult.InspirationSaved("") // Toast = inspiration saved
        else -> throw IllegalStateException("Unexpected UiState from scheduler: $uiState")
    }
}
