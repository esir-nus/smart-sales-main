package com.smartsales.prism.data.audio

import android.util.Log
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
                        Log.d(TAG, "New recording detected: ${notification.filename}")
                        processFile(notification.filename)
                    }
                }
            }
        }
    }
    
    override suspend fun processFile(filename: String) {
        var currentStage = PipelineEvent.Stage.DOWNLOAD
        try {
            // 1. 下载录音
            currentStage = PipelineEvent.Stage.DOWNLOAD
            _currentState.value = PipelineState.DOWNLOADING
            _events.emit(PipelineEvent.Downloading(filename))
            Log.d(TAG, "Downloading: $filename")
            
            val downloadResult = connectivityBridge.downloadRecording(filename)
            if (downloadResult is WavDownloadResult.Error) {
                _currentState.value = PipelineState.IDLE
                _events.emit(PipelineEvent.Error(
                    stage = PipelineEvent.Stage.DOWNLOAD,
                    message = downloadResult.message,
                    filename = filename
                ))
                Log.w(TAG, "Download failed: ${downloadResult.message}")
                return
            }
            
            val localFile = (downloadResult as WavDownloadResult.Success).localFile
            Log.d(TAG, "Downloaded ${downloadResult.sizeBytes} bytes")
            
            // 2. 转写
            currentStage = PipelineEvent.Stage.TRANSCRIBE
            _currentState.value = PipelineState.TRANSCRIBING
            _events.emit(PipelineEvent.Transcribing(filename, downloadResult.sizeBytes))
            Log.d(TAG, "Transcribing...")
            
            val asrResult = asrService.transcribe(localFile)
            if (asrResult is AsrResult.Error) {
                _currentState.value = PipelineState.IDLE
                _events.emit(PipelineEvent.Error(
                    stage = PipelineEvent.Stage.TRANSCRIBE,
                    message = "转写失败: ${asrResult.message}",
                    filename = filename
                ))
                Log.w(TAG, "Transcription failed: ${asrResult.message}")
                localFile.delete()
                return
            }
            
            val transcript = (asrResult as AsrResult.Success).text
            Log.d(TAG, "Transcribed: $transcript")
            
            // 3. 调度
            currentStage = PipelineEvent.Stage.SCHEDULE
            _currentState.value = PipelineState.PROCESSING
            _events.emit(PipelineEvent.Processing(transcript))
            Log.d(TAG, "Scheduling task...")
            
            val uiState = orchestrator.createScheduledTask(transcript)
            val schedulerResult = mapToSchedulerResult(uiState)
            
            if (schedulerResult == null) {
                // 非日程意图（聊天、无效输入等），静默完成
                _currentState.value = PipelineState.IDLE
                Log.d(TAG, "Non-scheduling intent, skipping: $uiState")
                localFile.delete()
                return
            }
            
            Log.d(TAG, "Scheduled: $schedulerResult")
            
            // 4. 清理
            currentStage = PipelineEvent.Stage.CLEANUP
            _currentState.value = PipelineState.IDLE
            _events.emit(PipelineEvent.Complete(schedulerResult, filename, transcript))
            
            val deleted = connectivityBridge.deleteRecording(filename)
            localFile.delete()
            Log.d(TAG, "Cleanup: badge=${if (deleted) "✓" else "✗"}, local=✓")
            
        } catch (e: Exception) {
            _currentState.value = PipelineState.IDLE
            _events.emit(PipelineEvent.Error(
                stage = currentStage,
                message = e.message ?: "未知错误",
                filename = filename
            ))
            Log.e(TAG, "Pipeline error at stage $currentStage", e)
        }
    }
    
    override fun isIdle(): Boolean = currentState.value == PipelineState.IDLE
    
    /**
     * 将 Orchestrator 返回的 UiState 映射为 Pipeline 领域的 SchedulerResult。
     * 
     * 可能的 UiState:
     *   SchedulerTaskCreated → 单任务创建
     *   AwaitingClarification → 需要用户澄清
     *   Toast → 灵感保存
     *   Response → 多任务创建（Wave 4.1）
     *   Error → 调度失败
     *   Idle → 非日程意图（静默忽略）
     * 
     * @return null 表示非日程意图，Pipeline 应静默跳过
     */
    private fun mapToSchedulerResult(uiState: UiState): SchedulerResult? = when (uiState) {
        is UiState.SchedulerTaskCreated -> SchedulerResult.TaskCreated(uiState.taskId, uiState.title)
        is UiState.AwaitingClarification -> SchedulerResult.AwaitingClarification(uiState.question)
        is UiState.Toast -> SchedulerResult.InspirationSaved(uiState.message)
        // 多任务：Orchestrator 返回 Response("✅ 已创建 N 个任务")，当前无 taskIds
        is UiState.Response -> SchedulerResult.TaskCreated("multi", uiState.content)
        // 非日程意图：Orchestrator 返回 Idle，Pipeline 静默跳过
        is UiState.Idle -> null
        // 调度错误：透传为 null，由上层 catch 处理
        is UiState.Error -> throw IllegalStateException(uiState.message)
        // 其他状态不应出现在调度流程中
        else -> {
            Log.w(TAG, "Unexpected UiState from scheduler: $uiState")
            null
        }
    }
}
