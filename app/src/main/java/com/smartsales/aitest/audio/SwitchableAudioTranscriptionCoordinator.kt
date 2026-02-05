// 文件：app/src/main/java/com/smartsales/aitest/audio/SwitchableAudioTranscriptionCoordinator.kt
// 模块：:app
// 说明：统一 AudioTranscriptionCoordinator 实现（仅使用 Tingwu）
// 作者：创建于 2025-12-15，简化于 2026-01-11
package com.smartsales.aitest.audio

import com.smartsales.core.util.Result
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator

import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Unified AudioTranscriptionCoordinator that delegates to Tingwu.
 */
@Singleton
class SwitchableAudioTranscriptionCoordinator @Inject constructor(
    private val tingwuDelegate: DefaultAudioTranscriptionCoordinator,
) : AudioTranscriptionCoordinator {

    override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> =
        tingwuDelegate.uploadAudio(file)

    override suspend fun submitTranscription(
        audioAssetName: String,
        language: String,
        uploadPayload: AudioUploadPayload,
        sessionId: String?,
    ): Result<String> = tingwuDelegate.submitTranscription(
        audioAssetName = audioAssetName,
        language = language,
        uploadPayload = uploadPayload,
        sessionId = sessionId,
    )

    override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> =
        tingwuDelegate.observeJob(jobId)


}
