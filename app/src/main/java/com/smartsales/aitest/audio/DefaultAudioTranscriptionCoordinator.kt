package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/DefaultAudioTranscriptionCoordinator.kt
// 模块：:app
// 说明：封装真实 Tingwu + OSS 协调器，供 AudioFilesViewModel 注入使用
// 作者：创建于 2025-11-21

import android.media.MediaMetadataRetriever
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
import com.smartsales.feature.media.audiofiles.TranscriptionBatchPlanWithWindows
import com.smartsales.feature.media.audiofiles.TranscriptionBatchPlanner
import com.smartsales.feature.media.audiofiles.V1TimedTextSegment
import java.io.File
import java.util.concurrent.ConcurrentHashMap
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

    private val pendingDurationByObjectKey = ConcurrentHashMap<String, Long>()
    private val durationByJobId = ConcurrentHashMap<String, Long>()

    override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> {
        return when (val upload = ossUploadClient.uploadAudio(OssUploadRequest(file = file))) {
            is Result.Success -> Result.Success(
                AudioUploadPayload(
                    objectKey = upload.data.objectKey,
                    presignedUrl = upload.data.presignedUrl,
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
            sessionId = sessionId
        )
        val result = tingwuCoordinator.submit(request)
        if (result is Result.Success) {
            durationMs?.let { durationByJobId[result.data] = it }
        }
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
                    durationByJobId.remove(state.jobId)
                    AudioTranscriptionJobState.Failed(
                        jobId = state.jobId,
                        reason = state.reason,
                    )
                }
            }
        }
    }

    override fun observeBatches(jobId: String): Flow<AudioTranscriptionBatchEvent> = channelFlow {
        tingwuCoordinator.observeJob(jobId)
            .filterIsInstance<TingwuJobState.Completed>()
            .take(1)
            .collect { state ->
                val planWithWindows = buildBatchPlanWithWindowsForTingwu(
                    markdown = state.transcriptMarkdown,
                    audioDurationMs = durationByJobId[state.jobId],
                    batchDurationMs = V1_BATCH_DURATION_MS,
                    overlapMs = V1_OVERLAP_MS
                )
                // 说明：窗口计划仅用于后续 V1 锚点接入，当前展示逻辑不变。
                val plan = planWithWindows?.plan ?: TranscriptionBatchPlanner.plan(state.transcriptMarkdown)
                if (plan.totalBatches <= 0) {
                    durationByJobId.remove(state.jobId)
                    return@collect
                }
                val timedSegments = state.artifacts?.diarizedSegments
                    ?.map { segment ->
                        V1TimedTextSegment(
                            startMs = segment.startMs,
                            endMs = segment.endMs,
                            text = segment.text
                        )
                    }
                    ?.takeIf { it.isNotEmpty() }
                plan.batches.forEach { batch ->
                    // 仅透传窗口数据（可选），不影响现有伪流式展示。
                    val v1Window = planWithWindows?.windows?.getOrNull(batch.batchIndex - 1)
                    send(
                        AudioTranscriptionBatchEvent.BatchReleased(
                            jobId = state.jobId,
                            batchIndex = batch.batchIndex,
                            totalBatches = batch.totalBatches,
                            markdownChunk = batch.markdownChunk,
                            isFinal = batch.batchIndex == batch.totalBatches,
                            batchSize = plan.batchSize,
                            lineCount = batch.lineCount,
                            ruleLabel = plan.ruleLabel,
                            v1Window = v1Window,
                            // 仅贯通时间戳分段数据，当前不改展示逻辑。
                            timedSegments = timedSegments
                        )
                    )
                }
                durationByJobId.remove(state.jobId)
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
        // 说明：为 V1 时间窗口预留的保守默认值（当前仅生成窗口，不影响展示）。
        private const val V1_BATCH_DURATION_MS = 60_000L
        private const val V1_OVERLAP_MS = 5_000L
    }
}

internal fun buildBatchPlanWithWindowsForTingwu(
    markdown: String,
    audioDurationMs: Long?,
    batchDurationMs: Long,
    overlapMs: Long
): TranscriptionBatchPlanWithWindows? {
    if (audioDurationMs == null) return null
    return TranscriptionBatchPlanner.planWithWindows(
        markdown = markdown,
        audioDurationMs = audioDurationMs,
        batchDurationMs = batchDurationMs,
        overlapMs = overlapMs
    )
}
