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
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.PipelineResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
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
    private val intentOrchestrator: IntentOrchestrator,
    private val simBadgeAudioPipelineIngestSupport: SimBadgeAudioPipelineIngestSupport
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

            val pathACompletion = CompletableDeferred<SchedulerResult>()

            scope.launch(Dispatchers.IO) {
                try {
                    intentOrchestrator.processInput(transcript, isVoice = true).collect { result ->
                        when (result) {
                            is PipelineResult.PathACommitted -> {
                                if (!pathACompletion.isCompleted) {
                                    pathACompletion.complete(
                                        SchedulerResult.TaskCreated(
                                            taskId = result.task.id,
                                            title = result.task.title,
                                            dayOffset = 0,
                                            scheduledAtMillis = result.task.startTime.toEpochMilli(),
                                            durationMinutes = result.task.durationMinutes
                                        )
                                    )
                                }
                            }
                            is PipelineResult.InspirationCommitted -> {
                                if (!pathACompletion.isCompleted) {
                                    pathACompletion.complete(SchedulerResult.InspirationSaved(result.id))
                                }
                            }
                            is PipelineResult.MascotIntercepted,
                            is PipelineResult.BadgeDelegationIntercepted,
                            is PipelineResult.ConversationalReply,
                            is PipelineResult.ToolRecommendation,
                            is PipelineResult.MutationProposal,
                            is PipelineResult.ToolDispatch,
                            is PipelineResult.PluginExecutionStarted,
                            is PipelineResult.PluginExecutionEmittedState,
                            is PipelineResult.AutoRenameTriggered -> {
                                if (!pathACompletion.isCompleted) {
                                    pathACompletion.complete(SchedulerResult.Ignored)
                                }
                            }
                            else -> Unit
                        }
                    }
                    if (!pathACompletion.isCompleted) {
                        pathACompletion.complete(SchedulerResult.Ignored)
                    }
                } catch (e: Exception) {
                    if (!pathACompletion.isCompleted) {
                        pathACompletion.completeExceptionally(e)
                    } else {
                        android.util.Log.e(TAG, "Path A/Path B collection failed after completion: ${e.message}", e)
                    }
                }
            }

            val schedulerResult = pathACompletion.await()
            android.util.Log.d(TAG, "Path A committed via IntentOrchestrator: $schedulerResult")
            
            // 4. Cleanup (Emit Completion for Path A to close Drawer)
            currentStage = PipelineEvent.Stage.CLEANUP
            val drawerIngested = simBadgeAudioPipelineIngestSupport.ingestCompletedRecording(
                filename = filename,
                localFile = localFile,
                transcript = transcript
            )
            if (!drawerIngested) {
                android.util.Log.w(
                    TAG,
                    "SIM drawer ingest failed; preserving badge file for potential manual recovery: $filename"
                )
            }

            _currentState.value = PipelineState.IDLE
            _events.emit(PipelineEvent.Complete(schedulerResult, filename, transcript))

            val deleted = if (drawerIngested) {
                connectivityBridge.deleteRecording(filename)
            } else {
                false
            }
            if (localFile.exists()) {
                localFile.delete()
            }
            android.util.Log.d(
                TAG,
                "Path A Cleanup: drawerIngested=$drawerIngested badge=${if (deleted) "deleted" else "preserved"} local=${if (localFile.exists()) "preserved" else "deleted"}"
            )
            
        } catch (e: Exception) {
            _currentState.value = PipelineState.IDLE
            _events.emit(PipelineEvent.Error(
                stage = currentStage, // Use tracked stage, not hardcoded CLEANUP
                message = e.message ?: "未知错误",
                filename = filename
            ))
            android.util.Log.e(TAG, "Pipeline error at stage $currentStage", e)
        }
    }
    
    override fun isIdle(): Boolean = currentState.value == PipelineState.IDLE
    
    // mapToSchedulerResult is deprecated and removed since we directly map PipelineResults now.
}
