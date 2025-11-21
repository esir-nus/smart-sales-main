package com.smartsales.feature.media.audiofiles

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audiofiles/AudioTranscriptionCoordinator.kt
// 模块：:feature:media
// 说明：抽象音频上传与转写协调器，隔离对底层 Tingwu/OSS 的依赖
// 作者：创建于 2025-11-21

import com.smartsales.core.util.Result
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
    ): Result<String>

    fun observeJob(jobId: String): Flow<AudioTranscriptionJobState>
}

data class AudioUploadPayload(
    val objectKey: String,
    val presignedUrl: String,
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
    ) : AudioTranscriptionJobState

    data class Failed(
        val jobId: String,
        val reason: String,
    ) : AudioTranscriptionJobState
}
