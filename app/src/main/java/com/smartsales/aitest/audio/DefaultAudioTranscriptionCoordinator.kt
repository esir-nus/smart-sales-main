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
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.media.audiofiles.TranscriptionBatchPlanner
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

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
        sessionId: String?,
    ): Result<String> {
        // 重要：Tingwu 走 OSS FileUrl 链路，使用上传后的预签名 URL 作为唯一输入。
        val request = TingwuRequest(
            audioAssetName = audioAssetName,
            language = language,
            ossObjectKey = uploadPayload.objectKey,
            fileUrl = uploadPayload.presignedUrl,
            sessionId = sessionId
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
                is TingwuJobState.Failed -> AudioTranscriptionJobState.Failed(
                    jobId = state.jobId,
                    reason = state.reason,
                )
            }
        }
    }

    override fun observeBatches(jobId: String): Flow<AudioTranscriptionBatchEvent> = channelFlow {
        tingwuCoordinator.observeJob(jobId)
            .filterIsInstance<TingwuJobState.Completed>()
            .take(1)
            .collect { state ->
                val plan = TranscriptionBatchPlanner.plan(state.transcriptMarkdown)
                if (plan.totalBatches <= 0) return@collect
                plan.batches.forEach { batch ->
                    send(
                        AudioTranscriptionBatchEvent.BatchReleased(
                            jobId = state.jobId,
                            batchIndex = batch.batchIndex,
                            totalBatches = batch.totalBatches,
                            markdownChunk = batch.markdownChunk,
                            isFinal = batch.batchIndex == batch.totalBatches,
                            batchSize = plan.batchSize,
                            lineCount = batch.lineCount,
                            ruleLabel = plan.ruleLabel
                        )
                    )
                }
            }
    }
}
