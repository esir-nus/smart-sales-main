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
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.xfyun.XfyunAsrCoordinator
import com.smartsales.data.aicore.xfyun.XfyunAsrJobState
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class XfyunAudioTranscriptionCoordinator @Inject constructor(
    private val xfyunAsrCoordinator: XfyunAsrCoordinator,
    private val aiParaSettingsProvider: AiParaSettingsProvider,
) : AudioTranscriptionCoordinator {

    private val pendingUploads = ConcurrentHashMap<String, PendingUpload>()

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

        // 重要：转写参数以 AiParaSettings 为唯一来源，避免多处硬编码导致“看起来开了但实际没生效”。
        val settings = aiParaSettingsProvider.snapshot()
        return xfyunAsrCoordinator.submitTranscription(
            file = pending.file,
            language = language,
            roleType = settings.xfyunRoleType,
            roleNum = settings.xfyunRoleNum,
            engSmoothproc = settings.xfyunEngSmoothproc,
            durationMs = pending.durationMs,
        )
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
                is XfyunAsrJobState.Failed -> AudioTranscriptionJobState.Failed(
                    jobId = state.jobId,
                    reason = state.reason
                )
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

    private data class PendingUpload(
        val file: File,
        val durationMs: Long?,
    )

    private companion object {
        private val SUPPORTED_EXTENSIONS = setOf("wav", "mp3")
    }
}
