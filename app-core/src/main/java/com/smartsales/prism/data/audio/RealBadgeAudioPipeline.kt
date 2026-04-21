package com.smartsales.prism.data.audio

import com.smartsales.prism.AppFlavor
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
import com.smartsales.prism.service.SchedulerPipelineOrchestrator
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.PipelineResult
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
import kotlinx.coroutines.withContext
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
    private val simBadgeAudioPipelineIngestSupport: SimBadgeAudioPipelineIngestSupport,
    private val schedulerPipelineOrchestrator: SchedulerPipelineOrchestrator
) : BadgeAudioPipeline {

    companion object {
        private const val TAG = "AudioPipeline"
    }

    private val schedulerEnabled = AppFlavor.schedulerEnabled
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _events = MutableSharedFlow<PipelineEvent>(
        replay = 3,
        extraBufferCapacity = 1
    )
    override val events: SharedFlow<PipelineEvent> = _events.asSharedFlow()
    
    private val _currentState = MutableStateFlow(PipelineState.IDLE)
    override val currentState: StateFlow<PipelineState> = _currentState.asStateFlow()

    init {
        if (schedulerEnabled) {
            // 启动录音监听
            scope.launch {
                connectivityBridge.recordingNotifications().collect { notification ->
                    when (notification) {
                        is RecordingNotification.RecordingReady -> {
                            android.util.Log.d(TAG, "New recording detected: ${notification.filename}")
                            schedulerPipelineOrchestrator.enqueue(notification.filename)
                        }
                        is RecordingNotification.AudioRecordingReady -> {
                            // rec# 通知由 SimBadgeAudioAutoDownloader 处理，scheduler pipeline 忽略
                        }
                    }
                }
            }
        } else {
            android.util.Log.d(TAG, "Scheduler disabled for this flavor; skipping badge scheduler listener")
        }
    }

    override suspend fun processFile(filename: String) {
        runStages(filename)
    }

    internal suspend fun runStages(
        filename: String,
        onStageChanged: (PipelineEvent.Stage) -> Unit = {}
    ): BadgeAudioPipelineRunOutcome {
        var currentStage = PipelineEvent.Stage.DOWNLOAD
        try {
            currentStage = PipelineEvent.Stage.DOWNLOAD
            onStageChanged(currentStage)
            _currentState.value = PipelineState.DOWNLOADING
            _events.emit(PipelineEvent.Downloading(filename))
            android.util.Log.d(TAG, "Downloading: $filename")

            val downloadResult = connectivityBridge.downloadRecording(filename)
            if (downloadResult is WavDownloadResult.Error) {
                _currentState.value = PipelineState.IDLE
                emitStageError(
                    stage = PipelineEvent.Stage.DOWNLOAD,
                    message = downloadResult.message,
                    filename = filename
                )
                android.util.Log.w(TAG, "Download failed: ${downloadResult.message}")
                return BadgeAudioPipelineRunOutcome.Failed(
                    stage = PipelineEvent.Stage.DOWNLOAD,
                    message = downloadResult.message
                )
            }

            val localFile = (downloadResult as WavDownloadResult.Success).localFile
            android.util.Log.d(TAG, "Downloaded ${downloadResult.sizeBytes} bytes")

            currentStage = PipelineEvent.Stage.TRANSCRIBE
            onStageChanged(currentStage)
            _currentState.value = PipelineState.TRANSCRIBING
            _events.emit(PipelineEvent.Transcribing(filename, downloadResult.sizeBytes))
            android.util.Log.d(TAG, "Transcribing...")

            val asrResult = asrService.transcribe(localFile)
            if (asrResult is AsrResult.Error) {
                _currentState.value = PipelineState.IDLE
                val errorMessage = "转写失败: ${asrResult.message}"
                emitStageError(
                    stage = PipelineEvent.Stage.TRANSCRIBE,
                    message = errorMessage,
                    filename = filename
                )
                android.util.Log.w(TAG, "Transcription failed: ${asrResult.message}")
                localFile.delete()
                return BadgeAudioPipelineRunOutcome.Failed(
                    stage = PipelineEvent.Stage.TRANSCRIBE,
                    message = errorMessage
                )
            }

            val transcript = (asrResult as AsrResult.Success).text
            android.util.Log.d(TAG, "Transcribed: $transcript")

            val schedulerResult = if (schedulerEnabled) {
                currentStage = PipelineEvent.Stage.SCHEDULE
                onStageChanged(currentStage)
                _currentState.value = PipelineState.PROCESSING
                _events.emit(PipelineEvent.Processing(transcript))
                android.util.Log.d(TAG, "Scheduling task...")

                resolveSchedulerResult(transcript).also { result ->
                    android.util.Log.d(TAG, "Path A committed via IntentOrchestrator: $result")
                }
            } else {
                android.util.Log.d(TAG, "Scheduler disabled for this flavor; keeping badge recording as audio-only ingest")
                SchedulerResult.Ignored
            }

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

            if (localFile.exists()) {
                localFile.delete()
            }
            android.util.Log.d(
                TAG,
                "Path A Cleanup: drawerIngested=$drawerIngested local=${if (localFile.exists()) "preserved" else "deleted"} (badge WAV retained — badge manages own retention)"
            )
            return BadgeAudioPipelineRunOutcome.Completed(schedulerResult)
        } catch (e: Exception) {
            _currentState.value = PipelineState.IDLE
            val errorMessage = e.message ?: "未知错误"
            emitStageError(
                stage = currentStage,
                message = errorMessage,
                filename = filename
            )
            android.util.Log.e(TAG, "Pipeline error at stage $currentStage", e)
            return BadgeAudioPipelineRunOutcome.Failed(
                stage = currentStage,
                message = errorMessage
            )
        }
    }

    override fun isIdle(): Boolean = currentState.value == PipelineState.IDLE

    private suspend fun emitStageError(
        stage: PipelineEvent.Stage,
        message: String,
        filename: String
    ) {
        _events.emit(
            PipelineEvent.Error(
                stage = stage,
                message = message,
                filename = filename
            )
        )
    }

    private suspend fun resolveSchedulerResult(transcript: String): SchedulerResult = withContext(Dispatchers.IO) {
        val committedTasks = mutableListOf<SchedulerResult.TaskCreated>()
        var inspirationResult: SchedulerResult.InspirationSaved? = null
        var clarificationResult: SchedulerResult.AwaitingClarification? = null

        intentOrchestrator.processInput(transcript, isVoice = true).collect { result ->
            when (result) {
                is PipelineResult.PathACommitted -> {
                    committedTasks += SchedulerResult.TaskCreated(
                        taskId = result.task.id,
                        title = result.task.title,
                        dayOffset = 0,
                        scheduledAtMillis = result.task.startTime.toEpochMilli(),
                        durationMinutes = result.task.durationMinutes
                    )
                }

                is PipelineResult.InspirationCommitted -> {
                    if (committedTasks.isEmpty() && inspirationResult == null) {
                        inspirationResult = SchedulerResult.InspirationSaved(result.id)
                    }
                }

                is PipelineResult.ClarificationNeeded -> {
                    if (committedTasks.isEmpty() && inspirationResult == null && clarificationResult == null) {
                        clarificationResult = SchedulerResult.AwaitingClarification(result.question)
                    }
                }

                is PipelineResult.DisambiguationIntercepted -> {
                    val question = (result.uiState as? UiState.AwaitingClarification)?.question
                    if (
                        committedTasks.isEmpty() &&
                        inspirationResult == null &&
                        clarificationResult == null &&
                        !question.isNullOrBlank()
                    ) {
                        clarificationResult = SchedulerResult.AwaitingClarification(question)
                    }
                }

                else -> Unit
            }
        }

        when {
            committedTasks.size == 1 -> committedTasks.single()
            committedTasks.isNotEmpty() -> SchedulerResult.MultiTaskCreated(committedTasks.toList())
            inspirationResult != null -> inspirationResult!!
            clarificationResult != null -> clarificationResult!!
            else -> SchedulerResult.Ignored
        }
    }
}

internal sealed interface BadgeAudioPipelineRunOutcome {
    data class Completed(val result: SchedulerResult) : BadgeAudioPipelineRunOutcome
    data class Failed(
        val stage: PipelineEvent.Stage,
        val message: String
    ) : BadgeAudioPipelineRunOutcome
}
