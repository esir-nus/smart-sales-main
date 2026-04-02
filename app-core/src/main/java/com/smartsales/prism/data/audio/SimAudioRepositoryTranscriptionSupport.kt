package com.smartsales.prism.data.audio

import com.smartsales.core.util.Result
import com.smartsales.data.oss.OssUploadResult
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuJobState
import com.smartsales.prism.domain.tingwu.TingwuRequest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class SimAudioRepositoryTranscriptionSupport(
    private val runtime: SimAudioRepositoryRuntime,
    private val storeSupport: SimAudioRepositoryStoreSupport,
    private val artifactSupport: SimAudioRepositoryArtifactSupport
) {

    fun resumeTrackedJobs() {
        runtime.audioFiles.value
            .filter { it.status == TranscriptionStatus.TRANSCRIBING && !it.activeJobId.isNullOrBlank() }
            .forEach { audio ->
                observeTranscription(audio.id, audio.activeJobId!!)
            }
    }

    suspend fun startTranscription(audioId: String) {
        val audio = storeSupport.getAudio(audioId) ?: throw Exception("找不到音频条目")
        if (audio.status == TranscriptionStatus.TRANSCRIBED && artifactSupport.getArtifacts(audioId) != null) {
            android.util.Log.d("SimAudioRepository", "skip rerun for already-transcribed audioId=$audioId")
            return
        }

        audio.activeJobId?.takeIf { audio.status == TranscriptionStatus.TRANSCRIBING }?.let { jobId ->
            observeTranscription(audioId, jobId)
            return
        }

        storeSupport.mutateAndSave { current ->
            current.map {
                if (it.id == audioId) {
                    it.copy(
                        status = TranscriptionStatus.TRANSCRIBING,
                        progress = 0.05f,
                        lastErrorMessage = null
                    )
                } else {
                    it
                }
            }
        }

        try {
            val fileToTranscribe = resolveSimStoredAudioFile(runtime.context, audioId)
                ?: throw Exception("找不到 SIM 本地音频实体")

            val objectKey = "smartsales/sim/audio/${System.currentTimeMillis()}/${fileToTranscribe.name}"
            val uploadResult = runtime.ossUploader.upload(fileToTranscribe, objectKey)
            val publicUrl = when (uploadResult) {
                is OssUploadResult.Success -> uploadResult.publicUrl
                is OssUploadResult.Error -> throw Exception("[OSS_${uploadResult.code}] ${uploadResult.message}")
            }

            val request = TingwuRequest(
                ossObjectKey = objectKey,
                fileUrl = publicUrl,
                audioAssetName = fileToTranscribe.name,
                language = "zh-CN",
                audioFilePath = fileToTranscribe
            )

            val submitResult = runtime.tingwuPipeline.submit(request)
            val jobId = when (submitResult) {
                is Result.Success -> submitResult.data
                is Result.Error -> throw Exception("Tingwu submission failed", submitResult.throwable)
            }
            storeSupport.mutateAndSave { current ->
                current.map {
                    if (it.id == audioId) {
                        it.copy(activeJobId = jobId, lastErrorMessage = null)
                    } else {
                        it
                    }
                }
            }
            observeTranscription(audioId, jobId)
        } catch (e: Exception) {
            storeSupport.mutateAndSave { current ->
                current.map {
                    if (it.id == audioId) {
                        it.copy(
                            status = TranscriptionStatus.PENDING,
                            progress = 0f,
                            activeJobId = null,
                            lastErrorMessage = e.message ?: "转写失败，请稍后重试。"
                        )
                    } else {
                        it
                    }
                }
            }
            throw e
        }
    }

    private fun observeTranscription(audioId: String, jobId: String) {
        val existingJob = runtime.observationJobs[audioId]
        if (existingJob?.isActive == true) return

        runtime.observationJobs[audioId] = runtime.repositoryScope.launch {
            runtime.tingwuPipeline.observeJob(jobId).collectLatest { state ->
                when (state) {
                    TingwuJobState.Idle -> Unit
                    is TingwuJobState.InProgress -> {
                        storeSupport.mutateAndSave { current ->
                            current.map {
                                if (it.id == audioId) {
                                    it.copy(
                                        status = TranscriptionStatus.TRANSCRIBING,
                                        progress = state.progressPercent / 100f,
                                        activeJobId = jobId,
                                        lastErrorMessage = null
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                    }

                    is TingwuJobState.Completed -> {
                        val artifacts = state.artifacts ?: TingwuJobArtifacts(
                            transcriptMarkdown = state.transcriptMarkdown
                        )
                        artifactSupport.writeArtifacts(audioId, artifacts)
                        val summary = summarizeSimArtifacts(artifacts)

                        storeSupport.mutateAndSave { current ->
                            current.map {
                                if (it.id == audioId) {
                                    it.copy(
                                        status = TranscriptionStatus.TRANSCRIBED,
                                        summary = summary,
                                        progress = 1.0f,
                                        activeJobId = null,
                                        lastErrorMessage = null
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                        runtime.observationJobs.remove(audioId)
                    }

                    is TingwuJobState.Failed -> {
                        storeSupport.mutateAndSave { current ->
                            current.map {
                                if (it.id == audioId) {
                                    it.copy(
                                        status = TranscriptionStatus.PENDING,
                                        progress = 0f,
                                        activeJobId = null,
                                        lastErrorMessage = state.reason
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                        runtime.observationJobs.remove(audioId)
                    }
                }
            }
        }
    }
}
