package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/DefaultAudioTranscriptionCoordinator.kt
// 模块：:app
// 说明：封装真实 Tingwu + OSS 协调器，供 AudioFilesViewModel 注入使用
// 作者：创建于 2025-11-21

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.OssUploadClient
import com.smartsales.data.aicore.OssUploadRequest
import com.smartsales.data.aicore.TingwuCoordinator
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.TingwuRequest
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultAudioTranscriptionCoordinator @Inject constructor(
    private val tingwuCoordinator: TingwuCoordinator,
    private val ossUploadClient: OssUploadClient,
) : AudioTranscriptionCoordinator {

    override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> {
        return when (val upload = ossUploadClient.uploadAudio(OssUploadRequest(file = file))) {
            is Result.Success -> Result.Success(
                AudioUploadPayload(
                    objectKey = upload.data.objectKey,
                    presignedUrl = upload.data.presignedUrl,
                )
            )
            is Result.Error -> Result.Error(upload.throwable)
        }
    }

    override suspend fun submitTranscription(
        audioAssetName: String,
        language: String,
        uploadPayload: AudioUploadPayload,
    ): Result<String> {
        val request = TingwuRequest(
            audioAssetName = audioAssetName,
            language = language,
            ossObjectKey = uploadPayload.objectKey,
            fileUrl = uploadPayload.presignedUrl,
        )
        return tingwuCoordinator.submit(request)
    }

    override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> {
        return tingwuCoordinator.observeJob(jobId).map { state ->
            when (state) {
                is TingwuJobState.Idle -> AudioTranscriptionJobState.Idle
                is TingwuJobState.InProgress -> AudioTranscriptionJobState.InProgress(
                    jobId = state.jobId,
                    progressPercent = state.progressPercent,
                )
                is TingwuJobState.Completed -> AudioTranscriptionJobState.Completed(
                    jobId = state.jobId,
                    transcriptMarkdown = state.transcriptMarkdown,
                )
                is TingwuJobState.Failed -> AudioTranscriptionJobState.Failed(
                    jobId = state.jobId,
                    reason = state.reason,
                )
            }
        }
    }
}
