package com.smartsales.prism.domain.audio

/**
 * Badge Audio Pipeline 事件流
 * 
 * Wave 1: 状态机定义
 * @see docs/cerb/badge-audio-pipeline/spec.md
 */
sealed class PipelineEvent {
    /** 录音开始 */
    object RecordingStarted : PipelineEvent()
    
    /** 正在下载录音文件 */
    data class Downloading(val filename: String) : PipelineEvent()
    
    /** 正在转写 */
    data class Transcribing(val filename: String, val sizeBytes: Long) : PipelineEvent()
    
    /** 正在处理（调度流程） */
    data class Processing(val transcript: String) : PipelineEvent()
    
    /** 完成 */
    data class Complete(val result: SchedulerResult, val filename: String, val transcript: String) : PipelineEvent()
    
    /** 错误 */
    data class Error(val stage: Stage, val message: String, val filename: String?) : PipelineEvent()
    
    enum class Stage { DOWNLOAD, TRANSCRIBE, SCHEDULE, CLEANUP }
}

/**
 * 调度结果（domain-agnostic）
 * 
 * Wave 3 将映射 UiState → SchedulerResult
 */
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

/**
 * Pipeline 当前状态
 */
enum class PipelineState { IDLE, DOWNLOADING, TRANSCRIBING, PROCESSING }
