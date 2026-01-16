package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/DefaultAudioTranscriptionCoordinator.kt
// 模块：:app
// 说明：封装真实 Tingwu + OSS 协调器，供 AudioFilesViewModel 注入使用
// 作者：创建于 2025-11-21

import android.media.MediaMetadataRetriever
import android.util.Log
import com.smartsales.aitest.BuildConfig
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultAudioTranscriptionCoordinator @Inject constructor(
    private val tingwuCoordinator: TingwuCoordinator,
    private val ossUploadClient: OssUploadClient,
) : AudioTranscriptionCoordinator {

    private val pendingDurationByObjectKey = ConcurrentHashMap<String, Long>()

    override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> {
        return when (val upload = ossUploadClient.uploadAudio(OssUploadRequest(file = file))) {
            is Result.Success -> Result.Success(
                AudioUploadPayload(
                    objectKey = upload.data.objectKey,
                    presignedUrl = upload.data.presignedUrl,
                    localFile = file
                ).also { payload ->
                    // 仅记录时长，不记录内容；用于后续 V1 时间窗口生成。
                    readDurationMillis(file)?.let { durationMs ->
                        pendingDurationByObjectKey[payload.objectKey] = durationMs
                    }
                }
            )
            is Result.Error -> Result.Error(upload.throwable)
        }
    }

    override suspend fun submitTranscription(
        audioAssetName: String,
        language: String,
        uploadPayload: AudioUploadPayload,
        sessionId: String?,
    ): Result<String> {
        val durationMs = pendingDurationByObjectKey.remove(uploadPayload.objectKey)
        // 重要：Tingwu 走 OSS FileUrl 链路，使用上传后的预签名 URL 作为唯一输入。
        val request = TingwuRequest(
            audioAssetName = audioAssetName,
            language = language,
            ossObjectKey = uploadPayload.objectKey,
            fileUrl = uploadPayload.presignedUrl,
            sessionId = sessionId,
            durationMs = durationMs,
            audioFilePath = uploadPayload.localFile
        )
        val result = tingwuCoordinator.submit(request)
        return result
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
                    transcriptionUrl = state.artifacts?.transcriptionUrl,
                    autoChaptersUrl = state.artifacts?.autoChaptersUrl,
                    chapters = state.artifacts?.chapters?.map {
                        com.smartsales.feature.media.audio.TingwuChapterUi(
                            title = it.title,
                            startMs = it.startMs,
                            endMs = it.endMs
                        )
                    },
                    smartSummary = state.artifacts?.smartSummary?.let {
                        com.smartsales.feature.media.audio.TingwuSmartSummaryUi(
                            summary = it.summary,
                            keyPoints = it.keyPoints,
                            actionItems = it.actionItems
                        )
                    }
                )
                is TingwuJobState.Failed -> {
                    AudioTranscriptionJobState.Failed(
                        jobId = state.jobId,
                        reason = state.reason,
                    )
                }
            }
        }
    }

    private fun readDurationMillis(file: File): Long? {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "DefaultAudioTranscriptionCoordinator"
    }
}
