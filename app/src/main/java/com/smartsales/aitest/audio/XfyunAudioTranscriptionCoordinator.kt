// 文件：app/src/main/java/com/smartsales/aitest/audio/XfyunAudioTranscriptionCoordinator.kt
// 模块：:app
// 说明：将讯飞 ASR 协调器适配为 AudioFiles 所需的 AudioTranscriptionCoordinator
// 作者：创建于 2025-12-15
package com.smartsales.aitest.audio

import android.media.MediaMetadataRetriever
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.xfyun.XfyunAsrCoordinator
import com.smartsales.data.aicore.xfyun.XfyunAsrJobState
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.media.audiofiles.TranscriptionBatchPlanWithWindows
import com.smartsales.feature.media.audiofiles.TranscriptionBatchPlanner
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

@Singleton
class XfyunAudioTranscriptionCoordinator @Inject constructor(
    private val xfyunAsrCoordinator: XfyunAsrCoordinator,
) : AudioTranscriptionCoordinator {

    private val pendingUploads = ConcurrentHashMap<String, PendingUpload>()
    private val durationByJobId = ConcurrentHashMap<String, Long>()

    override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> {
        val ext = file.extension.lowercase(Locale.ROOT)
        if (ext !in SUPPORTED_EXTENSIONS) {
            return Result.Error(
                AiCoreException(
                    source = AiCoreErrorSource.XFYUN,
                    reason = AiCoreErrorReason.IO,
                    message = "XFyun 当前仅支持 WAV/MP3，本文件格式不支持：${ext.ifBlank { "未知" }}"
                )
            )
        }
        val durationMs = readDurationMillis(file)
        val token = "local-${UUID.randomUUID()}"
        pendingUploads[token] = PendingUpload(file = file, durationMs = durationMs)
        return Result.Success(
            AudioUploadPayload(
                // 复用 objectKey 作为临时 token，submit 时再取回本地文件
                objectKey = token,
                presignedUrl = "xfyun://local"
            )
        )
    }

    override suspend fun submitTranscription(
        audioAssetName: String,
        language: String,
        uploadPayload: AudioUploadPayload,
        sessionId: String?,
    ): Result<String> {
        val pending = pendingUploads.remove(uploadPayload.objectKey)
            ?: return Result.Error(
                AiCoreException(
                    source = AiCoreErrorSource.XFYUN,
                    reason = AiCoreErrorReason.IO,
                    message = "本地音频上下文丢失，请重新选择并提交转写"
                )
        )

        // 重要：讯飞转写参数由 AiParaSettings 统一管理，这里只负责把本地文件提交给协调器。
        val result = xfyunAsrCoordinator.submitTranscription(
            file = pending.file,
            language = language,
            durationMs = pending.durationMs,
        )
        if (result is Result.Success) {
            pending.durationMs?.let { durationMs ->
                // 仅保存时长，不记录内容；用于后续 V1 时间窗口生成。
                durationByJobId[result.data] = durationMs
            }
        }
        return result
    }

    override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> {
        return xfyunAsrCoordinator.observeJob(jobId).map { state ->
            when (state) {
                is XfyunAsrJobState.Idle -> AudioTranscriptionJobState.Idle
                is XfyunAsrJobState.InProgress -> AudioTranscriptionJobState.InProgress(
                    jobId = state.jobId,
                    progressPercent = state.progressPercent
                )
                is XfyunAsrJobState.Completed -> AudioTranscriptionJobState.Completed(
                    jobId = state.jobId,
                    transcriptMarkdown = state.transcriptMarkdown
                )
                is XfyunAsrJobState.Failed -> {
                    durationByJobId.remove(state.jobId)
                    AudioTranscriptionJobState.Failed(
                        jobId = state.jobId,
                        reason = state.reason
                    )
                }
            }
        }
    }

    override fun observeBatches(jobId: String): Flow<AudioTranscriptionBatchEvent> = channelFlow {
        xfyunAsrCoordinator.observeJob(jobId)
            .filterIsInstance<XfyunAsrJobState.Completed>()
            .take(1)
            .collect { state ->
                val durationMs = durationByJobId[state.jobId]
                // 生成 V1 窗口计划仅为后续时间锚点铺垫，不影响现有展示逻辑。
                val planWithWindows = buildBatchPlanWithWindows(
                    markdown = state.transcriptMarkdown,
                    audioDurationMs = durationMs,
                    batchDurationMs = V1_BATCH_DURATION_MS,
                    overlapMs = V1_OVERLAP_MS
                )
                val plan = planWithWindows?.plan ?: TranscriptionBatchPlanner.plan(state.transcriptMarkdown)
                if (plan.totalBatches <= 0) {
                    durationByJobId.remove(state.jobId)
                    return@collect
                }
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

    private data class PendingUpload(
        val file: File,
        val durationMs: Long?,
    )

    private companion object {
        private val SUPPORTED_EXTENSIONS = setOf("wav", "mp3")
        // 说明：为 V1 时间锚点预留的保守窗口参数（当前只生成窗口，不改展示）。
        private const val V1_BATCH_DURATION_MS = 60_000L
        private const val V1_OVERLAP_MS = 5_000L
    }
}

internal fun buildBatchPlanWithWindows(
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
