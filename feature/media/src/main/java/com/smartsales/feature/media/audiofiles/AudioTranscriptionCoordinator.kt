package com.smartsales.feature.media.audiofiles

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audiofiles/AudioTranscriptionCoordinator.kt
// 模块：:feature:media
// 说明：抽象音频上传与转写协调器，隔离对底层 Tingwu/OSS 的依赖
// 作者：创建于 2025-11-21

import com.smartsales.core.util.Result
import com.smartsales.feature.media.audio.TingwuChapterUi
import com.smartsales.feature.media.audio.TingwuSmartSummaryUi
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * AudioFiles 模块只依赖该接口，不直接耦合 data 层 SDK。
 */
interface AudioTranscriptionCoordinator {
    suspend fun uploadAudio(file: File): Result<AudioUploadPayload>

    suspend fun submitTranscription(
        audioAssetName: String,
        language: String,
        uploadPayload: AudioUploadPayload,
        sessionId: String? = null,
    ): Result<String>

    fun observeJob(jobId: String): Flow<AudioTranscriptionJobState>

    // 说明：伪流式批次事件采用非 conflated 的 Flow 输出。
    fun observeBatches(jobId: String): Flow<AudioTranscriptionBatchEvent>
}

data class AudioUploadPayload(
    val objectKey: String,
    val presignedUrl: String,
)

// 说明：时间基准为录音起点 0ms（非 captureStartMs）。
data class V1TimedTextSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

sealed interface AudioTranscriptionJobState {
    data object Idle : AudioTranscriptionJobState
    data class InProgress(
        val jobId: String,
        val progressPercent: Int,
    ) : AudioTranscriptionJobState

    data class Completed(
        val jobId: String,
        val transcriptMarkdown: String,
        val transcriptionUrl: String? = null,
        val autoChaptersUrl: String? = null,
        val chapters: List<TingwuChapterUi>? = null,
        val smartSummary: TingwuSmartSummaryUi? = null,
    ) : AudioTranscriptionJobState

    data class Failed(
        val jobId: String,
        val reason: String,
    ) : AudioTranscriptionJobState
}

sealed interface AudioTranscriptionBatchEvent {
    val jobId: String

    data class BatchReleased(
        override val jobId: String,
        val batchIndex: Int,
        val totalBatches: Int,
        val markdownChunk: String,
        val isFinal: Boolean,
        val batchSize: Int,
        val lineCount: Int,
        val ruleLabel: String,
        // 说明：V1 可选窗口，仅用于后续时间锚定与宏窗口过滤（当前不改变展示）。
        val v1Window: V1TranscriptionBatchWindow? = null,
        // 说明：V1 可选分段时间戳，仅用于后续窗口过滤（当前不改变展示）。
        val timedSegments: List<V1TimedTextSegment>? = null
    ) : AudioTranscriptionBatchEvent
}
