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
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.media.audiofiles.TranscriptionBatchPlanWithWindows
import com.smartsales.feature.media.audiofiles.TranscriptionBatchPlanner
import com.smartsales.feature.media.audiofiles.V1TimedTextSegment
import com.smartsales.feature.media.audiofiles.V1WindowIndexedBatchReleasedBuilder
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
        Log.d(TAG, "observeBatches: starting channelFlow for jobId=$jobId")
        tingwuCoordinator.observeJob(jobId)
            .filterIsInstance<TingwuJobState.Completed>()
            .take(1)
            .collect { state ->
                Log.d(TAG, "observeBatches: received Completed state, markdown长度=${state.transcriptMarkdown.length}")
                val planWithWindows = buildBatchPlanWithWindowsForTingwu(
                    markdown = state.transcriptMarkdown,
                    audioDurationMs = durationByJobId[state.jobId],
                    batchDurationMs = V1_BATCH_DURATION_MS,
                    overlapMs = V1_OVERLAP_MS
                )
                // 说明：窗口计划仅用于后续 V1 锚点接入，当前展示逻辑不变。
                val plan = planWithWindows?.plan ?: TranscriptionBatchPlanner.plan(state.transcriptMarkdown)
                Log.d(TAG, "observeBatches: batch plan created, totalBatches=${plan.totalBatches}")
                val v1TotalBatches = planWithWindows?.windows?.size
                if (plan.totalBatches <= 0) {
                    Log.w(TAG, "observeBatches: plan.totalBatches <= 0, returning without emitting batches")
                    durationByJobId.remove(state.jobId)
                    return@collect
                }
                // 说明：timedSegments 必须是录音起点(0ms)绝对时间（非归一化），避免后续窗口过滤错位。
                val timedSegments = state.artifacts?.recordingOriginDiarizedSegments
                    ?.map { segment ->
                        V1TimedTextSegment(
                            startMs = segment.startMs,
                            endMs = segment.endMs,
                            text = segment.text
                        )
                    }
                    ?.takeIf { it.isNotEmpty() }
                val windows = planWithWindows?.windows
                if (windows != null && !timedSegments.isNullOrEmpty()) {
                    // 说明：按 V1 窗口顺序发批次，batchIndex 与窗口一致（非行分块索引）。
                    val releases = V1WindowIndexedBatchReleasedBuilder.build(
                        jobId = state.jobId,
                        windows = windows,
                        timedSegments = timedSegments,
                        transcriptMarkdown = state.transcriptMarkdown,
                        v1BatchPlanRule = "v1_windowed",
                        v1BatchDurationMs = V1_BATCH_DURATION_MS,
                        v1OverlapMs = V1_OVERLAP_MS
                    )
                    if (BuildConfig.DEBUG) {
                        val minStartMs = timedSegments.minOf { it.startMs }
                        for (release in releases) {
                            val v1Window = release.v1Window
                            if (v1Window != null &&
                                v1Window.absStartMs >= 60_000L &&
                                minStartMs <= 5_000L
                            ) {
                                // 归一化时间会破坏宏窗口过滤；此处仅 DEBUG 告警，便于尽早发现回归。
                                Log.w(
                                    "DefaultAudioTranscriptionCoordinator",
                                    "event=v1_tingwu_timedSegments_timebase_suspect " +
                                        "batchIndex=${release.batchIndex} " +
                                        "absStartMs=${v1Window.absStartMs} " +
                                        "minStartMs=$minStartMs"
                                )
                            }
                        }
                    }
                    for (release in releases) {
                        send(release)
                    }
                } else {
                    plan.batches.forEach { batch ->
                        // 仅透传窗口数据（可选），不影响现有伪流式展示。
                        val v1Window = planWithWindows?.windows?.getOrNull(batch.batchIndex - 1)
                        if (BuildConfig.DEBUG && v1Window != null && !timedSegments.isNullOrEmpty()) {
                            val minStartMs = timedSegments.minOf { it.startMs }
                            if (v1Window.absStartMs >= 60_000L && minStartMs <= 5_000L) {
                                // 归一化时间会破坏宏窗口过滤；此处仅 DEBUG 告警，便于尽早发现回归。
                                Log.w(
                                    "DefaultAudioTranscriptionCoordinator",
                                    "event=v1_tingwu_timedSegments_timebase_suspect " +
                                        "batchIndex=${batch.batchIndex} " +
                                        "absStartMs=${v1Window.absStartMs} " +
                                        "minStartMs=$minStartMs"
                                )
                            }
                        }
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
                                timedSegments = timedSegments,
                                // 说明：V1 窗口计划用于 HUD 展示，优先提供窗口级批次信息。
                                v1BatchPlanRule = if (v1TotalBatches != null) "v1_windowed" else null,
                                v1BatchDurationMs = if (v1TotalBatches != null) V1_BATCH_DURATION_MS else null,
                                v1OverlapMs = if (v1TotalBatches != null) V1_OVERLAP_MS else null,
                                v1TotalBatches = v1TotalBatches,
                                v1CurrentBatchIndex = v1Window?.batchIndex
                            )
                        )
                    }
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
        private const val TAG = "DefaultAudioTranscriptionCoordinator"
        // 说明：为 V1 时间窗口预留的保守默认值（当前仅生成窗口，不影响展示）。
        private const val V1_BATCH_DURATION_MS = 600_000L
        private const val V1_OVERLAP_MS = 10_000L
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
