package com.smartsales.prism.data.tingwu

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.TingwuCredentialsProvider
import com.smartsales.data.aicore.tingwu.api.TingwuApi
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskRequest
import com.smartsales.data.aicore.tingwu.api.TingwuStatusResponse
import com.smartsales.data.aicore.tingwu.api.TingwuTaskInput
import com.smartsales.data.aicore.tingwu.api.TingwuTaskParameters
import com.smartsales.data.aicore.tingwu.api.TingwuTranscriptionParameters
import com.smartsales.data.aicore.tingwu.api.TingwuDiarizationParameters
import com.smartsales.data.aicore.tingwu.api.TingwuSummarizationParameters
import com.smartsales.data.aicore.tingwu.api.TingwuTranscodingParameters
import com.smartsales.prism.domain.tingwu.DiarizedSegment
import com.smartsales.prism.domain.tingwu.TingwuChapter
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuJobState
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.domain.tingwu.TingwuRequest
import com.smartsales.prism.domain.tingwu.TingwuResultLink
import com.smartsales.prism.domain.tingwu.TingwuSmartSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealTingwuPipeline @Inject constructor(
    private val api: TingwuApi,
    private val credentialsProvider: TingwuCredentialsProvider,
    private val dispatchers: DispatcherProvider
) : TingwuPipeline {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val jobFlows = ConcurrentHashMap<String, MutableStateFlow<TingwuJobState>>()
    private val pollingJobs = ConcurrentHashMap<String, Job>()

    override suspend fun submit(request: TingwuRequest): Result<String> = withContext(dispatchers.io) {
        try {
            val credentials = credentialsProvider.obtain()
            val fileUrl = request.fileUrl ?: throw IllegalArgumentException("fileUrl must be provided for Tingwu submission")
            
            val taskKey = "${request.audioAssetName}_${System.currentTimeMillis()}".replace(Regex("[^a-zA-Z0-9]"), "_")

            val apiRequest = TingwuCreateTaskRequest(
                appKey = credentials.appKey,
                input = TingwuTaskInput(
                    sourceLanguage = "cn",
                    taskKey = taskKey,
                    fileUrl = fileUrl
                ),
                parameters = TingwuTaskParameters(
                    transcription = TingwuTranscriptionParameters(
                        diarizationEnabled = request.diarizationEnabled,
                        diarization = if (request.diarizationEnabled) TingwuDiarizationParameters(outputLevel = 1) else null
                    ),
                    autoChaptersEnabled = true, // We always want chapters for Analyst
                    summarizationEnabled = true, // We always want summaries for Analyst
                    summarization = TingwuSummarizationParameters(types = listOf("Paragraph", "Conversational")),
                    transcoding = TingwuTranscodingParameters(targetAudioFormat = "mp3")
                )
            )

            val response = api.createTranscriptionTask(body = apiRequest)
            
            if (response.code != null && response.code != "0") {
                return@withContext Result.Error(Exception("Tingwu create task failed: ${response.code} - ${response.message}"))
            }

            val taskId = response.data?.taskId ?: return@withContext Result.Error(Exception("No TaskId returned from Tingwu"))
            
            val flow = getOrCreateJobFlow(taskId)
            flow.value = TingwuJobState.InProgress(taskId, 1, "SUBMITTED")
            
            startPolling(taskId)
            
            Result.Success(taskId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(e)
        }
    }

    override fun observeJob(jobId: String): Flow<TingwuJobState> =
        getOrCreateJobFlow(jobId).asStateFlow()

    private fun getOrCreateJobFlow(jobId: String): MutableStateFlow<TingwuJobState> =
        jobFlows.getOrPut(jobId) { MutableStateFlow(TingwuJobState.Idle) }

    private fun startPolling(jobId: String) {
        if (pollingJobs.containsKey(jobId) && pollingJobs[jobId]?.isActive == true) return

        val flow = getOrCreateJobFlow(jobId)
        val job = scope.launch {
            try {
                var isCompleted = false
                while (!isCompleted && isActive) {
                    delay(5000) // Poll every 5s
                    
                    val statusResponse = try {
                        api.getTaskStatus(taskId = jobId)
                    } catch (e: Exception) {
                        continue // Ignore transient network errors during polling
                    }

                    if (statusResponse.code != null && statusResponse.code != "0") {
                        if (statusResponse.code == "TaskNotFound") {
                            flow.value = TingwuJobState.Failed(jobId, "Task Not Found on Aliyun servers.", statusResponse.code)
                            isCompleted = true
                            continue
                        }
                    }

                    val taskStatus = statusResponse.data?.taskStatus
                    when (taskStatus) {
                        "COMPLETED" -> {
                            fetchAndEmitResult(jobId, statusResponse, flow)
                            isCompleted = true
                        }
                        "FAILED" -> {
                            val data = statusResponse.data
                            flow.value = TingwuJobState.Failed(
                                jobId, 
                                statusResponse.data?.errorMessage ?: "Tingwu task failed.",
                                statusResponse.data?.errorCode
                            )
                            isCompleted = true
                        }
                        else -> {
                            val progress = statusResponse.data?.taskProgress ?: 5
                            flow.value = TingwuJobState.InProgress(jobId, progress, taskStatus)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    flow.value = TingwuJobState.Failed(jobId, e.message ?: "Unknown polling error")
                }
            } finally {
                pollingJobs.remove(jobId)
            }
        }
        pollingJobs[jobId] = job
    }

    private suspend fun fetchAndEmitResult(
        jobId: String, 
        statusResponse: TingwuStatusResponse,
        flow: MutableStateFlow<TingwuJobState>
    ) {
        try {
            val resultResponse = api.getTaskResult(taskId = jobId)
            
            if (resultResponse.code != null && resultResponse.code != "0") {
                flow.value = TingwuJobState.Failed(jobId, "Failed to fetch result: ${resultResponse.message}", resultResponse.code)
                return
            }

            val transcription = resultResponse.data?.transcription
            val markdown = transcription?.text ?: "转写结果为空。"
            
            // Map the segments
            val diarizedSegments = transcription?.segments?.mapIndexed { index, seg ->
                DiarizedSegment(
                    speakerId = seg.speaker,
                    speakerIndex = index,
                    startMs = (seg.start?.times(1000))?.toLong() ?: 0L,
                    endMs = (seg.end?.times(1000))?.toLong() ?: 0L,
                    text = seg.text ?: ""
                )
            }

            // In a real scenario, we'd fetch the ExtraResults (AutoChapters, Summarization URL JSONs) here.
            // But to keep this decoupled Prism layer lean, we directly serialize the resultLinks. The Analyst layer will parse them.
            val resultLinks = resultResponse.data?.resultLinks?.map { TingwuResultLink(it.key, it.value) } ?: emptyList()
            
            val autoChaptersUrl = resultResponse.data?.resultLinks?.get("AutoChapters")
            val summarizationUrl = resultResponse.data?.resultLinks?.get("Summarization")

            val artifacts = TingwuJobArtifacts(
                outputMp3Path = statusResponse.data?.outputMp3Path,
                outputMp4Path = statusResponse.data?.outputMp4Path,
                outputSpectrumPath = statusResponse.data?.outputSpectrumPath,
                outputThumbnailPath = statusResponse.data?.outputThumbnailPath,
                transcriptionUrl = transcription?.url,
                autoChaptersUrl = autoChaptersUrl,
                resultLinks = resultLinks,
                diarizedSegments = diarizedSegments,
                recordingOriginDiarizedSegments = diarizedSegments,
                // We'll leave `smartSummary` and `chapters` to be parsed from the URLs locally if needed,
                // or the user can just use the provided summary for now.
                smartSummary = TingwuSmartSummary("智能摘要已生成，可通过结果链接下载。")
            )

            flow.value = TingwuJobState.Completed(
                jobId = jobId,
                transcriptMarkdown = markdown,
                artifacts = artifacts,
                statusLabel = "COMPLETED"
            )
        } catch (e: Exception) {
            flow.value = TingwuJobState.Failed(jobId, "Failed to parse Tingwu results: ${e.message}")
        }
    }
}
