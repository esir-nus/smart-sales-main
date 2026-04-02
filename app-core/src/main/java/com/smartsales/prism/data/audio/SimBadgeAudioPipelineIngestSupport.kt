package com.smartsales.prism.data.audio

import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 将 BadgeAudioPipeline 成功处理后的录音持久化到 SIM 音频抽屉命名空间。
 *
 * 说明：
 * - 音频抽屉库存是规格真相源，因此自动管道完成后必须把同一条录音带入抽屉。
 * - 这里保留原始转写正文为最小工件，避免因为清理 Badge 源文件而丢失抽屉可见性。
 */
@Singleton
class SimBadgeAudioPipelineIngestSupport @Inject constructor(
    private val runtime: SimAudioRepositoryRuntime
) {

    private val storeSupport = SimAudioRepositoryStoreSupport(runtime)
    private val artifactSupport = SimAudioRepositoryArtifactSupport(runtime, storeSupport)

    suspend fun ingestCompletedRecording(
        filename: String,
        localFile: File,
        transcript: String
    ): Boolean {
        val normalizedFilename = simPendingBadgeDeleteFilename(filename)
        val existing = storeSupport.getAudioFilesSnapshot().firstOrNull {
            it.source == AudioSource.SMARTBADGE &&
                simPendingBadgeDeleteFilename(it.filename) == normalizedFilename
        }
        val audioId = existing?.id ?: UUID.randomUUID().toString()
        val destFile = simStoredAudioFile(runtime.context, audioId, "wav")

        return runCatching {
            if (!destFile.exists()) {
                localFile.copyTo(destFile, overwrite = true)
            }

            val artifacts = TingwuJobArtifacts(
                transcriptMarkdown = transcript.takeIf { it.isNotBlank() }
            )
            artifactSupport.writeArtifacts(audioId, artifacts)
            val summary = summarizeSimArtifacts(artifacts)

            storeSupport.mutateAndSave { current ->
                val updated = AudioFile(
                    id = audioId,
                    filename = filename,
                    timeDisplay = existing?.timeDisplay ?: "Just now",
                    source = AudioSource.SMARTBADGE,
                    status = TranscriptionStatus.TRANSCRIBED,
                    isStarred = existing?.isStarred ?: false,
                    summary = summary,
                    progress = 1f,
                    boundSessionId = existing?.boundSessionId,
                    activeJobId = null,
                    lastErrorMessage = null
                )
                if (existing == null) {
                    current + updated
                } else {
                    current.map { audio -> if (audio.id == audioId) updated else audio }
                }
            }

            true
        }.onFailure {
            android.util.Log.e(
                "AudioPipeline",
                "SIM drawer ingest failed for filename=$filename",
                it
            )
        }.getOrDefault(false).also {
            localFile.delete()
        }
    }
}
