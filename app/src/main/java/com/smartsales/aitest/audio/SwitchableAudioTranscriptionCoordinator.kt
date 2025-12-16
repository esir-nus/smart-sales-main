// 文件：app/src/main/java/com/smartsales/aitest/audio/SwitchableAudioTranscriptionCoordinator.kt
// 模块：:app
// 说明：根据设置在 Tingwu 与 XFyun 之间切换 AudioTranscriptionCoordinator
// 作者：创建于 2025-12-15
package com.smartsales.aitest.audio

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.TRANSCRIPTION_PROVIDER_TINGWU
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SwitchableAudioTranscriptionCoordinator @Inject constructor(
    private val tingwuDelegate: DefaultAudioTranscriptionCoordinator,
    private val xfyunDelegate: XfyunAudioTranscriptionCoordinator,
    private val aiParaSettingsProvider: AiParaSettingsProvider,
) : AudioTranscriptionCoordinator {

    override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> =
        delegate().uploadAudio(file)

    override suspend fun submitTranscription(
        audioAssetName: String,
        language: String,
        uploadPayload: AudioUploadPayload,
        sessionId: String?,
    ): Result<String> = delegate().submitTranscription(
        audioAssetName = audioAssetName,
        language = language,
        uploadPayload = uploadPayload,
        sessionId = sessionId,
    )

    override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> =
        delegate().observeJob(jobId)

    private fun delegate(): AudioTranscriptionCoordinator =
        when (aiParaSettingsProvider.snapshot().transcription.provider.trim().uppercase(Locale.US)) {
            TRANSCRIPTION_PROVIDER_TINGWU -> tingwuDelegate
            else -> xfyunDelegate
        }
}
