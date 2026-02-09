package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.PipelineState
import com.smartsales.prism.domain.audio.SchedulerResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FakeBadgeAudioPipeline — Wave 2 UI 开发用测试实现
 * 
 * 场景方法在调用者的 coroutineScope 中运行
 * 
 * @see docs/cerb/badge-audio-pipeline/spec.md
 */
@Singleton
class FakeBadgeAudioPipeline @Inject constructor() : BadgeAudioPipeline {
    
    private val _events = MutableSharedFlow<PipelineEvent>(replay = 3)
    override val events: SharedFlow<PipelineEvent> = _events.asSharedFlow()
    
    private val _currentState = MutableStateFlow(PipelineState.IDLE)
    override val currentState: StateFlow<PipelineState> = _currentState.asStateFlow()
    
    override suspend fun processFile(filename: String) {
        // 默认模拟成功路径
        simulateHappyPath(filename)
    }
    
    override fun isIdle(): Boolean = _currentState.value == PipelineState.IDLE
    
    // =====================
    // L2 场景模拟方法
    // =====================
    
    /**
     * 模拟成功路径: Downloading → Transcribing → Processing → Complete
     */
    suspend fun simulateHappyPath(filename: String = "test.wav") {
        _currentState.value = PipelineState.DOWNLOADING
        _events.emit(PipelineEvent.Downloading(filename))
        delay(500)
        
        _currentState.value = PipelineState.TRANSCRIBING
        _events.emit(PipelineEvent.Transcribing(filename, 1024L))
        delay(800)
        
        _currentState.value = PipelineState.PROCESSING
        _events.emit(PipelineEvent.Processing("明天开会"))
        delay(600)
        
        _currentState.value = PipelineState.IDLE
        _events.emit(PipelineEvent.Complete(
            SchedulerResult.TaskCreated(
                taskId = "fake-task-1",
                title = "明天开会",
                dayOffset = 1,
                scheduledAtMillis = System.currentTimeMillis() + 86_400_000,
                durationMinutes = 60
            ),
            filename,
            "明天开会"
        ))
    }
    
    /**
     * 模拟下载错误: Downloading → Error(DOWNLOAD)
     */
    suspend fun simulateDownloadError(filename: String = "test.wav") {
        _currentState.value = PipelineState.DOWNLOADING
        _events.emit(PipelineEvent.Downloading(filename))
        delay(300)
        
        _currentState.value = PipelineState.IDLE
        _events.emit(PipelineEvent.Error(
            PipelineEvent.Stage.DOWNLOAD,
            "Badge offline - 请检查徽章连接",
            filename
        ))
    }
    
    /**
     * 模拟转写错误: Downloading → Transcribing → Error(TRANSCRIBE)
     */
    suspend fun simulateAsrError(filename: String = "test.wav") {
        _currentState.value = PipelineState.DOWNLOADING
        _events.emit(PipelineEvent.Downloading(filename))
        delay(500)
        
        _currentState.value = PipelineState.TRANSCRIBING
        _events.emit(PipelineEvent.Transcribing(filename, 1024L))
        delay(300)
        
        _currentState.value = PipelineState.IDLE
        _events.emit(PipelineEvent.Error(
            PipelineEvent.Stage.TRANSCRIBE,
            "ASR service unavailable - 转写服务不可用",
            filename
        ))
    }
    
    /**
     * 模拟灵感保存: Downloading → Transcribing → Processing → InspirationSaved
     */
    suspend fun simulateInspirationSaved(filename: String = "test.wav") {
        _currentState.value = PipelineState.DOWNLOADING
        _events.emit(PipelineEvent.Downloading(filename))
        delay(500)
        
        _currentState.value = PipelineState.TRANSCRIBING
        _events.emit(PipelineEvent.Transcribing(filename, 512L))
        delay(600)
        
        _currentState.value = PipelineState.PROCESSING
        _events.emit(PipelineEvent.Processing("以后想学吉他"))
        delay(400)
        
        _currentState.value = PipelineState.IDLE
        _events.emit(PipelineEvent.Complete(
            SchedulerResult.InspirationSaved("inspiration-1"),
            filename,
            "以后想学吉他"
        ))
    }
}
