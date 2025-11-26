package com.smartsales.data.aicore

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.tingwu.TingwuApi
import com.smartsales.data.aicore.tingwu.TingwuCreateTaskData
import com.smartsales.data.aicore.tingwu.TingwuCreateTaskRequest
import com.smartsales.data.aicore.tingwu.TingwuCreateTaskResponse
import com.smartsales.data.aicore.tingwu.TingwuResultData
import com.smartsales.data.aicore.tingwu.TingwuResultResponse
import com.smartsales.data.aicore.tingwu.TingwuStatusData
import com.smartsales.data.aicore.tingwu.TingwuStatusResponse
import com.smartsales.data.aicore.tingwu.TingwuTranscription
import com.smartsales.data.aicore.tingwu.TingwuTaskParameters
import com.smartsales.data.aicore.tingwu.TingwuSummarizationParameters
import com.google.gson.Gson
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.io.path.createTempFile

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/RealTingwuCoordinatorTest.kt
// 模块：:data:ai-core
// 说明：验证真实 Tingwu 协调器的轮询与状态映射
// 作者：创建于 2025-11-16
@OptIn(ExperimentalCoroutinesApi::class)
class RealTingwuCoordinatorTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = FakeDispatcherProvider(dispatcher)
    private val credentialsProvider = object : TingwuCredentialsProvider {
        override fun obtain(): TingwuCredentials = TingwuCredentials(
            apiKey = "demo-api-key",
            baseUrl = "https://tingwu.cn/",
            appKey = "demo-app",
            accessKeyId = "id",
            accessKeySecret = "secret",
            securityToken = "sts",
            model = "tingwu-demo"
        )
    }
    private val signedUrlProvider = object : OssSignedUrlProvider {
        override suspend fun generate(objectKey: String, expiresInSeconds: Long): Result<String> =
            Result.Success("https://oss.example.com/$objectKey?exp=$expiresInSeconds")
    }
    @Test
    fun submit_emitsCompletedTranscript() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "## 摘要\n- 测试成功",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 10.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = mapOf(
                    "Transcription" to "https://example.com/transcription.json",
                    "AutoChapters" to "https://example.com/chapters.json"
                ),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        assertTrue(completed.transcriptMarkdown.contains("测试成功"))
        assertEquals("https://example.com/transcription.json", completed.artifacts?.transcriptionUrl)
        assertEquals("https://example.com/chapters.json", completed.artifacts?.autoChaptersUrl)
    }

    @Test
    fun createTask_enablesSummarization() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            optionalConfig = Optional.empty()
        )

        coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )

        val request = api.lastCreateRequest
        assertEquals(true, request?.parameters?.summarizationEnabled)
        assertEquals(listOf("Paragraph"), request?.parameters?.summarization?.types)
        val json = Gson().toJson(request)
        assertTrue(json.contains("\"SummarizationEnabled\":true"))
        assertTrue(json.contains("\"Summarization\":{\"Types\":[\"Paragraph\"]}"))
    }

    @Test
    fun createTask_whenSummarizationDisabled_omitsSummarizationTypes() {
        val request = TingwuCreateTaskRequest(
            appKey = "demo",
            input = com.smartsales.data.aicore.tingwu.TingwuTaskInput(
                sourceLanguage = "cn",
                taskKey = "k1",
                fileUrl = "https://example.com/a.wav"
            ),
            parameters = TingwuTaskParameters(
                transcription = null,
                translationEnabled = null,
                summarizationEnabled = false,
                summarization = null
            )
        )
        val json = Gson().toJson(request)
        assertTrue(json.contains("\"SummarizationEnabled\":false"))
        assertTrue(!json.contains("\"Summarization\""))
    }

    @Test
    fun completedJob_withSummarizationLink_emitsSmartSummary() = runTest(dispatcher) {
        val summaryFile = createTempFile(suffix = ".json").toFile().apply {
            writeText(
                """
                {
                  "Summary":"会议概览",
                  "KeyPoints":["要点1"],
                  "ActionItems":["行动A"]
                }
                """.trimIndent()
            )
        }
        val summaryUrl = summaryFile.toURI().toURL().toString()
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf(
                    "Transcription" to "https://example.com/transcription.json",
                    "Summarization" to summaryUrl
                )
            )
        )
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "正文",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 10.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = mapOf(
                    "Transcription" to "https://example.com/transcription.json",
                    "Summarization" to summaryUrl
                ),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val summary = completed.artifacts?.smartSummary
        assertEquals("会议概览", summary?.summary)
        assertEquals(listOf("要点1"), summary?.keyPoints)
        assertEquals(listOf("行动A"), summary?.actionItems)
    }

    @Test
    fun completedJob_withoutSummarizationLink_keepsSmartSummaryNull() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "正文",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 10.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = mapOf("Transcription" to "https://example.com/transcription.json"),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        assertEquals(null, completed.artifacts?.smartSummary)
    }

    @Test
    fun submit_updatesFailedStateWhenApiFails() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(
            statusResponse(
                status = "FAILED",
                progress = 60,
                errorCode = "Error",
                errorMessage = "语音太短"
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "error.wav",
                fileUrl = "https://oss.example.com/error.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()
        val failed = coordinator.observeJob(jobId).first { it is TingwuJobState.Failed } as TingwuJobState.Failed
        assertEquals("语音太短", failed.reason)
        assertEquals(AiCoreErrorReason.REMOTE, failed.error.reason)
    }

    @Test
    fun submit_returnsErrorWhenCredentialsMissing() = runTest(dispatcher) {
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = FakeTingwuApi(),
            credentialsProvider = object : TingwuCredentialsProvider {
                override fun obtain(): TingwuCredentials = TingwuCredentials(
                    apiKey = "",
                    baseUrl = "https://tingwu.cn/",
                    appKey = "",
                    accessKeyId = "",
                    accessKeySecret = "",
                    securityToken = null,
                    model = "tingwu"
                )
            },
            signedUrlProvider = signedUrlProvider,
            optionalConfig = Optional.empty()
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "missing.wav",
                fileUrl = "https://oss.example.com/missing.wav"
            )
        )

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable as AiCoreException
        assertEquals(AiCoreErrorReason.MISSING_CREDENTIALS, error.reason)
    }

    @Test
    fun fetchTranscriptFallsBackToResultLinkWhenApiMissing() = runTest(dispatcher) {
        val api = FakeTingwuApi().apply { failResultWith404 = true }
        val processing = statusResponse(status = "PROCESSING", progress = 20)
        api.enqueueStatus(processing)
        val tempFile = createTempFile(suffix = ".json").toFile().apply {
            writeText(
                """
                {
                  "RequestId":"req-link",
                  "Code":"0",
                  "Message":"success",
                  "Data":{
                    "TaskId":"job-1",
                    "Transcription":{
                      "Text":"## Link 模式",
                      "Segments":[],
                      "Speakers":[],
                      "Language":"zh",
                      "Duration":5.0
                    }
                  }
                }
                """.trimIndent()
            )
            deleteOnExit()
        }
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf("Transcription" to tempFile.toURI().toURL().toString())
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            optionalConfig = Optional.of(AiCoreConfig(tingwuPollIntervalMillis = 10))
        )
        val submitResult = coordinator.submit(
            TingwuRequest(
                audioAssetName = "link.wav",
                fileUrl = "https://oss.example.com/link.wav"
            )
        )
        assertTrue(submitResult is Result.Success)
        advanceTimeBy(20)
        advanceUntilIdle()
        val completed = coordinator.observeJob("job-1").first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        assertTrue(completed.transcriptMarkdown.contains("Link 模式"))
    }

    private class FakeTingwuApi : TingwuApi {
        private val statusQueue = ConcurrentLinkedQueue<TingwuStatusResponse>()
        var resultData: TingwuResultResponse? = null
        var failResultWith404: Boolean = false
        var lastCreateRequest: TingwuCreateTaskRequest? = null

        fun enqueueStatus(data: TingwuStatusResponse) {
            statusQueue += data
        }

        override suspend fun createTranscriptionTask(
            type: String,
            operation: String?,
            body: TingwuCreateTaskRequest
        ): TingwuCreateTaskResponse {
            lastCreateRequest = body
            return TingwuCreateTaskResponse(
                requestId = "req-create",
                code = "0",
                message = "Success",
                data = TingwuCreateTaskData(
                    taskId = "job-1",
                    taskKey = body.input.taskKey,
                    taskStatus = "QUEUED",
                    meetingJoinUrl = null
                )
            )
        }

        override suspend fun getTaskStatus(
            taskId: String,
            type: String
        ): TingwuStatusResponse {
            return statusQueue.poll()
                ?: statusResponse(
                    status = "FAILED",
                    progress = 0,
                    errorCode = "Empty",
                    errorMessage = "状态为空"
                )
        }

        override suspend fun getTaskResult(
            taskId: String,
            format: String
        ): TingwuResultResponse {
            if (failResultWith404) {
                throw HttpException(
                    Response.error<TingwuResultResponse>(
                        404,
                        "Not Found".toResponseBody("application/json".toMediaType())
                    )
                )
            }
            return resultData
                ?: TingwuResultResponse(
                    requestId = "req-result-empty",
                    code = "0",
                    message = "Success",
                    data = TingwuResultData(
                        taskId = taskId,
                        transcription = TingwuTranscription(
                            text = "",
                            segments = emptyList(),
                            speakers = emptyList(),
                            language = "zh",
                            duration = 0.0
                        ),
                        resultLinks = emptyMap(),
                        outputMp3Path = null,
                        outputMp4Path = null,
                        outputThumbnailPath = null,
                        outputSpectrumPath = null
                    )
                )
        }
    }

    @Test
    fun parseAutoChaptersPayload_parsesList() {
        val json = """
            {
              "Chapters": [
                {"Title": "开场", "Start": 0, "End": 12.5},
                {"Title": "报价讨论", "StartTime": 42, "EndTime": 85}
              ]
            }
        """.trimIndent()
        val chapters = parseAutoChaptersPayload(json)
        assertEquals(2, chapters.size)
        assertEquals("开场", chapters.first().title)
        assertEquals(0L, chapters.first().startMs)
    }

    @Test
    fun parseSmartSummaryPayload_parsesSummaryAndLists() {
        val json = """
            {
              "Summary": "会议概览",
              "KeyPoints": ["要点1", "要点2"],
              "ActionItems": ["行动A"]
            }
        """.trimIndent()
        val summary = parseSmartSummaryPayload(json)
        assertEquals("会议概览", summary?.summary)
        assertEquals(listOf("要点1", "要点2"), summary?.keyPoints)
        assertEquals(listOf("行动A"), summary?.actionItems)
    }
}

private fun statusResponse(
    status: String,
    progress: Int,
    errorCode: String? = null,
    errorMessage: String? = null,
    resultLinks: Map<String, String>? = null
): TingwuStatusResponse = TingwuStatusResponse(
    requestId = "req-$status-$progress",
    code = "0",
    message = "Success",
    data = TingwuStatusData(
        taskId = "job-1",
        taskKey = "job-1",
        taskStatus = status,
        taskProgress = progress,
        errorCode = errorCode,
        errorMessage = errorMessage,
        outputMp3Path = null,
        outputMp4Path = null,
        outputThumbnailPath = null,
        outputSpectrumPath = null,
        resultLinks = resultLinks,
        meetingJoinUrl = null
    )
)
