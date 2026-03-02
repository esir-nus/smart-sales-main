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
import com.smartsales.data.aicore.tingwu.api.TingwuMeetingAssistanceParameters
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
import kotlinx.coroutines.async
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
                    meetingAssistanceEnabled = true, // Extracts MeetingAssistance links
                    summarization = TingwuSummarizationParameters(types = listOf("Paragraph", "Conversational", "QuestionsAnswering")),
                    meetingAssistance = TingwuMeetingAssistanceParameters(types = listOf("Actions", "KeyInformation")),
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
            val transcriptionUrl = statusResponse.data?.resultLinks?.get("Transcription")
            
            if (transcriptionUrl.isNullOrBlank()) {
                flow.value = TingwuJobState.Failed(jobId, "Tingwu API did not return a Transcription URL in ResultLinks.")
                return
            }

            val client = okhttp3.OkHttpClient()
            
            suspend fun fetchJson(url: String?): String? = withContext(dispatchers.io) {
                if (url.isNullOrBlank()) return@withContext null
                try {
                    val req = okhttp3.Request.Builder().url(url).get().build()
                    val res = client.newCall(req).execute()
                    if (res.isSuccessful) res.body?.string() else null
                } catch (e: Exception) {
                    android.util.Log.e("RealTingwuPipeline", "Job $jobId failed to download from $url: ${e.message}")
                    null
                }
            }

            val summarizationUrl = statusResponse.data?.resultLinks?.get("Summarization")
            val autoChaptersUrl = statusResponse.data?.resultLinks?.get("AutoChapters")
            val meetingAssistanceUrl = statusResponse.data?.resultLinks?.get("MeetingAssistance")

            // Concurrently download artifacts
            val bodies = kotlinx.coroutines.coroutineScope {
                val deferredTranscription = async(dispatchers.io) { fetchJson(transcriptionUrl) }
                val deferredSummary = async(dispatchers.io) { fetchJson(summarizationUrl) }
                val deferredChapters = async(dispatchers.io) { fetchJson(autoChaptersUrl) }
                val deferredMeetingAssistance = async(dispatchers.io) { fetchJson(meetingAssistanceUrl) }
                
                listOf(
                    deferredTranscription.await(),
                    deferredSummary.await(),
                    deferredChapters.await(),
                    deferredMeetingAssistance.await()
                )
            }
            val responseBody = bodies[0]
            val summaryBody = bodies[1]
            val chaptersBody = bodies[2]
            val meetingAssistanceBody = bodies[3]

            if (responseBody == null) throw Exception("Empty body when downloading transcription result.")

            // Parse Transcription resiliently
            val rootObj = kotlinx.serialization.json.Json.parseToJsonElement(responseBody)
            val rootJson = if (rootObj is kotlinx.serialization.json.JsonObject) rootObj else kotlinx.serialization.json.JsonObject(emptyMap())
            
            // Lookahead pattern to find Text, Segments, and Paragraphs
            val innerTranscription = rootJson["Transcription"] as? kotlinx.serialization.json.JsonObject
            val innerDataTranscription = (rootJson["Data"] as? kotlinx.serialization.json.JsonObject)?.get("Transcription") as? kotlinx.serialization.json.JsonObject

            val activeRoot = innerTranscription ?: innerDataTranscription ?: rootJson

            var textStr = activeRoot["Text"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
            val segsArr = activeRoot["Segments"] as? kotlinx.serialization.json.JsonArray
            val paragraphsArr = activeRoot["Paragraphs"] as? kotlinx.serialization.json.JsonArray
            
            // Fallback for paragraph style if Text is missing
            if (textStr.isNullOrBlank() && paragraphsArr != null) {
                val sb = StringBuilder()
                for (i in 0 until paragraphsArr.size) {
                    val para = paragraphsArr[i] as? kotlinx.serialization.json.JsonObject ?: continue
                    val words = para["Words"] as? kotlinx.serialization.json.JsonArray ?: continue
                    for (w in 0 until words.size) {
                        val wordObj = words[w] as? kotlinx.serialization.json.JsonObject ?: continue
                        val wordText = wordObj["Text"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else "" }
                        sb.append(wordText)
                    }
                    sb.append("\n\n")
                }
                textStr = sb.toString().trim()
            }
            
            val markdown = textStr?.ifBlank { null } ?: "转写结果为空。"
            
            val diarizedSegments = mutableListOf<DiarizedSegment>()
            if (segsArr != null) {
                for (i in 0 until segsArr.size) {
                    val seg = segsArr[i] as? kotlinx.serialization.json.JsonObject ?: continue
                    
                    val text = seg["Text"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: ""
                    val speaker = seg["SpeakerId"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                        ?: seg["Speaker"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                    val startSec = seg["Start"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toDoubleOrNull() else null } ?: 0.0
                    val endSec = seg["End"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toDoubleOrNull() else null } ?: 0.0
                    
                    diarizedSegments.add(
                        DiarizedSegment(
                            speakerId = speaker,
                            speakerIndex = i,
                            startMs = (startSec * 1000).toLong(),
                            endMs = (endSec * 1000).toLong(),
                            text = text
                        )
                    )
                }
            }

            // Parse Summarization
            val finalSmartSummary = if (summaryBody != null) {
                val sumRoot = kotlinx.serialization.json.Json.parseToJsonElement(summaryBody)
                val sumWrapper = if (sumRoot is kotlinx.serialization.json.JsonObject) sumRoot else kotlinx.serialization.json.JsonObject(emptyMap())
                
                val sumJson = sumWrapper["Summarization"] as? kotlinx.serialization.json.JsonObject ?: sumWrapper
                
                val paragraphTitle = sumJson["ParagraphTitle"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                val paragraphSummary = sumJson["ParagraphSummary"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                val conversationalArr = sumJson["ConversationalSummary"] as? kotlinx.serialization.json.JsonArray
                val conversational = conversationalArr?.mapNotNull { item -> 
                    val obj = item as? kotlinx.serialization.json.JsonObject
                    val name = obj?.get("SpeakerName")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else "发言人" }
                    val summary = obj?.get("Summary")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                    if (summary != null) "**$name**: $summary" else null
                }?.joinToString("\n")
                
                val qaArr = sumJson["QuestionsAnsweringSummary"] as? kotlinx.serialization.json.JsonArray
                val qa = qaArr?.mapNotNull { item ->
                    val obj = item as? kotlinx.serialization.json.JsonObject
                    val q = obj?.get("Question")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                    val a = obj?.get("Answer")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                    if (q != null && a != null) "Q: $q\nA: $a" else null
                }?.joinToString("\n\n")

                val text = buildString {
                    if (!paragraphTitle.isNullOrBlank()) appendLine("**$paragraphTitle**")
                    if (!paragraphSummary.isNullOrBlank()) appendLine(paragraphSummary)
                    if (!conversational.isNullOrBlank()) {
                        appendLine("\n**发言人总结**")
                        appendLine(conversational)
                    }
                    if (!qa.isNullOrBlank()) {
                        appendLine("\n**问答回顾**")
                        appendLine(qa)
                    }
                }.trim().ifBlank { 
                    sumJson["Summary"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: "无摘要内容" 
                }
                
                val keyPointsArray = sumJson["KeyPoints"] as? kotlinx.serialization.json.JsonArray
                    ?: sumJson["Highlights"] as? kotlinx.serialization.json.JsonArray
                    ?: sumJson["Keypoints"] as? kotlinx.serialization.json.JsonArray
                
                val keyPointsList = mutableListOf<String>()
                if (keyPointsArray != null) {
                    for (i in 0 until keyPointsArray.size) {
                        (keyPointsArray[i] as? kotlinx.serialization.json.JsonPrimitive)?.content?.let { keyPointsList.add(it) }
                    }
                }
                TingwuSmartSummary(summary = text, keyPoints = keyPointsList)
            } else {
                TingwuSmartSummary("智能摘要已生成，可通过结果链接下载。")
            }

            // Parse Chapters (by injecting a small parse function locally, or using TingwuPayloadParser through reflection/making it public if we had to)
            // But doing a quick local parse for safety:
            val finalChapters = if (chaptersBody != null) {
                val chRoot = kotlinx.serialization.json.Json.parseToJsonElement(chaptersBody)
                val chArray = if (chRoot is kotlinx.serialization.json.JsonArray) chRoot else {
                    (chRoot as? kotlinx.serialization.json.JsonObject)?.get("AutoChapters") as? kotlinx.serialization.json.JsonArray    
                }
                
                chArray?.mapNotNull { element ->
                    val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                    val title = obj["Headline"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                        ?: obj["Title"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: "Unknown"
                    val startRaw = obj["Start"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toLongOrNull() else null } ?: 0L
                    val endRaw = obj["End"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toLongOrNull() else null } ?: 0L
                    
                    val startMs = if (startRaw < 100000) startRaw * 1000 else startRaw
                    val endMs = if (endRaw < 100000) endRaw * 1000 else endRaw
                    val chapSummary = obj["Summary"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                    
                    TingwuChapter(title = title, startMs = startMs, endMs = endMs, summary = chapSummary)
                }
            } else {
                null
            }

            val resultLinks = statusResponse.data?.resultLinks?.map { TingwuResultLink(it.key, it.value) } ?: emptyList()

            val artifacts = TingwuJobArtifacts(
                outputMp3Path = statusResponse.data?.outputMp3Path,
                outputMp4Path = statusResponse.data?.outputMp4Path,
                outputSpectrumPath = statusResponse.data?.outputSpectrumPath,
                outputThumbnailPath = statusResponse.data?.outputThumbnailPath,
                transcriptionUrl = transcriptionUrl,
                autoChaptersUrl = autoChaptersUrl,
                resultLinks = resultLinks,
                chapters = finalChapters, 
                diarizedSegments = diarizedSegments,
                recordingOriginDiarizedSegments = diarizedSegments,
                smartSummary = finalSmartSummary,
                meetingAssistanceRaw = meetingAssistanceBody,
                transcriptMarkdown = markdown // Important!
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
